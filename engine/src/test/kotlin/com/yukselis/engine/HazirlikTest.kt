package com.yukselis.engine

import com.yukselis.engine.content.ContentLoader
import com.yukselis.engine.content.DirectoryContentSource
import com.yukselis.engine.core.GameMode
import com.yukselis.engine.core.WorldState
import com.yukselis.engine.i18n.Lang
import com.yukselis.engine.save.InMemorySaveStorage
import com.yukselis.engine.save.SaveManager
import com.yukselis.engine.story.StoryEngine
import com.yukselis.engine.world.ChapterPhase
import com.yukselis.engine.world.ChapterTable
import com.yukselis.engine.world.HubAction
import com.yukselis.engine.world.Mission
import com.yukselis.engine.world.MissionKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Tarihli operasyonlar (DECISIONS #141): geri sayım, hazırlık, yan görevi kesmeme, uyarı, operasyon öncesi kayıt. */
class HazirlikTest {
    private val content = ContentLoader.load(DirectoryContentSource(File("../content")))
    private val engine = StoryEngine(content)
    private val lang = Lang.TR

    /** Bölüm 4 (Kule, tarihli: Gün 40) hazırlık evresinde, üste, kilitsiz. */
    private fun chapter4(day: Int = 35): WorldState {
        var s = engine.newGame("T", GameMode.NORMAL, 3)
        s = engine.throughBriefing(engine.choose(s.copy(sceneId = "ilk_sabah", day = 1), "gune_basla", lang).state)
        s = engine.world.enterHub(s).state
        return s.copy(
            day = day, chapter = 4, chapterPhase = ChapterPhase.PREP, region = "kiyikent_liman", mission = null,
            dayState = s.dayState.copy(slotsLeft = 4, slotsTotal = 4, forcedLock = false, hand = emptyList()),
        )
    }

    @Test
    fun `tarihli bolumde hedef satirinda geri sayim var`() {
        val line = engine.view(chapter4(35), Lang.TR).chapter!!
        assertEquals("Kule'nin yükselişine 5 gün", line.countdown)
        assertEquals("5 days until the Spire rises", engine.view(chapter4(35), Lang.EN).chapter!!.countdown)
        // Erken girilebilen bölümde geri sayım yok.
        assertTrue(content.chapters.get(1)!!.earlyEntry)
        assertNull(content.chapters.get(1)!!.countdown)
    }

    @Test
    fun `her tarihli operasyonun en az iki hazirligi var ve erken giris yok`() {
        content.chapters.chapters.filter { !it.earlyEntry }.forEach { ch ->
            assertTrue("bölüm ${ch.no}", ch.preparations.size >= 2)
            assertNotNull("bölüm ${ch.no} geri sayım", ch.countdown)
        }
        val s = chapter4()
        assertTrue(engine.world.operationChoices(s).isEmpty())
    }

    @Test
    fun `hazirlik bayrak birakir ve operasyonu degistirir`() {
        var s = chapter4()
        assertTrue(HubAction.Prepare("kule_tahliye") in engine.world.legalActions(s))
        s = engine.hubAction(s, HubAction.Prepare("kule_tahliye"), lang).state
        assertTrue(s.hasFlag("hazirlik_kule_tahliye"))
        s = s.copy(dayState = s.dayState.copy(forcedLock = false))
        s = engine.hubAction(s, HubAction.Prepare("kule_iskele"), lang).state
        assertTrue(s.hasFlag("hazirlik_kule_iskele"))
        // Bir kez yapılır.
        assertFalse(engine.world.preparationChoices(s).any { it.id == "kule_tahliye" })
        // Operasyon günü: tahliye kayıpları azaltır, iskele ek seçenek açar.
        val before = s.variable("hayatta_kalan")
        s = engine.world.enterHub(s.copy(day = 40, dayState = s.dayState.copy(forcedLock = false))).state
        assertEquals(MissionKind.OPERATION, s.mission?.kind)
        assertEquals(before + 3, s.variable("hayatta_kalan"))
        while (s.dayState.currentEvent != "kule_yukselisi" && s.sceneId != WorldState.HUB) {
            s = engine.choose(s, engine.view(s, lang).choices.first { it.enabled }.id, lang).state
        }
        val choices = engine.view(s, lang).choices
        assertTrue(choices.any { it.id == "cephe_hazirlik" && it.enabled })
        // Hazırlıksız oyunda ek seçenek görünmez.
        var plain = engine.world.enterHub(chapter4(40)).state
        while (plain.dayState.currentEvent != "kule_yukselisi" && plain.sceneId != WorldState.HUB) {
            plain = engine.choose(plain, engine.view(plain, lang).choices.first { it.enabled }.id, lang).state
        }
        assertFalse(engine.view(plain, lang).choices.any { it.id == "cephe_hazirlik" })
    }

    @Test
    fun `son tarih yan gorevi kesmez, gorev bitince operasyon baslar`() {
        val side = chapter4(39).copy(mission = Mission(MissionKind.SIDE, "sanayi", emptyList(), 39), region = "sanayi")
        var s = engine.world.startDay(side, advance = true).state
        assertEquals(40, s.day)
        assertEquals(MissionKind.SIDE, s.mission?.kind)
        assertFalse(s.hasFlag(ChapterTable.overflowFlag(4)))
        // Görev biter (kuyruk boş): üsse dönülür, oyuncunun ilk eyleminden sonra operasyon başlar.
        s = engine.world.enterHub(s).state
        assertNull(s.mission)
        s = engine.hubAction(s, HubAction.Rest, lang).state
        assertEquals(MissionKind.OPERATION, s.mission?.kind)
        // Keşif görevi ise operasyon günü üsse çağrılır.
        val explore = chapter4(39).copy(mission = Mission(MissionKind.EXPLORE, "sanayi", emptyList(), 39), region = "sanayi")
        val e = engine.world.startDay(explore, advance = true).state
        assertNull(e.mission)
        assertEquals("kiyikent_liman", e.region)
    }

    @Test
    fun `son tarihe az kala uste ayrilmak icin uyari var`() {
        assertNull(engine.view(chapter4(35), lang).hub!!.deadlineWarning)
        val w = engine.view(chapter4(39), lang).hub!!.deadlineWarning
        assertNotNull(w)
        assertTrue(w!!.contains("1"))
    }

    @Test
    fun `operasyon oncesi kayit ayri yuvaya yazilir, Demir Modda yazilmaz`() {
        val storage = InMemorySaveStorage()
        val saves = SaveManager(storage)
        val s = chapter4()
        saves.saveBeforeOperation(s)
        assertEquals(s.day, saves.load(SaveManager.OPERATION)?.day)
        assertTrue(saves.allSlots().any { it.first == SaveManager.OPERATION && it.second != null })
        val iron = SaveManager(InMemorySaveStorage())
        iron.saveBeforeOperation(s.copy(mode = GameMode.IRONMAN))
        assertNull(iron.load(SaveManager.OPERATION))
    }
}
