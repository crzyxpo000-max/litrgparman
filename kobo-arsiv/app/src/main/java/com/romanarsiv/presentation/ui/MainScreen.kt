package com.romanarsiv.presentation.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romanarsiv.R
import com.romanarsiv.background.DetectState
import com.romanarsiv.background.UiMessage
import com.romanarsiv.background.describeError
import com.romanarsiv.domain.model.NovelSource
import com.romanarsiv.presentation.MainViewModel
import com.romanarsiv.presentation.SearchState
import com.romanarsiv.presentation.Tab
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel, initialCrashReport: String? = null, onLanguageChanged: () -> Unit = {}) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val detectState by viewModel.detectState.collectAsStateWithLifecycle()
    val selection by viewModel.selection.collectAsStateWithLifecycle()
    val challengeUrl by viewModel.challengeUrl.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val tab by viewModel.tab.collectAsStateWithLifecycle()
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    var crashReport by rememberSaveable { mutableStateOf(initialCrashReport) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val showMessage: (String) -> Unit = { text -> scope.launch { snackbarHostState.showSnackbar(text, withDismissAction = true) } }

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

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            when (message) {
                is UiMessage.Saved -> {
                    val r = message.result
                    val text = if (r.skippedChapters > 0) {
                        context.getString(R.string.saved_success_skipped, r.fileName, r.chapterCount, r.skippedChapters)
                    } else {
                        context.getString(R.string.saved_success, r.fileName, r.chapterCount)
                    }
                    val uri = r.uri
                    val outcome = snackbarHostState.showSnackbar(
                        text,
                        actionLabel = if (uri != null) context.getString(R.string.action_open) else null,
                        withDismissAction = true,
                        duration = SnackbarDuration.Long,
                    )
                    if (outcome == SnackbarResult.ActionPerformed && uri != null && !FileActions.open(context, uri)) {
                        snackbarHostState.showSnackbar(context.getString(R.string.no_reader), withDismissAction = true)
                    }
                }
                is UiMessage.Failed -> snackbarHostState.showSnackbar(
                    context.getString(R.string.error_prefix, describeError(context, message.error)),
                    withDismissAction = true,
                    duration = SnackbarDuration.Long,
                )
            }
        }
    }

    val currentSelection = selection
    BackHandler(enabled = currentSelection != null || tab != Tab.SEARCH) {
        if (currentSelection != null) viewModel.closeSelection() else viewModel.selectTab(Tab.SEARCH)
    }

    val activeCount = tasks.count { it.status.isActive }

    Scaffold(
        topBar = {
            when {
                currentSelection != null -> TopAppBar(
                    title = {
                        Column {
                            Text(stringResource(R.string.select_title))
                            Text(
                                currentSelection.source.siteName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = viewModel::closeSelection) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                )
                else -> TopAppBar(
                    title = {
                        Text(
                            stringResource(
                                when (tab) {
                                    Tab.SEARCH -> R.string.app_name
                                    Tab.DOWNLOADS -> R.string.tab_downloads
                                    Tab.SETTINGS -> R.string.tab_settings
                                },
                            ),
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    actions = {
                        if (tab == Tab.DOWNLOADS && tasks.any { !it.status.isActive }) {
                            TextButton(onClick = viewModel::clearFinished) { Text(stringResource(R.string.downloads_clear)) }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                )
            }
        },
        bottomBar = {
            if (currentSelection != null) {
                SelectionBottomBar(currentSelection, onDownload = {
                    ensureNotificationPermission()
                    if (viewModel.startDownload()) {
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                context.getString(R.string.download_queued, currentSelection.bookTitle),
                                actionLabel = context.getString(R.string.action_show),
                                withDismissAction = true,
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.closeSelection()
                                viewModel.selectTab(Tab.DOWNLOADS)
                            }
                        }
                    }
                })
            } else {
                AppNavigationBar(tab, activeCount, onSelect = viewModel::selectTab)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (currentSelection != null) {
            SelectionContent(
                state = currentSelection,
                authorName = prefs.authorName,
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
        Crossfade(targetState = tab, label = "tab", modifier = Modifier.padding(padding)) { current ->
            when (current) {
                Tab.SEARCH -> SearchTab(
                    query = query,
                    searchState = searchState,
                    detectState = detectState,
                    onQueryChange = viewModel::onQueryChange,
                    onSearch = viewModel::search,
                    onSelectSource = { source ->
                        ensureNotificationPermission()
                        viewModel.selectSource(source)
                    },
                    onCancelDetection = viewModel::cancelDetection,
                )
                Tab.DOWNLOADS -> DownloadsTab(
                    tasks = tasks,
                    onCancel = viewModel::cancelDownload,
                    onRetry = { id ->
                        ensureNotificationPermission()
                        viewModel.retryDownload(id)
                    },
                    onRemove = viewModel::removeDownload,
                    onMessage = showMessage,
                    onGoSearch = { viewModel.selectTab(Tab.SEARCH) },
                )
                Tab.SETTINGS -> SettingsTab(
                    prefs = prefs,
                    onChange = viewModel::updatePrefs,
                    onLanguageChanged = onLanguageChanged,
                )
            }
        }
    }

    // Site doğrulaması gerçek bir etkileşim ister; tam ekran açılır.
    challengeUrl?.let { url -> ChallengeDialog(url, onResult = viewModel::onChallengeResult) }

    crashReport?.let { report -> CrashReportDialog(report, onDismiss = { crashReport = null }) }
}

@Composable
private fun AppNavigationBar(tab: Tab, activeDownloads: Int, onSelect: (Tab) -> Unit) {
    NavigationBar {
        NavItem(tab == Tab.SEARCH, Icons.Filled.Search, R.string.tab_search) { onSelect(Tab.SEARCH) }
        NavigationBarItem(
            selected = tab == Tab.DOWNLOADS,
            onClick = { onSelect(Tab.DOWNLOADS) },
            icon = {
                BadgedBox(badge = { if (activeDownloads > 0) Badge { Text(activeDownloads.toString()) } }) {
                    Icon(AppIcons.Download, contentDescription = null)
                }
            },
            label = { Text(stringResource(R.string.tab_downloads)) },
        )
        NavItem(tab == Tab.SETTINGS, Icons.Filled.Settings, R.string.tab_settings) { onSelect(Tab.SETTINGS) }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.NavItem(selected: Boolean, icon: ImageVector, label: Int, onClick: () -> Unit) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(stringResource(label)) },
    )
}

// ---------------------------------------------------------------------- arama sekmesi

@Composable
private fun SearchTab(
    query: String,
    searchState: SearchState,
    detectState: DetectState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onSelectSource: (NovelSource) -> Unit,
    onCancelDetection: () -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    var showHidden by rememberSaveable { mutableStateOf(false) }
    val doSearch = {
        keyboard?.hide()
        onSearch()
    }
    val detecting = detectState as? DetectState.Loading

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            placeholder = { Text(stringResource(R.string.search_hint), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.clear))
                        }
                        FilledIconButton(
                            onClick = doSearch,
                            enabled = searchState !is SearchState.Loading,
                            modifier = Modifier.padding(end = 6.dp),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.search_button))
                        }
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { doSearch() }),
        )

        AnimatedVisibility(visible = detecting != null) {
            if (detecting != null) DetectCard(detecting, onCancel = onCancelDetection)
        }
        Spacer(Modifier.height(12.dp))

        when (val s = searchState) {
            SearchState.Idle -> EmptyState(
                icon = AppIcons.Book,
                title = stringResource(R.string.search_intro_title),
                text = stringResource(R.string.search_intro),
            )
            SearchState.Loading -> Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.searching), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            is SearchState.Error -> ErrorCard(describeError(context, s.error))
            is SearchState.Success ->
                if (s.results.isEmpty() && s.hidden.isEmpty()) {
                    EmptyState(icon = Icons.Filled.Search, title = stringResource(R.string.search_empty_title), text = stringResource(R.string.search_empty))
                } else {
                    SourceList(
                        sources = s.results,
                        hidden = s.hidden,
                        showHidden = showHidden,
                        enabled = detecting == null,
                        onToggleHidden = { showHidden = !showHidden },
                        onClick = onSelectSource,
                    )
                }
        }
    }
}

