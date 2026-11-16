package com.upvotesaver.app.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.upvotesaver.app.Graph
import com.upvotesaver.app.MainViewModel
import com.upvotesaver.app.Message
import com.upvotesaver.app.R
import com.upvotesaver.app.UiState
import com.upvotesaver.core.PostRecord
import com.upvotesaver.core.Status

private const val REDDIT_APPS_URL = "https://www.reddit.com/prefs/apps"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel, state: UiState) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val messageText = state.message?.let { m -> if (m.arg != null) stringResource(m.resId, m.arg) else stringResource(m.resId) }
    LaunchedEffect(state.message) {
        if (messageText != null) {
            snackbar.showSnackbar(messageText)
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { LinkCard(viewModel, state) }
            if (!state.signedIn) {
                item { SetupCard(viewModel, state, onOpen = { url -> openUrl(context, url, viewModel) }) }
            } else {
                item { SyncCard(viewModel, state) }
                item { AutoCard(viewModel, state) }
            }
            item { Text(stringResource(R.string.recent_title), style = MaterialTheme.typography.titleMedium) }
            if (state.recent.isEmpty()) {
                item { Text(stringResource(R.string.recent_empty), style = MaterialTheme.typography.bodyMedium) }
            }
            items(state.recent, key = { it.id }) { record ->
                val url = if (record.permalink.startsWith("http")) record.permalink else "https://www.reddit.com${record.permalink}"
                PostRow(record, onClick = { openUrl(context, url, viewModel) })
            }
        }
    }
}

private fun openUrl(context: Context, url: String, viewModel: MainViewModel) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        viewModel.show(Message(R.string.error_no_browser))
    }
}

/** Paste a Redgifs link (or share one to the app) to save the clip with its sound. Needs no account. */
@Composable
private fun LinkCard(viewModel: MainViewModel, state: UiState) {
    var text by rememberSaveable { mutableStateOf("") }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.links_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.links_hint), style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.links_field)) },
                maxLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = { if (viewModel.saveLinks(text)) text = "" }, enabled = text.isNotBlank()) {
                Text(stringResource(R.string.links_download))
            }
            if (state.linksRunning) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SetupCard(viewModel: MainViewModel, state: UiState, onOpen: (String) -> Unit) {
    val context = LocalContext.current
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.setup_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.setup_approval), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.setup_steps), style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = { onOpen(REDDIT_APPS_URL) }) { Text(stringResource(R.string.open_reddit_apps)) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.redirect_uri), style = MaterialTheme.typography.labelMedium)
                    Text(Graph.REDIRECT_URI, fontFamily = FontFamily.Monospace)
                }
                TextButton(onClick = {
                    context.getSystemService(ClipboardManager::class.java)
                        .setPrimaryClip(ClipData.newPlainText("redirect uri", Graph.REDIRECT_URI))
                    // Android 13+ shows its own "copied" confirmation.
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) viewModel.show(Message(R.string.copied))
                }) { Text(stringResource(R.string.copy)) }
            }
            OutlinedTextField(
                value = state.clientId,
                onValueChange = viewModel::onClientIdChange,
                label = { Text(stringResource(R.string.client_id)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { onOpen(viewModel.signInUrl()) },
                enabled = state.clientId.isNotBlank() && !state.signingIn,
            ) { Text(stringResource(if (state.signingIn) R.string.signing_in else R.string.sign_in)) }
        }
    }
}

@Composable
private fun SyncCard(viewModel: MainViewModel, state: UiState) {
    // Android 13+: ask for the notification permission so a long run can show its progress notification.
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    fun start(action: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        action()
    }
    val pickCsv = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) start { viewModel.importCsv(uri) }
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.signed_in_as, state.username), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = viewModel::signOut) { Text(stringResource(R.string.sign_out)) }
            }
            if (state.running) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                val p = state.progress
                Text(
                    if (p == null) stringResource(R.string.waiting_network)
                    else stringResource(R.string.running, p.scanned, p.saved, p.failed),
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedButton(onClick = viewModel::cancel) { Text(stringResource(R.string.cancel)) }
            } else {
                Button(onClick = { start(viewModel::syncNow) }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.sync_now)) }
                OutlinedButton(onClick = { start(viewModel::scanAll) }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.scan_all)) }
                OutlinedButton(
                    // Data exports label the file text/csv, but some file managers report other types.
                    onClick = { pickCsv.launch(arrayOf("text/*", "application/csv", "application/octet-stream")) },
                    Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.import_csv)) }
                Text(stringResource(R.string.scan_all_hint), style = MaterialTheme.typography.bodySmall)
            }
            Text(
                stringResource(
                    R.string.stats,
                    state.counts[Status.DONE] ?: 0, state.counts[Status.FAILED] ?: 0, state.counts[Status.SKIPPED] ?: 0,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            val last = if (state.lastSyncMs == 0L) stringResource(R.string.never)
            else DateUtils.getRelativeTimeSpanString(state.lastSyncMs, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
            Text(stringResource(R.string.last_sync, last), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.files_location), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun AutoCard(viewModel: MainViewModel, state: UiState) {
    val options = listOf(
        0 to R.string.interval_off, 15 to R.string.interval_15m, 60 to R.string.interval_1h,
        360 to R.string.interval_6h, 1440 to R.string.interval_24h,
    )
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.auto_title), style = MaterialTheme.typography.titleMedium)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { (minutes, label) ->
                    FilterChip(
                        selected = state.intervalMinutes == minutes,
                        onClick = { viewModel.setInterval(minutes) },
                        label = { Text(stringResource(label)) },
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.wifi_only), Modifier.weight(1f))
                Switch(checked = state.wifiOnly, onCheckedChange = viewModel::setWifiOnly, enabled = state.intervalMinutes > 0)
            }
            Text(stringResource(R.string.auto_hint), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PostRow(record: PostRecord, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(enabled = record.permalink.isNotEmpty(), onClick = onClick).padding(vertical = 4.dp)) {
        Text(record.title.ifEmpty { record.id }, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Row {
            if (record.subreddit.isNotEmpty()) {
                Text("r/${record.subreddit}", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(8.dp))
            }
            val status = when (record.status) {
                Status.DONE -> stringResource(R.string.status_done, record.files)
                Status.FAILED -> stringResource(R.string.status_failed)
                Status.SKIPPED -> stringResource(R.string.status_skipped)
            }
            Text(
                if (record.note.isNotEmpty() && record.status != Status.SKIPPED) "$status · ${record.note}" else status,
                style = MaterialTheme.typography.bodySmall,
                color = if (record.status == Status.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
