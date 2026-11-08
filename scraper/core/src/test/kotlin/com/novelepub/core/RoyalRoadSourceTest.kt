package com.novelepub.core

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoyalRoadSourceTest {
    private val source = RoyalRoadSource()
    private val fictionUrl = "https://www.royalroad.com/fiction/12345/the-test-fiction"

    @Test
    fun `chapter and fiction URLs normalize to the fiction page`() {
        assertEquals(fictionUrl, RoyalRoadSource.fictionUrl("$fictionUrl/chapter/100/chapter-1-start"))
        assertEquals(fictionUrl, RoyalRoadSource.fictionUrl("royalroad.com/fiction/12345/the-test-fiction?foo=1"))
        assertEquals("https://www.royalroad.com/fiction/12345", RoyalRoadSource.fictionUrl("https://www.royalroad.com/fiction/12345"))
        assertNull(RoyalRoadSource.fictionUrl("https://www.royalroad.com/home"))
    }

    @Test
    fun `matches royal road hosts only`() {
        assertTrue(source.matches("https://www.royalroad.com/fiction/1"))
        assertTrue(source.matches("https://royalroad.com/fiction/1"))
        assertFalse(source.matches("https://notroyalroad.com/fiction/1"))
        assertTrue(Sources.forUrl("https://www.royalroad.com/fiction/1") is RoyalRoadSource)
        assertNull(Sources.forUrl("https://example.com/novel"))
    }

    @Test
    fun `parses fiction page`() {
        val novel = source.parseNovel(fixtureDoc("rr_fiction.html", fictionUrl))
        assertEquals("The Test Fiction", novel.title)
        assertEquals("Jane Writer", novel.author)
        assertEquals("https://www.royalroad.com/covers/1.jpg", novel.coverUrl)
        assertTrue(novel.description.contains("A story about tests."))
        assertEquals(2, novel.chapters.size)
        assertEquals(ChapterRef("Chapter 1 - Start", "$fictionUrl/chapter/100/chapter-1-start"), novel.chapters[0])
        assertEquals("Chapter 2 - Middle", novel.chapters[1].title)
        assertFalse(novel.isCrawl)
    }

    @Test
    fun `falls back to the window chapters script`() {
        val url = "https://www.royalroad.com/fiction/999/script-fiction"
        val novel = source.parseNovel(fixtureDoc("rr_fiction_script.html", url))
        assertEquals("Script Fiction", novel.title)
        assertEquals("Sam Author", novel.author)
        assertEquals(listOf("Prologue", "One \"quoted\""), novel.chapters.map { it.title })
        assertEquals("$url/chapter/2/one", novel.chapters[1].url)
    }

    @Test
    fun `finds classes hidden by inline styles`() {
        val hidden = source.hiddenClasses(fixtureDoc("rr_chapter.html", fictionUrl))
        assertEquals(setOf("cmFkZXN0b3Jl", "cjYmM2NTg2"), hidden)
    }

    @Test
    fun `chapter text drops anti-theft paragraphs, scripts and links`() {
        val ref = ChapterRef("Ch 1", "$fictionUrl/chapter/100/chapter-1-start")
        val chapter = source.parseChapter(fixtureDoc("rr_chapter.html", ref.url), ref)
        assertEquals("Chapter 1 - Start", chapter.title)
        val html = chapter.html
        assertTrue(html, html.contains("<em>emphasis</em>"))
        assertTrue(html, html.contains("a link."))
        assertFalse(html, html.contains("<a"))
        assertFalse(html, html.contains("unlawfully"))
        assertFalse(html, html.contains("Stolen content"))
        assertFalse(html, html.contains("alert"))
        assertTrue(html, html.contains("Still visible."))
        assertTrue(html, html.contains("<td colspan=\"2\">Status</td>"))
        assertTrue(html, html.contains("[Map of the city]"))
        assertFalse(html, html.contains("<img"))
        assertFalse(html, html.contains("Thanks for reading"))
        assertWellFormed("<div>$html</div>", "chapter")
    }

    @Test
    fun `author notes are included when asked, in order`() {
        val ref = ChapterRef("Ch 1", "$fictionUrl/chapter/100/chapter-1-start")
        val html = RoyalRoadSource(includeAuthorNotes = true).parseChapter(fixtureDoc("rr_chapter.html", ref.url), ref).html
        val before = html.indexOf("Thanks for reading!")
        val text = html.indexOf("First paragraph")
        val after = html.indexOf("See you next week.")
        assertTrue(html, before in 0 until text)
        assertTrue(html, after > text)
        assertWellFormed("<div>$html</div>", "chapter with notes")
    }

    @Test
    fun `fetchNovel requests the normalized fiction page`() = runTest {
        val fetcher = FakeFetcher(mapOf(fictionUrl to fixture("rr_fiction.html")))
        val novel = source.fetchNovel("$fictionUrl/chapter/101/chapter-2-middle", fetcher)
        assertEquals(listOf(fictionUrl), fetcher.requests)
        assertEquals(2, novel.chapters.size)
    }
}
