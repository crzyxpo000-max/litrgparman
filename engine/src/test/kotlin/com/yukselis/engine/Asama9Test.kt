package com.yukselis.engine

import com.yukselis.engine.content.ContentLoader
import com.yukselis.engine.content.DirectoryContentSource
import com.yukselis.engine.core.GameMode
import com.yukselis.engine.i18n.Lang
import com.yukselis.engine.story.Notifier
import com.yukselis.engine.story.StoryEngine
import com.yukselis.engine.world.Gate
import com.yukselis.engine.world.RegionState
import com.yukselis.engine.system.Grade
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Aşama 9: oturumlar arası bildirim seçimi ve bildirimden olay açma. */
class Asama9Test {
    private val engine = StoryEngine(ContentLoader.load(DirectoryContentSource(File("../content"))))

    @Test
    fun `dikkat yuksekse ust kademeden goz bildirimi ve dokununca olay tohumlanir`() {
        val s = engine.newGame("A", GameMode.NORMAL, 3).copy(attention = 80, day = 120)
        val note = engine.pendingNotification(s, Lang.TR)
        assertNotNull(note)
        assertEquals("goz", note!!.first)
        assertTrue(note.third.contains("A kademesinden"))
        val opened = engine.openNotification(s, "goz")
        assertTrue(opened.seeds.any { it.eventId == Notifier.GAZE_EVENT && it.dueDay == 120 })
        // İkinci dokunuş aynı olayı ikinci kez eklemez.
        assertEquals(opened.seeds.size, engine.openNotification(opened, "goz").seeds.size)
    }

    @Test
    fun `tasacak kapi bildirimi ve sakin oyunda bildirim yok`() {
        val calm = engine.newGame("A", GameMode.NORMAL, 4).copy(day = 50, region = "kordon")
        assertNull(engine.pendingNotification(calm, Lang.EN))
        val gate = Gate(1, Grade.E, listOf("kanal_faresi"), 48, 51)
        val tense = calm.copy(regions = mapOf("kordon" to RegionState("kordon", 40, "canavar", listOf(gate))))
        val note = engine.pendingNotification(tense, Lang.EN)
        assertEquals("kapi", note?.first)
        assertTrue(note!!.third.contains("Grade E Gate"))
    }

    @Test
    fun `bitmis oyunda bildirim yok`() {
        val s = engine.newGame("A", GameMode.NORMAL, 5).copy(attention = 90, ended = true)
        assertNull(engine.pendingNotification(s, Lang.TR))
        assertEquals(s, engine.openNotification(s, "goz"))
    }
}
