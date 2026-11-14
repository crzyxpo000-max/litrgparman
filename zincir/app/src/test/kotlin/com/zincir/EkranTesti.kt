package com.zincir

import android.Manifest
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zincir.veri.GunKaydi
import com.zincir.veri.Seans
import com.zincir.veri.SetKaydi
import com.zincir.veri.Tur
import com.zincir.veri.Yedek
import com.zincir.veri.YemekKaydi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

/**
 * Ekran testleri: uygulamayı telefon olmadan (Robolectric ile) açar, düğmelere dokunur ve
 * build/ekran/ klasörüne ekran görüntüleri yazar. Asıl amaç "uygulama açılıyor ve temel akışlar çöküyor mu" kontrolüdür.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w360dp-h1700dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EkranTesti {
    @get:Rule
    val kural = createAndroidComposeRule<MainActivity>()

    private val depo get() = (kural.activity.application as ZincirUygulamasi).depo
    private val bugun = LocalDate.now().toEpochDay()

    private fun bekle(metin: String) = kural.waitUntil(15_000) {
        kural.onAllNodesWithText(metin, substring = true).fetchSemanticsNodes().isNotEmpty()
    }

    private fun tikla(metin: String, kaydir: Boolean = true) {
        val dugum = kural.onAllNodes(hasText(metin, substring = true) and hasClickAction()).onFirst()
        if (kaydir) dugum.performScrollTo()
        dugum.performClick()
        kural.waitForIdle()
    }

    /** Alt sekmeye dokunur (ad tam eşleşmeli; "Bugün" ile "Bugünkü tartı…" karışmasın). */
    private fun sekme(ad: String) {
        kural.onAllNodes(hasText(ad) and hasClickAction()).onFirst().performClick()
        kural.waitForIdle()
    }

    /** Pencerenin ekran görüntüsünü build/ekran/ klasörüne yazar. */
    private fun cek(ad: String) {
        kural.waitForIdle()
        val pencere = kural.activity.window
        val resim = Bitmap.createBitmap(pencere.decorView.width, pencere.decorView.height, Bitmap.Config.ARGB_8888)
        PixelCopy.request(pencere, resim, { }, Handler(Looper.getMainLooper()))
        shadowOf(Looper.getMainLooper()).idle()
        val klasor = File("build/ekran").apply { mkdirs() }
        FileOutputStream(File(klasor, "$ad.png")).use { resim.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** Programın 4. haftasındaymış gibi örnek veri yazar. Son ağırlık seansı 5 gün önce; sırada Seans A var. */
    private suspend fun ornekVeriYaz() {
        val dao = depo.dao
        depo.ayarlariDegistir { it.copy(baslangicGunu = bugun - 24, siraIndex = 0, siraGunu = bugun - 1) }
        // Tartılar: 24 günde 96,0 → yaklaşık 93,5.
        for (g in 24 downTo 0) {
            val dalga = if (g % 3 == 0) 0.3 else if (g % 3 == 1) -0.2 else 0.0
            depo.olcumKaydet(Tur.KILO, bugun - g, 96.0 - (24 - g) * 0.105 + dalga)
        }
        depo.olcumKaydet(Tur.BEL, bugun - 16, 101.0)
        depo.olcumKaydet(Tur.ESNEKLIK, bugun - 5, 14.0)
        // Geçmiş Seans A kayıtları: son seansta ilk hareketin tüm setleri 12 tekrar (ilerleme önerisi çıkmalı).
        val hareketler = dao.hareketler("A")
        listOf(17L to 40.0, 10L to 45.0, 5L to 50.0).forEach { (once, kg) ->
            val id = dao.seansEkle(Seans(gun = bugun - once, tur = Tur.A, tamamlandi = true))
            hareketler.forEachIndexed { i, h ->
                repeat(2) { s -> dao.setEkle(SetKaydi(seansId = id, hareketId = h.id, hareketAdi = h.ad, setNo = s + 1, kg = if (h.agirlikli) kg / (i + 1) else 0.0, tekrar = if (i == 0) 12 else 10)) }
            }
        }
        dao.seansEkle(Seans(gun = bugun - 2, tur = Tur.KARDIYO, kardiyoTuru = "Bisiklet", sureDk = 35, tamamlandi = true))
        for (g in 1..6) {
            dao.gunYaz(GunKaydi(gun = bugun - g, adim = 7000 + g * 500, suMl = 2500, uykuSaat = 6.5 + g % 2, germeDk = if (g % 2 == 0) 10 else 0))
            dao.yemekEkle(YemekKaydi(gun = bugun - g, zaman = 0, ad = "Gün toplamı", kcal = 2100.0 + g * 30, protein = 140.0 + g))
        }
    }

    /**
     * Tek, uçtan uca senaryo. (Yazı kutusu içeren pencereler Robolectric'te takıldığı için burada açılmıyor;
     * onların arkasındaki işlemler doğrudan depo üzerinden deneniyor.)
     */
    @Test
    fun `uygulama acilir ve temel akislar calisir`() {
        // Bildirim izni verilmiş say (izin penceresi test ortamında açılamaz).
        shadowOf(kural.activity.application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        // 1) İlk açılış: başlangıç verisi yazılmış olmalı.
        bekle("Seans A")
        kural.onNodeWithText("Program başlangıcı", substring = true).assertExists()
        // Program başlamadan önce de sıradaki iş görünür; "tamam" yazmaz.
        kural.onNodeWithText("Başla").assertExists()
        assertEquals(0, kural.onAllNodesWithText("tamam.", substring = true).fetchSemanticsNodes().size)
        runBlocking {
            assertEquals(12, depo.dao.hareketSayisi())
            assertEquals(2250, depo.dao.ogunler().sumOf { it.kcal })
        }
        cek("01-ilk-acilis")

        // 2) Programın 4. haftası: zincir uyarısı ve sıradaki iş.
        runBlocking { ornekVeriYaz() }
        bekle("İki seansı üst üste kaçırma.")
        cek("02-bugun")
        tikla("+500 ml")
        bekle("0,5")

        // 3) Antrenman: seansı başlat, bir set kaydet, bitir.
        tikla("Başla")
        bekle("Leg press")
        kural.onNodeWithText("Ağırlığı artır, 8 tekrardan başla.").assertExists()
        cek("03-seans")
        tikla("Set 1 kaydet")
        bekle("Set 2 kaydet")
        bekle("Geç") // dinlenme sayacı
        cek("04-seans-dinlenme")
        tikla("Seansı bitir")
        bekle("Eksik setler var")
        tikla("Bitir", kaydir = false)
        bekle("Kardiyoyu kaydet")
        cek("05-antrenman")

        // Seans bitti: sıra Kardiyo'ya geçti; aynı gün sıradaki iş de yapılabilir; son 7 gün sayacı arttı.
        runBlocking { assertEquals(1, depo.dao.ayarlar()!!.siraIndex) }
        sekme("Bugün")
        bekle("Bugün yapıldı: Seans A")
        bekle("2/3")
        assertEquals(0, kural.onAllNodesWithText("İki seansı üst üste kaçırma.").fetchSemanticsNodes().size)
        cek("06-bugun-yapildi")

        // Kardiyo atlanabilir; ağırlık seansı atlanamaz.
        tikla("Bu adımı atla")
        bekle("yapılmadan sıradaki işe")
        tikla("Atla", kaydir = false)
        bekle("Seans B")
        runBlocking { assertEquals(2, depo.dao.ayarlar()!!.siraIndex) }
        assertEquals(0, kural.onAllNodesWithText("Bu adımı atla").fetchSemanticsNodes().size)

        // "Planımı hatırla" tek dokunuşla açılır.
        tikla("Planımı hatırla")
        bekle("Eğer canım tatlı isterse")
        tikla("Tamam", kaydir = false)

        // "‹ Dün": girişler önceki güne yazılır.
        tikla("‹ Dün", kaydir = false)
        bekle("düne (")
        tikla("+500 ml")
        kural.waitUntil(15_000) { runBlocking { depo.dao.gun(bugun - 1)?.suMl == 3000 } }
        runBlocking { assertEquals(500, depo.dao.gun(bugun)!!.suMl) }
        cek("06b-dun")
        tikla("Bugün ›", kaydir = false)
        bekle("Bugün yapıldı: Seans A")

        // 4) Beslenme: öğünü tek dokunuşla işaretle; hızlı ekleme listesi açılır.
        sekme("Beslenme")
        bekle("Planlandığı gibi yedim")
        tikla("Planlandığı gibi yedim")
        bekle("geri al")
        runBlocking { assertEquals(600.0, depo.dao.yemekKayitlari(bugun).sumOf { it.kcal }, 0.01) }
        tikla("Hızlı ekle")
        bekle("Tavuk göğsü")
        tikla("Vazgeç", kaydir = false)
        runBlocking {
            val tavuk = depo.dao.yiyeceklerAkisi().first().first { it.ad.startsWith("Tavuk göğsü") }
            depo.hizliEkle(tavuk, 150.0, bugun)
            assertEquals(600.0 + 247.5, depo.dao.yemekKayitlari(bugun).sumOf { it.kcal }, 0.01)
        }
        bekle("150 g")
        cek("07-beslenme")

        // 5) Esneklik: zamanlayıcı açılır ve kapanır.
        sekme("Esneklik")
        bekle("Başlat · 10 dk")
        cek("08-esneklik")
        tikla("Başlat · 10 dk")
        bekle("Rutini bitir")
        cek("09-germe-zamanlayici")
        tikla("Rutini bitir", kaydir = false)
        bekle("Başlat · 10 dk")

        // 6) İlerleme: grafik, öneriler, haftalık gözden geçirme.
        sekme("İlerleme")
        bekle("Sabah, aç karnına.")
        cek("10-ilerleme")
        tikla("Haftalık gözden geçirme")
        bekle("Tartı ortalaması")
        cek("11-gozden-gecirme")
        tikla("‹ Geri")

        // 7) Ayarlar, düzenleme listesi, koyu tema.
        sekme("Bugün")
        tikla("Ayarlar", kaydir = false)
        bekle("Program başlangıcı")
        cek("12-ayarlar")
        tikla("Hareketler")
        bekle("Dumbbell bench press")
        cek("13-hareketler")
        tikla("‹ Ayarlar")
        tikla("Koyu")
        tikla("‹ Geri")
        bekle("Bugün yapıldı: Seans A")
        cek("14-bugun-koyu")
        sekme("İlerleme")
        bekle("Sabah, aç karnına.")
        cek("15-ilerleme-koyu")

        // 8) Yedek: dışa aktar, veriyi boz, geri yükle.
        runBlocking {
            val dao = depo.dao
            val json = Yedek.disaAktar(depo.db)
            val setOnce = dao.setSayisi(1)
            depo.ayarlariDegistir { it.copy(kaloriHedef = 1900, siraIndex = 5) }
            depo.seansSil(1)
            assertEquals(0, dao.setSayisi(1))
            Yedek.iceAktar(depo.db, json)
            assertEquals(2250, dao.ayarlar()!!.kaloriHedef)
            assertEquals(2, dao.ayarlar()!!.siraIndex)
            assertEquals(setOnce, dao.setSayisi(1))
            // Geçersiz dosya reddedilir ve hiçbir şeyi değiştirmez.
            assertTrue(runCatching { Yedek.iceAktar(depo.db, "{\"uygulama\":\"baska\"}") }.isFailure)
            assertEquals(12, dao.hareketSayisi())
        }
    }
}
