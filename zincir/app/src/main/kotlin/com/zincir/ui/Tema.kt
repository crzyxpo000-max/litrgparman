package com.zincir.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/*
 * Tema: minimalist İsviçre tipografisi. Beyaz zemin, siyah ve koyu gri metin, ince çizgiler, köşesiz kutular.
 * Renk yok; tek vurgu rengi (kırmızı) yalnızca uyarılarda kullanılır ve şemada "error" adıyla durur.
 */

private val Siyah = Color(0xFF0A0A0A)
private val Beyaz = Color(0xFFFFFFFF)
private val KoyuGri = Color(0xFF555555)
private val AcikCizgi = Color(0xFFD6D6D6)
private val AcikZemin = Color(0xFFF3F3F3)
private val VurguAcik = Color(0xFFD0021B)

private val KoyuZemin = Color(0xFF000000)
private val KoyuMetin = Color(0xFFF2F2F2)
private val KoyuGriMetin = Color(0xFFA6A6A6)
private val KoyuCizgi = Color(0xFF3A3A3A)
private val KoyuKutu = Color(0xFF161616)
private val KoyuBant = Color(0xFF2B2B2B)
private val VurguKoyu = Color(0xFFFF5247)

private val AcikRenkler = lightColorScheme(
    primary = Siyah, onPrimary = Beyaz, primaryContainer = AcikZemin, onPrimaryContainer = Siyah,
    secondary = Siyah, onSecondary = Beyaz, secondaryContainer = AcikZemin, onSecondaryContainer = Siyah,
    tertiary = Siyah, onTertiary = Beyaz, tertiaryContainer = AcikZemin, onTertiaryContainer = Siyah,
    background = Beyaz, onBackground = Siyah, surface = Beyaz, onSurface = Siyah,
    surfaceVariant = AcikZemin, onSurfaceVariant = KoyuGri, surfaceTint = Beyaz,
    surfaceContainerLowest = Beyaz, surfaceContainerLow = Beyaz, surfaceContainer = Beyaz,
    surfaceContainerHigh = Beyaz, surfaceContainerHighest = AcikZemin,
    outline = Siyah, outlineVariant = AcikCizgi,
    error = VurguAcik, onError = Beyaz, errorContainer = Beyaz, onErrorContainer = VurguAcik,
    scrim = Color(0x99000000),
)

private val KoyuRenkler = darkColorScheme(
    primary = KoyuMetin, onPrimary = KoyuZemin, primaryContainer = KoyuKutu, onPrimaryContainer = KoyuMetin,
    secondary = KoyuMetin, onSecondary = KoyuZemin, secondaryContainer = KoyuKutu, onSecondaryContainer = KoyuMetin,
    tertiary = KoyuMetin, onTertiary = KoyuZemin, tertiaryContainer = KoyuKutu, onTertiaryContainer = KoyuMetin,
    background = KoyuZemin, onBackground = KoyuMetin, surface = KoyuZemin, onSurface = KoyuMetin,
    surfaceVariant = KoyuBant, onSurfaceVariant = KoyuGriMetin, surfaceTint = KoyuZemin,
    surfaceContainerLowest = KoyuZemin, surfaceContainerLow = KoyuKutu, surfaceContainer = KoyuKutu,
    surfaceContainerHigh = KoyuKutu, surfaceContainerHighest = KoyuKutu,
    outline = KoyuMetin, outlineVariant = KoyuCizgi,
    error = VurguKoyu, onError = KoyuZemin, errorContainer = KoyuZemin, onErrorContainer = VurguKoyu,
    scrim = Color(0xCC000000),
)

private fun stil(boyut: Int, agirlik: FontWeight, satir: Int, aralik: Double = 0.0) = TextStyle(
    fontFamily = FontFamily.SansSerif, fontWeight = agirlik, fontSize = boyut.sp, lineHeight = satir.sp, letterSpacing = aralik.sp,
)

/** Büyük punto, sıkı harf aralığı, az sayıda ağırlık. */
private val Yazi = Typography(
    displayLarge = stil(88, FontWeight.Bold, 92, -3.0),     // germe geri sayımı
    displayMedium = stil(52, FontWeight.Bold, 56, -1.5),    // dinlenme sayacı, büyük sayılar
    displaySmall = stil(40, FontWeight.Bold, 44, -1.0),
    headlineLarge = stil(34, FontWeight.Bold, 38, -0.8),    // ekran başlığı
    headlineMedium = stil(26, FontWeight.Bold, 30, -0.4),
    headlineSmall = stil(22, FontWeight.Bold, 27, -0.2),
    titleLarge = stil(20, FontWeight.Bold, 26),
    titleMedium = stil(18, FontWeight.SemiBold, 24),
    titleSmall = stil(16, FontWeight.SemiBold, 22),
    bodyLarge = stil(18, FontWeight.Normal, 26),
    bodyMedium = stil(16, FontWeight.Normal, 23),
    bodySmall = stil(14, FontWeight.Normal, 20),
    labelLarge = stil(17, FontWeight.SemiBold, 22),         // düğmeler
    labelMedium = stil(12, FontWeight.SemiBold, 16, 1.4),   // BÜYÜK HARF etiketler
    labelSmall = stil(12, FontWeight.Medium, 16, 0.2),
)

private val Koseler = RoundedCornerShape(0.dp).let { Shapes(it, it, it, it, it) }

/** [tema]: 0 sistem, 1 açık, 2 koyu. */
@Composable
fun ZincirTema(tema: Int, content: @Composable () -> Unit) {
    val koyu = when (tema) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    // Durum çubuğundaki saat/pil simgeleri zemine göre koyu ya da açık olsun.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val pencere = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(pencere, view).apply {
                isAppearanceLightStatusBars = !koyu
                isAppearanceLightNavigationBars = !koyu
            }
        }
    }
    MaterialTheme(colorScheme = if (koyu) KoyuRenkler else AcikRenkler, typography = Yazi, shapes = Koseler, content = content)
}
