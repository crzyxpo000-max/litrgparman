package com.zincir.mantik

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class SaglikTest {
    private val dakka = ZoneOffset.ofHours(6)      // Bangladeş
    private val istanbul = ZoneOffset.ofHours(3)   // Türkiye
    private val gun = LocalDate.of(2026, 12, 2).toEpochDay()

    private fun an(gunAy: Int, saat: Int, dakika: Int, ofset: ZoneOffset): Instant =
        LocalDateTime.of(2026, 12, gunAy, saat, dakika).toInstant(ofset)

    @Test
    fun `uyku uyanilan gune yazilir`() {
        // 1 Aralık 23:30 → 2 Aralık 06:30 (Dakka): 7 saat, 2 Aralık'ın uykusu.
        val gece = UykuOturumu(an(1, 23, 30, dakka), an(2, 6, 30, dakka), dakka)
        assertEquals(7.0, Saglik.uykuSaati(listOf(gece), gun, ZoneId.of("UTC"))!!, 0.001)
        assertNull("önceki günün uykusu değil", Saglik.uykuSaati(listOf(gece), gun - 1, ZoneId.of("UTC")))
    }

    @Test
    fun `gece uykusu ile sekerleme toplanir`() {
        val gece = UykuOturumu(an(1, 23, 0, dakka), an(2, 4, 30, dakka), dakka)       // 5,5 sa
        val sekerleme = UykuOturumu(an(2, 14, 0, dakka), an(2, 15, 12, dakka), dakka) // 1,2 sa
        assertEquals(6.7, Saglik.uykuSaati(listOf(sekerleme, gece), gun, ZoneId.of("UTC"))!!, 0.001)
    }

    @Test
    fun `iki kaynaktan gelen ayni uyku iki kez sayilmaz`() {
        val saat = UykuOturumu(an(1, 23, 0, dakka), an(2, 6, 0, dakka), dakka)
        val kopya = UykuOturumu(an(1, 23, 10, dakka), an(2, 6, 0, dakka), dakka)
        val tasan = UykuOturumu(an(2, 5, 0, dakka), an(2, 6, 30, dakka), dakka) // yarım saat fazlası
        assertEquals(7.5, Saglik.uykuSaati(listOf(saat, kopya, tasan), gun, ZoneId.of("UTC"))!!, 0.001)
    }

    @Test
    fun `gun uyanilan yerin saat dilimine gore belirlenir`() {
        // Aynı an: Dakka'da 2 Aralık 02:00, İstanbul'da 1 Aralık 23:00.
        val bitis = an(2, 2, 0, dakka)
        val dakkada = UykuOturumu(bitis.minusSeconds(4 * 3600), bitis, dakka)
        val istanbulda = UykuOturumu(bitis.minusSeconds(4 * 3600), bitis, istanbul)
        assertEquals(4.0, Saglik.uykuSaati(listOf(dakkada), gun, ZoneId.of("UTC"))!!, 0.001)
        assertNull(Saglik.uykuSaati(listOf(istanbulda), gun, ZoneId.of("UTC")))
        assertEquals(4.0, Saglik.uykuSaati(listOf(istanbulda), gun - 1, ZoneId.of("UTC"))!!, 0.001)
        // Kayıtta saat farkı yoksa telefonun saat dilimi kullanılır.
        val ofsetsiz = UykuOturumu(bitis.minusSeconds(4 * 3600), bitis, null)
        assertEquals(4.0, Saglik.uykuSaati(listOf(ofsetsiz), gun, ZoneId.of("Asia/Dhaka"))!!, 0.001)
    }

    @Test
    fun `adimda buyuk olan kalir uyku yalnizca bossa dolar`() {
        // Saat daha çok adım saydı: saat değeri yazılır.
        assertEquals(6500 to 7.2, Saglik.birlestir(4000, null, SaatVerisi(6500, 7.2, 300)))
        // Elle girilen daha büyük adım ve elle girilen uyku ezilmez.
        assertEquals(9000 to 6.0, Saglik.birlestir(9000, 6.0, SaatVerisi(6500, 7.2, null)))
        // Saatten veri gelmediyse kayıt aynen kalır.
        assertEquals(4000 to null, Saglik.birlestir(4000, null, SaatVerisi(null, null, null)))
        assertEquals(0 to null, Saglik.birlestir(0, null, SaatVerisi(0, 0.0, 0)))
    }

    @Test
    fun `saatteki egzersizler kardiyo listesine cevrilir`() {
        val yuruyus = EgzersizOturumu("Yürüyüş", an(2, 9, 0, dakka), an(2, 9, 35, dakka))
        val kopya = EgzersizOturumu("Yürüyüş", an(2, 9, 1, dakka), an(2, 9, 35, dakka))   // aynı egzersiz, ikinci kaynak
        val kisa = EgzersizOturumu("Bisiklet", an(2, 12, 0, dakka), an(2, 12, 6, dakka))   // 10 dakikadan kısa
        val bisiklet = EgzersizOturumu("Bisiklet", an(2, 18, 0, dakka), an(2, 18, 40, dakka))
        assertEquals(
            listOf(SaatEgzersizi("Yürüyüş", 35), SaatEgzersizi("Bisiklet", 40)),
            Saglik.egzersizler(listOf(bisiklet, kopya, kisa, yuruyus)),
        )
    }

    @Test
    fun `zaten kaydedilmis kardiyo yeniden onerilmez`() {
        val saatte = listOf(SaatEgzersizi("Yürüyüş", 35), SaatEgzersizi("Bisiklet", 40))
        assertEquals(saatte, Saglik.kardiyoOnerileri(saatte, emptyList()))
        // 35 dk'lık kayıt var (1 dk fark aynı sayılır): yalnızca bisiklet önerilir.
        assertEquals(listOf(SaatEgzersizi("Bisiklet", 40)), Saglik.kardiyoOnerileri(saatte, listOf(36)))
        assertEquals(emptyList<SaatEgzersizi>(), Saglik.kardiyoOnerileri(saatte, listOf(40, 35)))
        // Aynı süreli iki egzersizden biri kaydedildiyse diğeri hâlâ önerilir.
        val ikiz = listOf(SaatEgzersizi("Yürüyüş", 30), SaatEgzersizi("Yürüyüş", 30))
        assertEquals(1, Saglik.kardiyoOnerileri(ikiz, listOf(30)).size)
    }
}
