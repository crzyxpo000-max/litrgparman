package com.yukselis.protocol.ui.system

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yukselis.engine.story.NoticeKind
import com.yukselis.engine.story.NoticeMoment
import com.yukselis.engine.story.NoticeView
import com.yukselis.engine.story.PromptOptionView
import com.yukselis.engine.story.PromptView
import com.yukselis.engine.system.Grade
import com.yukselis.engine.system.Rarity
import com.yukselis.protocol.R
import com.yukselis.protocol.ui.components.EchoLine
import com.yukselis.protocol.ui.components.systemWindow
import com.yukselis.protocol.ui.theme.Game
import com.yukselis.protocol.ui.theme.GameMotion
import com.yukselis.protocol.ui.theme.GameType
import com.yukselis.protocol.ui.theme.glitch
import com.yukselis.protocol.ui.theme.noticeColor
import com.yukselis.protocol.ui.theme.rarityColorOf
import kotlinx.coroutines.delay
import kotlin.random.Random

/*
 * Büyük anlar (docs/UI_DIRECTION.md, "Kırık pencere"): her biri kendi ekranında, kendi hareketiyle.
 * Seviye: pencere açılır, sayı sayar, satırlar sırayla yanar. Evrim: kademe harfi eskisini ezer. Benzersiz: beyaz-altın
 * pencere ve tek bir ışık geçişi. Sıralama: eski sıranın üstü çizilir. Sınır kırılması: ekran çöker, sonra fark yazılır.
 * Cihazda animasyonlar kapalıysa (Game.reduceMotion) her şey son haliyle, hareketsiz gelir. Dokununca kapanır.
 */

/** Büyük anın türü: kartın biçimini seçer. */
enum class Moment { LEVEL, EVOLUTION, CLASS, UNIQUE, RANK, BREACH }

fun momentOf(n: NoticeView): Moment? = when {
    n.moment == NoticeMoment.REVEAL || n.moment == NoticeMoment.CRUSH -> Moment.BREACH
    n.moment == NoticeMoment.RANK_UP -> Moment.RANK
    n.rarity == Rarity.UNIQUE && (n.kind == NoticeKind.SKILL || n.kind == NoticeKind.EVOLUTION) -> Moment.UNIQUE
    n.kind == NoticeKind.EVOLUTION && n.to != null -> Moment.EVOLUTION
    n.kind == NoticeKind.EVOLUTION -> Moment.CLASS
    n.kind == NoticeKind.CLASS -> Moment.CLASS
    n.kind == NoticeKind.LEVEL_UP -> Moment.LEVEL
    else -> null
}

/** Öncelik: aynı anda birden çok büyük an varsa en çarpıcısı kartın başına geçer. */
private val PRIORITY = listOf(Moment.BREACH, Moment.EVOLUTION, Moment.UNIQUE, Moment.CLASS, Moment.LEVEL, Moment.RANK)

/** Açılış ilerlemesi 0→1 (pencere açılması) ve titreşim. Hareket kapalıysa hemen 1. */
@Composable
private fun rememberOpening(key: Any, haptics: Boolean, durationMs: Int = 260, delayMs: Long = 0): Float {
    val reduce = Game.reduceMotion
    val p = remember(key) { Animatable(if (reduce) 1f else 0f) }
    val feedback = LocalHapticFeedback.current
    LaunchedEffect(key) {
        if (haptics) feedback.performHapticFeedback(HapticFeedbackType.LongPress)
        if (!reduce) {
            delay(delayMs)
            p.animateTo(1f, tween(durationMs, easing = FastOutSlowInEasing))
        }
    }
    return p.value
}

/** Pencerenin "açılması": yatay bir çizgi dikeyde genişler. */
private fun Modifier.opening(progress: Float): Modifier = graphicsLayer {
    scaleY = 0.02f + 0.98f * progress
    alpha = (progress * 3f).coerceAtMost(1f)
}

/**
 * Sistem istemi (sınıf teklifi, evrim, meslek, yetenek evrimi, kaynaştırma, Öz bağlama).
 * Tam ekran; seçim yapılmadan kapanmaz. Seçenek pencerelerine dokunmak seçer.
 */
