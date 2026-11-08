package com.romanarsiv.data.repository

import com.romanarsiv.data.remote.ChapterExtractor
import com.romanarsiv.data.remote.ChapterLink
import com.romanarsiv.data.remote.PageLoader
import com.romanarsiv.data.remote.SearchProvider
import com.romanarsiv.data.remote.UrlUtils
import com.romanarsiv.domain.AppConfig
import com.romanarsiv.domain.model.NovelChapter
import com.romanarsiv.domain.model.NovelSource
import com.romanarsiv.domain.repository.ChapterFetchResult
import com.romanarsiv.domain.repository.NovelRepository
import org.jsoup.nodes.Document
import java.io.IOException

class NovelRepositoryImpl(
    private val loader: PageLoader,
    private val providers: List<SearchProvider>,
    private val maxChapters: Int = AppConfig.MAX_CHAPTERS,
    private val maxIndexPages: Int = 100,
) : NovelRepository {

    // ------------------------------------------------------------------ arama

    override suspend fun search(query: String): List<NovelSource> {
        if (UrlUtils.isHttpUrl(query)) {
            val doc = loader.load(query.trim())
            val url = doc.location().ifEmpty { query.trim() }
            return listOf(NovelSource(url, UrlUtils.hostOf(url) ?: url, ChapterExtractor.pageTitle(doc)))
        }

        val results = LinkedHashMap<String, NovelSource>()
        var firstError: IOException? = null
        for (provider in providers) {
            try {
                provider.parse(loader.load(provider.searchUrl(query)))
                    .filterNot { isNoise(it.siteName) }
                    .forEach { results.putIfAbsent(UrlUtils.normalize(it.url), it) }
            } catch (e: IOException) {
                if (firstError == null) firstError = e
            }
            if (results.size >= ENOUGH_RESULTS) break
        }
        if (results.isEmpty() && firstError != null) {
            throw IOException("Arama yapılamadı: ${firstError.message ?: firstError.javaClass.simpleName}", firstError)
        }
        return results.values.toList()
    }

    private fun isNoise(host: String): Boolean = NOISE_HOSTS.any { host == it || host.endsWith(".$it") }

    // ------------------------------------------------------------------ bölümler

    override suspend fun fetchChapters(
        source: NovelSource,
        onChapter: suspend (index: Int, total: Int, title: String) -> Unit,
    ): ChapterFetchResult {
        val first = loader.load(source.url)
        val links = ChapterExtractor.findChapterLinks(first)
        val textLength = ChapterExtractor.mainTextLength(first)
        val isChapterPage = textLength >= ChapterExtractor.CHAPTER_PAGE_CHARS

        val result = when {
            // Dizin sayfası ya da bölüm listesini (seçme kutusu) içeren bir bölüm sayfası.
            links.size >= 2 && (!isChapterPage || ChapterExtractor.listContainsPage(links, first.location())) ->
                fetchFromList(first, links, followIndexPagination = !isChapterPage, onChapter = onChapter)
            // Tek bir bölüm: "sonraki" bağlantılarını izle.
            textLength >= ChapterExtractor.MIN_CHAPTER_CHARS -> fetchChain(first, onChapter)
            links.isNotEmpty() -> fetchFromList(first, links, followIndexPagination = true, onChapter = onChapter)
            else -> ChapterFetchResult(emptyList(), 0)
        }
        if (result.chapters.isEmpty()) {
            throw IOException("Bu sayfada bölüm bağlantısı ya da okunabilir metin bulunamadı.")
        }
        return result
    }

    private suspend fun fetchFromList(
        first: Document,
        initialLinks: List<ChapterLink>,
        followIndexPagination: Boolean,
        onChapter: suspend (Int, Int, String) -> Unit,
    ): ChapterFetchResult {
        val all = LinkedHashMap<String, ChapterLink>()
        initialLinks.forEach { all.putIfAbsent(UrlUtils.normalize(it.url), it) }

        // Bölüm listesi birden çok dizin sayfasına bölünmüş olabilir (?page=2 ...).
        if (followIndexPagination) {
            val visited = hashSetOf(UrlUtils.normalize(first.location()))
            var next = ChapterExtractor.findNextLink(first)
            var pages = 0
            while (next != null && pages < maxIndexPages && all.size < maxChapters) {
                if (!visited.add(UrlUtils.normalize(next)) || all.containsKey(UrlUtils.normalize(next))) break
                val page = try {
                    loader.load(next)
                } catch (_: IOException) {
                    break
                }
                val added = ChapterExtractor.findChapterLinks(page).count { all.putIfAbsent(UrlUtils.normalize(it.url), it) == null }
                if (added == 0) break
                next = ChapterExtractor.findNextLink(page)
                pages++
            }
        }

        val links = sortIfFullyNumbered(all.values.toList()).take(maxChapters)
        val firstKey = UrlUtils.normalize(first.location())
        val chapters = ArrayList<NovelChapter>(links.size)
        var skipped = 0
        links.forEachIndexed { i, link ->
            val doc = if (UrlUtils.normalize(link.url) == firstKey) {
                first
            } else {
                try {
                    loader.load(link.url)
                } catch (_: IOException) {
                    null
                }
            }
            val chapter = doc?.let { ChapterExtractor.extractChapter(it, link.text, "Bölüm ${i + 1}") }
            if (chapter == null) {
                skipped++
            } else {
                chapters += chapter
                onChapter(i + 1, links.size, chapter.title)
            }
        }
        return ChapterFetchResult(chapters, skipped)
    }

    /** Birden çok dizin sayfası ters sırada birleşmiş olabilir; her bağlantının tekil numarası varsa numaraya göre diz. */
    private fun sortIfFullyNumbered(links: List<ChapterLink>): List<ChapterLink> {
        val numbers = links.map { it.number ?: return links }
        if (numbers.toSet().size != numbers.size) return links
        return links.sortedBy { it.number }
    }

    private suspend fun fetchChain(
        first: Document,
        onChapter: suspend (Int, Int, String) -> Unit,
    ): ChapterFetchResult {
        val chapters = ArrayList<NovelChapter>()
        val visited = HashSet<String>()
        var skipped = 0
        var doc = first
        while (chapters.size < maxChapters) {
            visited += UrlUtils.normalize(doc.location())
            val chapter = ChapterExtractor.extractChapter(doc, null, "Bölüm ${chapters.size + 1}")
            if (chapter == null) {
                // Zincirin sonunda çoğunlukla metinsiz bir "son" ya da dizin sayfası olur.
                break
            }
            chapters += chapter
            onChapter(chapters.size, 0, chapter.title)

            val next = ChapterExtractor.findNextLink(doc) ?: break
            if (UrlUtils.normalize(next) in visited) break
            doc = try {
                loader.load(next)
            } catch (_: IOException) {
                skipped++
                break
            }
        }
        return ChapterFetchResult(chapters, skipped)
    }

    private companion object {
        const val ENOUGH_RESULTS = 15

        /** Metin barındırmayan, sonuçları kirleten siteler. */
        val NOISE_HOSTS = listOf(
            "youtube.com", "youtu.be", "facebook.com", "instagram.com", "twitter.com", "x.com", "tiktok.com",
            "pinterest.com", "linkedin.com", "spotify.com", "music.apple.com", "play.google.com", "apps.apple.com",
        )
    }
}
