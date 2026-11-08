package com.romanarsiv.presentation.ui

import android.text.format.DateUtils
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romanarsiv.R
import com.romanarsiv.background.ArchiveNotifications
import com.romanarsiv.background.DownloadStatus
import com.romanarsiv.background.DownloadTask
import com.romanarsiv.background.describeError

/**
 * İndirilenler: süren ve sıradaki indirmeler üstte, bitenler altta. Birden fazla indirme aynı anda
 * sürer; her satır kendi ilerlemesini ve düğmelerini gösterir (açılır pencere yok).
 */
@Composable
fun DownloadsTab(
    tasks: List<DownloadTask>,
    onCancel: (Long) -> Unit,
    onRetry: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    onMessage: (String) -> Unit,
    onGoSearch: () -> Unit,
) {
    if (tasks.isEmpty()) {
        EmptyState(
            icon = AppIcons.DownloadOutlined,
            title = stringResource(R.string.downloads_empty_title),
            text = stringResource(R.string.downloads_empty),
            action = { Button(onClick = onGoSearch) { Text(stringResource(R.string.downloads_go_search)) } },
        )
        return
    }
    val running = tasks.filter { it.status == DownloadStatus.RUNNING }.sortedBy { it.id }
    val queued = tasks.filter { it.status == DownloadStatus.QUEUED }.sortedBy { it.id }
    val finished = tasks.filter { !it.status.isActive }.sortedByDescending { it.finishedAt ?: it.id }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (running.isNotEmpty() || queued.isNotEmpty()) {
            item(key = "h-active") { SectionHeader(stringResource(R.string.downloads_active, running.size + queued.size)) }
            items(running + queued, key = { it.id }) { task -> ActiveCard(task, onCancel = { onCancel(task.id) }) }
        }
        if (finished.isNotEmpty()) {
            item(key = "h-done") { SectionHeader(stringResource(R.string.downloads_finished)) }
            items(finished, key = { it.id }) { task ->
                FinishedCard(task, onRetry = { onRetry(task.id) }, onRemove = { onRemove(task.id) }, onMessage = onMessage)
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun StatusBadge(icon: ImageVector, container: Color, content: Color) {
    Box(
        modifier = Modifier.size(44.dp).background(container, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun ActiveCard(task: DownloadTask, onCancel: () -> Unit) {
    val context = LocalContext.current
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 14.dp, top = 14.dp, bottom = 14.dp, end = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusBadge(AppIcons.Download, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(task.bookTitle, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        pluralStringResource(R.plurals.downloads_meta, task.requestedChapters, task.siteName, task.requestedChapters),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onCancel) { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cancel)) }
            }
            Spacer(Modifier.height(10.dp))
            Column(Modifier.padding(end = 10.dp)) {
                val fraction = task.fraction
                if (task.status == DownloadStatus.QUEUED) {
                    LinearProgressIndicator(progress = { 0f }, modifier = Modifier.fillMaxWidth())
                } else if (fraction != null) {
                    LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                } else {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(6.dp))
                Row {
                    Text(
                        if (task.status == DownloadStatus.QUEUED) stringResource(R.string.status_queued)
                        else ArchiveNotifications.progressLine(context, task.progress),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1f),
                    )
                    if (fraction != null && task.status == DownloadStatus.RUNNING) {
                        Text("${(fraction * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                    }
                }
                val p = task.progress
                if (task.status == DownloadStatus.RUNNING && p is com.romanarsiv.domain.model.ArchiveProgress.FetchingChapter && p.title.isNotBlank()) {
                    Text(
                        p.title,
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
private fun FinishedCard(task: DownloadTask, onRetry: () -> Unit, onRemove: () -> Unit, onMessage: (String) -> Unit) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val result = task.result
    val uri = result?.uri

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 14.dp, top = 14.dp, bottom = 10.dp, end = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (task.status) {
                    DownloadStatus.DONE -> StatusBadge(Icons.Filled.CheckCircle, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
                    DownloadStatus.FAILED -> StatusBadge(Icons.Filled.Warning, MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
                    else -> StatusBadge(Icons.Filled.Close, MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(task.bookTitle, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val line = when (task.status) {
                        DownloadStatus.DONE -> result?.let {
                            if (it.skippedChapters > 0) stringResource(R.string.downloads_done_skipped, it.chapterCount, it.skippedChapters)
                            else pluralStringResource(R.plurals.downloads_done, it.chapterCount, it.chapterCount)
                        }.orEmpty()
                        DownloadStatus.FAILED -> stringResource(R.string.status_failed)
                        else -> stringResource(R.string.status_cancelled)
                    }
                    val time = task.finishedAt?.let {
                        DateUtils.getRelativeTimeSpanString(it, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
                    }
                    Text(
                        listOfNotNull(line, time).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (task.status == DownloadStatus.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more)) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.downloads_remove)) },
                            leadingIcon = { Icon(Icons.Filled.Close, contentDescription = null) },
                            onClick = { menu = false; onRemove() },
                        )
                        if (uri != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.downloads_delete_file)) },
                                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                                onClick = { menu = false; confirmDelete = true },
                            )
                        }
                    }
                }
            }

            if (task.status == DownloadStatus.DONE && result != null) {
                Text(
                    result.location,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp, end = 10.dp),
                )
            }
            if (task.status == DownloadStatus.FAILED && task.error != null) {
                Text(
                    describeError(context, task.error),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp, end = 10.dp),
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 10.dp, end = 10.dp),
            ) {
                if (task.status == DownloadStatus.DONE && uri != null) {
                    FilledTonalButton(onClick = {
                        if (!FileActions.open(context, uri)) onMessage(context.getString(R.string.no_reader))
                    }) {
                        Icon(AppIcons.Book, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.action_open))
                    }
                    OutlinedButton(onClick = { FileActions.share(context, uri, task.bookTitle) }) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.action_share))
                    }
                }
                if (task.canRetry) {
                    FilledTonalButton(onClick = onRetry) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.action_retry))
                    }
                }
            }
        }
    }

    if (confirmDelete && uri != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_title)) },
            text = { Text(stringResource(R.string.delete_text, result?.fileName.orEmpty())) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    if (FileActions.delete(context, uri)) {
                        onRemove()
                        onMessage(context.getString(R.string.delete_done))
                    } else {
                        onMessage(context.getString(R.string.delete_failed))
                    }
                }) { Text(stringResource(R.string.delete_confirm)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
