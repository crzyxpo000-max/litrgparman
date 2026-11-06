package com.yukselis.engine

import com.yukselis.engine.content.ContentLoader
import com.yukselis.engine.content.DirectoryContentSource
import com.yukselis.engine.core.GameMode
import com.yukselis.engine.core.MoralAxis
import com.yukselis.engine.core.WorldState
import com.yukselis.engine.i18n.Lang
import com.yukselis.engine.npc.RelAxis
import com.yukselis.engine.story.ChoiceDef
import com.yukselis.engine.story.Content
import com.yukselis.engine.story.Effect
import com.yukselis.engine.story.StoryEngine
import com.yukselis.engine.system.Stat
import com.yukselis.engine.world.HubAction
import com.yukselis.engine.world.QuestStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

/**
 * Aşama 7: Perde 2–4 ve sonlar. İçerik kuralları ve bütçe, fraksiyon zincirleri, NPC yayları, Kule;
 * 365 günlük başsız oynanış çıkmazsız biter; **her son en az bir başsız oynanış stratejisiyle ulaşılır** (görev dosyası).
 */
class Perde2_4Test {
    private val engine = StoryEngine(ContentLoader.load(DirectoryContentSource(File("../content"))))
    private val content: Content = engine.content
    private val lang = Lang.TR

    private val later = content.events.values.filter { e -> e.acts.any { it >= 2 } || e.id.startsWith("kule_kat_") }

    private fun eventScenes(entry: String): Set<String> {
        val seen = mutableSetOf<String>()
        val q = ArrayDeque(listOf(entry))
        while (q.isNotEmpty()) {
            val id = q.removeFirst()
            if (!seen.add(id)) continue
            content.scenes[id]?.choices?.forEach { c ->
                (listOfNotNull(c.next, c.check?.failNext) + c.allEffects().filterIsInstance<Effect.StartCombat>()
                    .flatMap { listOfNotNull(it.spec.win, it.spec.lose, it.spec.flee) })
                    .filter { it in content.scenes }.forEach { q.addLast(it) }
            }
        }
        return seen
    }

    private fun ChoiceDef.allEffects() = effects + (check?.failEffects ?: emptyList())

    // ---------------------------------------------------------------- içerik

    @Test
    fun `perde 2-4 butcesi`() {
        fun count(act: Int) = later.count { e -> e.acts.isNotEmpty() && e.acts.min() == act }
        assertTrue("Perde 2 olayı ≥ 60 (${count(2)})", count(2) >= 60)
        assertTrue("Perde 3 olayı ≥ 45 (${count(3)})", count(3) >= 45)
        assertTrue("Perde 4 olayı ≥ 25 (${count(4)})", count(4) >= 25)
        // Fraksiyon zincirleri: her biri 5 düğüm ve tamamlanabilir.
        for ((quest, prefix) in CHAINS) {
            val nodes = content.events.keys.filter { it.startsWith(prefix) && it.getOrNull(prefix.length)?.isDigit() == true }
            assertTrue("$quest düğümleri: $nodes", nodes.size >= 5)
            val completes = content.scenes.values.flatMap { it.choices }.flatMap { it.allEffects() }
                .any { it is Effect.SetQuest && it.questId == quest && it.status == QuestStatus.DONE }
            assertTrue("$quest hiçbir yerde tamamlanmıyor", completes)
        }
        // 8 ana NPC'nin tam yayı: her biri en az 3 yazılmış düğüm (Perde 1 dahil), listedekilerin hepsi var.
        for (npc in MAIN_NPCS) {
            val nodes = content.npcs.getValue(npc).arcNodes
            val missing = nodes.filter { it !in content.events }
            assertTrue("$npc eksik yay düğümleri: $missing", missing.isEmpty())
            assertTrue("$npc yay düğümü ${nodes.size}", nodes.size >= 3)
        }
        // Kule: 100 kat, her 10. kat hikaye katı.
        val t = content.tower!!
        assertEquals(100, t.floors)
        assertEquals((1..10).map { it * 10 }.toSet(), t.storyFloors.keys)
        (1..100).forEach { f -> assertTrue("kat $f", f in t.storyFloors || t.band(f) != null) }
        // Çok-evren: 3 dünya, her birinde olay.
        // Dünyanın olayları: bölgeye bağlı olaylar + onların tohumladığı (bölgesiz) devam olayları.
        for (w in WORLDS) {
            val own = content.events.values.filter { w in it.regions }
            val followUps = own.flatMap { e -> eventScenes(e.entry).flatMap { content.scenes[it]?.choices ?: emptyList() } }
                .flatMap { it.allEffects() }.filterIsInstance<Effect.PlantSeed>().map { it.eventId }
                .filter { content.events[it]?.let { f -> f.seedOnly && f.regions.isEmpty() } == true }.toSet()
            assertTrue("$w olayı: ${own.size} + ${followUps.size}", own.size + followUps.size >= 4)
        }
        // Sonlar: 8 + gizli Döngü.
        assertEquals(ENDINGS.toSet(), content.endings.endings.keys)
        assertTrue(content.endings.endings.getValue("dongu").hidden)
        assertTrue(content.endings.endings.getValue("hasat").fallback)
    }

