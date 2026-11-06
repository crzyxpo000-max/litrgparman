package com.yukselis.engine

import com.yukselis.engine.combat.CombatAction
import com.yukselis.engine.combat.CombatEngine
import com.yukselis.engine.combat.CombatMath
import com.yukselis.engine.combat.CombatSpec
import com.yukselis.engine.combat.Stance
import com.yukselis.engine.combat.StatusEffects
import com.yukselis.engine.content.ContentLoader
import com.yukselis.engine.content.DirectoryContentSource
import com.yukselis.engine.core.GameMode
import com.yukselis.engine.core.Rng
import com.yukselis.engine.core.WorldState
import com.yukselis.engine.i18n.Lang
import com.yukselis.engine.save.SaveCodec
import com.yukselis.engine.story.ActionRejected
import com.yukselis.engine.story.Content
import com.yukselis.engine.story.Effect
import com.yukselis.engine.story.StoryEngine
import com.yukselis.engine.system.Grade
import com.yukselis.engine.system.Progression
import com.yukselis.engine.system.Stat
import com.yukselis.engine.system.Stats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/** Aşama 3: Savaş. GDD formülleri, belirlenimcilik, modlar, ölüm ve simülasyonla kazanma oranları. */
class CombatTest {
    private val engine = StoryEngine(ContentLoader.load(DirectoryContentSource(File("../content"))))
    private val lang = Lang.TR

    private fun player(level: Int, seed: Long = 1, mode: GameMode = GameMode.NORMAL, stat: Int = Stats.BASE_VALUE): WorldState {
        val base = engine.newGame("T", mode, seed)
        // Seviyenin getirdiği serbest puanların dağıtılmış olduğunu varsay: statlar seviyeyle büyür.
        val grown = stat + (level - 1) * 3 / 8 + 0
        val stats = Stats(Stat.entries.associateWith { grown })
        val p = base.player.copy(level = level, stats = stats, unlockedGrade = Grade.forLevel(level))
        return base.copy(player = p.copy(hp = p.maxHp, mp = p.maxMp))
    }

    private fun spec(vararg enemies: String, env: List<String> = emptyList(), lose: String? = null) =
        CombatSpec(enemies.toList(), env, win = "ilk_gece", lose = lose, flee = "ilk_gece")

    private fun start(s: WorldState, spec: CombatSpec) = engine.startCombat(s, spec, lang).state

    private fun auto(s: WorldState, limit: Int = 300): WorldState {
        var st = s
        var n = 0
        while (st.combat != null && !st.dead && n++ < limit) st = engine.combatAction(st, engine.autoCombatAction(st), lang).state
        return st
    }

    // ---- Formüller (GDD) ----

    @Test
    fun `gdd savas formulleri`() {
        val s = Stats(Stat.entries.associateWith { 10 })
        assertEquals(10 + 5.0 + 7, CombatMath.initiative(s, 7), 1e-9)
        assertEquals(0.75, CombatMath.hitChance(s, s), 1e-9)
        assertEquals(0.95, CombatMath.hitChance(Stats(mapOf(Stat.PER to 100, Stat.AGI to 0)), Stats(mapOf(Stat.AGI to 0))), 1e-9)
        assertEquals(0.05, CombatMath.hitChance(s, s, blind = true, bonus = -0.5), 1e-9)
        assertEquals(0.35, CombatMath.hitChance(s, s, blind = true), 1e-9)
        assertEquals(0.07, CombatMath.critChance(s), 1e-9)
        assertEquals(1.55, CombatMath.critMultiplier(s), 1e-9)
        assertEquals(2.0, CombatMath.gradeMultiplier(Grade.E, Grade.F), 1e-9)
        assertEquals(0.25, CombatMath.gradeMultiplier(Grade.F, Grade.D), 1e-9)
        assertEquals(20.0, CombatMath.resistance(s, magical = false, burning = false), 1e-9)
        assertEquals(18.0, CombatMath.resistance(s, magical = true, burning = true), 1e-9)
        assertEquals(3, CombatMath.bleedDamage(100))
    }

    @Test
    fun `hasar formulu varyans araliginda`() {
        val rng = Rng(9)
        repeat(200) {
            // (10 + 10·1.0) · 100/120 = 16,67 → ±%10
            val d = CombatMath.damage(10, 10, 1.0, 20.0, rng)
            assertTrue(d in 15..18)
        }
    }

