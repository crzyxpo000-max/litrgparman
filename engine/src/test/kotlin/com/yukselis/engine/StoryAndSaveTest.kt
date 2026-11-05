package com.yukselis.engine

import com.yukselis.engine.content.ContentLoader
import com.yukselis.engine.content.ContentValidator
import com.yukselis.engine.content.DirectoryContentSource
import com.yukselis.engine.core.GameMode
import com.yukselis.engine.core.WorldState
import com.yukselis.engine.i18n.Lang
import com.yukselis.engine.save.InMemorySaveStorage
import com.yukselis.engine.save.IronmanSaveException
import com.yukselis.engine.save.SaveCodec
import com.yukselis.engine.save.SaveFormatException
import com.yukselis.engine.save.SaveManager
import com.yukselis.engine.story.NoticeKind
import com.yukselis.engine.story.StoryEngine
import com.yukselis.engine.system.Stat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

private fun loadContent() = ContentLoader.load(DirectoryContentSource(File("../content")))

class ContentFilesTest {
    @Test
    fun gercekIcerikGecerli() {
        val problems = ContentValidator.validate(loadContent())
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }
}

class StoryEngineTest {
    private val engine = StoryEngine(loadContent())

    @Test
    fun yeniOyunBaslangicSahnesindeBaslar() {
        val s = engine.newGame("Arda", GameMode.NORMAL, seed = 1)
        val v = engine.view(s, Lang.TR)
        assertEquals("giris_entegrasyon", v.current.sceneId)
        assertEquals("Kıyıkent İskelesi", v.placeName)
        assertTrue(v.current.paragraphs.first().contains("Deniz"))
        assertEquals(3, v.choices.size)
        assertEquals("Deniz'in elini sıkıca tut", v.choices.first().label)
    }

    @Test
    fun ingilizceSurumdeIsimlerYerellesir() {
        val s = engine.newGame("Alex", GameMode.NORMAL, seed = 1)
        val v = engine.view(s, Lang.EN)
        assertEquals("Harlow Ferry Terminal", v.placeName)
        assertTrue(v.current.paragraphs.first().contains("Devon"))
        assertFalse(v.current.paragraphs.joinToString().contains("Deniz"))
        assertEquals("Grab Devon's hand and hold on", v.choices.first().label)
    }

    @Test
    fun algiKosuluSeciminSonucunaBaglidir() {
        // "Pencereyi oku" Algı +1 verir; ikinci sahnedeki [ALG 6] seçeneği açılır.
        val reader = engine.choose(engine.newGame("A", GameMode.NORMAL, 1), "pencereyi_oku", Lang.TR).state
        val holder = engine.choose(engine.newGame("A", GameMode.NORMAL, 1), "el_tut", Lang.TR).state
        val listenForReader = engine.view(reader, Lang.TR).choices.first { it.id == "dinle" }
        val listenForHolder = engine.view(holder, Lang.TR).choices.first { it.id == "dinle" }
        assertTrue(listenForReader.enabled)
        assertFalse(listenForHolder.enabled)
        assertEquals("[ALG 6]", listenForHolder.requirementTag)
        assertEquals("[PER 6]", engine.view(holder, Lang.EN).choices.first { it.id == "dinle" }.requirementTag)
        try {
            engine.choose(holder, "dinle", Lang.TR)
            fail("Kilitli seçenek seçilebilmemeli")
        } catch (_: IllegalArgumentException) {
        }
    }

    @Test
    fun kosulluParagraflarSecimleriYansitir() {
        val withDeniz = engine.choose(engine.newGame("A", GameMode.NORMAL, 1), "el_tut", Lang.TR).state
        val alone = engine.choose(engine.newGame("A", GameMode.NORMAL, 1), "kalabaliga_kos", Lang.TR).state
        val a = engine.view(withDeniz, Lang.TR).current.paragraphs.joinToString(" ")
        val b = engine.view(alone, Lang.TR).current.paragraphs.joinToString(" ")
        assertTrue(a.contains("parmakların hâlâ"))
        assertTrue(b.contains("yalnızsın"))
        assertTrue(b.contains("tekerlekli sandalyedeki"))
    }

