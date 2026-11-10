package com.yukselis.engine

import com.yukselis.engine.content.ContentLoader
import com.yukselis.engine.content.DirectoryContentSource
import com.yukselis.engine.core.GameMode
import com.yukselis.engine.core.WorldState
import com.yukselis.engine.i18n.Lang
import com.yukselis.engine.story.ActionRejected
import com.yukselis.engine.story.Effect
import com.yukselis.engine.core.PromptKind
import com.yukselis.engine.story.StoryEngine
import com.yukselis.engine.system.Progression
import com.yukselis.engine.world.SkipFocus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Paket 4 (sistem çıkarma; DECISIONS #166–#169): meslek/üretim ve Kredi kapalı, Yozlaşma Dikkat'e katıldı,
 * Ordu ve Bilgi günlüğü ilk kullanımda kalıcı açılır. Kod duruyor; anahtarlar `Progression.*_ENABLED`.
 */
class Paket4Test {
    private val content = ContentLoader.load(DirectoryContentSource(File("../content")))
    private val engine = StoryEngine(content)
    private val lang = Lang.TR

    private fun fresh(): WorldState = engine.newGame("T", GameMode.NORMAL, 7)

    private fun apply(state: WorldState, vararg effects: Effect) = engine.applyEffects(state, effects.toList(), lang)

    @Test
    fun `anahtarlar kapali`() {
        assertFalse(Progression.PROFESSIONS_ENABLED)
        assertFalse(Progression.CREDITS_ENABLED)
        assertFalse(Progression.CORRUPTION_ENABLED)
    }

    @Test
    fun `meslek teklifi gelmez ve uretim reddedilir`() {
        var s = apply(fresh(), Effect.GainXp(Progression.xpToNext(1) * 40)).state
        assertTrue("seviye ${s.player.level}", s.player.level >= Progression.PROFESSION_OFFER_LEVEL)
        assertTrue(s.prompts.none { it.kind == PromptKind.PROFESSION })
        s = apply(s, Effect.OfferProfession).state
        assertTrue(s.prompts.none { it.kind == PromptKind.PROFESSION })
        assertEquals(null, engine.bag(s, lang).professionName)
        assertTrue(engine.bag(s, lang).recipes.isEmpty())
        assertTrue(engine.world.skipFoci(s).none { it is SkipFocus.Craft })
        try {
            engine.craft(s, "kucuk_iyilesme_iksiri", lang)
            throw AssertionError("üretim kapalıyken kabul edilmemeli")
        } catch (_: ActionRejected) {
        }
    }

    @Test
    fun `eski kayittaki meslek arayuze cikmaz`() {
        val s = fresh().let { it.copy(player = it.player.copy(profession = com.yukselis.engine.core.ProfessionState("simyaci"))) }
        assertEquals(null, engine.bag(s, lang).professionName)
        assertTrue(engine.world.skipFoci(s).none { it is SkipFocus.Craft })
    }

    @Test
    fun `kredi etkisi bakiyeyi degistirmez ve bildirim uretmez`() {
        val r = apply(fresh(), Effect.AddCredits(25))
        assertEquals(0, r.state.player.credits)
        assertTrue(r.notices.isEmpty())
        assertEquals(0, engine.bag(r.state, lang).credits)
    }

    @Test
    fun `yozlasma etkisi dikkate yazilir ve yozlasma sifir kalir`() {
        val s0 = fresh()
        val r = apply(s0, Effect.ShiftCorruption(7))
        assertEquals(s0.attention + 7, r.state.attention)
        assertEquals(0, r.state.corruption)
    }

    @Test
    fun `ceza yetenegi dikkati on artirir`() {
        val s0 = fresh()
        val r = apply(s0, Effect.GrantSkill("ruh_emme"))
        assertEquals(s0.attention + com.yukselis.engine.story.EffectApplier.PENALTY_ATTENTION, r.state.attention)
        assertEquals(0, r.state.corruption)
    }

    @Test
    fun `iz dikkat artirir`() {
        val trait = content.traits.values.first { it.corruption > 0 }
        val s0 = fresh()
        val r = apply(s0, Effect.PlayerTrait(trait.id))
        assertEquals(s0.attention + trait.corruption, r.state.attention)
        assertEquals(0, r.state.corruption)
    }

    @Test
    fun `oz kristali kosulu`() {
        val s = fresh().let { it.copy(player = it.player.copy(essenceCrystals = 3)) }
        assertTrue(com.yukselis.engine.story.Condition.CrystalsAtLeast(3).test(s))
        assertFalse(com.yukselis.engine.story.Condition.CrystalsAtLeast(4).test(s))
    }

    @Test
    fun `ilk bilgi ve ilk birlik bolumleri kalici acar`() {
        val s0 = fresh()
        val st0 = engine.status(s0, lang)
        assertFalse(st0.showKnowledge)
        assertFalse(st0.showArmy)
        val knowledge = content.knowledge.keys.first()
        val unit = content.army.keys.first()
        var s = apply(s0, Effect.Learn(knowledge), Effect.AddArmy(unit, 2)).state
        assertEquals(1, s.variable(WorldState.UI_KNOWLEDGE_VAR))
        assertEquals(1, s.variable(WorldState.UI_ARMY_VAR))
        assertTrue(engine.status(s, lang).showKnowledge)
        assertTrue(engine.status(s, lang).showArmy)
        // Birlikler biterse bile Ordu bölümü kapanmaz (kalıcı bayrak).
        s = apply(s, Effect.AddArmy(unit, -2)).state
        assertTrue(engine.status(s, lang).showArmy)
    }

    @Test
    fun `icerikte kredi yozlasma ve meslek anahtari kalmadi`() {
        val yaml = File("../content").walkTopDown().filter { it.extension == "yaml" && it.name != "system.yaml" }.toList()
        val bad = mutableListOf<String>()
        val effectKey = Regex("""^\s*(- )?(kredi|yozlasma|yozlasma_min|meslek_teklifi): """)
        val inlineKey = Regex("""[{,]\s*(kredi|yozlasma|yozlasma_min|meslek_teklifi): """)
        yaml.forEach { f ->
            f.readLines().forEachIndexed { i, line ->
                if (effectKey.containsMatchIn(line) || inlineKey.containsMatchIn(line)) bad += "${f.name}:${i + 1}"
            }
        }
        assertTrue("kalan anahtarlar: $bad", bad.isEmpty())
    }
}
