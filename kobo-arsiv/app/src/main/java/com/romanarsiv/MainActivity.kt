package com.romanarsiv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.romanarsiv.presentation.MainViewModel
import com.romanarsiv.presentation.ui.ArchiverTheme
import com.romanarsiv.presentation.ui.MainScreen

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels { MainViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            ArchiverTheme {
                MainScreen(viewModel)
            }
        }
    }
}
