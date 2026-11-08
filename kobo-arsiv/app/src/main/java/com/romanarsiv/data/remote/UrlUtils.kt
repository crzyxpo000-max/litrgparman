package com.romanarsiv.data.remote

import java.net.URI
import java.net.URLDecoder
import java.util.Base64

internal object UrlUtils {

    fun isHttpUrl(text: String): Boolean {
        val t = text.trim()
        if (!(t.startsWith("http://", ignoreCase = true) || t.startsWith("https://", ignoreCase = true))) return false
        return hostOf(t) != null
    }

    /** Alan adı, küçük harf ve `www.` olmadan; ayrıştırılamazsa null. */
    fun hostOf(url: String): String? = try {
        URI(url.trim()).host?.lowercase()?.removePrefix("www.")
    } catch (_: Exception) {
        null
    }

    fun sameSite(a: String, b: String): Boolean {
        val ha = hostOf(a) ?: return false
        val hb = hostOf(b) ?: return false
        return ha == hb || ha.endsWith(".$hb") || hb.endsWith(".$ha")
    }

    /** Karşılaştırma anahtarı: parça (#...) atılır, alan adı küçültülür, sondaki `/` kaldırılır. */
    fun normalize(url: String): String = try {
        val u = URI(url.trim())
        val host = u.host?.lowercase()?.removePrefix("www.") ?: ""
        val path = (u.rawPath ?: "").trimEnd('/')
        val query = u.rawQuery?.let { "?$it" } ?: ""
        "$host$path$query"
    } catch (_: Exception) {
        url.substringBefore('#').trimEnd('/')
    }

    fun stripFragment(url: String): String = url.substringBefore('#')

    fun queryParam(url: String, name: String): String? = try {
        URI(url).rawQuery
            ?.split('&')
            ?.firstOrNull { it.substringBefore('=') == name }
            ?.substringAfter('=', "")
            ?.let { URLDecoder.decode(it, "UTF-8") }
    } catch (_: Exception) {
        null
    }

    /**
     * Arama motoru yönlendirme bağlantılarını gerçek hedefe çevirir:
     * DuckDuckGo `//duckduckgo.com/l/?uddg=<hedef>`, Bing `bing.com/ck/a?...&u=a1<base64url>`.
     */
    fun unwrapRedirect(href: String): String {
        val url = if (href.startsWith("//")) "https:$href" else href
        val host = hostOf(url) ?: return url
        if (host.endsWith("duckduckgo.com")) {
            queryParam(url, "uddg")?.let { return it }
        }
        if (host.endsWith("bing.com") && url.contains("/ck/a")) {
            val u = queryParam(url, "u")
            if (u != null && u.startsWith("a1")) {
                try {
                    val decoded = String(Base64.getUrlDecoder().decode(padBase64(u.substring(2))), Charsets.UTF_8)
                    if (isHttpUrl(decoded)) return decoded
                } catch (_: IllegalArgumentException) {
                    // geçersiz base64: olduğu gibi bırak
                }
            }
        }
        return url
    }

    private fun padBase64(s: String): String = s + "=".repeat((4 - s.length % 4) % 4)
}
