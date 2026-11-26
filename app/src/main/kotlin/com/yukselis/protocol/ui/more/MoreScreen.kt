package com.yukselis.protocol.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.yukselis.protocol.ui.components.Button
import androidx.compose.material3.MaterialTheme
import com.yukselis.protocol.ui.components.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yukselis.engine.core.GameMode
import com.yukselis.engine.i18n.Lang
import com.yukselis.engine.save.SaveManager
import com.yukselis.engine.save.SlotInfo
import com.yukselis.protocol.R
import com.yukselis.protocol.ui.theme.Game
import com.yukselis.protocol.data.Settings
import com.yukselis.protocol.data.ThemeMode
import com.yukselis.protocol.ui.components.SectionTitle
import com.yukselis.protocol.ui.components.StatBar
import com.yukselis.protocol.ui.theme.EchoColor
import com.yukselis.engine.story.DailyView
import com.yukselis.engine.story.JournalView
import com.yukselis.engine.story.RegionView
import com.yukselis.engine.story.StatusView
import com.yukselis.engine.world.QuestStatus
import com.yukselis.engine.story.NoticeView
import com.yukselis.protocol.ui.components.SystemCard
import androidx.compose.ui.graphics.Color
import com.yukselis.protocol.ui.theme.GameType
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import com.yukselis.protocol.data.TextSize
import com.yukselis.protocol.ui.components.Glossary
import com.yukselis.protocol.ui.components.HelpEntry
import com.yukselis.protocol.ui.components.TipCard

