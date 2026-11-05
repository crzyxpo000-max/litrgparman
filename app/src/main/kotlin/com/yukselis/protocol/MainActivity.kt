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
        setContent { App(viewModel) }
    }

    override fun onStop() {
        super.onStop()
        // GDD: uygulama arka plana alındığında otomatik kayıt.
        viewModel.autosave()
    }
}
