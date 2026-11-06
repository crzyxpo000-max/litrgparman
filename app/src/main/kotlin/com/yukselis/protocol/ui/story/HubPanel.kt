package com.yukselis.protocol.ui.story

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yukselis.engine.story.HubView
import com.yukselis.engine.world.HubAction
import com.yukselis.protocol.R
import com.yukselis.protocol.ui.theme.GameType

/** Gün akışı: fırsat eli (olay kartları), serbest eylemler, kapılar, yolculuk, akşam kampı. */
@Composable
fun HubPanel(hub: HubView, onAction: (HubAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Text(
            text = stringResource(R.string.hub_slots, hub.slotsLeft, hub.slotsTotal, hub.danger),
            style = GameType.system,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (hub.mustCamp) {
            Button(onClick = { onAction(HubAction.Camp) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.hub_camp)) }
            return@Column
        }
        if (hub.cards.isNotEmpty()) {
            Text(stringResource(R.string.hub_hand), style = GameType.systemTitle, color = MaterialTheme.colorScheme.primary)
            hub.cards.forEach { card ->
                OutlinedButton(onClick = { onAction(HubAction.Card(card.id)) }, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(card.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        // Gerilim: ● sayısı
                        Text("●".repeat(card.tension), style = GameType.system, color = tensionColor(card.tension))
                    }
                }
            }
        }
        Text(stringResource(R.string.hub_free), style = GameType.systemTitle, color = MaterialTheme.colorScheme.primary)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val pad = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
            OutlinedButton(onClick = { onAction(HubAction.Explore) }, contentPadding = pad, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.hub_explore), maxLines = 1) }
            OutlinedButton(onClick = { onAction(HubAction.Train) }, contentPadding = pad, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.hub_train), maxLines = 1) }
            OutlinedButton(onClick = { onAction(HubAction.Rest) }, contentPadding = pad, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.hub_rest), maxLines = 1) }
        }
        hub.gates.forEach { g ->
            OutlinedButton(onClick = { onAction(HubAction.EnterGate(g.uid)) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.hub_gate, g.grade, g.enemies, g.daysLeft), style = GameType.system, color = Color(0xFFFFA726))
            }
        }
        if (hub.travel.isNotEmpty()) {
            Text(stringResource(R.string.hub_travel), style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant)
            hub.travel.forEach { r ->
                OutlinedButton(onClick = { onAction(HubAction.Travel(r.id)) }, modifier = Modifier.fillMaxWidth()) { Text("→ ${r.name}") }
            }
        }
    }
}

private fun tensionColor(t: Int) = when (t) {
    1 -> Color(0xFF66BB6A)
    2 -> Color(0xFFFFD54F)
    3 -> Color(0xFFFFA726)
    else -> Color(0xFFEF5350)
}
