package com.yukselis.engine

import com.yukselis.engine.content.ContentLoader
import com.yukselis.engine.content.DirectoryContentSource
import com.yukselis.engine.core.GameMode
import com.yukselis.engine.core.WorldState
import com.yukselis.engine.i18n.Lang
import com.yukselis.engine.story.StoryEngine
import com.yukselis.engine.world.ChapterPhase
import com.yukselis.engine.world.HubAction
import com.yukselis.engine.world.Mission
import com.yukselis.engine.world.MissionKind
import com.yukselis.engine.world.WorldEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Saha kampı sahneleri (DECISIONS #144): keşifte, o bölgede, yoldaşla, gün sonundaki kampta. */
class SahaKampiTest {
    private val real = ContentLoader.load(DirectoryContentSource(File("../content")))

    /** Gerçek içeriğe Kordon'a etiketli, Deniz'li bir kamp sahnesi eklenir (var olan bir sahneyi kullanır). */
    private val content = run {
        val template = real.events.values.first { it.id == "sb6_deniz_liste" }
        val camp = template.copy(id = "test_kamp", camp = true, seedOnly = false, location = listOf("kordon"), regions = emptyList(),
            condition = null, dayMin = 0, dayMax = 400, acts = emptyList())
        real.copy(events = real.events + (camp.id to camp))
    }
    private val engine = StoryEngine(content)
    private val lang = Lang.TR

    private fun inField(
        companion: Boolean,
        mission: MissionKind? = MissionKind.EXPLORE,
        region: String = "kordon",
        trust: Int = 0,
        known: Boolean = true,
        marks: Set<String> = emptySet(),
        status: com.yukselis.engine.npc.NpcStatus = com.yukselis.engine.npc.NpcStatus.ACTIVE,
    ): WorldState {
        var s = engine.newGame("T", GameMode.NORMAL, 5)
        s = engine.throughBriefing(engine.choose(s.copy(sceneId = "ilk_sabah", day = 1), "gune_basla", lang).state)
        s = engine.world.enterHub(s).state
        val deniz = s.npcs.getValue("deniz_arman").copy(companion = companion, status = status, known = known, marks = marks,
            relation = com.yukselis.engine.npc.Relation(trust = trust))
        return s.copy(
            day = 70, chapter = 6, chapterPhase = ChapterPhase.PREP, region = region, seeds = emptyList(),
            mission = mission?.let { Mission(it, region, emptyList(), 70) },
            npcs = s.npcs + ("deniz_arman" to deniz),
            dayState = s.dayState.copy(slotsLeft = 0, forcedLock = false, hand = emptyList(), seen = s.dayState.seen - "test_kamp"),
        )
    }

    @Test
    fun `keşifte yoldaşla kamp kurunca sahne oynanır, sonra gece geçer`() {
        val s = inField(companion = true)
        val camp = engine.hubAction(s, HubAction.Camp, lang).state
        assertEquals("test_kamp", camp.dayState.currentEvent)
        assertEquals(70, camp.day)
        assertEquals(1, camp.variable(WorldEngine.CAMP_PENDING_VAR))
        // Sahne bitince (akışa dönüş) gece işlenir.
        val back = engine.world.enterHub(camp).state
        assertEquals(71, back.day)
        assertEquals(0, back.variable(WorldEngine.CAMP_PENDING_VAR))
        assertEquals(MissionKind.EXPLORE, back.mission?.kind)
        // Bir kez oynanır.
        val again = engine.hubAction(back.copy(sceneId = WorldState.HUB, dayState = back.dayState.copy(slotsLeft = 0)), HubAction.Camp, lang).state
        assertNotEquals("test_kamp", again.dayState.currentEvent)
    }

    @Test
    fun `güvenilen müttefik yoldaş olmasa da kampa gelir`() {
        val camp = engine.hubAction(inField(companion = false, trust = 10), HubAction.Camp, lang).state
        assertEquals("test_kamp", camp.dayState.currentEvent)
    }

    @Test
    fun `güvensiz, tanışılmamış, hain, giden ya da başka bölgede, üsteyken kamp sahnesi gelmez`() {
        val cases = listOf(
            inField(companion = false, trust = 9),
            inField(companion = false, trust = 50, known = false),
            inField(companion = false, trust = 50, marks = setOf("hain")),
            inField(companion = false, trust = 50, status = com.yukselis.engine.npc.NpcStatus.GONE),
            inField(companion = true, status = com.yukselis.engine.npc.NpcStatus.GONE),
            inField(companion = true, region = "eski_sehir"),
            inField(companion = true, mission = null),
        )
        for (s in cases) {
            val after = engine.hubAction(s, HubAction.Camp, lang).state
            assertNotEquals("test_kamp", after.dayState.currentEvent)
            assertEquals(0, after.variable(WorldEngine.CAMP_PENDING_VAR))
        }
    }

    @Test
    fun `kamp sahnesi havuzdan ve fırsat elinden gelmez`() {
        val s = inField(companion = true).copy(dayState = inField(true).dayState.copy(slotsLeft = 3))
        val pool = engine.world.director.candidates(s).map { it.first.id }
        assertFalse("test_kamp" in pool)
        assertTrue(engine.world.director.campScenes(s).any { it.id == "test_kamp" })
    }
}