/** Ekranı kilitlemeyen tespit kartı: arama ve İndirilenler kullanılabilir kalır. */
@Composable
private fun DetectCard(state: DetectState.Loading, onCancel: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.detect_title), style = MaterialTheme.typography.titleSmall)
                    Text(
                        if (state.found > 0) {
                            pluralStringResource(R.plurals.detect_found_site, state.found, state.source.siteName, state.found)
                        } else {
                            state.source.siteName
                        },
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(end = 8.dp))
        }
    }
}

@Composable
private fun SourceList(
    sources: List<NovelSource>,
    hidden: List<NovelSource>,
    showHidden: Boolean,
    enabled: Boolean,
    onToggleHidden: () -> Unit,
    onClick: (NovelSource) -> Unit,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        if (sources.isEmpty()) {
            item(key = "empty") {
                Text(
                    stringResource(R.string.search_empty_novel),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        } else {
            item(key = "count") {
                Text(
                    stringResource(R.string.search_result_count, sources.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(sources, key = { "n:" + it.url }) { source -> SourceCard(source, dimmed = false, enabled = enabled, onClick = onClick) }
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
                items(hidden, key = { "h:" + it.url }) { source -> SourceCard(source, dimmed = true, enabled = enabled, onClick = onClick) }
            }
        }
    }
}

@Composable
private fun SourceCard(source: NovelSource, dimmed: Boolean, enabled: Boolean, onClick: (NovelSource) -> Unit) {
    ElevatedCard(
        onClick = { onClick(source) },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().alpha(if (dimmed) 0.6f else 1f),
    ) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            SiteAvatar(source.siteName)
            Column(Modifier.weight(1f)) {
                Text(source.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    source.siteName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (source.snippet.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        source.snippet,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Sitenin baş harfi, renkli bir daire içinde. */
@Composable
private fun SiteAvatar(siteName: String) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            siteName.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "?",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

// ---------------------------------------------------------------------- ortak parçalar

@Composable
fun EmptyState(icon: ImageVector, title: String, text: String, action: (@Composable () -> Unit)? = null) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(72.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Spacer(Modifier.height(16.dp))
            action()
        }
    }
}

@Composable
fun ErrorCard(message: String) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
