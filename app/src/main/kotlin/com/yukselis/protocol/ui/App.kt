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
import com.yukselis.protocol.ui.status.StatusScreen
import com.yukselis.protocol.ui.story.StoryScreen
import com.yukselis.protocol.ui.theme.GameType
import com.yukselis.protocol.ui.theme.YukselisTheme
import com.yukselis.protocol.ui.title.TitleScreen
import kotlinx.coroutines.launch
import java.util.Locale

private enum class Tab(val label: Int, val icon: ImageVector) {
    STORY(R.string.tab_story, Icons.Filled.Home),
    STATUS(R.string.tab_status, Icons.Filled.Person),
    SKILLS(R.string.tab_skills, Icons.Filled.Star),
    BAG(R.string.tab_bag, Icons.Filled.Build),
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

    Scaffold(
        topBar = { Header(scene, status) },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = null) },
                        label = { Text(stringResource(t.label), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        val content = Modifier.padding(padding)
        when (tab) {
            Tab.STORY -> StoryScreen(
                scene = scene,
                freshNotices = ui.freshNotices,
                turn = ui.turn,
                typewriter = ui.settings.typewriter,
                haptics = ui.settings.haptics,
                onChoose = viewModel::choose,
                onBackToTitle = viewModel::backToTitle,
                modifier = content,
            )
            Tab.STATUS -> StatusScreen(status, content)
            Tab.SKILLS -> CenteredNote(stringResource(R.string.skills_placeholder), content)
            Tab.BAG -> CenteredNote(stringResource(R.string.bag_placeholder), content)
            Tab.MORE -> MoreScreen(
                lang = ui.lang,
                settings = ui.settings,
                mode = ui.game?.mode ?: GameMode.NORMAL,
                slots = ui.slots,
                onSave = viewModel::saveTo,
                onLoad = viewModel::load,
                onLanguage = viewModel::setLanguage,
                onSettings = viewModel::updateSettings,
                onBackToTitle = viewModel::backToTitle,
                modifier = content,
            )
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
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = listOfNotNull(stringResource(R.string.day_short, scene.day), scene.placeName).joinToString(" · "),
                    style = GameType.systemTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.level_grade, status.level, status.grade.name),
                    style = GameType.system,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            StatBar(stringResource(R.string.hp), status.hp.toLong(), status.maxHp.toLong(), Color(0xFFEF5350), Modifier.padding(top = 6.dp))
            StatBar(stringResource(R.string.mp), status.mp.toLong(), status.maxMp.toLong(), Color(0xFF42A5F5), Modifier.padding(top = 4.dp))
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
    CompositionLocalProvider(
        LocalContext provides localized,
        LocalConfiguration provides configuration,
        content = content,
    )
}
