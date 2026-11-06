package com.yukselis.engine

import com.yukselis.engine.content.ContentLoader
import com.yukselis.engine.content.DirectoryContentSource
import com.yukselis.engine.core.GameMode
import com.yukselis.engine.core.WorldState
import com.yukselis.engine.i18n.Lang
import com.yukselis.engine.story.ChoiceDef
import com.yukselis.engine.story.Content
import com.yukselis.engine.story.Effect
import com.yukselis.engine.story.StoryEngine
import com.yukselis.engine.system.Stat
import com.yukselis.engine.world.ChoiceKind
import com.yukselis.engine.world.HubAction
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

/** Aşama 6: Perde 1 dikey dilimi (içerik kuralları, 500 başsız oynanış, olay kapsamı, dünya durumları). */
class Perde1Test {
    private val engine = StoryEngine(ContentLoader.load(DirectoryContentSource(File("../content"))))
    private val content: Content = engine.content
    private val lang = Lang.TR

    private val act1 = content.events.values.filter { 1 in it.acts && !it.id.startsWith("esik_") }

    /** Bir olayın kendi sahneleri (giriş sahnesinden olay içi geçişler). */
    private fun eventScenes(entry: String): Set<String> {
        val seen = mutableSetOf<String>()
        val q = ArrayDeque(listOf(entry))
        while (q.isNotEmpty()) {
            val id = q.removeFirst()
            if (!seen.add(id)) continue
            content.scenes[id]?.choices?.forEach { c ->
                (listOfNotNull(c.next, c.check?.failNext) + (c.effects + (c.check?.failEffects ?: emptyList()))
                    .filterIsInstance<Effect.StartCombat>().flatMap { listOfNotNull(it.spec.win, it.spec.lose, it.spec.flee) })
                    .filter { it in content.scenes }.forEach { q.addLast(it) }
            }
        }
        return seen
    }

    private fun ChoiceDef.allEffects() = effects + (check?.failEffects ?: emptyList())