    @Test
    fun tamOyunAkisiSeviyeUnvanVeSon() {
        var s = engine.newGame("Arda", GameMode.NORMAL, seed = 5)
        s = engine.choose(s, "el_tut", Lang.TR).state
        s = engine.choose(s, "ciglige_kos", Lang.TR).state
        val fight = engine.choose(s, "dovus", Lang.TR)
        s = fight.state
        assertEquals(2, s.player.level)
        assertEquals(40L, s.player.xp)
        assertEquals(3, s.player.freePoints)
        assertEquals(6, s.player.stats[Stat.STR]) // unvan bonusu
        assertTrue("ogreticinin_ilk_kani" in s.player.titles)
        assertTrue(fight.notices.any { it.kind == NoticeKind.LEVEL_UP })
        assertTrue(fight.notices.any { it.kind == NoticeKind.TITLE && it.title.contains("Öğreticinin İlk Kanı") })
        assertEquals(s.player.maxHp, s.player.hp)

        s = engine.choose(s, "uyu", Lang.TR).state
        assertEquals(1, s.day)
        assertTrue(s.ended)
        val end = engine.view(s, Lang.EN)
        assertTrue(end.isEnding)
        assertTrue(end.choices.isEmpty())
        assertEquals(4, end.history.size)
        assertEquals("Grab Devon's hand and hold on", end.history.first().chosenLabel)
    }

    @Test
    fun kacmakTohumBildirimiBirakir() {
        var s = engine.newGame("A", GameMode.NORMAL, 1)
        s = engine.choose(s, "pencereyi_oku", Lang.TR).state
        s = engine.choose(s, "saklan", Lang.TR).state
        s = engine.choose(s, "kac", Lang.TR).state
        assertEquals(-14, s.morality.mercy) // saklan −4 + kac −10
        s = engine.choose(s, "nobet_tut", Lang.TR).state
        val notices = engine.view(s, Lang.TR).current.notices
        assertTrue(notices.any { it.lines.any { l -> l.contains("Geride bıraktığın") } })
        assertFalse(notices.any { it.lines.any { l -> l.contains("Kurtardığın") } })
    }

    @Test
    fun durumEkrani() {
        val st = engine.status(engine.newGame("Arda", GameMode.IRONMAN, 1), Lang.TR)
        assertEquals("Arda", st.playerName)
        assertEquals(1, st.level)
        assertEquals(100L, st.xpToNext)
        assertEquals("GÜÇ", st.stats.first().abbr)
        assertEquals(GameMode.IRONMAN, st.mode)
    }
}

class SaveTest {
    private val engine = StoryEngine(loadContent())

    private fun midGame(): WorldState {
        var s = engine.newGame("Arda", GameMode.NORMAL, seed = 77)
        s = engine.choose(s, "el_tut", Lang.TR).state
        s = engine.choose(s, "ciglige_kos", Lang.TR).state
        return engine.choose(s, "dovus", Lang.TR).state
    }

    @Test
    fun kayitTamOlarakGeriYuklenir() {
        val s = midGame().copy(reputation = mapOf("siginak" to 15), rngState = 123456789L)
        val decoded = SaveCodec.decode(SaveCodec.encode(s, 1000L))
        assertEquals(s, decoded.state)
        assertEquals(1000L, decoded.savedAtMillis)
    }

    @Test
    fun yeniSurumKayitReddedilir() {
        try {
            SaveCodec.decode("""{"v":999,"scene":"x"}""")
            fail()
        } catch (_: SaveFormatException) {
        }
    }

    @Test
    fun eksikAlanlarVarsayilanaDuser() {
        val s = SaveCodec.decode("""{"v":1,"scene":"giris_entegrasyon","name":"Eski"}""").state
        assertEquals("Eski", s.playerName)
        assertEquals(1, s.player.level)
        assertEquals(s.player.maxHp, s.player.hp)
    }

    @Test
    fun slotlarVeDemirMod() {
        val mgr = SaveManager(InMemorySaveStorage()) { 42L }
        val s = midGame()
        mgr.save("slot2", s)
        mgr.autosave(s)
        assertEquals(s, mgr.load("slot2"))
        assertNull(mgr.load("slot1"))
        val info = mgr.info("auto")
        assertNotNull(info)
        assertEquals(2, info!!.level)
        assertEquals(42L, info.savedAtMillis)
        assertEquals(4, mgr.allSlots().size)

        try {
            mgr.save("slot1", s.copy(mode = GameMode.IRONMAN))
            fail()
        } catch (_: IronmanSaveException) {
        }
    }

    @Test
    fun bozukKayitCokertmez() {
        val storage = InMemorySaveStorage()
        storage.write("slot1", "{bozuk")
        assertNull(SaveManager(storage).load("slot1"))
    }
}
