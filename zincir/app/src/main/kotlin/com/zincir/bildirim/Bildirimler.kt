package com.zincir.bildirim

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.zincir.MainActivity
import com.zincir.R
import com.zincir.ZincirUygulamasi
import com.zincir.veri.Ayarlar
import com.zincir.veri.Tur
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Yerel hatırlatmalar. İnternet ya da sunucu yok: telefonun kendi alarm servisi (AlarmManager) belirlenen
 * saatte [HatirlatmaAlici]'yı uyandırır, o da bildirimi gösterir. Alarmlar "yaklaşık" türdendir
 * (birkaç dakika gecikebilir); bu sayede özel bir izin gerekmez.
 */
object Bildirimler {
    private const val KANAL = "hatirlatma"
    const val TARTI = "com.zincir.TARTI"
    const val GERME = "com.zincir.GERME"
    const val OGUN = "com.zincir.OGUN"

    /** Gece bu saatler arasında öğün hatırlatması gösterilmez. */
    private val SESSIZ_BASLANGIC: LocalTime = LocalTime.of(23, 0)
    private val SESSIZ_BITIS: LocalTime = LocalTime.of(7, 0)

    fun kanalOlustur(context: Context) {
        val kanal = NotificationChannel(KANAL, "Hatırlatmalar", NotificationManager.IMPORTANCE_DEFAULT)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(kanal)
    }

    fun izinVar(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun niyet(context: Context, eylem: String): PendingIntent = PendingIntent.getBroadcast(
        context, eylem.hashCode(), Intent(context, HatirlatmaAlici::class.java).setAction(eylem),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Ayarlara göre günlük alarmları kurar ya da iptal eder. Ayar değişince ve telefon açılınca çağrılır. */
    fun planla(context: Context, a: Ayarlar) {
        gunluk(context, TARTI, a.bildirimTarti, a.tartiDakika)
        gunluk(context, GERME, a.bildirimGerme, a.germeDakika)
        if (!a.bildirimOgun) iptal(context, OGUN)
    }

    private fun gunluk(context: Context, eylem: String, acik: Boolean, dakika: Int) {
        if (!acik) {
            iptal(context, eylem)
            return
        }
        var zaman = LocalDate.now().atTime(dakika / 60, dakika % 60).atZone(ZoneId.systemDefault())
        if (!zaman.isAfter(ZonedDateTime.now())) zaman = zaman.plusDays(1)
        context.getSystemService(AlarmManager::class.java)
            .setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, zaman.toInstant().toEpochMilli(), niyet(context, eylem))
    }

    /** Son yemekten [Ayarlar.ogunAralikSaat] saat sonrasına öğün hatırlatması kurar. */
    fun ogunPlanla(context: Context, a: Ayarlar, sonYemekZamani: Long?) {
        val an = sonYemekZamani?.plus(a.ogunAralikSaat * 3_600_000L)
        if (!a.bildirimOgun || an == null || an <= System.currentTimeMillis()) {
            iptal(context, OGUN)
            return
        }
        context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, an, niyet(context, OGUN))
    }

    private fun iptal(context: Context, eylem: String) {
        context.getSystemService(AlarmManager::class.java).cancel(niyet(context, eylem))
    }

    fun sessizSaat(simdi: LocalTime = LocalTime.now()): Boolean = simdi >= SESSIZ_BASLANGIC || simdi < SESSIZ_BITIS

    fun goster(context: Context, id: Int, baslik: String, metin: String) {
        if (!izinVar(context)) return
        val ac = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val bildirim = NotificationCompat.Builder(context, KANAL)
            .setSmallIcon(R.drawable.ic_bildirim)
            .setContentTitle(baslik)
            .setContentText(metin)
            .setStyle(NotificationCompat.BigTextStyle().bigText(metin))
            .setContentIntent(ac)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, bildirim)
        } catch (e: SecurityException) {
            // Bildirim izni bu arada geri alınmış: sessizce geç.
        }
    }
}

/** Alarm çalınca ya da telefon yeniden başlayınca Android bu sınıfı çağırır. */
class HatirlatmaAlici : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val bekleyen = goAsync() // veritabanı okuması bitene kadar alıcıyı açık tut
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = (context.applicationContext as ZincirUygulamasi).depo.dao
                val a = dao.ayarlar() ?: return@launch
                val bugun = LocalDate.now().toEpochDay()
                when (intent.action) {
                    Intent.ACTION_BOOT_COMPLETED -> {
                        Bildirimler.planla(context, a)
                        Bildirimler.ogunPlanla(context, a, dao.sonYemekZamani())
                    }
                    Bildirimler.TARTI -> {
                        if (a.bildirimTarti && dao.olcum(Tur.KILO, bugun) == null) {
                            Bildirimler.goster(context, 1, "Sabah tartısı", "Aç karnına tartıl ve kiloyu kaydet.")
                        }
                        Bildirimler.planla(context, a) // yarın için yeniden kur
                    }
                    Bildirimler.GERME -> {
                        if (a.bildirimGerme && (dao.gun(bugun)?.germeDk ?: 0) < a.germeHedefDk) {
                            Bildirimler.goster(context, 2, "Akşam germesi", "${a.germeHedefDk} dakika. Başlat'a bas, gerisi kendiliğinden ilerler.")
                        }
                        Bildirimler.planla(context, a)
                    }
                    Bildirimler.OGUN -> {
                        val bugunYenen = dao.yemekKayitlari(bugun).mapNotNull { it.ogunId }.toSet()
                        val kalanOgun = dao.ogunler().any { !it.pay && it.id !in bugunYenen }
                        if (a.bildirimOgun && kalanOgun && bugunYenen.isNotEmpty() && !Bildirimler.sessizSaat()) {
                            Bildirimler.goster(context, 3, "Öğün zamanı", "Son öğünden beri ${a.ogunAralikSaat} saat geçti. Sıradaki plandaki öğün.")
                        }
                    }
                }
            } finally {
                bekleyen.finish()
            }
        }
    }
}

/** Kısa titreşim (set dinlenmesi bitti, germe geçişi). */
object Titresim {
    fun titret(context: Context, milisaniye: Long = 400) {
        val titrestirici: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
        titrestirici?.vibrate(VibrationEffect.createOneShot(milisaniye, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    /** İki kısa vuruş: rutin bitti. */
    fun cift(context: Context) {
        val titrestirici: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
        titrestirici?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 200, 300, 200, 600), -1))
    }
}
