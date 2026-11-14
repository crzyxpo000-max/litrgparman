package com.zincir.bildirim

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.zincir.MainActivity
import com.zincir.R
import com.zincir.ZincirUygulamasi
import com.zincir.veri.Germe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Germe rutininin tek adımı (iki taraflı germeler iki adım olur). */
data class GermeAdimi(val ad: String, val bolge: String, val taraf: String?, val sureSn: Int)

/**
 * Çalışan germe rutininin durumu. Süre "kalan saniye" olarak tutulmaz; adımın biteceği an tutulur
 * ([adimBitis], SystemClock.elapsedRealtime cinsinden). Böylece telefon kilitlense de süre şaşmaz.
 */
data class GermeCalisma(
    val adimlar: List<GermeAdimi>,
    val index: Int,
    val adimBitis: Long,
    val duraklatmaKalanMs: Long? = null,   // duraklatıldıysa o andaki kalan süre
    val bitti: Boolean = false,
    val gun: Long,                         // rutinin kaydedileceği gün
) {
    val adim: GermeAdimi get() = adimlar[index]
    val toplamMs: Long get() = adimlar.sumOf { it.sureSn } * 1000L

    /** Şu anki adımda kalan süre. */
    fun kalanMs(simdi: Long): Long = (duraklatmaKalanMs ?: (adimBitis - simdi)).coerceAtLeast(0)

    /** Rutinin başından beri geçen süre (atlanan adımlar tam sayılır). */
    fun gecenMs(simdi: Long): Long =
        if (bitti) toplamMs else adimlar.take(index).sumOf { it.sureSn } * 1000L + (adim.sureSn * 1000L - kalanMs(simdi))
}

/**
 * Germe rutinini yürüten motor. ViewModel'de değil uygulama düzeyinde durur; çünkü ekran kapansa da
 * [GermeServisi] (ön plan servisi) sayesinde çalışmaya devam etmesi gerekir.
 */
object GermeMotoru {
    private val _durum = MutableStateFlow<GermeCalisma?>(null)
    val durum: StateFlow<GermeCalisma?> = _durum

    private val kapsam = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var isi: Job? = null

    private fun simdi() = SystemClock.elapsedRealtime()

    fun baslat(app: ZincirUygulamasi, germeler: List<Germe>, gun: Long) {
        val adimlar = germeler.flatMap { g ->
            if (g.ikiTaraf) listOf(GermeAdimi(g.ad, g.bolge, "Sol", g.sureSn), GermeAdimi(g.ad, g.bolge, "Sağ", g.sureSn))
            else listOf(GermeAdimi(g.ad, g.bolge, null, g.sureSn))
        }
        if (adimlar.isEmpty()) return
        isi?.cancel()
        _durum.value = GermeCalisma(adimlar, index = 0, adimBitis = simdi() + adimlar[0].sureSn * 1000L, gun = gun)
        Titresim.titret(app, 200)
        // Ön plan servisi: ekran kilitlense de uygulama uyutulmaz, geçişlerde titreşim gelir.
        try {
            ContextCompat.startForegroundService(app, Intent(app, GermeServisi::class.java))
        } catch (e: Exception) {
            // Servis başlatılamadıysa rutin yine çalışır; yalnızca arka planda garanti verilemez.
        }
        isi = kapsam.launch {
            while (true) {
                val d = _durum.value ?: break
                if (d.bitti) break
                if (d.duraklatmaKalanMs != null) {
                    delay(200)
                    continue
                }
                val kalan = d.adimBitis - simdi()
                if (kalan > 0) delay(minOf(kalan, 250)) else ilerle(app, d, d.adimBitis)
            }
        }
    }

    /**
     * Sıradaki adıma geçer. [baslangic]: yeni adımın başladığı an. Sayaç gecikmeyle uyandıysa (telefon uyuduysa)
     * önceki adımın bittiği an verilir; böylece gecikme sonraki adıma taşınmaz, toplam süre değişmez.
     */
    private suspend fun ilerle(app: ZincirUygulamasi, d: GermeCalisma, baslangic: Long) {
        val sonraki = d.index + 1
        if (sonraki < d.adimlar.size) {
            val bitis = baslangic + d.adimlar[sonraki].sureSn * 1000L
            _durum.value = d.copy(index = sonraki, adimBitis = bitis)
            if (bitis > simdi()) Titresim.titret(app, 500) // geride kalmış adımlar için ayrı ayrı titreme
        } else {
            val toplamDk = (d.adimlar.sumOf { it.sureSn } + 59) / 60
            _durum.value = d.copy(bitti = true) // önce durumu kapat: aynı bitiş iki kez işlenmesin
            app.depo.gunDegistir(d.gun) { it.copy(germeDk = maxOf(it.germeDk, toplamDk)) }
            Titresim.cift(app)
            if (!ZincirUygulamasi.onPlanda) {
                Bildirimler.goster(app, Bildirimler.ID_GERME_BITTI, "Germe tamamlandı", "$toplamDk dakika bugüne kaydedildi.", Bildirimler.KANAL_ZAMANLAYICI)
            }
        }
    }

