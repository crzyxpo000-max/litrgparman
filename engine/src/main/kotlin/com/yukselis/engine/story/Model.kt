package com.yukselis.engine.story

import com.yukselis.engine.core.MoralAxis
import com.yukselis.engine.core.WorldState
import com.yukselis.engine.i18n.LText
import com.yukselis.engine.i18n.NameRegistry
import com.yukselis.engine.system.Rarity
import com.yukselis.engine.system.Stat

/** Sistem bildirimi türleri; arayüz her türe ayrı renk ve ikon verir. */
enum class NoticeKind(val key: String) {
    SYSTEM("sistem"),
    STATUS("durum"),
    ANALYZE("analiz"),
    LEVEL_UP("seviye"),
    TITLE("unvan"),
    QUEST("gorev"),
    WARNING("uyari");

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
}

data class Paragraph(val text: LText, val condition: Condition? = null)

data class SceneNotice(val notice: Notice, val condition: Condition? = null)

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
)

data class TitleDef(
    val id: String,
    val name: LText,
    val description: LText,
    val rarity: Rarity,
    val statBonus: Map<Stat, Int> = emptyMap(),
)

/** Yüklenmiş oyun içeriğinin tamamı. */
data class Content(
    val scenes: Map<String, Scene>,
    val names: NameRegistry,
    val titles: Map<String, TitleDef>,
    val startScene: String,
)
