package com.yukselis.engine

import com.yukselis.engine.combat.CombatSpec
import com.yukselis.engine.content.ContentLoader
import com.yukselis.engine.content.DirectoryContentSource
import com.yukselis.engine.core.ChronicleEntry
import com.yukselis.engine.core.GameMode
import com.yukselis.engine.core.SkillState
import com.yukselis.engine.i18n.Lang
import com.yukselis.engine.story.StoryEngine
import com.yukselis.engine.system.ClassKind
import com.yukselis.engine.system.Rarity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Paket 7 testleri:
 * - Boss faz eşikleri, analiz sonrası zaaf etiketi ve yetenek etiketleri (`CombatView`, UI_REVIEW E4)
 * - Bölüm kapanışında kronik özeti ve sonraki bölüm adı (`ChapterLine`, UI_REVIEW E3)
 * - Kalıcı Arşiv / Kodeks (`StoryEngine.archive`, `unlockedHiddenClasses`, `unlockedUniqueSkills`)
 */
class Paket7Test {
    private val content by lazy { ContentLoader.load(DirectoryContentSource(File("../content"))) }
    private val engine by lazy { StoryEngine(content) }

    @Test
    fun `boss faz esikleri ve analiz sonrasi zaaf etiketi savas gorunumunde gelir`() {
        val bossDef = content.enemies.values.first { it.boss && it.phases.isNotEmpty() }
        val weakEnemy = content.enemies.values.first { !it.boss && it.weakness != null }
        val taggedSkill = content.skills.values.first { it.tags.isNotEmpty() && it.type == com.yukselis.engine.system.SkillType.ACTIVE }

        val s0 = engine.newGame("Arda", GameMode.NORMAL, seed = 101L)
            .let { it.copy(player = it.player.copy(skills = listOf(SkillState(taggedSkill.id)))) }
        val started = engine.startCombat(s0, CombatSpec(listOf(bossDef.id, weakEnemy.id), win = "son"), Lang.TR).state
        val before = engine.combatView(started, Lang.TR)!!

        val bossView = before.enemies.first { it.boss }
        assertEquals(bossDef.phases.map { it.threshold }, bossView.phaseThresholds)
        // Analiz edilmeden zaaf etiketi açılmaz.
        val weakBefore = before.enemies.first { !it.boss }
        assertNull(weakBefore.weaknessKey)

        // Analiz edildikten sonra zaaf etiketi ve yetenek etiketleri görünür.
        val analyzed = started.copy(combat = started.combat!!.copy(enemies = started.combat!!.enemies.map { it.copy(analyzed = true) }))
        val after = engine.combatView(analyzed, Lang.TR)!!
        assertEquals(weakEnemy.weakness, after.enemies.first { !it.boss }.weaknessKey)
        assertEquals(taggedSkill.tags, after.skills.first { it.id == taggedSkill.id }.tags)
    }

    @Test
    fun `bolum kapanis satirinda bolum kronigi ve sonraki bolum adi yer alir`() {
        val ch1 = content.chapters.chapters.first()
        val ch2 = content.chapters.next(ch1.no)!!
        val lastCh = content.chapters.chapters.last()

        val choiceWithChronicle = content.events.values.firstNotNullOf { ev ->
            val sc = content.scenes[ev.entry] ?: return@firstNotNullOf null
            val ch = sc.choices.firstOrNull { it.chronicle != null } ?: return@firstNotNullOf null
            Triple(ev.id, sc.id, ch.id)
        }
        val s1 = engine.newGame("Arda", GameMode.NORMAL, seed = 55L).copy(
            chapter = ch1.no,
            day = ch1.end,
            chronicle = listOf(
                ChronicleEntry(ch1.open + 1, choiceWithChronicle.first, choiceWithChronicle.second, choiceWithChronicle.third, 20),
                ChronicleEntry(ch2.end, choiceWithChronicle.first, choiceWithChronicle.second, choiceWithChronicle.third, 50),
            ),
        )
        val line1 = engine.chapterLine(s1, Lang.TR)!!
        assertEquals(1, line1.highlights.size)
        assertNotNull(line1.nextTitle)
        assertTrue(line1.nextTitle!!.isNotBlank())

        val sLast = s1.copy(chapter = lastCh.no, day = lastCh.end)
        val lineLast = engine.chapterLine(sLast, Lang.EN)!!
        assertTrue(lineLast.isFinal)
        assertNull(lineLast.nextTitle)
    }

    @Test
    fun `arsiv kilitli girdileri gizler acilanlari iki dilde gosterir`() {
        val endingId = content.endings.endings.keys.first()
        val archetypeId = content.endings.archetypes.first().id
        val hiddenClass = content.classes.values.first { it.kind == ClassKind.HIDDEN }
        val hiddenEvo = content.classes.values.firstOrNull { it.evolvesFrom == hiddenClass.id }
        val uniqueSkill = content.skills.values.first { it.rarity == Rarity.UNIQUE }

        // Hiçbir şey açık değilken kilitli girdiler "???" döner.
        val empty = engine.archive(emptySet(), emptySet(), emptySet(), emptySet(), 0, Lang.TR)
        assertEquals(0, empty.unlockedTotal)
        assertTrue(empty.grandTotal > 10)
        assertTrue(empty.endings.entries.all { !it.unlocked && it.name == "???" && it.description.isEmpty() })

        // Açılan girdiler TR ve EN'de gerçek ad ve açıklamayla görünür.
        val partial = engine.archive(setOf(endingId), setOf(archetypeId), setOf(hiddenClass.id), setOf(uniqueSkill.id), 2, Lang.EN)
        assertEquals(4, partial.unlockedTotal)
        assertEquals(2, partial.completedGames)
        val unlockedEnding = partial.endings.entries.first { it.id == endingId }
        assertTrue(unlockedEnding.unlocked)
        assertNotEquals("???", unlockedEnding.name)
        assertTrue(unlockedEnding.description.isNotBlank())
        assertFalse(partial.endings.entries.first { it.id != endingId }.unlocked)

        // Gizli sınıf evrimleşse bile temel gizli sınıf kimliği yakalanır.
        val baseState = engine.newGame("Arda", GameMode.NORMAL, seed = 9L)
        val withHidden = baseState.copy(player = baseState.player.copy(classId = hiddenEvo?.id ?: hiddenClass.id, skills = listOf(SkillState(uniqueSkill.id))))
        assertTrue(hiddenClass.id in engine.unlockedHiddenClasses(withHidden))
        assertEquals(setOf(uniqueSkill.id), engine.unlockedUniqueSkills(withHidden))
    }
}
