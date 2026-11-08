package com.romanarsiv.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.romanarsiv.ArchiverApp
import com.romanarsiv.data.remote.ChallengeBroker
import com.romanarsiv.data.remote.ChallengeDeclinedException
import com.romanarsiv.data.remote.ChallengeRequiredException
import com.romanarsiv.data.remote.UrlUtils
import com.romanarsiv.domain.FileNames
import com.romanarsiv.domain.model.ArchiveProgress
import com.romanarsiv.domain.model.ArchiveResult
import com.romanarsiv.domain.model.ChapterRef
import com.romanarsiv.domain.model.NovelSource
import com.romanarsiv.domain.usecase.ArchiveEvent
import com.romanarsiv.domain.usecase.ArchiveNovelUseCase
import com.romanarsiv.domain.usecase.DetectChaptersUseCase
import com.romanarsiv.domain.usecase.SearchNovelUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
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

/** Bölüm tespiti (kaynağa dokunulduktan sonra, seçim ekranından önce). */
sealed interface DetectState {
    data object Idle : DetectState
    data class Loading(val source: NovelSource, val found: Int) : DetectState
}

sealed interface DownloadState {
    data object Idle : DownloadState
    data class Loading(val source: NovelSource, val progress: ArchiveProgress) : DownloadState
    data class Success(val result: ArchiveResult) : DownloadState
    data class Error(val message: String) : DownloadState
}

/** Bölüm seçim ekranı. [selected] 0 tabanlı sıra numaralarıdır. */
data class SelectionState(
    val source: NovelSource,
    val chapters: List<ChapterRef>,
    val selected: Set<Int>,
    val bookTitle: String,
    val fileName: String,
    /** Kullanıcı dosya adını elle değiştirdiyse kitap adı değişince artık güncellenmez. */
    val fileNameEdited: Boolean = false,
) {
    val selectedCount: Int get() = selected.size
    val canDownload: Boolean get() = selected.isNotEmpty() && bookTitle.isNotBlank()

    /** Okuma sırasıyla seçilmiş bölümler. */
    fun selectedChapters(): List<ChapterRef> = chapters.filterIndexed { i, _ -> i in selected }
}

/** Tek seferlik bildirimler (Snackbar). */
sealed interface UiMessage {
    data class Saved(val result: ArchiveResult) : UiMessage
    data class Failed(val message: String) : UiMessage
}

