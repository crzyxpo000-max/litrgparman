package com.yukselis.simulator

import com.yukselis.engine.combat.CombatAction
import com.yukselis.engine.content.ContentLoader
import com.yukselis.engine.content.DirectoryContentSource
import com.yukselis.engine.core.GameMode
import com.yukselis.engine.core.MoralAxis
import com.yukselis.engine.core.PromptKind
import com.yukselis.engine.core.WorldState
import com.yukselis.engine.i18n.Lang
import com.yukselis.engine.npc.RelAxis
import com.yukselis.engine.story.ChoiceDef
import com.yukselis.engine.story.Content
import com.yukselis.engine.story.Effect
import com.yukselis.engine.story.StoryEngine
import com.yukselis.engine.system.Progression
import com.yukselis.engine.system.Stat
import com.yukselis.engine.world.HubAction
import com.yukselis.engine.world.QuestStatus
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

/**
 * Denge simülatörü (Aşama 9, GDD: Simülatör). Engine'i doğrudan kullanır; rastgele ve sezgisel stratejilerle
 * oynanışları baştan sona (Gün 0 → son) oynatır ve `docs/BALANCE.md` raporunu yazar.
 *
 * Kullanım: `./gradlew :tools:simulator:run --args="1000"` (isteğe bağlı ikinci argüman: iş parçacığı sayısı).
 */
fun main(args: Array<String>) {
    val runs = args.getOrNull(0)?.toIntOrNull() ?: 1000
    val threads = args.getOrNull(1)?.toIntOrNull() ?: Runtime.getRuntime().availableProcessors().coerceAtMost(8)
    val content = ContentLoader.load(DirectoryContentSource(File("content")))
    val sim = Simulator(content)
    val done = AtomicInteger()
    val pool = Executors.newFixedThreadPool(threads)
    val started = System.currentTimeMillis()
    val futures = (0 until runs).map { i ->
        pool.submit<RunResult> {
            val r = sim.play(seed = 10_000L + i, profile = Simulator.populationProfile(i))
            val n = done.incrementAndGet()
            if (n % 50 == 0) println("  $n/$runs oynanış (${(System.currentTimeMillis() - started) / 1000} sn)")
            r
        }
    }
    val results = futures.map { it.get() }
    pool.shutdown()
    val report = Report.render(results, content, (System.currentTimeMillis() - started) / 1000)
    File("docs/BALANCE.md").writeText(report, Charsets.UTF_8)
    println(report.lines().take(40).joinToString("\n"))
    println("Rapor: docs/BALANCE.md")
}

/** Bir oyun tarzı: seçimleri ve gün akışını belirli bir sona doğru iter (null = rastgele). */
data class Profile(
    val name: String,
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
    /** Kararsızlık: seçimlerin bu oranı rastgele (gerçek oyuncu gürültüsü). */
    val noise: Double = 0.25,
    val ironman: Boolean = false,
)

data class RunResult(
    val seed: Long,
    val profile: String,
    val ending: String?,
    val finished: Boolean,
    val day: Int,
    val level: Int,
    val deaths: Int,
    val ironman: Boolean,
    /** Gün → seviye (her 30 günde bir). */
    val levels: Map<Int, Int>,
    /** Gün → Dikkat. */
    val attention: Map<Int, Int>,
    /** Perde sonu → ana NPC'lerin ortalama seviyesi ve en güçlüsü. */
    val npcMean: Map<Int, Double>,
    val npcMax: Map<Int, Int>,
    /** Gün 31+'de oyuncunun dünya seviyesinin üstünde olduğu günlerin oranı. */
    val dominance: Double,
    val crushShare: Double,
    val towerFloor: Int,
    val events: Int,
    /** Ölüm nedenleri: "kaynak:düşman" (kaynak: kesif/kapi/kule/hikaye). */
    val deathCauses: List<String> = emptyList(),
    /** Ölüm anındaki oyuncu seviyesi − düşman seviyesi. */
    val deathGaps: List<Int> = emptyList(),
    /** Bitmediyse son sahne ve neden. */
    val stuck: String? = null,
)

class Simulator(private val content: Content) {
    private val engine = StoryEngine(content)

    private val lang = Lang.TR

