package com.novelepub.core

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchTest {
    @Test
    fun `royal road search page`() {
        val results = RoyalRoadSearch().parse(fixture("rr_search.html"), "https://www.royalroad.com/fictions/search?title=test")
        assertEquals(listOf("The Test Fiction", "Another & One"), results.map { it.title })
        assertEquals("https://www.royalroad.com/fiction/12345/the-test-fiction", results[0].url)
        assertEquals("A story about tests.", results[0].snippet)
        assertEquals("Royal Road", results[0].siteName)
    }

    @Test
    fun `brave json strips markup and skips entries without url`() {
        val results = BraveSearch("key").parse(fixture("brave.json"))
        assertEquals(4, results.size)
        assertEquals("The Test Fiction | Royal Road", results[0].title)
        assertEquals("A story about tests.", results[0].snippet)
    }

    @Test
    fun `brave sends the key as a header`() = runTest {
        val fetcher = FakeFetcher(mapOf("https://api.search.brave.com/res/v1/web/search?q=a%20b&count=20" to fixture("brave.json")))
        BraveSearch("secret").search("a b", fetcher)
        assertEquals("secret", fetcher.headers.single()["X-Subscription-Token"])
    }

    @Test
    fun `service merges, filters and ranks results`() = runTest {
        val fetcher = FakeFetcher(
            mapOf(
                "https://www.royalroad.com/fictions/search?title=Test%20Fiction" to fixture("rr_search.html"),
                "https://api.search.brave.com/res/v1/web/search?q=Test%20Fiction%20novel&count=20" to fixture("brave.json"),
                "https://searx.example/search?q=Test%20Fiction%20novel&format=json" to fixture("searx.json"),
            ),
        )
        val service = SearchService(listOf(BraveSearch("k"), SearxSearch("https://searx.example/"), RoyalRoadSearch()))
        val outcome = service.search(" Test Fiction ", fetcher)
        assertTrue(outcome.errors.toString(), outcome.errors.isEmpty())
        val urls = outcome.results.map { it.url }
        // Royal Road (dedicated parser) results first, the duplicate Royal Road hit only once.
        assertEquals(
            listOf(
                "https://www.royalroad.com/fiction/12345/the-test-fiction",
                "https://www.royalroad.com/fiction/777/other",
                "https://reader.example/novel/test-fiction",
                "https://books.example/test-fiction",
            ),
            urls,
        )
        assertEquals("Brave", outcome.results[0].provider)
    }

    @Test
    fun `a failing provider is reported without hiding the others`() = runTest {
        val fetcher = FakeFetcher(mapOf("https://www.royalroad.com/fictions/search?title=x" to fixture("rr_search.html")))
        val outcome = SearchService(listOf(RoyalRoadSearch(), BraveSearch("bad"))).search("x", fetcher)
        assertEquals(2, outcome.results.size)
        assertTrue(outcome.errors.containsKey("Brave"))
    }

    @Test
    fun `book shops and wikis are not readable`() {
        assertTrue(SearchService.isNotReadable("en.wikipedia.org"))
        assertTrue(SearchService.isNotReadable("www.amazon.co.uk"))
        assertTrue(SearchService.isNotReadable("novelupdates.com"))
        assertFalse(SearchService.isNotReadable("royalroad.com"))
        assertFalse(SearchService.isNotReadable("xamazon.example"))
    }
}
