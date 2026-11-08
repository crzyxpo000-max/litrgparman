package com.romanarsiv.domain

import com.romanarsiv.domain.model.NovelSource
import java.net.URI

/**
 * Arama sonucunun roman (okuma) sitesi olup olmadığını puanlar. Siteye özgü değildir; sezgiseldir:
 *
 * - bilinen roman/fanfik siteleri ve alan adında "novel", "wuxia", "fanfic" … geçenler güçlü artı;
 * - adres yolunda ve başlıkta "bölüm", "chapter", "novel", "oku" … artı;
 * - ansiklopedi, kitap mağazası, sözlük, TV/video siteleri ve başlıkta "izle" geçenler eksi
 *   (Türkçede dizi bölümleri de "bölüm" diye geçer).
 *
 * [MIN_SCORE] ve üstü roman sitesi sayılır.
 */
object NovelSiteFilter {
    const val MIN_SCORE = 2

    private val KNOWN_NOVEL_HOSTS = listOf(
        "wattpad.com", "webnovel.com", "royalroad.com", "scribblehub.com", "novelupdates.com",
        "wuxiaworld.com", "wuxiaworld.site", "archiveofourown.org", "fanfiction.net", "fictionpress.com",
        "tapas.io", "inkitt.com", "novelbin.com", "novelfull.com", "lightnovelpub.com", "lightnovelworld.com",
        "ranobes.net", "boxnovel.com", "readlightnovel.me", "novelhall.com", "freewebnovel.com", "mtlnovel.com",
        "novelcool.com", "booknet.com", "litnet.com", "goodnovel.com", "dreame.com", "syosetu.com", "kakuyomu.jp",
    )

    private val NOVEL_HOST_WORDS = listOf("novel", "wuxia", "ranobe", "fanfic", "fiction", "hikaye", "roman-oku", "romanoku")

    /** Okunabilir roman metni barındırmayan siteler: ansiklopedi, mağaza, sözlük, arama, TV/video. */
    private val BLOCKED_HOSTS = listOf(
        "wikipedia.org", "wikiwand.com", "wikidata.org", "wiktionary.org", "britannica.com",
        "amazon.com", "amazon.com.tr", "amazon.de", "amazon.co.uk", "trendyol.com", "hepsiburada.com", "n11.com",
        "kitapyurdu.com", "dr.com.tr", "idefix.com", "bkmkitap.com", "pandora.com.tr", "kitapsec.com", "ebay.com",
        "aliexpress.com", "etsy.com", "akakce.com", "cimri.com", "epey.com", "sahibinden.com",
        "goodreads.com", "1000kitap.com", "imdb.com", "storytel.com", "audible.com", "google.com", "books.google.com",
        "eksisozluk.com", "reddit.com", "quora.com", "medium.com",
        "showtv.com.tr", "atv.com.tr", "kanald.com.tr", "startv.com.tr", "nowtv.com.tr", "trt.net.tr", "tabii.com",
        "puhutv.com", "exxen.com", "blutv.com", "netflix.com", "disneyplus.com", "primevideo.com", "dizibox.com",
    )

    private val TEXT_SIGNAL = Regex(
        "(?<!\\p{L})(?:novel|web ?novel|light ?novel|bölüm|bolum|chapter|fanfic|fanfik|hikaye|wattpad|" +
            "online oku|oku|okuma|read online|read|cilt \\d|prolog)(?!\\p{L})",
        RegexOption.IGNORE_CASE,
    )
    private val NEGATIVE_TEXT = Regex(
        "(?<!\\p{L})(?:izle|izleyin|fragman|dizi|dizisi|film|watch|episode|sezon|season|satın al|fiyat|indirim|kargo)(?!\\p{L})",
        RegexOption.IGNORE_CASE,
    )
    private val PATH_SIGNAL = Regex("(?:novel|chapter|bolum|b%c3%b6l%c3%bcm|bölüm|fiction|story|/read|/oku|seri)", RegexOption.IGNORE_CASE)

    fun score(source: NovelSource): Int {
        val host = source.siteName.lowercase().removePrefix("www.")
        if (BLOCKED_HOSTS.any { host == it || host.endsWith(".$it") }) return -100

        var score = 0
        if (KNOWN_NOVEL_HOSTS.any { host == it || host.endsWith(".$it") }) score += 10
        if (NOVEL_HOST_WORDS.any { host.contains(it) }) score += 4

        val path = runCatching { URI(source.url).rawPath.orEmpty() }.getOrDefault("")
        if (PATH_SIGNAL.containsMatchIn(path)) score += 2
        if (TEXT_SIGNAL.containsMatchIn(source.title)) score += 2
        if (TEXT_SIGNAL.containsMatchIn(source.snippet)) score += 1
        if (NEGATIVE_TEXT.containsMatchIn(source.title)) score -= 4
        if (NEGATIVE_TEXT.containsMatchIn(source.snippet)) score -= 2
        return score
    }

    fun isNovelSite(source: NovelSource): Boolean = score(source) >= MIN_SCORE
}