    companion object {
        const val STEP_LIMIT = 250_000
        const val DEATH_LIMIT = 80
        val CHECKPOINTS = (1..12).map { it * 30 } + 365
        val ACT_ENDS = listOf(30, 180, 330, 365)
        val MAIN_NPCS = listOf("deniz_arman", "kaya_demir", "elif_sari", "vahit", "aras_kurt", "mert_kilic")

        val PROFILES = listOf(
            Profile(
                "koruyucu", "koruyucu", moral = mapOf(MoralAxis.MERCY to 1.0, MoralAxis.HUMANITY to 0.4), reps = mapOf("siginak" to 1.5, "kadimler" to -0.5),
                wantFlags = setOf("kadimler_yenildi", "uye_siginak", "cekirdek_siginak", "kordon_kurtarildi"), avoidFlags = setOf("kiyikent_dustu"), quests = setOf("zincir_siginak"),
                vars = mapOf("hayatta_kalan" to 1.5, "kadim_tehdit" to -0.5), protect = setOf("elif_sari"),
            ),
            Profile(
                "demir_taht", "demir_taht", moral = mapOf(MoralAxis.ORDER to 1.0, MoralAxis.HUMANITY to -0.8), reps = mapOf("lonca" to 1.5),
                wantFlags = setOf("lonca_hakimiyeti", "uye_lonca", "cekirdek_lonca", "kaya_devrildi"), quests = setOf("zincir_lonca"),
            ),
            Profile(
                "yukselis", "yukselis", moral = mapOf(MoralAxis.HUMANITY to -1.5), reps = mapOf("yukselenler" to 1.5),
                wantFlags = setOf("uye_yukselenler", "cekirdek_yukselenler", "vahit_hakli"), quests = setOf("zincir_yukselenler"), protect = setOf("vahit"),
            ),
            Profile(
                "kod_kirici", "kod_kirici", reps = mapOf("sessiz_ag" to 1.5), relations = mapOf("yanki" to mapOf(RelAxis.TRUST to 1.5)),
                wantFlags = setOf("uye_sessiz_ag", "cekirdek_sessiz_ag"), avoidFlags = setOf("mert_kadimlere_satti", "yanki_silindi"),
                quests = setOf("zincir_sessiz_ag"), protect = setOf("mert_kilic"),
            ),
            Profile(
                "ozgurluk", "ozgurluk", moral = mapOf(MoralAxis.ORDER to -1.5, MoralAxis.HUMANITY to 1.0),
                wantFlags = setOf("sistem_cekirdegine_ulasildi", "cekirdek_yolu_acik", "uye_yok"),
            ),
            Profile(
                "vassal", "vassal", reps = mapOf("kadimler" to 1.5), relations = mapOf("thessar" to mapOf(RelAxis.BOND to 1.5, RelAxis.TRUST to 0.5)),
                wantFlags = setOf("kadim_anlasmasi"), quests = setOf("zincir_kadimler"), vars = mapOf("kadim_tehdit" to 1.5), protect = setOf("thessar"),
            ),
            Profile("yalniz_avci", "yalniz_avci", loner = true, tower = 0.75),
            Profile(
                "dongu", "dongu", relations = mapOf("yanki" to mapOf(RelAxis.TRUST to 1.0)), wantFlags = setOf("yanki_son_sirri"),
                avoidFlags = setOf("yanki_silindi"), tower = 0.85, previousGames = 1,
            ),
        )
        val RANDOM = Profile("rastgele", null, noise = 1.0)

        /**
         * Oyuncu nüfusu: %30 rastgele, %70 sekiz hedefli tarz (gürültülü). Her 10. oynanış Demir Mod'dur.
         * Dağılım sınırı (%1–%40) bu karışım üzerinde ölçülür.
         */
        fun populationProfile(i: Int): Profile {
            val base = if (i % 10 < 3) RANDOM else PROFILES[(i * 7 + i / 10) % PROFILES.size]
            return base.copy(ironman = i % 10 == 7)
        }
    }