    @Test
    fun `perde 1 icerik butcesi ve olay kurallari`() {
        assertTrue("Perde 1 olayı en az 40 olmalı (${act1.size})", act1.size >= 40)
        val problems = mutableListOf<String>()
        for (e in act1) {
            val entry = content.scenes.getValue(e.entry)
            if (entry.choices.size < 3) problems += "${e.id}: giriş sahnesinde ${entry.choices.size} seçenek (en az 3)"
            val choices = eventScenes(e.entry).flatMap { content.scenes[it]?.choices ?: emptyList() }
            if (choices.none { it.requires != null || it.showWhen != null }) problems += "${e.id}: koşullu seçenek yok"
            if (choices.none { c -> c.allEffects().any { it is Effect.PlantSeed } }) problems += "${e.id}: tohum eken seçenek yok"
            if (choices.none { c -> c.allEffects().any { it is Effect.ShiftRelation } }) problems += "${e.id}: NPC ilişki eksenini değiştiren seçenek yok"
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
        // Bütçe: Deniz, Yankı ve Aras'ın en az 3 yay düğümü Perde 1'de yazılı; 2 çok odalı zindan.
        for (npc in listOf("deniz_arman", "yanki", "aras_kurt")) {
            val written = content.npcs.getValue(npc).arcNodes.count { it in content.events }
            assertTrue("$npc yay düğümü: $written", written >= 3)
        }
        assertTrue(listOf("zindan_metro", "zindan_kasa").all { id -> eventScenes(content.events.getValue(id).entry).size >= 4 })
        assertTrue(content.enemies.size >= 15 && content.enemies.values.any { it.boss })
        assertTrue(content.dailyTemplates.size >= 10)
    }

    @Test
    fun `metinde duz yazilmis NPC adi yok`() {
        val named = com.yukselis.engine.content.contentWarnings(content).filter { "düz yazılmış" in it }
        assertTrue(named.joinToString("\n"), named.isEmpty())
    }

    @Test
    fun `lider fraksiyon ve anlati sezgisi`() {
        val s = engine.newGame("T", GameMode.NORMAL, 1).copy(reputation = mapOf("siginak" to 20, "lonca" to 30, "yukselenler" to 30))
        val top = com.yukselis.engine.story.Condition.TopReputation("lonca", listOf("lonca", "sessiz_ag", "siginak", "yukselenler"))
        assertTrue(top.test(s))
        assertTrue(!com.yukselis.engine.story.Condition.TopReputation("siginak", listOf("lonca", "siginak")).test(s))
        // Aktarım sahnesinde el tutulduysa anlatı satırı paragraf olarak, Yankı rengi olmadan gelir.
        val t = transfer(engine, engine.newGame("T", GameMode.NORMAL, 1), "el_tut")
        val scene = engine.view(engine.choose(engine.newGame("T", GameMode.NORMAL, 1), "el_tut", lang).state, Lang.EN)
        assertTrue(scene.current.paragraphs.any { "You didn't let go" in it })
        assertTrue(scene.current.insights.none { "You didn't let go" in it.text })
        assertTrue(t.hasFlag("yanki_bagi"))
    }

    // ---- 500 başsız oynanış ----

    /** Oynanış tarzı: seçimleri ve gün akışı eylemlerini yönlendirir (kapsama için çeşitlilik). */
    private enum class Style { RANDOM, REVEAL, CONCEAL, MERCY, CRUEL }

    private data class Run(val end: WorldState, val reached: Set<String>)

    private fun play(seed: Long): Run {
        val rng = Random(seed)
        val style = Style.entries[(seed % Style.entries.size).toInt()]
        var s = engine.newGame("T", if (seed % 7 == 3L) GameMode.IRONMAN else GameMode.NORMAL, seed)
        val reached = mutableSetOf<String>()
        var steps = 0
        while (!s.ended && steps++ < STEP_LIMIT) {
            s.dayState.currentEvent?.let { reached += it }
            if (s.dead) break
            // Serbest puanları rastgele dağıt: stat koşullu seçenekler de denensin.
            if (s.player.freePoints > 0 && s.combat == null) {
                val stat = Stat.entries[rng.nextInt(Stat.entries.size)]
                s = engine.allocate(s, mapOf(stat to s.player.freePoints), lang).state
            }
            s = when {
                s.prompts.isNotEmpty() -> {
                    val p = engine.view(s, lang).prompt!!
                    engine.resolvePrompt(s, p.options[rng.nextInt(p.options.size)].id, lang, p.kind, p.subject).state
                }
                s.combat != null -> engine.combatAction(s, engine.autoCombatAction(s), lang).state
                s.sceneId == WorldState.HUB -> engine.hubAction(s, hubAction(s, rng), lang).state
                else -> engine.choose(s, pickChoice(s, rng, style), lang).state
            }
        }
        s.dayState.currentEvent?.let { reached += it }
        assertTrue("seed $seed: oynanış bitmedi (Gün ${s.day}, ${s.sceneId})", s.ended || s.dead)
        return Run(s, reached + s.dayState.seen)
    }

    private fun hubAction(s: WorldState, rng: Random): HubAction {
        val hub = engine.view(s, lang).hub!!
        if (hub.mustCamp) return HubAction.Camp
        val r = rng.nextDouble()
        return when {
            hub.cards.isNotEmpty() && r < 0.55 -> HubAction.Card(hub.cards[rng.nextInt(hub.cards.size)].id)
            hub.travel.isNotEmpty() && r < 0.70 -> HubAction.Travel(hub.travel[rng.nextInt(hub.travel.size)].id)
            hub.gates.isNotEmpty() && r < 0.75 -> HubAction.EnterGate(hub.gates.first().uid)
            r < 0.88 -> HubAction.Explore
            r < 0.95 -> HubAction.Train
            else -> HubAction.Rest
        }
    }

    private fun pickChoice(s: WorldState, rng: Random, style: Style): String {
        val views = engine.view(s, lang).choices.filter { it.enabled }
        assertTrue("çıkmaz: ${s.sceneId} (Gün ${s.day})", views.isNotEmpty())
        val defs = content.scenes.getValue(s.sceneId).choices.associateBy { it.id }
        fun score(id: String): Double {
            val c = defs[id] ?: return 1.0
            val moral = c.effects.filterIsInstance<Effect.ShiftMorality>().sumOf { if (it.axis == com.yukselis.engine.core.MoralAxis.MERCY) it.delta else 0 }
            return when (style) {
                Style.RANDOM -> 1.0
                Style.REVEAL -> if (c.kind == ChoiceKind.REVEAL || c.kind == ChoiceKind.FLEX) 6.0 else 1.0
                Style.CONCEAL -> if (c.kind == ChoiceKind.CONCEAL) 6.0 else 1.0
                Style.MERCY -> if (moral > 0) 4.0 else 1.0
                Style.CRUEL -> if (moral < 0) 4.0 else 1.0
            }
        }
        val weights = views.map { score(it.id) }
        var r = rng.nextDouble() * weights.sum()
        for ((i, v) in views.withIndex()) {
            r -= weights[i]
            if (r <= 0) return v.id
        }
        return views.last().id
    }

    @Test
    fun `500 basliksiz oynanis cikmazsiz biter ve tum perde 1 olaylarina ulasilir`() {
        val runs = (1L..RUNS).map { play(it) }
        val reached = runs.flatMap { it.reached }.toSet()
        val missing = act1.map { it.id }.filter { it !in reached }.sorted()
        assertTrue("Hiç ulaşılmayan Perde 1 olayları: $missing", missing.isEmpty())

        // Perde 1 sonunda belirgin farklı dünya durumları: hayatta kalan oranı, Deniz'in akıbeti, ilk fraksiyon eğilimi.
        val finals = runs.map { it.end }.filter { it.sceneId == "perde1_son" || it.sceneId == "perde1_son_yenilgi" }
        assertTrue("Perde 1 sonuna ulaşan oynanış az: ${finals.size}", finals.size >= RUNS / 4)
        val survival = finals.map { val v = it.variable("hayatta_kalan"); if (v <= 35) "dusuk" else if (v >= 65) "yuksek" else "orta" }.toSet()
        val deniz = finals.map { s ->
            val d = s.npcs["deniz_arman"]
            when {
                d?.alive == false -> "oldu"
                s.hasFlag("deniz_yukselenlere_gitti") -> "yukselenler"
                d?.companion == true -> "yoldas"
                else -> "yok"
            }
        }.toSet()
        val faction = finals.map { s ->
            listOf("siginak", "lonca", "yukselenler", "sessiz_ag").maxByOrNull { s.reputation[it] ?: 0 }!!
                .takeIf { (s.reputation[it] ?: 0) >= 15 } ?: "yalniz"
        }.toSet()
        assertTrue("hayatta kalan: $survival", survival.size >= 2)
        assertTrue("Deniz: $deniz", deniz.size >= 3)
        assertTrue("fraksiyon: $faction", faction.size >= 3)
    }

    companion object {
        const val RUNS = 500L
        const val STEP_LIMIT = 4000
    }
}
