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
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.saveable.rememberSaveable
import com.yukselis.protocol.ui.components.Glossary
import com.yukselis.protocol.ui.components.HelpButton
import com.yukselis.protocol.ui.components.LocalTips
import com.yukselis.protocol.ui.components.Localized
import com.yukselis.protocol.ui.components.TipCard
import kotlinx.coroutines.delay

/**
 * Hikaye sekmesi: ekranda yalnızca güncel sahne durur (daktilo efektiyle yazılır, dokununca tamamlanır).
 * Seçenekler ya da gün akışı eylemleri altta sabit bir panelde: aramak için kaydırmak gerekmez.
 * Önceki sahneler üstteki düğmeyle açılan bir sayfada okunur.
 */
@OptIn(ExperimentalMaterial3Api::class)
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

    // Seçenekler, "metni göster" düğmesinin yerinde belirir: aynı yere yapılan ikinci dokunuş yanlışlıkla
    // seçim yapmasın diye eylemler kısa bir süre sonra etkinleşir.
    var armed by remember(passageKey) { mutableStateOf(false) }
    LaunchedEffect(passageKey, finished) {
        if (finished) {
            delay(ARM_DELAY_MS)
            armed = true
        }
    }
    val choose: (String) -> Unit = { if (armed) onChoose(it) }
    val hubAct: (HubAction) -> Unit = { if (armed) onHub(it) }

    // Her yeni sahne en baştan okunur.
    val listState = rememberLazyListState()
    LaunchedEffect(passageKey) { listState.scrollToItem(0) }
    var showHistory by rememberSaveable { mutableStateOf(false) }

    val skip = remember { MutableInteractionSource() }
    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clickable(interactionSource = skip, indication = null) { finished = true },
        ) {
            if (scene.history.isNotEmpty()) {
                item(key = "earlier") {
                    TextButton(onClick = { showHistory = true }, contentPadding = PaddingValues(horizontal = 0.dp)) {
                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.earlier), style = GameType.system)
                    }
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
            }
            // Son ekranının epilog kartları uzun bir okuma: kaydırılan alanda kalır.
            if (finished && scene.isEnding) {
                item(key = "ending-$passageKey") { EndingContent(ending) }
            }
        }

        ActionDock(resetKey = passageKey) {
            val hub = scene.hub
            when {
                !finished -> TextButton(onClick = { finished = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.show_all_text), style = GameType.system)
                }
                scene.isEnding -> Button(onClick = onBackToTitle, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.back_to_title))
                }
                hub != null -> HubPanel(hub, hubAct)
                else -> Choices(scene.choices, choose)
            }
        }
    }

    if (showHistory) {
        ModalBottomSheet(onDismissRequest = { showHistory = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
          Localized {
            Text(
                stringResource(R.string.earlier_title),
                style = GameType.systemTitle,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            val historyState = rememberLazyListState(initialFirstVisibleItemIndex = (scene.history.size - 1).coerceAtLeast(0))
            LazyColumn(
                state = historyState,
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                itemsIndexed(scene.history, key = { i, p -> "h$i-${p.sceneId}" }) { i, passage ->
                    if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), modifier = Modifier.padding(bottom = 14.dp))
                    PastPassage(passage)
                }
            }
          }
        }
    }
}

/**
 * Altta sabit eylem paneli: ekranın en fazla yarısını kaplar, sığmazsa kendi içinde kayar.
 * Başparmağın ulaştığı yerde durur; metin üstte serbestçe okunur.
 */
@Composable
private fun ActionDock(resetKey: String, content: @Composable () -> Unit) {
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.5f).dp
    val scroll = remember(resetKey) { androidx.compose.foundation.ScrollState(0) }
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .heightIn(max = maxHeight)
                .verticalScroll(scroll)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) { content() }
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Etiketli seçenek ilk kez çıktığında tek satırlık ipucu; (i) her zaman açıklar.
        val tagged = choices.any { it.requirementTag != null || it.odds != null || it.kindTag != null }
        if (tagged) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TipCard("choices", stringResource(R.string.tip_choices), Modifier.weight(1f))
                if ("choices" in LocalTips.current.seen) Spacer(Modifier.weight(1f))
                HelpButton(stringResource(R.string.help_story), Glossary.story())
            }
        }
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
                            text = " " + stringResource(R.string.odds_fmt, it),
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
private fun EndingContent(ending: com.yukselis.engine.story.EndingView?) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (ending == null) {
            Text(text = stringResource(R.string.the_end), style = GameType.systemTitle, color = MaterialTheme.colorScheme.primary)
        } else {
            EndingCards(ending)
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

private const val ARM_DELAY_MS = 400L
private const val CHAR_DELAY_MS = 16L
private const val CHARS_PER_TICK = 2
