package com.upvotesaver.desktop

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** Drives a real Chromium-family browser; skipped where none is installed. */
class BrowserLoginTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun browser(): String? = BrowserLogin.findBrowser()
        ?: File("/opt/pw-browsers").listFiles()?.map { File(it, "chrome-linux64/chrome") }?.firstOrNull { it.isFile }?.path

    private fun server(page: String) = MockWebServer().apply {
        dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) =
                if (request.path == "/") MockResponse().setHeader("Content-Type", "text/html").setBody(page) else MockResponse().setBody("{}")
        }
        start()
    }

    private fun login(server: MockWebServer, exe: String) = BrowserLogin(
        profileDir = tmp.newFolder(), browserPath = { exe }, apiHost = server.hostName,
        startUrl = server.url("/").toString(), extraArgs = listOf("--no-sandbox", "--no-proxy-server"),
    )

    @Test
    fun `the token the page sends to its api is captured with the browser's user agent`() = runBlocking {
        val exe = browser()
        assumeTrue(exe != null)
        val server = server(
            """<script>
               fetch('/v2/x', {headers: {Authorization: 'Bearer visitor.token'}})
                 .then(() => fetch('/v2/y', {headers: {Authorization: 'Bearer user.token'}}));
               </script>""",
        )
        val offered = mutableListOf<String>()
        val result = login(server, exe!!).capture(headless = true, userAgent = "TestAgent/1.0", timeoutMs = 30_000) {
            offered += it.token
            it.token == "user.token"
        }
        assertEquals(listOf("visitor.token", "user.token"), offered)
        assertEquals("user.token", result?.token)
        assertEquals("TestAgent/1.0", result?.userAgent)
        server.shutdown()
    }

    @Test
    fun `no accepted token ends with nothing after the timeout`() = runBlocking {
        val exe = browser()
        assumeTrue(exe != null)
        val server = server("<p>no requests</p>")
        assertNull(login(server, exe!!).capture(headless = true, userAgent = null, timeoutMs = 3_000) { true })
        server.shutdown()
    }
}
