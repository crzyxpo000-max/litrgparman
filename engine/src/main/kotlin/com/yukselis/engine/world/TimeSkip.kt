package com.yukselis.engine.world

import com.yukselis.engine.core.WorldState
import com.yukselis.engine.npc.RelAxis
import com.yukselis.engine.story.Effect
import com.yukselis.engine.system.Stat

/**
 * Antrenman / zaman atlama (DECISIONS #142): üste boş günleri bir odakla geçirmek. Kazançlar bilerek Günlük Görev ve
 * normal oynanıştan azdır: Günlük Görev günde 1 serbest puan verir, atlama 3 günde 1 stat. XP günde seviye XP'sinin
 * perdeye göre %25 / %50 / %100'ü: simülatörde (BALANCE.md) normal oynanış Perde 1'de günde ~0,3, Perde 2'de en az
 * ~0,6, Perde 3–4'te 2–11 seviye kazandırır; atlama her perdede bunun altında kalır.
 */
sealed interface SkipFocus {
    /** Özet olayının soneki (`atlama_<key>`) ve arayüz anahtarı. */
    val key: String

    /** Seçilen bir statı çalışmak. */
    data class Train(val stat: Stat) : SkipFocus {
        override val key get() = "antrenman"
    }

    /** Üs bölgesinin kapılarını temizlemek: tehlike düşer, ara sıra Öz Kristali. */
    data object Gates : SkipFocus {
        override val key get() = "kapi"
    }

    /** Meslek çalışması (meslek seçilmiş olmalı). */
    data object Craft : SkipFocus {
        override val key get() = "meslek"
    }

    /** Bir yoldaşla vakit geçirmek. */
    data class Companion(val npc: String) : SkipFocus {
        override val key get() = "yoldas"
    }
}

object TimeSkip {
    /** Perde başına günlük XP (seviye XP'sinin yüzdesi): Perde 1, 2, 3+. */
    val LEVEL_XP_PERCENT_BY_ACT = listOf(25, 50, 100)

    fun levelXpPercent(act: Int): Int = LEVEL_XP_PERCENT_BY_ACT[(act - 1).coerceIn(0, LEVEL_XP_PERCENT_BY_ACT.size - 1)]
    const val STAT_DAYS = 3
    const val CRYSTAL_DAYS = 3
    const val GATE_DANGER_PER_DAY = -3
    const val CRAFT_XP_PER_DAY = 5
    const val COMPANION_TRUST_MAX = 6
    const val COMPANION_BOND_DAYS = 2
    const val COMPANION_BOND_MAX = 2
    const val INTERRUPT_CHANCE = 0.25

    /** Hazırlık evresinde atlama operasyon gününe en az bu kadar gün bırakır. */
    const val SAFETY_DAYS = 2

    const val EVENT_PREFIX = "atlama_"
    const val STAT_CARRY_VAR = "atlama_birikim"
    const val CRYSTAL_CARRY_VAR = "atlama_kristal_birikim"
    const val DAYS_VAR = "_atlama_gun"
    const val VARIANT_VAR = "_atlama_varyant"
    const val CUT_VAR = "_atlama_kesildi"
    const val VARIANTS = 3

    fun summaryEvent(focus: SkipFocus): String = EVENT_PREFIX + focus.key

    /**
     * `days` günün kazanç etkileri (meslek XP'si hariç: onu motor doğrudan işler). Stat ve kristal kesirleri
     * birikim değişkeninde sonraki atlamaya taşınır (kısa atlamalar da boşa gitmez, toplam oran aynı kalır).
     */
    fun effects(state: WorldState, focus: SkipFocus, days: Int, base: String?): List<Effect> {
        if (days <= 0) return emptyList()
        val list = mutableListOf<Effect>(Effect.GainLevelXp(levelXpPercent(state.act) * days))
        when (focus) {
            is SkipFocus.Train -> list += carry(state, STAT_CARRY_VAR, days, STAT_DAYS) { Effect.AddStat(focus.stat, it) }
            SkipFocus.Gates -> {
                if (base != null && base in state.regions) list += Effect.ShiftDanger(base, GATE_DANGER_PER_DAY * days)
                list += carry(state, CRYSTAL_CARRY_VAR, days, CRYSTAL_DAYS) { Effect.AddCrystals(it) }
            }
            SkipFocus.Craft -> Unit
            is SkipFocus.Companion -> {
                val trust = days.coerceAtMost(COMPANION_TRUST_MAX)
                val bond = (days / COMPANION_BOND_DAYS).coerceAtMost(COMPANION_BOND_MAX)
                list += Effect.ShiftRelation(focus.npc, buildMap {
                    put(RelAxis.TRUST, trust)
                    if (bond > 0) put(RelAxis.BOND, bond)
                })
            }
        }
        return list
    }

    private fun carry(state: WorldState, key: String, days: Int, per: Int, gain: (Int) -> Effect): List<Effect> {
        val old = state.variable(key)
        val total = old + days
        val earned = total / per
        val rest = total % per
        return listOfNotNull(Effect.AddVar(key, rest - old).takeIf { rest != old }, earned.takeIf { it > 0 }?.let(gain))
    }
}