class MainViewModel(
    private val searchNovel: SearchNovelUseCase,
    private val detectChapters: DetectChaptersUseCase,
    private val archiveNovel: ArchiveNovelUseCase,
    private val challengeBroker: ChallengeBroker,
) : ViewModel() {

    /** Kullanıcının tarayıcıda geçmesi gereken doğrulama sayfası; yoksa null. */
    val challengeUrl: StateFlow<String?> = challengeBroker.pendingUrl

    fun onChallengeResult(solved: Boolean) = challengeBroker.complete(solved)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _searchState = MutableStateFlow<SearchState>(SearchState.Idle)
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    private val _detectState = MutableStateFlow<DetectState>(DetectState.Idle)
    val detectState: StateFlow<DetectState> = _detectState.asStateFlow()

    private val _selection = MutableStateFlow<SelectionState?>(null)
    val selection: StateFlow<SelectionState?> = _selection.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages: Flow<UiMessage> = _messages.receiveAsFlow()

    private var lastSearchedQuery = ""
    private var searchJob: Job? = null
    private var detectJob: Job? = null
    private var downloadJob: Job? = null

    private val busy: Boolean
        get() = _downloadState.value is DownloadState.Loading || _detectState.value is DetectState.Loading

    // ------------------------------------------------------------------ arama

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun search() {
        val q = _query.value.trim()
        if (q.isEmpty() || busy) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _searchState.value = SearchState.Loading
            _searchState.value = try {
                val results = searchNovel(q)
                lastSearchedQuery = q
                SearchState.Success(results)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                SearchState.Error(describe(e))
            }
        }
    }

    // ------------------------------------------------------------------ bölüm tespiti

    /** Kaynağa dokunuldu: bölümleri tespit et, sonra seçim ekranını aç. */
    fun selectSource(source: NovelSource) {
        if (busy) return
        detectJob?.cancel()
        detectJob = viewModelScope.launch {
            _detectState.value = DetectState.Loading(source, 0)
            try {
                val chapters = detectChapters(source) { found -> _detectState.value = DetectState.Loading(source, found) }
                val title = suggestedTitle(source)
                _selection.value = SelectionState(
                    source = source,
                    chapters = chapters,
                    selected = chapters.indices.toSet(),
                    bookTitle = title,
                    fileName = FileNames.suggest(title),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _messages.send(UiMessage.Failed(describe(e)))
            } finally {
                _detectState.value = DetectState.Idle
            }
        }
    }

    fun cancelDetection() {
        detectJob?.cancel()
        detectJob = null
        _detectState.value = DetectState.Idle
    }

    /** Önerilen kitap adı: aranan roman adı; arama bir URL ise sayfa başlığı. */
    private fun suggestedTitle(source: NovelSource): String =
        lastSearchedQuery.takeIf { it.isNotBlank() && !UrlUtils.isHttpUrl(it) } ?: source.title

    // ------------------------------------------------------------------ seçim ekranı

    fun closeSelection() {
        if (_downloadState.value is DownloadState.Loading) return
        _selection.value = null
    }

    fun toggleChapter(index: Int) = _selection.update { s ->
        s?.copy(selected = if (index in s.selected) s.selected - index else s.selected + index)
    }

    fun selectAll() = _selection.update { s -> s?.copy(selected = s.chapters.indices.toSet()) }

    fun selectNone() = _selection.update { s -> s?.copy(selected = emptySet()) }

    fun invertSelection() = _selection.update { s ->
        s?.copy(selected = s.chapters.indices.toSet() - s.selected)
    }

    /** 1 tabanlı, iki uç dahil; uçlar sırasız da verilebilir ve listeye kırpılır. Seçimi bu aralıkla değiştirir. */
    fun selectRange(from: Int, to: Int) = _selection.update { s ->
        s?.copy(selected = rangeIndices(from, to, s.chapters.size))
    }

    fun onBookTitleChange(value: String) = _selection.update { s ->
        s?.copy(bookTitle = value, fileName = if (s.fileNameEdited) s.fileName else FileNames.suggest(value))
    }

    fun onFileNameChange(value: String) = _selection.update { s -> s?.copy(fileName = value, fileNameEdited = true) }

    /** Dosya adını yeniden kitap adından öner. */
    fun resetFileName() = _selection.update { s -> s?.copy(fileName = FileNames.suggest(s.bookTitle), fileNameEdited = false) }

    // ------------------------------------------------------------------ indirme

    fun startDownload() {
        val s = _selection.value ?: return
        if (!s.canDownload || busy) return
        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            val chapters = s.selectedChapters()
            _downloadState.value = DownloadState.Loading(
                s.source,
                ArchiveProgress.FetchingChapter(0, chapters.size, chapters.first().title),
            )
            try {
                archiveNovel(chapters, s.bookTitle, s.fileName).collect { event ->
                    when (event) {
                        is ArchiveEvent.Progress -> _downloadState.value = DownloadState.Loading(s.source, event.progress)
                        is ArchiveEvent.Completed -> {
                            _downloadState.value = DownloadState.Success(event.result)
                            _messages.send(UiMessage.Saved(event.result))
                        }
                    }
                }
            } catch (e: CancellationException) {
                _downloadState.value = DownloadState.Idle
                throw e
            } catch (e: Throwable) {
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

    private fun describe(e: Throwable): String = when (e) {
        is UnknownHostException -> "İnternet bağlantısı yok ya da site bulunamadı."
        is SocketTimeoutException -> "Sunucu zamanında yanıt vermedi."
        is SSLException -> "Güvenli bağlantı kurulamadı."
        is HttpStatusException -> {
            val host = UrlUtils.hostOf(e.url) ?: e.url
            if (e.statusCode == 403 || e.statusCode == 503) {
                "$host erişimi reddetti (HTTP ${e.statusCode}). Site otomatik erişimi engelliyor olabilir."
            } else {
                "$host HTTP ${e.statusCode} hatası döndürdü."
            }
        }
        is ChallengeRequiredException -> "${e.message}. Doğrulama sayfası geçilemedi."
        is ChallengeDeclinedException -> e.message ?: "Doğrulama tamamlanmadı."
        is OutOfMemoryError -> "Bellek yetmedi; kitap çok büyük olabilir."
        else -> e.message?.takeIf { it.isNotBlank() }?.let { "$it (${e.javaClass.simpleName})" } ?: e.javaClass.simpleName
    }

    companion object {
        internal fun rangeIndices(from: Int, to: Int, size: Int): Set<Int> {
            if (size == 0) return emptySet()
            val lo = minOf(from, to).coerceIn(1, size)
            val hi = maxOf(from, to).coerceIn(1, size)
            return (lo - 1..hi - 1).toSet()
        }

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ArchiverApp
                val c = app.container
                MainViewModel(c.searchNovel, c.detectChapters, c.archiveNovel, c.challengeBroker)
            }
        }
    }
}
