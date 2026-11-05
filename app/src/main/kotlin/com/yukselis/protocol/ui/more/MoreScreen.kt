package com.yukselis.protocol.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yukselis.engine.core.GameMode
import com.yukselis.engine.i18n.Lang
import com.yukselis.engine.save.SaveManager
import com.yukselis.engine.save.SlotInfo
import com.yukselis.protocol.R
import com.yukselis.protocol.data.Settings
import com.yukselis.protocol.data.ThemeMode
import com.yukselis.protocol.ui.components.SectionTitle
import com.yukselis.protocol.ui.theme.GameType

@Composable
fun MoreScreen(
    lang: Lang,
    settings: Settings,
    mode: GameMode,
    slots: List<Pair<String, SlotInfo?>>,
    onSave: (String) -> Unit,
    onLoad: (String) -> Unit,
    onLanguage: (Lang) -> Unit,
    onSettings: ((Settings) -> Settings) -> Unit,
    onBackToTitle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        // Sonraki aşamalarda açılacak bölümler
        SectionTitle(stringResource(R.string.tab_more))
        ComingSoon(stringResource(R.string.map), 4)
        ComingSoon(stringResource(R.string.daily_quest), 2)
        ComingSoon(stringResource(R.string.relationships), 5)
        ComingSoon(stringResource(R.string.quests), 4)

        SectionTitle(stringResource(R.string.saves))
        slots.forEach { (slot, info) ->
            SlotRow(
                slot = slot,
                info = info,
                canSave = slot != SaveManager.AUTO && mode != GameMode.IRONMAN,
                onSave = { onSave(slot) },
                onLoad = { onLoad(slot) },
            )
        }
        if (mode == GameMode.IRONMAN) {
            Text(stringResource(R.string.msg_ironman_no_manual), style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        SectionTitle(stringResource(R.string.settings))
        LanguagePicker(lang, onLanguage)
        ThemePicker(settings.theme, { mode -> onSettings { it.copy(theme = mode) } })
        ToggleRow(stringResource(R.string.typewriter), null, settings.typewriter, enabled = true) { on ->
            onSettings { it.copy(typewriter = on) }
        }
        ToggleRow(stringResource(R.string.haptics), null, settings.haptics, enabled = true) { on ->
            onSettings { it.copy(haptics = on) }
        }
        ToggleRow(stringResource(R.string.narrator_mode), stringResource(R.string.narrator_mode_desc), settings.narratorMode, enabled = false) { on ->
            onSettings { it.copy(narratorMode = on) }
        }

        Spacer(Modifier.padding(top = 16.dp))
        OutlinedButton(onClick = onBackToTitle, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.back_to_title))
        }
        Spacer(Modifier.padding(bottom = 24.dp))
    }
}

@Composable
private fun ComingSoon(label: String, phase: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(stringResource(R.string.coming_in_phase, phase), style = GameType.system, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun SlotRow(slot: String, info: SlotInfo?, canSave: Boolean, onSave: () -> Unit, onLoad: () -> Unit) {
    val name = when (slot) {
        SaveManager.AUTO -> stringResource(R.string.slot_auto)
        else -> stringResource(R.string.slot_n, SaveManager.MANUAL.indexOf(slot) + 1)
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = GameType.systemTitle)
                Text(
                    text = info?.let { stringResource(R.string.continue_summary, it.playerName, it.level, it.day) }
                        ?: stringResource(R.string.empty_slot),
                    style = GameType.system,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (canSave) {
                OutlinedButton(onClick = onSave) { Text(stringResource(R.string.save)) }
                Spacer(Modifier.width(8.dp))
            }
            Button(onClick = onLoad, enabled = info != null) { Text(stringResource(R.string.load)) }
        }
    }
}

@Composable
fun LanguagePicker(current: Lang, onPick: (Lang) -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
    ) {
        Text(stringResource(R.string.language), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        // Dil adları her zaman kendi dillerinde gösterilir.
        listOf(Lang.TR to "Türkçe", Lang.EN to "English").forEach { (lang, label) ->
            if (lang == current) {
                Button(onClick = { }) { Text(label) }
            } else {
                OutlinedButton(onClick = { onPick(lang) }) { Text(label) }
            }
        }
    }
}

/** Tema: Sistem · Açık · Koyu. Seçim anında uygulanır ve kaydedilir. */
@Composable
fun ThemePicker(current: ThemeMode, onPick: (ThemeMode) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(stringResource(R.string.theme), style = MaterialTheme.typography.bodyLarge)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        ) {
            listOf(
                ThemeMode.SYSTEM to R.string.theme_system,
                ThemeMode.LIGHT to R.string.theme_light,
                ThemeMode.DARK to R.string.theme_dark,
                ThemeMode.AMOLED to R.string.theme_amoled,
            ).forEach { (mode, label) ->
                val padding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                if (mode == current) {
                    Button(onClick = { }, contentPadding = padding, modifier = Modifier.weight(1f)) { Text(stringResource(label), maxLines = 1) }
                } else {
                    OutlinedButton(onClick = { onPick(mode) }, contentPadding = padding, modifier = Modifier.weight(1f)) {
                        Text(stringResource(label), maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, description: String?, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            description?.let { Text(it, style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}
