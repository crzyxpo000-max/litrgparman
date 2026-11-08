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

sealed interface DownloadState {
    data object Idle : DownloadState
    data class Loading(val source: NovelSource, val bookTitle: String, val progress: ArchiveProgress) : DownloadState
    data class Success(val result: ArchiveResult) : DownloadState
    data class Error(val message: String) : DownloadState
}

/** Tamamlanmış ama arayüzün henüz almadığı tespit (uygulama arka plandayken bitmiş olabilir). */
data class DetectResult(val source: NovelSource, val chapters: List<ChapterRef>, val suggestedTitle: String)

/** Tek seferlik bildirimler (Snackbar). */
sealed interface UiMessage {
    data class Saved(val result: ArchiveResult) : UiMessage
    data class Failed(val message: String) : UiMessage
}