    fun play(seed: Long, profile: Profile, onCombatStart: ((WorldState) -> Unit)? = null): RunResult {
        val rng = Random(seed)
        val p = profile.takeIf { it.ending != null }
        var s = engine.newGame("Sim", if (profile.ironman) GameMode.IRONMAN else GameMode.NORMAL, seed, previousGames = profile.previousGames)
        var checkpoint: WorldState? = null
        var deaths = 0
        val causes = mutableListOf<String>()
        val gaps = mutableListOf<Int>()
        var fightSource = "?"
        var lastDeathDay = -10
        var steps = 0
        val levels = sortedMapOf<Int, Int>()
        val attention = sortedMapOf<Int, Int>()
        val npcMean = sortedMapOf<Int, Double>()
        val npcMax = sortedMapOf<Int, Int>()
        var daysAhead = 0
        var daysCounted = 0
        var lastDay = -1
        var fights = 0
        var crushes = 0
        while (!s.ended && steps++ < STEP_LIMIT) {
            if (s.day != lastDay) {
                lastDay = s.day
                if (s.day > 30) {
                    daysCounted++
                    if (s.player.level >= Progression.worldLevel(s.day)) daysAhead++
                }
                CHECKPOINTS.filter { it <= s.day && it !in levels }.forEach { d ->
                    levels[d] = s.player.level
                    attention[d] = s.attention
                }
                ACT_ENDS.filter { it <= s.day && it !in npcMean }.forEach { d ->
                    val lv = MAIN_NPCS.mapNotNull { id -> s.npcs[id]?.takeIf { it.alive }?.level }
                    npcMean[d] = if (lv.isEmpty()) 0.0 else lv.average()
                    npcMax[d] = lv.maxOrNull() ?: 0
                }
            }
            if (s.dead) {
                val foes = s.combat?.enemies?.map { it.id } ?: emptyList()
                val top = foes.mapNotNull { engine.content.enemy(it) }.maxByOrNull { it.level }
                causes += fightSource + ":" + (top?.id?.substringBefore('@') ?: "?")
                top?.let { gaps += s.player.level - it.level }
                deaths++
                lastDeathDay = s.day
                if (deaths >= DEATH_LIMIT) break
                s = checkpoint ?: break
                checkpoint = null
                continue
            }
            if (s.player.freePoints > 0 && s.combat == null) {
                s = engine.allocate(s, mapOf(allocationStat(s, rng, p) to s.player.freePoints), lang).state
            }
            val before = s
            s = when {
                s.prompts.isNotEmpty() -> {
                    val pv = engine.view(s, lang).prompt!!
                    val opt = pv.options.firstOrNull { it.id != PromptKind.DECLINE } ?: pv.options.first()
                    engine.resolvePrompt(s, (if (rng.nextDouble() < 0.7) opt else pv.options[rng.nextInt(pv.options.size)]).id, lang, pv.kind, pv.subject).state
                }
                s.combat != null -> {
                    val c = s.combat!!
                    val flee = c.spec.flee == WorldState.HUB && s.player.hp * 100 < s.player.maxHp * 35
                    val action = if (flee) CombatAction.Flee else engine.autoCombatAction(s)
                    runCatching { engine.combatAction(s, action, lang).state }.getOrElse { engine.combatAction(s, engine.autoCombatAction(s), lang).state }
                }
                s.sceneId == WorldState.HUB -> engine.hubAction(s, hubAction(s, rng, p, s.day - lastDeathDay < 4), lang).state
                else -> engine.choose(s, pickChoice(s, rng, profile), lang).state
            }
            if (before.combat == null && s.combat != null) {
                onCombatStart?.invoke(s)
                checkpoint = before
                fightSource = when {
                    before.sceneId != WorldState.HUB -> "hikaye:" + (before.dayState.currentEvent ?: before.sceneId)
                    s.variable("_bekleyen_kat") > 0 -> "kule"
                    s.variable("_bekleyen_kapi") > 0 -> "kapi"
                    else -> "kesif"
                }
            }
            // Kazanılan her savaş öldürme sayacını artırır; o savaşın Ezme olup olmadığı bağlam değişkeninde.
            if (s.variable("oldurme_sayisi") > before.variable("oldurme_sayisi") && s.combat == null) {
                fights++
                if (s.variable("_savas_ezme") == 1) crushes++
            }
        }
        val lv = MAIN_NPCS.mapNotNull { id -> s.npcs[id]?.takeIf { it.alive }?.level }
        ACT_ENDS.filter { it !in npcMean }.forEach { d -> npcMean[d] = if (lv.isEmpty()) 0.0 else lv.average(); npcMax[d] = lv.maxOrNull() ?: 0 }
        CHECKPOINTS.filter { it !in levels }.forEach { d -> levels[d] = s.player.level; attention[d] = s.attention }
        return RunResult(
            seed = seed, profile = profile.name + if (profile.ironman) " (Demir)" else "", ending = engine.endingOf(s)?.id, finished = s.ended,
            day = s.day, level = s.player.level, deaths = deaths, ironman = profile.ironman, levels = levels, attention = attention,
            npcMean = npcMean, npcMax = npcMax,
            dominance = if (daysCounted == 0) 0.0 else daysAhead.toDouble() / daysCounted,
            crushShare = if (fights == 0) 0.0 else crushes.toDouble() / fights,
            towerFloor = s.variable("kule_kat"), events = s.dayState.seen.size,
            deathCauses = causes, deathGaps = gaps,
            stuck = if (s.ended) null else "sahne ${s.sceneId}, olay ${s.dayState.currentEvent}, gün ${s.day}, adım $steps, ölüm $deaths, savaş ${s.combat?.enemies?.map { it.id }}",
        )
    }

