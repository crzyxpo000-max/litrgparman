package com.romanarsiv.settings

import android.content.Context
import android.content.res.Resources
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display

/**
 * Ekranın OLED/AMOLED olup olmadığını tahmin eder. Android'de bunu doğrudan söyleyen bir API yok;
 * iki güvenilir işaret kullanılır:
 *
 * 1. **Always-on display**: sistem yapılandırmasında AOD varsa (`config_dozeAlwaysOnDisplayAvailable`).
 *    Ekran sürekli açık kalabiliyorsa pratikte hep OLED'dir (LCD'de arka ışık yüzünden yapılmaz).
 * 2. **HDR destekli panel**: telefonlarda HDR10/HLG/Dolby Vision panelleri neredeyse hep OLED'dir.
 *
 * İkisi de yoksa LCD sayılır. Yanılırsa kullanıcı Ayarlar'dan "Saf siyah"ı elle açıp kapatabilir.
 */
object ScreenInfo {

    @Volatile private var cached: Boolean? = null

    fun isLikelyOled(context: Context): Boolean = cached ?: detect(context.applicationContext).also { cached = it }

    private fun detect(context: Context): Boolean = hasAlwaysOnDisplay() || hasHdrPanel(context)

    @Suppress("DiscouragedApi") // Herkese açık API yok; yalnızca okunur, yoksa 0 döner.
    private fun hasAlwaysOnDisplay(): Boolean = runCatching {
        val res = Resources.getSystem()
        val id = res.getIdentifier("config_dozeAlwaysOnDisplayAvailable", "bool", "android")
        id != 0 && res.getBoolean(id)
    }.getOrDefault(false)

    @Suppress("DEPRECATION") // hdrCapabilities API 34'te Display.Mode'a taşındı; eski sürümler için gerekli.
    private fun hasHdrPanel(context: Context): Boolean = runCatching {
        val display = context.getSystemService(DisplayManager::class.java)?.getDisplay(Display.DEFAULT_DISPLAY)
            ?: return false
        val types = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            display.mode.supportedHdrTypes
        } else {
            display.hdrCapabilities?.supportedHdrTypes ?: IntArray(0)
        }
        types.isNotEmpty()
    }.getOrDefault(false)
}
