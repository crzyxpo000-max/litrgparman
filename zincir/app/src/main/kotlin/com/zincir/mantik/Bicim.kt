package com.zincir.mantik

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Sayı ve tarih biçimleri: Türkçe, ondalık ayırıcı virgül. */
object Bicim {
    val TR: Locale = Locale.forLanguageTag("tr-TR")

    /** 82.5 → "82,5", 80.0 → "80", 2250.0 → "2.250" */
    fun ondalik(d: Double, hane: Int = 1): String =
        NumberFormat.getNumberInstance(TR).apply { maximumFractionDigits = hane; minimumFractionDigits = 0 }.format(d)

    /** 2250 → "2.250" */
    fun tam(n: Int): String = NumberFormat.getIntegerInstance(TR).format(n)

    /** Giriş kutusuna yazılacak biçim (binlik ayırıcı yok): 12.5 → "12,5" */
    fun giris(d: Double, hane: Int = 1): String =
        NumberFormat.getNumberInstance(TR).apply { maximumFractionDigits = hane; minimumFractionDigits = 0; isGroupingUsed = false }.format(d)

    /** İşaretli fark: -0.6 → "−0,6", 0.3 → "+0,3" */
    fun fark(d: Double, hane: Int = 1): String {
        val s = ondalik(kotlin.math.abs(d), hane)
        return when {
            s == "0" -> "0"
            d < 0 -> "−$s"
            else -> "+$s"
        }
    }

    /** Kullanıcının yazdığı sayıyı çözer; virgül de nokta da kabul edilir. */
    fun coz(metin: String): Double? = metin.trim().replace(',', '.').toDoubleOrNull()

    fun cozTam(metin: String): Int? = metin.trim().toIntOrNull()

    private val uzun = DateTimeFormatter.ofPattern("d MMMM EEEE", TR)
    private val orta = DateTimeFormatter.ofPattern("d MMMM yyyy", TR)
    private val kisa = DateTimeFormatter.ofPattern("d MMM", TR)
    private val gunAdi = DateTimeFormatter.ofPattern("EEE", TR)

    /** "14 Kasım Cumartesi" */
    fun tarih(gun: Long): String = LocalDate.ofEpochDay(gun).format(uzun)

    /** "23 Kasım 2026" */
    fun tamTarih(gun: Long): String = LocalDate.ofEpochDay(gun).format(orta)

    /** "14 Kas" */
    fun kisaTarih(gun: Long): String = LocalDate.ofEpochDay(gun).format(kisa)

    /** "Cmt" */
    fun gunAdi(gun: Long): String = LocalDate.ofEpochDay(gun).format(gunAdi)

    /** Geri sayım: 92 → "1:32" */
    fun sayac(saniye: Int): String = "%d:%02d".format(saniye / 60, saniye % 60)

    /** Dinlenme süresi: 120 → "2 dk", 90 → "90 sn" */
    fun sure(saniye: Int): String = if (saniye % 60 == 0) "${saniye / 60} dk" else "$saniye sn"

    /** Gece yarısından dakika → "07:30" */
    fun saat(dakika: Int): String = "%02d:%02d".format(dakika / 60, dakika % 60)

    /** "07:30" → dakika; geçersizse null. */
    fun cozSaat(metin: String): Int? {
        val p = metin.trim().split(":", ".", " ")
        if (p.size != 2) return null
        val s = p[0].toIntOrNull() ?: return null
        val d = p[1].toIntOrNull() ?: return null
        return if (s in 0..23 && d in 0..59) s * 60 + d else null
    }
}
