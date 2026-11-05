package com.yukselis.protocol.ui.story

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.yukselis.engine.story.ChoiceView
import com.yukselis.engine.story.NoticeKind
import com.yukselis.engine.story.NoticeView
import com.yukselis.engine.story.PassageView
import com.yukselis.engine.story.SceneView
import com.yukselis.protocol.R
import com.yukselis.protocol.ui.components.SystemCard
import com.yukselis.protocol.ui.theme.GameType
import com.yukselis.protocol.ui.theme.InsightColor
import com.yukselis.protocol.ui.theme.EchoColor
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import kotlinx.coroutines.delay

/**
 * Hikaye sekmesi: geçmiş pasajlar soluk, güncel pasaj daktilo efektiyle yazılır.
 * Ekrana dokunmak yazmayı anında tamamlar. Metin bitince Sistem kartları ve seçenekler belirir.
 */
@Composable
fun StoryScreen(
    scene: SceneView,
    freshNotices: List<NoticeView>,
    turn: Int,
    typewriter: Boolean,
    haptics: Boolean,
    onChoose: (String) -> Unit,
    onBackToTitle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fullText = scene.current.paragraphs.joinToString("\n\n")
    val textLength by rememberUpdatedState(fullText.length)
    val passageKey = "${scene.current.sceneId}#$turn"

    var shown by remember(passageKey) { mutableIntStateOf(0) }
    var finished by remember(passageKey) { mutableStateOf(!typewriter) }

    LaunchedEffect(passageKey, finished) {
        if (!finished) {
            while (shown < textLength) {
                delay(CHAR_DELAY_MS)
                shown = (shown + CHARS_PER_TICK).coerceAtMost(textLength)
            }
            finished = true
        }
    }

    val hapticFeedback = LocalHapticFeedback.current
    val hasBigMoment = freshNotices.any { it.kind == NoticeKind.LEVEL_UP || it.kind == NoticeKind.TITLE }
    LaunchedEffect(passageKey) {
        if (haptics && hasBigMoment) hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    // Yeni pasaj başlarken onun başına, yazı bitince seçeneklere kaydır.
    // (Son öğeye kaydırmak listeyi en alta yaslar; LazyColumn sonun ötesine kaymaz.)
    val listState = rememberLazyListState()
    val startIndex = scene.history.size + if (scene.history.isNotEmpty()) 1 else 0
    val lastIndex = startIndex + freshNotices.size + if (finished) scene.current.notices.size + 1 else 1
    LaunchedEffect(passageKey, finished) {
        listState.animateScrollToItem(if (finished) lastIndex else startIndex)
    }

    val skip = remember { MutableInteractionSource() }
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxSize()
            .clickable(interactionSource = skip, indication = null) { finished = true },
    ) {
        itemsIndexed(scene.history, key = { i, p -> "h$i-${p.sceneId}" }) { _, passage ->
            PastPassage(passage)
        }

        if (scene.history.isNotEmpty()) {
            item(key = "divider-$passageKey") {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            }
        }

        // Son seçimin sonuçları (XP, seviye atlama, unvan) yeni pasajdan önce gösterilir.
        itemsIndexed(freshNotices, key = { i, _ -> "fresh-$passageKey-$i" }) { _, notice ->
            AppearingCard(notice)
        }

        item(key = "text-$passageKey") {
            val visible = if (finished) fullText else fullText.take(shown)
            Text(
                text = visible,
                style = GameType.narration,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        if (finished && scene.current.insights.isNotEmpty()) {
            item(key = "insights-$passageKey") { Insights(scene.current.insights) }
        }

        if (finished) {
            itemsIndexed(scene.current.notices, key = { i, _ -> "notice-$passageKey-$i" }) { _, notice ->
                AppearingCard(notice)
            }
            item(key = "choices-$passageKey") {
                if (scene.isEnding) {
                    EndingActions(onBackToTitle)
                } else {
                    Choices(scene.choices, onChoose)
                }
            }
        } else if (typewriter) {
            item(key = "skip-$passageKey") {
                Text(
                    text = stringResource(R.string.tap_to_skip),
                    style = GameType.system,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@Composable
private fun PastPassage(passage: PassageView) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        passage.paragraphs.forEach {
            Text(
                text = it,
                style = GameType.narrationPast,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            )
        }
        passage.chosenLabel?.let {
            Text(
                text = "› $it",
                style = GameType.narrationPast.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
            )
        }
    }
}

/** Pasif Sistem sezgisi: yalnızca oyuncunun gördüğü satırlar; tek aralıklı, renkli, sol çizgili. */
@Composable
private fun Insights(lines: List<String>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind { drawRect(InsightColor, size = Size(3.dp.toPx(), size.height)) }
            .padding(start = 12.dp),
    ) {
        lines.forEach {
            // "[ALG]" önekli satır stat sezgisidir; öneksiz satır Yankı'nın sesidir.
            Text(it, style = GameType.system.copy(fontStyle = FontStyle.Italic), color = if (it.startsWith("[")) InsightColor else EchoColor)
        }
    }
}

@Composable
private fun AppearingCard(notice: NoticeView) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 }),
    ) {
        SystemCard(notice)
    }
}

@Composable
private fun Choices(choices: List<ChoiceView>, onChoose: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
        choices.forEach { choice ->
            OutlinedButton(
                onClick = { onChoose(choice.id) },
                enabled = choice.enabled,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    choice.requirementTag?.let {
                        Text(
                            text = "$it ",
                            style = GameType.system,
                            color = if (choice.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(text = choice.label, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun EndingActions(onBackToTitle: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(
            text = stringResource(R.string.end_of_demo),
            style = GameType.systemTitle,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = onBackToTitle, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.back_to_title))
        }
    }
}

private const val CHAR_DELAY_MS = 16L
private const val CHARS_PER_TICK = 2
