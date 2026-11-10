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
import com.yukselis.protocol.ui.components.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import com.yukselis.protocol.ui.components.OutlinedButton
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
import androidx.compose.ui.text.style.TextOverflow
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
import com.yukselis.protocol.ui.theme.Game
import com.yukselis.protocol.ui.components.EchoLine
import com.yukselis.protocol.ui.components.leftRule
import com.yukselis.protocol.ui.components.systemWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Box

/**
 * Hikaye sekmesi: ekranda yalnızca güncel sahne durur (daktilo efektiyle yazılır, dokununca tamamlanır).
 * Hikaye metni ekranın ana unsurudur (Paket 5, UI_REVIEW A1–A4): seçenekler ve fırsat kartları sabit panelde değil,
 * metnin altında aynı kaydırmada gelir. Altta yalnız ince bir çubuk kalır: gün akışının rutin eylemleri, "metni göster"
 * ya da son ekranının düğmesi. Önceki sahneler üstteki düğmeyle açılan bir sayfada okunur.
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
    modifier: Modifier = Modifier,
    ending: com.yukselis.engine.story.EndingView? = null,
) {
    val fullText = scene.current.paragraphs.joinToString("\n\n")
    val textLength by rememberUpdatedState(fullText.length)
    val passageKey = "${scene.current.sceneId}#$turn"

    // Döndürme/yapılandırma değişince daktilo baştan başlamasın (QA P2-4).
    var shown by rememberSaveable(passageKey) { mutableIntStateOf(0) }
    var finished by rememberSaveable(passageKey) { mutableStateOf(!typewriter) }

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
    val hubUi = rememberHubUi()
    val hubScene = scene.hub

    val skip = remember { MutableInteractionSource() }
    Column(modifier = modifier.fillMaxSize()) {
        scene.chapter?.let { ChapterBar(it) }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
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
            // Seçenekler ve fırsat kartları metnin devamıdır: okuyup kaydırınca gelir.
            if (finished && !scene.isEnding) {
                if (hubScene != null) {
                    item(key = "hub-$passageKey") { HubStory(hubScene, hubUi, hubAct) }
                } else if (scene.choices.isNotEmpty()) {
                    item(key = "choices-$passageKey") { Choices(scene.choices, passageKey, choose) }
                }
            }
        }
        // Seçenekler ekranın altında kaldıysa küçük bir işaret: dokununca oraya kayar.
        val more = finished && !scene.isEnding && listState.canScrollForward
        if (more) {
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            val scrim = MaterialTheme.colorScheme.background.copy(alpha = 0.88f)
            Text(
                text = stringResource(if (hubScene != null) R.string.more_below else R.string.choices_below),
                style = GameType.system,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp)
                    .drawBehind { drawRect(scrim) }
                    .clickable { scope.launch { listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1) } }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
        }

        when {
            !finished -> ActionDock {
                TextButton(onClick = { finished = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.show_all_text), style = GameType.system)
                }
            }
            scene.isEnding -> ActionDock {
                Button(onClick = onBackToTitle, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.back_to_title)) }
            }
            // Rutin eylemler (Keşfet, Antrenman, Dinlen, Kule, Kapılar, Yolculuk, Zaman atla) hikaye seçeneklerinden ayrı.
            hubScene != null -> ActionDock { HubActionBar(hubScene, hubUi, hubAct) }
            else -> Unit
        }
    }
    hubScene?.let { HubDialogs(it, hubUi, hubAct) }

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
 * Her zaman görünen bölüm satırı: "Bölüm N · Hedef · Kalan gün" (DECISIONS #130). Dokununca hedef tam açılır.
 */
@Composable
private fun ChapterBar(line: com.yukselis.engine.world.ChapterLine) {
    var open by rememberSaveable { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = stringResource(R.string.a11y_chapter)) { open = !open }
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.chapter_line, line.no, if (open) line.title + " — " + line.goal else line.goal),
            style = GameType.systemTitle.copy(fontSize = 12.sp, letterSpacing = 0.4.sp, fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.primary,
            maxLines = if (open) 6 else 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            line.countdown ?: androidx.compose.ui.res.pluralStringResource(R.plurals.chapter_days, line.daysLeft, line.daysLeft),
            style = GameType.systemTitle.copy(fontSize = 12.sp, letterSpacing = 0.4.sp, fontWeight = FontWeight.Medium),
            // Geri sayım her zaman kehribar: gün saymak Sistem'in uyarısıdır; son günlerde kızıla döner.
            color = if (line.daysLeft <= DEADLINE_WARN_DAYS) Game.colors.danger else Game.colors.amber,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
    HorizontalDivider(color = Game.colors.line)
}

private const val DEADLINE_WARN_DAYS = 3

