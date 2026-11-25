package com.yukselis.engine

import com.yukselis.engine.combat.CombatEngine
import com.yukselis.engine.content.ContentCache
import com.yukselis.engine.content.ContentFormatException
import com.yukselis.engine.content.ContentLoader
import com.yukselis.engine.content.DirectoryContentSource
import com.yukselis.engine.core.GameMode
import com.yukselis.engine.core.SkillState
import com.yukselis.engine.core.WorldState
import com.yukselis.engine.i18n.Lang
import com.yukselis.engine.save.InMemorySaveStorage
import com.yukselis.engine.save.SaveManager
import com.yukselis.engine.story.Effect
import com.yukselis.engine.story.StoryEngine
import com.yukselis.engine.system.Grade
import com.yukselis.engine.system.Progression
import com.yukselis.engine.world.Gate
import com.yukselis.engine.world.RegionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Paket 6 testleri:
 * - İkili içerik önbelleği (ContentCache, UI_REVIEW B5)
 * - Önceki oturum kayıt yuvası (SaveManager.PREVIOUS_SESSION, UI_REVIEW B3)
 * - İlk saat kendini açan arayüz bayrakları (StatusView, UI_REVIEW D1)
 */
class Paket6Test {
    private val contentDir = File("../content")
    private val content by lazy { ContentLoader.load(DirectoryContentSource(contentDir)) }
    private val engine by lazy { StoryEngine(content) }

    @Test
    fun `ikili onbellek gercek icerigi birebir geri yukler`() {
        val src = DirectoryContentSource(contentDir)
        val yaml = ContentLoader.load(src)
        val bytes = ByteArrayOutputStream().also { ContentCache.write(ContentLoader.loadBundle(src), it) }.toByteArray()
        val cached = ContentLoader.loadFromCache(ByteArrayInputStream(bytes))

        assertEquals(yaml.startScene, cached.startScene)
        assertEquals(yaml.scenes.keys, cached.scenes.keys)
        assertEquals(yaml.events.keys, cached.events.keys)
        assertEquals(yaml.npcs.keys, cached.npcs.keys)
        assertEquals(yaml.enemies.keys, cached.enemies.keys)
        assertEquals(yaml.classes.keys, cached.classes.keys)
        assertEquals(yaml.skills.keys, cached.skills.keys)
        assertEquals(yaml.items.keys, cached.items.keys)
        assertEquals(yaml.titles.keys, cached.titles.keys)
        assertEquals(yaml.flags.keys, cached.flags.keys)
        assertEquals(yaml.quests.keys, cached.quests.keys)
        assertEquals(yaml.regions.keys, cached.regions.keys)
        assertEquals(yaml.chapters.chapters.size, cached.chapters.chapters.size)
        assertEquals(yaml.endings.endings.keys, cached.endings.endings.keys)
    }

    @Test(expected = ContentFormatException::class)
    fun `bozuk onbellek reddedilir`() {
        ContentCache.read(ByteArrayInputStream(byteArrayOf(1, 2, 3, 4, 0, 0, 0, 1)))
    }

    @Test
    fun `onceki oturum normal modda saklanir ve otomatik kayitla ezilmez`() {
        val mgr = SaveManager(InMemorySaveStorage())
        val s1 = engine.newGame("Arda", GameMode.NORMAL, seed = 10L).copy(day = 15)
        val s2 = engine.newGame("Bora", GameMode.NORMAL, seed = 20L).copy(day = 2)

        assertEquals(5, mgr.allSlots().size)
        assertNull(mgr.load(SaveManager.PREVIOUS_SESSION))

        mgr.savePreviousSession(s1)
        // Yeni oyunda birkaç otomatik kayıt yapılsa da önceki oturum korunur.
        mgr.autosave(s2)
        mgr.autosave(s2.copy(day = 3))
        mgr.autosave(s2.copy(day = 4))

        val slots = mgr.allSlots()
        assertEquals(6, slots.size)
        assertEquals(SaveManager.PREVIOUS_SESSION, slots[1].first)
        assertEquals(15, mgr.load(SaveManager.PREVIOUS_SESSION)?.day)
        assertEquals("Arda", mgr.load(SaveManager.PREVIOUS_SESSION)?.playerName)
    }

    @Test
    fun `demir modda onceki oturum yazilmaz`() {
        val mgr = SaveManager(InMemorySaveStorage())
        val iron = engine.newGame("Arda", GameMode.IRONMAN, seed = 7L).copy(day = 9)
        mgr.savePreviousSession(iron)
        assertNull(mgr.load(SaveManager.PREVIOUS_SESSION))
        assertEquals(5, mgr.allSlots().size)
    }

    @Test
    fun `ilk saatte gizli arayuz ogeleri ilgili olayla acilir`() {
        val fresh = engine.newGame("Arda", GameMode.NORMAL, seed = 42L)
        val st0 = engine.status(fresh, Lang.TR)
        assertFalse(st0.showMana)
        assertFalse(st0.showGrade)
        assertFalse(st0.showStamina)
        assertFalse(st0.showDanger)
        assertFalse(st0.showCrystals)

        val manaSkillId = content.skills.values.first { it.costMp > 0 }.id
        val withMana = fresh.copy(player = fresh.player.copy(skills = listOf(SkillState(manaSkillId))))
        assertTrue(engine.status(withMana, Lang.TR).showMana)

        val lv10 = fresh.copy(player = fresh.player.copy(level = Progression.CLASS_OFFER_LEVEL))
        assertTrue(engine.status(lv10, Lang.TR).showGrade)

        val afterFight = fresh.copy(vars = fresh.vars + (CombatEngine.VAR_FIGHTS to 1))
        assertTrue(engine.status(afterFight, Lang.TR).showStamina)

        val withGate = fresh.copy(regions = mapOf("orman" to RegionState("orman", danger = 20, control = "canavar", gates = listOf(Gate(1, Grade.F, listOf("fare"), openedDay = 1, deadline = 10)))))
        assertTrue(engine.status(withGate, Lang.TR).showDanger)

        // İlk kristal sayacı kalıcı açar; harcandıktan sonra 0 olsa bile açık kalır.
        val gotCrystal = engine.applyEffects(fresh, listOf(Effect.AddCrystals(2)), Lang.TR).state
        assertTrue(engine.status(gotCrystal, Lang.TR).showCrystals)
        val spentAll = engine.applyEffects(gotCrystal, listOf(Effect.AddCrystals(-2)), Lang.TR).state
        assertEquals(0, spentAll.player.essenceCrystals)
        assertTrue(engine.status(spentAll, Lang.TR).showCrystals)

        // Perde 2'de (Gün 35) hepsi açıktır.
        val act2 = fresh.copy(day = 35)
        val st2 = engine.status(act2, Lang.TR)
        assertTrue(st2.showMana && st2.showGrade && st2.showStamina && st2.showDanger && st2.showCrystals)
    }
}
