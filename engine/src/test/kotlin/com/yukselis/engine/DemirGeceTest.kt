package com.yukselis.engine

import com.yukselis.engine.content.ContentLoader
import com.yukselis.engine.content.DirectoryContentSource
import com.yukselis.engine.core.GameMode
import com.yukselis.engine.core.WorldState
import com.yukselis.engine.i18n.Lang
import com.yukselis.engine.npc.NpcEngine
import com.yukselis.engine.npc.Relation
import com.yukselis.engine.story.Effect
import com.yukselis.engine.story.StoryEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Demir Gece (DECISIONS #150): uyarı önce gelir, hazırlık İskele yolunu açar, bağlı olunanın ölümü yas başlatır. */
class DemirGeceTest {
    private val content = ContentLoader.load(DirectoryContentSource(File("../content")))
    private val engine = StoryEngine(content)
    private val lang = Lang.TR

    private fun state(vararg flags: String): WorldState {
        val s = engine.newGame("T", GameMode.NORMAL, 4)
        return s.copy(day = 250, chapter = 12, flags = s.flags + "siginak_1_goruldu" + flags.toSet())
    }

    @Test
    fun `Demir Gece uyarı görülmeden gelmez`() {
        val darbe = content.events.getValue("elif_lonca_darbesi")
        assertFalse(darbe.condition!!.test(state()))
        assertTrue(darbe.condition!!.test(state("demir_gece_uyarildi")))
        // Uyarı ana yay düğümüdür ve görülünce bir daha gelmez.
        val uyari = content.events.getValue("elif_demir_gece_uyari")
        assertEquals(236, uyari.mainArcDay)
        assertFalse(uyari.condition!!.test(state("demir_gece_uyarildi")))
    }

    @Test
    fun `hazırlık İskele yolunda Gümrük'e yetişmeyi açar`() {
        val iskele = content.scenes.getValue("elif_lonca_darbesi_iskele")
        val nobet = iskele.choices.first { it.id == "nobetle" }
        assertFalse(nobet.showWhen!!.test(state("demir_gece_uyarildi")))
        assertTrue(nobet.showWhen!!.test(state("demir_gece_uyarildi", "demir_gece_nobet")))
        assertEquals("elif_lonca_darbesi_hastane", nobet.next)
        // Hazırlıksız ve güçsüz yolda kalan seçenekler ölümdür; ölüm hep görünen bir seçimden gelir.
        val olum = iskele.choices.filter { c -> c.effects.any { it is Effect.KillNpc } }
        assertTrue(olum.isNotEmpty() && olum.all { it.requires == null && it.showWhen == null })
    }

    @Test
    fun `yoldaş olmasa da bağlı olunanın ölümü yas başlatır`() {
        var s = engine.newGame("T", GameMode.NORMAL, 4)
        s = engine.world.enterHub(s).state
        val elif = s.npcs.getValue("elif_sari").copy(companion = false, relation = Relation(bond = 35))
        s = s.copy(npcs = s.npcs + ("elif_sari" to elif))
        val after = engine.applyEffects(s, listOf(Effect.KillNpc("elif_sari")), lang).state
        assertTrue(after.variable(NpcEngine.VAR_MOURNING) > 0)
    }
}
