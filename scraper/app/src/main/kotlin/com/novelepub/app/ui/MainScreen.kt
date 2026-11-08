package com.novelepub.app.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.novelepub.app.AppSettings
import com.novelepub.app.Busy
import com.novelepub.app.MainViewModel
import com.novelepub.app.Mode
import com.novelepub.app.R
import com.novelepub.app.UiState
import com.novelepub.core.Cover
import com.novelepub.core.GenericConfig
import com.novelepub.core.SearchResult
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel, state: UiState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/epub+zip")) { uri ->
        if (uri != null) scope.launch {
            val error = vm.saveTo(uri)
            snackbar.showSnackbar(if (error == null) context.getString(R.string.saved) else context.getString(R.string.save_failed, error))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.novel == null) {
                ModeTabs(state.mode, enabled = state.busy == null, onChange = vm::onModeChange)
            }

            if (state.novel == null && state.mode == Mode.SEARCH) {
                SearchSection(vm, state, hasWebSearch = vm.settings.hasWebSearch)
            } else {
                UrlSection(vm, state)

                if (state.isGeneric && state.novel == null) {
                    GenericPanel(state.generic, enabled = state.busy == null, onChange = vm::onGenericChange)
                }

                if (state.novel == null || state.busy == Busy.LOADING_INFO) {
                    Button(
                        onClick = vm::fetchInfo,
                        enabled = state.busy == null && state.url.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.fetch)) }
                }
            }

            if (state.busy == Busy.LOADING_INFO) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(stringResource(R.string.loading_info), style = MaterialTheme.typography.bodySmall)
            }

            ErrorCard(state, onDismiss = vm::dismissError)

            state.novel?.let { novel ->
                NovelCard(state)
                if (state.siteName == null && !novel.isCrawl && state.result == null && state.busy == null) {
                    TextButton(onClick = vm::switchToCrawl) { Text(stringResource(R.string.use_crawl)) }
                }
                if (state.result == null) {
                    if (!novel.isCrawl) RangeFields(state, vm)
                    DownloadSection(state, vm)
                }
            }

            state.result?.let { file ->
                DoneCard(
                    file = file,
                    chapters = state.resultChapters,
                    onSave = { saveLauncher.launch(file.name) },
                    onOpen = { if (!openEpub(context, file)) scope.launch { snackbar.showSnackbar(context.getString(R.string.no_reader)) } },
                    onShare = { shareEpub(context, file) },
                    onStartOver = vm::startOver,
                )
            }

            if (state.novel != null && state.busy == null && state.result == null) {
                TextButton(onClick = vm::startOver) { Text(stringResource(R.string.reset)) }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.personal_use),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showSettings) {
        SettingsDialog(vm.settings, cacheBytes = remember { vm.cacheSizeBytes() }, onClearCache = {
            vm.clearCache()
            scope.launch { snackbar.showSnackbar(context.getString(R.string.settings_cache_cleared)) }
        }, onDismiss = { showSettings = false })
    }
}

@Composable
private fun ModeTabs(mode: Mode, enabled: Boolean, onChange: (Mode) -> Unit) {
    TabRow(selectedTabIndex = mode.ordinal) {
        Tab(selected = mode == Mode.SEARCH, enabled = enabled, onClick = { onChange(Mode.SEARCH) }, text = { Text(stringResource(R.string.mode_search)) })
        Tab(selected = mode == Mode.URL, enabled = enabled, onClick = { onChange(Mode.URL) }, text = { Text(stringResource(R.string.mode_url)) })
    }
}