    @Test
    fun `ezme kurali`() {
        assertTrue(CombatMath.isCrush(20, Grade.F, 4, Grade.F))
        assertFalse(CombatMath.isCrush(8, Grade.F, 4, Grade.F)) // fark 5'ten az
        assertTrue(CombatMath.isCrush(25, Grade.E, 24, Grade.F)) // alt kademe
    }

    // ---- Akış ----

    @Test
    fun `ayni tohum ayni savasi uretir`() {
        val a = auto(start(player(5, seed = 11), spec("sis_tazisi", "sis_tazisi")))
        val b = auto(start(player(5, seed = 11), spec("sis_tazisi", "sis_tazisi")))
        assertEquals(a, b)
    }

    @Test
    fun `savas ortasinda kaydedip yuklenince ayni yerden devam eder`() {
        var s = start(player(5, seed = 3), spec("sis_tazisi", "sis_tazisi", env = listOf("dar_gecit")))
        s = engine.combatAction(s, CombatAction.Attack(0), lang).state
        val loaded = SaveCodec.decode(SaveCodec.encode(s, 1)).state
        assertEquals(s, loaded)
        assertEquals(auto(s), auto(loaded))
    }

    @Test
    fun `cok zayif dusman ezme moduyla tek satirda cozulur`() {
        val s = start(player(20), spec("igne_tavsan", "pas_kenesi"))
        assertNull(s.combat)
        assertEquals("ilk_gece", s.sceneId)
        assertEquals(1, s.variable(CombatEngine.CTX_CRUSH))
        assertTrue(s.player.xp > 0 || s.player.level > 20)
    }

    @Test
    fun `son dusman dusunce infaz secilir`() {
        var s = start(player(4, seed = 4), spec("kanal_faresi"))
        var n = 0
        while (s.combat?.pendingExecution == null && s.combat != null && n++ < 100) {
            s = engine.combatAction(s, CombatAction.Attack(0), lang).state
        }
        assertNotNull(s.combat?.pendingExecution)
        try {
            engine.combatAction(s, CombatAction.Attack(0), lang)
            fail("İnfaz beklerken saldırı olmamalı")
        } catch (_: ActionRejected) {
        }
        val mercy = s.morality.mercy
        s = engine.combatAction(s, CombatAction.Execute(CombatEngine.EXEC_BRUTAL), lang).state
        assertNull(s.combat)
        // Akılsız canavarın (fare) infazı ahlakı kaydırmaz (DECISIONS #113); sayaç yine artar.
        assertEquals(mercy, s.morality.mercy)
        assertEquals(1, s.variable("acimasiz_infaz"))
        assertTrue(s.hasFlag("yenildi_kanal_faresi"))
    }

    @Test
    fun `boss fazlari esiklerde acilir`() {
        var s = start(player(30, seed = 2, stat = 20), spec("ogretici_bekcisi"))
        val phases = mutableListOf<Int>()
        var n = 0
        while (s.combat != null && s.combat!!.pendingExecution == null && n++ < 200 && !s.dead) {
            s = engine.combatAction(s, CombatAction.Attack(0), lang).state
            s.combat?.enemies?.first()?.phase?.let { if (phases.lastOrNull() != it) phases += it }
        }
        assertTrue("fazlar sırayla geçilmeli: $phases", phases.containsAll(listOf(1, 2, 3)))
    }

    @Test
    fun `boss savasinda kacilamaz`() {
        val s = start(player(18, seed = 5, stat = 12), spec("ogretici_bekcisi"))
        val after = engine.combatAction(s, CombatAction.Flee, lang).state
        assertNotNull(after.combat)
    }

    @Test
    fun `durum etkileri tur sonunda isler`() {
        var s = start(player(10, seed = 6), spec("kemik_tacli_geyik"))
        s = s.copy(combat = s.combat!!.copy(playerStatuses = listOf(com.yukselis.engine.combat.StatusInstance(StatusEffects.BLEED, 2))))
        val hp = s.player.hp
        s = engine.combatAction(s, CombatAction.Defend, lang).state
        assertTrue(s.player.hp < hp)
    }

    @Test
    fun `durus ve efor hasari degistirir ama tur tuketmez`() {
        var s = start(player(5, seed = 7), spec("sis_tazisi"))
        val round = s.combat!!.round
        s = engine.combatAction(s, CombatAction.SetStance(Stance.ALL_OUT), lang).state
        s = engine.combatAction(s, CombatAction.SetEffort(3), lang).state
        assertEquals(round, s.combat!!.round)
        assertEquals(Stance.ALL_OUT, s.combat!!.stance)
        assertEquals(3, s.combat!!.effort)
    }

