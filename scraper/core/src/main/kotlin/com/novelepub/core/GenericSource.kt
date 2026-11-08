package com.novelepub.core

import kotlinx.coroutines.delay
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Settings for a site without a dedicated parser. Every selector is optional:
 * a blank selector falls back to a heuristic.
 */
data class GenericConfig(
    /**
     * Selects chapter links on the start page. Blank = guess: a list of chapter-like links if the page has one,
     * otherwise the start URL is chapter 1 and "next" links are followed.
     */
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
    /** Safety limit for paginated chapter lists. */
    val maxTocPages: Int = 100,
)

/**
 * Reads sites without a dedicated parser.
 * @param pageDelayMs pause between the pages of a paginated chapter list.
 */
class GenericSource(private val config: GenericConfig, private val pageDelayMs: Long = 1_000) : NovelSource {
    override val id = "generic"
    override val name = "Generic"

    override suspend fun fetchNovel(url: String, fetcher: Fetcher): NovelInfo {
        val page = fetcher.get(url)
        val doc = page.document()
        val title = config.novelTitle.ifBlank { guessNovelTitle(doc) }
        val author = config.author.ifBlank { metaContent(doc, "author", "books:author", "og:novel:author") ?: "Unknown" }
        val cover = metaContent(doc, "og:image", "twitter:image")?.let { doc.resolve(it) }
        val description = metaContent(doc, "og:description", "description").orEmpty()

        val chapters = if (config.chapterLinkSelector.isBlank() && looksLikeChapter(doc)) {
            emptyList() // a chapter page: crawl from here, ignoring sidebars of chapter links
        } else {
            collectToc(doc, fetcher)
        }
        if (chapters.isEmpty() && config.chapterLinkSelector.isNotBlank()) {
            throw IllegalStateException("The chapter link selector \"${config.chapterLinkSelector}\" matched no links.")
        }
        return NovelInfo(
            title = title, author = author, url = page.url, description = description,
            coverUrl = cover, language = config.language,
            chapters = chapters,
            crawlFrom = if (chapters.isEmpty()) page.url else null,
        )
    }

    /** Chapter links of this page and of the following pages of a paginated list, oldest chapter first. */
    private suspend fun collectToc(first: Document, fetcher: Fetcher): List<ChapterRef> {
        val all = LinkedHashMap<String, ChapterRef>()
        val visited = hashSetOf(first.location())
        var doc = first
        var pages = 1
        while (true) {
            val before = all.size
            // Later pages may hold only the last one or two chapters.
            chapterLinks(doc, minLinks = if (pages == 1) 3 else 1).forEach { all.putIfAbsent(it.url, it) }
            if (all.size == before) break
            val next = tocNextPage(doc) ?: break
            if (!visited.add(next) || pages >= config.maxTocPages) break
            delay(pageDelayMs)
            doc = fetcher.get(next).document()
            pages++
        }
        return oldestFirst(all.values.toList())
    }

    private fun chapterLinks(doc: Document, minLinks: Int): List<ChapterRef> {
        if (config.chapterLinkSelector.isBlank()) return guessChapterLinks(doc, minLinks)
        return doc.select(config.chapterLinkSelector).mapNotNull { el ->
            val link = if (el.tagName() == "a") el else el.selectFirst("a[href]") ?: return@mapNotNull null
            val href = link.absUrl("href").ifBlank { return@mapNotNull null }
            ChapterRef(title = link.text().trim().ifEmpty { "Chapter" }, url = href.substringBefore('#'))
        }.distinctBy { it.url }
    }

    /** A page with a lot of story text and a "next" link is a chapter, not a table of contents. */
    private fun looksLikeChapter(doc: Document): Boolean =
        paragraphScore(guessContent(doc)) >= 1_500 && findNext(doc) != null

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
        private val CHAPTER_TEXT = Regex("""(?i)(^|[^\p{L}])(chapter|chap|ch|bölüm|bolum|episode|ep|part|kısım)\.?\s*[-#:]?\s*(\d+)""")
        private val LEADING_NUMBER = Regex("""^\s*(\d+)\s*[-.:)]""")
        private val CHAPTER_HREF = Regex("""(?i)(?<![a-z])(chapter|chap|bolum|episode|ch)[-_/]?(\d+)""")
        private val CONTAINERS = setOf("ul", "ol", "tbody", "table", "nav", "section", "dl")
        private val PAGE_NEXT = Regex("""(?i)^(next|next page|›|»|>|>>|sonraki|sonraki sayfa)$""")
        private val PAGINATION = ".pagination, .pager, .page-nav, .nav-links, .wp-pagenavi, [class*=paginat]"