@Composable
private fun SearchSection(vm: MainViewModel, state: UiState, hasWebSearch: Boolean) {
    OutlinedTextField(
        value = state.query,
        onValueChange = vm::onQueryChange,
        label = { Text(stringResource(R.string.search_label)) },
        singleLine = true,
        enabled = state.busy == null,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { vm.search() }),
        modifier = Modifier.fillMaxWidth(),
    )
    Button(
        onClick = vm::search,
        enabled = state.busy == null && state.query.isNotBlank(),
        modifier = Modifier.fillMaxWidth(),
    ) { Text(stringResource(R.string.search_button)) }
    if (!hasWebSearch) {
        Text(stringResource(R.string.search_only_rr), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (state.busy == Busy.SEARCHING) {
        LinearProgressIndicator(Modifier.fillMaxWidth())
        Text(stringResource(R.string.searching), style = MaterialTheme.typography.bodySmall)
    }
    state.searchErrors.forEach { (provider, message) ->
        Text(
            stringResource(R.string.search_provider_error, provider, message),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
    val results = state.searchResults ?: return
    if (results.isEmpty() && state.busy == null) {
        Text(stringResource(R.string.search_no_results))
    }
    results.forEach { result ->
        SearchResultCard(result, enabled = state.busy == null, onClick = { vm.pickResult(result) })
    }
}

@Composable
private fun SearchResultCard(result: SearchResult, enabled: Boolean, onClick: () -> Unit) {
    Card(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(result.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                result.host + " · " + (result.siteName?.let { stringResource(R.string.result_dedicated, it) } ?: stringResource(R.string.result_generic)),
                style = MaterialTheme.typography.labelMedium,
                color = if (result.siteName != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (result.snippet.isNotBlank()) {
                Text(result.snippet, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Text(
                stringResource(R.string.found_by, result.provider),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun UrlSection(vm: MainViewModel, state: UiState) {
    val clipboard = LocalClipboardManager.current
    val editable = state.busy == null && state.novel == null
    OutlinedTextField(
        value = state.url,
        onValueChange = vm::onUrlChange,
        label = { Text(stringResource(R.string.url_label)) },
        placeholder = { Text(stringResource(R.string.url_hint)) },
        singleLine = true,
        enabled = editable,
        isError = state.invalidUrl,
        supportingText = when {
            state.invalidUrl -> { { Text(stringResource(R.string.invalid_url)) } }
            state.siteName != null -> { { Text(stringResource(R.string.site_detected, state.siteName)) } }
            state.isGeneric -> { { Text(stringResource(R.string.site_generic)) } }
            else -> null
        },
        trailingIcon = if (editable) {
            { TextButton(onClick = { clipboard.getText()?.text?.let(vm::onUrlChange) }) { Text(stringResource(R.string.paste)) } }
        } else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun GenericPanel(config: GenericConfig, enabled: Boolean, onChange: (GenericConfig) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.generic_title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.generic_help), style = MaterialTheme.typography.bodySmall)
            @Composable
            fun field(label: Int, value: String, keyboard: KeyboardType = KeyboardType.Text, set: (String) -> GenericConfig) =
                OutlinedTextField(
                    value = value,
                    onValueChange = { onChange(set(it)) },
                    label = { Text(stringResource(label)) },
                    singleLine = true,
                    enabled = enabled,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboard),
                    modifier = Modifier.fillMaxWidth(),
                )
            field(R.string.generic_chapter_links, config.chapterLinkSelector) { config.copy(chapterLinkSelector = it) }
            field(R.string.generic_content, config.contentSelector) { config.copy(contentSelector = it) }
            field(R.string.generic_chapter_title, config.titleSelector) { config.copy(titleSelector = it) }
            field(R.string.generic_next, config.nextSelector) { config.copy(nextSelector = it) }
            field(R.string.generic_remove, config.removeSelector) { config.copy(removeSelector = it) }
            field(R.string.generic_novel_title, config.novelTitle) { config.copy(novelTitle = it) }
            field(R.string.generic_author, config.author) { config.copy(author = it) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    field(R.string.generic_language, config.language) { config.copy(language = it.trim().take(12)) }
                }
                Column(Modifier.weight(1f)) {
                    field(R.string.generic_max, config.maxChapters.toString(), KeyboardType.Number) {
                        config.copy(maxChapters = it.filter(Char::isDigit).take(5).toIntOrNull()?.coerceAtLeast(1) ?: 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun NovelCard(state: UiState) {
    val novel = state.novel ?: return
    var expanded by rememberSaveable { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CoverImage(state.cover)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(novel.title, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.by_author, novel.author), style = MaterialTheme.typography.bodyMedium)
                Text(
                    if (novel.isCrawl) stringResource(R.string.crawl_mode)
                    else pluralStringResource(R.plurals.chapter_count, novel.chapters.size, novel.chapters.size),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        if (novel.description.isNotBlank()) {
            Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp)) {
                Text(
                    novel.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = if (expanded) Int.MAX_VALUE else 4,
                    overflow = TextOverflow.Ellipsis,
                )
                TextButton(onClick = { expanded = !expanded }) {
                    Text(stringResource(if (expanded) R.string.description_less else R.string.description_more))
                }
            }
        }
    }
}

@Composable
private fun CoverImage(cover: Cover?) {
    val bitmap = remember(cover) {
        cover?.let { BitmapFactory.decodeByteArray(it.bytes, 0, it.bytes.size)?.asImageBitmap() }
    } ?: return
    Image(
        bitmap = bitmap,
        contentDescription = stringResource(R.string.cover),
        contentScale = ContentScale.Crop,
        modifier = Modifier.width(72.dp).height(108.dp),
    )
}

@Composable
private fun RangeFields(state: UiState, vm: MainViewModel) {
    val total = state.novel?.chapters?.size ?: 0
    val invalid = state.selectedRange() == null
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.rangeFrom,
                onValueChange = { vm.onRangeChange(it, state.rangeTo) },
                label = { Text(stringResource(R.string.range_from)) },
                singleLine = true,
                enabled = state.busy == null,
                isError = invalid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = state.rangeTo,
                onValueChange = { vm.onRangeChange(state.rangeFrom, it) },
                label = { Text(stringResource(R.string.range_to)) },
                singleLine = true,
                enabled = state.busy == null,
                isError = invalid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
        }
        if (invalid) {
            Text(
                stringResource(R.string.range_invalid, total),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun DownloadSection(state: UiState, vm: MainViewModel) {
    when (state.busy) {
        Busy.DOWNLOADING, Busy.WRITING -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val p = state.progress
            if (state.busy == Busy.WRITING) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(stringResource(R.string.writing_epub))
            } else if (p?.total != null) {
                LinearProgressIndicator(progress = { p.done.toFloat() / p.total!! }, modifier = Modifier.fillMaxWidth())
                Text(stringResource(R.string.progress_known, p.done, p.total!!))
            } else {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                if (p != null) Text(stringResource(R.string.progress_crawl, p.done))
            }
            if (p != null && state.busy == Busy.DOWNLOADING) {
                val suffix = if (p.fromCache) " " + stringResource(R.string.from_cache) else ""
                Text(p.currentTitle + suffix, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (state.busy == Busy.DOWNLOADING) {
                OutlinedButton(onClick = vm::cancel, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.cancel)) }
            }
        }
        else -> Button(
            onClick = vm::download,
            enabled = state.busy == null && (state.novel?.isCrawl == true || state.selectedRange() != null),
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.download)) }
    }
}

@Composable
private fun ErrorCard(state: UiState, onDismiss: () -> Unit) {
    if (state.error == null) return
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (state.errorCancelled) {
                Text(stringResource(R.string.error_cancelled))
            } else {
                Text(stringResource(R.string.error_title), style = MaterialTheme.typography.titleSmall)
                Text(state.error, style = MaterialTheme.typography.bodySmall)
                if (state.novel != null) Text(stringResource(R.string.error_resume_hint), style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text(stringResource(R.string.close)) }
        }
    }
}

@Composable
private fun DoneCard(file: File, chapters: Int, onSave: () -> Unit, onOpen: () -> Unit, onShare: () -> Unit, onStartOver: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.done_title), style = MaterialTheme.typography.titleMedium)
            Text(pluralStringResource(R.plurals.done_body, chapters, file.name, chapters), style = MaterialTheme.typography.bodySmall)
            Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.save)) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpen, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.open)) }
                OutlinedButton(onClick = onShare, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.share)) }
            }
            TextButton(onClick = onStartOver) { Text(stringResource(R.string.start_over)) }
        }
    }
}