    @Test
    fun `normal modda ikinci sans bir kez sonra olum`() {
        var s = start(player(1, seed = 8, stat = 3), spec("kemik_tacli_geyik"))
        s = auto(s)
        assertTrue(Grade.F in s.secondChances)
        assertTrue(s.dead)
        assertNotNull(s.combat)
    }

    @Test
    fun `yenilgi sahnesi varsa oraya gidilir`() {
        var s = player(1, seed = 8, stat = 3).copy(secondChances = setOf(Grade.F))
        s = auto(start(s, spec("kemik_tacli_geyik", lose = "ilk_sabah")))
        assertNull(s.combat)
        assertEquals("ilk_sabah", s.sceneId)
        assertEquals(1, s.player.hp)
    }

    @Test
    fun `demir modda olum hasat sonuna baglanir`() {
        val s = auto(start(player(1, seed = 8, mode = GameMode.IRONMAN, stat = 3), spec("kemik_tacli_geyik")))
        assertEquals(Content.HARVEST_SCENE, s.sceneId)
        assertTrue(s.ended)
    }

    @Test
    fun `kademe kirma buyuk xp ve unvan verir`() {
        // E kademesi elit, F kademesi oyuncu: kazanırsa ×3 XP ve Kademe Kıran.
        var s = player(24, seed = 12, stat = 80)
        s = auto(start(s, spec("kemik_tacli_geyik")))
        assertNull(s.combat)
        assertEquals(1, s.variable(CombatEngine.VAR_GAP_BREAKS))
        assertTrue("kademe_kiran" in s.player.titles)
        assertTrue(s.hasFlag("uyanis_kirik_tavan"))
    }

    @Test
    fun `kacan zeki dusman hafizaya girer ve guclenerek doner`() {
        val s = player(5).copy(enemyMemory = mapOf("cop_avcisi" to com.yukselis.engine.combat.EnemyMemory(fled = 2)))
        val started = start(s, spec("cop_avcisi"))
        val e = started.combat!!.enemies.first()
        val def = engine.content.enemies.getValue("cop_avcisi")
        assertEquals(def.level + 4, e.level)
        assertTrue(e.maxHp > def.maxHp)
    }

    @Test
    fun `dusuk canla kazanmak uyanis tetikler`() {
        val s = player(5).copy(vars = mapOf(CombatEngine.CTX_WON to 1, CombatEngine.CTX_LOWEST_HP to 4))
        val out = engine.applyEffects(s, listOf(Effect.AddVar("x", 0)), lang).state
        // Uyanışlar savaş sonunda denetlenir; burada doğrudan çağır.
        val awakened = com.yukselis.engine.combat.CombatEngine(engine.content, com.yukselis.engine.story.EffectApplier(engine.content)).checkAwakenings(out).state
        assertNotNull(awakened.player.skill("son_nefes"))
        assertTrue(awakened.hasFlag("uyanis_son_nefes"))
    }

    // ---- Simülasyon: kazanma oranları ----

    private fun winRate(level: Int, enemies: List<String>, runs: Int = 200, stat: Int = Stats.BASE_VALUE): Double {
        var wins = 0
        repeat(runs) { i ->
            val p = player(level, seed = 1000L + i, stat = stat).copy(secondChances = Grade.entries.toSet())
            val s = auto(start(p, CombatSpec(enemies, win = "ilk_gece", flee = "ilk_gece")))
            if (s.combat == null && !s.dead && s.sceneId == "ilk_gece" && s.variable(CombatEngine.CTX_WON) == 1) wins++
        }
        return wins.toDouble() / runs
    }

    @Test
    fun `seviye 5 oyuncu kanal faresini neredeyse hep yener`() {
        val r = winRate(5, listOf("kanal_faresi"))
        assertTrue("Kazanma oranı $r", r >= 0.95)
    }

    @Test
    fun `seviye 1 oyuncu ilk fareyi cogunlukla yener`() {
        val r = winRate(1, listOf("kanal_faresi"))
        assertTrue("Kazanma oranı $r", r in 0.75..1.0)
    }

