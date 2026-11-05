package com.yukselis.engine.story

import com.yukselis.engine.core.GameClock
import com.yukselis.engine.core.WorldState
import com.yukselis.engine.i18n.LText
import com.yukselis.engine.system.Progression
import com.yukselis.engine.system.Stat

/** Etkileri sırayla uygular ve oyuncuya gösterilecek Sistem bildirimlerini üretir. */
class EffectApplier(private val titles: Map<String, TitleDef>) {

    data class Result(val state: WorldState, val notices: List<Notice>)

    fun applyAll(state: WorldState, effects: List<Effect>): Result {
        var current = state
        val notices = mutableListOf<Notice>()
        for (effect in effects) {
            val r = apply(current, effect)
            current = r.state
            notices += r.notices
        }
        return Result(current, notices)
    }

    fun apply(state: WorldState, effect: Effect): Result = when (effect) {
        is Effect.SetFlag -> Result(state.copy(flags = state.flags + effect.flag), emptyList())
        is Effect.ClearFlag -> Result(state.copy(flags = state.flags - effect.flag), emptyList())
        is Effect.AddVar -> Result(
            state.copy(vars = state.vars + (effect.key to state.variable(effect.key) + effect.delta)),
            emptyList(),
        )
        is Effect.ShiftMorality -> Result(state.copy(morality = state.morality.shift(effect.axis, effect.delta)), emptyList())
        is Effect.ShiftReputation -> {
            val value = ((state.reputation[effect.faction] ?: 0) + effect.delta).coerceIn(-100, 100)
            Result(state.copy(reputation = state.reputation + (effect.faction to value)), emptyList())
        }
        is Effect.AdvanceDays -> Result(GameClock.advance(state, effect.days), emptyList())
        is Effect.AddStat -> addStat(state, effect.stat, effect.delta)
        is Effect.GainXp -> gainXp(state, effect.amount)
        is Effect.GrantTitle -> grantTitle(state, effect.titleId)
    }

    private fun addStat(state: WorldState, stat: Stat, delta: Int): Result {
        val p = state.player
        val stats = p.stats.plus(stat, delta)
        val updated = p.copy(stats = stats).let { it.copy(hp = it.hp.coerceAtMost(it.maxHp), mp = it.mp.coerceAtMost(it.maxMp)) }
        val sign = if (delta >= 0) "+" else ""
        val notice = Notice(
            kind = NoticeKind.STATUS,
            title = LText("${stat.label.tr} $sign$delta", "${stat.label.en} $sign$delta"),
        )
        return Result(state.copy(player = updated), listOf(notice))
    }

    private fun gainXp(state: WorldState, amount: Long): Result {
        if (amount <= 0) return Result(state, emptyList())
        var p = state.player.copy(xp = state.player.xp + amount)
        val startLevel = p.level
        while (p.xp >= Progression.xpToNext(p.level)) {
            p = p.copy(
                xp = p.xp - Progression.xpToNext(p.level),
                level = p.level + 1,
                freePoints = p.freePoints + Progression.FREE_POINTS_PER_LEVEL,
            )
        }
        val notices = mutableListOf(
            Notice(NoticeKind.SYSTEM, LText("+$amount XP", "+$amount XP")),
        )
        if (p.level > startLevel) {
            // Seviye atlayınca can ve mana dolar.
            p = p.copy(hp = p.maxHp, mp = p.maxMp)
            val gained = (p.level - startLevel) * Progression.FREE_POINTS_PER_LEVEL
            notices += Notice(
                kind = NoticeKind.LEVEL_UP,
                title = LText("Seviye atladın!", "Level up!"),
                lines = listOf(
                    LText("Seviye $startLevel → ${p.level}", "Level $startLevel → ${p.level}"),
                    LText("+$gained serbest stat puanı", "+$gained free stat points"),
                    LText("Can ve mana yenilendi.", "Health and mana restored."),
                ),
            )
        }
        return Result(state.copy(player = p), notices)
    }

    private fun grantTitle(state: WorldState, titleId: String): Result {
        val def = titles[titleId] ?: error("Tanımsız unvan: $titleId")
        if (titleId in state.player.titles) return Result(state, emptyList())
        var stats = state.player.stats
        def.statBonus.forEach { (stat, bonus) -> stats = stats.plus(stat, bonus) }
        val player = state.player.copy(titles = state.player.titles + titleId, stats = stats)
        val bonusTr = def.statBonus.entries.joinToString(", ") { "${it.key.abbr.tr} +${it.value}" }
        val bonusEn = def.statBonus.entries.joinToString(", ") { "${it.key.abbr.en} +${it.value}" }
        val lines = buildList {
            add(def.description)
            if (def.statBonus.isNotEmpty()) add(LText("Bonus: $bonusTr", "Bonus: $bonusEn"))
        }
        val notice = Notice(
            kind = NoticeKind.TITLE,
            title = LText("Unvan kazanıldı: ${def.name.tr}", "Title earned: ${def.name.en}"),
            lines = lines,
            rarity = def.rarity,
        )
        return Result(state.copy(player = player), listOf(notice))
    }
}
