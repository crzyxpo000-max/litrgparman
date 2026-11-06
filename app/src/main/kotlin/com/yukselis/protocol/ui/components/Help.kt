package com.yukselis.protocol.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yukselis.protocol.R
import com.yukselis.protocol.ui.theme.GameType

/** Bir kez gösterilen ipuçları: görülenler ayarlarda saklanır. */
class Tips(val seen: Set<String>, val markSeen: (String) -> Unit)

val LocalTips = staticCompositionLocalOf { Tips(emptySet()) {} }

/** İlk karşılaşmada çıkan tek satırlık ipucu kartı; "Anladım" deyince bir daha görünmez. */
@Composable
fun TipCard(id: String, text: String, modifier: Modifier = Modifier) {
    val tips = LocalTips.current
    if (id in tips.seen) return
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)) {
            Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Text(text, style = GameType.system, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f).padding(start = 8.dp))
            TextButton(onClick = { tips.markSeen(id) }) { Text(stringResource(R.string.tip_got_it)) }
        }
    }
}

/** Küçük (i) düğmesi: dokununca başlık + açıklama penceresi. */
@Composable
fun HelpButton(title: String, entries: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }, modifier = modifier.size(36.dp)) {
        Icon(Icons.Filled.Info, contentDescription = title, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
    if (open) HelpDialog(title, entries) { open = false }
}

@Composable
fun HelpDialog(title: String, entries: List<Pair<String, String>>, onDismiss: () -> Unit) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                entries.forEach { (term, meaning) -> HelpEntry(term, meaning) }
            }
        },
    )
}

@Composable
fun HelpEntry(term: String, meaning: String) {
    Column {
        Text(term, style = GameType.systemTitle, color = MaterialTheme.colorScheme.primary)
        Text(meaning, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
