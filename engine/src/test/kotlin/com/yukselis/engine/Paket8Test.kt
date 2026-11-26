package com.yukselis.engine

import com.yukselis.engine.combat.EnemyScaling
import com.yukselis.engine.content.ContentLoader
import com.yukselis.engine.content.ContentParser
import com.yukselis.engine.content.DirectoryContentSource
import com.yukselis.engine.content.YamlTree
import com.yukselis.engine.core.GameMode
import com.yukselis.engine.i18n.Lang
import com.yukselis.engine.story.Effect
import com.yukselis.engine.story.StoryEngine
import com.yukselis.engine.system.Grade
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Paket 8 testleri:
 * - Yeni Döngü (`yeni_dongu`) Yankı fısıltıları: yalnız NG+ oturumunda görünür ve Yankı susturulduğunda susar
 * - Zaman atlama (`96_zaman_atlama.yaml`) bültenleri ve yüksek Dikkat söylentileri
 * - `elif_yoldas_darbe` tohum koşulu: Konsey teklifi yer olmadığı için kaçtıysa Perde 3'te açılır; açık retten sonra açılmaz
 * - Perde 3–4 `@oyuncu±n%` ölçeklemeleri yüksek seviyede anlamlı fark yaratır ve kademeyi korur
 */
class Paket8Test {
    private val content by lazy { ContentLoader.load(DirectoryContentSource(File("../content"))) }
    private val engine by lazy { StoryEngine(content) }

    @Test
    fun `yeni dongu fisiltilari yalniz ng plus oturumunda acilir ve susturulmus yankida susar`() {
        val normal = engine.newGame("Arda", GameMode.NORMAL, seed = 42L, newGamePlus = false)
            .let { it.copy(sceneId = "brifing_1_1", flags = it.flags + "yanki_bagi") }
        val ngPlus = engine.newGame("Arda", GameMode.NORMAL, seed = 42L, previousGames = 1, newGamePlus = true)
            .let { it.copy(sceneId = "brifing_1_1", flags = it.flags + "yanki_bagi") }
        val silenced = ngPlus.copy(flags = ngPlus.flags + "yanki_susturuldu")

        val normalInsights = engine.view(normal, Lang.TR).current.insights
        val ngInsights = engine.view(ngPlus, Lang.TR).current.insights
        val silencedInsights = engine.view(silenced, Lang.TR).current.insights

        assertTrue(ngInsights.size > normalInsights.size)
        assertTrue(ngInsights.any { it.echo && "ezberlemiş" in it.text })
        assertTrue(silencedInsights.none { it.echo })
    }

    @Test
    fun `zaman atlama sahneleri perde ve lider fraksiyona gore sehir bulteni uretir`() {
        val base = engine.newGame("Arda", GameMode.NORMAL, seed = 7L).copy(
            day = 60, // Perde 2
            sceneId = "atlama_antrenman_1",
            reputation = mapOf("siginak" to 25, "lonca" to 5),
            attention = 50,
        )
        val scene = engine.view(base, Lang.TR).current
        assertTrue(scene.notices.any { "Şehir bülteni" in it.title && "Sığınak" in it.lines.first() })
        assertTrue(scene.paragraphs.any { "iki yabancı" in it })
    }

    @Test
    fun `elif acik ret secenekleri ikinci teklifi kapatir yer yok kapatmaz`() {
        fun flags(scene: String, choice: String) = content.scenes.getValue(scene).choices.first { it.id == choice }
            .effects.filterIsInstance<Effect.SetFlag>().map { it.flag }
        assertTrue("elif_yoldas_red" in flags("elif_yoldas_konsey_1", "konsey"))
        assertFalse("elif_yoldas_red" in flags("elif_yoldas_konsey_1", "yer_yok"))
        // Katılma seçeneği kapalıyken söylenen aynı söz ret sayılmaz.
        assertFalse("elif_yoldas_red" in flags("elif_yoldas_konsey_1", "konsey_bekle"))
        assertFalse("elif_yoldas_red" in flags("elif_yoldas_konsey_1", "katil"))
        assertTrue("elif_yoldas_red" in flags("elif_yoldas_darbe_1", "kogus"))
    }

    @Test
    fun `elif yoldas darbe tohumlari yer yokken tutar acik retten sonra tutmaz`() {
        val seeds = content.scenes.values.flatMap { sc ->
            sc.choices.flatMap { it.effects }.filterIsInstance<Effect.PlantSeed>()
                .filter { it.eventId == "elif_yoldas_darbe" }
        }
        assertEquals(5, seeds.size)

        val declinedEarlier = engine.newGame("Arda", GameMode.NORMAL, seed = 9L).let {
            it.copy(flags = it.flags + "elif_yoldas_teklif")
        }
        val alreadyCompanion = declinedEarlier.copy(flags = declinedEarlier.flags + "elif_yoldas")
        // Açık ret (DECISIONS #194): Konsey teklifinde "Konsey'de lazımsın" diyen oyuncuya ikinci teklif gelmez.
        val refused = declinedEarlier.copy(flags = declinedEarlier.flags + "elif_yoldas_red")

        seeds.forEach { seed ->
            val cond = ContentParser.condition(YamlTree.parseObject(seed.condition!!, "test"), content.endings)
            assertTrue(cond.test(declinedEarlier))
            assertFalse(cond.test(alreadyCompanion))
            assertFalse(cond.test(refused))
        }
    }

    @Test
    fun `perde 3 ve 4 yuzde olcekli dusmanlar yuksek seviyede kademeyi koruyup gucu ayirir`() {
        val solo = EnemyScaling.resolve("cekirdek_kapicisi@oyuncu+5%", day = 340, playerLevel = 820)
        val army = EnemyScaling.resolve("cekirdek_kapicisi@oyuncu-10%", day = 340, playerLevel = 820)
        assertEquals(Grade.S, EnemyScaling.pinnedGrade(solo))
        assertEquals(Grade.S, EnemyScaling.pinnedGrade(army))

        val soloEnemy = content.enemy(solo)!!
        val armyEnemy = content.enemy(army)!!
        assertEquals(Grade.S, soloEnemy.grade)
        assertEquals(Grade.S, armyEnemy.grade)
        assertTrue(soloEnemy.maxHp > armyEnemy.maxHp)
    }
}
