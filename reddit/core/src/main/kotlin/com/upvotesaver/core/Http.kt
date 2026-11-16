package com.upvotesaver.core

import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** The server answered with an error status that retrying did not fix. */
class HttpException(val url: String, val status: Int) : IOException("HTTP $status for $url")

/**
 * Small OkHttp wrapper: sets the User-Agent Reddit asks for, retries network errors, 429 and 5xx
 * with exponential backoff (honoring Retry-After), and cancels the call with the coroutine.
 * Response bodies are read on the calling thread, so call it from an IO dispatcher.
 */
class Http(
    private val userAgent: () -> String,
    private val maxRetries: Int = 3,
    private val baseBackoffMs: Long = 2_000,
    client: OkHttpClient? = null,
) {
    private val client = client ?: OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun <T> call(url: String, build: Request.Builder.() -> Unit = {}, read: suspend (Response) -> T): T {
        var attempt = 0
        while (true) {
            val request = Request.Builder().url(url).header("User-Agent", userAgent()).apply(build).build()
            var waitMs: Long? = null
            val failure: IOException = try {
                client.newCall(request).await().use { response ->
                    if (response.isSuccessful) return read(response)
                    waitMs = response.header("Retry-After")?.trim()?.toLongOrNull()?.coerceIn(0, 120)?.times(1000)
                    HttpException(url, response.code)
                }
            } catch (e: IOException) {
                e
            }
            val retryable = failure !is HttpException || failure.status == 429 || failure.status >= 500
            if (!retryable || attempt >= maxRetries) throw failure
            delay(waitMs ?: (baseBackoffMs shl attempt))
            attempt++
        }
    }

    suspend fun text(url: String, build: Request.Builder.() -> Unit = {}): String =
        call(url, build) { it.body?.string().orEmpty() }

    /** Streams [url] into [dest]. A failed or cancelled download leaves no partial file behind. */
    suspend fun download(url: String, dest: File) {
        try {
            call(url) { response ->
                val body = response.body ?: throw IOException("Empty body for $url")
                dest.outputStream().use { out ->
                    val input = body.byteStream()
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        coroutineContext.ensureActive()
                        val n = input.read(buffer)
                        if (n < 0) break
                        out.write(buffer, 0, n)
                    }
                }
            }
        } catch (e: Throwable) {
            dest.delete()
            throw e
        }
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
