package com.yukselis.protocol.ui.status

import com.yukselis.protocol.ui.components.systemWindow
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.sp
import com.yukselis.protocol.ui.components.AppAlertDialog
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
import com.yukselis.protocol.ui.components.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.yukselis.protocol.ui.components.OutlinedButton
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
import com.yukselis.protocol.ui.theme.Game
import com.yukselis.protocol.ui.components.SectionTitle
import com.yukselis.protocol.ui.components.StatBar
import com.yukselis.protocol.ui.components.SystemCard
import com.yukselis.protocol.ui.theme.GameType
import com.yukselis.protocol.ui.theme.rarityColorOf
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import com.yukselis.protocol.ui.components.Glossary
import com.yukselis.protocol.ui.components.HelpButton
import com.yukselis.protocol.ui.components.TipCard
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

    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(modifier = modifier.fillMaxSize()) {
        // Her sekmede görünen kısa özet.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 20.dp, end = 8.dp, top = 12.dp)) {
            // Seviye büyük bir sayı olarak durur; görünen seviye (dünyanın gördüğü) altında soluk.
            Text(
                text = "${status.level}",
                style = GameType.numeral,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(end = 14.dp)
                    .clearAndSetSemantics { },
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(status.playerName, style = GameType.heading)
                val levelText = if (status.showGrade) stringResource(R.string.level_grade, status.level, status.grade.name)
                    else stringResource(R.string.level_only, status.level)
                Text(
                    text = listOfNotNull(levelText, status.className).joinToString(" · "),
                    style = GameType.systemTitle.copy(fontSize = 12.sp, letterSpacing = 0.4.sp),
                    color = MaterialTheme.colorScheme.primary,
                )
                if (status.showRank) {
                    Text(
                        text = stringResource(R.string.humanity_percentile, formatPercent(status.humanityTopPercent)),
                        style = GameType.system,
                        color = Game.colors.amber,
                    )
                }
            }
            HelpButton(stringResource(R.string.help_status), Glossary.status())
        }
        // Alt sekme etiketleri büyük yazıda kesilmesin ("Unvanl" sorunu, UI_REVIEW): tavan 1.2.
        com.yukselis.protocol.ui.components.CappedFontScale(max = 1.2f) {
            TabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background, modifier = Modifier.padding(top = 8.dp)) {
                listOf(R.string.status_tab_stats, R.string.status_tab_titles, R.string.status_tab_morality).forEachIndexed { i, label ->
                    Tab(
                        selected = tab == i,
                        onClick = { tab = i },
                        text = {
                            // Harcanacak puan varsa Statlar sekmesinde nokta.
                            val dot = if (i == 0 && status.freePoints > 0) " •" else ""
                            Text(stringResource(label) + dot, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                        },
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            notices.forEach { SystemCard(it, Modifier.padding(bottom = 8.dp)) }
            TipCard("status", stringResource(R.string.tip_status), Modifier.padding(bottom = 8.dp))
            when (tab) {
                0 -> StatsTab(status, pending, spent) { confirm = true }
                1 -> TitlesTab(status)
                else -> MoralityTab(status)
            }
        }
    }

    if (confirm) {
        AppAlertDialog(
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

@Composable
private fun StatsTab(status: StatusView, pending: MutableMap<Stat, Int>, spent: Int, onConfirm: () -> Unit) {
    Column {
        val mode = if (status.mode == GameMode.IRONMAN) stringResource(R.string.mode_ironman) else stringResource(R.string.mode_normal)
        Text(
            text = "${stringResource(R.string.day_short, status.day)} · ${stringResource(R.string.act_short, status.act)} · $mode",
            style = GameType.system,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (status.evolutionPending) {
            Text(stringResource(R.string.evolution_pending), style = GameType.system, color = Game.colors.amber, modifier = Modifier.padding(top = 4.dp))
        }

        Spacer(Modifier.height(12.dp))
        StatBar(stringResource(R.string.hp), status.hp.toLong(), status.maxHp.toLong(), Game.colors.danger)
        if (status.showMana) {
            Spacer(Modifier.height(6.dp))
            StatBar(stringResource(R.string.mp), status.mp.toLong(), status.maxMp.toLong(), Game.colors.system)
        }
        Spacer(Modifier.height(6.dp))
        StatBar("XP", status.xp, status.xpToNext, Game.colors.amber)
        if (status.showStamina) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${stringResource(R.string.stamina)}: ${status.stamina}",
                style = GameType.system,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionTitle(stringResource(R.string.stats))
        val left = status.freePoints - spent
        if (status.freePoints > 0) {
            Text(
                text = stringResource(R.string.free_points, left),
                style = GameType.system,
                color = Game.colors.amber,
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
                Button(onClick = onConfirm, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.confirm)) }
            }
        }

        if (status.essenceSlots > 0 || status.essences.isNotEmpty()) {
            SectionTitle(stringResource(R.string.essence))
            Text(
                text = stringResource(R.string.essence_slots, status.essences.size, status.essenceSlots),
                style = GameType.system,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            status.essences.forEach { Text(it, style = GameType.systemTitle, color = Game.colors.amber) }
        }
    }
}

@Composable
private fun TitlesTab(status: StatusView) {
    Column {
        SectionTitle(stringResource(R.string.titles), Modifier.padding(top = 0.dp))
        if (status.titles.isEmpty()) {
            Text(stringResource(R.string.no_titles), style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        status.titles.forEach { title ->
            Column(modifier = Modifier.padding(bottom = 10.dp)) {
                Text(title.name, style = GameType.systemTitle, color = rarityColorOf(title.rarity))
                Text(title.description, style = GameType.system, color = MaterialTheme.colorScheme.onSurface)
            }
        }

        if (status.traits.isNotEmpty()) {
            SectionTitle(stringResource(R.string.traits))
            status.traits.forEach { t ->
                Column(modifier = Modifier.padding(bottom = 10.dp)) {
                    Text(t.name, style = GameType.systemTitle, color = rarityColorOf(t.rarity))
                    Text(t.description, style = GameType.system, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

    }
}

@Composable
private fun MoralityTab(status: StatusView) {
    Column {
        // OP MC: NPC'lerin gördüğü seviye ve Dikkat (Yozlaşma Dikkat'e katıldı, Paket 4). Bölüm kendini açar (Paket 2): ilk gizleme/Dikkat'e kadar yok.
        if (status.showHidden) {
            SectionTitle(stringResource(R.string.status_hidden_title), Modifier.padding(top = 0.dp))
            Text(stringResource(R.string.status_public, status.publicLevel), style = GameType.system, color = MaterialTheme.colorScheme.onSurface)
            StatBar(stringResource(R.string.status_attention), status.attention.toLong(), 100, Game.colors.danger, Modifier.padding(top = 6.dp), labelWidth = 96)
            if (status.showRank && status.publicRank > 0) {
                Text(stringResource(R.string.status_rank, status.publicRank), style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            }
            if (status.showArmy && status.armyPower > 0) {
                Text(stringResource(R.string.status_army, status.armyPower), style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        SectionTitle(stringResource(R.string.morality), if (status.showHidden) Modifier else Modifier.padding(top = 0.dp))
        MoralityBar(stringResource(R.string.cruelty), stringResource(R.string.mercy), status.mercy)
        MoralityBar(stringResource(R.string.freedom), stringResource(R.string.order), status.order)
        MoralityBar(stringResource(R.string.power), stringResource(R.string.humanity), status.humanity)
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
    Box(modifier = modifier.systemWindow(accent = Game.colors.line, cut = 8)) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(line.abbr, style = GameType.systemTitle, color = MaterialTheme.colorScheme.primary)
                    // Uzun adlar (Dayanıklılık) dar hücrede bölünmesin: orantılı yazı, tek satır.
                    Text(line.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    text = if (added > 0) "${line.value + added}" else "${line.value}",
                    style = GameType.heading.copy(fontSize = 24.sp, lineHeight = 28.sp),
                    color = if (added > 0) Game.colors.amber else MaterialTheme.colorScheme.onSurface,
                )
            }
            if (allocating) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                    FilledTonalIconButton(onClick = onRemove, enabled = added > 0, modifier = Modifier.size(48.dp)) { Text("−") }
                    FilledTonalIconButton(onClick = onAdd, enabled = canAdd, modifier = Modifier.size(48.dp)) { Text("+") }
                }
            }
        }
    }
}

/** −100 (sol uç) ile +100 (sağ uç) arasında ortadan iki yöne dolan eksen (UI_REVIEW D3). */
@Composable
private fun MoralityBar(negative: String, positive: String, value: Int) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = negative,
                style = if (value < 0) GameType.systemTitle else GameType.system,
                color = if (value < 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (value > 0) "+$value" else "$value",
                style = GameType.systemTitle,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(56.dp),
            )
            Text(
                text = positive,
                style = if (value > 0) GameType.systemTitle else GameType.system,
                color = if (value > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
        }
        val fillColor = MaterialTheme.colorScheme.secondary
        val trackColor = fillColor.copy(alpha = 0.18f)
        val tickColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
        val fraction = (value / 100f).coerceIn(-1f, 1f)
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
        ) {
            drawRect(color = trackColor)
            val mid = size.width / 2f
            if (fraction > 0f) {
                drawRect(
                    color = fillColor,
                    topLeft = androidx.compose.ui.geometry.Offset(mid, 0f),
                    size = androidx.compose.ui.geometry.Size(mid * fraction, size.height),
                )
            } else if (fraction < 0f) {
                val w = mid * (-fraction)
                drawRect(
                    color = fillColor,
                    topLeft = androidx.compose.ui.geometry.Offset(mid - w, 0f),
                    size = androidx.compose.ui.geometry.Size(w, size.height),
                )
            }
            val tickW = 1.5.dp.toPx()
            drawRect(
                color = tickColor,
                topLeft = androidx.compose.ui.geometry.Offset(mid - tickW / 2f, 0f),
                size = androidx.compose.ui.geometry.Size(tickW, size.height),
            )
        }
    }
}
