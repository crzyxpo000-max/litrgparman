package com.yukselis.protocol.ui.theme

import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yukselis.engine.story.NoticeKind
import com.yukselis.engine.system.Rarity
import com.yukselis.protocol.R

/*
 * Görsel kimlik "Kırık pencere" (docs/UI_DIRECTION.md, A + C): Sistem kusursuz bir cam pencere (kesik köşe, tek buz
 * mavisi); kahramanın anomalisi ve Yankı bu kusursuzluğu bozar (ters video, macenta kayma, çöken ekran).
 * Bütün renk, yazı, şekil ve hareket kararları bu dosyadadır; ekranlar yalnızca buradaki tokenları kullanır.
 */

// ---------------------------------------------------------------- renk

/** Oyunun kendi renk tokenları (Material şemasının taşımadıkları dahil). */
@Immutable
data class GameColors(
    val isDark: Boolean,
    /** Sayfa zemini. Koyu temada saf siyah: AMOLED'de pikseller kapanır, LCD'de gece mavisinden ayırt edilmez. */
    val night: Color,
    /** Sistem penceresinin içi. */
    val panel: Color,
    /** Bir kademe yukarı yüzey (dock, alt sayfa). */
    val panelHigh: Color,
    /** İnce çizgiler, boş çubuk izi. */
    val line: Color,
    val text: Color,
    val muted: Color,
    /** Sistem'in sesi: buz mavisi. Tek baskın vurgu. */
    val system: Color,
    /** Pencere kenarı (sistemin biraz daha doygun hali). */
    val edge: Color,
    /** Uyarı, geri sayım, artan değer. */
    val amber: Color,
    val danger: Color,
    val success: Color,
    /** Yankı: düzene sızan tek yanlış renk. */
    val echo: Color,
    /** Yankı'nın ters video bloğu: açık zemin, koyu yazı. */
    val echoPaper: Color,
    val echoInk: Color,
    /** Benzersiz (Unique) nadirlik: beyaz-altın. */
    val unique: Color,
    /** NPC sesi. */
    val companion: Color,
    /** Kule, izler, destansı nadirlik. */
    val violet: Color,
)

private val DarkGame = GameColors(
    isDark = true,
    night = Color(0xFF000000),
    panel = Color(0xFF0B1220),
    panelHigh = Color(0xFF101A2C),
    line = Color(0xFF1C2C44),
    text = Color(0xFFDCE6F2),
    muted = Color(0xFF8C9DB4),
    system = Color(0xFF8BE9FF),
    edge = Color(0xFF3FD3FF),
    amber = Color(0xFFFFB547),
    danger = Color(0xFFFF6B6B),
    success = Color(0xFF7BD88F),
    echo = Color(0xFFFF3DCB),
    echoPaper = Color(0xFFF1EDF6),
    echoInk = Color(0xFF1A0F1E),
    unique = Color(0xFFFFE08A),
    companion = Color(0xFFF48FB1),
    violet = Color(0xFFC39BFF),
)

/** Açık tema: aynı kimlik, gündüz okuması için. Bütün vurgular beyaz zeminde en az AA (4,5:1). */
private val LightGame = GameColors(
    isDark = false,
    night = Color(0xFFF3F6FA),
    panel = Color(0xFFFFFFFF),
    panelHigh = Color(0xFFE8EEF6),
    line = Color(0xFFC9D4E3),
    text = Color(0xFF10161F),
    muted = Color(0xFF4F5D70),
    system = Color(0xFF006A85),
    edge = Color(0xFF0089AB),
    amber = Color(0xFF8A5300),
    danger = Color(0xFFB3261E),
    success = Color(0xFF2E6B36),
    echo = Color(0xFFA0137F),
    echoPaper = Color(0xFF1A0F1E),
    echoInk = Color(0xFFF7EEF6),
    unique = Color(0xFF7A5C00),
    companion = Color(0xFFA3305F),
    violet = Color(0xFF6A3FB5),
)

private fun scheme(g: GameColors): ColorScheme {
    val base = if (g.isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = g.system,
        onPrimary = if (g.isDark) Color(0xFF00202B) else Color.White,
        primaryContainer = g.panelHigh,
        onPrimaryContainer = g.system,
        secondary = g.amber,
        onSecondary = if (g.isDark) Color(0xFF2A1A00) else Color.White,
        tertiary = g.echo,
        onTertiary = if (g.isDark) Color.Black else Color.White,
        error = g.danger,
        onError = if (g.isDark) Color.Black else Color.White,
        background = g.night,
        onBackground = g.text,
        surface = g.night,
        onSurface = g.text,
        surfaceVariant = g.panelHigh,
        onSurfaceVariant = g.muted,
        surfaceTint = Color.Transparent,
        surfaceDim = g.night,
        surfaceBright = g.panelHigh,
        surfaceContainerLowest = g.night,
        surfaceContainerLow = g.panel,
        surfaceContainer = g.panel,
        surfaceContainerHigh = g.panelHigh,
        surfaceContainerHighest = g.panelHigh,
        inverseSurface = g.echoPaper,
        inverseOnSurface = g.echoInk,
        outline = if (g.isDark) Color(0xFF2E4566) else Color(0xFF8A9AB0),
        outlineVariant = g.line,
        scrim = Color.Black,
    )
}

