package com.yukselis.protocol.ui.combat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yukselis.engine.combat.CombatAction
import com.yukselis.engine.combat.Stance
import com.yukselis.engine.story.CombatView
import com.yukselis.engine.story.EnemyView
import com.yukselis.engine.story.NoticeView
import com.yukselis.protocol.R
import com.yukselis.protocol.ui.components.StatBar
import com.yukselis.protocol.ui.components.SystemCard
import com.yukselis.protocol.ui.theme.EchoColor
import com.yukselis.protocol.ui.theme.GameType

private enum class Picker { NONE, SKILL, ENV, ITEM, TALK }

/**
 * Savaş ekranı (GDD: eylem ızgarası, hedef seçimi, uzun basınca ayrıntı, savaş günlüğü, otomatik tur).
 * OP MC: duruş + efor her turda; anlatım düzyazı, sayılar isteğe bağlı; son düşmanda infaz seçimi.
 */
@Composable
fun CombatScreen(
    combat: CombatView,
    notices: List<NoticeView>,
    auto: Boolean,
    onAction: (CombatAction) -> Unit,
    onToggleAuto: () -> Unit,
    onCheckpoint: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var target by rememberSaveable { mutableStateOf(0) }
    var detail by rememberSaveable { mutableStateOf<Int?>(null) }
    var picker by rememberSaveable { mutableStateOf(Picker.NONE) }
    var showNumbers by rememberSaveable { mutableStateOf(false) }
    val living = combat.enemies.filter { it.alive }
    if (living.isNotEmpty() && living.none { it.uid == target }) target = living.first().uid

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
        // Düşmanlar: dokun → hedef, uzun bas → ayrıntı (gerçek statlar)
        combat.enemies.forEach { e ->
            EnemyCard(e, selected = e.uid == target, onSelect = { if (e.alive) target = e.uid }, onDetail = { detail = e.uid })
            Spacer(Modifier.height(6.dp))
        }

        // Oyuncu: Dayanım, durumlar, duruş, efor
        StatBar(stringResource(R.string.stamina_short), combat.stamina.toLong(), combat.maxStamina.toLong(), Color(0xFF66BB6A))
        if (combat.playerStatuses.isNotEmpty()) {
            Text(combat.playerStatuses.joinToString(" · "), style = GameType.system, color = Color(0xFFFF7043))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Stance.entries.forEach { st ->
                FilterChip(
                    selected = combat.stance == st,
                    onClick = { onAction(CombatAction.SetStance(st)) },
                    label = { Text(stringResource(stanceLabel(st)), maxLines = 1) },
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.effort), style = GameType.system)
            (0..3).forEach { n ->
                FilterChip(selected = combat.effort == n, onClick = { onAction(CombatAction.SetEffort(n)) }, label = { Text("$n") })
            }
            Spacer(Modifier.weight(1f))
            FilterChip(selected = showNumbers, onClick = { showNumbers = !showNumbers }, label = { Text(stringResource(R.string.numbers)) })
        }

        // Günlük (düzyazı; sayılar isteğe bağlı)
        val listState = rememberLazyListState()
        LaunchedEffect(combat.log.size, notices.size) { listState.animateScrollToItem((combat.log.size + notices.size - 1).coerceAtLeast(0)) }
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            items(combat.log) { l ->
                Column {
                    Text(l.prose, style = GameType.narrationPast, color = MaterialTheme.colorScheme.onBackground)
                    val n = l.numbers
                    if (showNumbers && n != null) Text(n, style = GameType.system, color = Color(0xFFFFD54F))
                }
            }
            items(notices) { SystemCard(it) }
        }

        when {
            combat.dead -> DeathPanel(onCheckpoint)
            combat.execution != null -> ExecutionPanel(combat.execution!!.enemyName, combat.execution!!.options.map { it.id to (it.name to it.description) }) {
                onAction(CombatAction.Execute(it))
            }
            else -> ActionGrid(
                combat = combat,
                auto = auto,
                targetIntelligent = combat.enemies.firstOrNull { it.uid == target }?.intelligent == true,
                onAttack = { onAction(CombatAction.Attack(target)) },
                onDefend = { onAction(CombatAction.Defend) },
                onAnalyze = { onAction(CombatAction.Analyze(target)) },
                onFlee = { onAction(CombatAction.Flee) },
                onPick = { picker = it },
                onToggleAuto = onToggleAuto,
            )
        }
    }

    detail?.let { uid -> combat.enemies.firstOrNull { it.uid == uid }?.let { EnemyDetail(it) { detail = null } } }

    when (picker) {
        Picker.SKILL -> PickerDialog(
            title = stringResource(R.string.action_skill),
            options = combat.skills.map { s ->
                val cost = listOfNotNull(
                    if (s.costMp > 0) "${s.costMp} MP" else null,
                    if (s.costStamina > 0) "${s.costStamina} ${stringResource(R.string.stamina_short)}" else null,
                    if (s.cooldownLeft > 0) stringResource(R.string.cooldown_n, s.cooldownLeft) else null,
                ).joinToString(" · ")
                Triple(s.id, "${s.name}  $cost", s.usable)
            },
            onPick = { id -> picker = Picker.NONE; onAction(CombatAction.UseSkill(id, target)) },
            onDismiss = { picker = Picker.NONE },
        )
        Picker.ENV -> PickerDialog(
            title = stringResource(R.string.action_environment),
            options = combat.environment.map { Triple(it.tag, it.name, !it.used) },
            onPick = { tag -> picker = Picker.NONE; onAction(CombatAction.Environment(tag)) },
            onDismiss = { picker = Picker.NONE },
        )
        Picker.ITEM -> PickerDialog(
            title = stringResource(R.string.action_item),
            options = combat.items.map { Triple(it.id, "${it.name} ×${it.count}", !combat.minorUsed) },
            onPick = { id -> picker = Picker.NONE; onAction(CombatAction.UseItem(id, target)) },
            onDismiss = { picker = Picker.NONE },
        )
        Picker.TALK -> PickerDialog(
            title = stringResource(R.string.action_talk),
            options = listOf(
                Triple("ikna", stringResource(R.string.talk_persuade), true),
                Triple("tehdit", stringResource(R.string.talk_threaten), true),
            ),
            onPick = { id -> picker = Picker.NONE; onAction(CombatAction.Talk(target, threaten = id == "tehdit")) },
            onDismiss = { picker = Picker.NONE },
        )
        Picker.NONE -> Unit
    }
}

