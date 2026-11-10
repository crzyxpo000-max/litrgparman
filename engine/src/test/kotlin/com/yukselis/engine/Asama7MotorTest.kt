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
        // Paket 3 rampası: Gün 30'da eğrinin yarısı, Gün 50'den itibaren tam eğri.
        assertEquals(10.0, Progression.worldLevel(30), 0.01)
        assertEquals(20.0 * Math.pow(1.0385, 20.0 / 3.0), Progression.worldLevel(50), 0.01)
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

    @Test
    fun `sabit kademeli basvuru olceklenir ve kimligi korur`() {
        // DECISIONS #177: "@oyuncu±n%" çözülünce oyuncunun kademesini taşır; seviye S'in altına inse de kademe S kalır.
        val pinned = content.enemy("kanal_faresi@765^S")!!
        assertEquals("kanal_faresi@765^S", pinned.id)
        assertEquals(765, pinned.level)
        assertEquals(Grade.S, pinned.grade)
        assertEquals(Grade.A, content.enemy("kanal_faresi@765")!!.grade)
        assertEquals(pinned.maxHp, content.enemy("kanal_faresi@765")!!.maxHp)
    }

    @Test
    fun `vali savasinin destekleri yuzdelidir ve ikinci deneme en kolayidir`() {
        // Sv. 850'de ±5 seviye hiçbir şey değiştirmiyordu (kazanma %48 → %51); destekler yüzdeyle çalışır.
        val refs = content.scenes.values.flatMap { it.choices }.flatMap { it.effects }
            .filterIsInstance<com.yukselis.engine.story.Effect.StartCombat>()
            .flatMap { it.spec.enemies }.filter { it.startsWith("istila_hasat_valisi@") }
        assertTrue("Vali savaşı bulunamadı", refs.size >= 8)
        val offsets = refs.map { r ->
            assertTrue("Yüzdesiz Vali başvurusu: $r", r.endsWith("%"))
            r.substringAfter("@oyuncu").removeSuffix("%").toInt()
        }
        assertEquals("Desteksiz yüzleşme oyuncunun seviyesinde", 0, offsets.max())
        assertTrue("Destekler en az %10 indirir", offsets.filter { it != 0 && it != -5 }.all { it <= -10 })
        assertEquals("İkinci deneme en kolay savaş", -30, offsets.min())
        val retry = content.scenes.getValue("p4_istila_yenilgi").choices.first { it.id == "tekrar" }.effects
            .filterIsInstance<com.yukselis.engine.story.Effect.StartCombat>().single().spec.enemies.single()
        assertEquals("istila_hasat_valisi@oyuncu-30%", retry)
    }
}

class Asama7GoreliSeviyeTest {
    @Test
    fun `yuzdeli oyuncu basvurusu oyuncunun kademesinde kalir`() {
        // DECISIONS #177: Sv. 850'de −%10 → 765; kademe sabitlenmese düşman A'ya iner ve kademe farkı savaşı bitirir.
        assertEquals("kanal_faresi@765^S", EnemyScaling.resolve("kanal_faresi@oyuncu-10%", 342, 850))
        assertEquals("kanal_faresi@850^S", EnemyScaling.resolve("kanal_faresi@oyuncu+0%", 342, 850))
        assertEquals("kanal_faresi@559^A", EnemyScaling.resolve("kanal_faresi@oyuncu-30%", 342, 798))
        // Yukarı yönde de: Sv. 798 oyuncunun +%5'i 838 (S) olurdu; kademe A kalır.
        assertEquals("kanal_faresi@838^A", EnemyScaling.resolve("kanal_faresi@oyuncu+5%", 342, 798))
        assertEquals("kanal_faresi" to 765, EnemyScaling.parse("kanal_faresi@765^S"))
        assertEquals(Grade.S, EnemyScaling.pinnedGrade("kanal_faresi@765^S"))
        assertNull(EnemyScaling.pinnedGrade("kanal_faresi@765"))
        assertNull(EnemyScaling.pinnedGrade("kanal_faresi"))
        assertEquals("kanal_faresi", EnemyScaling.baseId("kanal_faresi@765^S"))
        // Çözülmüş başvuru yeniden çözülünce değişmez (kayıttan yüklenen savaş).
        assertEquals("kanal_faresi@765^S", EnemyScaling.resolve("kanal_faresi@765^S", 342, 900))
    }

    @Test
    fun `yuzde eki yalniz oyuncu basvurusunda gecerlidir`() {
        assertTrue(EnemyScaling.validSuffix("x@oyuncu-10%"))
        assertTrue(EnemyScaling.validSuffix("x@oyuncu+0%"))
        assertTrue(EnemyScaling.isRelative("x@oyuncu-10%"))
        assertTrue(EnemyScaling.validSuffix("x@oyuncu-5"))
        assertTrue(!EnemyScaling.validSuffix("x@dunya-10%"))
        assertTrue(!EnemyScaling.validSuffix("x@oyuncu%"))
        // Sabit kademeli biçimi yalnız motor üretir; içerikte yazılamaz.
        assertTrue(!EnemyScaling.validSuffix("x@765^S"))
    }

    @Test
    fun `goreli seviye savas basinda cozulur`() {
        // Paket 3 rampası: Gün 30'da dünya seviyesi 10 (DECISIONS #164).
        assertEquals("kanal_faresi@10", EnemyScaling.resolve("kanal_faresi@dunya", 30, 100))
        assertEquals("kanal_faresi@11", EnemyScaling.resolve("kanal_faresi@dunya+10", 30, 100))
        assertEquals("kanal_faresi@12", EnemyScaling.resolve("kanal_faresi@oyuncu+5", 100, 7))
        assertEquals("kanal_faresi@4", EnemyScaling.resolve("kanal_faresi@oyuncu-3", 100, 7))
        assertEquals("kanal_faresi@50", EnemyScaling.resolve("kanal_faresi@50", 100, 7))
        assertEquals("kanal_faresi@10", EnemyScaling.resolve("kanal_faresi@dunya", 30, 7))
        assertEquals("kanal_faresi@13", EnemyScaling.resolve("kanal_faresi@dunya", 60, 7))
        assertEquals("kanal_faresi", EnemyScaling.baseId("kanal_faresi@dunya+10"))
        assertTrue(EnemyScaling.isRelative("x@oyuncu"))
    }
}
