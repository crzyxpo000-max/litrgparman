package com.yukselis.engine.system

import com.yukselis.engine.i18n.LText
import kotlin.math.floor
import kotlin.math.pow

/** Sekiz temel stat. Koddaki kimlikler (key) İngilizcedir; ekranda dile göre ad ve kısaltma gösterilir. */
enum class Stat(val key: String, val label: LText, val abbr: LText) {
    STR("str", LText("Güç", "Strength"), LText("GÜÇ", "STR")),
    AGI("agi", LText("Çeviklik", "Agility"), LText("ÇEV", "AGI")),
    END("end", LText("Dayanıklılık", "Endurance"), LText("DAY", "END")),
    INT("int", LText("Zeka", "Intelligence"), LText("ZEK", "INT")),
    WIS("wis", LText("Bilgelik", "Wisdom"), LText("BİL", "WIS")),
    PER("per", LText("Algı", "Perception"), LText("ALG", "PER")),
    WIL("wil", LText("İrade", "Willpower"), LText("İRD", "WIL")),
    CHA("cha", LText("Karizma", "Charisma"), LText("KAR", "CHA"));

    companion object {
        fun fromKey(key: String): Stat? = entries.firstOrNull { it.key == key }
    }
}

data class Stats(val values: Map<Stat, Int> = Stat.entries.associateWith { BASE_VALUE }) {
    operator fun get(stat: Stat): Int = values[stat] ?: 0

    fun plus(stat: Stat, delta: Int): Stats =
        copy(values = values + (stat to (get(stat) + delta).coerceAtLeast(0)))

    companion object {
        const val BASE_VALUE = 5
    }
}

/** F → S kademeleri (GDD: Sistem mekanikleri → Kademeler). */
enum class Grade(val minLevel: Int) {
    F(1), E(25), D(50), C(100), B(200), A(400), S(800);

    companion object {
        fun forLevel(level: Int): Grade = entries.last { level >= it.minLevel }
    }
}

/** GDD'deki formüller. Tek yerde tutulur, testlerle sabitlenir. */
object Progression {
    const val FREE_POINTS_PER_LEVEL = 3

    /** Bir sonraki seviyeye geçmek için gereken XP: ⌊100 · L^1.8⌋ */
    fun xpToNext(level: Int): Long = floor(100.0 * level.toDouble().pow(1.8)).toLong()

    /** Seviye farkı çarpanı: 1.15^(düşman − oyuncu), 0.1 ile 5 arasında sınırlı. */
    fun xpMultiplier(enemyLevel: Int, playerLevel: Int): Double =
        1.15.pow(enemyLevel - playerLevel).coerceIn(0.1, 5.0)

    fun maxHp(stats: Stats, level: Int): Int = 50 + 10 * stats[Stat.END] + 5 * level

    fun maxMp(stats: Stats): Int = 30 + 8 * stats[Stat.INT] + 3 * stats[Stat.WIS]

    fun maxStamina(stats: Stats): Int = 40 + 5 * stats[Stat.END] + 5 * stats[Stat.AGI]
}

/** Yetenek, eşya ve unvan nadirlikleri; içerikte Türkçe ASCII anahtarlarla yazılır. */
enum class Rarity(val key: String, val label: LText) {
    COMMON("siradan", LText("Sıradan", "Common")),
    RARE("nadir", LText("Nadir", "Rare")),
    EPIC("epik", LText("Epik", "Epic")),
    LEGENDARY("efsanevi", LText("Efsanevi", "Legendary")),
    MYTHIC("mitik", LText("Mitik", "Mythic")),
    UNIQUE("benzersiz", LText("Benzersiz", "Unique"));

    companion object {
        fun fromKey(key: String?): Rarity? = entries.firstOrNull { it.key == key }
    }
}
