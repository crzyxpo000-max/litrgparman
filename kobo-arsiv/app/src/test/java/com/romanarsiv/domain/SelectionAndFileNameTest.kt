package com.romanarsiv.domain

import com.romanarsiv.domain.model.ChapterRef
import com.romanarsiv.domain.model.NovelChapter
import com.romanarsiv.domain.model.NovelSource
import com.romanarsiv.domain.repository.EpubStorage
import com.romanarsiv.domain.repository.NovelRepository
import com.romanarsiv.domain.repository.SavedFile
import com.romanarsiv.domain.usecase.ArchiveEvent
import com.romanarsiv.domain.usecase.ArchiveNovelUseCase
import com.romanarsiv.epub.EpubGenerator
import com.romanarsiv.presentation.MainViewModel
import com.romanarsiv.presentation.SelectionState
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SelectionAndFileNameTest {

    @Test
    fun `dosya adı önerisi ve kullanıcı adı kuralları`() {
        assertEquals("Yükseliş Protokolü_ Cilt 1.kepub.epub", FileNames.suggest("Yükseliş Protokolü: Cilt 1"))
        assertEquals("Kitap.kepub.epub", FileNames.suggest("  ...  "))
        // Uzantısız ad → .kepub.epub eklenir
        assertEquals("Cilt 2.kepub.epub", FileNames.normalize("Cilt 2", "Kitap"))
        // Düz .epub isteniyorsa korunur
        assertEquals("Cilt 2.epub", FileNames.normalize("Cilt 2.epub", "Kitap"))
        assertEquals("Cilt 2.kepub.epub", FileNames.normalize("Cilt 2.KEPUB.EPUB", "Kitap"))
        // Boş ya da yalnızca uzantı → kitap adından
        assertEquals("Romanım.kepub.epub", FileNames.normalize("  .kepub.epub ", "Romanım"))
        assertEquals("a_b_c.kepub.epub", FileNames.normalize("a/b\\c", "Kitap"))
    }

    @Test
    fun `aralık seçimi 1 tabanlı, iki uç dahil ve kırpılır`() {
        assertEquals(setOf(4, 5, 6, 7, 8, 9), MainViewModel.rangeIndices(5, 10, 20))
        assertEquals(setOf(4, 5, 6, 7, 8, 9), MainViewModel.rangeIndices(10, 5, 20))
        assertEquals(setOf(17, 18, 19), MainViewModel.rangeIndices(18, 999, 20))
        assertEquals(setOf(0), MainViewModel.rangeIndices(0, 1, 20))
        assertEquals(emptySet<Int>(), MainViewModel.rangeIndices(1, 5, 0))
    }

    @Test
    fun `seçili bölümler okuma sırasında döner`() {
        val chapters = (1..5).map { ChapterRef("https://s.com/$it", "Bölüm $it") }
        val state = SelectionState(NovelSource("u", "s", "t"), chapters, setOf(4, 0, 2), "Kitap", "Kitap.kepub.epub")
        assertEquals(listOf("Bölüm 1", "Bölüm 3", "Bölüm 5"), state.selectedChapters().map { it.title })
    }

    @Test
    fun `yalnızca seçilen bölümler indirilir ve verilen adla kaydedilir`() = runTest {
        val fetched = ArrayList<String>()
        val repo = object : NovelRepository {
            override suspend fun search(query: String) = emptyList<NovelSource>()
            override suspend fun listChapters(source: NovelSource, onFound: suspend (Int) -> Unit) = emptyList<ChapterRef>()
            override suspend fun fetchChapter(ref: ChapterRef, position: Int): NovelChapter? {
                fetched += ref.url
                return if (ref.url.endsWith("x")) null else NovelChapter(ref.title, "<p>${"metin ".repeat(50)}</p>", ref.url)
            }
        }
        var savedName = ""
        val storage = object : EpubStorage {
            override suspend fun save(fileName: String, bytes: ByteArray): SavedFile {
                savedName = fileName
                return SavedFile(fileName, "Download/$fileName")
            }
        }
        val useCase = ArchiveNovelUseCase(repo, EpubGenerator(), storage)
        val selected = listOf(ChapterRef("https://s.com/3", "Bölüm 3"), ChapterRef("https://s.com/x", "Bozuk"), ChapterRef("https://s.com/7", "Bölüm 7"))

        val events = useCase(selected, "Kitap", "Cilt 1").toList()
        val result = (events.last() as ArchiveEvent.Completed).result

        assertEquals(listOf("https://s.com/3", "https://s.com/x", "https://s.com/7"), fetched)
        assertEquals("Cilt 1.kepub.epub", savedName)
        assertEquals(2, result.chapterCount)
        assertEquals(1, result.skippedChapters)
    }
}