    /** Serbest puan: hedefli oyuncu iki ana stata yatırır, rastgele oyuncu dağıtır. */
    private fun allocationStat(s: WorldState, rng: Random, p: Profile?): Stat {
        if (p == null || rng.nextDouble() < 0.3) return Stat.entries[rng.nextInt(Stat.entries.size)]
        val st = s.player.stats
        // %60 güçlü statlara, %40 savaşta açık veren zayıf savunma/isabet statına (gerçek oyuncu dengesi).
        if (rng.nextDouble() < 0.4) return listOf(Stat.PER, Stat.AGI, Stat.WIS, Stat.END).minBy { st[it] }
        return listOf(Stat.STR, Stat.END, Stat.AGI, Stat.PER, Stat.INT, Stat.WIL).sortedByDescending { st[it] }.take(3)[rng.nextInt(3)]
    }

    private fun ChoiceDef.allEffects() = effects + (check?.failEffects ?: emptyList())

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

    private fun choiceScore(c: ChoiceDef, p: Profile, depth: Int = 2): Double {
        c.next?.let { n -> content.endings.forScene(n)?.let { return if (it.id == p.ending) 1e6 else -1e3 } }
        val own = c.effects.sumOf { effectScore(it, p) }
        val ahead = if (depth <= 0) 0.0 else c.next?.let { content.scenes[it] }?.choices
            ?.maxOfOrNull { choiceScore(it, p, depth - 1) }?.coerceAtLeast(0.0) ?: 0.0
        return own + 0.9 * ahead
    }

    private fun pickChoice(s: WorldState, rng: Random, profile: Profile): String {
        val views = engine.view(s, lang).choices.filter { it.enabled }
        check(views.isNotEmpty()) { "çıkmaz: ${s.sceneId} (Gün ${s.day})" }
        val defs = content.scenes.getValue(s.sceneId).choices.associateBy { it.id }
        val p = profile.takeIf { it.ending != null }
        if (p == null || rng.nextDouble() < profile.noise) return views[rng.nextInt(views.size)].id
        return views.maxBy { v -> (defs[v.id]?.let { choiceScore(it, p) } ?: 0.0) + rng.nextDouble() * 0.5 }.id
    }

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

    private val cardCache = java.util.concurrent.ConcurrentHashMap<Pair<String, String>, Double>()

    private fun cardScore(eventId: String, p: Profile?): Double {
        if (p == null) return 0.0
        return cardCache.getOrPut(eventId to p.name) {
            val e = content.events[eventId] ?: return@getOrPut 0.0
            eventScenes(e.entry).flatMap { content.scenes[it]?.choices ?: emptyList() }.maxOfOrNull { choiceScore(it, p) } ?: 0.0
        }
    }

    private fun hubAction(s: WorldState, rng: Random, p: Profile?, cautious: Boolean): HubAction {
        val hub = engine.view(s, lang).hub!!
        if (hub.mustCamp) return HubAction.Camp
        val pl = s.player
        if (pl.hp < pl.maxHp * 0.45) return HubAction.Rest
        val towerWish = p?.tower ?: 0.35
        hub.tower?.takeIf { !cautious }?.let { t ->
            if (pl.level >= t.level && rng.nextDouble() < towerWish) return HubAction.ClimbTower
        }
        val tower = content.tower
        if (tower != null && s.hasFlag("kule_acik") && s.region != tower.region && rng.nextDouble() < towerWish * 0.5) {
            hub.travel.firstOrNull { it.id == tower.region }?.let { return HubAction.Travel(it.id) }
        }
        if (hub.cards.isNotEmpty()) {
            val best = hub.cards.maxBy { cardScore(it.id, p) + rng.nextDouble() }
            if (rng.nextDouble() < 0.6) return HubAction.Card(best.id)
        }
        if (cautious) return HubAction.Train
        val r = rng.nextDouble()
        return when {
            hub.travel.isNotEmpty() && r < 0.12 -> HubAction.Travel(hub.travel[rng.nextInt(hub.travel.size)].id)
            hub.gates.isNotEmpty() && r < 0.25 -> HubAction.EnterGate(hub.gates.first().uid)
            r < 0.85 -> HubAction.Explore
            else -> HubAction.Train
        }
    }
}
