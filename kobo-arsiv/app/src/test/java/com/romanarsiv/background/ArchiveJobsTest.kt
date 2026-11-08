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

    private class FakeRepo(
        private val refs: List<ChapterRef>,
        val gate: CompletableDeferred<Unit>? = null,
        /** Bölüm indirmesini bekletir (indirmelerin aynı anda sürdüğünü görmek için). */
        val chapterGate: CompletableDeferred<Unit>? = null,
    ) : NovelRepository {
        override suspend fun search(query: String) = emptyList<NovelSource>()
        override suspend fun listChapters(source: NovelSource, onFound: suspend (Int) -> Unit): List<ChapterRef> {
            onFound(refs.size)
            gate?.await()
            return refs
        }
        override suspend fun fetchChapter(ref: ChapterRef, position: Int): NovelChapter {
            chapterGate?.await()
            return NovelChapter(ref.title, "<p>${"metin ".repeat(60)}</p>", ref.url)
        }
    }

    private val storage = object : EpubStorage {
        override suspend fun save(fileName: String, bytes: ByteArray) = SavedFile(fileName, "Download/$fileName")
    }

    private fun TestScope.jobs(
        repo: NovelRepository,
        started: MutableList<String>,
        parallel: Int = 3,
        history: DownloadHistory = InMemoryDownloadHistory(),
    ) = ArchiveJobs(
        detectChapters = DetectChaptersUseCase(repo),
        archiveNovel = ArchiveNovelUseCase(repo, EpubGenerator(), storage),
        history = history,
        onWorkStarted = { started += "start" },
        maxParallel = { parallel },
        scope = TestScope(StandardTestDispatcher(testScheduler)),
    )

    private fun ArchiveJobs.status(id: Long?) = tasks.value.first { it.id == id }.status

    @Test
    fun `tespit sonucu arayüz alana kadar saklanır ve hizmet başlatılır`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val started = mutableListOf<String>()
        val jobs = jobs(FakeRepo(refs, gate), started)

        jobs.detect(source, "Romanım")
        advanceUntilIdle()
        assertEquals(DetectState.Loading(source, 3), jobs.detectState.value)
        assertTrue(jobs.isDetecting)

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
    fun `indirme biter, mesaj gönderilir, kayıt geçmişe yazılır`() = runTest {
        val started = mutableListOf<String>()
        val history = InMemoryDownloadHistory()
        val jobs = jobs(FakeRepo(refs), started, history = history)

        val id = jobs.download(source, refs.take(2), "Kitap", "Cilt 1")
        // EPUB üretimi Dispatchers.Default'ta (gerçek iş parçacığı): sanal zamanı ilerletmek yetmez, sonucu bekle.
        val list = jobs.tasks.first { tasks -> tasks.all { !it.status.isActive } }
        val task = list.single()
        assertEquals(id, task.id)
        assertEquals(DownloadStatus.DONE, task.status)
        val result = task.result!!
        assertEquals("Cilt 1.kepub.epub", result.fileName)
        assertEquals(2, result.chapterCount)
        assertNull("bitince yeniden deneme verisi tutulmaz", task.request)
        assertEquals(UiMessage.Saved(task.id, result), jobs.messages.first())
        assertEquals(listOf(task.id), history.saved.map { it.id })
        assertEquals(listOf("start"), started)
    }

    @Test
    fun `birden fazla indirme aynı anda sürer`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val jobs = jobs(FakeRepo(refs, chapterGate = gate), mutableListOf(), parallel = 3)

        val a = jobs.download(source, refs, "A", "a")
        val b = jobs.download(source, refs, "B", "b")
        advanceUntilIdle()
        assertEquals(DownloadStatus.RUNNING, jobs.status(a))
        assertEquals(DownloadStatus.RUNNING, jobs.status(b))
        assertTrue(jobs.hasActiveDownloads)
        // En yeni üstte.
        assertEquals(listOf(b, a), jobs.tasks.value.map { it.id })

        gate.complete(Unit)
        val done = jobs.tasks.first { tasks -> tasks.all { !it.status.isActive } }
        assertTrue(done.all { it.status == DownloadStatus.DONE })
    }

    @Test
    fun `sınır doluysa indirme sırada bekler, biri bitince başlar`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val jobs = jobs(FakeRepo(refs, chapterGate = gate), mutableListOf(), parallel = 1)

        val a = jobs.download(source, refs, "A", "a")
        val b = jobs.download(source, refs, "B", "b")
        advanceUntilIdle()
        assertEquals(DownloadStatus.RUNNING, jobs.status(a))
        assertEquals(DownloadStatus.QUEUED, jobs.status(b))

        jobs.cancelDownload(a!!)
        advanceUntilIdle()
        assertEquals(DownloadStatus.CANCELLED, jobs.status(a))
        assertEquals(DownloadStatus.RUNNING, jobs.status(b))
    }

    @Test
    fun `iptal edilen indirme yeniden denenebilir, kaldırılınca listeden çıkar`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val jobs = jobs(FakeRepo(refs, chapterGate = gate), mutableListOf())

        val id = jobs.download(source, refs, "A", "a")!!
        advanceUntilIdle()
        jobs.cancelDownload(id)
        advanceUntilIdle()
        assertTrue(jobs.tasks.value.single().canRetry)

        gate.complete(Unit)
        jobs.retry(id)
        val done = jobs.tasks.first { tasks -> tasks.single().status == DownloadStatus.DONE }
        assertEquals(3, done.single().result?.chapterCount)

        jobs.remove(id)
        assertTrue(jobs.tasks.value.isEmpty())
    }

    @Test
    fun `geçmişteki indirmeler açılışta listelenir ve yeni kimlik çakışmaz`() = runTest {
        val old = DownloadTask(id = 7, bookTitle = "Eski", siteName = "s.com", requestedChapters = 1, status = DownloadStatus.DONE)
        val jobs = jobs(FakeRepo(refs), mutableListOf(), history = InMemoryDownloadHistory(listOf(old)))
        assertEquals(listOf(7L), jobs.tasks.value.map { it.id })
        val id = jobs.download(source, refs, "Yeni", "y")
        assertEquals(8L, id)
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
