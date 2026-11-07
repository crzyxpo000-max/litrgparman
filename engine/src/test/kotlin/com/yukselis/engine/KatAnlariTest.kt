package com.yukselis.engine

import com.yukselis.engine.content.ContentLoader
import com.yukselis.engine.content.DirectoryContentSource
import com.yukselis.engine.core.GameMode
import com.yukselis.engine.core.WorldState
import com.yukselis.engine.i18n.Lang
import com.yukselis.engine.story.EffectApplier
import com.yukselis.engine.story.StoryEngine
import com.yukselis.engine.world.ChapterPhase
import com.yukselis.engine.world.Mission
import com.yukselis.engine.world.MissionKind
import com.yukselis.engine.world.WorldEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Kule kat anları (DECISIONS #148): ilk temizleyen önceliği, günde bir, 30 gün tekrar etmeme. */
class KatAnlariTest {
    private val real = ContentLoader.load(DirectoryContentSource(File("../content")))
    private val content = run {
        val t = real.events.getValue(WorldEngine.FAST_EVENT)
        val an = t.copy(id = "test_kule_an", tags = listOf(WorldEngine.MOMENT_TAG), condition = null)
        val ilk = t.copy(id = WorldEngine.FIRST_CLEAR_PREFIX + "35", tags = emptyList(), repeatable = false, condition = null)
        // Gerçek içeriğin kat anları bu testte yok: yalnız sentetik olaylar.
        val events = real.events.filterValues { WorldEngine.MOMENT_TAG !in it.tags && !it.id.startsWith(WorldEngine.FIRST_CLEAR_PREFIX) }
        real.copy(events = events + (an.id to an) + (ilk.id to ilk))
    }
    private val engine = StoryEngine(content)
    private val lang = Lang.TR
    private val tower = content.tower!!

    private fun atTower(day: Int = 250, rng: Long = 1): WorldState {
        var s = engine.newGame("T", GameMode.NORMAL, 9)
        s = engine.throughBriefing(engine.choose(s.copy(sceneId = "ilk_sabah", day = 1), "gune_basla", lang).state)
        s = engine.world.enterHub(s).state
        return s.copy(
            day = day, chapter = 12, chapterPhase = ChapterPhase.PREP, region = tower.region, seeds = emptyList(), rngState = rng,
            mission = Mission(MissionKind.EXPLORE, tower.region, emptyList(), day),
            flags = s.flags + "kule_acik", vars = s.vars + (EffectApplier.TOWER_VAR to 40),
            dayState = s.dayState.copy(seen = s.dayState.seen - "test_kule_an" - "kule_ilk_35"),
        )
    }

    /** Zarı tutturan bir rastgelelik durumu bulup anı döndürür. */
    private fun hit(s: WorldState): Pair<WorldState, String> =
        (1L..500L).firstNotNullOf { r -> engine.world.towerMoment(s.copy(rngState = r), 41..41) }

    @Test
    fun `ilk temizleyen katı zar beklemeden önce gelir`() {
        val (st, id) = engine.world.towerMoment(atTower(), 31..39)!!
        assertEquals("kule_ilk_35", id)
        assertEquals(251, st.variable(WorldEngine.MOMENT_SEEN_PREFIX + id))
        // Görüldükten sonra bir daha gelmez.
        val seen = atTower().let { it.copy(dayState = it.dayState.copy(seen = it.dayState.seen + "kule_ilk_35")) }
        val again = (1L..50L).mapNotNull { r -> engine.world.towerMoment(seen.copy(rngState = r), 31..39)?.second }
        assertTrue(again.none { it == "kule_ilk_35" })
    }

    @Test
    fun `olasılık yaklaşık yüzde kırk, günde en çok bir an`() {
        val n = 400
        val hits = (1L..n.toLong()).count { r -> engine.world.towerMoment(atTower(rng = r * 7919), 41..41) != null }
        assertTrue("isabet ${hits.toDouble() / n}", hits.toDouble() / n in 0.3..0.5)
        val (st, _) = hit(atTower())
        assertNull((1L..200L).firstNotNullOfOrNull { r -> engine.world.towerMoment(st.copy(rngState = r), 42..42) })
    }

    @Test
    fun `aynı varyasyon otuz gün tekrar etmez`() {
        val (st, id) = hit(atTower(day = 250))
        assertEquals("test_kule_an", id)
        val day279 = st.copy(day = 279)
        assertNull((1L..200L).firstNotNullOfOrNull { r -> engine.world.towerMoment(day279.copy(rngState = r), 42..42) })
        val day280 = st.copy(day = 280)
        assertNotNull((1L..200L).firstNotNullOfOrNull { r -> engine.world.towerMoment(day280.copy(rngState = r), 42..42) })
    }
}
