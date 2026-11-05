package com.yukselis.protocol.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.yukselis.engine.story.NoticeKind
import com.yukselis.engine.system.Rarity

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4FC3F7),
    onPrimary = Color(0xFF00263A),
    secondary = Color(0xFFB39DDB),
    background = Color(0xFF0B1020),
    onBackground = Color(0xFFE6E9F2),
    surface = Color(0xFF121A2E),
    onSurface = Color(0xFFE6E9F2),
    surfaceVariant = Color(0xFF1B2540),
    onSurfaceVariant = Color(0xFFA9B3CC),
    outline = Color(0xFF34436B),
)

/**
 * AMOLED: arka plan ve yüzeyler saf siyah. AMOLED ekranda siyah pikseller kapalıdır;
 * pil tasarrufu ve en yüksek kontrast. Ayırıcı yüzeyler çok koyu gri kalır ki kartlar seçilebilsin.
 */
private val AmoledColors = DarkColors.copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceTint = Color.Black,
    surfaceVariant = Color(0xFF101318),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF07080B),
    surfaceContainer = Color(0xFF0A0C10),
    surfaceContainerHigh = Color(0xFF101318),
    surfaceContainerHighest = Color(0xFF151920),
    outline = Color(0xFF2A3245),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0277BD),
    onPrimary = Color.White,
    secondary = Color(0xFF5E35B1),
    background = Color(0xFFF6F7FB),
    onBackground = Color(0xFF141824),
    surface = Color.White,
    onSurface = Color(0xFF141824),
    surfaceVariant = Color(0xFFE6EAF4),
    onSurfaceVariant = Color(0xFF4A5470),
    outline = Color(0xFFB8C1D9),
)

/** Romanı okuyormuş hissi için anlatı serif, Sistem penceresi tek aralıklı yazılır. */
object GameType {
    val narration = TextStyle(fontFamily = FontFamily.Serif, fontSize = 17.sp, lineHeight = 27.sp)
    val narrationPast = narration.copy(fontSize = 15.sp, lineHeight = 23.sp)
    val system = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 19.sp)
    val systemTitle = system.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold)
}

/** Yankı'nın bozuk bildirimleri ve pasif sezgi satırları için. */
val EchoColor = Color(0xFFE040FB)
val InsightColor = Color(0xFF26C6DA)

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

fun rarityColor(rarity: Rarity): Color = when (rarity) {
    Rarity.COMMON -> Color(0xFF9AA0A6)
    Rarity.RARE -> Color(0xFF42A5F5)
    Rarity.EPIC -> Color(0xFFAB47BC)
    Rarity.LEGENDARY -> Color(0xFFFFA726)
    Rarity.MYTHIC -> Color(0xFFEF5350)
    Rarity.UNIQUE -> Color(0xFFFFD54F)
}

@Composable
fun noticeColor(kind: NoticeKind, rarity: Rarity?): Color {
    if (rarity != null && (kind == NoticeKind.TITLE || kind == NoticeKind.ANALYZE || kind == NoticeKind.WARNING)) {
        return rarityColor(rarity)
    }
    return when (kind) {
        NoticeKind.SYSTEM, NoticeKind.STATUS -> MaterialTheme.colorScheme.primary
        NoticeKind.ANALYZE -> Color(0xFF26A69A)
        NoticeKind.LEVEL_UP -> Color(0xFFFFD54F)
        NoticeKind.TITLE -> Color(0xFF42A5F5)
        NoticeKind.QUEST -> Color(0xFF66BB6A)
        NoticeKind.WARNING -> Color(0xFFFF7043)
        NoticeKind.SKILL -> Color(0xFF29B6F6)
        NoticeKind.EVOLUTION -> Color(0xFFFFA726)
        NoticeKind.CLASS -> Color(0xFFAB47BC)
        NoticeKind.ECHO -> EchoColor
    }
}

@Composable
fun YukselisTheme(dark: Boolean = isSystemInDarkTheme(), amoled: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = when {
            !dark -> LightColors
            amoled -> AmoledColors
            else -> DarkColors
        },
        typography = Typography(),
        content = content,
    )
}
