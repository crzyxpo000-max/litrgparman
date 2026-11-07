package com.yukselis.protocol.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yukselis.protocol.R
import com.yukselis.protocol.ui.components.AppAlertDialog
import com.yukselis.protocol.ui.components.OutlinedButton
import com.yukselis.protocol.ui.theme.GameType

/** Uygulamaya gömülü yazı tipleri ve lisans dosyaları (assets/licenses). Hepsi SIL Open Font License 1.1. */
private val FONTS = listOf(
    "Literata" to "licenses/literata_OFL.txt",
    "JetBrains Mono" to "licenses/jetbrains_mono_OFL.txt",
    "Chakra Petch" to "licenses/chakra_petch_OFL.txt",
)

/** Ayarlar'da "Lisanslar": yazı tiplerinin adı ve dokununca lisans metninin tamamı. */
@Composable
fun LicensesRow(modifier: Modifier = Modifier) {
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(stringResource(R.string.licenses), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(R.string.licenses_desc), style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FONTS.forEach { (name, path) ->
            OutlinedButton(onClick = { open = path }, modifier = Modifier.fillMaxWidth()) { Text(name) }
        }
    }
    open?.let { path ->
        val context = LocalContext.current
        val text = remember(path) { runCatching { context.assets.open(path).bufferedReader().use { it.readText() } }.getOrDefault("") }
        AppAlertDialog(
            onDismissRequest = { open = null },
            confirmButton = { TextButton(onClick = { open = null }) { Text(stringResource(R.string.close)) } },
            title = { Text(FONTS.first { it.second == path }.first) },
            text = { Text(text, style = GameType.system, modifier = Modifier.verticalScroll(rememberScrollState())) },
        )
    }
}
