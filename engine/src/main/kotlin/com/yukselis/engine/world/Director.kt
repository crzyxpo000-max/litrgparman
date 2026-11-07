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

    /**
     * @param anywhere konum ve bölge denetimi yapılmaz: üste gösterilecek "başka bölgede" görev kartları için
     *   (o kart oynanınca oraya bir yan görev başlar).
     */
    fun eligible(state: WorldState, e: EventDef, anywhere: Boolean = false): Boolean {
        if (e.id in state.dayState.seen && !e.repeatable) return false
        if (e.acts.isNotEmpty() && state.act !in e.acts) return false
        if (!anywhere && e.regions.isNotEmpty() && state.region !in e.regions) return false
        if (!anywhere && !fitsLocation(state, e)) return false
        if (state.day < e.dayMin || state.day > e.dayMax) return false
        if (!npcsAlive(state, e)) return false
        return e.condition?.test(state) ?: true
    }

    /** Üs bölgesi (perdeye göre); bölüm yapısı yoksa başlangıç bölgesi. */
    fun base(state: WorldState): String? =
        content.chapters.base(content.chapters.get(state.chapter)?.act ?: state.act) ?: content.startRegion

    /**
     * Konum kuralı (DECISIONS #130): üste yalnız `us` ve üs bölgesindeki olaylar; keşif görevinde yalnız o bölgenin
     * ve `*` olayları; operasyon ve yan görevde havuzdan hiçbir şey (yalnız görevin kendi olayları).
     */
    fun fitsLocation(state: WorldState, e: EventDef): Boolean {
        if (content.chapters.isEmpty) return true
        val w = e.where
        val m = state.mission ?: return ChapterTable.BASE in w || base(state) in w
        return m.kind == MissionKind.EXPLORE && (m.region in w || ChapterTable.ANYWHERE in w)
    }

    /** Olayın oyuncunun gidebileceği ilk bölgesi (üsteki görev kartı için); yoksa null. */
    fun missionRegion(state: WorldState, e: EventDef, reachable: (String) -> Boolean): String? =
        e.where.firstOrNull { it != ChapterTable.BASE && it != ChapterTable.ANYWHERE && it != base(state) && reachable(it) }

    /** Operasyon ve yan görev kuyruğundaki olay: gün ve yer denetimi yok, koşul ve NPC'ler var. */
    fun eligibleQueued(state: WorldState, e: EventDef): Boolean {
        if (e.id in state.dayState.seen && !e.repeatable) return false
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
        return dueSeed(state)?.let { Forced(it.eventId, it) }
    }

    /** Vadesi gelmiş, koşulu tutan ve buraya uyan ilk tohum (bölüm yapısında konum kuralıyla). */
    fun dueSeed(state: WorldState, here: Boolean = true): Seed? {
        val due = state.seeds.filter { it.dueDay <= state.day }.sortedBy { it.dueDay }
        for (seed in due) {
            val event = content.events[seed.eventId] ?: continue
            if (here && !fitsLocation(state, event)) continue
            if (seedConditionHolds(state, seed) && (event.repeatable || event.id !in state.dayState.seen)) return seed
        }
        return null
    }

    /** Vadesi gelmiş ama başka bir bölgede geçen tohumlar (üste görev kartı olur). */
    fun dueElsewhere(state: WorldState): List<Seed> =
        state.seeds.filter { seed ->
            val event = content.events[seed.eventId] ?: return@filter false
            seed.dueDay <= state.day && !fitsLocation(state, event) && seedConditionHolds(state, seed) &&
                (event.repeatable || event.id !in state.dayState.seen) && npcsAlive(state, event)
        }.sortedBy { it.dueDay }

    /** Vadesi geçmiş ama koşulu artık tutmayan tohumlar kurur (atılır). */
    fun witheredSeeds(state: WorldState): List<Seed> =
        state.seeds.filter { seed ->
            val event = content.events[seed.eventId]
            // Bölümün yönettiği olay (brifing/operasyon/darboğaz) tohumla gelmez: yeri bölüm tablosu (DECISIONS #134).
            // Ana yay düğümü de tohumla erken gelmez: bölüm kuralıyla gelir.
            seed.eventId in content.chapters.managed || (!content.chapters.isEmpty && event?.mainArcDay != null) ||
            seed.dueDay <= state.day && (
                event == null || !seedConditionHolds(state, seed) || (!event.repeatable && event.id in state.dayState.seen) ||
                    !npcsAlive(state, event) || (seed.npc != null && state.npcs[seed.npc]?.alive == false)
                )
        }

    data class Forced(val eventId: String, val seed: Seed?)

    /** Ölen NPC'nin yay düğümleri kapanır: olayın NPC'lerinden biri ölüyse olay gelmez (ölüm kalıcıdır). */
    /**
     * Saha kampı sahnesi (DECISIONS #144): keşif görevindeyken bu bölgeye etiketli, koşulu tutan ve bütün NPC'leri
     * yanındaki yoldaş olan kamp olayları (ağırlıklı seçim için puanıyla).
     */
    fun campScenes(state: WorldState, companions: Set<String>): List<EventDef> {
        val m = state.mission ?: return emptyList()
        if (m.kind != MissionKind.EXPLORE) return emptyList()
        return content.events.values.filter { e ->
            e.camp && m.region in e.where && e.npcs.isNotEmpty() && e.npcs.all { it in companions } && eligible(state, e)
        }.sortedBy { it.id }
    }

    fun npcsAlive(state: WorldState, e: EventDef): Boolean = e.npcs.all { state.npcs[it]?.alive != false }

    private fun eligibleIgnoringDay(state: WorldState, e: EventDef): Boolean {
        if (e.acts.isNotEmpty() && state.act !in e.acts) return false
        if (e.regions.isNotEmpty() && state.region !in e.regions) return false
        if (!npcsAlive(state, e)) return false
        return e.condition?.test(state) ?: true
    }

    /** Tohum koşulları kayıtta metin olarak durur; her gün yeniden ayrıştırılmasın diye önbellek (QA P2-8). */
    private val seedConditions = java.util.concurrent.ConcurrentHashMap<String, Result<com.yukselis.engine.story.Condition>>()

    fun seedConditionHolds(state: WorldState, seed: Seed): Boolean {
        val raw = seed.condition ?: return true
        val focused = state.copy(focusNpc = seed.npc ?: state.focusNpc)
        val cond = seedConditions.getOrPut(raw) { runCatching { ContentParser.condition(YamlTree.parseObject(raw, "tohum koşulu"), content.endings) } }
        return cond.getOrNull()?.let { runCatching { it.test(focused) }.getOrDefault(false) } ?: false
    }

    /** Hedef gerilim eğrisi: yükselen → zirve → nefes. Dikkat yükseldikçe eğri yukarı kayar. */
    fun desiredTension(state: WorldState): Int {
        val recent = state.dayState.recentTension
        // GDD: üç ağır olaydan sonra kamp ateşi (QA P1-12; önceden iki).
        if (recent.size >= HEAVY_RUN && recent.takeLast(HEAVY_RUN).all { it >= 3 }) return 1
        val last = recent.lastOrNull() ?: 1
        // Dikkat eğriyi en çok bir basamak yukarı kaydırır; yoksa yüksek Dikkat'te hedef hep 4'e kilitleniyordu.
        return (last + 1 + minOf(state.attention / ATTENTION_STEP, 1)).coerceIn(1, 4)
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

    fun score(state: WorldState, e: EventDef): Double =
        // GDD'deki Öncelik_anayay çarpanı burada yok: ana yay düğümleri puanlanmaz, `forced` ile her zaman kazanır
        // (eski arcPriority çarpanı adaylarda hep 1,0'dı; QA P2-7).
        e.weight * tensionFit(state, e) * freshness(state, e) * bond(state, e)

    /** Havuzdan seçilebilir olaylar (tohum/eşik olayları ve zorunlular hariç), puanlarıyla. */
    fun candidates(state: WorldState, anywhere: Boolean = false): List<Pair<EventDef, Double>> =
        content.events.values
            .filter { !it.seedOnly && !it.camp && it.mainArcDay == null && it.id !in content.chapters.managed && eligible(state, it, anywhere) }
            .sortedBy { it.id }
            .map { it to score(state, it) }

    /** Fırsat eli: puanla ağırlıklı, yerine koymasız n kart. */
    fun deal(
        state: WorldState,
        rng: Rng,
        n: Int = HAND_SIZE,
        tag: String? = null,
        anywhere: Boolean = false,
        filter: (EventDef) -> Boolean = { true },
    ): List<String> {
        val pool = candidates(state, anywhere).filter { (tag == null || tag in it.first.tags) && filter(it.first) }.toMutableList()
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
        const val HEAVY_RUN = 3
        const val RECENT_TENSION = 6
        const val RECENT_TAGS = 12
    }
}
