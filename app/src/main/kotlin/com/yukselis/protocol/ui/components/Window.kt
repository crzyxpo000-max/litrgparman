package com.yukselis.protocol.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yukselis.protocol.ui.theme.Game
import com.yukselis.protocol.ui.theme.GameShapes
import com.yukselis.protocol.ui.theme.GameType
import com.yukselis.protocol.ui.theme.windowCut

/**
 * Sistem penceresi: kesik köşeli cam panel, ince kenar ve (koyu temada) çok soluk tarama çizgileri.
 * Tarama çizgileri metnin altında kalır, opaklığı %4'tür: okumayı bozmaz.
 */
@Composable
fun Modifier.systemWindow(accent: Color = Game.colors.edge, cut: Int = 12, fill: Color = Game.colors.panel): Modifier {
    val shape = windowCut(cut)
    val scan = if (Game.colors.isDark) Game.colors.system.copy(alpha = 0.04f) else Color.Transparent
    return this
        .clip(shape)
        .background(fill)
        .drawBehind {
            if (scan.alpha > 0f) {
                val step = 3.dp.toPx()
                var y = 0f
                while (y < size.height) {
                    drawRect(scan, topLeft = Offset(0f, y), size = Size(size.width, 1f))
                    y += step
                }
            }
        }
        .border(1.dp, accent, shape)
}

/**
 * Yankı'nın satırı: ters video blok (koyu temada açık zemin, koyu yazı) ve kanal kayması gibi iki ince çizgi.
 * Sistem'in kusursuz penceresine sızan tek "yanlış" şey budur.
 */
@Composable
fun EchoLine(text: String, modifier: Modifier = Modifier, style: TextStyle = GameType.system) {
    val g = Game.colors
    Text(
        text = text,
        style = style,
        color = g.echoInk,
        modifier = modifier
            .drawBehind {
                drawRect(g.echo, topLeft = Offset(size.width, 0f), size = Size(3.dp.toPx(), size.height))
                drawRect(Color(0xFF2C6BFF), topLeft = Offset(-1.dp.toPx(), 0f), size = Size(1.dp.toPx(), size.height))
            }
            .background(g.echoPaper)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

/** Dolu düğme: pencere köşesi (Material'in hap biçimi yerine). */
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = GameShapes.button,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit,
) = androidx.compose.material3.Button(
    onClick = onClick, modifier = modifier, enabled = enabled, shape = shape, colors = colors,
    border = border, contentPadding = contentPadding, content = content,
)

/** Çerçeveli düğme: pencere köşesi, ince çizgi. */
@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = GameShapes.button,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    border: BorderStroke? = BorderStroke(1.dp, if (enabled) Game.colors.outlineFor() else Game.colors.line),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit,
) = androidx.compose.material3.OutlinedButton(
    onClick = onClick, modifier = modifier, enabled = enabled, shape = shape, colors = colors,
    border = border, contentPadding = contentPadding, content = content,
)

private fun com.yukselis.protocol.ui.theme.GameColors.outlineFor(): Color = if (isDark) Color(0xFF2E4566) else Color(0xFF8A9AB0)

/** İnce dikey vurgu çizgisi (sezgi, yoldaş görüşü). */
fun Modifier.leftRule(color: Color, width: Dp = 2.dp): Modifier =
    drawBehind { drawRect(color, size = Size(width.toPx(), size.height)) }
