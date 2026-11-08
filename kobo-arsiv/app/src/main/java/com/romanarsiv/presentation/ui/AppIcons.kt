package com.romanarsiv.presentation.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * material-icons-core'da olmayan birkaç simge (Material Symbols çizimleri, 24×24).
 * Tüm "extended" simge paketini (çok büyük) eklememek için elle tanımlı.
 */
object AppIcons {
    val Download: ImageVector by lazy { icon("Download", "M5,20h14v-2H5V20zM19,9h-4V3H9v6H5l7,7L19,9z") }

    val DownloadOutlined: ImageVector by lazy {
        icon("DownloadOutlined", "M18,15v3H6v-3H4v3c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2v-3H18zM17,11l-1.41,-1.41L13,12.17V4h-2v8.17L8.41,9.59L7,11l5,5L17,11z")
    }

    val Book: ImageVector by lazy {
        icon("Book", "M18,2H6c-1.1,0 -2,0.9 -2,2v16c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2V4c0,-1.1 -0.9,-2 -2,-2zM6,4h5v8l-2.5,-1.5L6,12V4z")
    }

    val DarkMode: ImageVector by lazy {
        icon(
            "DarkMode",
            "M12,3c-4.97,0 -9,4.03 -9,9s4.03,9 9,9s9,-4.03 9,-9c0,-0.46 -0.04,-0.92 -0.1,-1.36c-0.98,1.37 -2.58,2.26 -4.4,2.26" +
                "c-2.98,0 -5.4,-2.42 -5.4,-5.4c0,-1.81 0.89,-3.42 2.26,-4.4C12.92,3.04 12.46,3 12,3z",
        )
    }

    val Language: ImageVector by lazy {
        icon(
            "Language",
            "M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10s10,-4.48 10,-10S17.52,2 12,2zM11,19.93c-3.95,-0.49 -7,-3.85 -7,-7.93" +
                "c0,-0.62 0.08,-1.21 0.21,-1.79L9,15v1c0,1.1 0.9,2 2,2v1.93zM17.9,17.39c-0.26,-0.81 -1,-1.39 -1.9,-1.39h-1v-3" +
                "c0,-0.55 -0.45,-1 -1,-1H8v-2h2c0.55,0 1,-0.45 1,-1V7h2c1.1,0 2,-0.9 2,-2v-0.41c2.93,1.19 5,4.06 5,7.41" +
                "c0,2.08 -0.8,3.97 -2.1,5.39z",
        )
    }

    val Folder: ImageVector by lazy {
        icon("Folder", "M10,4H4c-1.1,0 -1.99,0.9 -1.99,2L2,18c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V8c0,-1.1 -0.9,-2 -2,-2h-8l-2,-2z")
    }

    val Speed: ImageVector by lazy {
        icon(
            "Layers",
            "M11.99,18.54l-7.37,-5.73L3,14.07l9,7 9,-7 -1.63,-1.27 -7.38,5.74zM12,16l7.36,-5.73L21,9l-9,-7 -9,7 1.63,1.27L12,16z",
        )
    }

    val Palette: ImageVector by lazy {
        icon(
            "Palette",
            "M12,3c-4.97,0 -9,4.03 -9,9s4.03,9 9,9c0.83,0 1.5,-0.67 1.5,-1.5c0,-0.39 -0.15,-0.74 -0.39,-1.01" +
                "c-0.23,-0.26 -0.38,-0.61 -0.38,-0.99c0,-0.83 0.67,-1.5 1.5,-1.5H16c2.76,0 5,-2.24 5,-5C21,6.58 16.97,3 12,3z" +
                "M6.5,12c-0.83,0 -1.5,-0.67 -1.5,-1.5S5.67,9 6.5,9S8,9.67 8,10.5S7.33,12 6.5,12zM9.5,8C8.67,8 8,7.33 8,6.5" +
                "S8.67,5 9.5,5S11,5.67 11,6.5S10.33,8 9.5,8zM14.5,8c-0.83,0 -1.5,-0.67 -1.5,-1.5S13.67,5 14.5,5S16,5.67 16,6.5" +
                "S15.33,8 14.5,8zM17.5,12c-0.83,0 -1.5,-0.67 -1.5,-1.5S16.67,9 17.5,9S19,9.67 19,10.5S18.33,12 17.5,12z",
        )
    }

    private fun icon(name: String, path: String): ImageVector =
        ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .addPath(pathData = addPathNodes(path), fill = SolidColor(Color.Black))
            .build()
}
