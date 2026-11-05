package com.yukselis.engine.story

import com.yukselis.engine.core.GameMode
import com.yukselis.engine.core.HistoryEntry
import com.yukselis.engine.core.WorldState
import com.yukselis.engine.i18n.Lang
import com.yukselis.engine.i18n.NameEntry
import com.yukselis.engine.i18n.TokenResolver
import com.yukselis.engine.system.Grade
import com.yukselis.engine.system.Progression
import com.yukselis.engine.system.Rarity
import com.yukselis.engine.system.Stat

// ---- Arayüze giden, dili çözülmüş görünüm modelleri ----

data class NoticeView(
    val kind: NoticeKind,
    val title: String,
    val lines: List<String>,
    val rarity: Rarity?,
)

data class ChoiceView(
    val id: String,
    val label: String,
    /** "[ALG 6]" gibi koşul etiketi; koşulsuz seçeneklerde null. */
    val requirementTag: String?,
    val enabled: Boolean,
)

data class PassageView(
    val sceneId: String,
    val placeName: String?,
    val paragraphs: List<String>,
    val notices: List<NoticeView>,
    /** Geçmiş pasajlarda oyuncunun seçtiği seçenek. */
    val chosenLabel: String? = null,
)

data class SceneView(
    val day: Int,
    val act: Int,
    val placeName: String?,
    val history: List<PassageView>,
    val current: PassageView,
    val choices: List<ChoiceView>,
    val isEnding: Boolean,
)

data class StatLine(val stat: Stat, val abbr: String, val label: String, val value: Int)

data class TitleLine(val name: String, val description: String, val rarity: Rarity)

data class StatusView(
    val playerName: String,
    val level: Int,
    val grade: Grade,
    val xp: Long,
    val xpToNext: Long,
    val hp: Int,
    val maxHp: Int,
    val mp: Int,
    val maxMp: Int,
    val stamina: Int,
    val freePoints: Int,
    val stats: List<StatLine>,
    val titles: List<TitleLine>,
    val mercy: Int,
    val order: Int,
    val humanity: Int,
    val day: Int,
    val act: Int,
    val mode: GameMode,
)

data class Outcome(val state: WorldState, val notices: List<NoticeView>)

/**
 * Hikaye oynatıcısı (Aşama 1). Saf fonksiyonlar: durum alır, yeni durum döndürür.
 * Aşama 4'te olay havuzu ve Hikaye Yönetmeni bunun yerine/yanına gelecek.
 */
class StoryEngine(val content: Content) {
    private val resolver = TokenResolver(content.names)
    private val effects = EffectApplier(content.titles)

    fun newGame(playerName: String, mode: GameMode, seed: Long): WorldState =
        WorldState.newGame(seed, playerName, mode, content.startScene)

    fun view(state: WorldState, lang: Lang): SceneView {
        val scene = scene(state.sceneId)
        val history = state.history.mapNotNull { entry ->
            val past = content.scenes[entry.sceneId] ?: return@mapNotNull null
            val chosen = past.choices.firstOrNull { it.id == entry.choiceId }
            passage(past, state, lang).copy(chosenLabel = chosen?.let { text(it.label[lang], state, lang) })
        }
        val choices = scene.choices
            .filter { it.showWhen?.test(state) ?: true }
            .map { choice ->
                ChoiceView(
                    id = choice.id,
                    label = text(choice.label[lang], state, lang),
                    requirementTag = requirementTag(choice.requires, lang),
                    enabled = choice.requires?.test(state) ?: true,
                )
            }
        return SceneView(
            day = state.day,
            act = state.act,
            placeName = placeName(scene.place, lang),
            history = history,
            current = passage(scene, state, lang),
            choices = choices,
            isEnding = scene.isEnding,
        )
    }

    fun choose(state: WorldState, choiceId: String, lang: Lang): Outcome {
        val scene = scene(state.sceneId)
        val choice = scene.choices.firstOrNull { it.id == choiceId }
            ?: throw IllegalArgumentException("'${scene.id}' sahnesinde '$choiceId' seçeneği yok")
        require(choice.showWhen?.test(state) ?: true) { "Gizli seçenek seçilemez: $choiceId" }
        require(choice.requires?.test(state) ?: true) { "Koşulu karşılanmayan seçenek: $choiceId" }

        val applied = effects.applyAll(state, choice.effects)
        var next = applied.state.copy(
            history = (applied.state.history + HistoryEntry(scene.id, choice.id)).takeLast(WorldState.HISTORY_LIMIT),
        )
        val nextId = choice.next
        if (nextId != null) {
            val nextScene = scene(nextId)
            next = next.copy(sceneId = nextId, ended = nextScene.isEnding)
        }
        return Outcome(next, applied.notices.map { noticeView(it, next, lang) })
    }

    fun status(state: WorldState, lang: Lang): StatusView {
        val p = state.player
        return StatusView(
            playerName = state.playerName,
            level = p.level,
            grade = Grade.forLevel(p.level),
            xp = p.xp,
            xpToNext = Progression.xpToNext(p.level),
            hp = p.hp,
            maxHp = p.maxHp,
            mp = p.mp,
            maxMp = p.maxMp,
            stamina = Progression.maxStamina(p.stats),
            freePoints = p.freePoints,
            stats = Stat.entries.map { StatLine(it, it.abbr[lang], it.label[lang], p.stats[it]) },
            titles = p.titles.mapNotNull { id ->
                content.titles[id]?.let { TitleLine(it.name[lang], text(it.description[lang], state, lang), it.rarity) }
            },
            mercy = state.morality.mercy,
            order = state.morality.order,
            humanity = state.morality.humanity,
            day = state.day,
            act = state.act,
            mode = state.mode,
        )
    }

    private fun scene(id: String): Scene =
        content.scenes[id] ?: throw IllegalStateException("Tanımsız sahne: $id")

    private fun passage(scene: Scene, state: WorldState, lang: Lang) = PassageView(
        sceneId = scene.id,
        placeName = placeName(scene.place, lang),
        paragraphs = scene.paragraphs
            .filter { it.condition?.test(state) ?: true }
            .map { text(it.text[lang], state, lang) },
        notices = scene.notices
            .filter { it.condition?.test(state) ?: true }
            .map { noticeView(it.notice, state, lang) },
    )

    private fun noticeView(notice: Notice, state: WorldState, lang: Lang) = NoticeView(
        kind = notice.kind,
        title = text(notice.title[lang], state, lang),
        lines = notice.lines.map { text(it[lang], state, lang) },
        rarity = notice.rarity,
    )

    private fun placeName(placeId: String?, lang: Lang): String? =
        placeId?.let { content.names.find(NameEntry.Kind.PLACE, it)?.full?.get(lang) }

    private fun text(raw: String, state: WorldState, lang: Lang): String =
        resolver.resolve(raw, lang, state.playerName)

    private fun requirementTag(condition: Condition?, lang: Lang): String? = when (condition) {
        null -> null
        is Condition.StatAtLeast -> "[${condition.stat.abbr[lang]} ${condition.value}]"
        is Condition.All -> condition.conditions.mapNotNull { requirementTag(it, lang) }
            .joinToString(" ").ifEmpty { null }
        else -> null
    }
}
