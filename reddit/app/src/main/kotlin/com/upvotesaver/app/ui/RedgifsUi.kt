package com.upvotesaver.app.ui

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.upvotesaver.app.Graph
import com.upvotesaver.app.MainViewModel
import com.upvotesaver.app.NicheState
import com.upvotesaver.app.R
import com.upvotesaver.app.UiState
import com.upvotesaver.core.Redgifs
import com.upvotesaver.core.RedgifsGif

private const val REDGIFS_URL = "https://www.redgifs.com/"

/** What the in-app browser is doing, shown on the sign-in screen so a blank page can be diagnosed. */
data class BrowserStatus(val progress: Int = 0, val error: String? = null)

/**
 * The Redgifs site in an in-app browser. The app never sees the password: it only notes the bearer
 * token the site itself sends to api.redgifs.com once the user is signed in.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RedgifsBrowser(
    onToken: (String) -> Unit,
    modifier: Modifier = Modifier,
    reloadKey: Int = 0,
    onStatus: (BrowserStatus) -> Unit = {},
) {
    var status = BrowserStatus()
    fun report(change: (BrowserStatus) -> BrowserStatus) {
        status = change(status)
        onStatus(status)
    }
    key(reloadKey) {
        AndroidView(
            modifier = modifier,
            factory = { context ->
                val main = Handler(Looper.getMainLooper())
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.userAgentString = Graph.redgifsUserAgent
                    // The sign-in page lives on another Redgifs address and needs its cookies.
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView, newProgress: Int) = report { it.copy(progress = newProgress) }

                        override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                            if (message.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                                report { it.copy(error = it.error ?: "JS: ${message.message().take(160)}") }
                            }
                            return true
                        }
                    }
                    webViewClient = object : WebViewClient() {
                        // Runs on a background thread for every request the page makes.
                        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                            if (request.url.host == "api.redgifs.com") {
                                val header = request.requestHeaders.entries
                                    .firstOrNull { it.key.equals("Authorization", ignoreCase = true) }?.value
                                if (header != null && header.startsWith("Bearer ", ignoreCase = true)) {
                                    val token = header.substring(7).trim()
                                    main.post { onToken(token) }
                                }
                            }
                            return null
                        }

                        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                            if (request.isForMainFrame) report { it.copy(error = "${error.errorCode}: ${error.description}") }
                        }

                        override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                            if (request.isForMainFrame) report { it.copy(error = "HTTP ${response.statusCode}") }
                        }
                    }
                    loadUrl(REDGIFS_URL)
                }
            },
            onRelease = { it.destroy() },
        )
    }
}

/** Full-screen sign-in inside the activity. Closes by itself once a signed-in token is seen. */
@Composable
fun RedgifsLoginScreen(viewModel: MainViewModel) {
    var status by remember { mutableStateOf(BrowserStatus()) }
    var reloadKey by remember { mutableIntStateOf(0) }
    BackHandler { viewModel.openRedgifsLogin(false) }
    Surface(Modifier.fillMaxSize().systemBarsPadding()) {
        Column {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.rg_login_hint), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { status = BrowserStatus(); reloadKey++ }) { Text(stringResource(R.string.reload)) }
                TextButton(onClick = { viewModel.openRedgifsLogin(false) }) { Text(stringResource(R.string.close)) }
            }
            if (status.progress in 0..99) LinearProgressIndicator(progress = { status.progress / 100f }, modifier = Modifier.fillMaxWidth())
            status.error?.let {
                Text(it, Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            RedgifsBrowser(viewModel::onRedgifsToken, Modifier.fillMaxSize(), reloadKey) { status = it }
        }
    }
}

@Composable
fun RedgifsCard(viewModel: MainViewModel, state: UiState, start: (() -> Unit) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.rg_title), style = MaterialTheme.typography.titleMedium)
            when {
                state.redgifsSignedIn -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.rg_signed_in, state.redgifsUser), Modifier.weight(1f))
                        TextButton(onClick = viewModel::redgifsSignOut) { Text(stringResource(R.string.sign_out)) }
                    }
                    if (!state.running) {
                        Button(onClick = { start { viewModel.syncLikes(false) } }, Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.rg_sync_new))
                        }
                        OutlinedButton(onClick = { start { viewModel.syncLikes(true) } }, Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.rg_sync_all))
                        }
                    }
                }
                else -> {
                    Text(
                        stringResource(if (state.redgifsUser.isNotEmpty()) R.string.rg_expired else R.string.rg_login_about),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(onClick = { viewModel.openRedgifsLogin(true) }) { Text(stringResource(R.string.rg_sign_in)) }
                }
            }
            OutlinedButton(onClick = viewModel::openNiches, Modifier.fillMaxWidth()) { Text(stringResource(R.string.niche_browse)) }
        }
    }
    // While signed in (or expired), the site loads out of sight so its refreshed token is picked up.
    if (state.redgifsUser.isNotEmpty() && !state.redgifsLogin) {
        RedgifsBrowser(viewModel::onRedgifsToken, Modifier.size(1.dp).alpha(0f))
    }
}

