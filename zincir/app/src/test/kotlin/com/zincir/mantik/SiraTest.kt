package com.zincir.mantik

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SiraTest {
    private val gun0 = 20780L // 23 Kasım 2026, Pazartesi

    @Test
    fun `dongu 14 adimlik tek bir dairesel siradir`() {
        val beklenen = listOf(
            Is.SEANS_A, Is.KARDIYO, Is.SEANS_B, Is.ADIM, Is.SEANS_A, Is.KARDIYO, Is.ADIM,
            Is.SEANS_B, Is.KARDIYO, Is.SEANS_A, Is.ADIM, Is.SEANS_B, Is.KARDIYO, Is.ADIM,
        )
        assertEquals(14, Sira.DONGU.size)
        assertEquals(beklenen, (0 until 14).map { Sira.isAt(it) })
        // 14. adımdan sonra 1. adıma döner.
        assertEquals(Is.SEANS_A, Sira.isAt(14))
        assertEquals(Is.KARDIYO, Sira.isAt(15))
        val son = Sira.tamamla(SiraDurumu(13, gun0), gun0)
        assertEquals(0, son.index)
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
    fun `sira kendiliginden ilerlemez`() {
        // Günlerce hiçbir şey yapılmadı: Seans A da Kardiyo da olduğu yerde bekler.
        assertEquals(SiraDurumu(0, gun0), Sira.guncelle(SiraDurumu(0, gun0), gun0 + 9))
        assertEquals(SiraDurumu(1, gun0), Sira.guncelle(SiraDurumu(1, gun0), gun0 + 9))
    }

    @Test
    fun `yalnizca adim gunu bitince kendiliginden tamamlanir`() {
        var d = SiraDurumu(3, gun0)
        // Aynı gün içinde hâlâ "Yalnızca adım".
        d = Sira.guncelle(d, gun0)
        assertEquals(Is.ADIM, Sira.isAt(d.index))
        // Gün bitti: sırada Seans A var.
        d = Sira.guncelle(d, gun0 + 1)
        assertEquals(Is.SEANS_A, Sira.isAt(d.index))
        assertEquals(gun0 + 1, d.gun)
    }

    @Test
    fun `uygulama gunlerce acilmazsa yalnizca bir adim gunu gecer`() {
        val d = Sira.guncelle(SiraDurumu(3, gun0), gun0 + 5)
        assertEquals(SiraDurumu(4, gun0 + 1), d)
    }

    @Test
    fun `ayni gun birden fazla adim tamamlanabilir`() {
        var d = SiraDurumu(0, gun0)
        d = Sira.tamamla(d, gun0)          // Seans A
        assertEquals(Is.KARDIYO, Sira.isAt(d.index))
        d = Sira.tamamla(d, gun0)          // aynı gün Kardiyo
        assertEquals(Is.SEANS_B, Sira.isAt(d.index))
        // Sıradaki iş yarının işidir; bugün "Yalnızca adım" gelseydi yarın bitene kadar beklerdi.
        assertEquals(gun0 + 1, d.gun)
    }

    @Test
    fun `seanstan sonra gelen adim gunu ertesi gunun isidir`() {
        // Seans B (index 2) bugün yapıldı: Adım yarının işi, öbür gün kendiliğinden geçer.
        var d = Sira.tamamla(SiraDurumu(2, gun0), gun0)
        assertEquals(Is.ADIM, Sira.isAt(d.index))
        d = Sira.guncelle(d, gun0 + 1)
        assertEquals(Is.ADIM, Sira.isAt(d.index))
        d = Sira.guncelle(d, gun0 + 2)
        assertEquals(Is.SEANS_A, Sira.isAt(d.index))
    }

    @Test
    fun `dune kaydedilen seans siradaki isi bugune getirir`() {
        // Gece yarısından sonra "Dün" seçilip seans bitirildi.
        val d = Sira.tamamla(SiraDurumu(0, gun0), gun0 - 1)
        assertEquals(SiraDurumu(1, gun0), d)
    }

    @Test
    fun `yalnizca kardiyo ve adim atlanabilir`() {
        assertTrue(Sira.atlanabilir(Is.KARDIYO))
        assertTrue(Sira.atlanabilir(Is.ADIM))
        assertFalse(Sira.atlanabilir(Is.SEANS_A))
        assertFalse(Sira.atlanabilir(Is.SEANS_B))
    }

    @Test
    fun `atlamak siradaki agirlik seansini one getirir`() {
        // Kardiyo günlerdir bekliyor: atlayınca Seans B bugünün işi olur.
        val d = Sira.atla(SiraDurumu(1, gun0), gun0 + 2)
        assertEquals(Is.SEANS_B, Sira.isAt(d.index))
        assertEquals(gun0 + 2, d.gun)
        // Adım atlanırsa beklemeden sıradaki seansa geçilir.
        assertEquals(Is.SEANS_A, Sira.isAt(Sira.atla(SiraDurumu(3, gun0), gun0).index))
    }

    // ---- Zincir uyarısı

    @Test
    fun `uc tam gun gecince uyari bes tam gun gecince belirgin uyari`() {
        val pazartesi = gun0 // son ağırlık seansı
        // Salı, Çarşamba, Perşembe: henüz 3 tam gün geçmedi.
        assertEquals(ZincirDurumu.YOK, Zincir.durum(pazartesi, gun0, pazartesi + 3))
        // Cuma: Salı, Çarşamba, Perşembe tam geçti.
        assertEquals(3, Zincir.gecenTamGun(pazartesi, pazartesi + 4))
        assertEquals(ZincirDurumu.UYARI, Zincir.durum(pazartesi, gun0, pazartesi + 4))
        assertEquals(ZincirDurumu.UYARI, Zincir.durum(pazartesi, gun0, pazartesi + 5))
        // Pazar: 5 tam gün geçti.
        assertEquals(ZincirDurumu.BELIRGIN, Zincir.durum(pazartesi, gun0, pazartesi + 6))
        assertEquals(ZincirDurumu.BELIRGIN, Zincir.durum(pazartesi, gun0, pazartesi + 20))
    }

    @Test
    fun `plana uyan araliklar uyari vermez`() {
        // Döngüdeki en uzun aralık: 5. gün Seans A, 8. gün Seans B (arada 2 tam gün).
        assertEquals(ZincirDurumu.YOK, Zincir.durum(gun0 + 4, gun0, gun0 + 7))
    }

    @Test
    fun `hic seans yoksa program baslangicindan sayilir`() {
        assertEquals(ZincirDurumu.YOK, Zincir.durum(null, gun0, gun0 - 5)) // program başlamadı
        assertEquals(ZincirDurumu.YOK, Zincir.durum(null, gun0, gun0 + 2))
        assertEquals(ZincirDurumu.UYARI, Zincir.durum(null, gun0, gun0 + 3))
        assertEquals(ZincirDurumu.BELIRGIN, Zincir.durum(null, gun0, gun0 + 5))
    }

    @Test
    fun `sayac son yedi gunun agirlik seanslarini sayar`() {
        val gunler = listOf(gun0 - 7, gun0 - 6, gun0 - 3, gun0)
        // Bugün dahil son 7 gün: gun0-6 … gun0.
        assertEquals(3, Zincir.son7GunSeans(gunler, gun0))
        assertEquals(1, Zincir.son7GunSeans(gunler, gun0 + 6))
        assertEquals(0, Zincir.son7GunSeans(gunler, gun0 + 7))
    }

    // ---- Takvim

    @Test
    fun `program haftalari baslangictan gun sayarak bulunur`() {
        assertEquals(0, Program.haftaIndex(gun0, gun0))
        assertEquals(0, Program.haftaIndex(gun0, gun0 + 6))
        assertEquals(1, Program.haftaIndex(gun0, gun0 + 7))
        assertEquals(-1, Program.haftaIndex(gun0, gun0 - 1))
        // Çarşamba başlayan programda ilk hafta Çarşamba–Salı'dır.
        assertEquals(0, Program.haftaIndex(gun0 + 2, gun0 + 8))
        assertEquals(1, Program.haftaIndex(gun0 + 2, gun0 + 9))
    }

    @Test
    fun `takvim haftasi pazartesi baslar`() {
        assertEquals(gun0, Program.takvimHaftaBasi(gun0))          // Pazartesi
        assertEquals(gun0, Program.takvimHaftaBasi(gun0 + 6))      // Pazar
        assertEquals(gun0 + 7, Program.takvimHaftaBasi(gun0 + 7))  // ertesi Pazartesi
        assertEquals(gun0 - 7, Program.takvimHaftaBasi(gun0 - 1))  // önceki Pazar
        assertEquals(java.time.DayOfWeek.MONDAY, java.time.LocalDate.ofEpochDay(Program.takvimHaftaBasi(gun0 + 3)).dayOfWeek)
        // Çarşamba başlayan programda ilk takvim haftası o Pazartesi–Pazar'dır.
        assertEquals(0, Program.takvimHaftaIndex(gun0 + 2, gun0 + 6))
        assertEquals(1, Program.takvimHaftaIndex(gun0 + 2, gun0 + 7))
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
