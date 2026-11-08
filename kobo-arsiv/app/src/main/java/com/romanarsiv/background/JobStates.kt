package com.romanarsiv.background

import com.romanarsiv.domain.model.ArchiveProgress
import com.romanarsiv.domain.model.ArchiveResult
import com.romanarsiv.domain.model.ChapterRef
import com.romanarsiv.domain.model.NovelSource

/** Bölüm tespiti (kaynağa dokunulduktan sonra, seçim ekranından önce). */
sealed interface DetectState {
    data object Idle : DetectState
    data class Loading(val source: NovelSource, val found: Int) : DetectState
}

enum class DownloadStatus {
    /** Sırada: aynı anda çalışan indirme sınırı dolu. */
    QUEUED,
    RUNNING,
    DONE,
    FAILED,
    CANCELLED,
    ;

    val isActive: Boolean get() = this == QUEUED || this == RUNNING
}

/** Bir indirmeyi yeniden başlatmak için gereken her şey (bitmiş kayıtlarda tutulmaz). */
data class DownloadRequest(
    val source: NovelSource,
    val chapters: List<ChapterRef>,
    val bookTitle: String,
    val fileName: String,
)

/**
 * İndirilenler sekmesindeki bir satır. Biten indirmeler ([DownloadStatus.DONE]) diske de yazılır
 * ve uygulama yeniden açıldığında listede kalır; diğerleri yalnızca bu oturumda yaşar.
 */
data class DownloadTask(
    val id: Long,
    val bookTitle: String,
    val siteName: String,
    /** İstenen bölüm sayısı. */
    val requestedChapters: Int,
    val status: DownloadStatus,
    val progress: ArchiveProgress? = null,
    val result: ArchiveResult? = null,
    val error: Throwable? = null,
    val createdAt: Long = 0L,
    val finishedAt: Long? = null,
    val request: DownloadRequest? = null,
) {
    /** Kaba ilerleme (0–1); bilinmiyorsa null. */
    val fraction: Float?
        get() = when (val p = progress) {
            is ArchiveProgress.FetchingChapter -> if (p.total > 0) p.index.toFloat() / p.total else null
            ArchiveProgress.BuildingEpub, ArchiveProgress.Saving -> 1f
            else -> if (status == DownloadStatus.DONE) 1f else null
        }

    val canRetry: Boolean get() = request != null && (status == DownloadStatus.FAILED || status == DownloadStatus.CANCELLED)
}

/** Tamamlanmış ama arayüzün henüz almadığı tespit (uygulama arka plandayken bitmiş olabilir). */
data class DetectResult(val source: NovelSource, val chapters: List<ChapterRef>, val suggestedTitle: String)

/** Tek seferlik bildirimler (Snackbar). */
sealed interface UiMessage {
    data class Saved(val taskId: Long, val result: ArchiveResult) : UiMessage
    data class Failed(val error: Throwable) : UiMessage
}

/** Biten indirmelerin kalıcı listesi. Android'de JSON dosyası (DownloadHistoryFile), testlerde bellek. */
interface DownloadHistory {
    fun load(): List<DownloadTask>
    fun save(tasks: List<DownloadTask>)
}

class InMemoryDownloadHistory(initial: List<DownloadTask> = emptyList()) : DownloadHistory {
    var saved: List<DownloadTask> = initial
        private set

    override fun load() = saved
    override fun save(tasks: List<DownloadTask>) {
        saved = tasks
    }
}
