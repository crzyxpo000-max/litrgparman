package com.zincir.mantik

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IlerlemeTest {
    @Test
    fun `tum setler ust sinira ulastiysa agirlik artirilir`() {
        val onceki = listOf(SetDegeri(20.0, 12), SetDegeri(20.0, 12), SetDegeri(20.0, 13))
        assertTrue(Ilerleme.agirlikArtirilmali(onceki, tekrarUst = 12))
    }

    @Test
    fun `bir set bile eksikse agirlik artirilmaz`() {
        val onceki = listOf(SetDegeri(20.0, 12), SetDegeri(20.0, 12), SetDegeri(20.0, 11))
        assertFalse(Ilerleme.agirlikArtirilmali(onceki, tekrarUst = 12))
        assertFalse("kayıt yoksa öneri yok", Ilerleme.agirlikArtirilmali(emptyList(), 12))
    }

    @Test
    fun `ilerleme onerisinde tekrar alt sinira iner`() {
        val onceki = listOf(SetDegeri(20.0, 12), SetDegeri(20.0, 12), SetDegeri(20.0, 12))
        val o = Ilerleme.oner(onceki, setSayisi = 3, tekrarMin = 8, tekrarUst = 12, donus = false)
        assertTrue(o.agirlikArtir)
        assertEquals(List(3) { SetDegeri(20.0, 8) }, o.setler)
    }

    @Test
    fun `oneri yoksa onceki degerler aynen dolar`() {
        val onceki = listOf(SetDegeri(20.0, 12), SetDegeri(20.0, 10))
        val o = Ilerleme.oner(onceki, setSayisi = 3, tekrarMin = 8, tekrarUst = 12, donus = false)
        assertFalse(o.agirlikArtir)
        // Üçüncü set için önceki kayıt yok: son setin değeri kullanılır.
        assertEquals(listOf(SetDegeri(20.0, 12), SetDegeri(20.0, 10), SetDegeri(20.0, 10)), o.setler)
    }

    @Test
    fun `onceki kayit yoksa sifir kilo ve alt tekrar`() {
        val o = Ilerleme.oner(emptyList(), setSayisi = 2, tekrarMin = 10, tekrarUst = 12, donus = false)
        assertEquals(List(2) { SetDegeri(0.0, 10) }, o.setler)
    }

    @Test
    fun `yedi gunden fazla ara verildiyse donus kurali gecerli`() {
        assertFalse(Ilerleme.kesintidenDonus(sonAgirlikGunu = 100, bugun = 107))
        assertTrue(Ilerleme.kesintidenDonus(sonAgirlikGunu = 100, bugun = 108))
        assertFalse("hiç seans yoksa dönüş yok", Ilerleme.kesintidenDonus(null, 108))
    }

    @Test
    fun `donuste agirliklar yuzde seksene iner ve yarim kiloya yuvarlanir`() {
        val onceki = listOf(SetDegeri(20.0, 12), SetDegeri(22.5, 12))
        val o = Ilerleme.oner(onceki, setSayisi = 2, tekrarMin = 8, tekrarUst = 12, donus = true)
        assertTrue(o.donus)
        assertFalse("dönüşte ağırlık artırma önerilmez", o.agirlikArtir)
        assertEquals(listOf(SetDegeri(16.0, 12), SetDegeri(18.0, 12)), o.setler)
    }

    @Test
    fun `yuvarlama yarim kiloya yapilir`() {
        assertEquals(18.0, Ilerleme.yuvarla(17.8), 0.0001)
        assertEquals(10.5, Ilerleme.yuvarla(10.4), 0.0001)
    }
}