private fun stanceLabel(s: Stance) = when (s) {
    Stance.ALL_OUT -> R.string.stance_all_out
    Stance.MEASURED -> R.string.stance_measured
    Stance.DEFENSIVE -> R.string.stance_defensive
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EnemyCard(e: EnemyView, selected: Boolean, onSelect: () -> Unit, onDetail: () -> Unit) {
    val shape = RoundedCornerShape(6.dp)
    val accent = when {
        !e.alive -> MaterialTheme.colorScheme.outline
        e.boss -> Color(0xFFEF5350)
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = shape,
        modifier = Modifier
            .fillMaxWidth()
            .border(if (selected && e.alive) 2.dp else 1.dp, accent, shape)
            .combinedClickable(onClick = onSelect, onLongClick = onDetail),
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(e.name, style = GameType.systemTitle, color = accent, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.enemy_level, e.level, e.gradeName), style = GameType.system)
            }
            StatBar(stringResource(R.string.hp), e.hp.toLong(), e.maxHp.toLong(), Color(0xFFEF5350), Modifier.padding(top = 2.dp))
            val extra = listOfNotNull(
                if (e.boss && e.phase > 0) stringResource(R.string.boss_phase, e.phase) else null,
                e.statuses.joinToString(" · ").ifEmpty { null },
                e.weakness?.let { stringResource(R.string.weakness_short) },
            )
            if (extra.isNotEmpty()) Text(extra.joinToString(" · "), style = GameType.system, color = Color(0xFFFFA726))
        }
    }
}

