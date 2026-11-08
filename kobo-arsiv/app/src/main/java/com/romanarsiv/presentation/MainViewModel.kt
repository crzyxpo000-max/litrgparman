package com.romanarsiv.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.romanarsiv.ArchiverApp
import com.romanarsiv.background.ArchiveJobs
import com.romanarsiv.background.DetectResult
import com.romanarsiv.background.DetectState
import com.romanarsiv.background.DownloadTask
import com.romanarsiv.background.UiMessage
import com.romanarsiv.data.remote.ChallengeBroker
import com.romanarsiv.data.remote.UrlUtils
import com.romanarsiv.domain.FileNames
import com.romanarsiv.domain.model.ChapterRef
import com.romanarsiv.domain.model.NovelSource
import com.romanarsiv.domain.usecase.SearchNovelUseCase
import com.romanarsiv.settings.AppPreferences
import com.romanarsiv.settings.UiPrefs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface SearchState {
    data object Idle : SearchState
    data object Loading : SearchState

    /** [results] roman siteleri; [hidden] filtrenin eledikleri (istenirse gösterilir). */
    data class Success(val results: List<NovelSource>, val hidden: List<NovelSource> = emptyList()) : SearchState
    data class Error(val error: Throwable) : SearchState
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

/** Alt gezinme çubuğunun sekmeleri. */
enum class Tab { SEARCH, DOWNLOADS, SETTINGS }

/**
 * Arama ve seçim ekranı burada; uzun işler (tespit, indirme) [ArchiveJobs]'ta koşar ki uygulama
 * arka plana alınsa ya da ekran kapansa da sürsün. Bu sınıf yalnızca onların durumunu yansıtır.
 */
class MainViewModel(
    private val searchNovel: SearchNovelUseCase,
    private val jobs: ArchiveJobs,
    private val challengeBroker: ChallengeBroker,
    private val preferences: AppPreferences,
) : ViewModel() {

    val prefs: StateFlow<UiPrefs> = preferences.prefs

    fun updatePrefs(change: (UiPrefs) -> UiPrefs) = preferences.update(change)

    private val _tab = MutableStateFlow(Tab.SEARCH)
    val tab: StateFlow<Tab> = _tab.asStateFlow()

    fun selectTab(tab: Tab) {
        _tab.value = tab
    }

    /** Kullanıcının tarayıcıda geçmesi gereken doğrulama sayfası; yoksa null. */
    val challengeUrl: StateFlow<String?> = challengeBroker.pendingUrl

    fun onChallengeResult(solved: Boolean) = challengeBroker.complete(solved)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _searchState = MutableStateFlow<SearchState>(SearchState.Idle)
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    val detectState: StateFlow<DetectState> = jobs.detectState
    val tasks: StateFlow<List<DownloadTask>> = jobs.tasks
    val messages: Flow<UiMessage> = jobs.messages

    private val _selection = MutableStateFlow<SelectionState?>(null)
    val selection: StateFlow<SelectionState?> = _selection.asStateFlow()

    private var lastSearchedQuery = ""
    private var searchJob: Job? = null

    init {
        // Tespit uygulama arka plandayken bitmiş olabilir: ekran geri gelince seçim ekranını aç.
        viewModelScope.launch {
            jobs.detectResult.filterNotNull().collect {
                jobs.consumeDetectResult()?.let(::openSelection)
            }
        }
    }

    // ------------------------------------------------------------------ arama

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun search() {
        val q = _query.value.trim()
        if (q.isEmpty()) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _searchState.value = SearchState.Loading
            _searchState.value = try {
                val results = searchNovel(q)
                lastSearchedQuery = q
                SearchState.Success(results.novelSites, results.hidden)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                SearchState.Error(e)
            }
        }
    }

    // ------------------------------------------------------------------ bölüm tespiti

    /** Kaynağa dokunuldu: bölümleri arka planda tespit et; bitince seçim ekranı açılır. */
    fun selectSource(source: NovelSource) {
        if (jobs.isDetecting) return
        val title = lastSearchedQuery.takeIf { it.isNotBlank() && !UrlUtils.isHttpUrl(it) } ?: source.title
        jobs.detect(source, title)
    }

    fun cancelDetection() = jobs.cancelDetection()

    private fun openSelection(result: DetectResult) {
        _selection.value = SelectionState(
            source = result.source,
            chapters = result.chapters,
            selected = result.chapters.indices.toSet(),
            bookTitle = result.suggestedTitle,
            fileName = FileNames.suggest(result.suggestedTitle),
        )
    }

    // ------------------------------------------------------------------ seçim ekranı

    fun closeSelection() {
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

    /** Seçimi İndirilenler kuyruğuna ekler; seçim ekranı açık kalır (başka bir aralık da eklenebilir). */
    fun startDownload(): Boolean {
        val s = _selection.value ?: return false
        if (!s.canDownload) return false
        return jobs.download(s.source, s.selectedChapters(), s.bookTitle, s.fileName) != null
    }

    fun cancelDownload(id: Long) = jobs.cancelDownload(id)

    fun retryDownload(id: Long) = jobs.retry(id)

    fun removeDownload(id: Long) = jobs.remove(id)

    fun clearFinished() = jobs.clearFinished()

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
                MainViewModel(c.searchNovel, c.archiveJobs, c.challengeBroker, c.preferences)
            }
        }
    }
}
