package com.yukselis.engine

import com.yukselis.engine.combat.EnemyScaling
import com.yukselis.engine.content.ContentLoader
import com.yukselis.engine.content.DirectoryContentSource
import com.yukselis.engine.core.ChronicleEntry
import com.yukselis.engine.core.GameMode
import com.yukselis.engine.save.SaveCodec
import com.yukselis.engine.story.StoryEngine
import com.yukselis.engine.system.Grade
import com.yukselis.engine.system.Progression
import com.yukselis.engine.system.Stat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Aşama 7 motoru: ölçekli düşmanlar, dünya seviyesi, kronik, kayıt v4, New Game+. */
class Asama7MotorTest {
    private val engine = StoryEngine(ContentLoader.load(DirectoryContentSource(File("../content"))))
    private val content = engine.content

    @Test
    fun `olcekli dusman basvurusu`() {
        assertEquals("kanal_faresi" to 120, EnemyScaling.parse("kanal_faresi@120"))
        assertEquals("kanal_faresi" to null, EnemyScaling.parse("kanal_faresi"))
        assertEquals("kanal_faresi", EnemyScaling.baseId("kanal_faresi@7"))
        val base = content.enemies.getValue("kanal_faresi")
        val scaled = content.enemy("kanal_faresi@120")
        assertNotNull(scaled)
        scaled!!
        assertEquals("kanal_faresi@120", scaled.id)
        assertEquals(120, scaled.level)
        assertEquals(Grade.C, scaled.grade)
        assertTrue("statlar büyür", scaled.stats[Stat.STR] > base.stats[Stat.STR] * 10)
        assertTrue("can büyür", scaled.maxHp > base.maxHp * 10)
        assertTrue("saldırı tabanı büyür", scaled.attacks.first().base > base.attacks.first().base)
        assertNull(content.enemy("yok_boyle_bir_sey@10"))
    }

    @Test
    fun `beklenen stat egrisi ve olcekli XP sabitlenir`() {
        // 5 + Σ (4 · çarpan + 3) / 8: F seviyesinde 0,875/seviye.
        assertEquals(5.0, EnemyScaling.expectedStat(1), 1e-9)
        assertEquals(5.0 + 24 * 0.875, EnemyScaling.expectedStat(25), 1e-9)
        // XP: bir sonraki seviyenin ihtiyacı × 0,012 · L × tür çarpanı (fare ≈ 1,1).
        val rat = content.enemies.getValue("kanal_faresi")
        val xp = EnemyScaling.xpFor(rat, 100)
        val perLevel = Progression.xpToNext(100)
        assertTrue("Seviye 100 fare ≈ 1,3 seviye ($xp / $perLevel)", xp in perLevel..(2 * perLevel))
        // Boss ×3
        val boss = content.enemies.values.first { it.boss }
        assertTrue(EnemyScaling.xpFor(boss, 100) > EnemyScaling.xpFor(rat, 100))
    }

    @Test
    fun `dunya seviyesi perdelerle yukselir`() {
        assertEquals(20.0, Progression.worldLevel(30), 0.01)
        assertTrue(Progression.worldLevel(180) in 120.0..145.0)
        assertTrue(Progression.worldLevel(330) in 800.0..950.0)
        assertEquals(Grade.C, Grade.forLevel(Progression.worldLevel(180).toInt()))
        assertEquals(Grade.S, Grade.forLevel(Progression.worldLevel(330).toInt()))
    }

    @Test
    fun `kayit v4 kronigi tasir ve v3 kaydi bos kronikle acilir`() {
        val s = engine.newGame("Arda", GameMode.NORMAL, seed = 5)
            .copy(chronicle = listOf(ChronicleEntry(40, "olay", "sahne", "secim", 17)))
        assertEquals(s, SaveCodec.decode(SaveCodec.encode(s, 1L)).state)
        val v3 = SaveCodec.decode("""{"v":3,"scene":"giris_entegrasyon","name":"Eski"}""").state
        assertTrue(v3.chronicle.isEmpty())
    }

    @Test
    fun `yeni oyun arti dongu bayragi ve stat bonusu`() {
        val normal = engine.newGame("Arda", GameMode.NORMAL, seed = 9)
        val plus = engine.newGame("Arda", GameMode.NORMAL, seed = 9, previousGames = 2, newGamePlus = true)
        assertTrue(plus.hasFlag(StoryEngine.NG_PLUS_FLAG))
        assertEquals(2, plus.variable(StoryEngine.PREVIOUS_GAMES_VAR))
        Stat.entries.forEach { assertEquals(normal.player.stats[it] + StoryEngine.NG_PLUS_STATS, plus.player.stats[it]) }
        assertTrue(!normal.hasFlag(StoryEngine.NG_PLUS_FLAG))
    }

    @Test
    fun `kronigin en kritik bes secimi agirliga gore`() {
        val entries = (1..8).map { ChronicleEntry(it * 10, "e$it", "s$it", "c$it", if (it % 2 == 0) 30 else 5) } +
            ChronicleEntry(90, "e9", "s9", "c9", 30)
        val top = engine.topChronicle(engine.newGame("A", GameMode.NORMAL, 1).copy(chronicle = entries))
        assertEquals(5, top.size)
        assertEquals(listOf(20, 40, 60, 80, 90), top.map { it.day })
    }
}
