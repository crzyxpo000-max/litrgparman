package com.upvotesaver.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.upvotesaver.app.ui.MainScreen
import com.upvotesaver.app.ui.UpvoteSaverTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleRedirect(intent)
        setContent {
            val state = viewModel.state.collectAsStateWithLifecycle().value
            UpvoteSaverTheme {
                MainScreen(viewModel, state)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleRedirect(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun handleRedirect(intent: Intent?) {
        val uri = intent?.data ?: return
        if (intent.action == Intent.ACTION_VIEW && uri.scheme == "upvotesaver") viewModel.onAuthRedirect(uri)
    }
}