/** Altta ince eylem çubuğu: rutin eylemler, "metni göster" ya da son düğmesi. Hikaye seçenekleri burada durmaz. */
@Composable
private fun ActionDock(content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth(),
    ) {
        HorizontalDivider(color = Game.colors.line)
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) { content() }
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

/** Yoldaşların görüşü: ad + tek satır. Yankı'nınki ters video bir satırdır. */
@Composable
private fun Opinions(lines: List<com.yukselis.engine.story.CompanionLineView>) {
    val companion = Game.colors.companion
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        lines.forEach {
            if (it.echo) EchoLine(it.text)
            else Text(
                "${it.npcName}: ${it.text}",
                style = GameType.system,
                color = companion,
                modifier = Modifier.fillMaxWidth().leftRule(companion).padding(start = 12.dp),
            )
        }
    }
}

/** Pasif Sistem sezgisi: yalnızca oyuncunun gördüğü satırlar; tek aralıklı, renkli, sol çizgili. */
@Composable
private fun Insights(lines: List<InsightView>) {
    val system = Game.colors.system
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        lines.forEach {
            if (it.echo) EchoLine(it.text)
            else Text(it.text, style = GameType.system, color = system, modifier = Modifier.fillMaxWidth().leftRule(system).padding(start = 12.dp))
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
private fun Choices(choices: List<ChoiceView>, key: String, onChoose: (String) -> Unit) {
    // Beşten fazla seçenek: ilk dördü görünür (açık olanlar önce), kalanı "Diğer seçenekler" altında katlı (UI_REVIEW A2).
    var expanded by rememberSaveable(key) { mutableStateOf(false) }
    val ordered = if (choices.size > FOLD_OVER) choices.filter { it.enabled } + choices.filter { !it.enabled } else choices
    val shown = if (choices.size > FOLD_OVER && !expanded) ordered.take(FOLD_SHOW) else ordered
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        // Etiketli seçenek ilk kez çıktığında tek satırlık ipucu; (i) her zaman açıklar.
        val tagged = choices.any { it.requirementTag != null || it.odds != null || it.kindTag != null }
        if (tagged) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TipCard("choices", stringResource(R.string.tip_choices), Modifier.weight(1f))
                if ("choices" in LocalTips.current.seen) Spacer(Modifier.weight(1f))
                HelpButton(stringResource(R.string.help_story), Glossary.story())
            }
        }
        shown.forEach { choice -> ChoiceButton(choice) { onChoose(choice.id) } }
        if (choices.size > FOLD_OVER && !expanded) {
            TextButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.other_choices, choices.size - FOLD_SHOW), style = GameType.system)
            }
        }
    }
}

/**
 * Kompakt seçenek düğmesi (UI_REVIEW A3, C1): etiketler (tür, koşul, olasılık) metnin üstünde ayrı küçük satır, metin tam
 * genişlikte. Metin tek satıra sığmazsa bir boy küçülür; hiçbir zaman kesilmez.
 */
@Composable
private fun ChoiceButton(choice: ChoiceView, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = choice.enabled,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (choice.kindTag != null || choice.requirementTag != null || choice.odds != null) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = listOfNotNull(choice.kindTag, choice.requirementTag).joinToString(" "),
                        style = GameType.system.copy(fontSize = 12.sp, lineHeight = 16.sp),
                        color = when {
                            !choice.enabled -> MaterialTheme.colorScheme.onSurfaceVariant
                            choice.kindTag != null -> Game.colors.echo
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.weight(1f),
                    )
                    // Görünür olasılık: tek seferlik (kırmızı) / yeniden denenebilir (beyaz).
                    choice.odds?.let {
                        Text(
                            text = stringResource(R.string.odds_fmt, it),
                            style = GameType.system.copy(fontSize = 12.sp, lineHeight = 16.sp),
                            color = if (choice.oneShot) Game.colors.danger else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            val base = MaterialTheme.typography.bodyLarge
            var small by remember(choice.label) { mutableStateOf(false) }
            Text(
                text = choice.label,
                style = if (small) base.copy(fontSize = base.fontSize * 0.88f, lineHeight = base.lineHeight * 0.88f) else base,
                color = if (choice.enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                onTextLayout = { if (!small && it.lineCount > 1) small = true },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private const val FOLD_OVER = 5
private const val FOLD_SHOW = 4

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
    val gold = Game.colors.unique
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
    Column(modifier = Modifier.fillMaxWidth().systemWindow(accent = Game.colors.line)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = title, style = GameType.system, color = MaterialTheme.colorScheme.primary)
            if (body.isNotEmpty()) Text(text = body, style = GameType.narration)
        }
    }
}

private const val ARM_DELAY_MS = 400L
private const val CHAR_DELAY_MS = 16L
private const val CHARS_PER_TICK = 2
