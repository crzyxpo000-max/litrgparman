package com.yukselis.engine.world

import com.yukselis.engine.content.ContentParser
import com.yukselis.engine.content.YamlTree
import com.yukselis.engine.core.Rng
import com.yukselis.engine.core.WorldState
import com.yukselis.engine.story.Content
import kotlin.math.abs
import kotlin.math.pow

/**
 * Hikaye Yönetmeni (GDD): uygun olayları filtreler ve puanlar.
 *
 *     Puan = Ağırlık · Gerilim uyumu · Tazelik · Kişisel bağ · Ana yay önceliği
 *
 * Ana yay düğümleri ve vadesi gelen tohumlar puanlanmaz; "zorunlu" olarak her zaman kazanır.
 */
class Director(private val content: Content) {

    fun eligible(state: WorldState, e: EventDef): Boolean {
        if (e.id in state.dayState.seen && !e.repeatable) return false
        if (e.acts.isNotEmpty() && state.act !in e.acts) return false
        if (e.regions.isNotEmpty() && state.region !in e.regions) return false
        if (state.day < e.dayMin || state.day > e.dayMax) return false
        if (!npcsAlive(state, e)) return false
        return e.condition?.test(state) ?: true
    }

    /** Zorunlu olay: önce vadesi gelmiş ana yay düğümü, sonra vadesi gelmiş ve koşulu tutan tohum. */
    fun forced(state: WorldState): Forced? {
        content.events.values
            .filter { it.mainArcDay != null && state.day >= it.mainArcDay && it.id !in state.dayState.seen }
            .sortedBy { it.mainArcDay }
            .firstOrNull { eligibleIgnoringDay(state, it) }
            ?.let { return Forced(it.id, null) }
        val due = state.seeds.filter { it.dueDay <= state.day }.sortedBy { it.dueDay }
        for (seed in due) {
            val event = content.events[seed.eventId] ?: continue
            if (seedConditionHolds(state, seed) && (event.repeatable || event.id !in state.dayState.seen)) return Forced(event.id, seed)
        }
        return null
    }

    /** Vadesi geçmiş ama koşulu artık tutmayan tohumlar kurur (atılır). */
    fun witheredSeeds(state: WorldState): List<Seed> =
        state.seeds.filter { seed ->
            val event = content.events[seed.eventId]
            seed.dueDay <= state.day && (
                event == null || !seedConditionHolds(state, seed) || (!event.repeatable && event.id in state.dayState.seen) ||
                    !npcsAlive(state, event) || (seed.npc != null && state.npcs[seed.npc]?.alive == false)
                )
        }

    data class Forced(val eventId: String, val seed: Seed?)

    /** Ölen NPC'nin yay düğümleri kapanır: olayın NPC'lerinden biri ölüyse olay gelmez (ölüm kalıcıdır). */
    fun npcsAlive(state: WorldState, e: EventDef): Boolean = e.npcs.all { state.npcs[it]?.alive != false }

    private fun eligibleIgnoringDay(state: WorldState, e: EventDef): Boolean {
        if (e.acts.isNotEmpty() && state.act !in e.acts) return false
        if (e.regions.isNotEmpty() && state.region !in e.regions) return false
        if (!npcsAlive(state, e)) return false
        return e.condition?.test(state) ?: true
    }

    fun seedConditionHolds(state: WorldState, seed: Seed): Boolean {
        val raw = seed.condition ?: return true
        val focused = state.copy(focusNpc = seed.npc ?: state.focusNpc)
        return runCatching { ContentParser.condition(YamlTree.parseObject(raw, "tohum koşulu"), content.endings).test(focused) }.getOrDefault(false)
    }

    /** Hedef gerilim eğrisi: yükselen → zirve → nefes. Dikkat yükseldikçe eğri yukarı kayar. */
    fun desiredTension(state: WorldState): Int {
        val recent = state.dayState.recentTension
        if (recent.size >= 2 && recent.takeLast(2).all { it >= 3 }) return 1 // iki ağır olaydan sonra kamp ateşi
        val last = recent.lastOrNull() ?: 1
        return (last + 1 + state.attention / ATTENTION_STEP).coerceIn(1, 4)
    }

    fun tensionFit(state: WorldState, e: EventDef): Double = 1.0 / (1 + abs(desiredTension(state) - e.tension))

    /** Tazelik: yakında görülen etiketler cezalandırılır (her çakışma ×0,6). */
    fun freshness(state: WorldState, e: EventDef): Double {
        val recent = state.dayState.recentTags
        val overlaps = e.tags.sumOf { tag -> recent.count { it == tag } }
        return 0.6.pow(overlaps)
    }

    /** Kişisel bağ: olaydaki NPC'lerle ilişki yüksekse öne çıkar (Aşama 5'e kadar `iliski_<npc>` değişkeni). */
    fun bond(state: WorldState, e: EventDef): Double {
        if (e.npcs.isEmpty()) return 1.0
        val best = e.npcs.maxOf { npc -> bondValue(state, npc) }
        return 1.0 + best.coerceAtLeast(0) / 100.0
    }

    /** Kişisel bağ değeri: NPC'ye güven ve bağlılığın ortalaması (Aşama 5). */
    fun bondValue(state: WorldState, npc: String): Int = state.npcs[npc]?.relation?.let { (it.trust + it.bond) / 2 } ?: 0

    /** Ana yay önceliği: vadesi gelmemiş ana yay olayları da öne çıkar. */
    fun arcPriority(e: EventDef): Double = if (e.mainArcDay != null) 3.0 else 1.0

    fun score(state: WorldState, e: EventDef): Double =
        e.weight * tensionFit(state, e) * freshness(state, e) * bond(state, e) * arcPriority(e)

    /** Havuzdan seçilebilir olaylar (tohum/eşik olayları ve zorunlular hariç), puanlarıyla. */
    fun candidates(state: WorldState): List<Pair<EventDef, Double>> =
        content.events.values
            .filter { !it.seedOnly && it.mainArcDay == null && eligible(state, it) }
            .sortedBy { it.id }
            .map { it to score(state, it) }

    /** Fırsat eli: puanla ağırlıklı, yerine koymasız n kart. */
    fun deal(state: WorldState, rng: Rng, n: Int = HAND_SIZE, tag: String? = null): List<String> {
        val pool = candidates(state).filter { tag == null || tag in it.first.tags }.toMutableList()
        val hand = mutableListOf<String>()
        while (hand.size < n && pool.isNotEmpty()) {
            val total = pool.sumOf { it.second }
            var r = rng.nextDouble() * total
            val pick = pool.firstOrNull { r -= it.second; r <= 0 } ?: pool.last()
            hand += pick.first.id
            pool.remove(pick)
        }
        return hand
    }

    companion object {
        const val HAND_SIZE = 3
        const val ATTENTION_STEP = 40
        const val RECENT_TENSION = 6
        const val RECENT_TAGS = 12
    }
}
