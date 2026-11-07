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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.yukselis.protocol.data.Settings
import com.yukselis.protocol.data.ThemeMode
import com.yukselis.protocol.ui.components.SectionTitle
import com.yukselis.protocol.ui.components.StatBar
import com.yukselis.protocol.ui.theme.EchoColor
import com.yukselis.engine.story.DailyView
import com.yukselis.engine.story.JournalView
import com.yukselis.engine.story.RegionView
import com.yukselis.engine.world.QuestStatus
import com.yukselis.engine.story.NoticeView
import com.yukselis.protocol.ui.components.SystemCard
import androidx.compose.ui.graphics.Color
import com.yukselis.protocol.ui.theme.GameType
import androidx.activity.compose.BackHandler
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
) {
    var page by rememberSaveable { mutableStateOf<MorePage?>(null) }
    // Sistem geri tuşu önce alt sayfayı kapatır.
    BackHandler(enabled = page != null) { page = null }
    val inDayLoop = map.isNotEmpty()

    val current = page
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
                add(Tile(MorePage.MAP, Icons.Filled.Place, stringResource(R.string.map),
                    if (map.isEmpty()) stringResource(R.string.tile_map_none) else map.firstOrNull { it.current }?.name ?: "",
                    enabled = inDayLoop))
                daily?.let {
                    add(Tile(MorePage.DAILY, Icons.Filled.DateRange, stringResource(R.string.daily_quest),
                        if (it.completed) stringResource(R.string.daily_done) else stringResource(R.string.tile_daily, it.progress, it.steps)))
                }
                journal?.let {
                    add(Tile(MorePage.JOURNAL, Icons.AutoMirrored.Filled.List, stringResource(R.string.journal),
                        stringResource(R.string.tile_quests, it.quests.count { q -> q.status == QuestStatus.ACTIVE })))
                }
                relations?.let {
                    add(Tile(MorePage.RELATIONS, Icons.Filled.Face, stringResource(R.string.relationships),
                        stringResource(R.string.party_line, it.companions, it.capacity)))
                }
                ranking?.takeIf { inDayLoop }?.let {
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
                MorePage.JOURNAL -> journal?.let { JournalSection(it) }
                MorePage.RELATIONS -> relations?.let { RelationsSection(it) }
                MorePage.RANKING -> ranking?.let { RankingSection(it) }
                MorePage.SAVES -> {
                    slots.forEach { (slot, info) ->
                        SlotRow(
                            slot = slot,
                            info = info,
                            canSave = slot != SaveManager.AUTO && mode != GameMode.IRONMAN,
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
                    NotificationToggle(settings.notifications, notificationDenied, onSettings, onNotificationPermission)
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
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(stringResource(R.string.text_size), style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
            listOf(
                TextSize.SMALL to R.string.text_small,
                TextSize.MEDIUM to R.string.text_medium,
                TextSize.LARGE to R.string.text_large,
                TextSize.XLARGE to R.string.text_xlarge,
            ).forEach { (size, label) ->
                val padding = PaddingValues(horizontal = 2.dp, vertical = 8.dp)
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

/** Harita: bölgeler, tehlike, kontrol, açık kapılar. */
@Composable
private fun MapSection(regions: List<RegionView>) {
    regions.forEach { r ->
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = (if (r.current) "▶ " else "") + r.name,
                    style = GameType.systemTitle,
                    color = if (r.current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
                Text(stringResource(R.string.map_line, r.danger, r.control, r.gates.size), style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant)
                r.gates.forEach { g -> Text(androidx.compose.ui.res.pluralStringResource(R.plurals.hub_gate, g.daysLeft, g.grade, g.enemies, g.daysLeft), style = GameType.system, color = Color(0xFFFFA726)) }
            }
        }
    }
}

/** Görevler ve efsane (tanıklı başarılar). */
@Composable
private fun JournalSection(journal: JournalView) {
    // Bölüm hedefi yan görevlerden ayrı (DECISIONS #130).
    journal.chapter?.let { ch ->
        Text(stringResource(R.string.chapter_goal_title), style = GameType.systemTitle, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
        Column(modifier = Modifier.padding(vertical = 3.dp)) {
            Text(stringResource(R.string.chapter_line, ch.no, ch.title), style = GameType.systemTitle)
            Text(ch.goal, style = GameType.system)
            if (ch.threat.isNotEmpty()) Text(ch.threat, style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                ch.phase + " · " + androidx.compose.ui.res.pluralStringResource(R.plurals.chapter_days, ch.daysLeft, ch.daysLeft),
                style = GameType.system,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
    Text(stringResource(R.string.side_quests), style = GameType.systemTitle, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
    if (journal.quests.isEmpty()) Text(stringResource(R.string.no_quests), style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant)
    journal.quests.forEach { q ->
        val color = when (q.status) {
            QuestStatus.ACTIVE -> MaterialTheme.colorScheme.onSurface
            QuestStatus.DONE -> Color(0xFF66BB6A)
            QuestStatus.FAILED -> Color(0xFFEF5350)
        }
        Column(modifier = Modifier.padding(vertical = 3.dp)) {
            Text(q.name, style = GameType.systemTitle, color = color)
            Text(q.description, style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (journal.legend.isNotEmpty()) {
        Text(stringResource(R.string.legend), style = GameType.systemTitle, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
        journal.legend.forEach { Text("${it.name} ×${it.count}", style = GameType.system) }
    }
    // OP MC bilgi günlüğü: söylentiler, sırlar, gerçek seviyeler.
    if (journal.knowledge.isNotEmpty()) {
        Text(stringResource(R.string.intel_log), style = GameType.systemTitle, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
        journal.knowledge.forEach { k ->
            Column(modifier = Modifier.padding(vertical = 3.dp)) {
                Text(k.name, style = GameType.systemTitle, color = Color(0xFF26A69A))
                Text(k.description, style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
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
            Text(daily.title, style = GameType.systemTitle, color = Color(0xFF66BB6A))
            Text(stringResource(R.string.daily_reward), style = GameType.system, color = Color(0xFFFFD54F))
            Text(daily.description, style = GameType.system)
            StatBar(stringResource(R.string.daily_steps), daily.progress.toLong(), daily.steps.toLong(), Color(0xFF66BB6A), Modifier.padding(top = 6.dp))
            Text(androidx.compose.ui.res.pluralStringResource(R.plurals.daily_streak, daily.streak, daily.streak), style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.daily_only_you), style = GameType.system, color = EchoColor)
            when {
                daily.completed -> Text(stringResource(R.string.daily_done), style = GameType.systemTitle, color = Color(0xFF66BB6A))
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
        else -> stringResource(R.string.slot_n, SaveManager.MANUAL.indexOf(slot) + 1)
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = GameType.systemTitle)
                Text(
                    text = info?.let { stringResource(R.string.continue_summary, it.playerName, it.level, it.day) }
                        ?: stringResource(R.string.empty_slot),
                    style = GameType.system,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (canSave) {
                OutlinedButton(onClick = onSave) { Text(stringResource(R.string.save)) }
                Spacer(Modifier.width(8.dp))
            }
            Button(onClick = onLoad, enabled = info != null) { Text(stringResource(R.string.load)) }
        }
    }
}

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
                ThemeMode.AMOLED to R.string.theme_amoled,
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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            description?.let { Text(it, style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}
