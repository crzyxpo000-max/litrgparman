package com.yukselis.protocol.ui.title

import androidx.compose.foundation.layout.widthIn
import com.yukselis.protocol.ui.components.EchoLine
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.yukselis.protocol.ui.components.Button
import androidx.compose.material3.MaterialTheme
import com.yukselis.protocol.ui.components.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yukselis.engine.i18n.Lang
import com.yukselis.engine.save.SlotInfo
import com.yukselis.engine.story.ArchiveSectionView
import com.yukselis.engine.story.ArchiveView
import com.yukselis.protocol.R
import com.yukselis.protocol.data.ThemeMode
import com.yukselis.protocol.ui.components.Localized
import com.yukselis.protocol.ui.components.SectionTitle
import com.yukselis.protocol.ui.components.systemWindow
import com.yukselis.protocol.ui.more.LanguagePicker
import com.yukselis.protocol.ui.more.ThemePicker
import com.yukselis.protocol.ui.theme.Game
import com.yukselis.protocol.ui.theme.GameType
import com.yukselis.protocol.ui.theme.rarityColorOf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitleScreen(
    lang: Lang,
    autosave: SlotInfo?,
    onNewGame: (name: String, ironman: Boolean, newGamePlus: Boolean) -> Unit,
    newGamePlusUnlocked: Boolean = false,
    onContinue: () -> Unit,
    onLanguage: (Lang) -> Unit,
    theme: ThemeMode,
    onTheme: (ThemeMode) -> Unit,
    ready: Boolean = true,
    archive: ArchiveView? = null,
) {
    val defaultName = stringResource(R.string.default_player_name)
    var name by rememberSaveable { mutableStateOf("") }
    var ironman by rememberSaveable { mutableStateOf(false) }
    var newGamePlus by rememberSaveable { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            // Tablette başlık ekranı tek bir okunur sütun kalır.
            .widthIn(max = 560.dp)
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Spacer(Modifier.height(56.dp))
        // Logo: kesik köşeli yazı, sola yaslı; Sistem'in açılış satırı altında.
        // Logo büyük yazıda sözcük ortasından bölünmesin (UI_REVIEW C4): ölçeği en çok 1.3.
        com.yukselis.protocol.ui.components.CappedFontScale {
            Text(
                text = stringResource(R.string.app_name),
                style = GameType.heading.copy(fontSize = 40.sp, lineHeight = 42.sp),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = "[ ${stringResource(R.string.system_header).uppercase(LocalConfiguration.current.locales[0])} ] " + stringResource(R.string.system_online),
            style = GameType.systemTitle.copy(fontSize = 12.sp),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.tagline),
            style = GameType.narration,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(36.dp))
        if (autosave != null) {
            Button(onClick = onContinue, enabled = ready, modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.continue_game))
                    Text(
                        text = stringResource(R.string.continue_summary, autosave.playerName, autosave.level, autosave.day),
                        style = GameType.system,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        Text(stringResource(R.string.new_game), style = GameType.systemTitle, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { if (it.length <= MAX_NAME) name = it },
            label = { Text(stringResource(R.string.your_name)) },
            placeholder = { Text(defaultName) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = ironman, role = androidx.compose.ui.semantics.Role.Switch, onValueChange = { ironman = it })
                .padding(vertical = 12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.ironman_mode), style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(R.string.ironman_desc), style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = ironman, onCheckedChange = null)
        }
        if (newGamePlusUnlocked) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(value = newGamePlus, role = androidx.compose.ui.semantics.Role.Switch, onValueChange = { newGamePlus = it })
                    .padding(bottom = 12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.ng_plus), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.ng_plus_desc), style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = newGamePlus, onCheckedChange = null)
            }
        }
        val start = { onNewGame(name.ifBlank { defaultName }, ironman, newGamePlus && newGamePlusUnlocked) }
        // Kayıt varken yeni oyun otomatik kaydın üstüne yazar: önce onay (UI_REVIEW B1).
        var confirmNew by rememberSaveable { mutableStateOf(false) }
        if (confirmNew && autosave != null) {
            com.yukselis.protocol.ui.components.AppAlertDialog(
                onDismissRequest = { confirmNew = false },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = { confirmNew = false; start() }) { Text(stringResource(R.string.confirm_new_game_ok)) }
                },
                dismissButton = { androidx.compose.material3.TextButton(onClick = { confirmNew = false }) { Text(stringResource(R.string.cancel)) } },
                title = { Text(stringResource(R.string.confirm_new_game_title)) },
                text = {
                    Text(stringResource(R.string.confirm_new_game, stringResource(R.string.continue_summary, autosave.playerName, autosave.level, autosave.day)))
                },
            )
        }
        if (autosave != null) {
            OutlinedButton(onClick = { confirmNew = true }, enabled = ready, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.start)) }
        } else {
            Button(onClick = start, enabled = ready, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.start)) }
        }
        if (!ready) {
            Text(
                text = stringResource(R.string.loading_content),
                style = GameType.system,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Spacer(Modifier.height(24.dp))
        // Yankı açılışta bir kez konuşur: kayıt varsa geri dönüşü, yoksa uyanışı fark eder.
        EchoLine(
            stringResource(if (autosave != null) R.string.title_echo_back else R.string.title_echo_new),
            modifier = Modifier.align(Alignment.Start),
        )
        var showArchive by rememberSaveable { mutableStateOf(false) }
        if (ready && archive != null) {
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = { showArchive = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.archive_button, archive.unlockedTotal, archive.grandTotal))
            }
        }
        Spacer(Modifier.height(20.dp))
        LanguagePicker(lang, onLanguage)
        ThemePicker(theme, onTheme)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "v1.0",
            style = GameType.system,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        if (showArchive && archive != null) {
            ModalBottomSheet(onDismissRequest = { showArchive = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
                Localized { ArchiveSheet(archive) }
            }
        }
    }
}

@Composable
private fun ArchiveSheet(archive: ArchiveView) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(stringResource(R.string.archive_title), style = GameType.heading, color = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.archive_runs, archive.completedGames), style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (archive.unlockedTotal == 0) {
            Text(stringResource(R.string.archive_empty), style = GameType.narrationPast, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ArchiveSection(stringResource(R.string.archive_endings), archive.endings)
        ArchiveSection(stringResource(R.string.archive_archetypes), archive.archetypes)
        ArchiveSection(stringResource(R.string.archive_hidden_classes), archive.hiddenClasses)
        ArchiveSection(stringResource(R.string.archive_unique_skills), archive.uniqueSkills)
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun ArchiveSection(title: String, section: ArchiveSectionView) {
    SectionTitle("$title (${section.unlockedCount}/${section.totalCount})")
    section.entries.forEach { entry ->
        val accent = if (entry.unlocked) (entry.rarity?.let { rarityColorOf(it) } ?: Game.colors.edge) else Game.colors.line
        Column(modifier = Modifier.fillMaxWidth().systemWindow(accent = accent, cut = 8).padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = entry.name,
                style = GameType.systemTitle,
                color = if (entry.unlocked) accent else MaterialTheme.colorScheme.outline,
            )
            if (entry.unlocked && entry.description.isNotBlank()) {
                Text(entry.description, style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private const val MAX_NAME = 20
