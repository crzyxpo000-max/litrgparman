package com.yukselis.protocol.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import kotlinx.coroutines.delay

/**
 * Katmanlı titreşim ipuçları (Paket 7): Sistem bildirimi (hafif tık), büyük an / boss fazı (çift vuruş),
 * Yankı bozulması (hızlı çift titreşim). Ayar kapalıysa sessizdir.
 */
enum class HapticCue { SYSTEM, BIG_MOMENT, ECHO }

suspend fun View.playCue(cue: HapticCue, enabled: Boolean) {
    if (!enabled) return
    when (cue) {
        HapticCue.SYSTEM -> performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        HapticCue.BIG_MOMENT -> {
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            delay(95L)
            val second = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.CONFIRM
            } else {
                HapticFeedbackConstants.LONG_PRESS
            }
            performHapticFeedback(second)
        }
        HapticCue.ECHO -> {
            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            delay(55L)
            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }
}
