package com.yukselis.protocol.ui.components

import android.content.Context
import android.content.res.Configuration
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/**
 * Pencereler (diyalog, alt sayfa) kendi pencerelerinde yeniden bileşir ve LocalContext / LocalConfiguration /
 * LocalDensity'yi cihaz ayarlarına sıfırlar: oyun içi dil ve yazı boyutu kaybolur. Bu yerel değerler onları taşır.
 */
val LocalAppContext = staticCompositionLocalOf<Context?> { null }
val LocalAppConfiguration = staticCompositionLocalOf<Configuration?> { null }
val LocalAppDensity = staticCompositionLocalOf<Density?> { null }

/** İçeriği oyunun dili ve yazı boyutuyla bileştirir (pencere içinde çağrılır). */
@Composable
fun Localized(content: @Composable () -> Unit) {
    val context = LocalAppContext.current
    val configuration = LocalAppConfiguration.current
    val density = LocalAppDensity.current
    val provided = buildList {
        if (context != null) add(LocalContext provides context)
        if (configuration != null) add(LocalConfiguration provides configuration)
        if (density != null) add(LocalDensity provides density)
    }
    CompositionLocalProvider(*provided.toTypedArray(), content = content)
}

/** Material AlertDialog; her bölümü oyunun diliyle bileşir. Uygulamada AlertDialog yerine bu kullanılır. */
@Composable
fun AppAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = { Localized(confirmButton) },
        modifier = modifier,
        dismissButton = dismissButton?.let { d -> { Localized(d) } },
        title = title?.let { t -> { Localized(t) } },
        text = text?.let { t -> { Localized(t) } },
    )
}

/**
 * Denetim etiketleri (alt sekmeler, savaş düğmeleri, başlık logosu) için yazı ölçeği tavanı (UI_REVIEW C2–C4): cihaz
 * ölçeği 2.0 olsa da bu metinler en çok [max] katına büyür; okuma metni (anlatı, seçenekler) etkilenmez.
 */
@Composable
fun CappedFontScale(max: Float = 1.3f, content: @Composable () -> Unit) {
    val d = LocalDensity.current
    if (d.fontScale <= max) content()
    else CompositionLocalProvider(LocalDensity provides androidx.compose.ui.unit.Density(d.density, max), content = content)
}