val LocalGameColors = staticCompositionLocalOf { DarkGame }

/** Animasyonlar kapalı mı (cihazın "animasyonları kaldır" ayarı). Kapalıysa büyük anlar hareketsiz gösterilir. */
val LocalReduceMotion = staticCompositionLocalOf { false }

/** Tokenlara tek giriş noktası: `Game.colors.system`, `Game.reduceMotion`. */
object Game {
    val colors: GameColors @Composable get() = LocalGameColors.current
    val reduceMotion: Boolean @Composable get() = LocalReduceMotion.current
}

// ---------------------------------------------------------------- yazı

@OptIn(ExperimentalTextApi::class)
private fun variable(res: Int, weight: Int, style: FontStyle = FontStyle.Normal) =
    Font(res, FontWeight(weight), style, variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

/** Anlatı: uzun okuma için tasarlanmış serif (OFL). */
val Literata = FontFamily(
    variable(R.font.literata, 400),
    variable(R.font.literata, 500),
    variable(R.font.literata, 600),
    variable(R.font.literata_italic, 400, FontStyle.Italic),
)

/** Sistem'in sesi: tek aralıklı (OFL). Yankı'nın bozuk blok karakterleri de bu yazıdadır. */
val JetBrainsMono = FontFamily(
    variable(R.font.jetbrains_mono, 400),
    variable(R.font.jetbrains_mono, 500),
    variable(R.font.jetbrains_mono, 700),
)

/** Başlık ve sayılar: köşeleri kesik grotesk (OFL). */
val ChakraPetch = FontFamily(
    Font(R.font.chakra_petch_medium, FontWeight.Medium),
    Font(R.font.chakra_petch_semibold, FontWeight.SemiBold),
    Font(R.font.chakra_petch_semibold, FontWeight.Bold),
)

/** Oyunun yazı ölçeği. Anlatı 17 sp / 1,65 satır; Sistem 13 sp. Ölçek `Settings.textSize` ile çarpılır. */
object GameType {
    val narration = TextStyle(fontFamily = Literata, fontSize = 17.sp, lineHeight = 28.sp)
    val narrationPast = narration.copy(fontSize = 15.sp, lineHeight = 24.sp)
    val system = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp, lineHeight = 20.sp)
    /** Sistem penceresinin başlığı: geniş aralıklı, kesik köşeli yazı. */
    val systemTitle = TextStyle(fontFamily = ChakraPetch, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 1.2.sp)
    /** Ekran başlıkları (sentence case). */
    val heading = TextStyle(fontFamily = ChakraPetch, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp)
    /** Büyük sayılar: seviye, sıra, kademe. */
    val numeral = TextStyle(fontFamily = ChakraPetch, fontWeight = FontWeight.SemiBold, fontSize = 48.sp, lineHeight = 50.sp)
    val numeralHuge = numeral.copy(fontSize = 80.sp, lineHeight = 82.sp)
}

