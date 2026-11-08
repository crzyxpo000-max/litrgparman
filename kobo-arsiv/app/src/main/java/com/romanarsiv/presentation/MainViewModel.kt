package com.romanarsiv.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.romanarsiv.ArchiverApp
import com.romanarsiv.data.remote.UrlUtils
import com.romanarsiv.domain.model.ArchiveProgress
import com.romanarsiv.domain.model.ArchiveResult
import com.romanarsiv.domain.model.NovelSource
import com.romanarsiv.domain.usecase.ArchiveEvent
import com.romanarsiv.domain.usecase.ArchiveNovelUseCase
import com.romanarsiv.domain.usecase.SearchNovelUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.jsoup.HttpStatusException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

sealed interface SearchState {
    data object Idle : SearchState
    data object Loading : SearchState
    data class Success(val results: List<NovelSource>) : SearchState
    data class Error(val message: String) : SearchState
}

sealed interface DownloadState {
    data object Idle : DownloadState
    data class Loading(val source: NovelSource, val progress: ArchiveProgress) : DownloadState
    data class Success(val result: ArchiveResult) : DownloadState
    data class Error(val message: String) : DownloadState
}

/** Tek seferlik bildirimler (Snackbar). */
sealed interface UiMessage {
    data class Saved(val result: ArchiveResult) : UiMessage
    data class Failed(val message: String) : UiMessage
}

class MainViewModel(
    private val searchNovel: SearchNovelUseCase,
    private val archiveNovel: ArchiveNovelUseCase,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _searchState = MutableStateFlow<SearchState>(SearchState.Idle)
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    /** Onay bekleyen kaynak (kitap adını düzenleme penceresi). */
    private val _pendingSource = MutableStateFlow<NovelSource?>(null)
    val pendingSource: StateFlow<NovelSource?> = _pendingSource.asStateFlow()

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages: Flow<UiMessage> = _messages.receiveAsFlow()

    private var lastSearchedQuery = ""
    private var searchJob: Job? = null
    private var downloadJob: Job? = null

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun search() {
        val q = _query.value.trim()
        if (q.isEmpty() || _downloadState.value is DownloadState.Loading) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _searchState.value = SearchState.Loading
            _searchState.value = try {
                val results = searchNovel(q)
                lastSearchedQuery = q
                SearchState.Success(results)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                SearchState.Error(describe(e))
            }
        }
    }

    fun selectSource(source: NovelSource) {
        if (_downloadState.value is DownloadState.Loading) return
        _pendingSource.value = source
    }

    fun dismissPending() {
        _pendingSource.value = null
    }

    /** Onay penceresinde önerilen kitap adı: aranan roman adı; arama bir URL ise sayfa başlığı. */
    fun suggestedTitle(source: NovelSource): String =
        lastSearchedQuery.takeIf { it.isNotBlank() && !UrlUtils.isHttpUrl(it) } ?: source.title

    fun startDownload(bookTitle: String) {
        val source = _pendingSource.value ?: return
        _pendingSource.value = null
        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            _downloadState.value = DownloadState.Loading(source, ArchiveProgress.DetectingChapters)
            try {
                archiveNovel(source, bookTitle).collect { event ->
                    when (event) {
                        is ArchiveEvent.Progress -> _downloadState.value = DownloadState.Loading(source, event.progress)
                        is ArchiveEvent.Completed -> {
                            _downloadState.value = DownloadState.Success(event.result)
                            _messages.send(UiMessage.Saved(event.result))
                        }
                    }
                }
            } catch (e: CancellationException) {
                _downloadState.value = DownloadState.Idle
                throw e
            } catch (e: Exception) {
                val message = describe(e)
                _downloadState.value = DownloadState.Error(message)
                _messages.send(UiMessage.Failed(message))
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _downloadState.value = DownloadState.Idle
    }

    private fun describe(e: Exception): String = when (e) {
        is UnknownHostException -> "İnternet bağlantısı yok ya da site bulunamadı."
        is SocketTimeoutException -> "Sunucu zamanında yanıt vermedi."
        is SSLException -> "Güvenli bağlantı kurulamadı."
        is HttpStatusException -> "Sunucu ${e.statusCode} hatası döndürdü."
        else -> e.message?.takeIf { it.isNotBlank() } ?: e.javaClass.simpleName
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ArchiverApp
                MainViewModel(app.container.searchNovel, app.container.archiveNovel)
            }
        }
    }
}
