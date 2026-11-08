package com.romanarsiv.presentation.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romanarsiv.R
import com.romanarsiv.domain.model.ArchiveProgress
import com.romanarsiv.domain.model.NovelSource
import com.romanarsiv.background.DetectState
import com.romanarsiv.background.DownloadState
import com.romanarsiv.background.UiMessage
import com.romanarsiv.presentation.MainViewModel
import com.romanarsiv.presentation.SearchState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel, initialCrashReport: String? = null) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val detectState by viewModel.detectState.collectAsStateWithLifecycle()
    val selection by viewModel.selection.collectAsStateWithLifecycle()
    val challengeUrl by viewModel.challengeUrl.collectAsStateWithLifecycle()
    var crashReport by rememberSaveable { mutableStateOf(initialCrashReport) }

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current

    // Android 13+: arka plan ilerleme/sonuç bildirimleri için izin; ilk uzun işte bir kez sorulur.
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    var askedNotifications by rememberSaveable { mutableStateOf(false) }
    val ensureNotificationPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !askedNotifications &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            askedNotifications = true
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val moveToBackground = { context.findActivity()?.moveTaskToBack(true); Unit }
    var showHidden by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            val text = when (message) {
                is UiMessage.Saved -> if (message.result.skippedChapters > 0) {
                    context.getString(
                        R.string.saved_success_skipped,
                        message.result.fileName,
                        message.result.chapterCount,
                        message.result.skippedChapters,
                    )
                } else {
                    context.getString(R.string.saved_success, message.result.fileName, message.result.chapterCount)
                }
                is UiMessage.Failed -> context.getString(R.string.error_prefix, message.message)
            }
            snackbarHostState.showSnackbar(text, withDismissAction = true, duration = SnackbarDuration.Long)
        }
    }

    val currentSelection = selection
    BackHandler(enabled = currentSelection != null) { viewModel.closeSelection() }

    Scaffold(
        topBar = {
            if (currentSelection != null) {
                TopAppBar(
                    title = { Text(stringResource(R.string.select_title)) },
                    navigationIcon = {
                        IconButton(onClick = viewModel::closeSelection) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                )
            } else {
                TopAppBar(title = { Text(stringResource(R.string.app_name)) })
            }
        },
        bottomBar = {
            if (currentSelection != null) {
                SelectionBottomBar(currentSelection, onDownload = {
                    ensureNotificationPermission()
                    viewModel.startDownload()
                })
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (currentSelection != null) {
            SelectionContent(
                state = currentSelection,
                actions = remember(viewModel) {
                    SelectionActions(
                        onToggle = viewModel::toggleChapter,
                        onSelectAll = viewModel::selectAll,
                        onSelectNone = viewModel::selectNone,
                        onInvert = viewModel::invertSelection,
                        onSelectRange = viewModel::selectRange,
                        onBookTitleChange = viewModel::onBookTitleChange,
                        onFileNameChange = viewModel::onFileNameChange,
                        onResetFileName = viewModel::resetFileName,
                    )
                },
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            val doSearch = {
                keyboard?.hide()
                viewModel.search()
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = viewModel::onQueryChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text(stringResource(R.string.search_hint)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { doSearch() }),
                )
                Button(
                    onClick = doSearch,
                    enabled = query.isNotBlank() && searchState !is SearchState.Loading,
                ) {
                    Text(stringResource(R.string.search_button))
                }
            }
            Spacer(Modifier.height(12.dp))

            when (val s = searchState) {
                SearchState.Idle -> Hint(stringResource(R.string.search_intro))
                SearchState.Loading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.searching))
                    }
                }
                is SearchState.Error -> Hint(stringResource(R.string.error_prefix, s.message), isError = true)
                is SearchState.Success ->
                    if (s.results.isEmpty() && s.hidden.isEmpty()) {
                        Hint(stringResource(R.string.search_empty))
                    } else {
                        SourceList(
                            sources = s.results,
                            hidden = s.hidden,
                            showHidden = showHidden,
                            onToggleHidden = { showHidden = !showHidden },
                            onClick = { source ->
                                ensureNotificationPermission()
                                viewModel.selectSource(source)
                            },
                        )
                    }
            }
        }
    }

    (detectState as? DetectState.Loading)?.let { loading ->
        DetectDialog(loading, onCancel = viewModel::cancelDetection, onBackground = moveToBackground)
    }

    (downloadState as? DownloadState.Loading)?.let { loading ->
        ProgressDialog(loading, onCancel = viewModel::cancelDownload, onBackground = moveToBackground)
    }

    // İlerleme penceresinin üstünde açılır.
    challengeUrl?.let { url -> ChallengeDialog(url, onResult = viewModel::onChallengeResult) }

    crashReport?.let { report -> CrashReportDialog(report, onDismiss = { crashReport = null }) }
}

