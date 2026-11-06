package com.yukselis.protocol.ui.status

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yukselis.engine.core.GameMode
import com.yukselis.engine.story.NoticeView
import com.yukselis.engine.story.StatLine
import com.yukselis.engine.story.StatusView
import com.yukselis.engine.system.Stat
import com.yukselis.protocol.R
import com.yukselis.protocol.ui.components.SectionTitle
import com.yukselis.protocol.ui.components.StatBar
import com.yukselis.protocol.ui.components.SystemCard
import com.yukselis.protocol.ui.theme.EchoColor
import com.yukselis.protocol.ui.theme.GameType
import com.yukselis.protocol.ui.theme.rarityColor
import java.text.NumberFormat
import kotlin.math.floor
import kotlin.math.log10

/** Durum penceresi: LitRPG'nin klasik stat ekranı, serbest puan dağıtımıyla. */
@Composable
fun StatusScreen(
    status: StatusView,
    notices: List<NoticeView>,
    onAllocate: (Map<Stat, Int>) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Onaylanmamış dağıtım; onaydan sonra geri alınamaz.
    val pending = remember(status.freePoints, status.stats) { mutableStateMapOf<Stat, Int>() }
    val spent = pending.values.sum()
    var confirm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        notices.forEach { SystemCard(it, Modifier.padding(bottom = 8.dp)) }
        Text(status.playerName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            text = listOfNotNull(stringResource(R.string.level_grade, status.level, status.grade.name), status.className).joinToString(" · "),
            style = GameType.systemTitle,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.humanity_percentile, formatPercent(status.humanityTopPercent)),
            style = GameType.systemTitle,
            color = EchoColor,
        )
        val mode = if (status.mode == GameMode.IRONMAN) stringResource(R.string.mode_ironman) else stringResource(R.string.mode_normal)
        Text(
            text = "${stringResource(R.string.day_short, status.day)} · ${stringResource(R.string.act_short, status.act)} · $mode",
            style = GameType.system,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // OP MC: Dikkat, Yozlaşma ve NPC'lerin gördüğü seviye.
        Text(
            text = stringResource(R.string.status_hidden, status.publicLevel, status.attention, status.corruption),
            style = GameType.system,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (status.publicRank > 0) {
            Text(stringResource(R.string.status_rank, status.publicRank), style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (status.armyPower > 0) {
            Text(stringResource(R.string.status_army, status.armyPower), style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (status.evolutionPending) {
            Text(stringResource(R.string.evolution_pending), style = GameType.system, color = Color(0xFFFFA726), modifier = Modifier.padding(top = 4.dp))
        }

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
        val left = status.freePoints - spent
        if (status.freePoints > 0) {
            Text(
                text = stringResource(R.string.free_points, left),
                style = GameType.system,
                color = Color(0xFFFFD54F),
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        status.stats.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                pair.forEach { line ->
                    StatCell(
                        line = line,
                        added = pending[line.stat] ?: 0,
                        canAdd = left > 0,
                        onAdd = { pending[line.stat] = (pending[line.stat] ?: 0) + 1 },
                        onRemove = { pending[line.stat] = ((pending[line.stat] ?: 0) - 1).coerceAtLeast(0) },
                        allocating = status.freePoints > 0,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
        }
        if (spent > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { pending.clear() }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.reset)) }
                Button(onClick = { confirm = true }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.confirm)) }
            }
        }

        if (status.essenceSlots > 0 || status.essences.isNotEmpty()) {
            SectionTitle(stringResource(R.string.essence))
            Text(
                text = stringResource(R.string.essence_slots, status.essences.size, status.essenceSlots),
                style = GameType.system,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            status.essences.forEach { Text(it, style = GameType.systemTitle, color = Color(0xFFFFA726)) }
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

        if (status.traits.isNotEmpty()) {
            SectionTitle(stringResource(R.string.traits))
            status.traits.forEach { t ->
                Column(modifier = Modifier.padding(bottom = 10.dp)) {
                    Text(t.name, style = GameType.systemTitle, color = rarityColor(t.rarity))
                    Text(t.description, style = GameType.system, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        SectionTitle(stringResource(R.string.morality))
        MoralityBar(stringResource(R.string.cruelty), stringResource(R.string.mercy), status.mercy)
        MoralityBar(stringResource(R.string.freedom), stringResource(R.string.order), status.order)
        MoralityBar(stringResource(R.string.power), stringResource(R.string.humanity), status.humanity)
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.confirm_allocation)) },
            text = { Text(stringResource(R.string.confirm_allocation_desc)) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    onAllocate(pending.filterValues { it > 0 }.toMap())
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

/** 0.00042 → "0,00042" (TR) / "0.00042" (EN); en az iki anlamlı basamak. */
@Composable
private fun formatPercent(value: Double): String {
    val locale = LocalConfiguration.current.locales[0]
    val decimals = if (value >= 10) 0 else (1 - floor(log10(value))).toInt().coerceIn(1, 10)
    return NumberFormat.getNumberInstance(locale).apply {
        maximumFractionDigits = decimals
        minimumFractionDigits = 0
    }.format(value)
}

@Composable
private fun StatCell(
    line: StatLine,
    added: Int,
    canAdd: Boolean,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    allocating: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small, modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(line.abbr, style = GameType.systemTitle, color = MaterialTheme.colorScheme.primary)
                    Text(line.label, style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = if (added > 0) "${line.value + added}" else "${line.value}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (added > 0) Color(0xFFFFD54F) else MaterialTheme.colorScheme.onSurface,
                )
            }
            if (allocating) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                    FilledTonalIconButton(onClick = onRemove, enabled = added > 0, modifier = Modifier.size(36.dp)) { Text("−") }
                    FilledTonalIconButton(onClick = onAdd, enabled = canAdd, modifier = Modifier.size(36.dp)) { Text("+") }
                }
            }
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
