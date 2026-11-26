package com.yukselis.protocol.ui.story

import com.yukselis.protocol.ui.components.AppAlertDialog
import com.yukselis.protocol.ui.components.systemWindow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.verticalScroll
import com.yukselis.protocol.ui.components.Button
import androidx.compose.material3.MaterialTheme
import com.yukselis.protocol.ui.components.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yukselis.engine.story.HubView
import com.yukselis.engine.story.OperationChoiceView
import com.yukselis.engine.story.SkipFocusView
import com.yukselis.engine.story.SkipView
import com.yukselis.engine.world.SkipFocus
import com.yukselis.engine.world.HubAction
import com.yukselis.protocol.R
import com.yukselis.protocol.ui.theme.Game
import com.yukselis.protocol.ui.components.Glossary
import com.yukselis.protocol.ui.components.HelpButton
import com.yukselis.protocol.ui.components.TipCard
import com.yukselis.protocol.ui.theme.GameType

internal enum class HubDialog { GATES, TRAVEL, PREPARE, SKIP }

/** Gün akışının pencere durumu: hikaye bölümü (kartlar) ve eylem çubuğu aynı pencereleri açar. */
class HubUi {
    internal var dialog by mutableStateOf<HubDialog?>(null)

    /** Operasyon yaklaşırken üsten ayrılan eylem önce onay ister (DECISIONS #141). */
    internal var pending by mutableStateOf<HubAction?>(null)

    /** Operasyon düğmesi önce onay penceresi açar: tehdit, seviye, hazırlıklar, yoldaş görüşleri, uyarı (Paket 2). */
    internal var confirmOp by mutableStateOf<OperationChoiceView?>(null)
}

@Composable
fun rememberHubUi(): HubUi = remember { HubUi() }

private fun leave(hub: HubView, ui: HubUi, onAction: (HubAction) -> Unit, a: HubAction) {
    if (hub.deadlineWarning != null) ui.pending = a else onAction(a)
}

/**
 * Gün akışının hikaye bölümü (Paket 5, UI_REVIEW A4): sahne metninin altında, aynı kaydırmada. Kalan eylem ve tehlike,
 * operasyon, hazırlıklar ve fırsat kartları. Rutin eylemler burada değil, alttaki [HubActionBar]'da.
 */
@Composable
fun HubStory(hub: HubView, ui: HubUi, onAction: (HubAction) -> Unit, showDanger: Boolean = true) {
    val largeFont = androidx.compose.ui.platform.LocalDensity.current.fontScale >= 1.4f
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.hub_actions, hub.slotsLeft, hub.slotsTotal),
                style = GameType.systemTitle,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (showDanger && !largeFont) {
                Text(stringResource(R.string.hub_danger, hub.danger), style = GameType.system, color = dangerColor(hub.danger))
            }
            HelpButton(stringResource(R.string.help_hub), Glossary.hub())
        }
        // Büyük yazıda (ölçek ≥ 1.4) Tehlike ayrı satıra iner: "Kalan eylem" sıkışıp 3 satır olmasın (UI_REVIEW C5).
        if (showDanger && largeFont) {
            Text(stringResource(R.string.hub_danger, hub.danger), style = GameType.system, color = dangerColor(hub.danger))
        }
        // Bölüm yapısı: görevdeysen nerede olduğun, üsteysen girilebilecek operasyon (DECISIONS #130).
        hub.mission?.let { Text(it, style = GameType.system, color = MaterialTheme.colorScheme.tertiary) }
        TipCard("hub", stringResource(R.string.tip_hub))
        if (hub.mustCamp) return@Column
        // Vurgulu operasyon (süre daralıyor, seviye yeterli) üstte; vurgusuz olan Fırsatlar'ın altına iner (Paket 2).
        hub.operations.filter { it.emphasized }.forEach { op -> OperationButton(op) { ui.confirmOp = op } }
        if (hub.preparations.isNotEmpty()) {
            OutlinedButton(onClick = { ui.dialog = HubDialog.PREPARE }, modifier = Modifier.fillMaxWidth()) {
                Text(pluralStringResource(R.plurals.hub_prepare_n, hub.preparations.size, hub.preparations.size), color = MaterialTheme.colorScheme.tertiary)
            }
        }
        if (hub.cards.isNotEmpty()) {
            Text(stringResource(R.string.hub_hand), style = GameType.system, color = MaterialTheme.colorScheme.primary)
            hub.cards.forEach { card ->
                OutlinedButton(
                    onClick = { if (card.region != null) leave(hub, ui, onAction, HubAction.Card(card.id)) else onAction(HubAction.Card(card.id)) },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(card.title, style = MaterialTheme.typography.bodyLarge)
                            // Başka bölgede geçen olay: oynamak oraya bir görev başlatır.
                            card.region?.let {
                                Text(stringResource(R.string.hub_card_away, it), style = GameType.system, color = MaterialTheme.colorScheme.tertiary)
                            }
                        }
                        // Gerilim: ● sayısı
                        Text("●".repeat(card.tension), style = GameType.system, color = tensionColor(card.tension))
                    }
                }
            }
        }
        hub.operations.filter { !it.emphasized }.forEach { op -> OperationButton(op) { ui.confirmOp = op } }
    }
}

