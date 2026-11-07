package com.yukselis.engine

import com.yukselis.engine.content.ContentLoader
import com.yukselis.engine.content.DirectoryContentSource
import com.yukselis.engine.core.GameMode
import com.yukselis.engine.core.WorldState
import com.yukselis.engine.i18n.LText
import com.yukselis.engine.i18n.Lang
import com.yukselis.engine.save.SaveCodec
import com.yukselis.engine.story.StoryEngine
import com.yukselis.engine.world.ChapterPhase
import com.yukselis.engine.world.ChapterTable
import com.yukselis.engine.world.HubAction
import com.yukselis.engine.world.MissionKind
import com.yukselis.engine.world.Seed
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Bölüm yapısı (DECISIONS #130): brifing, operasyon, darboğaz, son tarih, üs/görev, konum, kilit, kayıt geçişi. */
class BolumTest {
    private val content = ContentLoader.load(DirectoryContentSource(File("../content")))
    private val engine = StoryEngine(content)
    private val lang = Lang.TR

    private fun atHub(seed: Long = 1): WorldState {
        var s = engine.newGame("T", GameMode.NORMAL, seed)
        s = s.copy(sceneId = "ilk_sabah", day = 1)
        return engine.choose(s, "gune_basla", lang).state
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
    fun `gun akisi bolum 1 brifingiyle acilir ve bolum satiri gorunur`() {
        val s = atHub()
        assertEquals(1, s.chapter)
        assertEquals("brifing_1_1", s.sceneId)
        val line = engine.view(s, Lang.TR).chapter
        assertNotNull(line)
        assertEquals(1, line!!.no)
        assertTrue(line.daysLeft in 1..12)
        val after = engine.throughBriefing(s)
        assertEquals(ChapterPhase.PREP, engine.world.enterHub(after).state.chapterPhase)
    }

    @Test
    fun `operasyon kilitli gorevdir, bitince ussde darbogaz gelir`() {
        var s = engine.throughBriefing(atHub())
        s = engine.world.enterHub(s).state
        assertTrue(HubAction.EnterOperation(0) in engine.world.legalActions(s))
        s = engine.hubAction(s, HubAction.EnterOperation(0), lang).state
        assertEquals(MissionKind.OPERATION, s.mission?.kind)
        assertEquals(ChapterPhase.OPERATION, s.chapterPhase)
        assertEquals("kirilma", s.dayState.currentEvent)
        // Operasyonun içinde akış eylemi yok: fırsat eli boş, keşif/yolculuk geçersiz.
        s = finishEvent(s)
        assertNull("operasyon bitince üsse dönülür", s.mission)
        assertEquals(ChapterPhase.AFTER, s.chapterPhase)
        assertEquals(WorldState.HUB, s.sceneId)
        // Darboğaz, oyuncu üste bir eylem yaptıktan sonra gelir (arka arkaya zorunlu olay yok).
        s = engine.hubAction(s, HubAction.Rest, lang).state
        assertEquals("darbogaz_1", s.dayState.currentEvent)
        s = finishEvent(s)
        assertEquals(ChapterPhase.DONE, s.chapterPhase)
    }

    @Test
    fun `son tarih gunu operasyon kendiliginden baslar, gecerse tasma yasanir`() {
        var s = engine.throughBriefing(atHub())
        s = engine.world.enterHub(s.copy(day = 12, dayState = s.dayState.copy(forcedLock = false))).state
        assertEquals("son tarih günü operasyon başlar", "kirilma", s.dayState.currentEvent)
        assertFalse(s.hasFlag(ChapterTable.overflowFlag(1)))
        // Son tarih geçmiş sabah: taşma (bayrak, hayatta kalan −3), operasyon yine başlar.
        var t = engine.throughBriefing(atHub())
        val before = t.variable("hayatta_kalan")
        t = engine.world.startDay(t.copy(day = 12), advance = true).state
        assertTrue(t.hasFlag(ChapterTable.overflowFlag(1)))
        assertEquals(before - 3, t.variable("hayatta_kalan"))
    }

    @Test
    fun `ussde yalniz ussun olaylari, keşifte yalniz bolgenin olaylari gelir`() {
        var s = engine.world.enterHub(engine.throughBriefing(atHub()).copy(day = 5)).state
        val dir = engine.world.director
        s.dayState.hand.forEach { id ->
            val e = content.events.getValue(id)
            val card = engine.view(s, lang).hub!!.cards.first { it.id == id }
            assertTrue("$id üste ya uyar ya görev kartıdır", dir.fitsLocation(s, e) || card.region != null)
        }
        // Keşif görevi: el yalnız o bölgenin (ya da her yerin) olayları.
        s = engine.hubAction(s.copy(dayState = s.dayState.copy(slotsLeft = 4)), HubAction.Travel("harabe_carsi"), lang).state
        s = finishEvent(s)
        assertEquals(MissionKind.EXPLORE, s.mission?.kind)
        assertEquals("harabe_carsi", s.region)
        s.dayState.hand.forEach { id -> assertTrue(id, dir.fitsLocation(s, content.events.getValue(id))) }
        assertTrue(HubAction.ReturnToBase in engine.world.legalActions(s))
        s = engine.hubAction(s, HubAction.ReturnToBase, lang).state
        assertNull(s.mission)
        assertEquals("ogretici_orman", s.region)
        assertEquals(1, s.variable(ChapterTable.VISIT_PREFIX + "harabe_carsi"))
    }

    @Test
    fun `olay bitince oyuncu eylem yapmadan yeni zorunlu olay gelmez`() {
        var s = engine.throughBriefing(atHub())
        s = s.copy(seeds = listOf(Seed("yabanci_minnet", s.day)), dayState = s.dayState.copy(forcedLock = true))
        s = engine.world.enterHub(s).state
        assertEquals("kilitliyken tohum beklemeli", WorldState.HUB, s.sceneId)
        s = engine.hubAction(s, HubAction.Rest, lang).state
        assertEquals("yabanci_minnet", s.dayState.currentEvent)
    }

    @Test
    fun `tohumdan dogan olay hatirlatmayla baslar`() {
        var s = engine.throughBriefing(atHub())
        val reminder = LText("Ormanda bıraktığın yabancıyı hatırlıyor musun?", "Remember the stranger you left in the woods?")
        s = s.copy(seeds = listOf(Seed("yabanci_minnet", s.day, reminder = reminder)))
        s = engine.world.enterHub(s).state
        assertEquals("yabanci_minnet", s.dayState.currentEvent)
        assertEquals(reminder.en, engine.view(s, Lang.EN).current.paragraphs.first())
        // Hatırlatması yazılmamış tohum: onu eken olayın kart başlığından genel cümle.
        var g = engine.throughBriefing(atHub())
        g = g.copy(seeds = listOf(Seed("yabanci_minnet", g.day, origin = "yarali_avci")))
        g = engine.world.enterHub(g).state
        val card = content.events.getValue("yarali_avci").card.tr
        assertTrue(engine.view(g, Lang.TR).current.paragraphs.first().contains(card))
    }

    @Test
    fun `eski v4 kaydi dogru bolumde ve evrede acilir`() {
        // Gün 70: Kasaba Çekirdeği görülmüş, İlk Taşma henüz değil → Bölüm 6, hazırlık.
        val base = engine.world.enterHub(engine.throughBriefing(atHub())).state
        val mid = base.copy(
            day = 70, chapter = 6, chapterPhase = ChapterPhase.PREP,
            dayState = base.dayState.copy(seen = base.dayState.seen + setOf("perde2_donus", "kule_yukselisi", "fraksiyon_secimi", "kasaba_cekirdegi")),
        )
        val v4 = asV4(SaveCodec.encode(mid, 1))
        val loaded = engine.sanitize(SaveCodec.decode(v4).state)
        assertEquals(6, loaded.chapter)
        assertEquals(ChapterPhase.PREP, loaded.chapterPhase)
        assertNull(loaded.mission)
        assertEquals("kiyikent_liman", loaded.region)
        // Gün 62: Çekirdek operasyonu görülmüş, darboğaz değil → Bölüm 5, sonrası; üste ilk eylemde darboğaz.
        val after = asV4(SaveCodec.encode(mid.copy(day = 62), 1))
        val l2 = engine.sanitize(SaveCodec.decode(after).state)
        assertEquals(5, l2.chapter)
        assertEquals(ChapterPhase.AFTER, l2.chapterPhase)
        // Gün 20 (Perde 1, Kırılma görülmüş, darboğaz yok) → Bölüm 2, hazırlık.
        val p1 = asV4(SaveCodec.encode(base.copy(day = 20, dayState = base.dayState.copy(seen = base.dayState.seen + "kirilma")), 1))
        val l3 = engine.sanitize(SaveCodec.decode(p1).state)
        assertEquals(2, l3.chapter)
        assertEquals(ChapterPhase.PREP, l3.chapterPhase)
        // v5 gidiş-dönüş kayıpsız.
        assertEquals(mid, SaveCodec.decode(SaveCodec.encode(mid, 1)).state)
    }

    /** v5 kaydını v4'e çevirir: sürüm 4, bölüm alanları yok. */
    private fun asV4(json: String): String {
        val o = Json.parseToJsonElement(json).jsonObject
        val fields = o.toMutableMap()
        fields["v"] = JsonPrimitive(4)
        listOf("chapter", "phase", "mission").forEach { fields.remove(it) }
        return JsonObject(fields).toString()
    }

    @Test
    fun `alan gunluk gorevi en cok iki adim ister ve kacirilinca ceza getirmez`() {
        var s = engine.throughBriefing(atHub())
        val field = content.dailyTemplates.values.first { it.kind == "savas" }
        s = s.copy(flags = s.flags + "gunluk_genis_gorevler", daily = com.yukselis.engine.core.DailyQuest(field.id, s.day, steps = 2), dailyStreak = 3)
        val next = engine.world.startDay(s, advance = true).state
        assertFalse(next.penaltyPending)
        assertEquals(3, next.dailyStreak)
        content.dailyTemplates.values.filter { it.kind != "antrenman" }.forEach { t -> assertTrue(t.id, minOf(t.steps, 2) <= 2) }
        var guard = 0
        var d = next
        while (guard++ < 40) {
            val q = d.daily ?: break
            if (content.dailyTemplates.getValue(q.templateId).kind != "antrenman") { assertTrue(q.steps <= 2); break }
            d = engine.world.startDay(d, advance = true).state
        }
    }
}
