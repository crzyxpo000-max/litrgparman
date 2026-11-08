package com.romanarsiv.data.remote

import com.romanarsiv.domain.AppException
import com.romanarsiv.domain.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException

/** HTML sayfası getirir. Testlerde sahte bir uygulamayla değiştirilir. */
interface PageLoader {
    suspend fun load(url: String): Document
}

/**
 * Jsoup tabanlı yükleyici: site başına istek aralığı, zaman aşımı ve geçici hatalarda yeniden deneme.
 * Karakter kodlaması HTTP başlığından ya da `<meta charset>`'tan Jsoup tarafından çözülür.
 *
 * [userAgent] ve [cookies] Android'de WebView'den gelir: tarayıcı doğrulamasını (Cloudflare vb.) geçen
 * WebView'in çerezleri ve aynı User-Agent ile Jsoup istekleri de kabul edilir.
 */
class JsoupPageLoader(
    private val userAgent: () -> String = { USER_AGENT },
    private val cookies: (url: String) -> String? = { null },
    private val requestDelayMs: Long = AppConfig.REQUEST_DELAY_MS,
    private val timeoutMs: Int = 20_000,
    private val maxAttempts: Int = 3,
) : PageLoader {

    private val mutex = Mutex()
    private val lastRequestAt = HashMap<String, Long>()

    override suspend fun load(url: String): Document = withContext(Dispatchers.IO) {
        var lastError: IOException? = null
        for (attempt in 1..maxAttempts) {
            throttle(UrlUtils.hostOf(url) ?: url)
            try {
                val connection = Jsoup.connect(url)
                    .userAgent(userAgent())
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "tr-TR,tr;q=0.9,en-US;q=0.7,en;q=0.6")
                    .header("Upgrade-Insecure-Requests", "1")
                    .header("Sec-Fetch-Dest", "document")
                    .header("Sec-Fetch-Mode", "navigate")
                    .header("Sec-Fetch-Site", "none")
                    .header("Sec-Fetch-User", "?1")
                    .timeout(timeoutMs)
                    .followRedirects(true)
                    .maxBodySize(10 * 1024 * 1024)
                cookies(url)?.takeIf { it.isNotBlank() }?.let { connection.header("Cookie", it) }
                return@withContext connection.get()
            } catch (e: HttpStatusException) {
                // 4xx kalıcıdır (429 hariç). 503 çoğunlukla bot korumasıdır; beklemek yerine tarayıcı yedeğine düşülür.
                if ((e.statusCode in 400..499 && e.statusCode != 429) || e.statusCode == 503) throw e
                lastError = e
            } catch (e: IOException) {
                lastError = e
            }
            delay(1_000L * attempt * attempt)
        }
        throw lastError ?: AppException(AppException.Reason.PAGE_FAILED, url)
    }

    private suspend fun throttle(host: String) {
        val wait = mutex.withLock {
            val now = System.currentTimeMillis()
            val last = lastRequestAt[host] ?: 0L
            val next = maxOf(now, last + requestDelayMs)
            lastRequestAt[host] = next
            next - now
        }
        if (wait > 0) delay(wait)
    }

    companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"
    }
}
