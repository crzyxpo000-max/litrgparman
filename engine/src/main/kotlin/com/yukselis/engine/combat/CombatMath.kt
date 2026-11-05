package com.yukselis.engine.combat

import com.yukselis.engine.core.Rng
import com.yukselis.engine.system.Grade
import com.yukselis.engine.system.Stat
import com.yukselis.engine.system.Stats
import kotlin.math.pow
import kotlin.math.roundToInt

/** GDD: Savaş sistemi → Formüller. Tek yerde tutulur, testlerle sabitlenir. */
object CombatMath {

    /** İnisiyatif = ÇEV + 0,5 · ALG + d20 */
    fun initiative(stats: Stats, d20: Int): Double = stats[Stat.AGI] + 0.5 * stats[Stat.PER] + d20

    /** İsabet = 75% + (ALG_saldıran − ÇEV_savunan) · 1,5% (5%–95%). Kör: −40 puan. */
    fun hitChance(attacker: Stats, defender: Stats, blind: Boolean = false, bonus: Double = 0.0): Double {
        var p = 0.75 + (attacker[Stat.PER] - defender[Stat.AGI]) * 0.015 + bonus
        if (blind) p -= 0.40
        return p.coerceIn(0.05, 0.95)
    }

    /** Kritik şansı = 5% + 0,2 · ÇEV (yüzde puanı). */
    fun critChance(stats: Stats): Double = (0.05 + 0.002 * stats[Stat.AGI]).coerceAtMost(0.75)

    /** Kritik çarpanı = 1,5 + 0,005 · ALG */
    fun critMultiplier(stats: Stats): Double = 1.5 + 0.005 * stats[Stat.PER]

    /** Kademe farkı: her kademe için ×2 (üst) ya da ×0,5 (alt). */
    fun gradeMultiplier(attacker: Grade, defender: Grade): Double = 2.0.pow(attacker.ordinal - defender.ordinal)

    /** Fiziksel direnç = 2 · DAY; büyü direnci = 2 · BİL. Yanma direnci %10 düşürür. */
    fun resistance(stats: Stats, magical: Boolean, burning: Boolean): Double {
        val r = 2.0 * (if (magical) stats[Stat.WIS] else stats[Stat.END])
        return if (burning) r * 0.9 else r
    }

    /**
     * Hasar = (Taban + Stat · Kat) · 100 / (100 + Direnç) · (1 ± 0,1), ardından kademe çarpanı
     * ve diğer çarpanlar (duruş, zaaf, kritik, efor). En az 1.
     */
    fun damage(
        base: Int,
        statValue: Int,
        multiplier: Double,
        resistance: Double,
        rng: Rng,
        gradeMult: Double = 1.0,
        extra: Double = 1.0,
    ): Int {
        val raw = (base + statValue * multiplier) * 100.0 / (100.0 + resistance)
        val variance = 0.9 + rng.nextDouble() * 0.2
        return (raw * variance * gradeMult * extra).roundToInt().coerceAtLeast(1)
    }

    /** Efor kademesi başına hasar ve isabet bonusu ve Dayanım bedeli. */
    const val EFFORT_DAMAGE = 0.12
    const val EFFORT_HIT = 0.05
    const val EFFORT_STAMINA = 8

    const val WEAKNESS_MULTIPLIER = 1.5
    const val DEFEND_MULTIPLIER = 0.5

    /** Kaçış: 50% + (ÇEV_oyuncu − ort. ÇEV_düşman) · 3% (10%–95%). */
    fun fleeChance(player: Stats, enemies: List<Stats>): Double {
        val avg = if (enemies.isEmpty()) 0.0 else enemies.map { it[Stat.AGI] }.average()
        return (0.5 + (player[Stat.AGI] - avg) * 0.03).coerceIn(0.10, 0.95)
    }

    /**
     * OP MC "Ezme": düşman oyuncunun açıkça altında kalırsa savaş tek satırla çözülür.
     * Daha düşük kademe, ya da seviyesi oyuncunun yarısından az ve en az 5 seviye aşağıda.
     */
    fun isCrush(playerLevel: Int, playerGrade: Grade, enemyLevel: Int, enemyGrade: Grade): Boolean =
        enemyGrade < playerGrade || (enemyLevel * 2 <= playerLevel && playerLevel - enemyLevel >= 5)

    /** Korku: İRD kontrolü (d20 + İRD ≥ 15); başarısızsa saldırılamaz. */
    fun fearCheck(wil: Int, d20: Int): Boolean = d20 + wil >= 15

    /** Kanama: her tur maks. HP'nin %3'ü. */
    fun bleedDamage(maxHp: Int): Int = (maxHp * 0.03).roundToInt().coerceAtLeast(1)

    /** Zehir ve yanma için tur hasarı (uygulayanın seviyesine göre). */
    fun dotPower(level: Int): Int = 4 + level / 2

    /** Kademe Kırma: üst kademedeki düşmanı yenmek XP'yi ek olarak katlar. */
    const val GAP_BREAK_XP = 3.0

    /** İkinci Şans: canlanınca maks. HP'nin bu oranı. */
    const val SECOND_CHANCE_HP = 0.3
}
