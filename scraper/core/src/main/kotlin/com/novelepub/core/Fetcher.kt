package com.novelepub.core

import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FetchResult(
    /** URL after redirects. */
    val url: String,
    val body: ByteArray,
    val contentType: String?,
) {
    /** Parses the body as HTML. Jsoup picks the charset from the header or the page's meta tag. */
    fun text(): String {
        val charset = contentType?.let { CHARSET.find(it)?.groupValues?.get(1) }
            ?.let { runCatching { charset(it) }.getOrNull() } ?: Charsets.UTF_8
        return String(body, charset)
    }

    fun document(): Document {
        val charset = contentType?.let { CHARSET.find(it)?.groupValues?.get(1) }
            ?.takeIf { runCatching { charset(it) }.isSuccess }
        return Jsoup.parse(ByteArrayInputStream(body), charset, url)
    }

    private companion object {
        val CHARSET = Regex("charset=\"?([\\w.:-]+)", RegexOption.IGNORE_CASE)
    }
}

/** The site answered with an error status that retrying did not fix. */
class HttpStatusException(val url: String, val status: Int) : IOException(
    when (status) {
        401 -> "HTTP 401 for $url. The API key is missing or wrong."
        403, 503 -> "HTTP $status for $url. The site refused the request (often a Cloudflare check)."
        404 -> "HTTP 404 for $url. The page does not exist."
        429 -> "HTTP 429 for $url. Too many requests; increase the delay between requests."
        else -> "HTTP $status for $url."
    },
)

interface Fetcher {
    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): FetchResult
}

/**
 * OkHttp-based [Fetcher]. Retries network errors, 429 and 5xx with exponential backoff
 * (honoring Retry-After), and cancels the HTTP call when the coroutine is cancelled.
 */
class OkHttpFetcher(
    private val userAgent: String = DEFAULT_USER_AGENT,
    private val maxRetries: Int = 3,
    private val baseBackoffMs: Long = 2_000,
    client: OkHttpClient? = null,
) : Fetcher {
    private val client = client ?: OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    override suspend fun get(url: String, headers: Map<String, String>): FetchResult {
        var attempt = 0
        while (true) {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/*;q=0.8,*/*;q=0.7")
                .apply { headers.forEach { (k, v) -> header(k, v) } }
                .build()
            val outcome = try {
                client.newCall(request).await().use { response ->
                    when {
                        response.isSuccessful -> return FetchResult(
                            url = response.request.url.toString(),
                            body = response.body?.bytes() ?: ByteArray(0),
                            contentType = response.header("Content-Type"),
                        )
                        response.code == 429 || response.code >= 500 ->
                            Retry(HttpStatusException(url, response.code), retryAfterMs(response))
                        else -> throw HttpStatusException(url, response.code)
                    }
                }
            } catch (e: HttpStatusException) {
                throw e
            } catch (e: IOException) {
                Retry(e, null)
            }
            if (attempt >= maxRetries) throw outcome.error
            delay(outcome.waitMs ?: (baseBackoffMs shl attempt))
            attempt++
        }
    }

    private class Retry(val error: IOException, val waitMs: Long?)

    private fun retryAfterMs(response: Response): Long? =
        response.header("Retry-After")?.trim()?.toLongOrNull()?.coerceIn(0, 120)?.times(1000)

    companion object {
        const val DEFAULT_USER_AGENT = "NovelEpub/0.1 (Android; personal offline reader)"
    }
}

private suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) = cont.resume(response)
        override fun onFailure(call: Call, e: IOException) {
            if (!cont.isCancelled) cont.resumeWithException(e)
        }
    })
    cont.invokeOnCancellation { runCatching { cancel() } }
}
