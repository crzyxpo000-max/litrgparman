package com.romanarsiv.data.remote

import com.romanarsiv.domain.model.NovelSource
import org.jsoup.nodes.Document
import java.net.URLEncoder

/** Bir arama motorunun HTML sonuç sayfasını [NovelSource] listesine çevirir. */
interface SearchProvider {
    val name: String
    fun searchUrl(query: String): String
    fun parse(doc: Document): List<NovelSource>
}

private fun encode(q: String) = URLEncoder.encode(q, "UTF-8")

private fun sourceOf(href: String, title: String, snippet: String): NovelSource? {
    val url = UrlUtils.unwrapRedirect(href)
    if (!UrlUtils.isHttpUrl(url)) return null
    val host = UrlUtils.hostOf(url) ?: return null
    if (title.isBlank()) return null
    return NovelSource(url = url, siteName = host, title = title.trim(), snippet = snippet.trim())
}

/** JavaScript'siz DuckDuckGo arayüzü (html.duckduckgo.com). */
class DuckDuckGoSearchProvider : SearchProvider {
    override val name = "DuckDuckGo"

    override fun searchUrl(query: String) = "https://html.duckduckgo.com/html/?q=${encode(query)}&kl=tr-tr"

    override fun parse(doc: Document): List<NovelSource> =
        doc.select("div.result, div.web-result")
            .filterNot { it.hasClass("result--ad") }
            .mapNotNull { r ->
                val a = r.selectFirst("a.result__a") ?: return@mapNotNull null
                sourceOf(a.attr("href"), a.text(), r.selectFirst(".result__snippet")?.text().orEmpty())
            }
}

/** Bing'in klasik HTML sonuçları; DuckDuckGo boş dönerse ya da engellerse yedek. */
class BingSearchProvider : SearchProvider {
    override val name = "Bing"

    override fun searchUrl(query: String) = "https://www.bing.com/search?q=${encode(query)}&setlang=tr&cc=TR"

    override fun parse(doc: Document): List<NovelSource> =
        doc.select("li.b_algo").mapNotNull { r ->
            // Eski biçim <h2><a href>başlık</a></h2>, yeni biçim <a href><h2>başlık</h2></a>.
            val a = r.selectFirst("h2 a[href]") ?: r.selectFirst("a[href]:has(h2)") ?: return@mapNotNull null
            val title = a.selectFirst("h2")?.text() ?: a.text()
            sourceOf(a.absUrl("href").ifEmpty { a.attr("href") }, title, r.selectFirst(".b_caption p, p")?.text().orEmpty())
        }
}
