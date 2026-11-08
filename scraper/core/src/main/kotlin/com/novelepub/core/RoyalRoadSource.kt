package com.novelepub.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.nodes.Document

/**
 * royalroad.com. Accepts a fiction URL or any chapter URL of the fiction.
 *
 * Royal Road inserts hidden "this story was stolen" paragraphs into chapters, hidden by
 * classes declared in a `<style>` block in the page head; those elements are removed.
 */
class RoyalRoadSource(private val includeAuthorNotes: Boolean = false) : SiteSource {
    override val id = "royalroad"
    override val name = "Royal Road"
    override val hosts = listOf("royalroad.com")

    override suspend fun fetchNovel(url: String, fetcher: Fetcher): NovelInfo {
        val fictionUrl = fictionUrl(url) ?: throw IllegalArgumentException("Not a Royal Road fiction URL: $url")
        val page = fetcher.get(fictionUrl)
        return parseNovel(page.document())
    }

    fun parseNovel(doc: Document): NovelInfo {
        val title = doc.selectFirst("div.fic-title h1, .fic-header h1")?.text()
            ?: doc.metaContent("og:title")
            ?: throw IllegalStateException("No fiction title found; the page layout may have changed.")
        val author = doc.selectFirst("div.fic-title h4 a, .fic-header h4 a, .fic-title h4 span a")?.text()
            ?: doc.metaContent("books:author")
            ?: "Unknown"
        val cover = doc.selectFirst("div.fic-header img.thumbnail, img.thumbnail")?.absUrl("src")?.takeIf { it.isNotBlank() }
            ?: doc.metaContent("og:image")
        val description = doc.selectFirst("div.description .hidden-content, div.description")?.wholeText()?.trim().orEmpty()
        val chapters = chaptersFromTable(doc).ifEmpty { chaptersFromScript(doc) }
        if (chapters.isEmpty()) throw IllegalStateException("No chapters found on $title.")
        return NovelInfo(
            title = title,
            author = author,
            url = doc.location(),
            description = description,
            coverUrl = cover?.takeUnless { it.contains("nocover", ignoreCase = true) },
            language = "en",
            chapters = chapters,
        )
    }

    private fun chaptersFromTable(doc: Document): List<ChapterRef> =
        doc.select("table#chapters tr.chapter-row, table#chapters tbody tr").mapNotNull { row ->
            val link = row.selectFirst("td a[href]")
            val href = row.attr("data-url").ifBlank { link?.attr("href").orEmpty() }
            if (href.isBlank()) return@mapNotNull null
            ChapterRef(
                title = link?.text()?.trim().orEmpty().ifEmpty { "Chapter" },
                url = resolve(doc, href),
            )
        }.distinctBy { it.url }

    /** Newer pages also embed the chapter list as `window.chapters = [...]`. */
    private fun chaptersFromScript(doc: Document): List<ChapterRef> {
        val script = doc.select("script").map { it.data() }.firstOrNull { "window.chapters" in it } ?: return emptyList()
        val json = Regex("window\\.chapters\\s*=\\s*(\\[.*?\\])\\s*;", RegexOption.DOT_MATCHES_ALL)
            .find(script)?.groupValues?.get(1) ?: return emptyList()
        val array = runCatching { Json.parseToJsonElement(json) as JsonArray }.getOrNull() ?: return emptyList()
        return array.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val href = obj["url"]?.jsonPrimitive?.content ?: return@mapNotNull null
            ChapterRef(title = obj["title"]?.jsonPrimitive?.content ?: "Chapter", url = resolve(doc, href))
        }
    }

    override suspend fun fetchChapter(ref: ChapterRef, fetcher: Fetcher): Chapter =
        parseChapter(fetcher.get(ref.url).document(), ref)

    fun parseChapter(doc: Document, ref: ChapterRef): Chapter {
        val content = doc.selectFirst("div.chapter-inner.chapter-content, div.chapter-content")
            ?: throw IllegalStateException("No chapter text found at ${ref.url}.")
        val hidden = hiddenClasses(doc)
        if (hidden.isNotEmpty()) {
            content.select(hidden.joinToString(", ") { ".${cssEscape(it)}" }).remove()
        }
        val title = doc.selectFirst("div.fic-header h1, h1.font-white")?.text()?.takeIf { it.isNotBlank() } ?: ref.title

        val html = buildString {
            if (includeAuthorNotes) appendNotes(doc, before = true, hidden)
            append(ContentCleaner.clean(content))
            if (includeAuthorNotes) appendNotes(doc, before = false, hidden)
        }
        return Chapter(title = title, url = ref.url, html = html)
    }

    private fun StringBuilder.appendNotes(doc: Document, before: Boolean, hidden: Set<String>) {
        val chapter = doc.selectFirst("div.chapter-inner.chapter-content, div.chapter-content") ?: return
        val all = doc.allElements
        val chapterIndex = all.indexOf(chapter)
        // Notes before the chapter text come earlier in document order.
        val notes = doc.select("div.author-note-portlet").filter { (all.indexOf(it) < chapterIndex) == before }
        for (note in notes) {
            val body = note.selectFirst(".author-note") ?: note
            if (hidden.isNotEmpty()) body.select(hidden.joinToString(", ") { ".${cssEscape(it)}" }).remove()
            append("<blockquote class=\"author-note\"><p><strong>Author's note</strong></p>")
            append(ContentCleaner.clean(body))
            append("</blockquote>")
        }
    }

    /** Classes declared `display: none` in the page's inline styles. */
    fun hiddenClasses(doc: Document): Set<String> {
        val css = doc.select("style").joinToString("\n") { it.data() }
        return HIDDEN_RULE.findAll(css)
            .filter { DISPLAY_NONE.containsMatchIn(it.groupValues[2]) }
            .flatMap { rule -> CLASS_NAME.findAll(rule.groupValues[1]).map { it.groupValues[1] } }
            .toSet()
    }

    companion object {
        private val FICTION = Regex("/fiction/(\\d+)(/[^/?#]+)?")
        private val HIDDEN_RULE = Regex("([^{}]+)\\{([^{}]*)\\}")
        private val DISPLAY_NONE = Regex("display\\s*:\\s*none", RegexOption.IGNORE_CASE)
        private val CLASS_NAME = Regex("\\.(-?[_a-zA-Z][_a-zA-Z0-9-]*)")

        /** https://www.royalroad.com/fiction/123/slug for any fiction or chapter URL, or null. */
        fun fictionUrl(url: String): String? {
            val match = FICTION.find(url) ?: return null
            val slug = match.groupValues[2]
            return "https://www.royalroad.com/fiction/${match.groupValues[1]}$slug"
        }

        private fun resolve(doc: Document, href: String): String =
            (doc.location().toHttpUrlOrNull() ?: "https://www.royalroad.com/".toHttpUrl()).resolve(href)?.toString() ?: href

        private fun cssEscape(name: String) = name.replace(Regex("([^_a-zA-Z0-9-])"), "\\\\$1")
    }
}

private fun Document.metaContent(property: String): String? =
    selectFirst("meta[property=$property], meta[name=$property]")?.attr("content")?.trim()?.takeIf { it.isNotEmpty() }
