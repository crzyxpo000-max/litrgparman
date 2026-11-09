package com.yukselis.engine

import com.yukselis.engine.content.ContentLoader
import com.yukselis.engine.content.DirectoryContentSource
import com.yukselis.engine.core.GameMode
import com.yukselis.engine.core.WorldState
import com.yukselis.engine.i18n.Lang
import com.yukselis.engine.story.StoryEngine
import com.yukselis.engine.system.Progression
import com.yukselis.engine.world.ChapterTable
import com.yukselis.engine.world.Director
import com.yukselis.engine.world.HubAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Paket 3 (ritim; DECISIONS #161–#164): varış yalnız ilk ziyarette, yolculuk eylem harcamaz, Perde 1'de el 2 kart, dünya seviyesi rampası. */
class Paket3Test {
    private val content = ContentLoader.load(DirectoryContentSource(File("../content")))
    private val engine = StoryEngine(content)
    private val lang = Lang.TR

    private fun atHub(seed: Long = 1): WorldState {
        var s = engine.newGame("T", GameMode.NORMAL, seed)
        s = s.copy(sceneId = "ilk_sabah", day = 1)
        s = engine.throughBriefing(engine.choose(s, "gune_basla", lang).state)
        return engine.world.enterHub(s).state
    }

    private fun finishEvent(state: WorldState): WorldState {
        var s = state
        var guard = 0
        while (s.sceneId != WorldState.HUB && !s.ended && guard++ < 200) {
            s = when {
                s.prompts.isNotEmpty() -> engine.view(s, lang).prompt!!.let { p -> engine.resolvePrompt(s, p.options.first().id, lang, p.kind, p.subject).state }
                s.combat != null -> engine.combatAction(s, engine.autoCombatAction(s), lang).state
                else -> engine.choose(s, engine.view(s, lang).choices.first { it.enabled }.id, lang).state
            }
        }
        return s
    }

    @Test
    fun `yolculuk eylem harcamaz ve varis sahnesi yalniz ilk ziyarette`() {
        val s0 = atHub().let { it.copy(dayState = it.dayState.copy(slotsLeft = 2, slotsTotal = 2, forcedLock = false, hand = emptyList())) }
        val slots = s0.dayState.slotsLeft
        val first = engine.hubAction(s0, HubAction.Travel("harabe_carsi"), lang).state
        // İlk gidiş: varış sahnesi açılır, eylem harcanmaz.
        assertEquals("varis_harabe_carsi", first.dayState.currentEvent)
        assertEquals(slots, first.dayState.slotsLeft)
        var s = finishEvent(first)
        assertEquals("harabe_carsi", s.region)
        // Üsse dön ve yeniden git: bu kez sahne yok, yalnız yolculuk satırı; eylem yine harcanmaz.
        s = engine.hubAction(s, HubAction.ReturnToBase, lang).state
        s = s.copy(dayState = s.dayState.copy(forcedLock = false, hand = emptyList()))
        assertEquals(1, s.variable(ChapterTable.VISIT_PREFIX + "harabe_carsi"))
        val slotsBefore = s.dayState.slotsLeft
        val o = engine.hubAction(s, HubAction.Travel("harabe_carsi"), lang)
        assertTrue(o.state.dayState.currentEvent != "varis_harabe_carsi")
        assertEquals("harabe_carsi", o.state.region)
        assertEquals(slotsBefore, o.state.dayState.slotsLeft)
        assertTrue(o.notices.any { it.title.contains("Harabe Çarşı") })
    }

    @Test
    fun `perde 1 de el iki kart perde 2 de uc`() {
        val s = atHub()
        assertEquals(Director.HAND_SIZE_ACT1, Director.handSize(s))
        assertEquals(2, Director.HAND_SIZE_ACT1)
        assertEquals(Director.HAND_SIZE, Director.handSize(s.copy(day = 35)))
        // Üste dağıtılan el: en çok 2 kart + başka bölgedeki görev kartları.
        val dealt = engine.world.enterHub(s.copy(dayState = s.dayState.copy(hand = emptyList(), forcedLock = false))).state
        val local = dealt.dayState.hand.count { id -> content.events[id]?.let { engine.world.director.fitsLocation(dealt, it) } == true }
        assertTrue("yerel kart $local", local <= Director.HAND_SIZE_ACT1)
    }

    @Test
    fun `dunya seviyesi perde 2 girisinde rampali`() {
        assertEquals(10.0, Progression.worldLevel(30), 0.01)
        assertTrue(Progression.worldLevel(40) in 16.0..18.0)
        val full = 20.0 * Math.pow(1.0385, 20.0 / 3.0)
        assertEquals(full, Progression.worldLevel(50), 0.01)
        // Rampa sonrası eğri eskisiyle aynı: Gün 180 C kademesi, Gün 330 S kademesi.
        assertTrue(Progression.worldLevel(180) in 120.0..145.0)
        assertTrue(Progression.worldLevel(330) in 800.0..950.0)
        // Tekdüze artış.
        (30 until 60).forEach { d -> assertTrue("gün $d", Progression.worldLevel(d + 1) >= Progression.worldLevel(d)) }
    }
}
