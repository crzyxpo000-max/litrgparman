package com.romanarsiv.presentation.ui

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romanarsiv.R
import com.romanarsiv.domain.AppConfig
import com.romanarsiv.domain.model.ArchiveProgress
import com.romanarsiv.domain.model.NovelSource
import com.romanarsiv.presentation.DownloadState
import com.romanarsiv.presentation.MainViewModel
import com.romanarsiv.presentation.SearchState
import com.romanarsiv.presentation.UiMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val pending by viewModel.pendingSource.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            val text = when (message) {
                is UiMessage.Saved -> context.getString(
                    R.string.saved_success,
                    message.result.fileName,
                    message.result.chapterCount,
                )
                is UiMessage.Failed -> context.getString(R.string.error_prefix, message.message)
            }
            snackbarHostState.showSnackbar(text, withDismissAction = true, duration = SnackbarDuration.Long)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
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
                    if (s.results.isEmpty()) {
                        Hint(stringResource(R.string.search_empty))
                    } else {
                        SourceList(s.results, onClick = viewModel::selectSource)
                    }
            }
        }
    }

    pending?.let { source ->
        ConfirmDialog(
            source = source,
            initialTitle = viewModel.suggestedTitle(source),
            onConfirm = viewModel::startDownload,
            onDismiss = viewModel::dismissPending,
        )
    }

    (downloadState as? DownloadState.Loading)?.let { loading ->
        ProgressDialog(loading, onCancel = viewModel::cancelDownload)
    }
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
private fun SourceList(sources: List<NovelSource>, onClick: (NovelSource) -> Unit) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        items(sources, key = { it.url }) { source ->
            Card(onClick = { onClick(source) }, modifier = Modifier.fillMaxWidth()) {
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
    }
}

@Composable
private fun ConfirmDialog(
    source: NovelSource,
    initialTitle: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by rememberSaveable(source.url) { mutableStateOf(initialTitle) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.confirm_title)) },
        text = {
            Column {
                Text(source.siteName, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.confirm_book_title)) },
                )
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.confirm_author, AppConfig.AUTHOR_NAME), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(title) }, enabled = title.isNotBlank()) {
                Text(stringResource(R.string.confirm_start))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun ProgressDialog(state: DownloadState.Loading, onCancel: () -> Unit) {
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
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) } },
    )
}
