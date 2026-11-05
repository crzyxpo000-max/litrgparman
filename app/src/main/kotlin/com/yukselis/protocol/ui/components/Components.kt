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

/** Sistem bildirim kartı: köşeli çerçeve, renkli kenar, tek aralıklı yazı. */
@Composable
fun SystemCard(notice: NoticeView, modifier: Modifier = Modifier) {
    val accent = noticeColor(notice.kind, notice.rarity)
    val shape = RoundedCornerShape(6.dp)
    val spoken = (listOf(notice.title) + notice.lines).joinToString(" ") { if (it.endsWith(".")) it else "$it." }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(accent.copy(alpha = 0.08f))
            .border(1.5.dp, accent.copy(alpha = 0.8f), shape)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) { contentDescription = spoken },
    ) {
        Text(
            text = if (notice.kind == NoticeKind.ECHO) "[ ${glitch(stringResource(R.string.echo_header))} ]" else "[ ${stringResource(R.string.system_header)} ]",
            style = GameType.system,
            color = accent.copy(alpha = 0.8f),
        )
        Spacer(Modifier.height(4.dp))
        val echo = notice.kind == NoticeKind.ECHO
        Text(text = if (echo) glitch(notice.title) else notice.title, style = GameType.systemTitle, color = accent)
        notice.lines.forEach { line ->
            Text(
                text = line,
                style = GameType.system,
                color = if (echo) accent.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurface,
            )
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
) {
    val fraction = if (max <= 0) 0f else (value.toFloat() / max).coerceIn(0f, 1f)
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = GameType.system,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(28.dp),
        )
        LinearProgressIndicator(
            progress = { fraction },
            color = color,
            trackColor = color.copy(alpha = 0.18f),
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
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
    // Türkçede i → İ olmalı; büyük harfe çevirirken seçili dilin yerel ayarı kullanılır.
    val locale = LocalConfiguration.current.locales[0]
    Text(
        text = text.uppercase(locale),
        style = GameType.systemTitle,
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
