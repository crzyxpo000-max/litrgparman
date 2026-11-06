package com.yukselis.engine

import com.yukselis.engine.content.ContentLoader
import com.yukselis.engine.content.DirectoryContentSource
import com.yukselis.engine.core.GameMode
import com.yukselis.engine.core.WorldState
import com.yukselis.engine.i18n.LText
import com.yukselis.engine.i18n.Lang
import com.yukselis.engine.save.SaveCodec
import com.yukselis.engine.story.Effect
import com.yukselis.engine.story.StoryEngine
import com.yukselis.engine.system.Grade
import com.yukselis.engine.system.Stat
import com.yukselis.engine.world.DiceCheck
import com.yukselis.engine.world.EventDef
import com.yukselis.engine.world.Gate
import com.yukselis.engine.world.HubAction
import com.yukselis.engine.world.QuestStatus
import com.yukselis.engine.world.Seed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

/** Aşama 4: hikaye motoru (Yönetmen, tohumlar, gün döngüsü, bölgeler, fraksiyonlar, OP MC) ve başsız oynanış. */
class WorldTest {
    private val engine = StoryEngine(ContentLoader.load(DirectoryContentSource(File("../content"))))
    private val content = engine.content
    private val director = engine.world.director
    private val lang = Lang.TR

    /** Prologu geçip gün akışına gelmiş bir oyun. */
    private fun atHub(seed: Long = 1): WorldState {
        var s = engine.newGame("T", GameMode.NORMAL, seed)
        s = s.copy(sceneId = "ilk_sabah", day = 1)
        return engine.choose(s, "gune_basla", lang).state
    }

    private fun event(id: String = "x", weight: Double = 2.0, tension: Int = 2, tags: List<String> = emptyList(), npcs: List<String> = emptyList()) =
        EventDef(id = id, weight = weight, tension = tension, tags = tags, npcs = npcs, card = LText("k", "c"), entry = "ilk_sabah")

    // ---- Yönetmen ----

    @Test
    fun `yonetmen puani gdd formulu`() {
        val s = atHub()
        val e = event(weight = 4.0, tension = 2, tags = listOf("a"), npcs = listOf("deniz_arman"))
        val expected = 4.0 * director.tensionFit(s, e) * director.freshness(s, e) * director.bond(s, e) * director.arcPriority(e)
        assertEquals(expected, director.score(s, e), 1e-9)
    }

    @Test
    fun `iki agir olaydan sonra nefes`() {
        val s = atHub().let { it.copy(dayState = it.dayState.copy(recentTension = listOf(3, 4))) }
        assertEquals(1, director.desiredTension(s))
        val calm = atHub().let { it.copy(dayState = it.dayState.copy(recentTension = listOf(1))) }
        assertEquals(2, director.desiredTension(calm))
        // Dikkat eğriyi yukarı iter.
        assertEquals(3, director.desiredTension(calm.copy(attention = 45)))
    }

    @Test
    fun `tazelik ve kisisel bag`() {
        val s = atHub().let { it.copy(dayState = it.dayState.copy(recentTags = listOf("a", "a", "b"))) }
        assertEquals(0.6 * 0.6 * 0.6, director.freshness(s, event(tags = listOf("a", "b"))), 1e-9)
        val friend = s.copy(npcs = s.npcs + ("deniz_arman" to s.npcs.getValue("deniz_arman").copy(relation = com.yukselis.engine.npc.Relation(trust = 50, bond = 50))))
        assertEquals(1.5, director.bond(friend, event(npcs = listOf("deniz_arman"))), 1e-9)
    }

    @Test
    fun `ana yay dugumu zamani gelince zorlanir`() {
        val s = atHub().copy(day = 28)
        val f = director.forced(s)
        assertEquals("ogretici_bekci", f?.eventId)
        assertFalse(director.forced(s.copy(day = 10)) ?.eventId == "ogretici_bekci")
    }

    // ---- Tohumlar ----