/**
 * Rutin eylem çubuğu (UI_REVIEW A4, A5): Keşfet, Antrenman, Dinlen, Kule, Kapılar, Yolculuk, Üsse dön, Zaman atla tek
 * biçimde, tek satırda; sığmazsa yana kayar. Gün bittiyse yalnız Kamp (ve görevdeyse Üsse dön).
 */
@Composable
fun HubActionBar(hub: HubView, ui: HubUi, onAction: (HubAction) -> Unit) {
    if (hub.mustCamp) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { onAction(HubAction.Camp) }, modifier = Modifier.weight(1f)) { OneLine(stringResource(R.string.hub_camp)) }
            // Görevdeyken gün bitti: sahada kamp ya da üsse dönüş (dönüş yuva harcamaz).
            if (hub.canReturn) {
                OutlinedButton(onClick = { onAction(HubAction.ReturnToBase) }, modifier = Modifier.weight(1f)) { OneLine("← " + stringResource(R.string.hub_return)) }
            }
        }
        return
    }
    val tower = hub.tower
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    ) {
        if (hub.canExplore) Chip(stringResource(R.string.hub_explore)) { onAction(HubAction.Explore) }
        Chip(stringResource(R.string.hub_train)) { onAction(HubAction.Train) }
        Chip(stringResource(R.string.hub_rest)) { onAction(HubAction.Rest) }
        tower?.let { t ->
            Chip(stringResource(if (t.story) R.string.hub_tower_short_story else R.string.hub_tower_short, t.floor)) { onAction(HubAction.ClimbTower) }
            // Hızlı tırmanış (DECISIONS #147): kademenin altındaki katlar tek yuvada; katları tek tek oynamak hep mümkün.
            t.fastTo?.let { to -> Chip(stringResource(R.string.hub_tower_fast, t.floor, to)) { onAction(HubAction.FastClimb) } }
        }
        if (hub.gates.isNotEmpty()) Chip(stringResource(R.string.hub_gates_n, hub.gates.size)) { ui.dialog = HubDialog.GATES }
        if (hub.travel.isNotEmpty()) Chip(stringResource(R.string.hub_travel_btn) + " →") { ui.dialog = HubDialog.TRAVEL }
        if (hub.canReturn) Chip("← " + stringResource(R.string.hub_return)) { onAction(HubAction.ReturnToBase) }
        // Antrenman / zaman atlama (DECISIONS #142): boş günleri bir odakla geçirmek.
        if (hub.skip != null) Chip(stringResource(R.string.hub_skip) + " ⏩") { ui.dialog = HubDialog.SKIP }
    }
}

/** Eylem çubuğu düğmesi: tek biçim (kesik köşe çerçeve), en az 48dp yüksek, tek satır. */
@Composable
private fun Chip(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        modifier = Modifier.heightIn(min = 48.dp),
    ) { Text(label, maxLines = 1, softWrap = false) }
}

