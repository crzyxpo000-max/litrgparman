package com.romanarsiv.presentation.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.romanarsiv.settings.AmoledMode
import com.romanarsiv.settings.ScreenInfo
import com.romanarsiv.settings.ThemeMode
import com.romanarsiv.settings.UiPrefs

private val LightColors = lightColorScheme(
    primary = Color(0xFF2E5E4E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB4EFD6),
    onPrimaryContainer = Color(0xFF002117),
    secondary = Color(0xFF4C6358),
    secondaryContainer = Color(0xFFCEE9DA),
    onSecondaryContainer = Color(0xFF092016),
    tertiary = Color(0xFF3E6374),
    tertiaryContainer = Color(0xFFC2E8FC),
    background = Color(0xFFF6FBF7),
    surface = Color(0xFFF6FBF7),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF0F5F1),
    surfaceContainer = Color(0xFFEAEFEB),
    surfaceContainerHigh = Color(0xFFE4EAE5),
    surfaceContainerHighest = Color(0xFFDFE4E0),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FD1B8),
    onPrimary = Color(0xFF003828),
    primaryContainer = Color(0xFF14503D),
    onPrimaryContainer = Color(0xFFB4EFD6),
    secondary = Color(0xFFB3CCBF),
    secondaryContainer = Color(0xFF354B41),
    onSecondaryContainer = Color(0xFFCEE9DA),
    tertiary = Color(0xFFA6CCE0),
    tertiaryContainer = Color(0xFF254B5B),
    background = Color(0xFF0F1512),
    surface = Color(0xFF0F1512),
    surfaceContainerLowest = Color(0xFF0A0F0D),
    surfaceContainerLow = Color(0xFF171D1A),
    surfaceContainer = Color(0xFF1B211E),
    surfaceContainerHigh = Color(0xFF252B28),
    surfaceContainerHighest = Color(0xFF303633),
)

/** Saf siyah: OLED'de siyah pikseller kapalıdır. Kartlar ayırt edilsin diye çok koyu griler kalır. */
private fun ColorScheme.amoled(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0B0B0B),
    surfaceContainer = Color(0xFF121212),
    surfaceContainerHigh = Color(0xFF1A1A1A),
    surfaceContainerHighest = Color(0xFF222222),
    surfaceBright = Color(0xFF2A2A2A),
    inverseOnSurface = Color.Black,
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun isAppInDarkTheme(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

/** OLED algılandıysa (ya da elle açıldıysa) koyu tema saf siyah olur. */
@Composable
fun usesPureBlack(prefs: UiPrefs): Boolean {
    val context = LocalContext.current
    return when (prefs.amoled) {
        AmoledMode.ON -> true
        AmoledMode.OFF -> false
        AmoledMode.AUTO -> remember { ScreenInfo.isLikelyOled(context) }
    }
}

@Composable
fun ArchiverTheme(prefs: UiPrefs, content: @Composable () -> Unit) {
    val dark = isAppInDarkTheme(prefs.theme)
    val pureBlack = dark && usesPureBlack(prefs)
    val context = LocalContext.current
    val base = when {
        prefs.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = if (pureBlack) base.amoled() else base,
        shapes = AppShapes,
        content = content,
    )
}
