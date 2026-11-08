package com.romanarsiv.data.remote

import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class FallbackPageLoaderTest {

    private class Scripted(private val behaviour: (String, Int) -> Document) : PageLoader {
        var calls = 0
        override suspend fun load(url: String): Document = behaviour(url, ++calls)
    }

    private fun page(url: String) = Jsoup.parse("<title>ok</title>", url)
    private fun blocked(url: String, code: Int = 403): Nothing = throw HttpStatusException("engel", code, url)

    @Test
    fun `403 gelince tarayıcıya düşer, ikinci engelde site hep tarayıcıyla açılır`() = runTest {
        val http = Scripted { url, _ -> blocked(url) }
        val browser = Scripted { url, _ -> page(url) }
        val loader = FallbackPageLoader(http, browser)

        loader.load("https://s.com/1") // http 403 → çerez modu, tarayıcı
        loader.load("https://s.com/2") // çerezle http yine 403 → tarayıcı modu
        loader.load("https://s.com/3") // http hiç denenmez
        assertEquals(2, http.calls)
        assertEquals(3, browser.calls)
    }

    @Test
    fun `çerezler işe yararsa Jsoup ile devam edilir`() = runTest {
        val http = Scripted { url, n -> if (n == 1) blocked(url, 503) else page(url) }
        val browser = Scripted { url, _ -> page(url) }
        val loader = FallbackPageLoader(http, browser)
        repeat(3) { loader.load("https://s.com/$it") }
        assertEquals(3, http.calls)
        assertEquals(1, browser.calls)
    }

    @Test
    fun `404 tarayıcıya düşmez`() = runTest {
        val browser = Scripted { url, _ -> page(url) }
        val loader = FallbackPageLoader(Scripted { url, _ -> blocked(url, 404) }, browser)
        try {
            loader.load("https://s.com/yok")
            fail()
        } catch (e: HttpStatusException) {
            assertEquals(404, e.statusCode)
        }
        assertEquals(0, browser.calls)
    }

    @Test
    fun `doğrulama kullanıcıya sorulur, reddedilirse aynı site bir daha sorulmaz`() = runTest {
        val browser = Scripted { url, _ -> throw ChallengeRequiredException(url) }
        var asked = 0
        val loader = FallbackPageLoader(Scripted { url, _ -> blocked(url) }, browser) { asked++; false }

        repeat(2) {
            try {
                loader.load("https://s.com/$it")
                fail()
            } catch (_: ChallengeDeclinedException) {
            }
        }
        assertEquals(1, asked)
    }

    @Test
    fun `doğrulama geçilince sayfa yeniden açılır`() = runTest {
        val browser = Scripted { url, n -> if (n == 1) throw ChallengeRequiredException(url) else page(url) }
        val loader = FallbackPageLoader(Scripted { url, _ -> blocked(url) }, browser) { true }
        assertEquals("ok", loader.load("https://s.com/1").title())
        assertEquals(2, browser.calls)
    }

    @Test
    fun `aracı arayüz yanıt verene kadar bekler`() = runTest {
        val broker = ChallengeBroker()
        val answer = async { broker.requestSolve("https://s.com/x") }
        yield()
        assertEquals("https://s.com/x", broker.pendingUrl.value)
        broker.complete(true)
        assertTrue(answer.await())
        assertEquals(null, broker.pendingUrl.value)
    }

    @Test
    fun `doğrulama sayfası başlıktan ve öğeden tanınır`() {
        assertTrue(WebViewPageLoader.isChallenge("Just a moment...|0"))
        assertTrue(WebViewPageLoader.isChallenge("Bir dakika lütfen...|0"))
        assertTrue(WebViewPageLoader.isChallenge("Roman | Site|1"))
        assertFalse(WebViewPageLoader.isChallenge("Bölüm 3: Fırtına | Site|0"))
    }
}