    @Test
    fun `tohum ekilir vadesinde kosul tutarsa olay gelir yoksa kurur`() {
        var s = atHub()
        s = engine.applyEffects(s, listOf(Effect.PlantSeed("yabanci_minnet", 2, 2, "{\"bayrak\": \"yabanci_korundu\"}")), lang).state
        val seed = s.seeds.single()
        assertEquals(s.day + 2, seed.dueDay)
        val due = s.copy(day = seed.dueDay, flags = s.flags + "yabanci_korundu")
        assertEquals("yabanci_minnet", director.forced(due)?.eventId)
        val dry = s.copy(day = seed.dueDay)
        assertEquals(listOf(seed), director.witheredSeeds(dry))
        // Prolog gerçek tohumu ekiyor.
        var p = engine.newGame("T", GameMode.NORMAL, 3).copy(sceneId = "ilk_canavar")
        p = engine.choose(p, "kac", lang).state
        assertEquals("yabanci_geri_donus", p.seeds.single().eventId)
    }

    // ---- Gün döngüsü, bölgeler, kapılar ----

    @Test
    fun `gun akisi yuvalar el ve kamp`() {
        var s = atHub()
        assertEquals(WorldState.HUB, s.sceneId)
        assertTrue(s.dayState.slotsLeft in 2..4)
        assertTrue(s.dayState.hand.size <= 3)
        assertNotNull(s.region)
        assertTrue(s.hasFlag("gunluk_genis_gorevler"))
        val day = s.day
        while (s.dayState.slotsLeft > 0 && s.sceneId == WorldState.HUB && s.combat == null) {
            s = engine.hubAction(s, HubAction.Train, lang).state
        }
        if (s.sceneId == WorldState.HUB && s.combat == null) {
            s = engine.hubAction(s, HubAction.Camp, lang).state
            assertEquals(day + 1, s.day)
            assertNotNull(s.daily)
        }
    }

    @Test
    fun `zamaninda kapatilmayan kapi tasar`() {
        var s = atHub()
        val r = s.regions.getValue("ogretici_orman")
        s = s.copy(regions = s.regions + ("ogretici_orman" to r.copy(gates = listOf(Gate(99, Grade.F, listOf("sis_tazisi"), 0, s.day - 1)))))
        val danger = s.regions.getValue("ogretici_orman").danger
        s = engine.world.startDay(s, advance = true).state
        assertTrue(s.regions.getValue("ogretici_orman").gates.none { it.uid == 99 })
        assertEquals(danger + 15, s.regions.getValue("ogretici_orman").danger.coerceAtMost(danger + 15))
        assertEquals(1, s.variable("kapi_tasma_sayisi"))
    }

    @Test
    fun `kapi kazanilinca kapanir ve ilk kapi kapatan verilir`() {
        var s = atHub()
        val r = s.regions.getValue(s.region!!)
        s = s.copy(regions = s.regions + (r.id to r.copy(gates = listOf(Gate(7, Grade.F, listOf("igne_tavsan"), s.day, s.day + 3)))))
        s = s.copy(player = s.player.copy(level = 8, stats = s.player.stats.plus(Stat.STR, 10), unlockedGrade = Grade.F))
        s = s.copy(player = s.player.copy(hp = s.player.maxHp))
        s = engine.hubAction(s, HubAction.EnterGate(7), lang).state
        var n = 0
        while (s.combat != null && n++ < 100) s = engine.combatAction(s, engine.autoCombatAction(s), lang).state
        assertTrue(s.regions.getValue(r.id).gates.none { it.uid == 7 })
        assertTrue("ilk_kapi_kapatan" in s.player.titles)
    }

    // ---- Fraksiyonlar ----

    @Test
    fun `itibar dalgasi iliski matrisine gore yayilir`() {
        val s = engine.applyEffects(atHub(), listOf(Effect.ShiftReputation("siginak", 10)), lang).state
        assertEquals(10, s.reputation["siginak"])
        assertEquals(-2, s.reputation["yukselenler"]) // 10 · −70 · 0,3 / 100
        assertEquals(0, s.reputation["sessiz_ag"] ?: 0) // 10 · 10 · 0,3 / 100 = 0,3 → 0
    }

