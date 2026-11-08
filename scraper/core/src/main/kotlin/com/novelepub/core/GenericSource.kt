package com.novelepub.core

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Settings for a site without a dedicated parser. Every selector is optional:
 * a blank selector falls back to a heuristic.
 */
data class GenericConfig(
    /** Selects chapter links on the start page. Blank = crawl: the start URL is chapter 1, follow "next" links. */
    val chapterLinkSelector: String = "",
    /** Chapter title inside a chapter page. Blank = first h1/h2, then the page title. */
    val titleSelector: String = "",
    /** The element holding the chapter text. Blank = the element with the most paragraph text. */
    val contentSelector: String = "",
    /** The "next chapter" link (crawl mode). Blank = rel=next or a link whose text says "next". */
    val nextSelector: String = "",
    /** Junk to drop from chapter text (ads, navigation, notices). */
    val removeSelector: String = "",
    val novelTitle: String = "",
    val author: String = "",
    val language: String = "en",
    /** Safety limit for crawl mode. */
    val maxChapters: Int = 3000,
)

class GenericSource(private val config: GenericConfig) : NovelSource {
    override val id = "generic"
    override val name = "Generic"

    override suspend fun fetchNovel(url: String, fetcher: Fetcher): NovelInfo {
        val page = fetcher.get(url)
        val doc = page.document()
        val title = config.novelTitle.ifBlank { guessNovelTitle(doc) }
        val author = config.author.ifBlank { metaContent(doc, "author", "books:author", "og:novel:author") ?: "Unknown" }
        val cover = metaContent(doc, "og:image", "twitter:image")?.let { doc.resolve(it) }
        val description = metaContent(doc, "og:description", "description").orEmpty()

        if (config.chapterLinkSelector.isBlank()) {
            return NovelInfo(
                title = title, author = author, url = page.url, description = description,
                coverUrl = cover, language = config.language, crawlFrom = page.url,
            )
        }
        val chapters = doc.select(config.chapterLinkSelector).mapNotNull { el ->
            val link = if (el.tagName() == "a") el else el.selectFirst("a[href]") ?: return@mapNotNull null
            val href = link.absUrl("href").ifBlank { return@mapNotNull null }
            ChapterRef(title = link.text().trim().ifEmpty { "Chapter" }, url = href.substringBefore('#'))
        }.distinctBy { it.url }
        if (chapters.isEmpty()) {
            throw IllegalStateException("The chapter link selector \"${config.chapterLinkSelector}\" matched no links.")
        }
        return NovelInfo(
            title = title, author = author, url = page.url, description = description,
            coverUrl = cover, language = config.language, chapters = chapters,
        )
    }

    override suspend fun fetchChapter(ref: ChapterRef, fetcher: Fetcher): Chapter {
        val page = fetcher.get(ref.url)
        return parseChapter(page.document(), ref.copy(url = page.url))
    }

    fun parseChapter(doc: Document, ref: ChapterRef): Chapter {
        val content = if (config.contentSelector.isNotBlank()) {
            doc.selectFirst(config.contentSelector)
                ?: throw IllegalStateException("The content selector \"${config.contentSelector}\" matched nothing at ${ref.url}.")
        } else {
            guessContent(doc)
        }
        val title = when {
            config.titleSelector.isNotBlank() -> doc.selectFirst(config.titleSelector)?.text()
            else -> (content.selectFirst("h1, h2, h3") ?: doc.selectFirst("h1, h2"))?.text()
        }?.trim()?.takeIf { it.isNotEmpty() } ?: ref.title.takeIf { it.isNotBlank() && it != "Chapter" } ?: doc.title()

        val next = findNext(doc)?.takeIf { it != ref.url && it.substringBefore('#') != ref.url.substringBefore('#') }
        return Chapter(
            title = title,
            url = ref.url,
            html = ContentCleaner.clean(content, config.removeSelector),
            nextUrl = next,
        )
    }

    fun findNext(doc: Document): String? {
        if (config.nextSelector.isNotBlank()) {
            return doc.selectFirst(config.nextSelector)?.let { linkOf(it) }
        }
        doc.selectFirst("a[rel~=(?i)next][href], link[rel~=(?i)next][href]")?.let { linkOf(it) }?.let { return it }
        return doc.select("a[href]").firstOrNull { a ->
            val label = (a.text() + " " + a.attr("title") + " " + a.attr("aria-label")).trim().lowercase()
            NEXT_LABEL.containsMatchIn(label) && !a.attr("href").startsWith("javascript", ignoreCase = true)
        }?.let { linkOf(it) }
    }

    private fun linkOf(el: Element): String? {
        val link = if (el.hasAttr("href")) el else el.selectFirst("a[href]") ?: return null
        return link.absUrl("href").takeIf { it.startsWith("http") }?.substringBefore('#')
    }

    companion object {
        private val NEXT_LABEL = Regex("""^(next( chapter| chap)?|sonraki( bölüm)?|›|»|>|→|>>)(\s|$)|next chapter|sonraki bölüm""")

        /** The element whose direct paragraphs and line-broken text hold the most characters. */
        fun guessContent(doc: Document): Element {
            val body = doc.body()
            var best: Element = body
            var bestScore = 0
            for (el in body.select("article, main, section, div, td")) {
                var score = 0
                for (child in el.children()) {
                    if (child.tagName() == "p") score += child.text().length
                }
                // Text separated by <br> directly inside the element.
                if (el.select("> br").size >= 3) score += el.ownText().length
                if (score > bestScore) {
                    bestScore = score
                    best = el
                }
            }
            return best
        }

        fun guessNovelTitle(doc: Document): String =
            metaContent(doc, "og:title")
                ?: doc.selectFirst("h1")?.text()?.takeIf { it.isNotBlank() }
                ?: doc.title().ifBlank { "Untitled" }

        private fun metaContent(doc: Document, vararg names: String): String? = names.firstNotNullOfOrNull { n ->
            doc.selectFirst("meta[property=$n], meta[name=$n]")?.attr("content")?.trim()?.takeIf { it.isNotEmpty() }
        }

        private fun Document.resolve(href: String): String =
            createElement("a").attr("href", href).also { it.setBaseUri(location()) }.absUrl("href").ifBlank { href }
    }
}
