package com.zincir.mantik

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KiloTest {
    private val bas = 20780L

    /** Her haftaya verilen ortalamayla 7 tartı yazar. */
    private fun haftalar(vararg ortalamalar: Double): List<HaftaOrt> =
        ortalamalar.mapIndexed { i, o -> HaftaOrt(i, o, 7) }

    @Test
    fun `haftalik ortalama program haftasina gore hesaplanir`() {
        val tartilar = listOf(
            Tarti(bas, 96.0), Tarti(bas + 1, 95.0), Tarti(bas + 6, 95.6),   // 1. hafta
            Tarti(bas + 7, 95.0), Tarti(bas + 9, 94.0),                      // 2. hafta
        )
        val h = Kilo.haftalikOrtalamalar(tartilar, bas)
        assertEquals(2, h.size)
        assertEquals(0, h[0].index)
        assertEquals(95.5333, h[0].ortalama, 0.001)
        assertEquals(3, h[0].adet)
        assertEquals(94.5, h[1].ortalama, 0.001)
        assertEquals(-1.0333, Kilo.fark(h, 1)!!, 0.001)
        assertNull("önceki hafta yoksa fark yok", Kilo.fark(h, 0))
    }

    @Test
    fun `baslangictan onceki tartilar eksi haftaya duser`() {
        val h = Kilo.haftalikOrtalamalar(listOf(Tarti(bas - 2, 96.4)), bas)
        assertEquals(-1, h.single().index)
    }

    @Test
    fun `ilk iki haftada su notu gosterilir`() {
        val o = Kilo.oneriler(haftalar(96.0, 94.2), buHafta = 2, baslangicKilo = 96.0)
        assertEquals(listOf(Oneri.SU_NOTU), o)
    }

    @Test
    fun `haftada 0,4 ile 0,8 arasi dususte degistirme`() {
        val o = Kilo.oneriler(haftalar(96.0, 94.5, 94.0, 93.4), buHafta = 4, baslangicKilo = 96.0)
        assertEquals(listOf(Oneri.DEGISTIRME), o)
        // Sınırlar dahil.
        assertEquals(listOf(Oneri.DEGISTIRME), Kilo.oneriler(haftalar(96.0, 94.5, 94.0, 93.6), 4, 96.0))
        assertEquals(listOf(Oneri.DEGISTIRME), Kilo.oneriler(haftalar(96.0, 94.5, 94.0, 93.2), 4, 96.0))
    }

    @Test
    fun `iki hafta ust uste 0,3ten az dususte azalt`() {
        val o = Kilo.oneriler(haftalar(96.0, 94.5, 94.4, 94.3), buHafta = 4, baslangicKilo = 96.0)
        assertEquals(listOf(Oneri.AZALT), o)
    }

    @Test
    fun `tek hafta yavas dusus azalt demez`() {
        val o = Kilo.oneriler(haftalar(96.0, 94.5, 94.0, 93.9), buHafta = 4, baslangicKilo = 96.0)
        assertEquals(listOf(Oneri.IZLE), o)
    }

    @Test
    fun `ilk iki haftanin dususu yavas hafta sayiminda kullanilmaz`() {
        // 3. hafta (index 2) yavaş, ama önceki kayıp ilk 2 haftaya ait: "azalt" denmez.
        val o = Kilo.oneriler(haftalar(96.0, 95.9, 95.8), buHafta = 3, baslangicKilo = 96.0)
        assertEquals(listOf(Oneri.IZLE), o)
    }

    @Test
    fun `ucuncu haftadan sonra haftada 1 kilodan fazla dususte ekle`() {
        val o = Kilo.oneriler(haftalar(96.0, 94.5, 94.0, 92.8), buHafta = 4, baslangicKilo = 96.0)
        assertEquals(listOf(Oneri.EKLE), o)
        // 3. haftada (index 2) hızlı düşüş henüz "ekle" demez.
        assertEquals(listOf(Oneri.IZLE), Kilo.oneriler(haftalar(96.0, 94.5, 93.2), 3, 96.0))
    }

    @Test
    fun `her bes kiloda kalori dusurme onerisi`() {
        // 91,2 → 90,6: başlangıca göre 5 kg eşiği bu hafta geçildi.
        val o = Kilo.oneriler(haftalar(96.0, 94.0, 93.0, 92.0, 91.2, 90.6), buHafta = 6, baslangicKilo = 96.0)
        assertEquals(listOf(Oneri.DEGISTIRME, Oneri.BES_KG), o)
        // Bir sonraki hafta eşik yeniden geçilmediği için öneri tekrarlanmaz.
        val sonra = Kilo.oneriler(haftalar(96.0, 94.0, 93.0, 92.0, 91.2, 90.6, 90.0), buHafta = 7, baslangicKilo = 96.0)
        assertEquals(listOf(Oneri.DEGISTIRME), sonra)
    }

    @Test
    fun `icinde bulunulan hafta en az uc tarti olunca degerlendirilir`() {
        val h = listOf(HaftaOrt(0, 96.0, 7), HaftaOrt(1, 94.5, 7), HaftaOrt(2, 94.0, 7), HaftaOrt(3, 93.4, 2))
        // 4. haftada yalnızca 2 tartı var: 3. hafta değerlendirilir (0,5 kg düşüş).
        assertEquals(listOf(Oneri.DEGISTIRME), Kilo.oneriler(h, buHafta = 3, baslangicKilo = 96.0))
        val h3 = h.dropLast(1) + HaftaOrt(3, 92.9, 3)
        assertEquals(listOf(Oneri.EKLE), Kilo.oneriler(h3, buHafta = 3, baslangicKilo = 96.0))
    }

    @Test
    fun `veri yoksa oneri yok`() {
        assertTrue(Kilo.oneriler(emptyList(), 3, 96.0).isEmpty())
    }

    @Test
    fun `bicim virgul kullanir`() {
        assertEquals("82,5", Bicim.ondalik(82.5))
        assertEquals("80", Bicim.ondalik(80.0))
        assertEquals("94,0", Bicim.kilo(94.0))
        assertEquals("2.250", Bicim.tam(2250))
        assertEquals("−0,6", Bicim.fark(-0.6))
        assertEquals("+0,3", Bicim.fark(0.3))
        assertEquals(82.5, Bicim.coz("82,5")!!, 0.0001)
        assertEquals(82.5, Bicim.coz("82.5")!!, 0.0001)
        assertEquals("1:32", Bicim.sayac(92))
        assertEquals(450, Bicim.cozSaat("07:30"))
    }
}