    @Test
    fun `seviye 10 oyuncu sis tazisi surusune karsi zorlanir ama kazanir`() {
        val r = winRate(10, listOf("sis_tazisi", "sis_tazisi", "sis_tazisi"))
        assertTrue("Kazanma oranı $r", r in 0.6..1.0)
    }

    @Test
    fun `e kademe elit f kademe oyuncu icin cok tehlikeli`() {
        val r = winRate(12, listOf("kepenk_cenesi"), runs = 100)
        assertTrue("Kazanma oranı $r", r <= 0.2)
    }

    @Test
    fun `rastgele savaslardan kacilabilir - demir mod olumu haddini asmaktan gelir`() {
        // Sıradan düşmanlardan kaçmak çoğunlukla başarılı olur (boss hariç).
        var escaped = 0
        repeat(100) { i ->
            var s = start(player(3, seed = 500L + i, mode = GameMode.IRONMAN), spec("sis_tazisi", "sis_tazisi"))
            var n = 0
            while (s.combat != null && !s.dead && n++ < 10) s = engine.combatAction(s, CombatAction.Flee, lang).state
            if (s.combat == null && !s.ended) escaped++
        }
        assertTrue("Kaçış $escaped/100", escaped >= 90)
    }

    @Test
    fun `dusmanin uyguladigi bir turluk sersemleme oyuncunun bir turunu alir`() {
        var s = start(player(4, seed = 21), spec("sis_tazisi"))
        val hp = s.combat!!.enemies.first().hp
        val round = s.combat!!.round
        s = s.copy(combat = s.combat!!.copy(playerStatuses = listOf(com.yukselis.engine.combat.StatusInstance(StatusEffects.STUN, 1, fresh = true))))
        s = engine.combatAction(s, CombatAction.Attack(0), lang).state
        assertEquals(hp, s.combat!!.enemies.first().hp)
        assertTrue(s.combat!!.round > round)
        assertFalse(s.combat!!.playerHas(StatusEffects.STUN))
    }

    @Test
    fun `yazili yenilgi sahnesi ikinci sansi harcamaz`() {
        val s = auto(start(player(1, seed = 8, stat = 3), spec("kemik_tacli_geyik", lose = "ilk_sabah")))
        assertEquals("ilk_sabah", s.sceneId)
        assertTrue(s.secondChances.isEmpty())
    }

    @Test
    fun `hepsi kacarsa zafer sayilmaz`() {
        var s = start(player(5, seed = 22), spec("sis_tazisi"))
        s = s.copy(combat = s.combat!!.copy(enemies = s.combat!!.enemies.map { it.copy(gone = true, fled = true) }))
        s = engine.combatAction(s, CombatAction.Defend, lang).state
        assertNull(s.combat)
        assertEquals(0, s.variable(CombatEngine.CTX_WON))
    }

    @Test
    fun `analiz edilen goz zaafi her vurusta hedeflenir`() {
        var s = start(player(2, seed = 23), spec("kanal_faresi"))
        s = s.copy(combat = s.combat!!.copy(enemies = s.combat!!.enemies.map { it.copy(analyzed = true) }))
        var weak = false
        var n = 0
        while (!weak && s.combat != null && s.combat!!.pendingExecution == null && n++ < 30) {
            s = engine.combatAction(s, CombatAction.Attack(0), lang).state
            weak = s.combat?.log?.any { it.numbers?.en?.contains("WEAK SPOT") == true } == true
        }
        assertTrue(weak)
    }

    @Test
    fun `ingilizce gunlukte dusman adi artikel ve buyuk harfle`() {
        var s = start(player(3, seed = 24), spec("kanal_faresi"))
        s = engine.combatAction(s, CombatAction.Attack(0), lang).state
        val en = s.combat!!.log.map { it.prose.en }
        assertTrue(en.joinToString("|"), en.any { it.contains("The Sewer Rat") || it.contains("the Sewer Rat") })
        assertFalse(en.any { it.startsWith("the ") })
    }

    @Test
    fun `yeni savas onceki savasin baglam degiskenlerini sifirlar`() {
        val s = player(5).copy(vars = mapOf(CombatEngine.CTX_WON to 1, CombatEngine.CTX_LOWEST_HP to 3))
        val started = start(s, spec("sis_tazisi"))
        assertEquals(0, started.variable(CombatEngine.CTX_WON))
        assertEquals(0, started.variable(CombatEngine.CTX_LOWEST_HP))
    }
}
