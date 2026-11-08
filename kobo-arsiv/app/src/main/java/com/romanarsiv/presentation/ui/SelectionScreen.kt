package com.romanarsiv.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romanarsiv.R
import com.romanarsiv.presentation.SelectionState

/** Seçim ekranının eylemleri; ViewModel fonksiyonlarına bağlanır. */
class SelectionActions(
    val onToggle: (Int) -> Unit,
    val onSelectAll: () -> Unit,
    val onSelectNone: () -> Unit,
    val onInvert: () -> Unit,
    val onSelectRange: (Int, Int) -> Unit,
    val onBookTitleChange: (String) -> Unit,
    val onFileNameChange: (String) -> Unit,
    val onResetFileName: () -> Unit,
)

@Composable
fun SelectionContent(state: SelectionState, authorName: String, actions: SelectionActions, modifier: Modifier = Modifier) {
    val keyboard = LocalSoftwareKeyboardController.current
    val total = state.chapters.size
    var rangeFrom by rememberSaveable(state.source.url) { mutableStateOf("1") }
    var rangeTo by rememberSaveable(state.source.url) { mutableStateOf(total.toString()) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
    ) {
        item(key = "fields") {
            ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.bookTitle,
                    onValueChange = actions.onBookTitleChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.confirm_book_title)) },
                    isError = state.bookTitle.isBlank(),
                )
                OutlinedTextField(
                    value = state.fileName,
                    onValueChange = actions.onFileNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.select_file_name)) },
                    supportingText = { Text(stringResource(R.string.select_file_hint)) },
                    trailingIcon = if (state.fileNameEdited) {
                        { TextButton(onClick = actions.onResetFileName) { Text(stringResource(R.string.select_file_reset)) } }
                    } else {
                        null
                    },
                )
                Text(
                    stringResource(R.string.confirm_author, authorName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            }
        }

        item(key = "range") {
            Column(Modifier.padding(top = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val numberKeyboard = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
                    OutlinedTextField(
                        value = rangeFrom,
                        onValueChange = { rangeFrom = it.filter(Char::isDigit).take(5) },
                        modifier = Modifier.width(88.dp),
                        singleLine = true,
                        label = { Text(stringResource(R.string.select_range_from)) },
                        keyboardOptions = numberKeyboard,
                    )
                    Text("–")
                    OutlinedTextField(
                        value = rangeTo,
                        onValueChange = { rangeTo = it.filter(Char::isDigit).take(5) },
                        modifier = Modifier.width(88.dp),
                        singleLine = true,
                        label = { Text(stringResource(R.string.select_range_to)) },
                        keyboardOptions = numberKeyboard,
                    )
                    OutlinedButton(
                        onClick = {
                            keyboard?.hide()
                            actions.onSelectRange(rangeFrom.toIntOrNull() ?: 1, rangeTo.toIntOrNull() ?: total)
                        },
                        enabled = total > 0,
                    ) { Text(stringResource(R.string.select_range_apply)) }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    AssistChip(onClick = actions.onSelectAll, label = { Text(stringResource(R.string.select_all)) })
                    AssistChip(onClick = actions.onSelectNone, label = { Text(stringResource(R.string.select_none)) })
                    AssistChip(onClick = actions.onInvert, label = { Text(stringResource(R.string.select_invert)) })
                }
                Text(
                    stringResource(R.string.select_count, state.selectedCount, total),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
                HorizontalDivider()
            }
        }

        itemsIndexed(state.chapters, key = { i, _ -> i }) { i, chapter ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { actions.onToggle(i) }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = i in state.selected, onCheckedChange = { actions.onToggle(i) })
                Text(
                    "${i + 1}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(44.dp),
                )
                Text(chapter.title, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun SelectionBottomBar(state: SelectionState, onDownload: () -> Unit) {
    Surface(tonalElevation = 3.dp) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Button(
                onClick = onDownload,
                enabled = state.canDownload,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                androidx.compose.material3.Icon(AppIcons.Download, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(pluralStringResource(R.plurals.select_download, state.selectedCount, state.selectedCount))
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}
