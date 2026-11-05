package com.yukselis.engine.core

import com.yukselis.engine.system.Progression
import com.yukselis.engine.system.Stats

enum class GameMode { NORMAL, IRONMAN }

/** Üç ahlak ekseni, her biri −100…+100. Pozitif uç: Merhamet, Düzen, İnsanlık. */
enum class MoralAxis(val key: String) {
    MERCY("merhamet"),
    ORDER("duzen"),
    HUMANITY("insanlik");

    companion object {
        fun fromKey(key: String): MoralAxis? = entries.firstOrNull { it.key == key }
    }
}

data class Morality(
    val mercy: Int = 0,
    val order: Int = 0,
    val humanity: Int = 0,
) {
    operator fun get(axis: MoralAxis): Int = when (axis) {
        MoralAxis.MERCY -> mercy
        MoralAxis.ORDER -> order
        MoralAxis.HUMANITY -> humanity
    }

    fun shift(axis: MoralAxis, delta: Int): Morality {
        val value = (get(axis) + delta).coerceIn(-100, 100)
        return when (axis) {
            MoralAxis.MERCY -> copy(mercy = value)
            MoralAxis.ORDER -> copy(order = value)
            MoralAxis.HUMANITY -> copy(humanity = value)
        }
    }
}

data class PlayerState(
    val level: Int = 1,
    val xp: Long = 0,
    val freePoints: Int = 0,
    val stats: Stats = Stats(),
    val hp: Int = Progression.maxHp(stats, level),
    val mp: Int = Progression.maxMp(stats),
    val titles: List<String> = emptyList(),
) {
    val maxHp: Int get() = Progression.maxHp(stats, level)
    val maxMp: Int get() = Progression.maxMp(stats)
}

/** Oyuncunun geçtiği sahne ve yaptığı seçim; hikaye akışını yeniden çizmek için kullanılır. */
data class HistoryEntry(val sceneId: String, val choiceId: String)

/**
 * Tek doğruluk kaynağı. Değişmezdir: her değişiklik yeni bir kopya üretir.
 * Kayıt dosyası bu sınıfın tamamını tutar; dil bilgisi içermez.
 */
data class WorldState(
    val seed: Long,
    val rngState: Long,
    val playerName: String,
    val mode: GameMode,
    val sceneId: String,
    val day: Int = 0,
    val player: PlayerState = PlayerState(),
    val flags: Set<String> = emptySet(),
    val vars: Map<String, Int> = emptyMap(),
    val reputation: Map<String, Int> = emptyMap(),
    val morality: Morality = Morality(),
    val history: List<HistoryEntry> = emptyList(),
    val ended: Boolean = false,
) {
    val act: Int get() = GameClock.actFor(day)

    fun hasFlag(flag: String): Boolean = flag in flags

    fun variable(key: String): Int = vars[key] ?: 0

    companion object {
        const val HISTORY_LIMIT = 12

        fun newGame(seed: Long, playerName: String, mode: GameMode, startScene: String): WorldState =
            WorldState(
                seed = seed,
                rngState = seed,
                playerName = playerName.trim().ifEmpty { "?" },
                mode = mode,
                sceneId = startScene,
            )
    }
}

/** Oyun takvimi: 365 gün, dört perde (GDD: Yapı → Perdeler). */
object GameClock {
    const val LAST_DAY = 365

    fun actFor(day: Int): Int = when {
        day <= 30 -> 1
        day <= 180 -> 2
        day <= 330 -> 3
        else -> 4
    }

    fun advance(state: WorldState, days: Int): WorldState =
        state.copy(day = (state.day + days).coerceIn(0, LAST_DAY))
}
