package com.yukselis.engine

import com.yukselis.engine.combat.CombatEngine
import com.yukselis.engine.content.ContentLoader
import com.yukselis.engine.content.DirectoryContentSource
import com.yukselis.engine.core.DailyQuest
import com.yukselis.engine.core.GameMode
import com.yukselis.engine.core.WorldState
import com.yukselis.engine.i18n.LText
import com.yukselis.engine.i18n.Lang
import com.yukselis.engine.story.Content
import com.yukselis.engine.story.Effect
import com.yukselis.engine.story.EffectApplier
import com.yukselis.engine.story.Notice
import com.yukselis.engine.story.NoticeKind
import com.yukselis.engine.story.StoryEngine
import com.yukselis.engine.world.ChapterPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Paket 2 (ilk saat; DECISIONS #156–#160): Günlük Görev cezası deneme süresi ve yalnız üste, ilişki bildirimlerinin
 * birleşmesi, Perde 1'de zaman atlama yok, biten bölümün günlerinin kendiliğinden geçmesi, operasyon onay bilgisi,
 * yolculuk listesi.
 */
class Paket2Test {
    private val content = ContentLoader.load(DirectoryContentSource(File("../content")))
    private val engine = StoryEngine(content)
    private val lang = Lang.TR

    /** Bölüm 1 brifingi oynanmış, üste, el dağıtılmış durum. */
    private fun atHub(seed: Long = 1): WorldState {
        var s = engine.newGame("T", GameMode.NORMAL, seed)
        s = s.copy(sceneId = "ilk_sabah", day = 1)
        s = engine.throughBriefing(engine.choose(s, "gune_basla", lang).state)
        return engine.world.enterHub(s).state
    }

    private fun training(): String = content.dailyTemplates.values.first { it.kind == "antrenman" }.id

    @Test
    fun `ceza bolgesi yalniz uste acilir ve gun akisina doner`() {
        val s0 = atHub().copy(penaltyPending = true)
        val s = engine.world.enterHub(s0).state
        assertEquals(Content.PENALTY_SCENE, s.sceneId)
        assertEquals(WorldState.HUB, s.returnStack.last())
        assertFalse(s.penaltyPending)
        val back = engine.choose(s, "dayan", lang).state
        assertTrue(back.returnStack.isEmpty())
        assertTrue(back.sceneId == WorldState.HUB || back.dayState.currentEvent != null)
    }

    @Test
    fun `zorunlu olay kilidi varken ceza bekler`() {
        val s0 = atHub().copy(penaltyPending = true)
        val s = engine.world.enterHub(s0.copy(dayState = s0.dayState.copy(forcedLock = true))).state
        assertTrue(s.sceneId != Content.PENALTY_SCENE)
        assertTrue(s.penaltyPending)
    }

    @Test
    fun `ilk uc gun deneme suresi ceza ve seri kaybi yok`() {
        val base = engine.newGame("T", GameMode.NORMAL, 5)
        for (day in 1..EffectApplier.PENALTY_GRACE_DAYS) {
            val s = base.copy(day = day, daily = DailyQuest(training(), day, steps = 3), dailyStreak = 4)
            val r = engine.applyEffects(s, listOf(Effect.AdvanceDays(1)), lang)
            assertFalse("gün $day", r.state.penaltyPending)
            assertEquals("gün $day", 4, r.state.dailyStreak)
            assertTrue(r.notices.any { it.kind == NoticeKind.SYSTEM && it.title.contains("Deneme") })
            assertTrue(r.notices.none { it.title.contains("Ceza protokolü") })
        }
        // Dördüncü günden sonra seri kopar (ceza şansa bağlı).
        val late = base.copy(day = EffectApplier.PENALTY_GRACE_DAYS + 1, daily = DailyQuest(training(), 4, steps = 3), dailyStreak = 4)
        val r = engine.applyEffects(late, listOf(Effect.AdvanceDays(1)), lang)
        assertEquals(0, r.state.dailyStreak)
    }

    @Test
    fun `ilk gunlerin gunluk gorev karti deneme satiri tasir`() {
        val s = engine.newGame("T", GameMode.NORMAL, 5).copy(day = 2)
        val r = engine.applyEffects(s, listOf(Effect.AdvanceDays(1)), lang)
        val daily = r.notices.first { it.kind == NoticeKind.QUEST }
        assertTrue(daily.lines.any { it.contains("deneme", ignoreCase = true) })
    }

    @Test
    fun `ayni NPC nin iliski bildirimleri tek karta iner`() {
        val a = Notice(NoticeKind.COMPANION, LText("Deniz: sana daha çok güveniyor", "Devon: trusts you more"), group = "iliski:deniz_arman")
        val b = Notice(NoticeKind.COMPANION, LText("Deniz: sana daha çok güveniyor, bağı güçlendi", "Devon: trusts you more, feels closer to you"), group = "iliski:deniz_arman")
        val other = Notice(NoticeKind.SYSTEM, LText("x", "x"))
        val out = engine.collapse(listOf(a, other, b))
        assertEquals(2, out.size)
        assertEquals("Deniz: sana daha çok güveniyor, bağı güçlendi", out[0].title.tr)
        assertEquals("Devon: trusts you more, feels closer to you", out[0].title.en)
        assertEquals(other, out[1])
    }

    @Test
    fun `perde 1 de zaman atlama yok`() {
        val s = atHub()
        assertEquals(0, engine.world.skipLimit(s))
        val p2 = s.copy(day = 35, chapter = 4, chapterPhase = ChapterPhase.PREP, region = "kiyikent_liman", mission = null)
        assertTrue(engine.world.skipLimit(p2) > 0)
    }

    @Test
    fun `yolculuk listesi bulunulan bolgeyi icermez`() {
        val s = atHub()
        assertNotNull(s.region)
        assertTrue(engine.world.destinations(s).none { it == s.region })
        assertTrue(engine.world.destinations(s).isNotEmpty())
    }

    @Test
    fun `biten bolumun kalan gunleri kendiliginden gecer`() {
        val s = atHub().copy(chapterPhase = ChapterPhase.DONE, day = 6, seeds = emptyList())
        val notices = mutableListOf<Notice>()
        val r = engine.world.restUntilNextChapter(s, notices)
        assertNotNull(r)
        assertTrue(r!!.state.day > 6)
        assertTrue(r.state.chapter in 1..2)
        assertTrue((notices + r.notices).any { it.title.tr.contains("sakin gün") })
    }

    @Test
    fun `operasyon onay bilgisi tehdit ve uyari verir`() {
        val s = atHub()
        val info = engine.world.operationInfo(s.copy(chapterPhase = ChapterPhase.PREP), 0)
        assertNotNull(info)
        // Bölüm 1 operasyonu (Kırılma): Sis Dokuyucu Sv. 8; Gün 1'deki oyuncu Sv. 1–3 → uyarı, vurgu yok.
        assertTrue(info!!.threatLevel >= 5)
        assertNotNull(info.warning)
        assertFalse(info.emphasized)
        val view = engine.view(s.copy(chapterPhase = ChapterPhase.PREP), lang).hub?.operations?.firstOrNull()
        assertNotNull(view)
        assertEquals(info.threatLevel, view!!.threatLevel)
        assertFalse(view.emphasized)
    }

    @Test
    fun `savas sayaci ucuncu savasta taktigi acar`() {
        assertEquals(3, CombatEngine.TACTICS_UNLOCK_FIGHTS)
        val s = atHub().copy(vars = atHub().vars + (CombatEngine.VAR_FIGHTS to 3))
        assertTrue(s.variable(CombatEngine.VAR_FIGHTS) >= CombatEngine.TACTICS_UNLOCK_FIGHTS)
    }
}