    fun duraklat(duraklat: Boolean) {
        val d = _durum.value ?: return
        if (d.bitti) return
        _durum.value = if (duraklat) {
            if (d.duraklatmaKalanMs != null) d else d.copy(duraklatmaKalanMs = (d.adimBitis - simdi()).coerceAtLeast(0))
        } else {
            val kalan = d.duraklatmaKalanMs ?: return
            d.copy(adimBitis = simdi() + kalan, duraklatmaKalanMs = null)
        }
    }

    /** "Geç": bu adımı bırakıp sıradakine şimdi başlar. */
    fun atla(app: ZincirUygulamasi) {
        val d = _durum.value ?: return
        if (d.bitti) return
        kapsam.launch { ilerle(app, d.copy(duraklatmaKalanMs = null), simdi()) }
    }

    /** Rutini kapatır. Yarıda bırakıldıysa geçen tam dakikalar yine de güne yazılır. */
    fun kapat(app: ZincirUygulamasi) {
        val d = _durum.value ?: return
        isi?.cancel()
        _durum.value = null
        if (!d.bitti) {
            val dk = (d.gecenMs(simdi()) / 60_000L).toInt()
            if (dk > 0) kapsam.launch { app.depo.gunDegistir(d.gun) { it.copy(germeDk = it.germeDk + dk) } }
        }
    }
}

/**
 * Germe rutini sürerken çalışan ön plan servisi. Kendisi süre saymaz; [GermeMotoru]'nu izler,
 * bildirim çubuğunda kalan süreyi gösterir ve işlemcinin uyumaması için kilit tutar.
 */
class GermeServisi : Service() {
    private val kapsam = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var uyanikKilit: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val d = GermeMotoru.durum.value
        val bildirim = bildirim(d)
        if (Build.VERSION.SDK_INT >= 34) {
            ServiceCompat.startForeground(this, Bildirimler.ID_GERME_SURUYOR, bildirim, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(Bildirimler.ID_GERME_SURUYOR, bildirim)
        }
        if (d == null || d.bitti) {
            kapat()
            return START_NOT_STICKY
        }
        if (uyanikKilit == null) {
            // En çok rutinin süresi + 5 dakika tutulur; unutulsa bile kendiliğinden bırakılır.
            uyanikKilit = getSystemService(PowerManager::class.java)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "zincir:germe")
                .apply { acquire(d.toplamMs + 5 * 60_000L) }
            kapsam.launch {
                var sonIndex = -1
                var sonDuraklatma: Boolean? = null
                GermeMotoru.durum.collect { durum ->
                    if (durum == null || durum.bitti) {
                        kapat()
                    } else if (durum.index != sonIndex || (durum.duraklatmaKalanMs != null) != sonDuraklatma) {
                        sonIndex = durum.index
                        sonDuraklatma = durum.duraklatmaKalanMs != null
                        getSystemService(android.app.NotificationManager::class.java).notify(Bildirimler.ID_GERME_SURUYOR, bildirim(durum))
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun kapat() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        kapsam.cancel()
        uyanikKilit?.let { if (it.isHeld) it.release() }
        uyanikKilit = null
        super.onDestroy()
    }

    private fun bildirim(d: GermeCalisma?): Notification {
        val ac = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val olusturucu = NotificationCompat.Builder(this, Bildirimler.KANAL_GERME)
            .setSmallIcon(R.drawable.ic_bildirim)
            .setContentIntent(ac)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        if (d == null || d.bitti) return olusturucu.setContentTitle("Germe").build()
        olusturucu
            .setContentTitle("Germe ${d.index + 1} / ${d.adimlar.size}" + (d.adim.taraf?.let { " · $it" } ?: ""))
            .setContentText(d.adim.ad)
        if (d.duraklatmaKalanMs == null) {
            // Bildirimde geri sayan sayaç.
            olusturucu.setWhen(System.currentTimeMillis() + d.kalanMs(SystemClock.elapsedRealtime()))
                .setUsesChronometer(true).setChronometerCountDown(true)
        } else {
            olusturucu.setSubText("Duraklatıldı")
        }
        return olusturucu.build()
    }
}