    // ---- OP MC ----

    @Test
    fun `dikkat esigi gecince bayrak ve esik olayi`() {
        val s = engine.applyEffects(atHub(), listOf(Effect.ShiftAttention(30)), lang).state
        assertTrue(s.hasFlag("dikkat_esik_25"))
        assertTrue(s.seeds.any { it.eventId == "dikkat_25" })
        assertEquals("dikkat_25", director.forced(s)?.eventId)
    }

    @Test
    fun `aciga vurmak gorunen seviyeyi esitler ve dikkat cekere`() {
        var s = atHub()
        s = s.copy(player = s.player.copy(level = 9), publicLevel = 2)
        s = engine.applyEffects(s, listOf(Effect.Reveal), lang).state
        assertEquals(9, s.publicLevel)
        // 5 + 2 · fark; sıralamada üst sıralara çıkmak ayrıca dikkat çekebilir (ranking.yaml tepkileri).
        assertTrue(s.attention >= 5 + 2 * 7)
    }

    @Test
    fun `zar olasiligi gorunur`() {
        val s = atHub()
        assertEquals(70, engine.odds(s, DiceCheck(Stat.CHA, 12)))
        assertEquals(100, engine.odds(s, DiceCheck(Stat.CHA, 2)))
        assertEquals(0, engine.odds(s, DiceCheck(Stat.CHA, 40)))
    }

    @Test
    fun `tanikli basari soylentiye fraksiyon tepkisine ve unvana donusur`() {
        var s = atHub()
        repeat(3) { s = engine.applyEffects(s, listOf(Effect.Feat("yabancilari_iyilestirdi")), lang).state }
        assertEquals(3, s.legend["yabancilari_iyilestirdi"])
        assertTrue((s.reputation["siginak"] ?: 0) >= 20)
        assertTrue("uzanan_el" in s.player.titles)
    }

    @Test
    fun `gorev durumu`() {
        val s = engine.applyEffects(atHub(), listOf(Effect.SetQuest("deniz_bul", QuestStatus.ACTIVE)), lang).state
        assertEquals(QuestStatus.ACTIVE, s.quests["deniz_bul"])
        assertTrue(engine.journal(s, lang).quests.any { it.status == QuestStatus.ACTIVE })
    }

    @Test
    fun `kayit dunya durumunu korur`() {
        var s = atHub()
        s = s.copy(seeds = listOf(Seed("yabanci_minnet", 9, "{\"bayrak\": \"yabanci_korundu\"}")), attention = 33, corruption = 12, publicLevel = 3,
            legend = mapOf("tek_basina_suru" to 2), quests = mapOf("deniz_bul" to QuestStatus.DONE))
        assertEquals(s, SaveCodec.decode(SaveCodec.encode(s, 1)).state)
    }

    // ---- Bağımsız inceleme ----

    @Test
    fun `kapi savasindan kacmak kapiyi kapatmaz`() {
        var s = atHub().copy(vars = mapOf("_savas_kazanildi" to 1))
        val r = s.regions.getValue(s.region!!)
        s = s.copy(regions = s.regions + (r.id to r.copy(gates = listOf(Gate(5, Grade.F, listOf("diken_domuzu"), s.day, s.day + 3)))))
        s = engine.hubAction(s, HubAction.EnterGate(5), lang).state
        var n = 0
        while (s.combat != null && n++ < 20) s = engine.combatAction(s, com.yukselis.engine.combat.CombatAction.Flee, lang).state
        if (s.combat == null && s.variable("_savas_kazanildi") == 0) {
            assertTrue(s.regions.getValue(r.id).gates.any { it.uid == 5 })
        }
    }

    @Test
    fun `zorunlu olay yuva harcamaz`() {
        var s = atHub()
        val left = s.dayState.slotsLeft
        s = s.copy(seeds = listOf(Seed("yabanci_minnet", s.day)))
        s = engine.hubAction(s, HubAction.Rest, lang).state
        assertEquals("yabanci_minnet_1", s.sceneId)
        assertEquals(left - 1, s.dayState.slotsLeft)
    }

