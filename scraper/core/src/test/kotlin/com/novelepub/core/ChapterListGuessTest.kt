package com.novelepub.core

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChapterListGuessTest {
    private val base = "https://reader.example/novel/paged"

    @Test
    fun `finds the chapter list and ignores buttons and footer links`() {
        val refs = GenericSource.guessChapterLinks(fixtureDoc("toc_desc_page1.html", base))
        assertEquals(listOf("Chapter 5 - End", "Chapter 4 - Fall", "Chapter 3 - Rise"), refs.map { it.title })
    }

    @Test
    fun `newest-first lists are reversed`() {
        val refs = listOf(ChapterRef("Chapter 10", "u10"), ChapterRef("Chapter 9", "u9"), ChapterRef("Chapter 8", "u8"))
        assertEquals(listOf("u8", "u9", "u10"), GenericSource.oldestFirst(refs).map { it.url })
        val ascending = listOf(ChapterRef("1. One", "a"), ChapterRef("2. Two", "b"))
        assertEquals(ascending, GenericSource.oldestFirst(ascending))
    }

    @Test
    fun `next page from the active page number`() {
        assertEquals("$base?page=2", GenericSource.tocNextPage(fixtureDoc("toc_desc_page1.html", base)))
        assertNull(GenericSource.tocNextPage(fixtureDoc("toc_desc_page2.html", "$base?page=2")))
    }

    @Test
    fun `paginated list is followed and ordered oldest first`() = runTest {
        val fetcher = FakeFetcher(mapOf(base to fixture("toc_desc_page1.html"), "$base?page=2" to fixture("toc_desc_page2.html")))
        val novel = GenericSource(GenericConfig(), pageDelayMs = 0).fetchNovel(base, fetcher)
        assertEquals("Paged Novel", novel.title)
        assertEquals((1..5).map { "$base/chapter-$it" }, novel.chapters.map { it.url })
        assertNull(novel.crawlFrom)
    }

    @Test
    fun `a page with no chapter list falls back to crawling`() = runTest {
        val url = "https://reader.example/novel/some/1"
        val novel = GenericSource(GenericConfig()).fetchNovel(url, FakeFetcher(mapOf(url to fixture("generic_chapter.html"))))
        assertEquals(url, novel.crawlFrom)
    }
}
