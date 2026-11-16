package com.upvotesaver.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.upvotesaver.core.PostRecord
import com.upvotesaver.core.Progress
import com.upvotesaver.core.Redgifs
import com.upvotesaver.core.RedgifsGif
import com.upvotesaver.core.RedgifsLinks
import com.upvotesaver.core.RedgifsNiche
import com.upvotesaver.core.Status
import com.upvotesaver.core.VotesCsv
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/** A message for the user: a string resource and its argument. */
data class Message(val resId: Int, val arg: String? = null)

/** The niche browser: either the list of niches or one niche's clips. */
data class NicheState(
    val query: String = "",
    val loading: Boolean = false,
    val niches: List<RedgifsNiche> = emptyList(),
    val niche: RedgifsNiche? = null,
    val order: String = Redgifs.NICHE_ORDERS.first(),
    val gifs: List<RedgifsGif> = emptyList(),
    val page: Int = 0,
    val hasMore: Boolean = false,
    val selected: Set<String> = emptySet(),
    val error: String? = null,
)

data class UiState(
    val redgifsSignedIn: Boolean = false,
    /** Empty when never signed in; set with [redgifsSignedIn] false = the session expired. */
    val redgifsUser: String = "",
    val redgifsLogin: Boolean = false,
    val niches: NicheState? = null,
    val clientId: String = "",
    val signedIn: Boolean = false,
    val signingIn: Boolean = false,
    val username: String = "",
    val intervalMinutes: Int = 60,
    val wifiOnly: Boolean = true,
    val running: Boolean = false,
    /** Redgifs links are being downloaded. */
    val linksRunning: Boolean = false,
    val hideFromGallery: Boolean = false,
    /** Saved files are being moved after the setting changed. */
    val moving: Boolean = false,
    /** Null while a started run waits for the network. */
    val progress: Progress? = null,
    val counts: Map<Status, Int> = emptyMap(),
    val recent: List<PostRecord> = emptyList(),
    val lastSyncMs: Long = 0,
    val message: Message? = null,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val settings = Graph.settings
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    init {
        refresh()
        viewModelScope.launch {
            WorkManager.getInstance(app).getWorkInfosForUniqueWorkFlow(SyncWorker.WORK_MOVE).collect { infos ->
                val info = infos.firstOrNull()
                val was = _state.value.moving
                val now = info != null && !info.state.isFinished
                _state.update { it.copy(moving = now) }
                if (was && !now && info?.state == WorkInfo.State.FAILED) {
                    show(Message(R.string.error_generic, info.outputData.getString(SyncWorker.KEY_ERROR).orEmpty()))
                }
            }
        }
        viewModelScope.launch {
            WorkManager.getInstance(app).getWorkInfosForUniqueWorkFlow(SyncWorker.WORK_LINKS).collect { infos ->
                _state.update { it.copy(linksRunning = infos.any { i -> !i.state.isFinished }) }
                refresh()
            }
        }
        viewModelScope.launch {
            WorkManager.getInstance(app).getWorkInfosForUniqueWorkFlow(SyncWorker.WORK_MANUAL).collect { infos ->
                val info = infos.firstOrNull()
                val wasRunning = _state.value.running
                val running = info != null && !info.state.isFinished
                _state.update {
                    it.copy(
                        running = running,
                        progress = info?.takeIf { i -> i.state == WorkInfo.State.RUNNING }?.let { i -> SyncWorker.progress(i.progress) },
                    )
                }
                if (wasRunning && !running && info?.state == WorkInfo.State.FAILED) {
                    val message = if (info.outputData.getBoolean(SyncWorker.KEY_SIGNED_OUT, false)) {
                        Message(R.string.error_signed_out)
                    } else if (info.outputData.getBoolean(SyncWorker.KEY_RG_SIGNED_OUT, false)) {
                        Message(R.string.rg_expired)
                    } else {
                        Message(R.string.error_generic, info.outputData.getString(SyncWorker.KEY_ERROR).orEmpty())
                    }
                    _state.update { it.copy(message = message) }
                }
                refresh()
            }
        }
    }

    /**
     * Downloads the Redgifs clips linked in [text] (pasted, or shared from another app).
     * Returns false when the text has no Redgifs link.
     */
    fun saveLinks(text: String): Boolean {
        val ids = RedgifsLinks.ids(text)
        if (ids.isEmpty()) {
            show(Message(if ("reddit.com" in text || "redd.it" in text) R.string.links_reddit else R.string.links_none))
            return false
        }
        SyncWorker.saveLinks(getApplication(), ids)
        show(Message(R.string.links_started, ids.size.toString()))
        return true
    }

    /** Re-reads settings and the post log (the background schedule may have changed them). */
    fun refresh() = _state.update {
        it.copy(
            clientId = settings.clientId,
            signedIn = settings.signedIn,
            redgifsSignedIn = settings.redgifsSignedIn,
            redgifsUser = settings.redgifsUser,
            username = settings.username,
            intervalMinutes = settings.intervalMinutes,
            wifiOnly = settings.wifiOnly,
            hideFromGallery = settings.hideFromGallery,
            counts = Graph.store.counts(),
            recent = Graph.store.recent(50),
            lastSyncMs = settings.lastSyncMs,
        )
    }

    fun onClientIdChange(value: String) {
        settings.clientId = value
        _state.update { it.copy(clientId = value.trim()) }
    }

    /** The Reddit page to open in the browser for sign-in. */
    fun signInUrl(): String {
        val state = UUID.randomUUID().toString()
        settings.pendingState = state
        return Graph.auth().authorizeUrl(state)
    }

    /** Reddit's redirect back to the app: `upvotesaver://auth?state=…&code=…` (or `error=…`). */
    fun onAuthRedirect(uri: Uri) {
        val expected = settings.pendingState
        settings.pendingState = null
        val error = uri.getQueryParameter("error")
        val code = uri.getQueryParameter("code")
        when {
            error != null -> return show(Message(R.string.error_auth_denied, error))
            expected == null || uri.getQueryParameter("state") != expected || code == null ->
                return show(Message(R.string.error_auth_state))
        }
        _state.update { it.copy(signingIn = true, message = null) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    settings.save(Graph.auth().exchange(code!!))
                    settings.username = Graph.api().me()
                }
                SyncWorker.schedule(getApplication())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                settings.save(null)
                show(Message(R.string.error_generic, e.message.orEmpty()))
            }
            _state.update { it.copy(signingIn = false) }
            refresh()
        }
    }

    fun signOut() {
        SyncWorker.cancel(getApplication())
        settings.save(null)
        settings.username = ""
        SyncWorker.schedule(getApplication())
        refresh()
    }

    fun syncNow() = SyncWorker.runNow(getApplication(), SyncWorker.MODE_NEW)

    fun scanAll() = SyncWorker.runNow(getApplication(), SyncWorker.MODE_FULL)

    /** Reads a `post_votes.csv` picked by the user and downloads the upvoted posts in it. */
    fun importCsv(uri: Uri) {
        viewModelScope.launch {
            try {
                val ids = withContext(Dispatchers.IO) {
                    val text = getApplication<Application>().contentResolver.openInputStream(uri)
                        ?.use { it.readBytes().toString(Charsets.UTF_8) }.orEmpty()
                    VotesCsv.upvotedIds(text).also { ids ->
                        if (ids.isNotEmpty()) SyncWorker.importFile(getApplication()).writeText(ids.joinToString("\n"))
                    }
                }
                if (ids.isEmpty()) show(Message(R.string.import_empty)) else SyncWorker.runNow(getApplication(), SyncWorker.MODE_IMPORT)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                show(Message(R.string.error_generic, e.message.orEmpty()))
            }
        }
    }

    fun cancel() = SyncWorker.cancel(getApplication())

    fun setInterval(minutes: Int) {
        settings.intervalMinutes = minutes
        SyncWorker.schedule(getApplication())
        refresh()
    }

    /** New downloads follow the setting at once; the files saved so far are moved in the background. */
    fun setHideFromGallery(value: Boolean) {
        settings.hideFromGallery = value
        SyncWorker.relocate(getApplication())
        refresh()
    }

    fun setWifiOnly(value: Boolean) {
        settings.wifiOnly = value
        SyncWorker.schedule(getApplication())
        refresh()
    }

    // ---- Redgifs account ----

    private val checkedTokens = HashSet<String>()

    fun openRedgifsLogin(open: Boolean) = _state.update { it.copy(redgifsLogin = open) }

    /**
     * A bearer token seen on one of the Redgifs site's own API requests in the in-app browser.
     * Visitors get a client token too, so it is kept only if it identifies a signed-in user.
     */
    fun onRedgifsToken(token: String) {
        if (token == settings.redgifsToken || !checkedTokens.add(token)) return
        if (jwtSubject(token)?.startsWith("client/") == true) return
        viewModelScope.launch {
            val name = try {
                withContext(Dispatchers.IO) { Redgifs(Graph.redgifsHttp, userToken = { token }).me() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                return@launch
            }
            settings.redgifsToken = token
            settings.redgifsUser = name.ifEmpty { "Redgifs" }
            SyncWorker.schedule(getApplication())
            _state.update { it.copy(redgifsLogin = false) }
            refresh()
        }
    }

    private fun jwtSubject(token: String): String? = runCatching {
        val payload = token.split('.')[1]
        val json = String(android.util.Base64.decode(payload, android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING))
        org.json.JSONObject(json).optString("sub")
    }.getOrNull()

    fun redgifsSignOut() {
        settings.redgifsToken = null
        settings.redgifsUser = ""
        checkedTokens.clear()
        // Forget the site's own session too, or the in-app browser would sign straight back in.
        android.webkit.CookieManager.getInstance().removeAllCookies(null)
        android.webkit.WebStorage.getInstance().deleteAllData()
        SyncWorker.schedule(getApplication())
        refresh()
    }

    fun syncLikes(full: Boolean) =
        SyncWorker.runNow(getApplication(), if (full) SyncWorker.MODE_RG_FULL else SyncWorker.MODE_RG_NEW)

    // ---- Niches ----

    fun openNiches() {
        _state.update { it.copy(niches = NicheState()) }
        searchNiches("")
    }

    fun closeNiches() = _state.update { it.copy(niches = null) }

    private fun niche(change: (NicheState) -> NicheState) = _state.update { it.copy(niches = it.niches?.let(change)) }

    private fun nicheCall(block: suspend () -> Unit) {
        niche { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                niche { it.copy(error = e.message ?: e.javaClass.simpleName) }
            }
            niche { it.copy(loading = false) }
        }
    }

    fun onNicheQuery(query: String) = niche { it.copy(query = query) }

    fun searchNiches(query: String) = nicheCall {
        val found = withContext(Dispatchers.IO) { Graph.redgifs.niches(query) }
        niche { it.copy(niches = found, niche = null) }
    }

    fun pickNiche(picked: RedgifsNiche?) {
        niche { it.copy(niche = picked, gifs = emptyList(), page = 0, hasMore = false, selected = emptySet(), error = null) }
        if (picked != null) loadNicheGifs()
    }

    fun setNicheOrder(order: String) {
        niche { it.copy(order = order, gifs = emptyList(), page = 0, hasMore = false, selected = emptySet()) }
        loadNicheGifs()
    }

    /** Loads the next page of the open niche's clips. */
    fun loadNicheGifs() {
        val current = _state.value.niches ?: return
        val picked = current.niche ?: return
        nicheCall {
            val next = current.page + 1
            val result = withContext(Dispatchers.IO) { Graph.redgifs.nicheGifs(picked.id, current.order, next) }
            niche { s ->
                if (s.niche?.id != picked.id || s.order != current.order) s
                else s.copy(gifs = (s.gifs + result.gifs).distinctBy { it.id }, page = next, hasMore = result.hasMore)
            }
        }
    }

    fun toggleGif(id: String) = niche { it.copy(selected = if (id in it.selected) it.selected - id else it.selected + id) }

    fun selectAllGifs() = niche { s ->
        s.copy(selected = if (s.selected.size == s.gifs.size) emptySet() else s.gifs.map { it.id }.toSet())
    }

    fun downloadSelectedGifs() {
        val current = _state.value.niches ?: return
        val ids = current.gifs.map { it.id }.filter { it in current.selected }
        if (ids.isEmpty()) return
        SyncWorker.saveLinks(getApplication(), ids)
        niche { it.copy(selected = emptySet()) }
        show(Message(R.string.links_started, ids.size.toString()))
    }

    fun show(message: Message) = _state.update { it.copy(message = message) }

    fun dismissMessage() = _state.update { it.copy(message = null) }
}
