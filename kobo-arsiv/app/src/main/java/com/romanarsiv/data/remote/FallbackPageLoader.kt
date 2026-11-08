package com.romanarsiv.data.remote

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jsoup.HttpStatusException
import org.jsoup.nodes.Document
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/** Tarayıcı yükleyicisi süre içinde doğrulama sayfasını geçemedi; kullanıcının görmesi gerekiyor. */
class ChallengeRequiredException(val url: String) :
    IOException("Site tarayıcı doğrulaması istiyor: ${UrlUtils.hostOf(url) ?: url}")

/**
 * Kullanıcı doğrulamayı geçmedi (Vazgeç). IOException değildir: depo bunu "atlanan bölüm" sayıp
 * her bölümde yeniden sormasın, indirme bütünüyle dursun.
 */
class ChallengeDeclinedException(val host: String) :
    IllegalStateException("$host doğrulaması tamamlanmadı; site tarayıcı dışından erişime izin vermiyor.")

/**
 * Önce hızlı Jsoup; site 403/503 ile reddederse aynı sayfayı [browser] (WebView) ile açar.
 *
 * Site başına durum: ilk engelde tarayıcıyla açılır ve çerezleri alınır, sonraki istekler yine
 * Jsoup ile (bu kez çerezlerle) denenir; ikinci kez engellenirse o site için hep tarayıcı kullanılır.
 * Tarayıcı da doğrulamayı kendiliğinden geçemezse [solveChallenge] kullanıcıya görünür bir sayfa açar.
 */
class FallbackPageLoader(
    private val http: PageLoader,
    private val browser: PageLoader?,
    private val solveChallenge: (suspend (url: String) -> Boolean)? = null,
) : PageLoader {

    private enum class Mode { COOKIES, BROWSER }

    private val modes = ConcurrentHashMap<String, Mode>()
    private val declined = ConcurrentHashMap.newKeySet<String>()

    override suspend fun load(url: String): Document {
        val host = UrlUtils.hostOf(url) ?: url
        if (browser == null || modes[host] != Mode.BROWSER) {
            try {
                return http.load(url)
            } catch (e: HttpStatusException) {
                if (browser == null || e.statusCode !in BLOCK_CODES) throw e
                modes[host] = if (modes[host] == Mode.COOKIES) Mode.BROWSER else Mode.COOKIES
            }
        }
        return viaBrowser(browser, url, host)
    }

    private suspend fun viaBrowser(browser: PageLoader, url: String, host: String): Document {
        if (host in declined) throw ChallengeDeclinedException(host)
        return try {
            browser.load(url)
        } catch (e: ChallengeRequiredException) {
            val solver = solveChallenge ?: throw e
            if (!solver(url)) {
                declined += host
                throw ChallengeDeclinedException(host)
            }
            browser.load(url)
        }
    }

    companion object {
        /** Bot korumalarının (Cloudflare, DDoS-Guard, Sucuri …) döndürdüğü kodlar. */
        val BLOCK_CODES = setOf(403, 503)
    }
}

/**
 * Veri katmanı ile arayüz arasındaki köprü: [requestSolve] doğrulama sayfasını arayüze bildirir ve
 * kullanıcı "Devam" ya da "Vazgeç" diyene kadar askıda kalır. Aynı anda tek doğrulama sorulur.
 */
class ChallengeBroker {
    private val _pendingUrl = MutableStateFlow<String?>(null)
    val pendingUrl: StateFlow<String?> = _pendingUrl.asStateFlow()

    private val mutex = Mutex()
    private var deferred: CompletableDeferred<Boolean>? = null

    suspend fun requestSolve(url: String): Boolean = mutex.withLock {
        val d = CompletableDeferred<Boolean>()
        deferred = d
        _pendingUrl.value = url
        try {
            d.await()
        } finally {
            _pendingUrl.value = null
            deferred = null
        }
    }

    fun complete(solved: Boolean) {
        deferred?.complete(solved)
    }
}
