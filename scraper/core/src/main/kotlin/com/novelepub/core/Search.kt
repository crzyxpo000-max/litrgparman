package com.novelepub.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.Jsoup

data class SearchResult(
    val title: String,
    val url: String,
    val snippet: String,
    /** Which search provider found it. */
    val provider: String,
) {
    val host: String get() = url.toHttpUrlOrNull()?.host?.removePrefix("www.") ?: url

    /** Name of the dedicated parser for this URL, or null when the generic reader will be used. */
    val siteName: String? get() = Sources.forUrl(url)?.name
}

interface SearchProvider {
    val name: String
    suspend fun search(query: String, fetcher: Fetcher): List<SearchResult>
}

/** Royal Road's own title search. Needs no key. */
class RoyalRoadSearch : SearchProvider {
    override val name = "Royal Road"

    override suspend fun search(query: String, fetcher: Fetcher): List<SearchResult> {
        val url = "https://www.royalroad.com/fictions/search".toHttpUrl().newBuilder()
            .addQueryParameter("title", query).build().toString()
        return parse(fetcher.get(url).text(), url)
    }

    fun parse(html: String, baseUrl: String): List<SearchResult> {
        val doc = Jsoup.parse(html, baseUrl)
        val links = doc.select("h2.fiction-title a[href]").ifEmpty { doc.select(".fiction-list-item a[href*=/fiction/]") }
        return links.mapNotNull { a ->
            val href = a.absUrl("href")
            val fiction = RoyalRoadSource.fictionUrl(href) ?: return@mapNotNull null
            val title = a.text().trim().ifEmpty { return@mapNotNull null }
            val item = a.closest(".fiction-list-item")
            val snippet = item?.selectFirst("div[id^=description], .hidden-content")?.text().orEmpty()
            SearchResult(title, fiction, snippet.shorten(), name)
        }.distinctBy { it.url }
    }
}

/** Brave Search API (https://brave.com/search/api/). Needs the user's own API key. */
class BraveSearch(private val apiKey: String) : SearchProvider {
    override val name = "Brave"

    override suspend fun search(query: String, fetcher: Fetcher): List<SearchResult> {
        val url = "https://api.search.brave.com/res/v1/web/search".toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("count", "20")
            .build().toString()
        val body = fetcher.get(url, mapOf("Accept" to "application/json", "X-Subscription-Token" to apiKey)).text()
        return parse(body)
    }

    fun parse(json: String): List<SearchResult> {
        val results = (Json.parseToJsonElement(json).jsonObject["web"] as? JsonObject)?.get("results") as? JsonArray
            ?: return emptyList()
        return results.mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            val url = o.string("url") ?: return@mapNotNull null
            SearchResult(stripTags(o.string("title") ?: url), url, stripTags(o.string("description").orEmpty()).shorten(), name)
        }
    }
}

/**
 * A SearXNG instance (https://docs.searxng.org/) with the JSON format enabled. Many public
 * instances turn JSON off; a self-hosted one works.
 */
class SearxSearch(private val baseUrl: String) : SearchProvider {
    override val name = "SearXNG"

    override suspend fun search(query: String, fetcher: Fetcher): List<SearchResult> {
        val base = baseUrl.trim().trimEnd('/').toHttpUrlOrNull()
            ?: throw IllegalArgumentException("Invalid SearXNG address: $baseUrl")
        val url = base.newBuilder().addPathSegment("search")
            .addQueryParameter("q", query)
            .addQueryParameter("format", "json")
            .build().toString()
        return parse(fetcher.get(url, mapOf("Accept" to "application/json")).text())
    }

    fun parse(json: String): List<SearchResult> {
        val results = Json.parseToJsonElement(json).jsonObject["results"]?.jsonArray ?: return emptyList()
        return results.mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            val url = o.string("url") ?: return@mapNotNull null
            SearchResult(o.string("title") ?: url, url, o.string("content").orEmpty().shorten(), name)
        }
    }
}

class SearchOutcome(
    val results: List<SearchResult>,
    /** Provider name → error message, for providers that failed. */
    val errors: Map<String, String>,
)

/**
 * Asks every provider at once, merges the answers (dedicated-parser sites first, then in provider order),
 * drops duplicates and sites that only talk about books (shops, wikis, social media).
 */
class SearchService(private val providers: List<SearchProvider>) {

    suspend fun search(title: String, fetcher: Fetcher): SearchOutcome = coroutineScope {
        val trimmed = title.trim()
        require(trimmed.isNotEmpty()) { "Empty search" }
        val calls = providers.map { p ->
            p to async {
                // Web search engines need a hint; a site's own search wants the bare title.
                val query = if (p is RoyalRoadSearch || NOVEL_WORD.containsMatchIn(trimmed)) trimmed else "$trimmed novel"
                try {
                    Result.success(p.search(query, fetcher))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
        }
        val errors = LinkedHashMap<String, String>()
        val all = ArrayList<SearchResult>()
        for ((provider, call) in calls) {
            call.await()
                .onSuccess { all += it }
                .onFailure { errors[provider.name] = it.message ?: it.javaClass.simpleName }
        }
        val seen = HashSet<String>()
        val merged = all
            .filter { r -> r.url.toHttpUrlOrNull()?.let { it.scheme == "http" || it.scheme == "https" } == true }
            .filterNot { isNotReadable(it.host) }
            .filter { seen.add(dedupeKey(it.url)) }
            .sortedBy { if (it.siteName != null) 0 else 1 } // stable: keeps provider order otherwise
        SearchOutcome(merged, errors)
    }

    companion object {
        private val NOVEL_WORD = Regex("(?i)\\b(novel|roman|light novel|web ?novel)\\b")

        /** Sites that describe books but do not host chapters. */
        val NOT_READABLE = listOf(
            "youtube.com", "reddit.com", "wikipedia.org", "fandom.com", "amazon.", "goodreads.com", "novelupdates.com",
            "twitter.com", "x.com", "facebook.com", "instagram.com", "tiktok.com", "pinterest.", "quora.com",
            "play.google.com", "apps.apple.com", "audible.", "imdb.com", "myanimelist.net", "anilist.co",
        )

        fun isNotReadable(host: String): Boolean {
            val h = host.lowercase().removePrefix("www.")
            return NOT_READABLE.any { blocked ->
                if (blocked.endsWith(".")) h.startsWith(blocked) || h.contains(".$blocked") else h == blocked || h.endsWith(".$blocked")
            }
        }

        private fun dedupeKey(url: String): String {
            val u = url.toHttpUrlOrNull() ?: return url
            return u.host.removePrefix("www.") + u.encodedPath.trimEnd('/') + (u.encodedQuery?.let { "?$it" } ?: "")
        }
    }
}

private fun JsonObject.string(key: String): String? =
    (this[key] as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }

private fun stripTags(html: String): String = Jsoup.parse(html).text()

private fun String.shorten(max: Int = 240): String =
    replace(Regex("\\s+"), " ").trim().let { if (it.length <= max) it else it.take(max - 1).trimEnd() + "…" }
