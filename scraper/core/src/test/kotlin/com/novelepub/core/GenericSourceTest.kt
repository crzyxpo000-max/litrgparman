package com.novelepub.core

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GenericSourceTest {
    private val base = "https://reader.example/novel/some"

    @Test
    fun `heuristics find content, title and next link`() {
        val source = GenericSource(GenericConfig(removeSelector = ".ad"))
        val chapter = source.parseChapter(fixtureDoc("generic_chapter.html", "$base/1"), ChapterRef("Chapter", "$base/1"))
        assertEquals("Chapter 1: Beginning", chapter.title)
        assertTrue(chapter.html, chapter.html.contains("a bell rang three times"))
        assertFalse(chapter.html, chapter.html.contains("Short."))
        assertFalse(chapter.html, chapter.html.contains("Patreon"))
        assertFalse(chapter.html, chapter.html.contains("Home"))
        assertEquals("$base/2", chapter.nextUrl)
        assertWellFormed("<div>${chapter.html}</div>")
    }

    @Test
    fun `explicit selectors win`() {
        val source = GenericSource(GenericConfig(titleSelector = "title", contentSelector = ".sidebar", nextSelector = ".missing"))
        val chapter = source.parseChapter(fixtureDoc("generic_chapter.html", "$base/1"), ChapterRef("Chapter", "$base/1"))
        assertEquals("Chapter 1 | Example Reader", chapter.title)
        assertEquals("<p>Short.</p>", chapter.html)
        assertNull(chapter.nextUrl)
    }

    @Test
    fun `toc mode lists chapter links without duplicates`() = runTest {
        val fetcher = FakeFetcher(mapOf(base to fixture("generic_toc.html")))
        val novel = GenericSource(GenericConfig(chapterLinkSelector = "ul.chapter-list a")).fetchNovel(base, fetcher)
        assertEquals("Some Novel", novel.title)
        assertEquals("A. Person", novel.author)
        assertEquals(listOf("https://reader.example/novel/some/1", "https://reader.example/novel/some/2"), novel.chapters.map { it.url })
        assertFalse(novel.isCrawl)
    }

    @Test
    fun `without a link selector the start page is crawled`() = runTest {
        val fetcher = FakeFetcher(mapOf("$base/1" to fixture("generic_chapter.html")))
        val novel = GenericSource(GenericConfig(novelTitle = "My Title", language = "tr")).fetchNovel("$base/1", fetcher)
        assertEquals("My Title", novel.title)
        assertEquals("$base/1", novel.crawlFrom)
        assertEquals("tr", novel.language)
    }

    @Test
    fun `url normalization adds a scheme and rejects junk`() {
        assertEquals("https://example.com/a", Sources.normalizeUrl("  example.com/a "))
        assertEquals("http://example.com/", Sources.normalizeUrl("http://example.com"))
        assertNull(Sources.normalizeUrl(""))
        assertNull(Sources.normalizeUrl("ftp://example.com"))
    }
}