private val GameTypography = Typography().let { t ->
    t.copy(
        displayLarge = t.displayLarge.copy(fontFamily = ChakraPetch, fontWeight = FontWeight.SemiBold),
        displayMedium = t.displayMedium.copy(fontFamily = ChakraPetch, fontWeight = FontWeight.SemiBold),
        displaySmall = t.displaySmall.copy(fontFamily = ChakraPetch, fontWeight = FontWeight.SemiBold),
        headlineLarge = t.headlineLarge.copy(fontFamily = ChakraPetch, fontWeight = FontWeight.SemiBold),
        headlineMedium = t.headlineMedium.copy(fontFamily = ChakraPetch, fontWeight = FontWeight.SemiBold),
        headlineSmall = t.headlineSmall.copy(fontFamily = ChakraPetch, fontWeight = FontWeight.SemiBold),
        titleLarge = t.titleLarge.copy(fontFamily = ChakraPetch, fontWeight = FontWeight.SemiBold),
        titleMedium = t.titleMedium.copy(fontFamily = ChakraPetch, fontWeight = FontWeight.SemiBold),
        titleSmall = t.titleSmall.copy(fontFamily = ChakraPetch, fontWeight = FontWeight.Medium),
        bodyLarge = t.bodyLarge.copy(fontFamily = Literata, fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = t.bodyMedium.copy(fontFamily = Literata),
        bodySmall = t.bodySmall.copy(fontFamily = Literata),
        labelLarge = t.labelLarge.copy(fontFamily = ChakraPetch, fontWeight = FontWeight.Medium, fontSize = 15.sp),
        labelMedium = t.labelMedium.copy(fontFamily = ChakraPetch, fontWeight = FontWeight.Medium),
        labelSmall = t.labelSmall.copy(fontFamily = ChakraPetch, fontWeight = FontWeight.Medium),
    )
}

// ---------------------------------------------------------------- şekil

/** Pencere köşesi: sol üst ve sağ alt kesik, öbür ikisi keskin. */
fun windowCut(size: Int) = CutCornerShape(topStart = size.dp, topEnd = 0.dp, bottomEnd = size.dp, bottomStart = 0.dp)

object GameShapes {
    val window = windowCut(12)
    val button = windowCut(7)
    val chip = windowCut(4)
}

private val MaterialShapes = Shapes(
    extraSmall = windowCut(3),
    small = windowCut(5),
    medium = windowCut(8),
    large = windowCut(12),
    extraLarge = windowCut(14),
)

// ---------------------------------------------------------------- hareket

/** Süreler (ms). Kısa ve keskin; pencereler kayarak değil açılarak girer. */
object GameMotion {
    const val FAST = 120
    const val BASE = 200
    const val SLOW = 360
    /** Büyük anın toplam süresi; dokununca atlanır. */
    const val MOMENT = 900
}

// ---------------------------------------------------------------- bildirim renkleri

fun rarityColor(rarity: Rarity, g: GameColors = DarkGame): Color = when (rarity) {
    Rarity.COMMON -> g.muted
    Rarity.RARE -> g.system
    Rarity.EPIC -> g.violet
    Rarity.LEGENDARY -> g.amber
    Rarity.MYTHIC -> g.danger
    Rarity.UNIQUE -> g.unique
}

@Composable
fun rarityColorOf(rarity: Rarity): Color = rarityColor(rarity, Game.colors)

/** Yankı rengi (eski adı korunur). */
val EchoColor: Color @Composable get() = Game.colors.echo
/** Pasif sezgi satırları Sistem'in sesidir. */
val InsightColor: Color @Composable get() = Game.colors.system
/** NPC sesi. */
val CompanionColor: Color @Composable get() = Game.colors.companion

@Composable
fun noticeColor(kind: NoticeKind, rarity: Rarity?): Color {
    val g = Game.colors
    if (rarity != null && (kind == NoticeKind.TITLE || kind == NoticeKind.ANALYZE || kind == NoticeKind.WARNING || kind == NoticeKind.SKILL)) {
        return rarityColor(rarity, g)
    }
    return when (kind) {
        NoticeKind.SYSTEM, NoticeKind.STATUS, NoticeKind.ANALYZE, NoticeKind.SKILL, NoticeKind.TITLE -> g.system
        NoticeKind.LEVEL_UP, NoticeKind.EVOLUTION, NoticeKind.CLASS -> g.amber
        NoticeKind.QUEST -> g.success
        NoticeKind.WARNING -> g.danger
        NoticeKind.ECHO -> g.echo
        NoticeKind.COMPANION -> g.companion
    }
}

/**
 * Yankı bildirimleri için bozuk görünüm: belirli harflere birleşik "kesik" işareti ekler.
 * Belirleyicidir (aynı metin hep aynı görünür); ekran okuyucuya düz metin verilir.
 */
fun glitch(text: String): String = buildString {
    text.forEachIndexed { i, c ->
        append(c)
        if (c.isLetter() && (i * 7 + c.code) % 5 == 0) append('̶')
    }
}

// ---------------------------------------------------------------- tema

/**
 * Tek koyu tema (DECISIONS #148): zemin saf siyah, paneller gece mavisi. Android ekranın AMOLED olup olmadığını
 * güvenilir biçimde söylemez; saf siyah zemin AMOLED'de pikselleri kapatır, LCD'de gece mavisinden ayırt edilmez.
 */
@Composable
fun YukselisTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val game = if (dark) DarkGame else LightGame
    val context = LocalContext.current
    val reduce = remember(context) {
        runCatching { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }.getOrDefault(false)
    }
    CompositionLocalProvider(LocalGameColors provides game, LocalReduceMotion provides reduce) {
        MaterialTheme(
            colorScheme = scheme(game),
            typography = GameTypography,
            shapes = MaterialShapes,
            content = content,
        )
    }
}