@Composable
private fun EnemyDetail(e: EnemyView, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
        title = { Text("${e.name} · ${stringResource(R.string.enemy_level, e.level, e.gradeName)}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(e.description, style = GameType.narrationPast)
                // OP MC: gerçek statlar her zaman görünür.
                Text(stringResource(R.string.true_stats), style = GameType.system, color = EchoColor)
                Text(e.stats.joinToString("  ") { "${it.abbr} ${it.value}" }, style = GameType.system)
                Text(e.weakness ?: stringResource(R.string.weakness_unknown), style = GameType.system, color = Color(0xFFFFA726))
            }
        },
    )
}

@Composable
private fun ActionGrid(
    combat: CombatView,
    auto: Boolean,
    targetIntelligent: Boolean,
    onAttack: () -> Unit,
    onDefend: () -> Unit,
    onAnalyze: () -> Unit,
    onFlee: () -> Unit,
    onPick: (Picker) -> Unit,
    onToggleAuto: () -> Unit,
) {
    val enabled = !auto
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GridButton(R.string.action_attack, enabled, Modifier.weight(1f), primary = true, onClick = onAttack)
            GridButton(R.string.action_skill, enabled && combat.skills.isNotEmpty(), Modifier.weight(1f)) { onPick(Picker.SKILL) }
            GridButton(R.string.action_defend, enabled, Modifier.weight(1f), onClick = onDefend)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GridButton(R.string.action_analyze, enabled, Modifier.weight(1f), onClick = onAnalyze)
            GridButton(R.string.action_environment, enabled && combat.environment.any { !it.used }, Modifier.weight(1f)) { onPick(Picker.ENV) }
            GridButton(R.string.action_talk, enabled && targetIntelligent, Modifier.weight(1f)) { onPick(Picker.TALK) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GridButton(R.string.action_item, enabled && combat.items.isNotEmpty() && !combat.minorUsed, Modifier.weight(1f)) { onPick(Picker.ITEM) }
            GridButton(R.string.action_flee, enabled, Modifier.weight(1f), onClick = onFlee)
            GridButton(if (auto) R.string.auto_stop else R.string.auto_turn, true, Modifier.weight(1f), onClick = onToggleAuto)
        }
    }
}

@Composable
private fun GridButton(label: Int, enabled: Boolean, modifier: Modifier, primary: Boolean = false, onClick: () -> Unit) {
    val padding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
    if (primary) {
        Button(onClick = onClick, enabled = enabled, contentPadding = padding, modifier = modifier) { Text(stringResource(label), maxLines = 1) }
    } else {
        OutlinedButton(onClick = onClick, enabled = enabled, contentPadding = padding, modifier = modifier) { Text(stringResource(label), maxLines = 1) }
    }
}

@Composable
private fun ExecutionPanel(enemy: String, options: List<Pair<String, Pair<String, String>>>, onPick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
        Text(stringResource(R.string.execution_title, enemy), style = GameType.systemTitle, color = Color(0xFFEF5350), fontWeight = FontWeight.Bold)
        options.forEach { (id, label) ->
            OutlinedButton(onClick = { onPick(id) }, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(label.first, style = GameType.systemTitle)
                    Text(label.second, style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun DeathPanel(onCheckpoint: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.you_died), style = GameType.systemTitle, color = Color(0xFFEF5350))
            Button(onClick = onCheckpoint, modifier = Modifier.padding(top = 6.dp)) { Text(stringResource(R.string.return_checkpoint)) }
        }
    }
}

@Composable
private fun PickerDialog(title: String, options: List<Triple<String, String, Boolean>>, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                options.forEach { (id, label, enabled) ->
                    OutlinedButton(onClick = { onPick(id) }, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text(label) }
                }
            }
        },
    )
}
