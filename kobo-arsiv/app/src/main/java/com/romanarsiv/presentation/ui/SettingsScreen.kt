package com.romanarsiv.presentation.ui

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.romanarsiv.R
import com.romanarsiv.background.ArchiveJobs
import com.romanarsiv.settings.AmoledMode
import com.romanarsiv.settings.AppLanguage
import com.romanarsiv.settings.ScreenInfo
import com.romanarsiv.settings.ThemeMode
import com.romanarsiv.settings.UiPrefs
import kotlin.math.roundToInt

@Composable
fun SettingsTab(prefs: UiPrefs, onChange: ((UiPrefs) -> UiPrefs) -> Unit, onLanguageChanged: () -> Unit) {
    val context = LocalContext.current
    val oled = remember { ScreenInfo.isLikelyOled(context) }
    val darkNow = isAppInDarkTheme(prefs.theme)
    val pureBlackNow = darkNow && usesPureBlack(prefs)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SettingsGroup(AppIcons.Palette, stringResource(R.string.settings_appearance)) {
            Label(stringResource(R.string.settings_theme))
            Choice(
                options = listOf(
                    ThemeMode.SYSTEM to stringResource(R.string.theme_system),
                    ThemeMode.LIGHT to stringResource(R.string.theme_light),
                    ThemeMode.DARK to stringResource(R.string.theme_dark),
                ),
                selected = prefs.theme,
                onSelect = { mode -> onChange { it.copy(theme = mode) } },
            )

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.DarkMode, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.size(8.dp))
                Label(stringResource(R.string.settings_amoled))
            }
            Choice(
                options = listOf(
                    AmoledMode.AUTO to stringResource(R.string.amoled_auto),
                    AmoledMode.ON to stringResource(R.string.amoled_on),
                    AmoledMode.OFF to stringResource(R.string.amoled_off),
                ),
                selected = prefs.amoled,
                onSelect = { mode -> onChange { it.copy(amoled = mode) } },
            )
            Help(
                buildString {
                    append(stringResource(if (oled) R.string.amoled_detected_oled else R.string.amoled_detected_lcd))
                    append(' ')
                    append(
                        stringResource(
                            when {
                                !darkNow -> R.string.amoled_state_light
                                pureBlackNow -> R.string.amoled_state_black
                                else -> R.string.amoled_state_gray
                            },
                        ),
                    )
                },
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Spacer(Modifier.height(12.dp))
                SwitchRow(
                    title = stringResource(R.string.settings_dynamic),
                    help = stringResource(R.string.settings_dynamic_help),
                    checked = prefs.dynamicColor,
                    onCheckedChange = { value -> onChange { it.copy(dynamicColor = value) } },
                )
            }
        }

        SettingsGroup(AppIcons.Language, stringResource(R.string.settings_language)) {
            Choice(
                options = listOf(
                    AppLanguage.SYSTEM to stringResource(R.string.language_system),
                    AppLanguage.TURKISH to "Türkçe",
                    AppLanguage.ENGLISH to "English",
                ),
                selected = prefs.language,
                onSelect = { language ->
                    if (language != prefs.language) {
                        onChange { it.copy(language = language) }
                        onLanguageChanged()
                    }
                },
            )
            Help(stringResource(R.string.settings_language_help))
        }

        SettingsGroup(AppIcons.Speed, stringResource(R.string.settings_downloads)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Label(stringResource(R.string.settings_parallel), Modifier.weight(1f))
                Text(prefs.maxParallel.toString(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = prefs.maxParallel.toFloat(),
                onValueChange = { v -> onChange { it.copy(maxParallel = v.roundToInt()) } },
                valueRange = 1f..ArchiveJobs.MAX_PARALLEL.toFloat(),
                steps = ArchiveJobs.MAX_PARALLEL - 2,
            )
            Help(stringResource(R.string.settings_parallel_help))

            Spacer(Modifier.height(12.dp))
            var author by rememberSaveable { mutableStateOf(prefs.authorName) }
            OutlinedTextField(
                value = author,
                onValueChange = { value ->
                    author = value
                    onChange { it.copy(authorName = value) }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.settings_author)) },
                supportingText = { Text(stringResource(R.string.settings_author_help)) },
            )
        }

        SettingsGroup(AppIcons.Folder, stringResource(R.string.settings_storage)) {
            Help(stringResource(R.string.settings_storage_help))
        }

        Text(
            stringResource(R.string.settings_version, versionName(context)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 24.dp),
        )
    }
}

@Composable
private fun SettingsGroup(icon: ImageVector, title: String, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.size(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun Label(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.labelLarge, modifier = modifier.padding(bottom = 8.dp))
}

@Composable
private fun Help(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> Choice(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { i, (value, label) ->
            SegmentedButton(
                selected = value == selected,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(index = i, count = options.size),
                label = { Text(label, maxLines = 1) },
            )
        }
    }
}

@Composable
private fun SwitchRow(title: String, help: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(help, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun versionName(context: android.content.Context): String = runCatching {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName
}.getOrNull() ?: "?"
