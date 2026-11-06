package com.yukselis.protocol.ui

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yukselis.engine.core.GameMode
import com.yukselis.engine.i18n.Lang
import com.yukselis.engine.story.SceneView
import com.yukselis.engine.story.StatusView
import com.yukselis.protocol.R
import com.yukselis.protocol.data.ThemeMode
import com.yukselis.protocol.ui.components.CenteredNote
import com.yukselis.protocol.ui.components.StatBar
import com.yukselis.protocol.ui.more.MoreScreen
import com.yukselis.protocol.ui.bag.BagScreen
import com.yukselis.protocol.ui.combat.CombatScreen
import com.yukselis.protocol.ui.skills.SkillsScreen
import com.yukselis.protocol.ui.system.CelebrationOverlay
import com.yukselis.protocol.ui.system.PromptOverlay
import com.yukselis.engine.core.WorldState
import androidx.compose.foundation.layout.Box
import com.yukselis.protocol.ui.status.StatusScreen
import com.yukselis.protocol.ui.story.StoryScreen
import com.yukselis.protocol.ui.theme.GameType
import com.yukselis.protocol.ui.theme.YukselisTheme
import com.yukselis.protocol.ui.title.TitleScreen
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.yukselis.protocol.ui.components.LocalAppConfiguration
import com.yukselis.protocol.ui.components.LocalAppContext
import com.yukselis.protocol.ui.components.LocalAppDensity
import com.yukselis.protocol.ui.components.LocalTips
import com.yukselis.protocol.ui.components.Tips
import kotlinx.coroutines.launch
import java.util.Locale

/** `grant`: Sistem'in vermesi gereken sekme (kendini açan arayüz); null ise her zaman açık. */
private enum class Tab(val label: Int, val icon: ImageVector, val grant: String? = null) {
    STORY(R.string.tab_story, Icons.Filled.Home),
    STATUS(R.string.tab_status, Icons.Filled.Person),
    SKILLS(R.string.tab_skills, Icons.Filled.Star, WorldState.TAB_SKILLS),
    BAG(R.string.tab_bag, Icons.Filled.Build, WorldState.TAB_BAG),
    MORE(R.string.tab_more, Icons.Filled.Menu),
}

