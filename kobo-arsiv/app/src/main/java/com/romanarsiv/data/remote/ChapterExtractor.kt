package com.romanarsiv.data.remote

import com.romanarsiv.domain.model.NovelChapter
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.jsoup.safety.Cleaner
import org.jsoup.safety.Safelist

/** Sayfadaki bölüm bağlantısı; [number] metinden ya da adresten okunabildiyse dolu. */
data class ChapterLink(val url: String, val text: String, val number: Int?)

/**
 * Siteye özgü olmayan HTML sezgileri: bölüm listesi, "sonraki" bağlantısı ve ana metin tespiti.
 * Ağa çıkmaz; yalnızca verilen [Document] üzerinde çalışır (birim testleri bu yüzden çevrimdışı).
 */
object ChapterExtractor {

    /** Bir sayfanın bölüm sayılması için ana metnin en az bu kadar karakter olması gerekir. */
    const val MIN_CHAPTER_CHARS = 200

    /** Bu kadar uzun ana metni olan sayfa "dizin" değil "bölüm" sayfasıdır. */
    const val CHAPTER_PAGE_CHARS = 1200

    private val CHAPTER_WORD = "(?:bölüm|bolum|chapter|chap|ch|kısım|kisim|episode|ep|part|parça|parca)"

    private val TEXT_CHAPTER_PATTERNS = listOf(
        Regex("(?iU)(?<![\\p{L}])$CHAPTER_WORD\\.?\\s*[-:#]?\\s*(\\d{1,5})"),
        Regex("(?iU)(\\d{1,5})\\s*\\.?\\s*(?:bölüm|bolum)(?![\\p{L}])"),
    )

    private val HREF_CHAPTER_PATTERN =
        Regex("(?i)(?:chapter|bolum|b%c3%b6l%c3%bcm|bölüm|chap|ch|episode|ep|part|kisim)[-_/]?(\\d{1,5})(?!\\d)")

    private val NEXT_TEXT = Regex(
        "(?iU)^\\s*(?:sonraki(?:\\s+(?:bölüm|bolum|sayfa))?|ileri|devam(?:ı|i)?|next(?:\\s+(?:chapter|page|part))?)\\s*[»›>→]*\\s*$",
    )
    private val NEXT_ARROWS = setOf("»", "›", ">", ">>", "→", "»»")
    private val PREV_TEXT = Regex("(?iU)(önceki|onceki|geri|prev|previous|«|‹|←)")
    private val NAV_TEXT = Regex(
        "(?iU)^\\s*[«‹←]?\\s*(?:önceki|onceki|sonraki|ileri|geri|next|prev|previous|içindekiler|icindekiler|index|table of contents|bölüm listesi)(?:\\s+(?:bölüm|bolum|sayfa|chapter|page))?\\s*[»›→]?\\s*$",
    )

    private val KNOWN_CONTENT_SELECTORS = listOf(
        "#chapter-content", ".chapter-content", "#chr-content", ".chr-c", "#chaptercontent",
        ".reading-content", ".entry-content", ".post-content", ".chapter-body",
        "#content-area", "article .content", "article",
    )

    private val JUNK_SELECTOR =
        "script, style, noscript, iframe, form, nav, header, footer, aside, button, input, select, textarea, svg, ins, figure.ad"

    private val JUNK_TOKEN = Regex(
        "^(?:comments?|comment-\\S*|disqus\\S*|share\\S*|sharing|social\\S*|ads?|ad-\\S*|advert\\S*|sidebar|related\\S*|" +
            "breadcrumbs?|navbar|menu|site-header|site-footer|widget\\S*|cookie\\S*|popup|chapter-nav|nav-links|pagination)$",
        RegexOption.IGNORE_CASE,
    )

    private val BLOCK_TAGS = setOf("p", "h2", "h3", "h4", "blockquote", "hr")

    private val SAFELIST: Safelist = Safelist.none()
        .addTags("p", "br", "em", "strong", "i", "b", "u", "h2", "h3", "h4", "blockquote", "hr")

    // ---------------------------------------------------------------- bölüm listesi

    /**
     * Sayfadaki aynı siteye ait bölüm bağlantıları, okuma sırasına göre (eskiden yeniye).
     * Bölüm sayfalarındaki seçme kutuları (`<select>`) da taranır; sayfanın kendi adresi listede kalabilir.
     */
    fun findChapterLinks(doc: Document): List<ChapterLink> {
        val seen = HashSet<String>()
        val links = ArrayList<ChapterLink>()

        val anchors = doc.select("a[href]").map { it.absUrl("href") to it.text().trim() } +
            doc.select("select option[value]").map { it.absUrl("value") to it.text().trim() }

        for ((rawUrl, text) in anchors) {
            if (rawUrl.isBlank() || !UrlUtils.isHttpUrl(rawUrl)) continue
            if (!UrlUtils.sameSite(rawUrl, doc.location())) continue
            val url = UrlUtils.stripFragment(rawUrl)
            val key = UrlUtils.normalize(url)
            if (NAV_TEXT.matches(text) || NEXT_ARROWS.contains(text)) continue

            val number = chapterNumberFromText(text) ?: chapterNumberFromHref(url)
            if (number == null && !looksLikeChapterText(text)) continue
            if (!seen.add(key)) continue
            links += ChapterLink(url, text, number)
        }
        return orderAscending(links)
    }

