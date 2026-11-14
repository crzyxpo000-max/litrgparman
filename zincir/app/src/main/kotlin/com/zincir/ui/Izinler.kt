package com.zincir.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.zincir.bildirim.Bildirimler

/**
 * Zamanlayıcıların (dinlenme, germe) ekran kilitliyken de haber verebilmesi için gereken izinleri
 * ilk kullanımda ister:
 *  1. Bildirim izni (Android 13 ve sonrası sorar).
 *  2. Tam zamanlı alarm izni. Android 13+ bu uygulamaya kendiliğinden verir; yalnızca Android 12/12L'de
 *     kullanıcı kapatmışsa ayar sayfasına yönlendirilir.
 * Her izin yalnızca bir kez sorulur; reddedilirse uygulama çalışmaya devam eder (yalnızca kilitliyken haber gelmez).
 */
@Composable
fun ZamanlayiciIzinleri() {
    val context = LocalContext.current
    val tercih = remember { context.getSharedPreferences("zincir_izin", Context.MODE_PRIVATE) }
    var alarmPenceresi by remember { mutableStateOf(false) }

    fun alarmIzniniSor() {
        if (!Bildirimler.tamAlarmVar(context) && !tercih.getBoolean("alarm_soruldu", false)) alarmPenceresi = true
    }
    val bildirimIzni = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { alarmIzniniSor() }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && !Bildirimler.izinVar(context) && !tercih.getBoolean("bildirim_soruldu", false)) {
            tercih.edit().putBoolean("bildirim_soruldu", true).apply()
            bildirimIzni.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            alarmIzniniSor()
        }
    }

    if (alarmPenceresi) {
        OnayDialog(
            "Tam zamanlı alarm izni",
            "Ekran kilitliyken dinlenme süresi bittiğinde tam zamanında haber verebilmem için izin gerekiyor. " +
                "Açılan sayfada Zincir için \"Alarm ve hatırlatıcılara izin ver\" seçeneğini aç.",
            "Ayarları aç",
            onKapat = {
                tercih.edit().putBoolean("alarm_soruldu", true).apply()
                alarmPenceresi = false
            },
        ) {
            if (Build.VERSION.SDK_INT >= 31) {
                try {
                    context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
                } catch (e: Exception) {
                    // Bu ayar sayfası telefonda yoksa sessizce geç; alarm yaklaşık zamanlı kurulur.
                }
            }
        }
    }
}
