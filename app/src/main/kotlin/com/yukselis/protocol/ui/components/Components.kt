package com.yukselis.protocol.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yukselis.engine.story.NoticeKind
import com.yukselis.engine.story.NoticeView
import com.yukselis.protocol.R
import com.yukselis.protocol.ui.theme.GameType
import com.yukselis.protocol.ui.theme.glitch
import com.yukselis.protocol.ui.theme.noticeColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Sistem bildirim kartı: kesik köşeli pencere, türün renginde kenar, tek aralıklı yazı.
 * Yankı'nın bildirimi pencere değildir: ters video bir blok olarak araya sızar.
 */
@Composable
fun SystemCard(notice: NoticeView, modifier: Modifier = Modifier) {
    val accent = noticeColor(notice.kind, notice.rarity)
    val spoken = (listOf(notice.title) + notice.lines).joinToString(" ") { if (it.endsWith(".")) it else "$it." }
    val locale = LocalConfiguration.current.locales[0]
    if (notice.kind == NoticeKind.ECHO) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = modifier.semantics(mergeDescendants = true) { contentDescription = spoken },
        ) {
            EchoLine(glitch(notice.title), style = GameType.system.copy(fontWeight = FontWeight.Medium))
            notice.lines.forEach { EchoLine(it) }
        }
        return
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .systemWindow(accent = accent.copy(alpha = 0.85f))
            .padding(horizontal = 14.dp, vertical = 11.dp)
            .semantics(mergeDescendants = true) { contentDescription = spoken },
    ) {
        val header = if (notice.kind == NoticeKind.COMPANION) stringResource(R.string.companion_header) else stringResource(R.string.system_header)
        Text(text = "[ ${header.uppercase(locale)} ]", style = GameType.systemTitle.copy(fontSize = 11.sp), color = accent)
        Spacer(Modifier.height(5.dp))
        Text(text = notice.title, style = GameType.systemTitle, color = MaterialTheme.colorScheme.onSurface)
        notice.lines.forEach { line ->
            Text(text = line, style = GameType.system, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f))
        }
    }
}

/** Etiketli ince çubuk: HP, MP, XP. */
@Composable
fun StatBar(
    label: String,
    value: Long,
    max: Long,
    color: Color,
    modifier: Modifier = Modifier,
    labelWidth: Int = 28,
) {
    val fraction = if (max <= 0) 0f else (value.toFloat() / max).coerceIn(0f, 1f)
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = GameType.system,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            // Büyük yazıda etiket kesilmesin: en az bu genişlik, gerekirse daha geniş.
            modifier = Modifier.widthIn(min = labelWidth.dp).padding(end = 4.dp),
        )
        LinearProgressIndicator(
            progress = { fraction },
            color = color,
            trackColor = color.copy(alpha = 0.18f),
            gapSize = 0.dp,
            drawStopIndicator = {},
            modifier = Modifier
                .weight(1f)
                .height(6.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$value/$max",
            style = GameType.system,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    // Bölüm başlığı: büyük harf değil, kesik köşeli başlık yazısı (DECISIONS: arayüz kimliği).
    Text(
        text = text,
        style = GameType.heading.copy(fontSize = 17.sp),
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(top = 20.dp, bottom = 8.dp),
    )
}

@Composable
fun CenteredNote(text: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = text, style = GameType.narration, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
