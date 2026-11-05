package com.yukselis.engine.story

import com.yukselis.engine.combat.AwakeningDef
import com.yukselis.engine.combat.CombatSpec
import com.yukselis.engine.combat.EnemyDef
import com.yukselis.engine.core.MoralAxis
import com.yukselis.engine.core.WorldState
import com.yukselis.engine.i18n.LText
import com.yukselis.engine.i18n.NameRegistry
import com.yukselis.engine.system.Affinity
import com.yukselis.engine.system.ClassDef
import com.yukselis.engine.system.DailyTemplate
import com.yukselis.engine.system.EssenceDef
import com.yukselis.engine.system.FusionRecipe
import com.yukselis.engine.system.ItemDef
import com.yukselis.engine.system.ProfessionDef
import com.yukselis.engine.system.Rarity
import com.yukselis.engine.system.RecipeDef
import com.yukselis.engine.system.SkillDef
import com.yukselis.engine.system.Stat

/** Sistem bildirimi türleri; arayüz her türe ayrı renk ve ikon verir. */
enum class NoticeKind(val key: String) {
    SYSTEM("sistem"),
    STATUS("durum"),
    ANALYZE("analiz"),
    LEVEL_UP("seviye"),
    TITLE("unvan"),
    QUEST("gorev"),
    WARNING("uyari"),
    SKILL("yetenek"),
    EVOLUTION("evrim"),
    CLASS("sinif"),
    /** Yankı'nın bozuk, yalnızca oyuncuya görünen bildirimleri (glitchli yazı). */
    ECHO("yanki");

