package com.yukselis.protocol.ui.status

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yukselis.engine.core.GameMode
import com.yukselis.engine.story.StatLine
import com.yukselis.engine.story.StatusView
import com.yukselis.protocol.R
import com.yukselis.protocol.ui.components.SectionTitle
import com.yukselis.protocol.ui.components.StatBar
import com.yukselis.protocol.ui.theme.GameType
import com.yukselis.protocol.ui.theme.rarityColor

/** Durum penceresi: LitRPG'nin klasik stat ekranı. */
@Composable
fun StatusScreen(status: StatusView, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text(status.playerName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            text = stringResource(R.string.level_grade, status.level, status.grade.name),
            style = GameType.systemTitle,
            color = MaterialTheme.colorScheme.primary,
        )
        val mode = if (status.mode == GameMode.IRONMAN) stringResource(R.string.mode_ironman) else stringResource(R.string.mode_normal)
        Text(
            text = "${stringResource(R.string.day_short, status.day)} · ${stringResource(R.string.act_short, status.act)} · $mode",
            style = GameType.system,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))
        StatBar(stringResource(R.string.hp), status.hp.toLong(), status.maxHp.toLong(), Color(0xFFEF5350))
        Spacer(Modifier.height(6.dp))
        StatBar(stringResource(R.string.mp), status.mp.toLong(), status.maxMp.toLong(), Color(0xFF42A5F5))
        Spacer(Modifier.height(6.dp))
        StatBar("XP", status.xp, status.xpToNext, Color(0xFFFFD54F))
        Spacer(Modifier.height(6.dp))
        Text(
            text = "${stringResource(R.string.stamina)}: ${status.stamina}",
            style = GameType.system,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SectionTitle(stringResource(R.string.stats))
        if (status.freePoints > 0) {
            Text(
                text = stringResource(R.string.free_points, status.freePoints),
                style = GameType.system,
                color = Color(0xFFFFD54F),
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        status.stats.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                pair.forEach { StatCell(it, Modifier.weight(1f)) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
        }

        SectionTitle(stringResource(R.string.titles))
        if (status.titles.isEmpty()) {
            Text(stringResource(R.string.no_titles), style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        status.titles.forEach { title ->
            Column(modifier = Modifier.padding(bottom = 10.dp)) {
                Text(title.name, style = GameType.systemTitle, color = rarityColor(title.rarity))
                Text(title.description, style = GameType.system, color = MaterialTheme.colorScheme.onSurface)
            }
        }

        SectionTitle(stringResource(R.string.morality))
        MoralityBar(stringResource(R.string.cruelty), stringResource(R.string.mercy), status.mercy)
        MoralityBar(stringResource(R.string.freedom), stringResource(R.string.order), status.order)
        MoralityBar(stringResource(R.string.power), stringResource(R.string.humanity), status.humanity)
    }
}

@Composable
private fun StatCell(line: StatLine, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(line.abbr, style = GameType.systemTitle, color = MaterialTheme.colorScheme.primary)
                Text(line.label, style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(line.value.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

/** −100 (sol uç) ile +100 (sağ uç) arasında tek eksen. */
@Composable
private fun MoralityBar(negative: String, positive: String, value: Int) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(negative, style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text(
                text = if (value > 0) "+$value" else "$value",
                style = GameType.systemTitle,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(56.dp),
            )
            Text(
                positive,
                style = GameType.system,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
        }
        val color = MaterialTheme.colorScheme.secondary
        LinearProgressIndicator(
            progress = { ((value + 100) / 200f).coerceIn(0f, 1f) },
            color = color,
            trackColor = color.copy(alpha = 0.18f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
        )
    }
}
