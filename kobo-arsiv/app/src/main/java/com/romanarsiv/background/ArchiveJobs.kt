package com.romanarsiv.background

import com.romanarsiv.domain.AppConfig
import com.romanarsiv.domain.model.ArchiveProgress
import com.romanarsiv.domain.model.ChapterRef
import com.romanarsiv.domain.model.NovelSource
import com.romanarsiv.domain.usecase.ArchiveEvent
import com.romanarsiv.domain.usecase.ArchiveNovelUseCase
import com.romanarsiv.domain.usecase.DetectChaptersUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Uzun işler (bölüm tespiti ve indirmeler) burada, uygulama ömrü boyunca yaşayan bir kapsamda koşar;
 * ekran (Activity/ViewModel) kapansa da sürer. İş başlarken [onWorkStarted] çağrılır; Android'de bu,
 * süreci canlı tutan ve ilerlemeyi bildirimde gösteren ön plan hizmetini başlatır (ArchiveService).
 *
 * İndirmeler bir kuyruktadır: en fazla [maxParallel] tanesi aynı anda çalışır, gerisi sırada bekler.
 * Aynı siteye giden istekler yine de site başına istek aralığıyla sıralanır (JsoupPageLoader).
 * Tespit tek seferde birdir ve indirmelerle aynı anda yapılabilir.
 */
