package com.yukselis.protocol.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yukselis.engine.story.NpcView
import com.yukselis.engine.story.RankingView
import com.yukselis.engine.story.RelationsView
import com.yukselis.protocol.R
import com.yukselis.protocol.ui.theme.CompanionColor
import com.yukselis.protocol.ui.theme.EchoColor
import com.yukselis.protocol.ui.theme.GameType

/** İlişkiler: tanınan NPC'ler; dört eksen, durum, son hatıralar ve izler. Dokununca ayrıntı açılır. */
@Composable
fun RelationsSection(view: RelationsView) {
    Text(stringResource(R.string.party_line, view.companions, view.capacity), style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (view.armyPower > 0) {
        Text(stringResource(R.string.army_line, view.armyPower, view.army.joinToString(", ")), style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (view.npcs.isEmpty()) Text(stringResource(R.string.no_relations), style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant)
    view.npcs.forEach { NpcCard(it) }
}

@Composable
private fun NpcCard(n: NpcView) {
    var open by rememberSaveable(n.id) { mutableStateOf(false) }
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable { open = !open },
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            val nameColor = when {
                !n.alive -> MaterialTheme.colorScheme.outline
                n.companion -> CompanionColor
                else -> MaterialTheme.colorScheme.onSurface
            }
            Text("${n.fullName} · ${stringResource(R.string.npc_level, n.level, n.gradeName)}", style = GameType.systemTitle, color = nameColor)
            if (n.status.isNotEmpty()) Text(n.status, style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Axis(stringResource(R.string.axis_trust), n.trust, Modifier.weight(1f))
                Axis(stringResource(R.string.axis_respect), n.respect, Modifier.weight(1f))
                Axis(stringResource(R.string.axis_fear), n.fear, Modifier.weight(1f))
                Axis(stringResource(R.string.axis_loyalty), n.bond, Modifier.weight(1f))
            }
            if (open) {
                Text(n.description, style = GameType.system, modifier = Modifier.padding(top = 6.dp))
                n.goal?.let { Text(stringResource(R.string.npc_goal, it), style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if (n.traits.isNotEmpty()) Text(stringResource(R.string.npc_traits, n.traits.joinToString(", ")), style = GameType.system, color = Color(0xFFAB47BC))
                if (n.memories.isNotEmpty()) {
                    Text(stringResource(R.string.npc_memories), style = GameType.system, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
                    n.memories.forEach { Text("· $it", style = GameType.system) }
                }
            }
        }
    }
}

@Composable
private fun Axis(label: String, value: Int, modifier: Modifier) {
    val color = when {
        value >= 40 -> Color(0xFF66BB6A)
        value <= -40 -> Color(0xFFEF5350)
        else -> MaterialTheme.colorScheme.onSurface
    }
    Column(modifier = modifier) {
        Text(label, style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text(if (value > 0) "+$value" else "$value", style = GameType.systemTitle, color = color, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

/** OP MC Küresel Sıralama: dünyanın gördüğü liste + Yankı'nın sakladığı gerçek yer. */
@Composable
fun RankingSection(view: RankingView) {
    Text(stringResource(R.string.ranking_you, view.publicRank, view.total), style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(view.note, style = GameType.system, color = EchoColor)
    view.rows.forEach { r ->
        val color = when {
            r.you -> MaterialTheme.colorScheme.primary
            r.npc -> CompanionColor
            else -> MaterialTheme.colorScheme.onSurface
        }
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
            Text("#${r.rank}", style = GameType.system, color = color, modifier = Modifier.width(44.dp))
            Text(r.name, style = GameType.system, color = color, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.npc_level, r.level, r.gradeName), style = GameType.system, color = color)
        }
    }
}
