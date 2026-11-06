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
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        // Sonraki aşamalarda açılacak bölümler
        SectionTitle(stringResource(R.string.tab_more))
        if (map.isNotEmpty()) MapSection(map)
        notices.forEach { SystemCard(it, Modifier.padding(vertical = 4.dp)) }
        daily?.let { DailyPanel(it, onTrain, inDayLoop = map.isNotEmpty()) }
        relations?.let { RelationsSection(it) }
        ranking?.takeIf { map.isNotEmpty() }?.let { RankingSection(it) }
        journal?.let { JournalSection(it) }

        SectionTitle(stringResource(R.string.saves))
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

        SectionTitle(stringResource(R.string.settings))
        LanguagePicker(lang, onLanguage)
        ThemePicker(settings.theme, { mode -> onSettings { it.copy(theme = mode) } })
        ToggleRow(stringResource(R.string.typewriter), null, settings.typewriter, enabled = true) { on ->
            onSettings { it.copy(typewriter = on) }
        }
        ToggleRow(stringResource(R.string.haptics), null, settings.haptics, enabled = true) { on ->
            onSettings { it.copy(haptics = on) }
        }
        NotificationToggle(settings.notifications, notificationDenied, onSettings, onNotificationPermission)

        Spacer(Modifier.padding(top = 16.dp))
        OutlinedButton(onClick = onBackToTitle, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.back_to_title))
        }
        Spacer(Modifier.padding(bottom = 24.dp))
    }
}

/** Harita: bölgeler, tehlike, kontrol, açık kapılar. */
@Composable
private fun MapSection(regions: List<RegionView>) {
    Text(stringResource(R.string.map), style = GameType.systemTitle, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
    regions.forEach { r ->
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = (if (r.current) "▶ " else "") + r.name,
                    style = GameType.systemTitle,
                    color = if (r.current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
                Text(stringResource(R.string.map_line, r.danger, r.control, r.gates.size), style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant)
                r.gates.forEach { g -> Text(stringResource(R.string.hub_gate, g.grade, g.enemies, g.daysLeft), style = GameType.system, color = Color(0xFFFFA726)) }
            }
        }
    }
}

/** Görevler ve efsane (tanıklı başarılar). */
@Composable
private fun JournalSection(journal: JournalView) {
    Text(stringResource(R.string.quests), style = GameType.systemTitle, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
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
            Text(stringResource(R.string.daily_streak, daily.streak), style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU ->
                launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            else -> onSettings { it.copy(notifications = true) }
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