    /** Sayfa kendi bölüm listesinde (ör. bölüm seçme kutusunda) yer alıyor mu? */
    fun listContainsPage(links: List<ChapterLink>, pageUrl: String): Boolean {
        val key = UrlUtils.normalize(pageUrl)
        return links.any { UrlUtils.normalize(it.url) == key }
    }

    fun chapterNumberFromText(text: String): Int? {
        if (text.length > 160) return null
        for (p in TEXT_CHAPTER_PATTERNS) {
            p.find(text)?.let { return it.groupValues[1].toIntOrNull() }
        }
        return null
    }

    fun chapterNumberFromHref(url: String): Int? =
        HREF_CHAPTER_PATTERN.find(url.substringAfter("://").substringAfter('/'))?.groupValues?.get(1)?.toIntOrNull()

    private fun looksLikeChapterText(text: String): Boolean =
        text.length in 2..160 && Regex("(?iU)(?<![\\p{L}])(?:bölüm|bolum|chapter|prolog|prologue|epilog|epilogue|önsöz|onsoz)(?![\\p{L}])").containsMatchIn(text)

    /** Siteler listeyi çoğunlukla en yeniden eskiye dizer; numaralar azalıyorsa ters çevir. */
    private fun orderAscending(links: List<ChapterLink>): List<ChapterLink> {
        val numbers = links.mapNotNull { it.number }
        if (numbers.size < 2) return links
        var descending = 0
        var ascending = 0
        for (i in 1 until numbers.size) {
            if (numbers[i] < numbers[i - 1]) descending++ else if (numbers[i] > numbers[i - 1]) ascending++
        }
        return if (descending > ascending) links.reversed() else links
    }

    // ---------------------------------------------------------------- sonraki sayfa

    /** "Sonraki bölüm / sayfa" bağlantısı; yoksa null. Yalnızca aynı siteye ve başka bir sayfaya işaret eder. */
    fun findNextLink(doc: Document): String? {
        val pageKey = UrlUtils.normalize(doc.location())
        fun accept(raw: String): String? {
            if (raw.isBlank() || !UrlUtils.isHttpUrl(raw)) return null
            if (!UrlUtils.sameSite(raw, doc.location())) return null
            val url = UrlUtils.stripFragment(raw)
            return if (UrlUtils.normalize(url) == pageKey) null else url
        }

        doc.select("link[rel=next][href], a[rel~=(?i)^next$][href]").forEach { el ->
            accept(el.absUrl("href"))?.let { return it }
        }
        for (a in doc.select("a[href]")) {
            val text = a.text().trim()
            val attrs = (a.className() + " " + a.id() + " " + a.attr("title") + " " + a.attr("aria-label")).lowercase()
            val byText = NEXT_TEXT.matches(text) || text in NEXT_ARROWS
            val byAttr = Regex("(?:^|[\\s_-])(?:next|sonraki)(?:$|[\\s_-])").containsMatchIn(attrs) &&
                !PREV_TEXT.containsMatchIn(text) && text.length < 40
            if (byText || byAttr) accept(a.absUrl("href"))?.let { return it }
        }
        return null
    }

    // ---------------------------------------------------------------- ana metin

    fun pageTitle(doc: Document): String {
        val og = doc.selectFirst("meta[property=og:title]")?.attr("content")?.trim().orEmpty()
        val title = og.ifEmpty { doc.title().trim() }
        return title.ifEmpty { UrlUtils.hostOf(doc.location()) ?: doc.location() }
    }

    /** Ana metnin yaklaşık uzunluğu (karakter); bölüm/dizin ayrımı için. */
    fun mainTextLength(doc: Document): Int = findContentElement(doc.clone())?.text()?.length ?: 0

    /**
     * Sayfadaki bölümü çıkarır. [titleHint] listedeki bağlantı metnidir.
     * Ana metin [MIN_CHAPTER_CHARS] karakterden kısaysa null döner.
     */
    fun extractChapter(doc: Document, titleHint: String?, fallbackTitle: String): NovelChapter? {
        val work = doc.clone()
        val heading = findHeading(work)
        val content = findContentElement(work) ?: return null

        val title = chooseTitle(heading, titleHint, fallbackTitle)
        val html = cleanContent(content, work.location(), title)
        val textLength = org.jsoup.Jsoup.parseBodyFragment(html).text().length
        if (textLength < MIN_CHAPTER_CHARS) return null
        return NovelChapter(title = title, contentHtml = html, sourceUrl = doc.location())
    }

    private fun chooseTitle(heading: String?, hint: String?, fallback: String): String {
        val h = heading?.takeIf { it.isNotBlank() && it.length <= 200 }
        val l = hint?.trim()?.takeIf { it.isNotBlank() && it.length <= 200 && !NAV_TEXT.matches(it) }
        return when {
            h != null && chapterNumberFromText(h) != null -> h
            l != null -> l
            h != null -> h
            else -> fallback
        }
    }