    companion object {
        fun fromKey(key: String?): NoticeKind = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

/** Çözülmemiş (iki dilli) bildirim. */
data class Notice(
    val kind: NoticeKind,
    val title: LText,
    val lines: List<LText> = emptyList(),
    val rarity: Rarity? = null,
)

/** Güvenli koşul dili; betik çalıştırma yoktur. */
sealed interface Condition {
    fun test(state: WorldState): Boolean

    data class StatAtLeast(val stat: Stat, val value: Int) : Condition {
        override fun test(state: WorldState) = state.player.stats[stat] >= value
    }

    data class HasFlag(val flag: String) : Condition {
        override fun test(state: WorldState) = state.hasFlag(flag)
    }

    data class LacksFlag(val flag: String) : Condition {
        override fun test(state: WorldState) = !state.hasFlag(flag)
    }

    data class VarAtLeast(val key: String, val value: Int) : Condition {
        override fun test(state: WorldState) = state.variable(key) >= value
    }

    data class VarAtMost(val key: String, val value: Int) : Condition {
        override fun test(state: WorldState) = state.variable(key) <= value
    }

    data class MoralityAtLeast(val axis: MoralAxis, val value: Int) : Condition {
        override fun test(state: WorldState) = state.morality[axis] >= value
    }

    data class MoralityAtMost(val axis: MoralAxis, val value: Int) : Condition {
        override fun test(state: WorldState) = state.morality[axis] <= value
    }

    data class LevelAtLeast(val value: Int) : Condition {
        override fun test(state: WorldState) = state.player.level >= value
    }

    data class AffinityAtLeast(val affinity: Affinity, val value: Int) : Condition {
        override fun test(state: WorldState) = state.player.affinity[affinity] >= value
    }

    data class HasSkill(val skillId: String) : Condition {
        override fun test(state: WorldState) = state.player.skill(skillId) != null
    }

    data class HasTitle(val titleId: String) : Condition {
        override fun test(state: WorldState) = titleId in state.player.titles
    }

    data class HasClass(val classId: String) : Condition {
        override fun test(state: WorldState) = state.player.classId == classId
    }

    data class HasItem(val itemId: String, val count: Int) : Condition {
        override fun test(state: WorldState) = state.player.itemCount(itemId) >= count
    }

    data class ReputationAtLeast(val faction: String, val value: Int) : Condition {
        override fun test(state: WorldState) = (state.reputation[faction] ?: 0) >= value
    }

    data class All(val conditions: List<Condition>) : Condition {
        override fun test(state: WorldState) = conditions.all { it.test(state) }
    }
}

/** Dünya durumunu değiştirmenin tek yolu. */
sealed interface Effect {
    data class SetFlag(val flag: String) : Effect
    data class ClearFlag(val flag: String) : Effect
    data class AddVar(val key: String, val delta: Int) : Effect
    data class ShiftMorality(val axis: MoralAxis, val delta: Int) : Effect
    data class ShiftReputation(val faction: String, val delta: Int) : Effect
    data class GainXp(val amount: Long) : Effect
    data class AdvanceDays(val days: Int) : Effect
    data class AddStat(val stat: Stat, val delta: Int) : Effect
    data class GrantTitle(val titleId: String) : Effect
    data class ShiftAffinity(val affinity: Affinity, val delta: Int) : Effect
    data class GrantSkill(val skillId: String) : Effect
    data class SkillXp(val skillId: String, val amount: Int) : Effect
    data class AddItem(val itemId: String, val count: Int) : Effect
    data class AddCredits(val amount: Int) : Effect
    data class AddCrystals(val amount: Int) : Effect
    data class EssenceInsight(val essenceId: String, val amount: Int) : Effect
    /** Aynı türdeki (antrenman, kesif, savas, kapi) Günlük Görevi ilerletir. */
    data class DailyProgress(val kind: String, val amount: Int) : Effect
    /** Can/mana değişimi. Savaş dışında can 1'in altına düşmez (savaşta ölüm Aşama 3). */
    data class AddHp(val delta: Int) : Effect
    data class AddMp(val delta: Int) : Effect
    /** Savaş başlatır; seçenek `sonraki` yerine savaşın kazan/kaybet/kaç sahnelerine gider. */
    data class StartCombat(val spec: CombatSpec) : Effect
    /** Kendini açan arayüz: bir sekmeyi verir (yetenekler, canta, harita). */
    data class UnlockTab(val tab: String) : Effect
    /** Meslek seçimi istemini açar. */
    data object OfferProfession : Effect
}

data class Paragraph(val text: LText, val condition: Condition? = null)

data class SceneNotice(val notice: Notice, val condition: Condition? = null)

/**
 * Pasif Sistem sezgisi: koşulu (çoğunlukla yüksek stat) sağlanınca sahneye yalnızca oyuncunun
 * okuduğu özel bir satır eklenir. Stat koşulunda satırın başına "[ALG]" gibi önek gelir.
 */
data class Insight(val text: LText, val condition: Condition)

data class ChoiceDef(
    val id: String,
    val label: LText,
    /** Karşılanmazsa seçenek görünür ama kilitlidir ("[ALG 25]"). */
    val requires: Condition? = null,
    /** Karşılanmazsa seçenek hiç gösterilmez. */
    val showWhen: Condition? = null,
    val effects: List<Effect> = emptyList(),
    val next: String? = null,
)

data class Scene(
    val id: String,
    val place: String?,
    val paragraphs: List<Paragraph>,
    val notices: List<SceneNotice> = emptyList(),
    val choices: List<ChoiceDef> = emptyList(),
    val isEnding: Boolean = false,
    val insights: List<Insight> = emptyList(),
)

data class TitleDef(
    val id: String,
    val name: LText,
    val description: LText,
    val rarity: Rarity,
    val statBonus: Map<Stat, Int> = emptyMap(),
    /** "İlk" unvanı: dünyada yalnızca bir sahibi olur. */
    val first: Boolean = false,
    /** Fraksiyon itibarı tavanı (Kardeş Katili: Sığınak en fazla −50). */
    val reputationCap: Map<String, Int> = emptyMap(),
)

/** Yüklenmiş oyun içeriğinin tamamı. */
data class Content(
    val scenes: Map<String, Scene>,
    val names: NameRegistry,
    val titles: Map<String, TitleDef>,
    val startScene: String,
    val classes: Map<String, ClassDef> = emptyMap(),
    val skills: Map<String, SkillDef> = emptyMap(),
    val fusions: List<FusionRecipe> = emptyList(),
    val items: Map<String, ItemDef> = emptyMap(),
    val professions: Map<String, ProfessionDef> = emptyMap(),
    val recipes: Map<String, RecipeDef> = emptyMap(),
    val essences: Map<String, EssenceDef> = emptyMap(),
    val dailyTemplates: Map<String, DailyTemplate> = emptyMap(),
    val texts: SystemTexts = SystemTexts.EMPTY,
    val enemies: Map<String, EnemyDef> = emptyMap(),
    val awakenings: List<AwakeningDef> = emptyList(),
) {
    companion object {
        /** Kesinti sahnesinden önceki sahneye dönüş. */
        const val RETURN = "@geri"

        /** Günlük Görev cezası kancası: bu kimlikte bir sahne varsa oraya ışınlanılır. */
        const val PENALTY_SCENE = "ceza_bolgesi"

        /** Demir Mod ölümü bu sahneye (Hasat sonu) bağlanır. */
        const val HARVEST_SCENE = "son_hasat"
    }
}
