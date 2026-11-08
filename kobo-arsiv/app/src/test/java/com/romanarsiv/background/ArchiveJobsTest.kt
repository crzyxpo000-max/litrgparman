package com.romanarsiv.background

import com.romanarsiv.domain.model.ChapterRef
import com.romanarsiv.domain.model.NovelChapter
import com.romanarsiv.domain.model.NovelSource
import com.romanarsiv.domain.repository.EpubStorage
import com.romanarsiv.domain.repository.NovelRepository
import com.romanarsiv.domain.repository.SavedFile
import com.romanarsiv.domain.usecase.ArchiveNovelUseCase
import com.romanarsiv.domain.usecase.DetectChaptersUseCase
import com.romanarsiv.epub.EpubGenerator
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchiveJobsTest {

    private val source = NovelSource("https://s.com/roman", "s.com", "Roman")
    private val refs = (1..3).map { ChapterRef("https://s.com/b/$it", "Bölüm $it") }

    private class FakeRepo(private val refs: List<ChapterRef>, val gate: CompletableDeferred<Unit>? = null) : NovelRepository {
        override suspend fun search(query: String) = emptyList<NovelSource>()
        override suspend fun listChapters(source: NovelSource, onFound: suspend (Int) -> Unit): List<ChapterRef> {
            onFound(refs.size)
            gate?.await()
            return refs
        }
        override suspend fun fetchChapter(ref: ChapterRef, position: Int) =
            NovelChapter(ref.title, "<p>${"metin ".repeat(60)}</p>", ref.url)
    }

    private val storage = object : EpubStorage {
        override suspend fun save(fileName: String, bytes: ByteArray) = SavedFile(fileName, "Download/$fileName")
    }

    private fun TestScope.jobs(repo: NovelRepository, started: MutableList<String>) = ArchiveJobs(
        detectChapters = DetectChaptersUseCase(repo),
        archiveNovel = ArchiveNovelUseCase(repo, EpubGenerator(), storage),
        onWorkStarted = { started += "start" },
        scope = TestScope(StandardTestDispatcher(testScheduler)),
    )

    @Test
    fun `tespit sonucu arayüz alana kadar saklanır ve hizmet başlatılır`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val started = mutableListOf<String>()
        val jobs = jobs(FakeRepo(refs, gate), started)

        jobs.detect(source, "Romanım")
        advanceUntilIdle()
        assertEquals(DetectState.Loading(source, 3), jobs.detectState.value)
        assertTrue(jobs.isBusy)

        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(DetectState.Idle, jobs.detectState.value)
        val result = jobs.consumeDetectResult()
        assertEquals("Romanım", result?.suggestedTitle)
        assertEquals(3, result?.chapters?.size)
        assertNull(jobs.consumeDetectResult())
        assertEquals(listOf("start"), started)
    }

    @Test
    fun `indirme biter, mesaj gönderilir, durum başarı olur`() = runTest {
        val started = mutableListOf<String>()
        val jobs = jobs(FakeRepo(refs), started)

        jobs.download(source, refs.take(2), "Kitap", "Cilt 1")
        // EPUB üretimi Dispatchers.Default'ta (gerçek iş parçacığı): sanal zamanı ilerletmek yetmez, sonucu bekle.
        val state = jobs.downloadState.first { it !is DownloadState.Loading } as DownloadState.Success
        assertEquals("Cilt 1.kepub.epub", state.result.fileName)
        assertEquals(2, state.result.chapterCount)
        assertEquals(UiMessage.Saved(state.result), jobs.messages.first())
        assertEquals(listOf("start"), started)
    }

    @Test
    fun `iptal edilen tespit sonuç bırakmaz`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val jobs = jobs(FakeRepo(refs, gate), mutableListOf())
        jobs.detect(source, "x")
        advanceUntilIdle()
        jobs.cancelDetection()
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(DetectState.Idle, jobs.detectState.value)
        assertNull(jobs.detectResult.value)
    }
}
