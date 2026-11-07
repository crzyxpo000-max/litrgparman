package com.yukselis.protocol.ui.system

import androidx.compose.ui.semantics.Role
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yukselis.engine.story.NoticeView
import com.yukselis.engine.story.PromptOptionView
import com.yukselis.engine.story.PromptView
import com.yukselis.protocol.R
import com.yukselis.protocol.ui.theme.EchoColor
import com.yukselis.protocol.ui.theme.GameType
import com.yukselis.protocol.ui.theme.noticeColor
import com.yukselis.protocol.ui.theme.rarityColor

/** Giriş animasyonu: hafif büyüyerek ve belirerek gelir; geldiğinde titreşim. */
@Composable
private fun rememberEntrance(key: Any, haptics: Boolean): Pair<Float, Float> {
    val scale = remember(key) { Animatable(0.85f) }
    val alpha = remember(key) { Animatable(0f) }
    val feedback = LocalHapticFeedback.current
    LaunchedEffect(key) {
        if (haptics) feedback.performHapticFeedback(HapticFeedbackType.LongPress)
        alpha.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(key) {
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    return scale.value to alpha.value
}

/**
 * Sistem istemi (sınıf teklifi, evrim, meslek, yetenek evrimi, kaynaştırma, Öz bağlama).
 * Tam ekran; seçim yapılmadan kapanmaz. Seçenek kartlarına dokunmak seçer.
 */
@Composable
fun PromptOverlay(prompt: PromptView, haptics: Boolean, onChoose: (String) -> Unit) {
    val (scale, alpha) = rememberEntrance(prompt.title, haptics)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.93f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
            .safeDrawingPadding(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
                .scale(scale)
                .alpha(alpha),
        ) {
            Text(
                text = "[ ${stringResource(R.string.system_header)} ]",
                style = GameType.system,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Text(
                text = prompt.title,
                style = GameType.systemTitle.copy(fontSize = 20.sp),
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            prompt.lines.forEach {
                Text(it, style = GameType.system, color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(4.dp))
            prompt.options.forEach { OptionCard(it) { onChoose(it.id) } }
        }
    }
}

@Composable
private fun OptionCard(option: PromptOptionView, onClick: () -> Unit) {
    val accent = option.rarity?.let { rarityColor(it) } ?: MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(8.dp)
    Surface(
        color = Color(0xFF0E1424),
        shape = shape,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.5.dp, accent, shape)
            // TalkBack dokununca ne olacağını söylesin (QA P2-3).
            .clickable(onClickLabel = stringResource(R.string.a11y_choose), role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = (listOfNotNull(option.name, option.description, option.echoNote) + option.details).joinToString(" ")
            },
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(option.name, style = GameType.systemTitle.copy(fontSize = 16.sp), color = accent)
            if (option.description.isNotBlank()) {
                Text(option.description, style = GameType.narrationPast, color = Color.White.copy(alpha = 0.9f))
            }
            option.echoNote?.let { Text(it, style = GameType.system, color = EchoColor) }
            option.details.forEach { d -> Text(d, style = GameType.system, color = Color.White.copy(alpha = 0.65f)) }
        }
    }
}

/**
 * Seviye atlama, evrim ve sınıf anları için tam ekran kart (GDD: "kendine ait bir ekran anı").
 * Dokununca kapanır.
 */
@Composable
fun CelebrationOverlay(notices: List<NoticeView>, haptics: Boolean, onDismiss: () -> Unit) {
    val main = notices.first()
    val (scale, alpha) = rememberEntrance(notices, haptics)
    val accent = noticeColor(main.kind, main.rarity)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.93f))
            .clickable(onClickLabel = stringResource(R.string.a11y_dismiss), role = Role.Button, onClick = onDismiss)
            .safeDrawingPadding()
            .semantics { contentDescription = notices.joinToString(". ") { (listOf(it.title) + it.lines).joinToString(". ") } },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(28.dp)
                .scale(scale)
                .alpha(alpha),
        ) {
            notices.forEach { n ->
                val c = noticeColor(n.kind, n.rarity)
                Text(
                    text = n.title,
                    style = GameType.systemTitle.copy(fontSize = if (n === main) 26.sp else 18.sp, fontWeight = FontWeight.Bold),
                    color = c,
                    textAlign = TextAlign.Center,
                )
                n.lines.forEach { Text(it, style = GameType.system, color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Center) }
            }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.tap_to_continue), style = GameType.system, color = accent.copy(alpha = 0.7f))
        }
    }
}
