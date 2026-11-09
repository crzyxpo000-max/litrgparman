package com.yukselis.engine

import com.yukselis.engine.content.ContentLoader
import com.yukselis.engine.content.DirectoryContentSource
import com.yukselis.engine.core.DailyQuest
import com.yukselis.engine.core.GameMode
import com.yukselis.engine.core.Rng
import com.yukselis.engine.core.WorldState
import com.yukselis.engine.i18n.Lang
import com.yukselis.engine.story.ActionRejected
import com.yukselis.engine.story.Effect
import com.yukselis.engine.story.StoryEngine
import com.yukselis.engine.system.Stat
import com.yukselis.engine.world.ChapterPhase
import com.yukselis.engine.world.HubAction
import com.yukselis.engine.world.Mission
import com.yukselis.engine.world.MissionKind
import com.yukselis.engine.world.Seed
import com.yukselis.engine.world.SkipFocus
import com.yukselis.engine.world.TimeSkip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/** Antrenman / zaman atlama (DECISIONS #142). */
class ZamanAtlamaTest {
    private val content = ContentLoader.load(DirectoryContentSource(File("../content")))
    private val engine = StoryEngine(content)
    private val lang = Lang.TR

    /** Bölüm 4 (Kule, tarihli: Gün 40) hazırlık evresinde, üste, kilitsiz, tohumsuz. */
    private fun chapter4(day: Int = 35, rngState: Long? = null): WorldState {
        var s = engine.newGame("T", GameMode.NORMAL, 3)
        s = engine.throughBriefing(engine.choose(s.copy(sceneId = "ilk_sabah", day = 1), "gune_basla", lang).state)
        s = engine.world.enterHub(s).state
        return s.copy(
            day = day, chapter = 4, chapterPhase = ChapterPhase.PREP, region = "kiyikent_liman", mission = null, seeds = emptyList(),
            rngState = rngState ?: s.rngState,
            dayState = s.dayState.copy(slotsLeft = 4, slotsTotal = 4, forcedLock = false, hand = emptyList()),
        )
    }

    /** Kesinti zarı tutmayan bir rastgelelik durumu (atlama sonuna kadar sürer). */
    private fun noCut(from: Long = 1): Long = generateSequence(from) { it + 1 }.first { !Rng(it).chance(TimeSkip.INTERRUPT_CHANCE) }

    private fun rejected(block: () -> Unit) {
        try { block(); fail("reddedilmeliydi") } catch (_: ActionRejected) {}
    }

    @Test
    fun `hazirlikta operasyona iki gun kalana kadar atlanir`() {
        val s = chapter4(35, noCut())
        assertEquals(3, engine.world.skipLimit(s))
        assertEquals(3, engine.view(s, lang).hub!!.skip!!.maxDays)
        rejected { engine.hubAction(s, HubAction.Skip(SkipFocus.Train(Stat.STR), 4), lang) }
        rejected { engine.hubAction(s, HubAction.Skip(SkipFocus.Train(Stat.STR), 0), lang) }
        val after = engine.hubAction(s, HubAction.Skip(SkipFocus.Train(Stat.STR), 3), lang).state
        assertEquals(38, after.day)
        assertEquals(ChapterPhase.PREP, after.chapterPhase)
        assertEquals("atlama_antrenman", after.dayState.currentEvent)
        assertEquals(3, after.variable(TimeSkip.DAYS_VAR))
        // Operasyona iki gün kaldı: atlama yok.
        assertEquals(0, engine.world.skipLimit(after.copy(sceneId = WorldState.HUB)))
    }

    @Test
    fun `brifingde ve gorevde atlama yok`() {
        val s = chapter4()
        assertEquals(0, engine.world.skipLimit(s.copy(chapterPhase = ChapterPhase.BRIEF)))
        val away = s.copy(mission = Mission(MissionKind.EXPLORE, "kordon", emptyList(), s.day), region = "kordon")
        assertEquals(0, engine.world.skipLimit(away))
        rejected { engine.hubAction(away, HubAction.Skip(SkipFocus.Gates, 1), lang) }
        // Meslek yokken meslek odağı, yoldaş değilken yoldaş odağı seçilemez.
        rejected { engine.hubAction(s.copy(player = s.player.copy(profession = null)), HubAction.Skip(SkipFocus.Craft, 1), lang) }
        rejected { engine.hubAction(s, HubAction.Skip(SkipFocus.Companion("yok_boyle_biri"), 1), lang) }
    }