@Composable
fun MoreScreen(
    lang: Lang,
    settings: Settings,
    daily: DailyView?,
    map: List<RegionView>,
    journal: JournalView?,
    relations: com.yukselis.engine.story.RelationsView?,
    ranking: com.yukselis.engine.story.RankingView?,
    notices: List<NoticeView>,
    onTrain: () -> Unit,
    mode: GameMode,
    slots: List<Pair<String, SlotInfo?>>,
    onSave: (String) -> Unit,
    onLoad: (String) -> Unit,
    onLanguage: (Lang) -> Unit,
    onSettings: ((Settings) -> Settings) -> Unit,
    onBackToTitle: () -> Unit,
    modifier: Modifier = Modifier,
    onNotificationPermission: (Boolean) -> Unit = {},
    notificationDenied: Boolean = false,
    status: StatusView? = null,
) {
    var page by rememberSaveable { mutableStateOf<MorePage?>(null) }
    // Sistem geri tuşu önce alt sayfayı kapatır.
    BackHandler(enabled = page != null) { page = null }
    val inDayLoop = map.isNotEmpty()
    // Karolar kendini açar (Paket 2): Harita Perde 2'de, İlişkiler ilk tanışmada, Sıralama ilk sıralama gününde.
    val showMap = status?.showMap ?: true
    val showRelations = status?.showRelations ?: true
    val showRank = status?.showRank ?: true
    val showKnowledge = status?.showKnowledge ?: true

    // Açık alt sayfa artık gizlendiyse (örn. yükleme sonrası) karo ızgarası gösterilir.
    val current = page?.takeUnless { it == MorePage.MAP && !showMap || it == MorePage.RELATIONS && !showRelations || it == MorePage.RANKING && !showRank }
    if (current == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            notices.forEach { SystemCard(it) }
            TipCard("more", stringResource(R.string.tip_more))
            val tiles = buildList {
                if (showMap) {
                    add(Tile(MorePage.MAP, Icons.Filled.Place, stringResource(R.string.map),
                        if (map.isEmpty()) stringResource(R.string.tile_map_none) else map.firstOrNull { it.current }?.name ?: "",
                        enabled = inDayLoop))
                }
                daily?.let {
                    add(Tile(MorePage.DAILY, Icons.Filled.DateRange, stringResource(R.string.daily_quest),
                        if (it.completed) stringResource(R.string.daily_done) else stringResource(R.string.tile_daily, it.progress, it.steps)))
                }
                journal?.let {
                    add(Tile(MorePage.JOURNAL, Icons.AutoMirrored.Filled.List, stringResource(R.string.journal),
                        stringResource(R.string.tile_quests, it.quests.count { q -> q.status == QuestStatus.ACTIVE })))
                }
                relations?.takeIf { showRelations }?.let {
                    add(Tile(MorePage.RELATIONS, Icons.Filled.Face, stringResource(R.string.relationships),
                        stringResource(R.string.party_line, it.companions, it.capacity)))
                }
                ranking?.takeIf { inDayLoop && showRank }?.let {
                    add(Tile(MorePage.RANKING, Icons.Filled.Star, stringResource(R.string.ranking), "#${it.publicRank} / ${it.total}"))
                }
                add(Tile(MorePage.SAVES, Icons.Filled.Check, stringResource(R.string.saves), stringResource(R.string.tile_saves)))
                add(Tile(MorePage.SETTINGS, Icons.Filled.Settings, stringResource(R.string.settings), stringResource(R.string.tile_settings)))
                add(Tile(MorePage.HELP, Icons.Filled.Info, stringResource(R.string.help), stringResource(R.string.help_desc)))
            }
            tiles.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { t -> TileCard(t, Modifier.weight(1f)) { page = t.page } }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            OutlinedButton(onClick = onBackToTitle, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                Text(stringResource(R.string.back_to_title))
            }
        }
        return
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 4.dp)) {
            IconButton(onClick = { page = null }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) }
            Text(stringResource(current.title), style = GameType.systemTitle, color = MaterialTheme.colorScheme.primary)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 4.dp),
        ) {
            when (current) {
                MorePage.MAP -> MapSection(map)
                MorePage.DAILY -> {
                    notices.forEach { SystemCard(it, Modifier.padding(vertical = 4.dp)) }
                    daily?.let { DailyPanel(it, onTrain, inDayLoop) }
                }
                MorePage.JOURNAL -> journal?.let { JournalSection(it, showKnowledge) }
                MorePage.RELATIONS -> relations?.let { RelationsSection(it) }
                MorePage.RANKING -> ranking?.let { RankingSection(it) }
                MorePage.SAVES -> {
                    // Operasyon öncesi satırı ilk operasyon kaydına kadar gizli (UI_REVIEW D1); Önceki oturum yalnız doluysa listede (B3).
                    slots.filter { (slot, info) -> slot != SaveManager.OPERATION || info != null }.forEach { (slot, info) ->
                        SlotRow(
                            slot = slot,
                            info = info,
                            canSave = slot in SaveManager.MANUAL && mode != GameMode.IRONMAN,
                            onSave = { onSave(slot) },
                            onLoad = { onLoad(slot) },
                        )
                    }
                    if (mode == GameMode.IRONMAN) {
                        Text(stringResource(R.string.msg_ironman_no_manual), style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                MorePage.SETTINGS -> {
                    LanguagePicker(lang, onLanguage)
                    ThemePicker(settings.theme, { mode -> onSettings { it.copy(theme = mode) } })
                    TextSizePicker(settings.textSize) { size -> onSettings { it.copy(textSize = size) } }
                    ToggleRow(stringResource(R.string.typewriter), null, settings.typewriter, enabled = true) { on ->
                        onSettings { it.copy(typewriter = on) }
                    }
                    ToggleRow(stringResource(R.string.haptics), null, settings.haptics, enabled = true) { on ->
                        onSettings { it.copy(haptics = on) }
                    }
                    ToggleRow(stringResource(R.string.combat_numbers), null, settings.combatNumbers, enabled = true) { on ->
                        onSettings { it.copy(combatNumbers = on) }
                    }
                    ToggleRow(stringResource(R.string.fast_combat), null, settings.fastCombat, enabled = true) { on ->
                        onSettings { it.copy(fastCombat = on) }
                    }
                    NotificationToggle(settings.notifications, notificationDenied, onSettings, onNotificationPermission)
                    LicensesRow()
                }
                MorePage.HELP -> HelpPage(onResetTips = { onSettings { it.copy(seenTips = emptySet()) } })
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private enum class MorePage(val title: Int) {
    MAP(R.string.map), DAILY(R.string.daily_quest), JOURNAL(R.string.journal), RELATIONS(R.string.relationships),
    RANKING(R.string.ranking), SAVES(R.string.saves), SETTINGS(R.string.settings), HELP(R.string.help),
}

private data class Tile(val page: MorePage, val icon: ImageVector, val title: String, val subtitle: String, val enabled: Boolean = true)

@Composable
private fun TileCard(tile: Tile, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = tile.enabled,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.heightIn(min = 92.dp),
    ) {
        val alpha = if (tile.enabled) 1f else 0.45f
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(tile.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = alpha), modifier = Modifier.size(24.dp))
            Text(tile.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(tile.subtitle, style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Yardım: her ekranın sembolleri tek yerde; ipuçlarını yeniden gösterme. */
@Composable
private fun HelpPage(onResetTips: () -> Unit) {
    listOf(
        R.string.help_story to Glossary.story(),
        R.string.help_hub to Glossary.hub(),
        R.string.help_combat to Glossary.combat(),
        R.string.help_status to Glossary.status(),
    ).forEach { (title, entries) ->
        SectionTitle(stringResource(title))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { entries.forEach { (t, m) -> HelpEntry(t, m) } }
    }
    OutlinedButton(onClick = onResetTips, modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
        Text(stringResource(R.string.help_tips_reset))
    }
}

@Composable
private fun TextSizePicker(current: TextSize, onPick: (TextSize) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.text_size), style = MaterialTheme.typography.bodyLarge)
        // 2×2 ızgara: "Çok büyük" / "Extra large" tek satırda kesilmeden sığar (UI_REVIEW C6).
        listOf(
            TextSize.SMALL to R.string.text_small,
            TextSize.MEDIUM to R.string.text_medium,
            TextSize.LARGE to R.string.text_large,
            TextSize.XLARGE to R.string.text_xlarge,
        ).chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                pair.forEach { (size, label) ->
                    val padding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    if (size == current) {
                        Button(onClick = { }, contentPadding = padding, modifier = Modifier.weight(1f)) { Text(stringResource(label), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    } else {
                        OutlinedButton(onClick = { onPick(size) }, contentPadding = padding, modifier = Modifier.weight(1f)) {
                            Text(stringResource(label), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

/** Harita: bulunduğun bölge üstte, diğerleri perdeye göre gruplu; süresi dolan kapı "Taşmak üzere" (UI_REVIEW D4). */
@Composable
private fun MapSection(regions: List<RegionView>) {
    val current = regions.firstOrNull { it.current }
    val others = regions.filterNot { it.current }
    current?.let { RegionCard(it) }
    val acts = others.map { it.act }.distinct()
    if (acts.size <= 1) {
        others.forEach { RegionCard(it) }
    } else {
        others.groupBy { it.act }.forEach { (act, list) ->
            Text(
                text = stringResource(R.string.act_short, act),
                style = GameType.systemTitle,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
            )
            list.forEach { RegionCard(it) }
        }
    }
}

@Composable
private fun RegionCard(r: RegionView) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = (if (r.current) "▶ " else "") + r.name,
                style = GameType.systemTitle,
                color = if (r.current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            Text(stringResource(R.string.map_line, r.danger, r.control, r.gates.size), style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant)
            r.gates.forEach { g ->
                val imminent = g.daysLeft <= 0
                Text(
                    text = if (imminent) stringResource(R.string.hub_gate_imminent, g.grade, g.enemies)
                    else androidx.compose.ui.res.pluralStringResource(R.plurals.hub_gate, g.daysLeft, g.grade, g.enemies, g.daysLeft),
                    style = GameType.system,
                    color = if (imminent) Game.colors.danger else Game.colors.amber,
                )
            }
        }
    }
}

/** Görevler ve efsane (UI_REVIEW C7): her görev kendi kartında, etkin/tamamlanan ayrımı ve anlatı yazı tipiyle açıklama. */
@Composable
private fun JournalSection(journal: JournalView, showKnowledge: Boolean) {
    journal.chapter?.let { ch ->
        SectionTitle(stringResource(R.string.chapter_goal_title), Modifier.padding(top = 6.dp))
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.chapter_line, ch.no, ch.title), style = GameType.systemTitle, color = MaterialTheme.colorScheme.primary)
                Text(ch.goal, style = GameType.narrationPast, color = MaterialTheme.colorScheme.onSurface)
                if (ch.threat.isNotEmpty()) Text(ch.threat, style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    ch.phase + " · " + androidx.compose.ui.res.pluralStringResource(R.plurals.chapter_days, ch.daysLeft, ch.daysLeft),
                    style = GameType.system,
                    color = Game.colors.amber,
                )
            }
        }
    }
    SectionTitle(stringResource(R.string.side_quests))
    if (journal.quests.isEmpty()) {
        Text(stringResource(R.string.no_quests), style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        val active = journal.quests.filter { it.status == QuestStatus.ACTIVE }
        val finished = journal.quests.filter { it.status != QuestStatus.ACTIVE }
        if (active.isNotEmpty()) {
            Text(stringResource(R.string.quests_active), style = GameType.system, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 4.dp))
            active.forEach { QuestCard(it) }
        }
        if (finished.isNotEmpty()) {
            Text(stringResource(R.string.quests_finished), style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp, bottom = 4.dp))
            finished.forEach { QuestCard(it) }
        }
    }
    if (journal.legend.isNotEmpty()) {
        SectionTitle(stringResource(R.string.legend))
        journal.legend.forEach { Text("${it.name} ×${it.count}", style = GameType.system, modifier = Modifier.padding(vertical = 2.dp)) }
    }
    // OP MC bilgi günlüğü: söylentiler, sırlar, gerçek seviyeler (ilk bilgiyle açılır).
    if (showKnowledge && journal.knowledge.isNotEmpty()) {
        SectionTitle(stringResource(R.string.intel_log))
        journal.knowledge.forEach { k ->
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(k.name, style = GameType.systemTitle, color = Game.colors.system)
                    Text(k.description, style = GameType.narrationPast, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun QuestCard(q: com.yukselis.engine.story.QuestView) {
    val badgeColor = when (q.status) {
        QuestStatus.ACTIVE -> MaterialTheme.colorScheme.primary
        QuestStatus.DONE -> Game.colors.success
        QuestStatus.FAILED -> Game.colors.danger
    }
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(q.name, style = GameType.systemTitle, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                if (q.status != QuestStatus.ACTIVE) {
                    Text(
                        text = stringResource(if (q.status == QuestStatus.DONE) R.string.quest_status_done else R.string.quest_status_failed),
                        style = GameType.system,
                        color = badgeColor,
                    )
                }
            }
            Text(q.description, style = GameType.narrationPast, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Günlük Görev: yalnızca oyuncunun gördüğü pencere. */
@Composable
private fun DailyPanel(daily: DailyView, onTrain: () -> Unit, inDayLoop: Boolean) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(stringResource(R.string.daily_quest), style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(daily.title, style = GameType.systemTitle, color = Game.colors.success)
            Text(stringResource(R.string.daily_reward), style = GameType.system, color = Game.colors.amber)
            Text(daily.description, style = GameType.system)
            StatBar(stringResource(R.string.daily_steps), daily.progress.toLong(), daily.steps.toLong(), Game.colors.success, Modifier.padding(top = 6.dp))
            Text(androidx.compose.ui.res.pluralStringResource(R.plurals.daily_streak, daily.streak, daily.streak), style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.daily_only_you), style = GameType.system, color = EchoColor)
            when {
                daily.completed -> Text(stringResource(R.string.daily_done), style = GameType.systemTitle, color = Game.colors.success)
                daily.trainable && !inDayLoop -> Button(onClick = onTrain, modifier = Modifier.padding(top = 6.dp)) { Text(stringResource(R.string.daily_train)) }
                daily.trainable -> Text(stringResource(R.string.daily_train_hub), style = GameType.system, color = MaterialTheme.colorScheme.outline)
                else -> Text(stringResource(R.string.daily_in_story), style = GameType.system, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}


@Composable
private fun SlotRow(slot: String, info: SlotInfo?, canSave: Boolean, onSave: () -> Unit, onLoad: () -> Unit) {
    val name = when (slot) {
        SaveManager.AUTO -> stringResource(R.string.slot_auto)
        SaveManager.PREVIOUS_SESSION -> stringResource(R.string.slot_previous)
        SaveManager.OPERATION -> stringResource(R.string.slot_operation)
        else -> stringResource(R.string.slot_n, SaveManager.MANUAL.indexOf(slot) + 1)
    }
    var confirm by androidx.compose.runtime.remember(slot) { mutableStateOf<SlotConfirm?>(null) }
    // Kaydedilen satırda 1 sn vurgu (UI_REVIEW B6): aynı gün içinde kaydedilince geri bildirim netleşir.
    var savedPulse by androidx.compose.runtime.remember(slot) { mutableStateOf(0) }
    var highlighted by androidx.compose.runtime.remember(slot) { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(savedPulse) {
        if (savedPulse > 0) {
            highlighted = true
            kotlinx.coroutines.delay(1000L)
            highlighted = false
        }
    }
    val doSave = {
        onSave()
        savedPulse++
    }
    Surface(
        color = if (highlighted) Game.colors.success.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = GameType.systemTitle, color = if (highlighted) Game.colors.success else MaterialTheme.colorScheme.onSurface)
                Text(
                    text = info?.let { stringResource(R.string.continue_summary, it.playerName, it.level, it.day) }
                        ?: stringResource(R.string.empty_slot),
                    style = GameType.system,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Dolu slota kaydetmek ve yüklemek geri alınamaz: önce onay (UI_REVIEW B2, B3). Kaydet birincil, Yükle ikincil (B6).
            if (canSave) {
                Button(onClick = { if (info != null) confirm = SlotConfirm.SAVE else doSave() }) { Text(stringResource(R.string.save)) }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { confirm = SlotConfirm.LOAD }, enabled = info != null) { Text(stringResource(R.string.load)) }
            } else {
                Button(onClick = { confirm = SlotConfirm.LOAD }, enabled = info != null) { Text(stringResource(R.string.load)) }
            }
        }
    }
    val pending = confirm
    if (pending != null && info != null) {
        val summary = stringResource(R.string.continue_summary, info.playerName, info.level, info.day)
        val saving = pending == SlotConfirm.SAVE
        com.yukselis.protocol.ui.components.AppAlertDialog(
            onDismissRequest = { confirm = null },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { confirm = null; if (saving) doSave() else onLoad() }) {
                    Text(stringResource(if (saving) R.string.confirm_overwrite_ok else R.string.confirm_load_ok))
                }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { confirm = null }) { Text(stringResource(R.string.cancel)) } },
            title = { Text(stringResource(if (saving) R.string.confirm_overwrite_title else R.string.confirm_load_title)) },
            text = { Text(stringResource(if (saving) R.string.confirm_overwrite else R.string.confirm_load, name, summary)) },
        )
    }
}

private enum class SlotConfirm { SAVE, LOAD }

@Composable
fun LanguagePicker(current: Lang, onPick: (Lang) -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
    ) {
        Text(stringResource(R.string.language), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        // Dil adları her zaman kendi dillerinde gösterilir.
        listOf(Lang.TR to "Türkçe", Lang.EN to "English").forEach { (lang, label) ->
            if (lang == current) {
                Button(onClick = { }) { Text(label) }
            } else {
                OutlinedButton(onClick = { onPick(lang) }) { Text(label) }
            }
        }
    }
}

/** Tema: Sistem · Açık · Koyu. Seçim anında uygulanır ve kaydedilir. */
@Composable
fun ThemePicker(current: ThemeMode, onPick: (ThemeMode) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(stringResource(R.string.theme), style = MaterialTheme.typography.bodyLarge)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        ) {
            listOf(
                ThemeMode.SYSTEM to R.string.theme_system,
                ThemeMode.LIGHT to R.string.theme_light,
                ThemeMode.DARK to R.string.theme_dark,
            ).forEach { (mode, label) ->
                val padding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                if (mode == current) {
                    Button(onClick = { }, contentPadding = padding, modifier = Modifier.weight(1f)) { Text(stringResource(label), maxLines = 1) }
                } else {
                    OutlinedButton(onClick = { onPick(mode) }, contentPadding = padding, modifier = Modifier.weight(1f)) {
                        Text(stringResource(label), maxLines = 1)
                    }
                }
            }
        }
    }
}

/**
 * Bildirim anahtarı (Aşama 9): açıklama metni her zaman görünür; Android 13+'da açarken izin istenir.
 * Reddedilirse anahtar kapanır, Android ayarlarına yönlendiren not çıkar ve izin bir daha kendiliğinden istenmez.
 */
@Composable
private fun NotificationToggle(
    enabled: Boolean,
    denied: Boolean,
    onSettings: ((Settings) -> Settings) -> Unit,
    onPermission: (Boolean) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { granted -> onPermission(granted) }
    val description = if (denied && !enabled) stringResource(R.string.notifications_denied) else stringResource(R.string.notifications_desc)
    ToggleRow(stringResource(R.string.notifications), description, enabled, enabled = true) { on ->
        when {
            !on -> onSettings { it.copy(notifications = false) }
            com.yukselis.protocol.data.Notifications.permitted(context) -> onSettings { it.copy(notifications = true) }
            // Daha önce reddedildi: ısrar yok, sistem ayarlarını aç.
            denied -> context.startActivity(
                android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            // `permitted` 33 altında hep true; buraya yalnız Android 13+ gelir (lint ObsoleteSdkInt).
            else -> launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
private fun ToggleRow(label: String, description: String?, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    // Satırın tamamı tek anahtar: ekran okuyucu etiketi durumla birlikte okur, dokunma alanı satır kadar (UI_REVIEW G2).
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = androidx.compose.ui.semantics.Role.Switch, onValueChange = onChange)
            .padding(vertical = 9.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            description?.let { Text(it, style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}
