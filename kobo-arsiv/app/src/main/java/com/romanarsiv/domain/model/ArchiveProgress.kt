package com.romanarsiv.domain.model

/** Arşivleme sürecinin aşamaları; ilerleme çubuğunda gösterilir. */
sealed interface ArchiveProgress {
    data object DetectingChapters : ArchiveProgress
    data class FetchingChapter(val index: Int, val total: Int, val title: String) : ArchiveProgress
    data object BuildingEpub : ArchiveProgress
    data object Saving : ArchiveProgress
}

/** Kaydedilen dosyanın özeti. */
data class ArchiveResult(
    val fileName: String,
    val location: String,
    val chapterCount: Int,
    val skippedChapters: Int,
)