@Composable
private fun Hint(text: String, isError: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 16.dp),
    )
}

@Composable
private fun SourceList(
    sources: List<NovelSource>,
    hidden: List<NovelSource>,
    showHidden: Boolean,
    onToggleHidden: () -> Unit,
    onClick: (NovelSource) -> Unit,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        if (sources.isEmpty()) {
            item(key = "empty") { Hint(stringResource(R.string.search_empty_novel)) }
        }
        items(sources, key = { "n:" + it.url }) { source -> SourceCard(source, dimmed = false, onClick = onClick) }
        if (hidden.isNotEmpty()) {
            item(key = "hidden-toggle") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.hidden_results, hidden.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onToggleHidden) {
                        Text(stringResource(if (showHidden) R.string.hidden_hide else R.string.hidden_show))
                    }
                }
            }
            if (showHidden) {
                item(key = "hidden-header") {
                    Text(stringResource(R.string.hidden_header), style = MaterialTheme.typography.labelLarge)
                }
                items(hidden, key = { "h:" + it.url }) { source -> SourceCard(source, dimmed = true, onClick = onClick) }
            }
        }
    }
}

@Composable
private fun SourceCard(source: NovelSource, dimmed: Boolean, onClick: (NovelSource) -> Unit) {
    Card(
        onClick = { onClick(source) },
        modifier = Modifier.fillMaxWidth().alpha(if (dimmed) 0.6f else 1f),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(source.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(source.siteName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            if (source.snippet.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(source.snippet, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Text(
                source.url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun BackgroundHint() {
    Spacer(Modifier.height(12.dp))
    Text(
        stringResource(R.string.background_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun DetectDialog(state: DetectState.Loading, onCancel: () -> Unit, onBackground: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text(stringResource(R.string.detect_title)) },
        text = {
            Column {
                Text(state.source.siteName, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(12.dp))
                Text(
                    if (state.found > 0) {
                        stringResource(R.string.detect_found, state.found)
                    } else {
                        stringResource(R.string.progress_detecting)
                    },
                )
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                BackgroundHint()
            }
        },
        confirmButton = { TextButton(onClick = onBackground) { Text(stringResource(R.string.background_button)) } },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun ProgressDialog(state: DownloadState.Loading, onCancel: () -> Unit, onBackground: () -> Unit) {
    val progress = state.progress
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text(stringResource(R.string.progress_title)) },
        text = {
            Column {
                Text(state.source.siteName, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(12.dp))
                val line = when (progress) {
                    ArchiveProgress.DetectingChapters -> stringResource(R.string.progress_detecting)
                    is ArchiveProgress.FetchingChapter ->
                        if (progress.total > 0) {
                            stringResource(R.string.progress_chapter, progress.index, progress.total)
                        } else {
                            stringResource(R.string.progress_chapter_open, progress.index)
                        }
                    ArchiveProgress.BuildingEpub -> stringResource(R.string.progress_building)
                    ArchiveProgress.Saving -> stringResource(R.string.progress_saving)
                }
                Text(line)
                if (progress is ArchiveProgress.FetchingChapter) {
                    Text(progress.title, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(12.dp))
                if (progress is ArchiveProgress.FetchingChapter && progress.total > 0) {
                    LinearProgressIndicator(
                        progress = { progress.index.toFloat() / progress.total },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                BackgroundHint()
            }
        },
        confirmButton = { TextButton(onClick = onBackground) { Text(stringResource(R.string.background_button)) } },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) } },
    )
}
