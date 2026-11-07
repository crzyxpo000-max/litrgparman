package com.yukselis.protocol.ui.story

import com.yukselis.protocol.ui.components.AppAlertDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yukselis.engine.story.HubView
import com.yukselis.engine.world.HubAction
import com.yukselis.protocol.R
import com.yukselis.protocol.ui.components.Glossary
import com.yukselis.protocol.ui.components.HelpButton
import com.yukselis.protocol.ui.components.TipCard
import com.yukselis.protocol.ui.theme.GameType

private enum class HubDialog { GATES, TRAVEL, PREPARE }

/**
 * Gün akışı, sabit alt panelde: üstte kalan eylem ve tehlike, sonra fırsat kartları, tek satır serbest eylem,
 * tek satır Kule / Kapılar / Yolculuk. Kapı ve yolculuk listeleri ayrı pencerede açılır; panel kısa kalır.
 */
@Composable
fun HubPanel(hub: HubView, onAction: (HubAction) -> Unit) {
    var dialog by remember { mutableStateOf<HubDialog?>(null) }
    // Operasyon yaklaşırken üsten ayrılan eylem önce onay ister (DECISIONS #141).
    var pending by remember { mutableStateOf<HubAction?>(null) }
    val leave: (HubAction) -> Unit = { a -> if (hub.deadlineWarning != null) pending = a else onAction(a) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.hub_actions, hub.slotsLeft, hub.slotsTotal),
                style = GameType.systemTitle,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(stringResource(R.string.hub_danger, hub.danger), style = GameType.system, color = dangerColor(hub.danger))
            HelpButton(stringResource(R.string.help_hub), Glossary.hub())
        }
        // Bölüm yapısı: görevdeysen nerede olduğun, üsteysen girilebilecek operasyon (DECISIONS #130).
        hub.mission?.let { Text(it, style = GameType.system, color = MaterialTheme.colorScheme.tertiary) }
        TipCard("hub", stringResource(R.string.tip_hub))
        if (hub.mustCamp) {
            Button(onClick = { onAction(HubAction.Camp) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.hub_camp)) }
            // Görevdeyken gün bitti: sahada kamp ya da üsse dönüş (dönüş yuva harcamaz).
            if (hub.canReturn) {
                OutlinedButton(onClick = { onAction(HubAction.ReturnToBase) }, modifier = Modifier.fillMaxWidth()) { Text("← " + stringResource(R.string.hub_return)) }
            }
            return@Column
        }
        hub.operations.forEach { op ->
            Button(onClick = { onAction(HubAction.EnterOperation(op.index)) }, modifier = Modifier.fillMaxWidth()) {
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
        }
        if (hub.preparations.isNotEmpty()) {
            OutlinedButton(onClick = { dialog = HubDialog.PREPARE }, modifier = Modifier.fillMaxWidth()) {
                Text(pluralStringResource(R.plurals.hub_prepare_n, hub.preparations.size, hub.preparations.size), color = MaterialTheme.colorScheme.tertiary)
            }
        }
        if (hub.cards.isNotEmpty()) {
            Text(stringResource(R.string.hub_hand), style = GameType.system, color = MaterialTheme.colorScheme.primary)
            hub.cards.forEach { card ->
                OutlinedButton(
                    onClick = { if (card.region != null) leave(HubAction.Card(card.id)) else onAction(HubAction.Card(card.id)) },
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
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val pad = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
            if (hub.canExplore) {
                FilledTonalButton(onClick = { onAction(HubAction.Explore) }, contentPadding = pad, modifier = Modifier.weight(1f)) {
                    OneLine(stringResource(R.string.hub_explore))
                }
            }
            FilledTonalButton(onClick = { onAction(HubAction.Train) }, contentPadding = pad, modifier = Modifier.weight(1f)) { OneLine(stringResource(R.string.hub_train)) }
            FilledTonalButton(onClick = { onAction(HubAction.Rest) }, contentPadding = pad, modifier = Modifier.weight(1f)) { OneLine(stringResource(R.string.hub_rest)) }
        }
        val tower = hub.tower
        if (tower != null || hub.gates.isNotEmpty() || hub.travel.isNotEmpty() || hub.canReturn) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val pad = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                tower?.let { t ->
                    OutlinedButton(onClick = { onAction(HubAction.ClimbTower) }, contentPadding = pad, modifier = Modifier.weight(1f)) {
                        OneLine(stringResource(if (t.story) R.string.hub_tower_short_story else R.string.hub_tower_short, t.floor), TowerColor)
                    }
                }
                if (hub.gates.isNotEmpty()) {
                    OutlinedButton(onClick = { dialog = HubDialog.GATES }, contentPadding = pad, modifier = Modifier.weight(1f)) {
                        OneLine(stringResource(R.string.hub_gates_n, hub.gates.size), GateColor)
                    }
                }
                if (hub.travel.isNotEmpty()) {
                    OutlinedButton(onClick = { dialog = HubDialog.TRAVEL }, contentPadding = pad, modifier = Modifier.weight(1f)) {
                        OneLine(stringResource(R.string.hub_travel_btn) + " →")
                    }
                }
                if (hub.canReturn) {
                    OutlinedButton(onClick = { onAction(HubAction.ReturnToBase) }, contentPadding = pad, modifier = Modifier.weight(1f)) {
                        OneLine("← " + stringResource(R.string.hub_return))
                    }
                }
            }
        }
    }

    when (dialog) {
        HubDialog.GATES -> ListDialog(
            title = stringResource(R.string.hub_gates_title),
            onDismiss = { dialog = null },
        ) {
            hub.gates.forEach { g ->
                OutlinedButton(onClick = { dialog = null; onAction(HubAction.EnterGate(g.uid)) }, modifier = Modifier.fillMaxWidth()) {
                    Text(androidx.compose.ui.res.pluralStringResource(R.plurals.hub_gate, g.daysLeft, g.grade, g.enemies, g.daysLeft), style = GameType.system, color = GateColor)
                }
            }
        }
        HubDialog.TRAVEL -> ListDialog(
            title = stringResource(R.string.hub_travel_title),
            onDismiss = { dialog = null },
        ) {
            hub.travel.forEach { r ->
                OutlinedButton(onClick = { dialog = null; leave(HubAction.Travel(r.id)) }, modifier = Modifier.fillMaxWidth()) { Text("→ ${r.name}") }
            }
        }
        HubDialog.PREPARE -> ListDialog(
            title = stringResource(R.string.hub_prepare_title),
            onDismiss = { dialog = null },
        ) {
            hub.preparations.forEach { p ->
                OutlinedButton(
                    onClick = { dialog = null; onAction(HubAction.Prepare(p.id)) },
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
        null -> Unit
    }
    pending?.let { action ->
        AppAlertDialog(
            onDismissRequest = { pending = null },
            confirmButton = { TextButton(onClick = { pending = null; onAction(action) }) { Text(stringResource(R.string.hub_leave_anyway)) } },
            dismissButton = { TextButton(onClick = { pending = null }) { Text(stringResource(R.string.cancel)) } },
            title = { Text(stringResource(R.string.hub_deadline_title)) },
            text = { Text(hub.deadlineWarning ?: "") },
        )
    }
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

private val TowerColor = Color(0xFFAB47BC)
private val GateColor = Color(0xFFFFA726)

private fun tensionColor(t: Int) = when (t) {
    1 -> Color(0xFF66BB6A)
    2 -> Color(0xFFFFD54F)
    3 -> Color(0xFFFFA726)
    else -> Color(0xFFEF5350)
}

private fun dangerColor(d: Int) = when {
    d < 25 -> Color(0xFF66BB6A)
    d < 50 -> Color(0xFFFFD54F)
    d < 75 -> Color(0xFFFFA726)
    else -> Color(0xFFEF5350)
}
