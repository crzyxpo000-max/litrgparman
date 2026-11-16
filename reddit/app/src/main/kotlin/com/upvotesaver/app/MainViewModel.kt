package com.upvotesaver.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.upvotesaver.core.PostRecord
import com.upvotesaver.core.Progress
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

data class UiState(
    val clientId: String = "",
    val signedIn: Boolean = false,
    val signingIn: Boolean = false,
    val username: String = "",
    val intervalMinutes: Int = 60,
    val wifiOnly: Boolean = true,
    val running: Boolean = false,
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
                    } else {
                        Message(R.string.error_generic, info.outputData.getString(SyncWorker.KEY_ERROR).orEmpty())
                    }
                    _state.update { it.copy(message = message) }
                }
                refresh()
            }
        }
    }

    /** Re-reads settings and the post log (the background schedule may have changed them). */
    fun refresh() = _state.update {
        it.copy(
            clientId = settings.clientId,
            signedIn = settings.signedIn,
            username = settings.username,
            intervalMinutes = settings.intervalMinutes,
            wifiOnly = settings.wifiOnly,
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

    fun setWifiOnly(value: Boolean) {
        settings.wifiOnly = value
        SyncWorker.schedule(getApplication())
        refresh()
    }

    fun show(message: Message) = _state.update { it.copy(message = message) }

    fun dismissMessage() = _state.update { it.copy(message = null) }
}
