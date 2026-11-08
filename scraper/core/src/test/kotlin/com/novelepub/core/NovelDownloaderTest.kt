package com.novelepub.core

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream

class NovelDownloaderTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun chapterPage(n: Int, next: Int?) = """
        <html><head><title>Ch $n</title></head><body>
        <div class="text"><h1>Chapter $n</h1><p>Text of chapter $n, long enough to be the content.</p></div>
        ${next?.let { "<a href=\"/c/$it\">Next</a>" } ?: ""}
        </body></html>
    """.trimIndent()

    @Test
    fun `list download uses the cache on the second run`() = runTest {
        val pages = (1..3).associate { "https://x.test/c/$it" to chapterPage(it, null) }
        val fetcher = FakeFetcher(pages)
        val cache = FileChapterCache(tmp.newFolder("cache"))
        val downloader = NovelDownloader(fetcher, cache, delayMs = 10)
        val refs = pages.keys.map { ChapterRef("x", it) }
        val source = GenericSource(GenericConfig())

        val progress = ArrayList<Progress>()
        downloader.downloadList(source, refs) { progress += it }
        assertEquals(3, fetcher.requests.size)
        assertEquals(listOf(1, 2, 3), progress.map { it.done })
        assertEquals("Chapter 2", cache.get("https://x.test/c/2")?.title)

        downloader.downloadList(source, refs)
        assertEquals("no new requests", 3, fetcher.requests.size)
    }

    @Test
    fun `crawl follows next links and stops on loops`() = runTest {
        val pages = mapOf(
            "https://x.test/c/1" to chapterPage(1, 2),
            "https://x.test/c/2" to chapterPage(2, 3),
            "https://x.test/c/3" to chapterPage(3, 1), // loops back
        )
        val downloader = NovelDownloader(FakeFetcher(pages), MemoryChapterCache(), delayMs = 0)
        val refs = downloader.crawl(GenericSource(GenericConfig()), "https://x.test/c/1", maxChapters = 100)
        assertEquals(listOf("Chapter 1", "Chapter 2", "Chapter 3"), refs.map { it.title })

        val limited = NovelDownloader(FakeFetcher(pages), MemoryChapterCache(), delayMs = 0)
            .crawl(GenericSource(GenericConfig()), "https://x.test/c/1", maxChapters = 2)
        assertEquals(2, limited.size)
    }

    @Test
    fun `writes an epub from the cache`() = runTest {
        val pages = mapOf("https://x.test/c/1" to chapterPage(1, null))
        val downloader = NovelDownloader(FakeFetcher(pages), MemoryChapterCache(), delayMs = 0)
        val novel = NovelInfo("N", "A", "https://x.test/", chapters = listOf(ChapterRef("c1", "https://x.test/c/1")))
        val refs = downloader.downloadList(GenericSource(GenericConfig()), novel.chapters)
        val out = ByteArrayOutputStream()
        downloader.writeEpub(novel, refs, null, out)
        assertTrue(out.size() > 0)
    }

    @Test
    fun `a failing cover is skipped`() = runTest {
        assertNull(NovelDownloader(FailingFetcher(), MemoryChapterCache()).fetchCover("https://x.test/cover.jpg"))
        assertNull(NovelDownloader(FailingFetcher(), MemoryChapterCache()).fetchCover(null))
    }
}