    @Test
    fun `perde 2-4 olay kurallari`() {
        val problems = mutableListOf<String>()
        for (e in later) {
            val entry = content.scenes.getValue(e.entry)
            if (entry.choices.size < 3 && e.id !in RULE_EXEMPT) problems += "${e.id}: giriş sahnesinde ${entry.choices.size} seçenek (en az 3)"
            val scenes = eventScenes(e.entry)
            val choices = scenes.flatMap { content.scenes[it]?.choices ?: emptyList() }
            // Tohumla gelen devam olayları zaten bir sonucun kendisidir: koşullu ve tohum kuralından muaf.
            // Perde 4'te başlayan olayların tohumu olgunlaşacak zaman bulamaz: tohum kuralından muaf (DECISIONS #111).
            val followUp = e.seedOnly && !e.id.startsWith("kule_kat_")
            val late = (e.mainArcDay ?: e.dayMin) >= 331
            if (!followUp && choices.none { it.requires != null || it.showWhen != null }) problems += "${e.id}: koşullu seçenek yok"
            if (e.id !in RULE_EXEMPT) {
                if (!followUp && !late && choices.none { c -> c.allEffects().any { it is Effect.PlantSeed } }) problems += "${e.id}: tohum eken seçenek yok"
                if (choices.none { c -> c.allEffects().any { it is Effect.ShiftRelation } }) problems += "${e.id}: ilişki eksenini değiştiren seçenek yok"
            }
            if (e.important) {
                scenes.mapNotNull { content.scenes[it] }.forEach { sc ->
                    sc.choices.filter { it.next != null && it.chronicle == null && sc.id == e.entry }.forEach {
                        problems += "${e.id}.${it.id}: önemli olayın giriş seçeneğinde `kronik` yok"
                    }
                }
            }
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun `son karar her sonu sunar`() {
        val decision = content.events.getValue("son_karar")
        val targets = eventScenes(decision.entry).flatMap { content.scenes[it]?.choices ?: emptyList() }
            .mapNotNull { it.next }.mapNotNull { content.endings.forScene(it)?.id }.toSet()
        assertEquals(ENDINGS.toSet(), targets)
    }

    // ---------------------------------------------------------------- başsız oynanış

    /** Bir oyun tarzı: seçimleri ve gün akışını belirli bir sona doğru iter. */
    private data class Profile(
        val ending: String?,
        val moral: Map<MoralAxis, Double> = emptyMap(),
        val reps: Map<String, Double> = emptyMap(),
        val wantFlags: Set<String> = emptySet(),
        val avoidFlags: Set<String> = emptySet(),
        val quests: Set<String> = emptySet(),
        val protect: Set<String> = emptySet(),
        val relations: Map<String, Map<RelAxis, Double>> = emptyMap(),
        val vars: Map<String, Double> = emptyMap(),
        val loner: Boolean = false,
        val tower: Double = 0.35,
        val previousGames: Int = 0,
        val drifting: Boolean = false,
    )

    private val profiles = listOf(
        Profile(
            "koruyucu", moral = mapOf(MoralAxis.MERCY to 1.0, MoralAxis.HUMANITY to 0.4), reps = mapOf("siginak" to 1.5, "kadimler" to -0.5),
            wantFlags = setOf("kadimler_yenildi", "uye_siginak", "cekirdek_siginak", "kordon_kurtarildi"), avoidFlags = setOf("kiyikent_dustu"), quests = setOf("zincir_siginak"),
            vars = mapOf("hayatta_kalan" to 1.5, "kadim_tehdit" to -0.5), protect = setOf("elif_sari"),
        ),
        Profile(
            "demir_taht", moral = mapOf(MoralAxis.ORDER to 1.0, MoralAxis.HUMANITY to -0.8), reps = mapOf("lonca" to 1.5),
            wantFlags = setOf("lonca_hakimiyeti", "uye_lonca", "cekirdek_lonca", "kaya_devrildi"), quests = setOf("zincir_lonca"),
        ),
        Profile(
            "yukselis", moral = mapOf(MoralAxis.HUMANITY to -1.5), reps = mapOf("yukselenler" to 1.5),
            wantFlags = setOf("uye_yukselenler", "cekirdek_yukselenler", "vahit_hakli"), quests = setOf("zincir_yukselenler"), protect = setOf("vahit"),
        ),
        Profile(
            "kod_kirici", reps = mapOf("sessiz_ag" to 1.5), relations = mapOf("yanki" to mapOf(RelAxis.TRUST to 1.5)),
            wantFlags = setOf("uye_sessiz_ag", "cekirdek_sessiz_ag"), avoidFlags = setOf("mert_kadimlere_satti", "yanki_silindi"),
            quests = setOf("zincir_sessiz_ag"), protect = setOf("mert_kilic"),
        ),
        Profile(
            "ozgurluk", moral = mapOf(MoralAxis.ORDER to -1.5, MoralAxis.HUMANITY to 1.0),
            wantFlags = setOf("sistem_cekirdegine_ulasildi", "cekirdek_yolu_acik", "uye_yok"),
        ),
        Profile(
            "vassal", reps = mapOf("kadimler" to 1.5), relations = mapOf("thessar" to mapOf(RelAxis.BOND to 1.5, RelAxis.TRUST to 0.5)),
            wantFlags = setOf("kadim_anlasmasi"), quests = setOf("zincir_kadimler"), vars = mapOf("kadim_tehdit" to 1.5), protect = setOf("thessar"),
        ),
        Profile("yalniz_avci", loner = true, tower = 0.75),
        Profile(
            "dongu", relations = mapOf("yanki" to mapOf(RelAxis.TRUST to 1.0)), wantFlags = setOf("yanki_son_sirri"),
            avoidFlags = setOf("yanki_silindi"), tower = 0.85, previousGames = 1,
        ),
        // Hasat: hiçbir yola bağlanmayan, kararsız (rastgele) bir oyun; son karar yalnızca Hasat'ı sunar.
        Profile("hasat", drifting = true),
    )

    private data class Run(val end: WorldState, val ending: String?, val reached: Set<String>)

    private fun play(seed: Long, profile: Profile?): Run {
        val rng = Random(seed)
        var s = engine.newGame("T", GameMode.NORMAL, seed, previousGames = profile?.previousGames ?: 0)
        val reached = mutableSetOf<String>()
        var checkpoint: WorldState? = null
        var deaths = 0
        var lastDeathDay = -10
        var steps = 0
        while (!s.ended && steps++ < STEP_LIMIT) {
            s.dayState.currentEvent?.let { reached += it }
            // Normal mod: ölüm savaş başındaki kayıt noktasına döndürür (arayüzdeki gibi); sonra başka bir şey dene.
            if (s.dead) {
                deaths++
                lastDeathDay = s.day
                assertTrue("seed $seed (${profile?.ending}): sürekli ölüm (Gün ${s.day}, sahne ${checkpoint?.sceneId}, olay ${checkpoint?.dayState?.currentEvent}, düşman ${s.combat?.enemies?.map { it.id }}, sv ${s.player.level})", deaths < DEATH_LIMIT)
                s = checkpoint ?: break
                checkpoint = null
                if (s.sceneId == WorldState.HUB && !engine.view(s, lang).hub!!.mustCamp) {
                    s = engine.hubAction(s, HubAction.Rest, lang).state
                    continue
                }
            }
            if (s.player.freePoints > 0 && s.combat == null) {
                // Hedefli oyuncu güçlü statlarına yatırır (simülatörle aynı); rastgele oyuncu dağıtır.
                val st = s.player.stats
                val stat = if (profile == null || profile.drifting || rng.nextDouble() < 0.3) Stat.entries[rng.nextInt(Stat.entries.size)]
                else if (rng.nextDouble() < 0.4) listOf(Stat.PER, Stat.AGI, Stat.WIS, Stat.END).minBy { st[it] }
                else listOf(Stat.STR, Stat.END, Stat.AGI, Stat.PER, Stat.INT, Stat.WIL).sortedByDescending { st[it] }.take(3)[rng.nextInt(3)]
                s = engine.allocate(s, mapOf(stat to s.player.freePoints), lang).state
            }
            val before = s
            s = when {
                s.prompts.isNotEmpty() -> {
                    val p = engine.view(s, lang).prompt!!
                    val opt = p.options.firstOrNull { it.id != com.yukselis.engine.core.PromptKind.DECLINE } ?: p.options.first()
                    engine.resolvePrompt(s, (if (rng.nextDouble() < 0.7) opt else p.options[rng.nextInt(p.options.size)]).id, lang, p.kind, p.subject).state
                }
                // Normal oyuncu gibi: gün akışından açılan (kaçılabilir) savaşta can %25'in altına inince kaç.
                s.combat != null -> {
                    val c = s.combat!!
                    val flee = c.spec.flee == WorldState.HUB && s.player.hp < s.player.maxHp / 4
                    val action = if (flee) com.yukselis.engine.combat.CombatAction.Flee else engine.autoCombatAction(s)
                    runCatching { engine.combatAction(s, action, lang).state }.getOrElse { engine.combatAction(s, engine.autoCombatAction(s), lang).state }
                }
                s.sceneId == WorldState.HUB -> engine.hubAction(s, hubAction(s, rng, profile, cautious = s.day - lastDeathDay < 4), lang).state
                else -> engine.choose(s, pickChoice(s, rng, profile), lang).state
            }
            if (before.combat == null && s.combat != null) checkpoint = before
        }
        s.dayState.currentEvent?.let { reached += it }
        assertTrue("seed $seed (${profile?.ending}): oyun bitmedi (Gün ${s.day}, ${s.sceneId})", s.ended)
        return Run(s, engine.endingOf(s)?.id, reached + s.dayState.seen)
    }

    private fun effectScore(e: Effect, p: Profile): Double = when (e) {
        is Effect.ShiftMorality -> (p.moral[e.axis] ?: 0.0) * e.delta
        is Effect.ShiftReputation -> (p.reps[e.faction] ?: 0.0) * e.delta
        is Effect.SetFlag -> when (e.flag) { in p.wantFlags -> 40.0; in p.avoidFlags -> -60.0; else -> 0.0 }
        is Effect.SetQuest -> if (e.questId in p.quests) (if (e.status == QuestStatus.FAILED) -80.0 else 30.0) else 0.0
        is Effect.KillNpc -> if (e.npc in p.protect || e.npc in p.relations) -200.0 else 0.0
        is Effect.ShiftRelation -> p.relations[e.npc]?.entries?.sumOf { (axis, w) -> w * (e.deltas[axis] ?: 0) } ?: 0.0
        is Effect.AddVar -> (p.vars[e.key] ?: 0.0) * e.delta
        is Effect.JoinParty -> if (p.loner) -40.0 else 2.0
        is Effect.LeaveParty, is Effect.NpcGone -> if (p.loner) 40.0 else 0.0
        else -> 0.0
    }

    private fun choiceScore(c: ChoiceDef, p: Profile?, depth: Int = 2): Double {
        if (p == null) return 1.0
        // Hedef sonun sahnesine giden seçenek her zaman kazanır.
        c.next?.let { n -> content.endings.forScene(n)?.let { return if (it.id == p.ending) 1e6 else -1e3 } }
        val own = c.effects.sumOf { effectScore(it, p) }
        // Bir adım ileri bak: etkisi bir sonraki sahnede olan seçimler (ör. "tahta yürü" → "otur").
        val ahead = if (depth <= 0) 0.0 else c.next?.let { content.scenes[it] }?.choices
            ?.maxOfOrNull { choiceScore(it, p, depth - 1) }?.coerceAtLeast(0.0) ?: 0.0
        return own + 0.9 * ahead
    }

    private fun pickChoice(s: WorldState, rng: Random, p: Profile?): String {
        val views = engine.view(s, lang).choices.filter { it.enabled }
        assertTrue("çıkmaz: ${s.sceneId} (Gün ${s.day})", views.isNotEmpty())
        val defs = content.scenes.getValue(s.sceneId).choices.associateBy { it.id }
        if (p?.drifting == true) {
            views.firstOrNull { v -> defs[v.id]?.next?.let { content.endings.forScene(it)?.id } == p.ending }?.let { return it.id }
        }
        if (p == null || p.drifting || rng.nextDouble() < 0.1) return views[rng.nextInt(views.size)].id
        return views.maxBy { v -> (defs[v.id]?.let { choiceScore(it, p) } ?: 0.0) + rng.nextDouble() * 0.5 }.id
    }

    private fun cardScore(eventId: String, p: Profile?): Double {
        if (p == null) return 0.0
        val e = content.events[eventId] ?: return 0.0
        return eventScenes(e.entry).flatMap { content.scenes[it]?.choices ?: emptyList() }.maxOfOrNull { choiceScore(it, p) } ?: 0.0
    }

    private fun hubAction(s: WorldState, rng: Random, p: Profile?, cautious: Boolean = false): HubAction {
        val hub = engine.view(s, lang).hub!!
        if (hub.mustCamp) return HubAction.Camp
        val pl = s.player
        if (pl.hp < pl.maxHp * 0.45) return HubAction.Rest
        hub.tower?.takeIf { !cautious }?.let { t ->
            if (pl.level >= t.level - 2 && rng.nextDouble() < (p?.tower ?: 0.35)) return HubAction.ClimbTower
        }
        // Kule'nin bölgesine yönel (kule açık ve tırmanmak istiyorsa).
        if (content.tower != null && s.hasFlag("kule_acik") && s.region != content.tower!!.region && rng.nextDouble() < (p?.tower ?: 0.35) * 0.5) {
            hub.travel.firstOrNull { it.id == content.tower!!.region }?.let { return HubAction.Travel(it.id) }
        }
        if (hub.cards.isNotEmpty()) {
            val best = hub.cards.maxBy { cardScore(it.id, p) + rng.nextDouble() }
            if (rng.nextDouble() < 0.6) return HubAction.Card(best.id)
        }
        // Yeni öldüyse toparlan: keşif ve kapı yerine antrenman.
        if (cautious) return HubAction.Train
        val r = rng.nextDouble()
        return when {
            hub.travel.isNotEmpty() && r < 0.12 -> HubAction.Travel(hub.travel[rng.nextInt(hub.travel.size)].id)
            hub.gates.isNotEmpty() && r < 0.25 -> HubAction.EnterGate(hub.gates.first().uid)
            r < 0.85 -> HubAction.Explore
            else -> HubAction.Train
        }
    }

    @Test
    fun `365 gunluk basliksiz oynanis cikmazsiz biter`() {
        val runs = (1L..FULL_RUNS).map { play(1000 + it, null) }
        val endings = runs.mapNotNull { it.ending }.groupingBy { it }.eachCount()
        println("Rastgele oynanış sonları: $endings")
        assertTrue(runs.all { it.end.ended })
        val reached = runs.flatMap { it.reached }.toSet()
        println("Perde 2–4 olay kapsamı: ${later.count { it.id in reached }}/${later.size}")
    }

    @Test
    fun `her son en az bir stratejiyle ulasilir`() {
        val unreached = mutableListOf<String>()
        for (p in profiles) {
            val hit = (1L..TRIES).firstOrNull { seed ->
                val r = play(seed * 7919 + p.ending.hashCode(), p)
                if (r.ending != p.ending) {
                    val e = r.end
                    println("  ${p.ending} deneme $seed → ${r.ending}: merhamet ${e.morality.mercy} düzen ${e.morality.order} insanlık ${e.morality.humanity} " +
                        "itibar ${e.reputation} kademe ${e.player.grade} yoldaş ${e.npcs.values.count { it.companion && it.alive }} kule ${e.variable("kule_kat")} " +
                        "tehdit ${e.variable("kadim_tehdit")} hayatta ${e.variable("hayatta_kalan")} görevler ${e.quests.filterKeys { it.startsWith("zincir_") }} " +
                        "bayraklar ${e.flags.filter { it in setOf("lonca_hakimiyeti", "kaya_devrildi", "kadimler_yenildi", "kadim_anlasmasi", "sistem_cekirdegine_ulasildi", "yanki_son_sirri", "uye_lonca") }}")
                }
                r.ending == p.ending
            }
            println("Son ${p.ending}: ${if (hit != null) "ulaşıldı (deneme $hit)" else "ULAŞILAMADI"}")
            if (hit == null) unreached += p.ending!!
        }
        assertTrue("Ulaşılamayan sonlar: $unreached", unreached.isEmpty())
    }

    companion object {
        const val STEP_LIMIT = 200_000
        const val DEATH_LIMIT = 40
        const val FULL_RUNS = 12L
        const val TRIES = 16L
        val ENDINGS = listOf("koruyucu", "demir_taht", "yukselis", "kod_kirici", "ozgurluk", "vassal", "yalniz_avci", "hasat", "dongu")
        val MAIN_NPCS = listOf("deniz_arman", "kaya_demir", "elif_sari", "vahit", "yanki", "aras_kurt", "mert_kilic", "thessar")
        val WORLDS = listOf("batik_takimada", "pirinc_kervan", "yedi_taht")
        val CHAINS = mapOf(
            "zincir_siginak" to "siginak_", "zincir_lonca" to "lonca_", "zincir_yukselenler" to "yukselenler_",
            "zincir_sessiz_ag" to "sessiz_ag_", "zincir_kadimler" to "kadimler_",
        )
        val RULE_EXEMPT = setOf("son_karar", "hasat_vakti")
    }
}
