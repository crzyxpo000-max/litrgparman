package com.novelepub.core

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class OkHttpFetcherTest {
    private lateinit var server: MockWebServer

    @Before fun start() { server = MockWebServer().apply { start() } }
    @After fun stop() { server.shutdown() }

    @Test
    fun `retries server errors then succeeds`() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))
        server.enqueue(MockResponse().setResponseCode(429).setHeader("Retry-After", "0"))
        server.enqueue(MockResponse().setBody("<p>ok</p>").setHeader("Content-Type", "text/html; charset=utf-8"))
        val result = OkHttpFetcher(baseBackoffMs = 1).get(server.url("/a").toString())
        assertEquals("ok", result.document().text())
        assertEquals(3, server.requestCount)
        assertEquals(OkHttpFetcher.DEFAULT_USER_AGENT, server.takeRequest().getHeader("User-Agent"))
    }

    @Test
    fun `client errors are not retried`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))
        try {
            OkHttpFetcher(baseBackoffMs = 1).get(server.url("/missing").toString())
            fail("expected HttpStatusException")
        } catch (e: HttpStatusException) {
            assertEquals(404, e.status)
        }
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `charset comes from the page when the header has none`() = runTest {
        val html = "<html><head><meta charset=\"windows-1254\"></head><body>Ğ</body></html>"
        server.enqueue(MockResponse().setBody(okio.Buffer().write(html.toByteArray(charset("windows-1254")))).setHeader("Content-Type", "text/html"))
        val doc = OkHttpFetcher().get(server.url("/tr").toString()).document()
        assertEquals("Ğ", doc.body().text())
    }
}