@Composable
fun PromptOverlay(prompt: PromptView, haptics: Boolean, onChoose: (String) -> Unit) {
    val open = rememberOpening(prompt.title, haptics)
    val locale = LocalConfiguration.current.locales[0]
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Game.colors.night)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
            .safeDrawingPadding(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
                .opening(open),
        ) {
            Text(
                text = "[ ${stringResource(R.string.system_header).uppercase(locale)} ]",
                style = GameType.systemTitle.copy(fontSize = 12.sp),
                color = Game.colors.system,
            )
            Text(text = prompt.title, style = GameType.heading.copy(fontSize = 24.sp, lineHeight = 30.sp), color = MaterialTheme.colorScheme.onSurface)
            prompt.lines.forEach { Text(it, style = GameType.system, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)) }
            Spacer(Modifier.height(4.dp))
            prompt.options.forEach { OptionCard(it) { onChoose(it.id) } }
        }
    }
}

@Composable
private fun OptionCard(option: PromptOptionView, onClick: () -> Unit) {
    val accent = option.rarity?.let { rarityColorOf(it) } ?: Game.colors.edge
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .systemWindow(accent = accent)
            // TalkBack dokununca ne olacağını söylesin (QA P2-3).
            .clickable(onClickLabel = stringResource(R.string.a11y_choose), role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = (listOfNotNull(option.name, option.description, option.echoNote) + option.details).joinToString(" ")
            }
            .padding(14.dp),
    ) {
        Text(option.name, style = GameType.heading.copy(fontSize = 17.sp, lineHeight = 22.sp), color = accent)
        if (option.description.isNotBlank()) {
            Text(option.description, style = GameType.narrationPast, color = MaterialTheme.colorScheme.onSurface)
        }
        option.echoNote?.let { EchoLine(it) }
        option.details.forEach { d -> Text(d, style = GameType.system, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

/**
 * Büyük an kartı: seviye, evrim, sınıf, Benzersiz yetenek, Küresel Sıralama, sınır kırılması.
 * Aynı anın yetenek/unvan/Yankı bildirimleri ana pencerenin altında küçük satırlar olarak gelir. Dokununca kapanır.
 */
@Composable
fun CelebrationOverlay(notices: List<NoticeView>, haptics: Boolean, onDismiss: () -> Unit) {
    val main = notices.minByOrNull { n -> momentOf(n)?.let { PRIORITY.indexOf(it) } ?: PRIORITY.size } ?: return
    val moment = momentOf(main) ?: Moment.LEVEL
    val rest = notices.filter { it !== main }
    val spoken = notices.joinToString(". ") { (listOf(it.title) + it.lines).joinToString(". ") }
    // Sınır kırılması önce ekranı bozar; pencere ancak bozulma bitince açılır.
    val crashMs = if (moment == Moment.BREACH && !Game.reduceMotion) CRASH_MS else 0L
    val open = rememberOpening(notices, haptics, delayMs = crashMs)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Game.colors.night)
            .clickable(onClickLabel = stringResource(R.string.a11y_dismiss), role = Role.Button, onClick = onDismiss)
            .safeDrawingPadding()
            .semantics {
                contentDescription = spoken
                liveRegion = LiveRegionMode.Polite
            },
    ) {
        if (moment == Moment.BREACH) Crash(notices)
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .clearAndSetSemantics { },
        ) {
            MainWindow(main, moment, open)
            rest.forEachIndexed { i, n -> Follower(n, open, i) }
            Text(
                stringResource(R.string.tap_to_continue),
                style = GameType.system,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alpha(open).padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun MainWindow(n: NoticeView, moment: Moment, open: Float) {
    val g = Game.colors
    val locale = LocalConfiguration.current.locales[0]
    val accent = when (moment) {
        Moment.LEVEL, Moment.RANK -> g.edge
        Moment.EVOLUTION -> g.amber
        Moment.CLASS -> g.violet
        Moment.UNIQUE -> g.unique
        Moment.BREACH -> g.echo
    }
    val header = when (moment) {
        Moment.BREACH -> stringResource(R.string.moment_error)
        Moment.RANK -> stringResource(R.string.moment_rank)
        Moment.UNIQUE -> stringResource(R.string.moment_unique)
        else -> stringResource(R.string.system_header)
    }
    // Sınır kırılmasında pencere yerinden kaymış durur: düzen bozuldu.
    val shift = if (moment == Moment.BREACH) 6.dp else 0.dp
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .offset(x = shift)
            .fillMaxWidth()
            .opening(open)
            .systemWindow(accent = accent, fill = if (moment == Moment.UNIQUE) g.unique.copy(alpha = 0.07f).compositeOn(g.panel) else g.panel)
            .then(if (moment == Moment.UNIQUE) Modifier.sweep(g.unique, open) else Modifier)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Text("[ ${header.uppercase(locale)} ]", style = GameType.systemTitle.copy(fontSize = 12.sp), color = accent)
        when (moment) {
            Moment.LEVEL -> {
                val to = n.to
                Text(n.title, style = GameType.heading, color = g.system)
                if (to != null) {
                    CountUp(from = n.from ?: (to - 1), to = to, run = open >= 1f)
                    n.from?.let { Text(stringResource(R.string.moment_from, it), style = GameType.system, color = g.muted) }
                }
                StaggeredLines(n.lines.drop(if (to != null) 1 else 0), open)
            }
            Moment.EVOLUTION -> {
                val to = n.to?.let { Grade.entries.getOrNull(it) }
                val from = n.from?.let { Grade.entries.getOrNull(it) }
                Row(verticalAlignment = Alignment.Bottom) {
                    // Eski kademe soluk durur; üstünü çizmek harfi (D → Đ) başka bir harfe benzetir.
                    from?.let {
                        Text(
                            "${it.name} →",
                            style = GameType.numeral,
                            color = g.muted,
                            modifier = Modifier.padding(end = 14.dp, bottom = 10.dp),
                        )
                    }
                    to?.let { Text(it.name, style = GameType.numeralHuge, color = g.amber, modifier = Modifier.punch(open)) }
                }
                Text(n.title, style = GameType.heading, color = g.text)
                StaggeredLines(n.lines, open)
            }
            Moment.CLASS, Moment.UNIQUE -> {
                Text(n.title, style = GameType.heading.copy(fontSize = 24.sp, lineHeight = 30.sp), color = if (moment == Moment.UNIQUE) g.unique else g.text)
                StaggeredLines(n.lines, open)
            }
            Moment.RANK -> {
                n.from?.takeIf { it > 0 }?.let {
                    Text("#$it", style = GameType.heading, color = g.muted, textDecoration = TextDecoration.LineThrough)
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("#${n.to ?: ""}", style = GameType.numeral, color = g.text)
                    val from = n.from
                    val to = n.to
                    if (from != null && to != null && from > to) {
                        Text("▲ ${from - to}", style = GameType.systemTitle, color = g.amber, modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
                    }
                }
            }
            Moment.BREACH -> {
                Text(n.title, style = GameType.systemTitle, color = g.text)
                val from = n.from
                val to = n.to
                if (from != null && to != null) {
                    Text("− ${stringResource(R.string.moment_public)}  $from", style = GameType.system.copy(fontSize = 15.sp), color = g.danger)
                    Text("+ ${stringResource(R.string.moment_true)}  $to", style = GameType.system.copy(fontSize = 15.sp), color = g.success)
                    Text("$to", style = GameType.numeralHuge, color = g.text, modifier = Modifier.punch(open))
                }
                StaggeredLines(n.lines, open)
            }
        }
    }
}

/** Anın yan bildirimi: Yankı ters video satırdır, ötekiler ince bir pencere. Ana pencereden sonra sırayla gelir. */
@Composable
private fun Follower(n: NoticeView, open: Float, index: Int) {
    val shown = stagger(open, index + 2)
    val mod = Modifier.alpha(shown)
    if (n.kind == NoticeKind.ECHO) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = mod) {
            EchoLine(glitch(n.title))
            n.lines.forEach { EchoLine(it) }
        }
        return
    }
    val accent = noticeColor(n.kind, n.rarity)
    Column(
        modifier = mod
            .fillMaxWidth()
            .systemWindow(accent = accent.copy(alpha = 0.8f), cut = 8)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(n.title, style = GameType.systemTitle, color = accent)
        n.lines.forEach { Text(it, style = GameType.system, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f)) }
    }
}

/** Satırlar sırayla yanar: her biri bir öncekinden kısa süre sonra. */
@Composable
private fun StaggeredLines(lines: List<String>, open: Float) {
    lines.forEachIndexed { i, line ->
        Text(line, style = GameType.system, color = Game.colors.text, modifier = Modifier.alpha(stagger(open, i)))
    }
}

/** Açılışın son bölümünde i. öğenin görünürlüğü (0→1). Hareket kapalıysa hep 1. */
@Composable
private fun stagger(open: Float, i: Int): Float {
    if (Game.reduceMotion) return 1f
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(open >= 1f) {
        if (open >= 1f) {
            delay(STAGGER_MS * i)
            reveal.animateTo(1f, tween(GameMotion.BASE))
        }
    }
    return reveal.value
}

/** Sayı eski değerden yenisine hızla döner. */
@Composable
private fun CountUp(from: Int, to: Int, run: Boolean) {
    val reduce = Game.reduceMotion
    var value by remember(from, to) { mutableIntStateOf(if (reduce) to else from) }
    LaunchedEffect(run, from, to) {
        if (run && !reduce) {
            val anim = Animatable(from.toFloat())
            anim.animateTo(to.toFloat(), tween(GameMotion.SLOW + 240, easing = LinearEasing)) { value = this.value.toInt() }
            value = to
        }
    }
    Text("$value", style = GameType.numeralHuge, color = Game.colors.text)
}

/** Kademe harfi ve gerçek seviye "çarparak" gelir: büyükten yerine oturur. */
private fun Modifier.punch(open: Float): Modifier = graphicsLayer {
    val s = 1f + (1f - open) * 0.6f
    scaleX = s
    scaleY = s
}

/** Benzersiz: pencerenin üstünden bir kez geçen ışık çizgisi. */
@Composable
private fun Modifier.sweep(color: Color, open: Float): Modifier {
    val reduce = Game.reduceMotion
    val x = remember { Animatable(if (reduce) 2f else -0.2f) }
    LaunchedEffect(open >= 1f) {
        if (open >= 1f && !reduce) x.animateTo(1.2f, tween(GameMotion.MOMENT, easing = FastOutSlowInEasing))
    }
    val pos = x.value
    return drawWithContent {
        drawContent()
        if (pos in -0.2f..1.2f) {
            val w = 3.dp.toPx()
            drawRect(color.copy(alpha = 0.55f), topLeft = Offset(size.width * pos, 0f), size = Size(w, size.height))
            drawRect(color.copy(alpha = 0.18f), topLeft = Offset(size.width * pos - 4 * w, 0f), size = Size(4 * w, size.height))
        }
    }
}

/**
 * Sınır kırılmasının bozulması: ekranı rastgele blok karakterler ve macenta yırtıklar kaplar, sonra çekilir.
 * Ekran okuyucu bunu görmez (dekoratif); anlam pencerede ve tek bir duyuruda.
 */
@Composable
private fun Crash(notices: List<NoticeView>) {
    if (Game.reduceMotion) return
    val g = Game.colors
    var tick by remember(notices) { mutableIntStateOf(0) }
    var visible by remember(notices) { mutableStateOf(true) }
    LaunchedEffect(notices) {
        val frames = (CRASH_MS / CRASH_FRAME_MS).toInt()
        repeat(frames) {
            delay(CRASH_FRAME_MS)
            tick++
        }
        visible = false
    }
    if (!visible) return
    val rnd = Random(tick * 31 + 7)
    BoxWithConstraints(Modifier.fillMaxSize().clearAndSetSemantics { }) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            repeat(48) {
                Text(junk(rnd, 34), style = GameType.system, color = g.system.copy(alpha = 0.25f + rnd.nextFloat() * 0.35f), maxLines = 1)
            }
        }
        val h = maxHeight
        repeat(3) {
            Box(
                Modifier
                    .offset(y = h * rnd.nextFloat())
                    .fillMaxWidth()
                    .height((1 + rnd.nextInt(3)).dp)
                    .background(g.echo.copy(alpha = 0.8f)),
            )
        }
    }
}

private const val JUNK = "▓▒░█▌▐¶§#<>/\\|=+-0123456789abcdefABCDEF?!⌁"

private fun junk(rnd: Random, n: Int) = buildString { repeat(n) { append(JUNK[rnd.nextInt(JUNK.length)]) } }

private fun Color.compositeOn(bg: Color): Color = Color(
    red = red * alpha + bg.red * (1 - alpha),
    green = green * alpha + bg.green * (1 - alpha),
    blue = blue * alpha + bg.blue * (1 - alpha),
    alpha = 1f,
)

private const val CRASH_MS = 700L
private const val CRASH_FRAME_MS = 60L
private const val STAGGER_MS = 90L
