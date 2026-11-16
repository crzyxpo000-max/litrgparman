package com.upvotesaver.app.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Light = lightColorScheme(
    primary = Color(0xFFC2410C),
    secondary = Color(0xFFB7791F),
    background = Color(0xFFFBF8F3),
    surface = Color(0xFFFBF8F3),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFFFB59A),
    secondary = Color(0xFFE8A33D),
)

@Composable
fun UpvoteSaverTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        dark -> Dark
        else -> Light
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
