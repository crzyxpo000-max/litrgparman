package com.romanarsiv.background

import com.romanarsiv.domain.model.ArchiveProgress
import com.romanarsiv.domain.model.ChapterRef
import com.romanarsiv.domain.model.NovelSource
import com.romanarsiv.domain.usecase.ArchiveEvent
import com.romanarsiv.domain.usecase.ArchiveNovelUseCase
import com.romanarsiv.domain.usecase.DetectChaptersUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Uzun işler (bölüm tespiti ve indirme) burada, uygulama ömrü boyunca yaşayan bir kapsamda koşar;
 * ekran (Activity/ViewModel) kapansa da sürer. İş başlarken [onWorkStarted] çağrılır; Android'de bu,
 * süreci canlı tutan ve ilerlemeyi bildirimde gösteren ön plan hizmetini başlatır (ArchiveService).
 */
class ArchiveJobs(
    private val detectChapters: DetectChaptersUseCase,
    private val archiveNovel: ArchiveNovelUseCase,
    private val onWorkStarted: () -> Unit = {},
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) {
    private val _detectState = MutableStateFlow<DetectState>(DetectState.Idle)
    val detectState: StateFlow<DetectState> = _detectState.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val _detectResult = MutableStateFlow<DetectResult?>(null)
    val detectResult: StateFlow<DetectResult?> = _detectResult.asStateFlow()

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages: Flow<UiMessage> = _messages.receiveAsFlow()

    private var detectJob: Job? = null
    private var downloadJob: Job? = null

    val isBusy: Boolean
        get() = _detectState.value is DetectState.Loading || _downloadState.value is DownloadState.Loading

    /** [suggestedTitle] seçim ekranında önerilecek kitap adıdır (aranan roman adı). */
    fun detect(source: NovelSource, suggestedTitle: String) {
        if (isBusy) return
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
                _messages.send(UiMessage.Failed(describeError(e)))
            } finally {
                _detectState.value = DetectState.Idle
            }
        }
    }

    /** Arayüz tespit sonucunu aldı (seçim ekranını açtı). */
    fun consumeDetectResult(): DetectResult? = _detectResult.value.also { _detectResult.value = null }

    fun download(source: NovelSource, chapters: List<ChapterRef>, bookTitle: String, fileName: String) {
        if (isBusy || chapters.isEmpty()) return
        _downloadState.value = DownloadState.Loading(
            source,
            bookTitle,
            ArchiveProgress.FetchingChapter(0, chapters.size, chapters.first().title),
        )
        onWorkStarted()
        downloadJob = scope.launch {
            try {
                archiveNovel(chapters, bookTitle, fileName).collect { event ->
                    when (event) {
                        is ArchiveEvent.Progress -> _downloadState.value = DownloadState.Loading(source, bookTitle, event.progress)
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
                val message = describeError(e)
                _downloadState.value = DownloadState.Error(message)
                _messages.send(UiMessage.Failed(message))
            }
        }
    }

    fun cancelDetection() {
        detectJob?.cancel()
        detectJob = null
        _detectState.value = DetectState.Idle
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _downloadState.value = DownloadState.Idle
    }

    fun cancelAll() {
        cancelDetection()
        cancelDownload()
    }
}
