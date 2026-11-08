package com.romanarsiv

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romanarsiv.background.ArchiveNotifications
import com.romanarsiv.presentation.MainViewModel
import com.romanarsiv.presentation.Tab
import com.romanarsiv.presentation.ui.ArchiverTheme
import com.romanarsiv.presentation.ui.MainScreen
import com.romanarsiv.presentation.ui.isAppInDarkTheme
import com.romanarsiv.settings.AppPreferences

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels { MainViewModel.Factory }

    /** Uygulama içinden seçilen dil (Ayarlar → Dil); değişince Activity yeniden oluşturulur. */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppPreferences.localize(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Yalnızca ilk açılışta oku (döndürmede kaybolmasın diye MainScreen saklar).
        val crashReport = if (savedInstanceState == null) CrashLog.takeLast(this) else null
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            val prefs by viewModel.prefs.collectAsStateWithLifecycle()
            val dark = isAppInDarkTheme(prefs.theme)
            // Uygulama teması sistemden farklı olabilir: durum çubuğu simgeleri de ona uysun.
            DisposableEffect(dark) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            ArchiverTheme(prefs) {
                MainScreen(viewModel, crashReport, onLanguageChanged = ::applyLanguage)
            }
        }
    }

    private fun applyLanguage() {
        ArchiveNotifications.createChannels(AppPreferences.localize(applicationContext))
        recreate()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(ArchiveNotifications.EXTRA_OPEN_DOWNLOADS, false) == true) {
            viewModel.closeSelection()
            viewModel.selectTab(Tab.DOWNLOADS)
        }
    }
}
