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
import com.yukselis.engine.story.InsightView
import com.yukselis.engine.story.NoticeKind
import com.yukselis.engine.story.NoticeView
import com.yukselis.engine.story.PassageView
import com.yukselis.engine.story.SceneView
import com.yukselis.engine.world.HubAction
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
    onHub: (HubAction) -> Unit,
    onBackToTitle: () -> Unit,
    ending: com.yukselis.engine.story.EndingView? = null,
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
        // Yoldaş görüşleri: büyük seçimden önce yanındakilerin tek satırı.
        if (finished && scene.opinions.isNotEmpty()) {
            item(key = "opinions-$passageKey") { Opinions(scene.opinions) }
        }

        if (finished) {
            itemsIndexed(scene.current.notices, key = { i, _ -> "notice-$passageKey-$i" }) { _, notice ->
                AppearingCard(notice)
            }
            item(key = "choices-$passageKey") {
                val hub = scene.hub
                when {
                    scene.isEnding -> EndingActions(ending, onBackToTitle)
                    hub != null -> HubPanel(hub, onHub)
                    else -> Choices(scene.choices, onChoose)
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

/** Yoldaşların görüşü: ad + tek satır; Yankı'nınki kendi renginde. */
@Composable
private fun Opinions(lines: List<com.yukselis.engine.story.CompanionLineView>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind { drawRect(com.yukselis.protocol.ui.theme.CompanionColor, size = Size(3.dp.toPx(), size.height)) }
            .padding(start = 12.dp),
    ) {
        lines.forEach {
            val color = if (it.echo) EchoColor else com.yukselis.protocol.ui.theme.CompanionColor
            Text("${it.npcName}: ${it.text}", style = GameType.system, color = color)
        }
    }
}

/** Pasif Sistem sezgisi: yalnızca oyuncunun gördüğü satırlar; tek aralıklı, renkli, sol çizgili. */
@Composable
private fun Insights(lines: List<InsightView>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind { drawRect(InsightColor, size = Size(3.dp.toPx(), size.height)) }
            .padding(start = 12.dp),
    ) {
        lines.forEach {
            Text(it.text, style = GameType.system.copy(fontStyle = FontStyle.Italic), color = if (it.echo) EchoColor else InsightColor)
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
                    choice.kindTag?.let {
                        Text(text = "$it ", style = GameType.system, color = com.yukselis.protocol.ui.theme.EchoColor)
                    }
                    choice.requirementTag?.let {
                        Text(
                            text = "$it ",
                            style = GameType.system,
                            color = if (choice.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(text = choice.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    // Görünür olasılık: tek seferlik (kırmızı) / yeniden denenebilir (beyaz).
                    choice.odds?.let {
                        Text(
                            text = " %$it",
                            style = GameType.system,
                            color = if (choice.oneShot) androidx.compose.ui.graphics.Color(0xFFEF5350) else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EndingActions(ending: com.yukselis.engine.story.EndingView?, onBackToTitle: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (ending == null) {
            Text(text = stringResource(R.string.the_end), style = GameType.systemTitle, color = MaterialTheme.colorScheme.primary)
        } else {
            EndingCards(ending)
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onBackToTitle, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.back_to_title))
        }
    }
}

/** Epilog kartları: son, OP MC türü, NPC akıbetleri, fraksiyonlar, en kritik 5 seçim (Aşama 7). */
@Composable
private fun EndingCards(e: com.yukselis.engine.story.EndingView) {
    val gold = androidx.compose.ui.graphics.Color(0xFFFFC107)
    Text(text = stringResource(R.string.ending_label, e.name), style = GameType.systemTitle, color = gold)
    Text(text = e.summary, style = GameType.narration)
    Text(text = stringResource(R.string.ending_stats, e.level, e.grade, e.day), style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant)
    e.archetype?.let { name ->
        EpilogueSection(stringResource(R.string.ending_archetype))
        EpilogueCardView(name, e.archetypeText ?: "")
    }
    if (e.npcs.isNotEmpty()) {
        EpilogueSection(stringResource(R.string.ending_people))
        e.npcs.forEach { EpilogueCardView(it.title, it.text) }
    }
    if (e.factions.isNotEmpty()) {
        EpilogueSection(stringResource(R.string.ending_factions))
        e.factions.forEach { EpilogueCardView(it.title, it.text) }
    }
    if (e.choices.isNotEmpty()) {
        EpilogueSection(stringResource(R.string.ending_choices))
        e.choices.forEach { c ->
            EpilogueCardView(stringResource(R.string.ending_choice_day, c.day, c.event), listOfNotNull(c.choice, c.effect).joinToString("\n"))
        }
    }
    if (e.unlocksNewGamePlus) {
        Text(text = stringResource(R.string.ng_plus_unlocked), style = GameType.systemTitle, color = gold)
    }
}

@Composable
private fun EpilogueSection(title: String) {
    Text(text = title, style = GameType.systemTitle, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun EpilogueCardView(title: String, body: String) {
    androidx.compose.material3.Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = title, style = GameType.system, color = MaterialTheme.colorScheme.primary)
            if (body.isNotEmpty()) Text(text = body, style = GameType.narration)
        }
    }
}

private const val CHAR_DELAY_MS = 16L
private const val CHARS_PER_TICK = 2
