package com.zincir

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Looper
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zincir.bildirim.Bildirimler
import com.zincir.bildirim.GermeAdimi
import com.zincir.bildirim.GermeCalisma
import com.zincir.bildirim.GermeMotoru
import com.zincir.bildirim.HatirlatmaAlici
import com.zincir.veri.Germe
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

/** Zamanlayıcı testleri: süreler elapsedRealtime farkından hesaplanır; telefon uyusa da şaşmaz. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class ZamanlayiciTesti {
    private val app: ZincirUygulamasi = ApplicationProvider.getApplicationContext()
    private val gun = 20780L

    @After
    fun temizle() {
        GermeMotoru.kapat(app)
        ZincirUygulamasi.onPlanda = false
    }

    private fun ilerlet(saniye: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(saniye))

    /** Arka plandaki veritabanı işi bitene kadar bekler. */
    private fun bekle(kosul: () -> Boolean) {
        repeat(300) {
            shadowOf(Looper.getMainLooper()).idle()
            if (kosul()) return
            Thread.sleep(20)
        }
        throw AssertionError("Koşul sağlanmadı")
    }

    @Test
    fun `kalan sure bitis ani ile saat farkindan hesaplanir`() {
        val adimlar = listOf(GermeAdimi("a", "x", "Sol", 60), GermeAdimi("a", "x", "Sağ", 60), GermeAdimi("b", "y", null, 30))
        val c = GermeCalisma(adimlar, index = 1, adimBitis = 100_000, gun = gun)
        assertEquals(150_000, c.toplamMs)
        assertEquals(60_000, c.kalanMs(simdi = 40_000))
        assertEquals(15_000, c.kalanMs(simdi = 85_000))
        assertEquals(0, c.kalanMs(simdi = 500_000))                      // süre geçmişse eksiye düşmez
        assertEquals(60_000 + 45_000, c.gecenMs(simdi = 85_000))         // 1. adım tam + 2. adımın 45 sn'si
        // Duraklatılmışken saat ilerlese de kalan süre değişmez.
        val d = c.copy(duraklatmaKalanMs = 20_000)
        assertEquals(20_000, d.kalanMs(simdi = 999_000))
    }

    @Test
    fun `germe rutini adim adim ilerler ve gunu kaydeder`() {
        val germeler = listOf(Germe(sira = 1, ad = "Arka bacak", bolge = "x", sureSn = 60, ikiTaraf = true), Germe(sira = 2, ad = "Sırt", bolge = "y", sureSn = 60))
        GermeMotoru.baslat(app, germeler, gun)
        assertEquals(3, GermeMotoru.durum.value!!.adimlar.size)
        assertEquals("Sol", GermeMotoru.durum.value!!.adim.taraf)

        ilerlet(59)
        assertEquals(0, GermeMotoru.durum.value!!.index)
        ilerlet(2)
        assertEquals(1, GermeMotoru.durum.value!!.index)
        assertEquals("Sağ", GermeMotoru.durum.value!!.adim.taraf)

        // Duraklatınca süre durur.
        GermeMotoru.duraklat(true)
        ilerlet(300)
        assertEquals(1, GermeMotoru.durum.value!!.index)
        GermeMotoru.duraklat(false)
        assertEquals(59_000.0, GermeMotoru.durum.value!!.kalanMs(SystemClock.elapsedRealtime()).toDouble(), 1500.0)

        ilerlet(60)
        assertEquals(2, GermeMotoru.durum.value!!.index)
        ilerlet(61)
        bekle { GermeMotoru.durum.value?.bitti == true }
        bekle { runBlocking { app.depo.dao.gun(gun)?.germeDk == 3 } }
    }

    @Test
    fun `telefon uyuyup uyaninca rutin dogru adimdan surer`() {
        val germeler = (1..4).map { Germe(sira = it, ad = "Germe $it", bolge = "x", sureSn = 60) }
        GermeMotoru.baslat(app, germeler, gun)
        val baslangic = SystemClock.elapsedRealtime()
        // Saat 150 sn ilerliyor ama uygulama bu sırada hiç çalışmıyor (telefon uykuda).
        ShadowSystemClock.advanceBy(Duration.ofSeconds(150))
        ilerlet(1)
        val d = GermeMotoru.durum.value!!
        // 151. saniye: 3. adımdayız (120–180 sn) ve bu adımın bitişi başlangıçtan tam 180 sn sonra.
        assertEquals(2, d.index)
        assertEquals(baslangic + 180_000, d.adimBitis)
    }

    @Test
    fun `yarida birakilan rutinin tam dakikalari kaydedilir`() {
        GermeMotoru.baslat(app, (1..4).map { Germe(sira = it, ad = "Germe $it", bolge = "x", sureSn = 60) }, gun + 1)
        ilerlet(130)
        GermeMotoru.kapat(app)
        assertNull(GermeMotoru.durum.value)
        bekle { runBlocking { app.depo.dao.gun(gun + 1)?.germeDk == 2 } }
    }

    @Test
    fun `dinlenme bitisine tam zamanli uyandiran alarm kurulur`() {
        val bitis = SystemClock.elapsedRealtime() + 120_000
        Bildirimler.dinlenmeAlarmiKur(app, bitis)
        val alarm = shadowOf(app.getSystemService(AlarmManager::class.java)).scheduledAlarms.single()
        assertEquals(AlarmManager.ELAPSED_REALTIME_WAKEUP, alarm.type)
        assertEquals(bitis, alarm.triggerAtMs)
        assertTrue("ekran kilitliyken (Doze) de çalmalı", alarm.isAllowWhileIdle)
        Bildirimler.dinlenmeAlarminiIptalEt(app)
        assertTrue(shadowOf(app.getSystemService(AlarmManager::class.java)).scheduledAlarms.isEmpty())
    }

    @Test
    fun `dinlenme bitince uygulama arka plandaysa bildirim gelir ondeyse gelmez`() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val yonetici = shadowOf(app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        val alarm = Intent(app, HatirlatmaAlici::class.java).setAction(Bildirimler.DINLENME)

        ZincirUygulamasi.onPlanda = true
        HatirlatmaAlici().onReceive(app, alarm)
        assertNull(yonetici.getNotification(Bildirimler.ID_DINLENME))

        ZincirUygulamasi.onPlanda = false
        HatirlatmaAlici().onReceive(app, alarm)
        val bildirim = yonetici.getNotification(Bildirimler.ID_DINLENME)
        assertNotNull(bildirim)
        assertEquals(Bildirimler.KANAL_ZAMANLAYICI, bildirim.channelId)
        // Uygulama öne gelince bildirim kaldırılır.
        Bildirimler.zamanlayiciBildirimleriniKaldir(app)
        assertNull(yonetici.getNotification(Bildirimler.ID_DINLENME))
    }
}
