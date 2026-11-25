package com.yukselis.protocol.ui.combat

import com.yukselis.protocol.ui.components.systemWindow
import androidx.compose.ui.unit.sp
import com.yukselis.protocol.ui.components.AppAlertDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import com.yukselis.protocol.ui.components.Glossary
import com.yukselis.protocol.ui.components.HelpButton
import com.yukselis.protocol.ui.components.TipCard
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
import com.yukselis.protocol.ui.components.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import com.yukselis.protocol.ui.components.OutlinedButton
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
import com.yukselis.protocol.ui.theme.Game
import com.yukselis.protocol.ui.components.HapticCue
import com.yukselis.protocol.ui.components.StatBar
import com.yukselis.protocol.ui.components.SystemCard
import com.yukselis.protocol.ui.components.playCue
import com.yukselis.protocol.ui.theme.EchoColor
import com.yukselis.protocol.ui.theme.GameType
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.text.style.TextOverflow

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
    showNumbers: Boolean,
    onAction: (CombatAction) -> Unit,
    onToggleAuto: () -> Unit,
    onCheckpoint: () -> Unit,
    modifier: Modifier = Modifier,
    haptics: Boolean = true,
) {
    var chosen by rememberSaveable { mutableStateOf(0) }
    var detail by rememberSaveable { mutableStateOf<Int?>(null) }
    var picker by rememberSaveable { mutableStateOf(Picker.NONE) }
    var tactics by rememberSaveable { mutableStateOf(false) }
    val living = combat.enemies.filter { it.alive }
    // Seçili hedef düştüyse ilk yaşayan düşman hedeflenir (bileşim sırasında durum yazılmaz).
    val target = if (living.any { it.uid == chosen }) chosen else living.firstOrNull()?.uid ?: chosen
    val targetEnemy = combat.enemies.firstOrNull { it.uid == target }
    val targetWeakness = targetEnemy?.weaknessKey
    val skillWeakMatch = targetWeakness != null && combat.skills.any { it.usable && targetWeakness in it.tags }
    val envWeakMatch = targetWeakness != null && combat.environment.any { !it.used && envMatchesWeakness(it.tag, targetWeakness) }

    // Boss faz geçişi anı: titreşim + kısa tek satırlık çağrı bandı (UI_REVIEW E4).
    val activeBoss = combat.enemies.firstOrNull { it.boss && it.phase > 0 }
    val bossPhaseKey = activeBoss?.let { "${it.uid}:${it.phase}" }
    var lastBossPhase by rememberSaveable { mutableStateOf(0) }
    var phaseBanner by androidx.compose.runtime.remember { mutableStateOf<Pair<Int, String>?>(null) }
    val view = androidx.compose.ui.platform.LocalView.current
    LaunchedEffect(bossPhaseKey) {
        val p = activeBoss?.phase ?: 0
        if (p > lastBossPhase && activeBoss != null) {
            lastBossPhase = p
            phaseBanner = p to activeBoss.name
            view.playCue(HapticCue.BIG_MOMENT, haptics)
        } else if (p < lastBossPhase) {
            lastBossPhase = p
            phaseBanner = null
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
        // Düşmanlar: dokun → hedef, uzun bas → ayrıntı (gerçek statlar)
        combat.enemies.forEach { e ->
            EnemyCard(e, selected = e.uid == target, onSelect = { if (e.alive) chosen = e.uid }, onDetail = { detail = e.uid })
            Spacer(Modifier.height(6.dp))
        }
        phaseBanner?.let { (ph, bossName) ->
            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp).systemWindow(accent = Game.colors.danger, cut = 6)) {
                Text(
                    text = stringResource(R.string.boss_phase_shift, ph, bossName),
                    style = GameType.systemTitle,
                    color = Game.colors.danger,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
        // Yoldaşlar: kendi turlarında otomatik davranır.
        combat.allies.forEach { a ->
            Text(
                text = if (a.down) stringResource(R.string.ally_down, a.name) else stringResource(R.string.ally_line, a.name, a.role, a.level, a.hp, a.maxHp),
                style = GameType.system,
                color = if (a.down) MaterialTheme.colorScheme.outline else Game.colors.companion,
            )
        }
        if (combat.allies.isNotEmpty()) Spacer(Modifier.height(6.dp))

        // Oyuncu: Dayanım, durumlar, duruş, efor. Kademeli açılış (Paket 2): Perde 1'in ilk savaşlarında
        // Taktik, Dayanım ve Ortam gizlidir; yalnız Saldır · Savun · Analiz · Kaç · Otomatik görünür.
        if (combat.tacticsUnlocked) {
            StatBar(stringResource(R.string.stamina_short), combat.stamina.toLong(), combat.maxStamina.toLong(), Game.colors.success)
        }
        if (combat.playerStatuses.isNotEmpty()) {
            Text(combat.playerStatuses.joinToString(" · "), style = GameType.system, color = Game.colors.danger)
        }
        // Duruş ve efor tek satırda: dokununca açıklamalı seçim penceresi açılır.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        ) {
            if (combat.tacticsUnlocked) {
                Box(modifier = Modifier.weight(1f, fill = false)) {
                    com.yukselis.protocol.ui.components.CappedFontScale {
                        AssistChip(
                            onClick = { tactics = true },
                            label = { Text(stringResource(R.string.tactics_line, stringResource(stanceLabel(combat.stance)), combat.effort), maxLines = 1) },
                            leadingIcon = { Text(stringResource(R.string.tactics), style = GameType.system, color = MaterialTheme.colorScheme.primary) },
                        )
                    }
                }
            } else {
                Spacer(Modifier.weight(1f))
            }
            HelpButton(stringResource(R.string.help_combat), Glossary.combat())
        }
        TipCard("combat", stringResource(R.string.tip_combat))

        // Günlük (düzyazı; sayılar isteğe bağlı)
        val listState = rememberLazyListState()
        LaunchedEffect(combat.log.size, notices.size) { listState.animateScrollToItem((combat.log.size + notices.size - 1).coerceAtLeast(0)) }
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            items(combat.log) { l ->
                Column {
                    Text(l.prose, style = GameType.narrationPast, color = MaterialTheme.colorScheme.onBackground)
                    val n = l.numbers
                    if (showNumbers && n != null) Text(n, style = GameType.system, color = Game.colors.amber)
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
                targetIntelligent = targetEnemy?.intelligent == true,
                skillHighlight = skillWeakMatch,
                envHighlight = envWeakMatch,
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

    if (tactics && combat.tacticsUnlocked) TacticsDialog(combat.stance, combat.effort, onAction) { tactics = false }

    val weakSuffix = "  " + stringResource(R.string.weakness_match)
    when (picker) {
        Picker.SKILL -> PickerDialog(
            title = stringResource(R.string.action_skill),
            options = combat.skills.map { s ->
                val cost = listOfNotNull(
                    if (s.costMp > 0) "${s.costMp} MP" else null,
                    if (s.costStamina > 0) "${s.costStamina} ${stringResource(R.string.stamina_short)}" else null,
                    if (s.cooldownLeft > 0) stringResource(R.string.cooldown_n, s.cooldownLeft) else null,
                ).joinToString(" · ")
                val match = if (targetWeakness != null && targetWeakness in s.tags) weakSuffix else ""
                Triple(s.id, "${s.name}  $cost$match", s.usable)
            },
            onPick = { id -> picker = Picker.NONE; onAction(CombatAction.UseSkill(id, target)) },
            onDismiss = { picker = Picker.NONE },
        )
        Picker.ENV -> PickerDialog(
            title = stringResource(R.string.action_environment),
            options = combat.environment.map { e ->
                val match = if (targetWeakness != null && !e.used && envMatchesWeakness(e.tag, targetWeakness)) weakSuffix else ""
                Triple(e.tag, e.name + match, !e.used)
            },
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

/** Duruş ve efor: her seçeneğin altında ne yaptığı yazar. Seçim anında uygulanır. */
@Composable
private fun TacticsDialog(stance: Stance, effort: Int, onAction: (CombatAction) -> Unit, onDismiss: () -> Unit) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
        title = { Text(stringResource(R.string.tactics_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.stance), style = GameType.systemTitle, color = MaterialTheme.colorScheme.primary)
                Stance.entries.forEach { st ->
                    val selected = st == stance
                    OutlinedButton(
                        onClick = { onAction(CombatAction.SetStance(st)) },
                        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text((if (selected) "✓ " else "") + stringResource(stanceLabel(st)), style = GameType.systemTitle)
                            Text(stringResource(stanceDesc(st)), style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Text(stringResource(R.string.effort), style = GameType.systemTitle, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
                Text(stringResource(R.string.effort_desc), style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (0..3).forEach { n ->
                        FilterChip(selected = effort == n, onClick = { onAction(CombatAction.SetEffort(n)) }, label = { Text("$n") })
                    }
                }
            }
        },
    )
}

private fun stanceDesc(s: Stance) = when (s) {
    Stance.ALL_OUT -> R.string.stance_all_out_desc
    Stance.MEASURED -> R.string.stance_measured_desc
    Stance.DEFENSIVE -> R.string.stance_defensive_desc
}

private fun stanceLabel(s: Stance) = when (s) {
    Stance.ALL_OUT -> R.string.stance_all_out
    Stance.MEASURED -> R.string.stance_measured
    Stance.DEFENSIVE -> R.string.stance_defensive
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EnemyCard(e: EnemyView, selected: Boolean, onSelect: () -> Unit, onDetail: () -> Unit) {
    val accent = when {
        !e.alive -> MaterialTheme.colorScheme.outline
        e.boss -> Game.colors.danger
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
    // Düşman da bir Sistem penceresidir: seçili olan buz mavisi, boss kızıl kenarlı.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .systemWindow(accent = accent, cut = 8)
            .combinedClickable(onClick = onSelect, onLongClick = onDetail),
    ) {
        com.yukselis.protocol.ui.components.CappedFontScale {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (e.boss) {
                        Text(
                            text = "[ ${stringResource(R.string.boss_badge)} ] ",
                            style = GameType.systemTitle.copy(fontSize = 11.sp),
                            color = Game.colors.danger,
                        )
                    }
                    Text(
                        e.name,
                        style = GameType.heading.copy(fontSize = 17.sp, lineHeight = 22.sp),
                        color = if (e.alive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.weight(1f),
                    )
                    Text(stringResource(R.string.enemy_level, e.level, e.gradeName), style = GameType.system)
                }
                if (e.boss && e.phaseThresholds.isNotEmpty()) {
                    BossHpBar(e.hp.toLong(), e.maxHp.toLong(), e.phaseThresholds, Modifier.padding(top = 2.dp))
                } else {
                    StatBar(stringResource(R.string.hp), e.hp.toLong(), e.maxHp.toLong(), Game.colors.danger, Modifier.padding(top = 2.dp))
                }
                val extra = listOfNotNull(
                    if (e.boss && e.phase > 0) stringResource(R.string.boss_phase, e.phase) else null,
                    e.statuses.joinToString(" · ").ifEmpty { null },
                    e.weakness?.let { stringResource(R.string.weakness_short) },
                )
                if (extra.isNotEmpty()) Text(extra.joinToString(" · "), style = GameType.system, color = Game.colors.amber)
            }
        }
    }
}

@Composable
private fun EnemyDetail(e: EnemyView, onDismiss: () -> Unit) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
        title = { Text("${e.name} · ${stringResource(R.string.enemy_level, e.level, e.gradeName)}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(e.description, style = GameType.narrationPast)
                // OP MC: gerçek statlar her zaman görünür.
                Text(stringResource(R.string.true_stats), style = GameType.system, color = Game.colors.echo)
                Text(e.stats.joinToString("  ") { "${it.abbr} ${it.value}" }, style = GameType.system)
                Text(e.weakness ?: stringResource(R.string.weakness_unknown), style = GameType.system, color = Game.colors.amber)
            }
        },
    )
}

@Composable
private fun BossHpBar(hp: Long, maxHp: Long, thresholds: List<Int>, modifier: Modifier = Modifier) {
    val fraction = if (maxHp <= 0) 0f else (hp.toFloat() / maxHp).coerceIn(0f, 1f)
    val barColor = Game.colors.danger
    val notchColor = Game.colors.night
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.hp),
            style = GameType.system,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.widthIn(min = 28.dp).padding(end = 4.dp),
        )
        LinearProgressIndicator(
            progress = { fraction },
            color = barColor,
            trackColor = barColor.copy(alpha = 0.18f),
            gapSize = 0.dp,
            drawStopIndicator = {},
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .drawWithContent {
                    drawContent()
                    // Faz eşikleri: can çubuğu üstünde dikey çentikler (UI_REVIEW E4).
                    val notchW = 2.dp.toPx()
                    thresholds.forEach { pct ->
                        if (pct in 1..99) {
                            val x = size.width * (pct / 100f)
                            drawRect(notchColor, topLeft = Offset(x - notchW / 2f, 0f), size = Size(notchW, size.height))
                        }
                    }
                },
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$hp/$maxHp",
            style = GameType.system,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}

private fun envMatchesWeakness(envTag: String, weakness: String): Boolean = when (envTag) {
    "yanici" -> weakness == "ates"
    "su" -> weakness == "simsek" || weakness == "buz"
    "karanlik" -> weakness == "karanlik"
    else -> false
}

@Composable
private fun ActionGrid(
    combat: CombatView,
    auto: Boolean,
    targetIntelligent: Boolean,
    skillHighlight: Boolean,
    envHighlight: Boolean,
    onAttack: () -> Unit,
    onDefend: () -> Unit,
    onAnalyze: () -> Unit,
    onFlee: () -> Unit,
    onPick: (Picker) -> Unit,
    onToggleAuto: () -> Unit,
) {
    val enabled = !auto
    if (combat.tacticsUnlocked) {
        // Tam düzen: 3 sütunlu ızgara; konuşulamayan düşmanda "Konuş" gizlenir (UI_REVIEW G5).
        val buttons = buildList<@Composable (Modifier) -> Unit> {
            add { m -> GridButton(R.string.action_attack, enabled, m, primary = true, onClick = onAttack) }
            add { m -> GridButton(R.string.action_skill, enabled && combat.skills.isNotEmpty(), m, accent = skillHighlight) { onPick(Picker.SKILL) } }
            add { m -> GridButton(R.string.action_defend, enabled, m, onClick = onDefend) }
            add { m -> GridButton(R.string.action_analyze, enabled, m, onClick = onAnalyze) }
            add { m -> GridButton(R.string.action_environment, enabled && combat.environment.any { !it.used }, m, accent = envHighlight) { onPick(Picker.ENV) } }
            if (targetIntelligent) add { m -> GridButton(R.string.action_talk, enabled, m) { onPick(Picker.TALK) } }
            add { m -> GridButton(R.string.action_item, enabled && combat.items.isNotEmpty() && !combat.minorUsed, m) { onPick(Picker.ITEM) } }
            add { m -> GridButton(R.string.action_flee, enabled, m, onClick = onFlee) }
            add { m -> GridButton(if (auto) R.string.auto_stop else R.string.auto_turn, true, m, onClick = onToggleAuto) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
            buttons.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { b -> b(Modifier.weight(1f)) }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        return
    }
    // Kademeli düzen (ilk savaşlar): Ortam yok; Yetenek, Eşya ve Konuş yalnız kullanılabiliyorsa görünür.
    // Çekirdek: Saldır · Savun · Analiz · Kaç · Otomatik. Düğmeler ikişer sıralanır.
    val buttons = buildList<@Composable (Modifier) -> Unit> {
        add { m -> GridButton(R.string.action_attack, enabled, m, primary = true, onClick = onAttack) }
        add { m -> GridButton(R.string.action_defend, enabled, m, onClick = onDefend) }
        if (combat.skills.isNotEmpty()) add { m -> GridButton(R.string.action_skill, enabled, m, accent = skillHighlight) { onPick(Picker.SKILL) } }
        add { m -> GridButton(R.string.action_analyze, enabled, m, onClick = onAnalyze) }
        if (targetIntelligent) add { m -> GridButton(R.string.action_talk, enabled, m) { onPick(Picker.TALK) } }
        if (combat.items.isNotEmpty() && !combat.minorUsed) add { m -> GridButton(R.string.action_item, enabled, m) { onPick(Picker.ITEM) } }
        add { m -> GridButton(R.string.action_flee, enabled, m, onClick = onFlee) }
        add { m -> GridButton(if (auto) R.string.auto_stop else R.string.auto_turn, true, m, onClick = onToggleAuto) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
        buttons.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { b -> b(Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun GridButton(label: Int, enabled: Boolean, modifier: Modifier, primary: Boolean = false, accent: Boolean = false, onClick: () -> Unit) {
    val padding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
    if (primary) {
        Button(onClick = onClick, enabled = enabled, contentPadding = padding, modifier = modifier) { GridLabel(label) }
    } else if (accent && enabled) {
        OutlinedButton(onClick = onClick, enabled = true, border = BorderStroke(1.5.dp, Game.colors.amber), contentPadding = padding, modifier = modifier) { GridLabel(label, Game.colors.amber) }
    } else {
        OutlinedButton(onClick = onClick, enabled = enabled, contentPadding = padding, modifier = modifier) { GridLabel(label) }
    }
}

/** Düğme etiketi: büyük yazıda kesilmesin diye ölçeği en çok 1.3 (UI_REVIEW C3). */
@Composable
private fun GridLabel(label: Int, color: Color = Color.Unspecified) {
    com.yukselis.protocol.ui.components.CappedFontScale { Text(stringResource(label), maxLines = 1, softWrap = false, color = color) }
}

@Composable
private fun ExecutionPanel(enemy: String, options: List<Pair<String, Pair<String, String>>>, onPick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
        Text(stringResource(R.string.execution_title, enemy), style = GameType.systemTitle, color = Game.colors.danger, fontWeight = FontWeight.Bold)
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
            Text(stringResource(R.string.you_died), style = GameType.systemTitle, color = Game.colors.danger)
            Button(onClick = onCheckpoint, modifier = Modifier.padding(top = 6.dp)) { Text(stringResource(R.string.return_checkpoint)) }
        }
    }
}

@Composable
private fun PickerDialog(title: String, options: List<Triple<String, String, Boolean>>, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    AppAlertDialog(
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
