package com.romanarsiv.data.remote

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import kotlin.coroutines.resume

/**
 * Sayfayı ekran dışı bir WebView'de (gerçek Chromium motoru) açar ve oluşan HTML'i döndürür.
 *
 * Jsoup'u 403/503 ile reddeden siteler (Cloudflare "Just a moment…" vb.) çoğunlukla gerçek bir
 * tarayıcıya kendiliğinden izin verir; JavaScript ile içerik yükleyen sayfalar da böylece okunur.
 * Doğrulama [timeoutMs] içinde geçilemezse [ChallengeRequiredException] fırlatılır ve sayfa
 * kullanıcıya gösterilir. Çerezler süreç genelindeki [CookieManager]'da kalır; Jsoup da onları kullanır.
 */
class WebViewPageLoader(
    private val context: Context,
    private val timeoutMs: Long = 25_000,
) : PageLoader {

    private val mutex = Mutex()
    private var webView: WebView? = null

    private class LoadState {
        @Volatile var finished = false
        @Volatile var mainStatus = 200
        @Volatile var error: String? = null
    }

    override suspend fun load(url: String): Document = mutex.withLock {
        withContext(Dispatchers.Main) {
            val view = webView ?: createWebView().also { webView = it }
            val state = LoadState()
            view.webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                    state.finished = false
                    state.mainStatus = 200
                    state.error = null
                }

                override fun onPageFinished(view: WebView, url: String?) {
                    state.finished = true
                }

                override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                    if (request.isForMainFrame) state.mainStatus = response.statusCode
                }

                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    if (request.isForMainFrame) state.error = error.description?.toString() ?: "Bağlantı hatası"
                }
            }
            view.stopLoading()
            view.loadUrl(url)

            val deadline = System.currentTimeMillis() + timeoutMs
            var challenge = false
            while (true) {
                delay(POLL_MS)
                state.error?.let { throw IOException("Sayfa açılamadı: $it") }
                if (state.finished) {
                    challenge = isChallenge(view.eval(PROBE_JS))
                    if (!challenge) {
                        // Sayfa betiklerinin içeriği yerleştirmesi için kısa bir süre.
                        delay(SETTLE_MS)
                        if (state.finished) break
                    }
                }
                if (System.currentTimeMillis() > deadline) {
                    view.stopLoading()
                    if (challenge || state.mainStatus in FallbackPageLoader.BLOCK_CODES) throw ChallengeRequiredException(url)
                    throw IOException("Sayfa zamanında yüklenmedi: ${UrlUtils.hostOf(url) ?: url}")
                }
            }

            val finalUrl = view.url ?: url
            // Tanınmayan bir doğrulama sayfası da olabilir: kullanıcıya gösterilsin.
            if (state.mainStatus in FallbackPageLoader.BLOCK_CODES) throw ChallengeRequiredException(url)
            if (state.mainStatus >= 400) {
                throw HttpStatusException("Tarayıcıda da HTTP ${state.mainStatus}", state.mainStatus, finalUrl)
            }
            val html = view.eval("document.documentElement.outerHTML")
            CookieManager.getInstance().flush()
            Jsoup.parse(html, finalUrl)
        }
    }

    @SuppressLint("SetJavaScriptEnabled") // Bot doğrulamaları ve JS ile yüklenen bölümler için gerekli.
    private fun createWebView(): WebView = WebView(context).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.blockNetworkImage = true
        settings.loadsImagesAutomatically = false
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
        // Ekrana bağlı olmasa da sayfa düzeni için bir boyut ver.
        measure(
            android.view.View.MeasureSpec.makeMeasureSpec(1080, android.view.View.MeasureSpec.EXACTLY),
            android.view.View.MeasureSpec.makeMeasureSpec(1920, android.view.View.MeasureSpec.EXACTLY),
        )
        layout(0, 0, 1080, 1920)
    }

    private suspend fun WebView.eval(js: String): String = suspendCancellableCoroutine { cont ->
        evaluateJavascript(js) { raw -> if (cont.isActive) cont.resume(decodeJsString(raw)) }
    }

    companion object {
        private const val POLL_MS = 700L
        private const val SETTLE_MS = 600L

        /** Başlık + bilinen doğrulama öğelerinin varlığı. */
        private const val PROBE_JS =
            "(function(){var q=document.querySelector('#challenge-form,#challenge-stage,#cf-challenge-running," +
                ".cf-browser-verification,#cf-wrapper .cf-error-type,[name=cf-turnstile-response],#ddg-captcha,.g-recaptcha');" +
                "return (document.title||'')+'|'+(q?'1':'0');})()"

        private val CHALLENGE_TITLES = listOf(
            "just a moment", "bir dakika", "checking your browser", "attention required", "ddos-guard",
            "please wait", "lütfen bekleyin", "security check", "güvenlik kontrolü", "access denied", "one more step",
        )

        internal fun isChallenge(probe: String): Boolean {
            val title = probe.substringBeforeLast('|').lowercase()
            val hasWidget = probe.substringAfterLast('|') == "1"
            return hasWidget || CHALLENGE_TITLES.any { title.contains(it) }
        }

        /** evaluateJavascript sonucu JSON'dur: dizeler tırnaklı ve kaçışlı gelir. */
        internal fun decodeJsString(raw: String?): String {
            if (raw == null || raw == "null" || raw == "undefined") return ""
            return try {
                JSONArray("[$raw]").optString(0, "")
            } catch (_: Exception) {
                raw.trim('"')
            }
        }
    }
}
