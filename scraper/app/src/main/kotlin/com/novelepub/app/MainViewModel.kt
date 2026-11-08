package com.novelepub.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.novelepub.core.ChapterRef
import com.novelepub.core.Cover
import com.novelepub.core.FileChapterCache
import com.novelepub.core.GenericConfig
import com.novelepub.core.GenericSource
import com.novelepub.core.NovelDownloader
import com.novelepub.core.NovelInfo
import com.novelepub.core.NovelSource
import com.novelepub.core.OkHttpFetcher
import com.novelepub.core.Progress
import com.novelepub.core.BraveSearch
import com.novelepub.core.RoyalRoadSearch
import com.novelepub.core.SearchProvider
import com.novelepub.core.SearchResult
import com.novelepub.core.SearchService
import com.novelepub.core.SearxSearch
import com.novelepub.core.Sources
import com.novelepub.core.epubFileName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.File
import java.security.MessageDigest

enum class Busy { SEARCHING, LOADING_INFO, DOWNLOADING, WRITING }

enum class Mode { SEARCH, URL }

data class UiState(
    val mode: Mode = Mode.SEARCH,
    val query: String = "",
    /** Null until a search has run. */
    val searchResults: List<SearchResult>? = null,
    /** Provider name → error, for search providers that failed. */
    val searchErrors: Map<String, String> = emptyMap(),
    val url: String = "",
    /** Name of the dedicated parser for [url], or null when the generic reader is used. */
    val siteName: String? = null,
    val generic: GenericConfig = GenericConfig(),
    val novel: NovelInfo? = null,
    val cover: Cover? = null,
    val rangeFrom: String = "1",
    val rangeTo: String = "",
    val busy: Busy? = null,
    val progress: Progress? = null,
    val result: File? = null,
    val resultChapters: Int = 0,
    /** A message to show; [errorCancelled] marks a user cancel rather than a failure. */
    val error: String? = null,
    val errorCancelled: Boolean = false,
    val invalidUrl: Boolean = false,
) {
    val isGeneric: Boolean get() = url.isNotBlank() && siteName == null

    /** The chosen chapter slice, or null if the range fields are invalid. */
    fun selectedRange(): IntRange? {
        val n = novel?.chapters?.size ?: return null
        val from = rangeFrom.toIntOrNull() ?: return null
        val to = rangeTo.toIntOrNull() ?: return null
        return if (from in 1..to && to <= n) from..to else null
    }
}

class MainViewModel(app: Application) : AndroidViewModel(app) {
    val settings = AppSettings(app)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var job: Job? = null

    fun onUrlChange(input: String) {
        val normalized = Sources.normalizeUrl(input)
        val site = normalized?.let { Sources.forUrl(it) }
        val host = normalized?.toHttpUrlOrNull()?.host
        _state.update { s ->
            val hostChanged = host != s.url.let(Sources::normalizeUrl)?.toHttpUrlOrNull()?.host
            s.copy(
                url = input,
                siteName = site?.name,
                generic = if (hostChanged && host != null) settings.genericConfig(host) ?: GenericConfig() else s.generic,
                invalidUrl = false,
            )
        }
    }

    fun onModeChange(mode: Mode) = _state.update { it.copy(mode = mode) }

    fun onQueryChange(query: String) = _state.update { it.copy(query = query) }

    fun search() {
        val query = state.value.query.trim()
        if (query.isEmpty()) return
        launchWork(Busy.SEARCHING) {
            val outcome = SearchService(searchProviders()).search(query, fetcher())
            _state.update { it.copy(searchResults = outcome.results, searchErrors = outcome.errors) }
        }
    }

    /** The user chose a search result: read that page as the novel. */
    fun pickResult(result: SearchResult) {
        if (state.value.busy != null) return
        _state.update { UiState(mode = Mode.URL, query = it.query, searchResults = it.searchResults, searchErrors = it.searchErrors) }
        onUrlChange(result.url)
        fetchInfo()
    }

    /** For a guessed chapter list that looks wrong: read chapter 1 and follow "next" links instead. */
    fun switchToCrawl() = _state.update { s ->
        val novel = s.novel ?: return@update s
        val first = novel.chapters.firstOrNull() ?: return@update s
        s.copy(novel = novel.copy(chapters = emptyList(), crawlFrom = first.url), rangeFrom = "1", rangeTo = "")
    }

    private fun searchProviders(): List<SearchProvider> = buildList {
        settings.braveApiKey.takeIf { it.isNotBlank() }?.let { add(BraveSearch(it)) }
        settings.searxUrl.takeIf { it.isNotBlank() }?.let { add(SearxSearch(it)) }
        add(RoyalRoadSearch())
    }

    fun onGenericChange(config: GenericConfig) = _state.update { it.copy(generic = config) }

    fun onRangeChange(from: String, to: String) = _state.update {
        it.copy(rangeFrom = from.filter(Char::isDigit).take(6), rangeTo = to.filter(Char::isDigit).take(6))
    }

