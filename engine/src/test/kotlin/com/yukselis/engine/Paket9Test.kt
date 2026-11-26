package com.yukselis.engine

import com.yukselis.engine.content.ContentLoader
import com.yukselis.engine.content.DirectoryContentSource
import com.yukselis.engine.core.GameMode
import com.yukselis.engine.core.PromptKind
import com.yukselis.engine.core.SystemPrompt
import com.yukselis.engine.i18n.Lang
import com.yukselis.engine.story.StoryEngine
import com.yukselis.engine.system.ClassKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Paket 9 testleri (UI_REVIEW D4, D5, E5):
 * - Haritada bulunulan bölge ilk sırada gelir ve `RegionView.act` doldurulur
 * - Savaş yenilgi sahnelerinde `SceneView.isDefeat` doğrudur, normal/zafer sahnelerinde yanlıştır
 * - Evrim isteminde anlık statlar "Şimdi:" / "Now:", seviye başı artış "Her seviye:" / "Per level:" öneki taşır
 */
class Paket9Test {
    private val content by lazy { ContentLoader.load(DirectoryContentSource(File("../content"))) }
    private val engine by lazy { StoryEngine(content) }

    @Test
    fun `harita bulunulan bolgeyi en uste alir ve perde bilgisini tasir`() {
        val state = engine.newGame("Arda", GameMode.NORMAL, seed = 17L).copy(day = 60, region = "kordon")
        val regions = engine.map(state, Lang.TR)
        assertTrue(regions.isNotEmpty())
        assertEquals("kordon", regions.first().id)
        assertTrue(regions.first().current)
        assertEquals(2, regions.first().act)
        assertTrue(regions.any { it.act == 1 } && regions.any { it.act == 3 })
    }

    @Test
    fun `yenilgi sahneleri isDefeat isareti tasir`() {
        val base = engine.newGame("Arda", GameMode.NORMAL, seed = 23L)
        assertTrue(engine.view(base.copy(sceneId = "ilk_canavar_yenilgi"), Lang.TR).isDefeat)
        assertTrue(engine.view(base.copy(sceneId = "fenerci_kayip"), Lang.TR).isDefeat)
        assertFalse(engine.view(base.copy(sceneId = "giris_entegrasyon"), Lang.TR).isDefeat)
        assertFalse(engine.view(base.copy(sceneId = "ilk_kan"), Lang.TR).isDefeat)
    }

    @Test
    fun `evrim istemi simdi ve her seviye on eklerini icerir`() {
        val evoClass = content.classes.values.first { it.bonus.isNotEmpty() }
        val state = engine.newGame("Arda", GameMode.NORMAL, seed = 31L).copy(
            prompts = listOf(SystemPrompt(PromptKind.EVOLUTION, listOf(evoClass.id), "E")),
        )
        val trDetails = engine.view(state, Lang.TR).prompt!!.options.first().details
        val enDetails = engine.view(state, Lang.EN).prompt!!.options.first().details

        assertTrue(trDetails.any { it.startsWith("Şimdi: ") })
        assertTrue(trDetails.any { it.startsWith("Her seviye: ") })
        assertTrue(enDetails.any { it.startsWith("Now: ") })
        assertTrue(enDetails.any { it.startsWith("Per level: ") })
    }
}
