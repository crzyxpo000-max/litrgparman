package com.romanarsiv.domain.usecase

import com.romanarsiv.domain.AppConfig
import com.romanarsiv.domain.FileNames
import com.romanarsiv.domain.model.ArchiveProgress
import com.romanarsiv.domain.model.ArchiveResult
import com.romanarsiv.domain.model.ChapterRef
import com.romanarsiv.domain.model.NovelChapter
import com.romanarsiv.domain.repository.EpubStorage
import com.romanarsiv.domain.repository.NovelRepository
import com.romanarsiv.epub.EpubGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import com.romanarsiv.domain.AppException

sealed interface ArchiveEvent {
    data class Progress(val progress: ArchiveProgress) : ArchiveEvent
    data class Completed(val result: ArchiveResult) : ArchiveEvent
}

/** Seçilen bölümler → EPUB → İndirilenler. Akış iptal edilirse (toplayıcı iptali) indirme de durur. */
class ArchiveNovelUseCase(
    private val repository: NovelRepository,
    private val generator: EpubGenerator,
    private val storage: EpubStorage,
) {
    /**
     * [chapters] seçim ekranındaki sırayla seçilmiş bölümlerdir. [fileName] kullanıcının yazdığı ad;
     * boşsa ya da uzantısızsa [FileNames.normalize] kuralları uygulanır.
     */
    operator fun invoke(
        chapters: List<ChapterRef>,
        bookTitle: String,
        fileName: String,
        author: String = AppConfig.AUTHOR_NAME,
    ): Flow<ArchiveEvent> = channelFlow {
        require(chapters.isNotEmpty()) { "No chapters selected." }
        val title = bookTitle.trim().ifEmpty { "Kitap" }

        val fetched = ArrayList<NovelChapter>(chapters.size)
        var skipped = 0
        chapters.forEachIndexed { i, ref ->
            val chapter = repository.fetchChapter(ref, i + 1)
            if (chapter == null) skipped++ else fetched += chapter
            send(ArchiveEvent.Progress(ArchiveProgress.FetchingChapter(i + 1, chapters.size, chapter?.title ?: ref.title)))
        }
        if (fetched.isEmpty()) throw AppException(AppException.Reason.NOTHING_DOWNLOADED)

        send(ArchiveEvent.Progress(ArchiveProgress.BuildingEpub))
        val bytes = withContext(Dispatchers.Default) { generator.generate(fetched, title, author) }

        send(ArchiveEvent.Progress(ArchiveProgress.Saving))
        val saved = storage.save(FileNames.normalize(fileName, title), bytes)

        send(
            ArchiveEvent.Completed(
                ArchiveResult(
                    fileName = saved.fileName,
                    location = saved.location,
                    chapterCount = fetched.size,
                    skippedChapters = skipped,
                    uri = saved.uri,
                ),
            ),
        )
    }
}