    fun dismissError() = _state.update { it.copy(error = null, errorCancelled = false) }

    /** Back to an empty form, keeping the URL and the last search. */
    fun startOver() {
        job?.cancel()
        _state.update {
            UiState(
                mode = it.mode, query = it.query, searchResults = it.searchResults, searchErrors = it.searchErrors,
                url = it.url, siteName = it.siteName, generic = it.generic,
            )
        }
    }

    fun fetchInfo() {
        val url = Sources.normalizeUrl(state.value.url)
        if (url == null) {
            _state.update { it.copy(invalidUrl = true) }
            return
        }
        launchWork(Busy.LOADING_INFO) {
            val source = sourceFor(url)
            if (source is GenericSource) {
                url.toHttpUrlOrNull()?.host?.let { settings.saveGenericConfig(it, state.value.generic) }
            }
            val novel = source.fetchNovel(url, fetcher())
            val cover = downloader(novel).fetchCover(novel.coverUrl)
            _state.update {
                it.copy(
                    novel = novel,
                    cover = cover,
                    rangeFrom = "1",
                    rangeTo = novel.chapters.size.takeIf { n -> n > 0 }?.toString().orEmpty(),
                    result = null,
                )
            }
        }
    }

    fun download() {
        val s = state.value
        val novel = s.novel ?: return
        val url = Sources.normalizeUrl(s.url) ?: return
        if (!novel.isCrawl && s.selectedRange() == null) return
        launchWork(Busy.DOWNLOADING) {
            val source = sourceFor(url)
            val downloader = downloader(novel)
            val onProgress = { p: Progress -> _state.update { it.copy(progress = p) } }
            val chapters: List<ChapterRef>
            val name: String
            if (novel.isCrawl) {
                chapters = downloader.crawl(source, novel.crawlFrom!!, s.generic.maxChapters, onProgress)
                name = novel.title
            } else {
                val range = s.selectedRange()!!
                chapters = downloader.downloadList(source, novel.chapters.subList(range.first - 1, range.last), onProgress)
                name = if (range.first == 1 && range.last == novel.chapters.size) novel.title
                else "${novel.title} (${range.first}-${range.last})"
            }
            if (chapters.isEmpty()) throw IllegalStateException("No chapters were downloaded.")

            _state.update { it.copy(busy = Busy.WRITING) }
            val dir = File(getApplication<Application>().cacheDir, "epub").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() } // only the latest book is kept here
            val file = File(dir, epubFileName(name))
            file.outputStream().buffered().use { out -> downloader.writeEpub(novel, chapters, s.cover, out) }
            _state.update { it.copy(result = file, resultChapters = chapters.size) }
        }
    }

    fun cancel() {
        job?.cancel()
    }

    /** Copies the finished EPUB to a location the user picked. Returns an error message or null. */
    suspend fun saveTo(uri: Uri): String? = withContext(Dispatchers.IO) {
        val file = state.value.result ?: return@withContext "no file"
        try {
            val resolver = getApplication<Application>().contentResolver
            requireNotNull(resolver.openOutputStream(uri, "wt")) { "cannot open destination" }.use { out ->
                file.inputStream().use { it.copyTo(out) }
            }
            null
        } catch (e: Exception) {
            e.message ?: e.javaClass.simpleName
        }
    }

    fun cacheSizeBytes(): Long = chaptersRoot().walkBottomUp().filter { it.isFile }.sumOf { it.length() }

    fun clearCache() {
        chaptersRoot().deleteRecursively()
    }

    private fun launchWork(busy: Busy, block: suspend () -> Unit) {
        if (job?.isActive == true) return
        _state.update { it.copy(busy = busy, progress = null, error = null, errorCancelled = false, invalidUrl = false) }
        job = viewModelScope.launch(Dispatchers.IO) {
            try {
                block()
            } catch (e: CancellationException) {
                _state.update { it.copy(errorCancelled = busy == Busy.DOWNLOADING, error = if (busy == Busy.DOWNLOADING) "" else null) }
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: e.javaClass.simpleName) }
            } finally {
                _state.update { it.copy(busy = null) }
            }
        }
    }

    private fun sourceFor(url: String): NovelSource =
        Sources.forUrl(url, settings.includeAuthorNotes)
            ?: GenericSource(state.value.generic, pageDelayMs = (settings.delaySeconds * 1000).toLong())

    private fun fetcher() = OkHttpFetcher(userAgent = settings.userAgent)

    private fun downloader(novel: NovelInfo): NovelDownloader {
        // One cache folder per novel, so clearing or reusing it is simple.
        val key = MessageDigest.getInstance("SHA-1").digest(novel.url.toByteArray())
            .joinToString("") { "%02x".format(it) }.take(16)
        val cache = FileChapterCache(File(chaptersRoot(), key))
        return NovelDownloader(fetcher(), cache, delayMs = (settings.delaySeconds * 1000).toLong())
    }

    private fun chaptersRoot() = File(getApplication<Application>().cacheDir, "chapters")
}