@Composable
private fun SettingsDialog(settings: AppSettings, cacheBytes: Long, onClearCache: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var delay by remember { mutableStateOf(settings.delaySeconds.toString()) }
    var userAgent by remember { mutableStateOf(settings.userAgent) }
    var notes by remember { mutableStateOf(settings.includeAuthorNotes) }
    var braveKey by remember { mutableStateOf(settings.braveApiKey) }
    var searxUrl by remember { mutableStateOf(settings.searxUrl) }
    var cacheCleared by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = delay,
                    onValueChange = { delay = it.filter { c -> c.isDigit() || c == '.' }.take(5) },
                    label = { Text(stringResource(R.string.settings_delay)) },
                    supportingText = { Text(stringResource(R.string.settings_delay_help)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = userAgent,
                    onValueChange = { userAgent = it },
                    label = { Text(stringResource(R.string.settings_user_agent)) },
                    maxLines = 3,
                )
                Text(stringResource(R.string.settings_search_title), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.settings_search_help), style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = braveKey,
                    onValueChange = { braveKey = it },
                    label = { Text(stringResource(R.string.settings_brave_key)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                OutlinedTextField(
                    value = searxUrl,
                    onValueChange = { searxUrl = it },
                    label = { Text(stringResource(R.string.settings_searx)) },
                    placeholder = { Text("https://searx.example.org") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.settings_author_notes), Modifier.weight(1f))
                    Switch(checked = notes, onCheckedChange = { notes = it })
                }
                OutlinedButton(onClick = { onClearCache(); cacheCleared = true }, enabled = !cacheCleared && cacheBytes > 0) {
                    Text(stringResource(R.string.settings_clear_cache, Formatter.formatShortFileSize(context, if (cacheCleared) 0 else cacheBytes)))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                settings.delaySeconds = delay.toFloatOrNull() ?: AppSettings.DEFAULT_DELAY_SECONDS
                settings.userAgent = userAgent
                settings.includeAuthorNotes = notes
                settings.braveApiKey = braveKey
                settings.searxUrl = searxUrl
                onDismiss()
            }) { Text(stringResource(R.string.close)) }
        },
    )
}

private fun epubUri(context: Context, file: File) =
    FileProvider.getUriForFile(context, "${context.packageName}.files", file)

private fun openEpub(context: Context, file: File): Boolean {
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(epubUri(context, file), "application/epub+zip")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    return try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}

private fun shareEpub(context: Context, file: File) {
    val intent = Intent(Intent.ACTION_SEND)
        .setType("application/epub+zip")
        .putExtra(Intent.EXTRA_STREAM, epubUri(context, file))
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(intent, file.name))
}