        /**
         * The biggest group of chapter-like links ("Chapter 12", "Bölüm 3", ".../chapter-12") on the page.
         * Links are grouped by their nearest list-like container, so "latest chapter" buttons and
         * previous/next links do not count. Fewer than [minLinks] links = no chapter list.
         */
        fun guessChapterLinks(doc: Document, minLinks: Int = 3): List<ChapterRef> {
            val pageHost = doc.location().toHttpUrlOrNull()?.host
            val links = doc.select("a[href]").filter { a ->
                val href = a.absUrl("href")
                val host = href.toHttpUrlOrNull()?.host ?: return@filter false
                (pageHost == null || host == pageHost) &&
                    (CHAPTER_TEXT.containsMatchIn(a.text()) || LEADING_NUMBER.containsMatchIn(a.text()) || CHAPTER_HREF.containsMatchIn(href))
            }
            val best = links.groupBy { container(it) }.values
                .map { group -> group.map { ChapterRef(it.text().trim().ifEmpty { "Chapter" }, it.absUrl("href").substringBefore('#')) }.distinctBy { it.url } }
                .maxByOrNull { it.size } ?: return emptyList()
            return if (best.size >= minLinks) best else emptyList()
        }

        private fun container(a: Element): Element =
            a.parents().firstOrNull { it.tagName() in CONTAINERS || (it.tagName() == "div" && it.childrenSize() >= 3) }
                ?: a.parent() ?: a

        /** Lists that show the newest chapter first are reversed. */
        fun oldestFirst(chapters: List<ChapterRef>): List<ChapterRef> {
            val numbers = chapters.mapNotNull { chapterNumber(it) }
            return if (numbers.size >= 2 && numbers.first() > numbers.last()) chapters.reversed() else chapters
        }

        private fun chapterNumber(ref: ChapterRef): Int? =
            (CHAPTER_TEXT.find(ref.title)?.groupValues?.get(3)
                ?: LEADING_NUMBER.find(ref.title)?.groupValues?.get(1)
                ?: CHAPTER_HREF.find(ref.url)?.groupValues?.get(2))?.toIntOrNull()

        /** The next page of a paginated chapter list: a "next" link or the page after the current one. */
        fun tocNextPage(doc: Document): String? {
            for (box in doc.select(PAGINATION)) {
                box.select("a[href]").firstOrNull { a ->
                    a.attr("rel").contains("next", ignoreCase = true) ||
                        PAGE_NEXT.matches((a.text().ifBlank { a.attr("title").ifBlank { a.attr("aria-label") } }).trim())
                }?.let { return it.absUrl("href").takeIf { u -> u.startsWith("http") } }
                val current = box.selectFirst(".active, .current, [aria-current]")?.text()?.trim()?.toIntOrNull() ?: continue
                box.select("a[href]").firstOrNull { it.text().trim() == (current + 1).toString() }
                    ?.let { return it.absUrl("href").takeIf { u -> u.startsWith("http") } }
            }
            return doc.selectFirst("head link[rel=next][href]")?.absUrl("href")?.takeIf { it.startsWith("http") }
        }

        private fun paragraphScore(el: Element): Int =
            el.children().filter { it.tagName() == "p" }.sumOf { it.text().length } +
                (if (el.select("> br").size >= 3) el.ownText().length else 0)

        private val NEXT_LABEL = Regex("""^(next( chapter| chap)?|sonraki( bölüm)?|›|»|>|→|>>)(\s|$)|next chapter|sonraki bölüm""")

        /** The element whose direct paragraphs and line-broken text hold the most characters. */
        fun guessContent(doc: Document): Element {
            val body = doc.body()
            var best: Element = body
            var bestScore = 0
            for (el in body.select("article, main, section, div, td")) {
                // Paragraph text, plus text separated by <br> directly inside the element.
                val score = paragraphScore(el)
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