    @Test
    fun `akistan acilan savas kaydedilip yuklenince surer`() {
        var s = atHub()
        val r = s.regions.getValue(s.region!!)
        s = s.copy(regions = s.regions + (r.id to r.copy(gates = listOf(Gate(6, Grade.F, listOf("diken_domuzu"), s.day, s.day + 3)))))
        s = engine.hubAction(s, HubAction.EnterGate(6), lang).state
        val loaded = engine.sanitize(SaveCodec.decode(SaveCodec.encode(s, 1)).state)
        assertNotNull(loaded.combat)
    }

    @Test
    fun `gorulmus olayin tohumu kurur`() {
        val s = atHub().let { it.copy(seeds = listOf(Seed("yabanci_minnet", it.day)), dayState = it.dayState.copy(seen = setOf("yabanci_minnet"))) }
        assertEquals(1, director.witheredSeeds(s).size)
    }

    // ---- Başsız oynanış ----

    private data class Run(val ended: Boolean, val dead: Boolean, val day: Int, val steps: Int, val deadEnd: String?)

    private fun randomPlay(seed: Long, mode: GameMode): Run {
        val r = Random(seed)
        var s = engine.newGame("Bot", mode, seed)
        var steps = 0
        while (steps++ < 6000) {
            if (s.ended) return Run(true, false, s.day, steps, null)
            if (s.dead) return Run(false, true, s.day, steps, null)
            when {
                s.prompts.isNotEmpty() -> s = engine.resolvePrompt(s, s.prompts.first().options.random(r), lang).state
                s.combat != null -> {
                    val action = if (r.nextInt(10) == 0) com.yukselis.engine.combat.CombatAction.Defend else engine.autoCombatAction(s)
                    s = runCatching { engine.combatAction(s, action, lang).state }.getOrElse { engine.combatAction(s, engine.autoCombatAction(s), lang).state }
                }
                s.player.freePoints > 0 && r.nextInt(3) == 0 ->
                    s = engine.allocate(s, mapOf(Stat.entries.random(r) to s.player.freePoints), lang).state
                s.sceneId == WorldState.HUB -> {
                    val d = s.dayState
                    val actions: List<HubAction> = if (d.slotsLeft <= 0) {
                        listOf(HubAction.Camp)
                    } else {
                        buildList<HubAction> {
                            d.hand.forEach { add(HubAction.Card(it)) }
                            add(HubAction.Explore); add(HubAction.Train); add(HubAction.Rest)
                            s.region?.let { content.regions[it] }?.neighbors?.forEach { add(HubAction.Travel(it)) }
                            s.region?.let { s.regions[it] }?.gates?.forEach { add(HubAction.EnterGate(it.uid)) }
                        }
                    }
                    s = engine.hubAction(s, actions.random(r), lang).state
                }
                else -> {
                    val view = engine.view(s, lang)
                    val enabled = view.choices.filter { it.enabled }
                    if (enabled.isEmpty()) return Run(false, false, s.day, steps, s.sceneId)
                    s = engine.choose(s, enabled.random(r).id, lang).state
                }
            }
        }
        return Run(false, false, s.day, steps, "adım sınırı: ${s.sceneId}")
    }

    @Test
    fun `200 rastgele oynanis cokmeden ve cikmaza girmeden ilerler`() {
        val runs = (0 until 200).map { i -> randomPlay(10_000L + i, if (i % 2 == 0) GameMode.NORMAL else GameMode.IRONMAN) }
        val deadEnds = runs.filter { it.deadEnd != null }
        assertTrue("Çıkmazlar: ${deadEnds.take(5)}", deadEnds.isEmpty())
        // Her oynanış ya Perde 1 sonuna (ya da Demir Mod Hasat'ına) ulaşır ya da Normal modda ölür.
        assertTrue(runs.all { it.ended || it.dead })
        val finished = runs.count { it.ended }
        assertTrue("Bitirenler: $finished/200", finished >= 100)
    }
}
