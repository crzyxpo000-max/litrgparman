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

    /** Bir sonraki kademe; S'den sonra yok. */
    fun next(): Grade? = entries.getOrNull(ordinal + 1)

    companion object {
        fun forLevel(level: Int): Grade = entries.last { level >= it.minLevel }

        fun fromKey(key: String?): Grade? = entries.firstOrNull { it.name.equals(key, ignoreCase = true) }
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

    /** Sınıf teklifi seviyesi (GDD: Seviye 10'da temel sınıf). */
    const val CLASS_OFFER_LEVEL = 10

    /** Meslek teklifi seviyesi (GDD açık bırakıyor; DECISIONS #21). */
    const val PROFESSION_OFFER_LEVEL = 5

    const val SKILL_MAX_LEVEL = 10

    /** Yetenek seviyesi için gereken kullanım XP'si: 4 · L (1→10 toplam 180). */
    fun skillXpToNext(level: Int): Int = 4 * level

    /** Meslek seviyesi için gereken XP: 50 · L. */
    fun professionXpToNext(level: Int): Int = 50 * level

    /** Kademe başına Öz yuvası: D'de 1, C ve üstünde 2 (GDD: Kademeler tablosu). */
    fun essenceSlots(grade: Grade): Int = when {
        grade < Grade.D -> 0
        grade == Grade.D -> 1
        else -> 2
    }

    /** Öz bir yuvaya bağlanmadan önce gereken kavrayış. */
    const val ESSENCE_INSIGHT_TO_BIND = 100

    /**
     * OP MC üstel büyüme: sınıfın seviye başı artışı kademeyle çarpılır (F ×1, E ×2, D ×3 …).
     */
    fun growthMultiplier(grade: Grade): Int = grade.ordinal + 1

    /** Her 7 günlük Günlük Görev serisinde +1 ek serbest puan. */
    fun dailyReward(streakAfter: Int): Int = 1 + if (streakAfter > 0 && streakAfter % 7 == 0) 1 else 0

    /** Dünya nüfusu (insanlık yüzdeliğinin alt sınırı: tek kişi). */
    const val HUMANITY = 8_000_000_000.0

    /**
     * İnsanlık yüzdeliği: oyuncudan güçlü insanların yüzdesi ("İlk %0,001").
     * Ortalama insan seviyesi zamanla yükselir (1 + 0,08 · gün); seviyeler üstel kuyruklu
     * dağılır, ölçek ortalamayla büyür. Sonuç [1/nüfus, 100] aralığında.
     */
    fun humanityTopPercent(level: Int, day: Int): Double {
        val avg = 1.0 + 0.08 * day
        val scale = 1.0 + 0.25 * avg
        val fraction = kotlin.math.exp(-(level - avg) / scale).coerceAtMost(1.0)
        return (fraction * 100.0).coerceIn(100.0 / HUMANITY, 100.0)
    }

    /** Gün başına beklenen insan seviyesi (Dikkat hesabı): 1 + 0,4 · gün. */
    fun expectedLevel(day: Int): Int = 1 + (0.4 * day).toInt()

    /** Gün içi olay/eylem yuvası sayısı: 2–4 (GDD: Gün döngüsü). */
    const val MIN_SLOTS = 2
    const val MAX_SLOTS = 4

    /** Generic evolution (sınıfın o kademe için tanımlı evrimi yoksa) her stata verilen bonus. */
    const val GENERIC_EVOLUTION_BONUS = 1
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
