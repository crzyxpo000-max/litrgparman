package com.romanarsiv.domain.usecase

import com.romanarsiv.domain.AppConfig
import com.romanarsiv.domain.model.ArchiveProgress
import com.romanarsiv.domain.model.ArchiveResult
import com.romanarsiv.domain.model.NovelSource
import com.romanarsiv.domain.repository.EpubStorage
import com.romanarsiv.domain.repository.NovelRepository
import com.romanarsiv.epub.EpubGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext

sealed interface ArchiveEvent {
    data class Progress(val progress: ArchiveProgress) : ArchiveEvent
    data class Completed(val result: ArchiveResult) : ArchiveEvent
}

/** Kaynak → bölümler → EPUB → İndirilenler. Akış iptal edilirse (toplayıcı iptali) kazıma da durur. */
class ArchiveNovelUseCase(
    private val repository: NovelRepository,
    private val generator: EpubGenerator,
    private val storage: EpubStorage,
) {
    operator fun invoke(
        source: NovelSource,
        bookTitle: String,
        author: String = AppConfig.AUTHOR_NAME,
    ): Flow<ArchiveEvent> = channelFlow {
        send(ArchiveEvent.Progress(ArchiveProgress.DetectingChapters))

        val fetched = repository.fetchChapters(
            source = source,
            onChapter = { index, total, title ->
                send(ArchiveEvent.Progress(ArchiveProgress.FetchingChapter(index, total, title)))
            },
        )

        send(ArchiveEvent.Progress(ArchiveProgress.BuildingEpub))
        val title = bookTitle.trim().ifEmpty { source.title }
        val bytes = withContext(Dispatchers.Default) {
            generator.generate(fetched.chapters, title, author)
        }

        send(ArchiveEvent.Progress(ArchiveProgress.Saving))
        val saved = storage.save(title, bytes)

        send(
            ArchiveEvent.Completed(
                ArchiveResult(
                    fileName = saved.fileName,
                    location = saved.location,
                    chapterCount = fetched.chapters.size,
                    skippedChapters = fetched.skipped,
                ),
            ),
        )
    }
}
