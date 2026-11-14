package com.zincir.mantik

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SiraTest {
    private val gun0 = 20780L // herhangi bir gün

    @Test
    fun `dongu iki haftalik siraya uyar`() {
        val beklenen = listOf(
            Is.SEANS_A, Is.KARDIYO, Is.SEANS_B, Is.ADIM, Is.SEANS_A, Is.KARDIYO, Is.ADIM,
            Is.SEANS_B, Is.KARDIYO, Is.SEANS_A, Is.ADIM, Is.SEANS_B, Is.KARDIYO, Is.ADIM,
        )
        assertEquals(beklenen, (0 until 14).map { Sira.isAt(it) })
        // 14. adımdan sonra başa döner.
        assertEquals(Is.SEANS_A, Sira.isAt(14))
        // Her 7 adımda 3 ağırlık seansı var.
        assertEquals(3, (0 until 7).count { Sira.isAt(it).agirlik })
        assertEquals(3, (7 until 14).count { Sira.isAt(it).agirlik })
    }

    @Test
    fun `her gun yapilirsa sira gun gun ilerler`() {
        var d = SiraDurumu(0, gun0)
        val gorulen = mutableListOf<Is>()
        for (gun in gun0 until gun0 + 14) {
            d = Sira.guncelle(d, gun)
            val isi = Sira.isAt(d.index)
            gorulen += isi
            if (isi != Is.ADIM) d = Sira.tamamla(d, gun)
        }
        assertEquals(Sira.DONGU, gorulen)
    }

    @Test
    fun `atlanan gun sirayi bozmaz`() {
        var d = SiraDurumu(0, gun0)
        // Üç gün boyunca hiçbir şey yapılmadı: hâlâ Seans A bekliyor.
        d = Sira.guncelle(d, gun0 + 3)
        assertEquals(Is.SEANS_A, Sira.isAt(d.index))
        // Yapılınca sıradaki Kardiyo olur ve ertesi günden itibaren bekler.
        d = Sira.tamamla(d, gun0 + 3)
        assertEquals(Is.KARDIYO, Sira.isAt(d.index))
        assertTrue(Sira.bugunTamam(d, gun0 + 3))
        assertFalse(Sira.bugunTamam(d, gun0 + 4))
    }

    @Test
    fun `yalnizca adim gunu kendiliginden gecer`() {
        // Sıra "Yalnızca adım"da (index 3) ve o gün atanmış.
        var d = SiraDurumu(3, gun0)
        d = Sira.guncelle(d, gun0)
        assertEquals(Is.ADIM, Sira.isAt(d.index))
        // Ertesi gün: adım günü bitti, sırada Seans A var.
        d = Sira.guncelle(d, gun0 + 1)
        assertEquals(Is.SEANS_A, Sira.isAt(d.index))
        assertEquals(gun0 + 1, d.gun)
    }

    @Test
    fun `uygulama gunlerce acilmazsa yalnizca bir adim gunu gecer`() {
        var d = SiraDurumu(3, gun0)
        d = Sira.guncelle(d, gun0 + 5)
        assertEquals(4, d.index)
        // Seans A, adım gününün ertesi gününden beri bekliyor: 4 gün kaçırıldı.
        assertEquals(4, Sira.kacirilanGun(d, gun0 + 5))
    }

    @Test
    fun `zincir uyarisi agirlik seansinin gunu gecince cikar`() {
        val d = SiraDurumu(0, gun0)
        assertFalse("seans günü henüz geçmedi", Sira.zincirUyarisi(d, gun0))
        assertTrue(Sira.zincirUyarisi(d, gun0 + 1))
    }

    @Test
    fun `ucus ve erteleme gunleri kacirilmis sayilmaz`() {
        val d = SiraDurumu(0, gun0)
        assertFalse(Sira.zincirUyarisi(d, gun0 + 1, muafGunler = setOf(gun0)))
        assertEquals(1, Sira.kacirilanGun(d, gun0 + 3, muafGunler = setOf(gun0, gun0 + 2)))
    }

    @Test
    fun `kardiyo beklerken zincir uyarisi cikmaz`() {
        val d = SiraDurumu(1, gun0)
        assertFalse(Sira.zincirUyarisi(d, gun0 + 4))
    }

    @Test
    fun `atlamak sirayi bugune tasir`() {
        val d = Sira.atla(SiraDurumu(1, gun0), gun0 + 2)
        assertEquals(Is.SEANS_B, Sira.isAt(d.index))
        assertEquals(gun0 + 2, d.gun)
    }

    @Test
    fun `program haftalari baslangictan sayilir`() {
        assertEquals(0, Program.haftaIndex(gun0, gun0))
        assertEquals(0, Program.haftaIndex(gun0, gun0 + 6))
        assertEquals(1, Program.haftaIndex(gun0, gun0 + 7))
        assertEquals(-1, Program.haftaIndex(gun0, gun0 - 1))
        assertEquals(gun0 + 7, Program.haftaBasi(gun0, gun0 + 10))
    }

    @Test
    fun `ilk iki hafta iki set sonra tam set`() {
        assertEquals(2, Program.setSayisi(3, haftaIndex = 0, hafifHafta = false))
        assertEquals(2, Program.setSayisi(3, haftaIndex = 1, hafifHafta = false))
        assertEquals(3, Program.setSayisi(3, haftaIndex = 2, hafifHafta = false))
        assertEquals(2, Program.setSayisi(2, haftaIndex = 5, hafifHafta = false))
    }

    @Test
    fun `hafif hafta sekiz haftada bir onerilir ve setleri yariya indirir`() {
        assertFalse(Program.hafifHaftaOnerilir(6))
        assertTrue(Program.hafifHaftaOnerilir(7))   // 8. hafta
        assertTrue(Program.hafifHaftaOnerilir(15))  // 16. hafta
        assertEquals(2, Program.setSayisi(3, haftaIndex = 7, hafifHafta = true))
        assertEquals(1, Program.setSayisi(2, haftaIndex = 7, hafifHafta = true))
    }
}
