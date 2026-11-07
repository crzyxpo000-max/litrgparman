package com.yukselis.engine

import com.yukselis.engine.content.ContentLoader
import com.yukselis.engine.content.DirectoryContentSource
import com.yukselis.engine.core.GameMode
import com.yukselis.engine.core.WorldState
import com.yukselis.engine.i18n.Lang
import com.yukselis.engine.story.ActionRejected
import com.yukselis.engine.story.EffectApplier
import com.yukselis.engine.story.StoryEngine
import com.yukselis.engine.system.Grade
import com.yukselis.engine.world.ChapterPhase
import com.yukselis.engine.world.HubAction
import com.yukselis.engine.world.Mission
import com.yukselis.engine.world.MissionKind
import com.yukselis.engine.world.WorldEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/** Kule hızlı tırmanış (DECISIONS #147). */
class KuleHizliTest {
    private val content = ContentLoader.load(DirectoryContentSource(File("../content")))
    private val engine = StoryEngine(content)
    private val lang = Lang.TR
    private val tower = content.tower!!

    /** Kule Dibi'nde keşif görevinde, Kule açık, `floor` katı geçilmiş, `level` seviyesinde. */
    private fun atTower(floor: Int, level: Int): WorldState {
        var s = engine.newGame("T", GameMode.NORMAL, 9)
        s = engine.throughBriefing(engine.choose(s.copy(sceneId = "ilk_sabah", day = 1), "gune_basla", lang).state)
        s = engine.world.enterHub(s).state
        return s.copy(
            day = 250, chapter = 12, chapterPhase = ChapterPhase.PREP, region = tower.region, seeds = emptyList(),
            mission = Mission(MissionKind.EXPLORE, tower.region, emptyList(), 250),
            flags = s.flags + "kule_acik",
            vars = s.vars + (EffectApplier.TOWER_VAR to floor),
            player = s.player.copy(level = level, hp = s.player.maxHp),
            dayState = s.dayState.copy(slotsLeft = 4, slotsTotal = 4, forcedLock = true, hand = emptyList()),
        )
    }

    @Test
    fun `aralık hikaye katında ve kademeye denk katta durur`() {
        val s = atTower(30, 500)
        val range = engine.world.fastClimbRange(s)!!
        assertEquals(31, range.first)
        assertEquals(39, range.last)
        assertEquals(39, engine.view(s, lang).hub!!.tower!!.fastTo)
        // Kademesine denk ya da üstündeki kat aralığa girmez.
        val low = atTower(30, tower.levelOf(36))
        val r2 = engine.world.fastClimbRange(low)
        if (r2 != null) assertTrue(r2.all { Grade.forLevel(tower.levelOf(it)) < low.player.grade })
        // Kademenin altında kat yoksa hızlı tırmanış yok.
        val weak = atTower(30, 10)
        assertNull(engine.world.fastClimbRange(weak))
        try { engine.hubAction(weak, HubAction.FastClimb, lang); fail("reddedilmeli") } catch (_: ActionRejected) {}
    }

    @Test
    fun `ödüller katlar tek tek geçilmiş gibi, yalnız tek yuva harcanır`() {
        val s = atTower(30, 500)
        val fast = engine.hubAction(s, HubAction.FastClimb, lang).state
        assertEquals(39, fast.variable(EffectApplier.TOWER_VAR))
        assertEquals(3, fast.dayState.slotsLeft)
        assertEquals(WorldEngine.FAST_EVENT, fast.dayState.currentEvent)
        assertEquals(s.player.hp, fast.player.hp)
        val summary = engine.view(fast, lang).current.paragraphs.first()
        assertTrue(summary, summary.contains("Kat 31–39") && summary.contains("kat: 9") && summary.contains("hasar: 0"))

        // Aynı katlar tek tek: kat ve Öz Kristali ödülü aynı.
        var one = s
        repeat(9) {
            one = engine.hubAction(one.copy(sceneId = WorldState.HUB, prompts = emptyList(), dayState = one.dayState.copy(slotsLeft = 4, forcedLock = true)), HubAction.ClimbTower, lang).state
            assertEquals(null, one.combat)
        }
        assertEquals(39, one.variable(EffectApplier.TOWER_VAR))
        assertEquals(one.player.essenceCrystals - s.player.essenceCrystals, fast.player.essenceCrystals - s.player.essenceCrystals)
        assertTrue(fast.player.xp != s.player.xp || fast.player.level > s.player.level)
    }
}