/** Pick a niche, then pick clips from its most liked list to download. */
@Composable
fun NicheDialog(viewModel: MainViewModel, niches: NicheState) {
    Dialog(onDismissRequest = viewModel::closeNiches, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().systemBarsPadding()) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        niches.niche?.name ?: stringResource(R.string.niche_title),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (niches.niche != null) TextButton(onClick = { viewModel.pickNiche(null) }) { Text(stringResource(R.string.niche_back)) }
                    TextButton(onClick = viewModel::closeNiches) { Text(stringResource(R.string.close)) }
                }
                if (niches.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                niches.error?.let { Text(stringResource(R.string.error_generic, it), color = MaterialTheme.colorScheme.error) }
                if (niches.niche == null) NicheList(viewModel, niches) else NicheGifs(viewModel, niches)
            }
        }
    }
}

@Composable
private fun NicheList(viewModel: MainViewModel, niches: NicheState) {
    OutlinedTextField(
        value = niches.query,
        onValueChange = viewModel::onNicheQuery,
        label = { Text(stringResource(R.string.niche_search)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { viewModel.searchNiches(niches.query) }),
        modifier = Modifier.fillMaxWidth(),
    )
    LazyColumn(Modifier.fillMaxSize()) {
        items(niches.niches, key = { it.id }) { n ->
            Column(Modifier.fillMaxWidth().clickable { viewModel.pickNiche(n) }.padding(vertical = 10.dp)) {
                Text(n.name, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.niche_stats, n.gifs, n.subscribers), style = MaterialTheme.typography.bodySmall)
            }
            HorizontalDivider()
        }
        if (niches.niches.isEmpty() && !niches.loading) item { Text(stringResource(R.string.niche_none), Modifier.padding(vertical = 16.dp)) }
    }
}

@Composable
private fun NicheGifs(viewModel: MainViewModel, niches: NicheState) {
    val labels = mapOf(
        "best" to R.string.order_best, "hot" to R.string.order_hot,
        "trending" to R.string.order_trending, "latest" to R.string.order_latest,
    )
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Redgifs.NICHE_ORDERS.forEach { order ->
            FilterChip(
                selected = niches.order == order,
                onClick = { if (niches.order != order) viewModel.setNicheOrder(order) },
                label = { Text(stringResource(labels.getValue(order))) },
            )
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = viewModel::downloadSelectedGifs, enabled = niches.selected.isNotEmpty()) {
            Text(stringResource(R.string.niche_download, niches.selected.size))
        }
        TextButton(onClick = viewModel::selectAllGifs, enabled = niches.gifs.isNotEmpty()) { Text(stringResource(R.string.niche_select_all)) }
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(niches.gifs, key = { it.id }) { gif -> GifRow(gif, gif.id in niches.selected) { viewModel.toggleGif(gif.id) } }
        if (niches.hasMore) item {
            OutlinedButton(onClick = viewModel::loadNicheGifs, enabled = !niches.loading, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(stringResource(R.string.niche_more))
            }
        }
        if (niches.gifs.isEmpty() && !niches.loading) item { Text(stringResource(R.string.niche_none), Modifier.padding(vertical = 16.dp)) }
    }
}

@Composable
private fun GifRow(gif: RedgifsGif, selected: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (gif.thumbnail.isNotEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(gif.thumbnail)
                    .setHeader("User-Agent", Graph.redgifsUserAgent).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(gif.user.ifEmpty { gif.id }, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                stringResource(R.string.gif_stats, gif.likes, gif.durationSeconds.toInt()) +
                    if (gif.hasAudio) " · " + stringResource(R.string.gif_sound) else "",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Checkbox(checked = selected, onCheckedChange = { onToggle() })
    }
}
