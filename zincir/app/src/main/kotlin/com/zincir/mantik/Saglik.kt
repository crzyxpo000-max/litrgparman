package com.zincir.mantik

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.roundToInt

/** Saatten (Health Connect üzerinden) okunan bir günün verisi. Okunamayan alan null'dır. */
data class SaatVerisi(val adim: Int?, val uykuSaat: Double?, val aktifKcal: Int?, val egzersizler: List<SaatEgzersizi> = emptyList())

/** Saatte kaydedilmiş bir kardiyo egzersizi (yürüyüş, bisiklet…). */
data class SaatEgzersizi(val tur: String, val sureDk: Int)

/** Health Connect'ten okunan ham egzersiz oturumu. */
data class EgzersizOturumu(val tur: String, val baslangic: Instant, val bitis: Instant)

/** Bir uyku oturumu. [bitisOfseti]: oturumun bittiği yerin saat farkı (kayıtta yoksa null). */
data class UykuOturumu(val baslangic: Instant, val bitis: Instant, val bitisOfseti: ZoneOffset?)

/** Saat verisini günlük kayda işleme kuralları. Yalnızca doldurur; hiçbir hedefi değiştirmez. */
object Saglik {
    /**
     * Bir günün uykusu: o gün (uyanılan yerin yerel tarihiyle) biten oturumların toplamı, saat cinsinden.
     * Aynı uyku iki kaynaktan gelmişse (üst üste binen oturumlar) iki kez sayılmaz. Oturum yoksa null.
     */
    fun uykuSaati(oturumlar: List<UykuOturumu>, gun: Long, varsayilanBolge: ZoneId): Double? {
        val gununOturumlari = oturumlar.filter { o ->
            val ofset = o.bitisOfseti ?: varsayilanBolge.rules.getOffset(o.bitis)
            o.bitis.atOffset(ofset).toLocalDate().toEpochDay() == gun && o.bitis.isAfter(o.baslangic)
        }.sortedBy { it.baslangic }
        if (gununOturumlari.isEmpty()) return null
        // Üst üste binen aralıkları birleştirerek topla.
        var toplamSn = 0L
        var bas = gununOturumlari.first().baslangic
        var son = gununOturumlari.first().bitis
        for (o in gununOturumlari.drop(1)) {
            if (o.baslangic.isAfter(son)) {
                toplamSn += son.epochSecond - bas.epochSecond
                bas = o.baslangic
                son = o.bitis
            } else if (o.bitis.isAfter(son)) {
                son = o.bitis
            }
        }
        toplamSn += son.epochSecond - bas.epochSecond
        return (toplamSn / 360.0).roundToInt() / 10.0 // 0,1 saate yuvarla
    }

    /** Bundan kısa egzersizler kardiyo önerisi olarak gösterilmez. */
    const val EN_KISA_KARDIYO_DK = 10

    /**
     * Ham oturumları kardiyo listesine çevirir: süre dakikaya yuvarlanır, çok kısa olanlar atılır,
     * aynı egzersiz iki kaynaktan geldiyse (zamanı üst üste binen oturumlar) yalnızca ilki tutulur.
     */
    fun egzersizler(oturumlar: List<EgzersizOturumu>): List<SaatEgzersizi> {
        val sonuc = mutableListOf<SaatEgzersizi>()
        var sonBitis: Instant? = null
        for (o in oturumlar.sortedBy { it.baslangic }) {
            val ustUste = sonBitis?.let { o.baslangic.isBefore(it) } ?: false
            if (ustUste) continue
            sonBitis = o.bitis
            val dk = ((o.bitis.epochSecond - o.baslangic.epochSecond) / 60.0).roundToInt()
            if (dk >= EN_KISA_KARDIYO_DK) sonuc += SaatEgzersizi(o.tur, dk)
        }
        return sonuc
    }

    /**
     * Kardiyo önerileri: saatteki egzersizlerden, o gün zaten kardiyo olarak kaydedilmemiş olanlar.
     * [kayitliSureler]: o günün kardiyo kayıtlarının süreleri (dk). Süresi 1 dakika içinde tutan kayıt aynı sayılır.
     * Öneri yalnızca gösterilir; kaydı kullanıcı onaylar (sıra kendiliğinden ilerlemez).
     */
    fun kardiyoOnerileri(saattekiler: List<SaatEgzersizi>, kayitliSureler: List<Int>): List<SaatEgzersizi> {
        val kalan = kayitliSureler.toMutableList()
        return saattekiler.filter { e ->
            val eslesen = kalan.indexOfFirst { kotlin.math.abs(it - e.sureDk) <= 1 }
            if (eslesen >= 0) {
                kalan.removeAt(eslesen)
                false
            } else true
        }
    }

    /**
     * Saat verisini mevcut kayıtla birleştirir; (adım, uyku) döner.
     *  - Adım: büyük olan kalır (adım gün içinde yalnızca artar; elle girilen daha büyük değer ezilmez).
     *  - Uyku: yalnızca boşsa doldurulur (elle girilen değer ezilmez).
     */
    fun birlestir(adimMevcut: Int, uykuMevcut: Double?, okunan: SaatVerisi): Pair<Int, Double?> =
        maxOf(adimMevcut, okunan.adim ?: 0) to (uykuMevcut ?: okunan.uykuSaat?.takeIf { it > 0 })
}
