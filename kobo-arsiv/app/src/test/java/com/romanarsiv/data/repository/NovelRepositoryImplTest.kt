package com.romanarsiv.data.repository

import com.romanarsiv.data.remote.PageLoader
import com.romanarsiv.data.storage.FileHelper
import com.romanarsiv.domain.model.NovelSource
import kotlinx.coroutines.test.runTest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.junit.Assert.assertEquals
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

    @Test
    fun `dizin sayfası ve ikinci dizin sayfası birleştirilir, eksik bölüm atlanır`() = runTest {
        val loader = FakeLoader(
            mapOf(
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
                // b/4 yok: atlanmalı
            ),
        )
        val repo = NovelRepositoryImpl(loader, providers = emptyList())
        val progress = ArrayList<Pair<Int, Int>>()
        val result = repo.fetchChapters(NovelSource("https://s.com/roman", "s.com", "Roman")) { i, total, _ ->
            progress += i to total
        }
        assertEquals(listOf("Bölüm 1", "Bölüm 2", "Bölüm 3"), result.chapters.map { it.title })
        assertEquals(1, result.skipped)
        assertEquals(listOf(1 to 4, 2 to 4, 3 to 4), progress)
    }

    @Test
    fun `bölüm sayfasından sonraki zinciri izlenir, döngüde durur`() = runTest {
        val loader = FakeLoader(
            mapOf(
                "https://s.com/b/1" to chapterPage(1, 2),
                "https://s.com/b/2" to chapterPage(2, 3),
                "https://s.com/b/3" to chapterPage(3, 1),
            ),
        )
        val repo = NovelRepositoryImpl(loader, providers = emptyList())
        val result = repo.fetchChapters(NovelSource("https://s.com/b/1", "s.com", "Bölüm 1"))
        assertEquals(3, result.chapters.size)
        assertEquals(3, loader.requested.size)
    }

    @Test
    fun `URL ile arama tek kaynak döndürür`() = runTest {
        val loader = FakeLoader(mapOf("https://s.com/roman" to "<html><head><title>Romanım</title></head><body></body></html>"))
        val results = NovelRepositoryImpl(loader, providers = emptyList()).search("https://s.com/roman")
        assertEquals(listOf(NovelSource("https://s.com/roman", "s.com", "Romanım")), results)
    }

    @Test
    fun `kepub dosya adı güvenli ve Türkçe harfleri korur`() {
        assertEquals("Yükseliş Protokolü_ Cilt 1.kepub.epub", FileHelper.kepubFileName("Yükseliş Protokolü: Cilt 1"))
        assertEquals("Kitap.kepub.epub", FileHelper.kepubFileName("  ...  "))
    }
}