@Composable
fun App(viewModel: GameViewModel) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    ProvideLanguage(ui.lang) {
        val dark = when (ui.settings.theme) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK, ThemeMode.AMOLED -> true
        }
        val density = LocalDensity.current
        val tips = Tips(ui.settings.seenTips) { id -> viewModel.updateSettings { it.copy(seenTips = it.seenTips + id) } }
        // Yazı boyutu ayarı cihazın yazı ölçeğinin üstüne çarpılır.
        val scaled = Density(density.density, density.fontScale * ui.settings.textSize.scale)
        CompositionLocalProvider(
            LocalDensity provides scaled,
            LocalAppDensity provides scaled,
            LocalTips provides tips,
        ) {
        YukselisTheme(dark = dark, amoled = ui.settings.theme == ThemeMode.AMOLED) {
            Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                val scene = ui.scene
                val status = ui.status
                if (ui.inGame && scene != null && status != null) {
                    GameScaffold(ui, scene, status, viewModel)
                } else {
                    TitleScreen(
                        lang = ui.lang,
                        autosave = ui.autosave,
                        onNewGame = viewModel::newGame,
                        newGamePlusUnlocked = ui.newGamePlusUnlocked,
                        onContinue = viewModel::continueGame,
                        onLanguage = viewModel::setLanguage,
                        theme = ui.settings.theme,
                        onTheme = { mode -> viewModel.updateSettings { it.copy(theme = mode) } },
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun GameScaffold(ui: UiState, scene: SceneView, status: StatusView, viewModel: GameViewModel) {
    var tab by rememberSaveable { mutableStateOf(Tab.STORY) }
    val snackbar = remember { SnackbarHostState() }

    val messageText = ui.message?.let {
        stringResource(
            when (it) {
                UiMessage.SAVED -> R.string.msg_saved
                UiMessage.LOADED -> R.string.msg_loaded
                UiMessage.LOAD_FAILED -> R.string.msg_load_failed
                UiMessage.IRONMAN_NO_MANUAL -> R.string.msg_ironman_no_manual
                UiMessage.ACTION_FAILED -> R.string.msg_action_failed
            },
        )
    }
    // Mesaj, anahtardan bağımsız bir kapsamda gösterilir; böylece mesajı tüketmek snackbar'ı kapatmaz.
    val scope = rememberCoroutineScope()
    LaunchedEffect(ui.message) {
        val message = ui.message ?: return@LaunchedEffect
        if (message == UiMessage.LOADED) tab = Tab.STORY // Kayıt yüklenince hikayeye dön.
        messageText?.let { scope.launch { snackbar.showSnackbar(it) } }
        viewModel.consumeMessage()
    }

    val tabs = Tab.entries.filter { it.grant == null || it.grant in status.tabs }
    // Verilmemiş bir sekme seçili kaldıysa (ör. eski kayıt yüklendi) Hikaye gösterilir.
    val active = if (tab in tabs) tab else Tab.STORY

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        topBar = { Header(scene, status) },
        bottomBar = {
            // Sekme çubuğu gezinmedir, okuma değil: oyun içi yazı boyutu onu büyütmez (etiketler kesilmesin).
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, d.fontScale / ui.settings.textSize.scale)) {
            NavigationBar {
                tabs.forEach { t ->
                    NavigationBarItem(
                        selected = active == t,
                        onClick = {
                            tab = t
                            viewModel.clearActionNotices()
                        },
                        icon = { Icon(t.icon, contentDescription = null) },
                        label = { Text(stringResource(t.label), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        val content = Modifier.padding(padding)
        when (active) {
            Tab.STORY -> if (ui.combat != null) CombatScreen(
                combat = ui.combat,
                notices = ui.actionNotices,
                auto = ui.autoCombat,
                showNumbers = ui.settings.combatNumbers,
                onAction = viewModel::combatAction,
                onToggleAuto = viewModel::toggleAuto,
                onCheckpoint = viewModel::returnToCheckpoint,
                modifier = content,
            ) else StoryScreen(
                scene = scene,
                freshNotices = ui.freshNotices,
                turn = ui.turn,
                typewriter = ui.settings.typewriter,
                haptics = ui.settings.haptics,
                onChoose = viewModel::choose,
                onHub = viewModel::hubAction,
                onBackToTitle = viewModel::backToTitle,
                ending = ui.ending,
                modifier = content,
            )
            Tab.STATUS -> StatusScreen(status, ui.actionNotices, viewModel::allocate, content)
            Tab.SKILLS -> ui.skills?.let { SkillsScreen(it, content) } ?: CenteredNote("", content)
            Tab.BAG -> ui.bag?.let { BagScreen(it, ui.actionNotices, viewModel::useItem, viewModel::craft, content) } ?: CenteredNote("", content)
            Tab.MORE -> MoreScreen(
                lang = ui.lang,
                settings = ui.settings,
                daily = ui.daily,
                map = ui.map,
                journal = ui.journal,
                relations = ui.relations,
                ranking = ui.ranking,
                notices = ui.actionNotices,
                onTrain = viewModel::train,
                mode = ui.game?.mode ?: GameMode.NORMAL,
                slots = ui.slots,
                onSave = viewModel::saveTo,
                onLoad = viewModel::load,
                onLanguage = viewModel::setLanguage,
                onSettings = viewModel::updateSettings,
                onBackToTitle = viewModel::backToTitle,
                onNotificationPermission = viewModel::onNotificationPermission,
                notificationDenied = ui.notificationDenied,
                modifier = content,
            )
        }
    }
    // Önce kutlama (seviye/evrim/sınıf), sonra bekleyen Sistem istemi.
    if (ui.celebration.isNotEmpty()) {
        CelebrationOverlay(ui.celebration, ui.settings.haptics, viewModel::dismissCelebration)
    } else {
        scene.prompt?.let { p -> PromptOverlay(p, ui.settings.haptics) { option -> viewModel.resolvePrompt(p.kind, p.subject, option) } }
    }
    }
}

/** Üst bilgi: gün, yer ve HP/MP çubukları. */
@Composable
private fun Header(scene: SceneView, status: StatusView) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    // Telefonda "Bölge · Alan" sığmıyor: başlık yalnızca en özel yeri gösterir (tam ad Harita karosunda).
                    text = listOfNotNull(stringResource(R.string.day_short, scene.day), scene.placeName?.substringAfterLast(" · ")).joinToString(" · "),
                    style = GameType.systemTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
                Text(
                    text = stringResource(R.string.level_grade_short, status.level, status.grade.name),
                    style = GameType.system,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            // Can ve mana yan yana: hikayeye bir satır daha kalır.
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 6.dp)) {
                StatBar(stringResource(R.string.hp), status.hp.toLong(), status.maxHp.toLong(), Color(0xFFEF5350), Modifier.weight(1f))
                StatBar(stringResource(R.string.mp), status.mp.toLong(), status.maxMp.toLong(), Color(0xFF42A5F5), Modifier.weight(1f))
            }
        }
    }
}

/**
 * Uygulama içi dil seçimi: strings.xml'i cihaz dilinden bağımsız olarak seçili dile göre çözer.
 * Android 13'ün sistem düzeyi uygulama dili ayarına gerek kalmadan anında değişir.
 */
@Composable
private fun ProvideLanguage(lang: Lang, content: @Composable () -> Unit) {
    val base = LocalContext.current
    val configuration = remember(lang, base) {
        Configuration(base.resources.configuration).apply { setLocale(Locale.forLanguageTag(lang.code)) }
    }
    val localized = remember(configuration) { base.createConfigurationContext(configuration) }
    // Yapılandırma bağlamı Activity değildir: izin isteme kaydı (Aşama 9 bildirim izni) dıştaki Activity'den taşınır.
    val registryOwner = androidx.activity.compose.LocalActivityResultRegistryOwner.current
    val provided = buildList {
        add(LocalContext provides localized)
        add(LocalConfiguration provides configuration)
        // Pencereler (diyalog, alt sayfa) bunları sıfırlar; Localized{} geri verir.
        add(LocalAppContext provides localized)
        add(LocalAppConfiguration provides configuration)
        if (registryOwner != null) add(androidx.activity.compose.LocalActivityResultRegistryOwner provides registryOwner)
    }
    CompositionLocalProvider(*provided.toTypedArray(), content = content)
}
