package com.romanarsiv.data.repository

import com.romanarsiv.data.remote.PageLoader
import com.romanarsiv.domain.model.NovelSource
import kotlinx.coroutines.test.runTest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class NovelRepositoryImplTest {

    private val body = "Kahraman yürüdü, gökyüzü karardı ve Sistem konuştu. ".repeat(10)

    private class FakeLoader(private val pages: Map<String, String>) : PageLoader {
        val requested = ArrayList<String>()
        override suspend fun load(url: String): Document {
            requested += url
            val html = pages[url] ?: throw IOException("404 $url")
            return Jsoup.parse(html, url)
        }
    }

    private fun chapterPage(n: Int, next: Int?) = """
        <html><body><h1>Bölüm $n</h1><div class="entry-content"><p>$n. $body</p><p>$body</p></div>
        ${next?.let { """<a href="https://s.com/b/$it">Sonraki</a>""" } ?: ""}</body></html>
    """.trimIndent()

    private val indexPages = mapOf(
        "https://s.com/roman" to """
            <html><body><p>Kısa tanıtım.</p>
            <a href="https://s.com/b/3">Bölüm 3</a><a href="https://s.com/b/2">Bölüm 2</a>
            <a rel="next" href="https://s.com/roman?sayfa=2">2</a></body></html>
        """.trimIndent(),
        "https://s.com/roman?sayfa=2" to """
            <html><body><a href="https://s.com/b/1">Bölüm 1</a><a href="https://s.com/b/4">Bölüm 4</a></body></html>
        """.trimIndent(),
        "https://s.com/b/1" to chapterPage(1, 2),
        "https://s.com/b/2" to chapterPage(2, 3),
        "https://s.com/b/3" to chapterPage(3, null),
        // b/4 yok
    )

    @Test
    fun `dizin sayfaları birleştirilir ve tespit sırasında bölüm indirilmez`() = runTest {
        val loader = FakeLoader(indexPages)
        val repo = NovelRepositoryImpl(loader, providers = emptyList())
        val found = ArrayList<Int>()
        val refs = repo.listChapters(NovelSource("https://s.com/roman", "s.com", "Roman")) { found += it }

        assertEquals(listOf("Bölüm 1", "Bölüm 2", "Bölüm 3", "Bölüm 4"), refs.map { it.title })
        assertEquals(listOf(2, 4), found)
        assertEquals(listOf("https://s.com/roman", "https://s.com/roman?sayfa=2"), loader.requested)
        assertTrue(refs.all { it.preloaded == null })
    }

    @Test
    fun `yalnızca istenen bölüm indirilir, alınamayan null döner`() = runTest {
        val loader = FakeLoader(indexPages)
        val repo = NovelRepositoryImpl(loader, providers = emptyList())
        val refs = repo.listChapters(NovelSource("https://s.com/roman", "s.com", "Roman"))
        loader.requested.clear()

        val second = repo.fetchChapter(refs[1], 2)
        assertEquals("Bölüm 2", second?.title)
        assertEquals(listOf("https://s.com/b/2"), loader.requested)
        assertNull(repo.fetchChapter(refs[3], 4))
    }

    @Test
    fun `sonraki zincirinde bölümler tespitte indirilir, döngüde durur`() = runTest {
        val loader = FakeLoader(
            mapOf(
                "https://s.com/b/1" to chapterPage(1, 2),
                "https://s.com/b/2" to chapterPage(2, 3),
                "https://s.com/b/3" to chapterPage(3, 1),
            ),
        )
        val repo = NovelRepositoryImpl(loader, providers = emptyList())
        val refs = repo.listChapters(NovelSource("https://s.com/b/1", "s.com", "Bölüm 1"))
        assertEquals(3, refs.size)
        assertEquals(3, loader.requested.size)
        assertTrue(refs.all { it.preloaded != null })

        // Önceden indirilmiş bölüm için yeniden istek yapılmaz.
        assertNotNull(repo.fetchChapter(refs[2], 3))
        assertEquals(3, loader.requested.size)
    }

    @Test
    fun `URL ile arama tek kaynak döndürür`() = runTest {
        val loader = FakeLoader(mapOf("https://s.com/roman" to "<html><head><title>Romanım</title></head><body></body></html>"))
        val results = NovelRepositoryImpl(loader, providers = emptyList()).search("https://s.com/roman")
        assertEquals(listOf(NovelSource("https://s.com/roman", "s.com", "Romanım")), results)
    }
}