    @Test
    fun `atlanan gunlerde gunluk gorev cezasiz ve seri korunur`() {
        val training = content.dailyTemplates.values.first { it.kind == "antrenman" }
        val s = chapter4(35, noCut()).copy(daily = DailyQuest(training.id, 35, steps = training.steps), dailyStreak = 5)
        val after = engine.hubAction(s, HubAction.Skip(SkipFocus.Gates, 3), lang).state
        assertFalse(after.penaltyPending)
        assertEquals(5, after.dailyStreak)
        // Günlük Görevin serbest puanı yok: artış yalnız seviye atlamalarından.
        val levels = after.player.level - s.player.level
        assertEquals(s.player.freePoints + levels * com.yukselis.engine.system.Progression.FREE_POINTS_PER_LEVEL, after.player.freePoints)
        assertNotNull(after.daily)
        assertEquals(38, after.daily!!.day)
    }

    @Test
    fun `kazanclar gunluk gorevden az`() {
        val s = chapter4()
        // XP: normal oynanışın en yavaş temposunun altında (BALANCE.md: Perde 1 ~0,3, Perde 2 ≥ 0,6, Perde 3+ ≥ 2 seviye/gün).
        assertTrue(TimeSkip.levelXpPercent(1) < 30)
        assertTrue(TimeSkip.levelXpPercent(2) < 60)
        assertTrue(TimeSkip.levelXpPercent(3) < 200)
        for (days in 1..30) {
            val fx = TimeSkip.effects(s, SkipFocus.Train(Stat.STR), days, "kiyikent_liman")
            val stat = fx.filterIsInstance<Effect.AddStat>().sumOf { it.delta }
            // Günlük Görev günde 1 serbest puan verir; atlama 3 günde 1 stat.
            assertTrue("gün $days", stat <= days / TimeSkip.STAT_DAYS)
            assertTrue(stat < days)
            assertEquals(TimeSkip.levelXpPercent(s.act) * days, fx.filterIsInstance<Effect.GainLevelXp>().sumOf { it.percent })
            val rel = TimeSkip.effects(s, SkipFocus.Companion("deniz_arman"), days, null).filterIsInstance<Effect.ShiftRelation>().single()
            assertTrue(rel.deltas.values.sum() <= 8)
        }
        // Kesirler birikir: üç ayrı birer günlük atlama da bir stat verir.
        var st = chapter4(35, noCut())
        val str = st.player.stats[Stat.STR]
        repeat(3) { i ->
            st = st.copy(sceneId = WorldState.HUB, day = 35, dayState = st.dayState.copy(slotsLeft = 4, forcedLock = false), rngState = noCut(100L * (i + 1)))
            st = engine.hubAction(st, HubAction.Skip(SkipFocus.Train(Stat.STR), 1), lang).state
        }
        assertEquals(str + 1, st.player.stats[Stat.STR])
        assertEquals(0, st.variable(TimeSkip.STAT_CARRY_VAR))
    }

    @Test
    fun `kesinti olasiligi yaklasik yuzde yirmi bes`() {
        var cut = 0
        val runs = 400
        for (i in 0 until runs) {
            val s = chapter4(35, 7_000L + i * 7919L)
            val after = engine.hubAction(s, HubAction.Skip(SkipFocus.Train(Stat.AGI), 3), lang).state
            val summary = after.dayState.currentEvent == "atlama_antrenman"
            if (!summary || after.variable(TimeSkip.DAYS_VAR) < 3) cut++
        }
        val rate = cut.toDouble() / runs
        assertTrue("kesinti oranı $rate", rate in 0.15..0.35)
    }

    @Test
    fun `vadesi gelen tohumda atlama durur ve tohum olayi baslar`() {
        val base = chapter4(35, noCut())
        val local = engine.world.director.deal(base, Rng(3), n = 20) { engine.world.director.fitsLocation(base, it) }
        assertTrue("üste uyan bir havuz olayı olmalı", local.isNotEmpty())
        val ev = local.first()
        val s = base.copy(seeds = listOf(Seed(ev, dueDay = 37)))
        val after = engine.hubAction(s, HubAction.Skip(SkipFocus.Train(Stat.WIL), 3), lang).state
        assertEquals(37, after.day)
        assertEquals(ev, after.dayState.currentEvent)
        assertTrue(after.seeds.none { it.eventId == ev })
        assertEquals(2, after.variable(TimeSkip.DAYS_VAR))
        // Olayın ilk paragrafı atlamanın özeti.
        val first = engine.view(after, lang).current.paragraphs.first()
        assertTrue(first, first.contains("37. güne kadar"))
    }
}