    private val HEADING_SELECTORS = listOf(".chapter-title", ".chr-title", ".chapter-heading", "#chapter-heading", ".entry-title")

    /** Bölüm başlığı: önce bilinen sınıflar, sonra numara taşıyan ilk h1/h2, en son ilk h1. */
    private fun findHeading(doc: Document): String? {
        for (selector in HEADING_SELECTORS) {
            doc.selectFirst(selector)?.text()?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        }
        doc.select("h1, h2").firstOrNull { chapterNumberFromText(it.text()) != null }?.let { return it.text().trim() }
        return doc.selectFirst("h1")?.text()?.trim()?.takeIf { it.isNotEmpty() }
    }

    /** Bilinen içerik seçicileri; olmazsa paragraf yoğunluğuna göre puanlama (Readability benzeri). */
    internal fun findContentElement(doc: Document): Element? {
        doc.select(JUNK_SELECTOR).remove()
        doc.allElements.filter { it.tagName() != "body" && it.tagName() != "html" && isJunkByClass(it) }.forEach { it.remove() }

        for (selector in KNOWN_CONTENT_SELECTORS) {
            val el = doc.selectFirst(selector) ?: continue
            if (el.text().length >= MIN_CHAPTER_CHARS * 2) return el
        }

        val scores = HashMap<Element, Double>()
        for (block in doc.select("p, div, td, pre")) {
            val own = ownTextLength(block)
            if (own < 25) continue
            val commas = block.ownText().count { it == ',' || it == '.' }
            val score = own + commas * 10.0
            block.parent()?.let { scores.merge(it, score, Double::plus) }
            if (block.tagName() != "p") scores.merge(block, score, Double::plus)
            block.parent()?.parent()?.let { scores.merge(it, score / 2, Double::plus) }
        }
        val best = scores.maxByOrNull { it.value }?.key
        return best ?: doc.body()
    }

    private fun ownTextLength(el: Element): Int =
        el.childNodes().sumOf { n ->
            when (n) {
                is TextNode -> n.text().trim().length
                is Element -> if (n.tagName() in setOf("em", "strong", "i", "b", "u", "span", "a", "font")) n.text().length else 0
                else -> 0
            }
        }

    private fun isJunkByClass(el: Element): Boolean {
        val tokens = el.classNames() + listOfNotNull(el.id().takeIf { it.isNotEmpty() })
        return tokens.any { JUNK_TOKEN.matches(it) }
    }

    private fun isBrSeparated(p: Element): Boolean =
        p.select("> br").size >= 2 && p.text().length > 600

    /** İçeriği güvenli etiketlere indirger ve paragraflara ayırır. */
    internal fun cleanContent(content: Element, baseUri: String, title: String): String {
        val el = content.clone()
        // Bölüm içi gezinti bağlantılarını ("Önceki | Sonraki") at.
        el.select("a").filter { NAV_TEXT.matches(it.text()) || it.text().trim() in NEXT_ARROWS }.forEach { it.remove() }
        // Yalnızca satır içi çocuğu olan (kök dışındaki) div'ler paragraftır.
        el.select("div").forEach { div ->
            if (div !== el && div.children().none { it.isBlock && it.tagName() != "br" }) div.tagName("p")
        }

        val shell = Document.createShell(baseUri)
        shell.body().appendChild(el)
        val cleaned = Cleaner(SAFELIST).clean(shell).body()

        val out = Element("div")
        var current: Element? = null
        fun flush() {
            val p = current ?: return
            if (p.text().isNotBlank()) out.appendChild(p)
            current = null
        }
        fun flow(nodes: List<org.jsoup.nodes.Node>) {
            for (node in nodes) {
                when {
                    // Bütün bölümü tek <p> içine <br> ile yazan siteler: paragrafları <br>'den böl.
                    node is Element && node.tagName() == "p" && isBrSeparated(node) -> {
                        flush()
                        flow(node.childNodes().toList())
                        flush()
                    }
                    node is Element && node.tagName() in BLOCK_TAGS -> {
                        flush()
                        if (node.tagName() == "hr" || node.text().isNotBlank()) out.appendChild(node.clone())
                    }
                    node is Element && node.tagName() == "br" -> flush()
                    node is TextNode && node.isBlank -> current?.appendChild(TextNode(" "))
                    else -> {
                        val p = current ?: Element("p").also { current = it }
                        p.appendChild(node.clone())
                    }
                }
            }
        }
        flow(cleaned.childNodes().toList())
        flush()

        // Başlık içerikte tekrar ediyorsa ilk bloğu düşür (üretici başlığı kendisi ekler).
        out.children().firstOrNull()?.let { first ->
            if (first.text().trim().equals(title.trim(), ignoreCase = true)) first.remove()
        }
        // Baştaki ve sondaki boş/çizgi blokları temizle.
        while (out.children().firstOrNull()?.tagName() == "hr") out.child(0).remove()
        while (out.children().lastOrNull()?.tagName() == "hr") out.children().lastOrNull()?.remove()
        return out.html()
    }
}
