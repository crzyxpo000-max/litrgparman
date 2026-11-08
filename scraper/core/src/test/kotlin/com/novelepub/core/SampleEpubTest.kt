package com.novelepub.core

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Runs the whole pipeline on the Royal Road fixtures and leaves the book at
 * build/sample.epub, so it can be opened in a reader or checked with W3C epubcheck.
 */
class SampleEpubTest {
    @Test
    fun `royal road fixtures become an epub`() = runTest {
        val fiction = "https://www.royalroad.com/fiction/12345/the-test-fiction"
        val pages = mapOf(
            fiction to fixture("rr_fiction.html"),
            "$fiction/chapter/100/chapter-1-start" to fixture("rr_chapter.html"),
            "$fiction/chapter/101/chapter-2-middle" to fixture("rr_chapter.html").replace("Chapter 1 - Start", "Chapter 2 - Middle"),
        )
        val fetcher = FakeFetcher(pages)
        val source = RoyalRoadSource(includeAuthorNotes = true)
        val novel = source.fetchNovel(fiction, fetcher)
        val downloader = NovelDownloader(fetcher, MemoryChapterCache(), delayMs = 0)
        val refs = downloader.downloadList(source, novel.chapters)
        val png = javaClass.getResource("/fixtures/cover.png")!!.readBytes()

        val file = File("build/sample.epub")
        file.outputStream().use { downloader.writeEpub(novel, refs, Cover.detect(png), it) }
        assertTrue(file.length() > 0)
    }
}