/** Gün akışının pencereleri: kapılar, yolculuk, hazırlık, zaman atlama, son tarih uyarısı, operasyon onayı. */
@Composable
fun HubDialogs(hub: HubView, ui: HubUi, onAction: (HubAction) -> Unit) {
    when (ui.dialog) {
        HubDialog.GATES -> ListDialog(
            title = stringResource(R.string.hub_gates_title),
            onDismiss = { ui.dialog = null },
        ) {
            hub.gates.forEach { g ->
                val imminent = g.daysLeft <= 0
                OutlinedButton(onClick = { ui.dialog = null; onAction(HubAction.EnterGate(g.uid)) }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (imminent) stringResource(R.string.hub_gate_imminent, g.grade, g.enemies)
                        else pluralStringResource(R.plurals.hub_gate, g.daysLeft, g.grade, g.enemies, g.daysLeft),
                        style = GameType.system,
                        color = if (imminent) Game.colors.danger else GateColor,
                    )
                }
            }
        }
        HubDialog.TRAVEL -> ListDialog(
            title = stringResource(R.string.hub_travel_title),
            onDismiss = { ui.dialog = null },
        ) {
            hub.travel.forEach { r ->
                OutlinedButton(onClick = { ui.dialog = null; leave(hub, ui, onAction, HubAction.Travel(r.id)) }, modifier = Modifier.fillMaxWidth()) { Text("→ ${r.name}") }
            }
        }
        HubDialog.PREPARE -> ListDialog(
            title = stringResource(R.string.hub_prepare_title),
            onDismiss = { ui.dialog = null },
        ) {
            hub.preparations.forEach { p ->
                OutlinedButton(
                    onClick = { ui.dialog = null; onAction(HubAction.Prepare(p.id)) },
                    enabled = p.enabled,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(p.name + " · " + pluralStringResource(R.plurals.hub_prepare_cost, p.cost, p.cost), style = MaterialTheme.typography.bodyLarge)
                        Text(p.description, style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        HubDialog.SKIP -> hub.skip?.let { sk ->
            SkipDialog(sk, onDismiss = { ui.dialog = null }) { focus, days -> ui.dialog = null; onAction(HubAction.Skip(focus, days)) }
        }
        null -> Unit
    }
    ui.pending?.let { action ->
        AppAlertDialog(
            onDismissRequest = { ui.pending = null },
            confirmButton = { TextButton(onClick = { ui.pending = null; onAction(action) }) { Text(stringResource(R.string.hub_leave_anyway)) } },
            dismissButton = { TextButton(onClick = { ui.pending = null }) { Text(stringResource(R.string.cancel)) } },
            title = { Text(stringResource(R.string.hub_deadline_title)) },
            text = { Text(hub.deadlineWarning ?: "") },
        )
    }
    ui.confirmOp?.let { op ->
        OperationConfirmDialog(
            op = op,
            onConfirm = { ui.confirmOp = null; onAction(HubAction.EnterOperation(op.index)) },
            onDismiss = { ui.confirmOp = null },
        )
    }
}

/** Operasyon düğmesi: vurgulu (dolu) ya da vurgusuz (çerçeveli); iki satır: ad, bölge · kalan gün. */
@Composable
private fun OperationButton(op: OperationChoiceView, onClick: () -> Unit) {
    val content: @Composable () -> Unit = {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.hub_operation, op.title), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                pluralStringResource(R.plurals.hub_operation_days, op.daysLeft, op.region, op.daysLeft),
                style = GameType.system,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    if (op.emphasized) {
        Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { content() }
    } else {
        OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) { content() }
    }
}

/**
 * Operasyon onayı: tehdit ve oyuncu seviyesi (fark ≥ 3 ise tehdit kızıl), hazırlıkların durumu, yoldaş görüşleri,
 * büyük farkta Sistem uyarısı. Onay = operasyona giriş; Vazgeç = üste dönüş.
 */
@Composable
private fun OperationConfirmDialog(op: OperationChoiceView, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val gap = op.threatLevel - op.playerLevel
    AppAlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.op_confirm_enter)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        title = { Text(op.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    stringResource(R.string.op_confirm_threat, op.threatLevel),
                    style = GameType.systemTitle,
                    color = if (gap >= 3) Game.colors.danger else MaterialTheme.colorScheme.onSurface,
                )
                Text(stringResource(R.string.op_confirm_you, op.playerLevel), style = GameType.systemTitle, color = MaterialTheme.colorScheme.onSurface)
                if (op.preparations.isNotEmpty()) {
                    Text(
                        stringResource(R.string.op_confirm_preparations),
                        style = GameType.system,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    op.preparations.forEach { p ->
                        Text(
                            (if (p.done) "✓ " else "○ ") + p.name,
                            style = GameType.system,
                            color = if (p.done) Game.colors.success else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (op.opinions.isNotEmpty()) {
                    Text(
                        stringResource(R.string.op_confirm_opinions),
                        style = GameType.system,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    op.opinions.forEach { Text(it, style = GameType.system, color = Game.colors.companion) }
                }
                op.warning?.let { w ->
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 6.dp).systemWindow(accent = Game.colors.danger, cut = 8)) {
                        Text(w, style = GameType.system, color = Game.colors.danger, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                    }
                }
            }
        },
    )
}

/** Zaman atlama: odak (gruplu liste) ve gün sayısı (1…en çok). */
@Composable
private fun SkipDialog(skip: SkipView, onDismiss: () -> Unit, onStart: (SkipFocus, Int) -> Unit) {
    var chosen by remember { mutableStateOf<SkipFocusView?>(null) }
    var days by remember { mutableIntStateOf(minOf(3, skip.maxDays)) }
    AppAlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { chosen?.let { onStart(it.focus, days) } }, enabled = chosen != null) {
                Text(pluralStringResource(R.plurals.hub_skip_start, days, days))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        title = { Text(stringResource(R.string.hub_skip_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.hub_skip_info), style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { days-- }, enabled = days > 1) { Text("−") }
                    Text(
                        pluralStringResource(R.plurals.hub_skip_days, days, days),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(onClick = { days++ }, enabled = days < skip.maxDays) { Text("+") }
                }
                skip.foci.groupBy { it.group }.forEach { (group, items) ->
                    Text(stringResource(skipGroupLabel(group)), style = GameType.system, color = MaterialTheme.colorScheme.primary)
                    items.forEach { f ->
                        val selected = chosen == f
                        if (selected) {
                            Button(onClick = { chosen = f }, modifier = Modifier.fillMaxWidth()) { Text("✓ " + f.name) }
                        } else {
                            OutlinedButton(onClick = { chosen = f }, modifier = Modifier.fillMaxWidth()) { Text(f.name) }
                        }
                    }
                }
            }
        },
    )
}

private fun skipGroupLabel(group: String): Int = when (group) {
    "antrenman" -> R.string.hub_skip_group_train
    "kapi" -> R.string.hub_skip_group_gates
    "meslek" -> R.string.hub_skip_group_craft
    else -> R.string.hub_skip_group_companion
}

@Composable
private fun OneLine(text: String, color: Color = Color.Unspecified) {
    Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis, color = color)
}

@Composable
private fun ListDialog(title: String, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.verticalScroll(rememberScrollState()).padding(top = 2.dp)) {
                content()
            }
        },
    )
}

private val TowerColor: Color @Composable get() = Game.colors.violet
private val GateColor: Color @Composable get() = Game.colors.amber

/** Dört basamaklı ölçek: sakin → tehlikeli. */
@Composable
private fun scaleColor(step: Int): Color = when (step) {
    1 -> Game.colors.success
    2 -> Game.colors.system
    3 -> Game.colors.amber
    else -> Game.colors.danger
}

@Composable
private fun tensionColor(t: Int) = scaleColor(t)

@Composable
private fun dangerColor(d: Int) = scaleColor(d / 25 + 1)
