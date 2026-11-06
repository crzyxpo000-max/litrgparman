package com.yukselis.protocol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.yukselis.protocol.ui.App
import com.yukselis.protocol.ui.GameViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Süreç yeniden kurulduysa ya da "Son kullanılanlar"dan açıldıysa eski bildirim intent'i yeniden işlenmez.
        val fromHistory = (intent?.flags ?: 0) and android.content.Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0
        if (savedInstanceState == null && !fromHistory) handleNotification(intent)
        setContent { App(viewModel) }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleNotification(intent)
    }

    /** Bildirime dokunuldu: oyun açılır, bildirimin olayı bir sonraki gün akışında gelir. */
    private fun handleNotification(intent: android.content.Intent?) {
        val note = intent?.getStringExtra(com.yukselis.protocol.data.Notifications.EXTRA_NOTE) ?: return
        intent.removeExtra(com.yukselis.protocol.data.Notifications.EXTRA_NOTE)
        viewModel.openFromNotification(note)
    }

    override fun onStop() {
        super.onStop()
        // GDD: uygulama arka plana alındığında otomatik kayıt.
        viewModel.autosave()
    }
}
