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
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
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
    /** Zamanlayıcı bitişi: yüksek öncelikli, titreşimli. */
    const val KANAL_ZAMANLAYICI = "zamanlayici"
    /** Germe sürerken duran sessiz bildirim. */
    const val KANAL_GERME = "germe"
    const val TARTI = "com.zincir.TARTI"
    const val GERME = "com.zincir.GERME"
    const val OGUN = "com.zincir.OGUN"
    const val DINLENME = "com.zincir.DINLENME"

    const val ID_DINLENME = 10
    const val ID_GERME_BITTI = 11
    const val ID_GERME_SURUYOR = 12

    /** Gece bu saatler arasında öğün hatırlatması gösterilmez. */
    private val SESSIZ_BASLANGIC: LocalTime = LocalTime.of(23, 0)
    private val SESSIZ_BITIS: LocalTime = LocalTime.of(7, 0)

    fun kanalOlustur(context: Context) {
        val yonetici = context.getSystemService(NotificationManager::class.java)
        yonetici.createNotificationChannel(NotificationChannel(KANAL, "Hatırlatmalar", NotificationManager.IMPORTANCE_DEFAULT))
        yonetici.createNotificationChannel(
            NotificationChannel(KANAL_ZAMANLAYICI, "Zamanlayıcı bitişi", NotificationManager.IMPORTANCE_HIGH).apply {
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
            }
        )
        yonetici.createNotificationChannel(NotificationChannel(KANAL_GERME, "Germe sürüyor", NotificationManager.IMPORTANCE_LOW))
    }

    // ---- Dinlenme sayacı: bitiş anına tam zamanlı alarm

    /** Tam zamanlı alarm kurulabiliyor mu? (Android 12–12L'de kullanıcı izni kapatmış olabilir.) */
    fun tamAlarmVar(context: Context): Boolean =
        Build.VERSION.SDK_INT < 31 || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    /**
     * Dinlenme süresinin bittiği ana alarm kurar. [bitis] SystemClock.elapsedRealtime cinsindendir.
     * Ekran kilitli ya da uygulama arka plandaysa alarm [HatirlatmaAlici]'yı uyandırır ve bildirim + titreşim gelir.
     */
    fun dinlenmeAlarmiKur(context: Context, bitis: Long) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = niyet(context, DINLENME)
        if (tamAlarmVar(context)) am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, bitis, pi)
        else am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, bitis, pi) // izin yoksa yaklaşık alarm
    }

    fun dinlenmeAlarminiIptalEt(context: Context) {
        iptal(context, DINLENME)
        NotificationManagerCompat.from(context).cancel(ID_DINLENME)
    }

    /** Uygulama öne gelince: görevini tamamlamış zamanlayıcı bildirimlerini kaldır. */
    fun zamanlayiciBildirimleriniKaldir(context: Context) {
        NotificationManagerCompat.from(context).cancel(ID_DINLENME)
        NotificationManagerCompat.from(context).cancel(ID_GERME_BITTI)
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

    fun goster(context: Context, id: Int, baslik: String, metin: String, kanal: String = KANAL) {
        if (!izinVar(context)) return
        val ac = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val bildirim = NotificationCompat.Builder(context, kanal)
            .setPriority(if (kanal == KANAL_ZAMANLAYICI) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(if (kanal == KANAL_ZAMANLAYICI) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
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
        if (intent.action == Bildirimler.DINLENME) {
            // Uygulama öndeyse titreşimi ekran kendisi yapar; değilse bildirim (titreşimli kanal) haber verir.
            if (!ZincirUygulamasi.onPlanda) {
                Bildirimler.goster(context, Bildirimler.ID_DINLENME, "Dinlenme bitti", "Sıradaki sete geç.", Bildirimler.KANAL_ZAMANLAYICI)
            }
            return
        }
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

/**
 * Titreşim (set dinlenmesi bitti, germe geçişi). "Alarm" kullanımıyla istenir: Android, arka plandaki
 * uygulamaların sıradan titreşimlerini yok sayar, alarm titreşimlerine izin verir.
 */
object Titresim {
    fun titret(context: Context, milisaniye: Long = 400) =
        calistir(context, VibrationEffect.createOneShot(milisaniye, VibrationEffect.DEFAULT_AMPLITUDE))

    /** Üç vuruş: rutin bitti. */
    fun cift(context: Context) = calistir(context, VibrationEffect.createWaveform(longArrayOf(0, 300, 200, 300, 200, 600), -1))

    private fun calistir(context: Context, etki: VibrationEffect) {
        val titrestirici: Vibrator = (
            if (Build.VERSION.SDK_INT >= 31) {
                context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Vibrator::class.java)
            }
            ) ?: return
        if (Build.VERSION.SDK_INT >= 33) {
            titrestirici.vibrate(etki, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            titrestirici.vibrate(etki, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
        }
    }
}
