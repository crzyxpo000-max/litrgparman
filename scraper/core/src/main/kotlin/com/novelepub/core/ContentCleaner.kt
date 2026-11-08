package com.novelepub.core

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Entities
import org.jsoup.nodes.TextNode
import org.jsoup.safety.Safelist

/**
 * Turns a scraped chapter element into a well-formed XHTML body fragment for the EPUB:
 * scripts, ads and forms removed, links unwrapped, images replaced by their alt text,
 * only basic formatting and tables kept.
 */
object ContentCleaner {

    private val safelist: Safelist = Safelist()
        .addTags(
            "p", "br", "hr", "div", "span", "em", "strong", "i", "b", "u", "s", "del", "ins", "sub", "sup", "small",
            "blockquote", "pre", "code", "h1", "h2", "h3", "h4", "h5", "h6", "ul", "ol", "li",
            "table", "thead", "tbody", "tfoot", "tr", "th", "td", "caption",
        )
        .addAttributes("td", "colspan", "rowspan")
        .addAttributes("th", "colspan", "rowspan")
        .addAttributes("ol", "start")

    private val JUNK = listOf(
        "script", "style", "noscript", "iframe", "form", "button", "input", "select", "textarea", "svg",
        "ins.adsbygoogle", "[hidden]",
    ).joinToString(", ")

    // Control characters XML 1.0 does not allow.
    private val INVALID_XML = Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F\\uFFFE\\uFFFF]")

    /**
     * @param removeSelector extra CSS selector for site junk (navigation, "support us" banners...).
     */
    fun clean(content: Element, removeSelector: String = ""): String {
        val copy = content.clone()
        copy.select(JUNK).remove()
        copy.select("[style]")
            .filter { it.attr("style").replace(" ", "").contains("display:none", ignoreCase = true) }
            .forEach { it.remove() }
        if (removeSelector.isNotBlank()) copy.select(removeSelector).remove()
        copy.select("img").forEach { img ->
            val alt = img.attr("alt").trim()
            if (alt.isEmpty()) img.remove() else img.replaceWith(TextNode("[$alt]"))
        }
        copy.select("a").forEach { it.unwrap() }

        val output = Document.OutputSettings()
            .syntax(Document.OutputSettings.Syntax.xml)
            .escapeMode(Entities.EscapeMode.xhtml)
            .charset(Charsets.UTF_8)
            .prettyPrint(false)
        val cleaned = Jsoup.clean(copy.html(), content.baseUri(), safelist, output)
        return INVALID_XML.replace(cleaned, "").trim()
    }

    /** Escapes text for use inside XML text nodes and attribute values. */
    fun escapeXml(text: String): String = buildString(text.length) {
        for (c in INVALID_XML.replace(text, "")) when (c) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            '\'' -> append("&apos;")
            else -> append(c)
        }
    }
}