class ArchiveJobs(
    private val detectChapters: DetectChaptersUseCase,
    private val archiveNovel: ArchiveNovelUseCase,
    private val history: DownloadHistory = InMemoryDownloadHistory(),
    private val onWorkStarted: () -> Unit = {},
    private val maxParallel: () -> Int = { DEFAULT_PARALLEL },
    private val author: () -> String = { AppConfig.AUTHOR_NAME },
    private val clock: () -> Long = System::currentTimeMillis,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) {
    private val _detectState = MutableStateFlow<DetectState>(DetectState.Idle)
    val detectState: StateFlow<DetectState> = _detectState.asStateFlow()

    private val _detectResult = MutableStateFlow<DetectResult?>(null)
    val detectResult: StateFlow<DetectResult?> = _detectResult.asStateFlow()

    /** En yeni üstte. */
    private val _tasks = MutableStateFlow(history.load().sortedByDescending { it.id })
    val tasks: StateFlow<List<DownloadTask>> = _tasks.asStateFlow()

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages: Flow<UiMessage> = _messages.receiveAsFlow()

    private var detectJob: Job? = null
    private val running = HashMap<Long, Job>()
    private var nextId = (_tasks.value.maxOfOrNull { it.id } ?: 0L) + 1

    val isDetecting: Boolean get() = _detectState.value is DetectState.Loading

    /** Süren ya da sırada bekleyen indirme var mı? */
    val hasActiveDownloads: Boolean get() = _tasks.value.any { it.status.isActive }

    // ------------------------------------------------------------------ tespit

    /** [suggestedTitle] seçim ekranında önerilecek kitap adıdır (aranan roman adı). */
    fun detect(source: NovelSource, suggestedTitle: String) {
        if (isDetecting) return
        _detectResult.value = null
        _detectState.value = DetectState.Loading(source, 0)
        onWorkStarted()
        detectJob = scope.launch {
            try {
                val chapters = detectChapters(source) { found -> _detectState.value = DetectState.Loading(source, found) }
                _detectResult.value = DetectResult(source, chapters, suggestedTitle)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _messages.send(UiMessage.Failed(e))
            } finally {
                _detectState.value = DetectState.Idle
            }
        }
    }

    /** Arayüz tespit sonucunu aldı (seçim ekranını açtı). */
    fun consumeDetectResult(): DetectResult? = _detectResult.value.also { _detectResult.value = null }

    fun cancelDetection() {
        detectJob?.cancel()
        detectJob = null
        _detectState.value = DetectState.Idle
    }

    // ------------------------------------------------------------------ indirmeler

    /** Kuyruğa ekler ve kimliğini döndürür; bölüm yoksa null. */
    fun download(source: NovelSource, chapters: List<ChapterRef>, bookTitle: String, fileName: String): Long? {
        if (chapters.isEmpty()) return null
        val id = nextId++
        val task = DownloadTask(
            id = id,
            bookTitle = bookTitle.trim().ifEmpty { source.title },
            siteName = source.siteName,
            requestedChapters = chapters.size,
            status = DownloadStatus.QUEUED,
            createdAt = clock(),
            request = DownloadRequest(source, chapters, bookTitle, fileName),
        )
        _tasks.update { listOf(task) + it }
        onWorkStarted()
        schedule()
        return id
    }

    /** İptal edilen ya da başarısız olan indirmeyi baştan sıraya koyar. */
    fun retry(id: Long) {
        val task = find(id) ?: return
        if (!task.canRetry) return
        set(id) { it.copy(status = DownloadStatus.QUEUED, progress = null, error = null, finishedAt = null) }
        onWorkStarted()
        schedule()
    }

    fun cancelDownload(id: Long) {
        val task = find(id) ?: return
        if (!task.status.isActive) return
        running.remove(id)?.cancel()
        set(id) { it.copy(status = DownloadStatus.CANCELLED, finishedAt = clock()) }
        schedule()
    }

    /** Satırı listeden kaldırır (dosyaya dokunmaz). Süren indirme önce iptal edilir. */
    fun remove(id: Long) {
        running.remove(id)?.cancel()
        _tasks.update { list -> list.filterNot { it.id == id } }
        persist()
        schedule()
    }

    /** Bitmiş, başarısız ve iptal edilmiş satırları temizler. */
    fun clearFinished() {
        _tasks.update { list -> list.filter { it.status.isActive } }
        persist()
    }

    fun cancelAllDownloads() {
        _tasks.value.filter { it.status.isActive }.forEach { cancelDownload(it.id) }
    }

    fun cancelAll() {
        cancelDetection()
        cancelAllDownloads()
    }

    /** Boş yer oldukça sıradaki en eski indirmeleri başlatır. */
    private fun schedule() {
        val limit = maxParallel().coerceIn(1, MAX_PARALLEL)
        while (running.size < limit) {
            val next = _tasks.value.filter { it.status == DownloadStatus.QUEUED && it.id !in running }.minByOrNull { it.id } ?: return
            start(next)
        }
    }

    private fun start(task: DownloadTask) {
        val request = task.request ?: return
        set(task.id) {
            it.copy(
                status = DownloadStatus.RUNNING,
                progress = ArchiveProgress.FetchingChapter(0, request.chapters.size, request.chapters.first().title),
            )
        }
        // Başlamadan önce kaydedilir ki iş anında biterse (önbellek, test) finally bulsun.
        lateinit var job: Job
        job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                archiveNovel(request.chapters, request.bookTitle, request.fileName, author()).collect { event ->
                    when (event) {
                        is ArchiveEvent.Progress -> set(task.id) { it.copy(progress = event.progress) }
                        is ArchiveEvent.Completed -> {
                            set(task.id) {
                                it.copy(
                                    status = DownloadStatus.DONE,
                                    result = event.result,
                                    progress = null,
                                    finishedAt = clock(),
                                    // Bölüm metinleri ("sonraki" zincirinde) bellekte kalmasın.
                                    request = null,
                                )
                            }
                            persist()
                            _messages.send(UiMessage.Saved(task.id, event.result))
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                set(task.id) { it.copy(status = DownloadStatus.FAILED, error = e, progress = null, finishedAt = clock()) }
                _messages.send(UiMessage.Failed(e))
            } finally {
                if (running[task.id] === job) running.remove(task.id)
                schedule()
            }
        }
        running[task.id] = job
        job.start()
    }

    private fun find(id: Long) = _tasks.value.firstOrNull { it.id == id }

    private fun set(id: Long, change: (DownloadTask) -> DownloadTask) =
        _tasks.update { list -> list.map { if (it.id == id) change(it) else it } }

    private fun persist() {
        val done = _tasks.value.filter { it.status == DownloadStatus.DONE }
        runCatching { history.save(done) }
    }

    companion object {
        const val DEFAULT_PARALLEL = 3
        const val MAX_PARALLEL = 5
    }
}
