package com.romanarsiv.data.repository

import com.romanarsiv.domain.AppException
import com.romanarsiv.data.remote.ChapterExtractor
import com.romanarsiv.data.remote.ChapterLink
import com.romanarsiv.data.remote.PageLoader
import com.romanarsiv.data.remote.SearchProvider
import com.romanarsiv.data.remote.UrlUtils
import com.romanarsiv.domain.AppConfig
import com.romanarsiv.domain.model.ChapterRef
import com.romanarsiv.domain.model.NovelChapter
import com.romanarsiv.domain.model.NovelSource
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

        // Önce roman sitelerini öne çıkaran sorgu, sonra kullanıcının yazdığı haliyle.
        val variants = listOf("$query novel oku", query)
        val results = LinkedHashMap<String, NovelSource>()
        var firstError: IOException? = null
        search@ for (provider in providers) {
            for (variant in variants) {
                try {
                    provider.parse(loader.load(provider.searchUrl(variant)))
                        .filterNot { isNoise(it.siteName) }
                        .forEach { results.putIfAbsent(UrlUtils.normalize(it.url), it) }
                } catch (e: IOException) {
                    if (firstError == null) firstError = e
                    continue@search // bu motor engelliyorsa diğer sorguyu deneme, sonraki motora geç
                }
                if (results.size >= ENOUGH_RESULTS) break@search
            }
        }
        if (results.isEmpty() && firstError != null) {
            throw AppException(AppException.Reason.SEARCH_FAILED, firstError.message ?: firstError.javaClass.simpleName, firstError)
        }
        return results.values.toList()
    }

    private fun isNoise(host: String): Boolean = NOISE_HOSTS.any { host == it || host.endsWith(".$it") }

    // ------------------------------------------------------------------ bölüm tespiti

    override suspend fun listChapters(
        source: NovelSource,
        onFound: suspend (count: Int) -> Unit,
    ): List<ChapterRef> {
        val first = loader.load(source.url)
        val links = ChapterExtractor.findChapterLinks(first)
        val textLength = ChapterExtractor.mainTextLength(first)
        val isChapterPage = textLength >= ChapterExtractor.CHAPTER_PAGE_CHARS

        val refs = when {
            // Dizin sayfası ya da bölüm listesini (seçme kutusu) içeren bir bölüm sayfası.
            links.size >= 2 && (!isChapterPage || ChapterExtractor.listContainsPage(links, first.location())) ->
                listFromIndex(first, links, followIndexPagination = !isChapterPage, onFound = onFound)
            // Tek bir bölüm: "sonraki" bağlantılarını izle (metinler bu sırada indirilir).
            textLength >= ChapterExtractor.MIN_CHAPTER_CHARS -> listFromChain(first, onFound)
            links.isNotEmpty() -> listFromIndex(first, links, followIndexPagination = true, onFound = onFound)
            else -> emptyList()
        }
        if (refs.isEmpty()) {
            throw AppException(AppException.Reason.NO_CHAPTERS_FOUND)
        }
        return refs
    }

    private suspend fun listFromIndex(
        first: Document,
        initialLinks: List<ChapterLink>,
        followIndexPagination: Boolean,
        onFound: suspend (Int) -> Unit,
    ): List<ChapterRef> {
        val all = LinkedHashMap<String, ChapterLink>()
        initialLinks.forEach { all.putIfAbsent(UrlUtils.normalize(it.url), it) }
        onFound(all.size)

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
                onFound(all.size)
                next = ChapterExtractor.findNextLink(page)
                pages++
            }
        }

        return sortIfFullyNumbered(all.values.toList())
            .take(maxChapters)
            .mapIndexed { i, link -> ChapterRef(url = link.url, title = link.text.ifBlank { "Bölüm ${i + 1}" }) }
    }

    /** Birden çok dizin sayfası ters sırada birleşmiş olabilir; her bağlantının tekil numarası varsa numaraya göre diz. */
    private fun sortIfFullyNumbered(links: List<ChapterLink>): List<ChapterLink> {
        val numbers = links.map { it.number ?: return links }
        if (numbers.toSet().size != numbers.size) return links
        return links.sortedBy { it.number }
    }

    private suspend fun listFromChain(first: Document, onFound: suspend (Int) -> Unit): List<ChapterRef> {
        val refs = ArrayList<ChapterRef>()
        val visited = HashSet<String>()
        var doc = first
        while (refs.size < maxChapters) {
            visited += UrlUtils.normalize(doc.location())
            // Zincirin sonunda çoğunlukla metinsiz bir "son" ya da dizin sayfası olur.
            val chapter = ChapterExtractor.extractChapter(doc, null, "Bölüm ${refs.size + 1}") ?: break
            refs += ChapterRef(url = doc.location(), title = chapter.title, preloaded = chapter)
            onFound(refs.size)

            val next = ChapterExtractor.findNextLink(doc) ?: break
            if (UrlUtils.normalize(next) in visited) break
            doc = try {
                loader.load(next)
            } catch (_: IOException) {
                break
            }
        }
        return refs
    }

    // ------------------------------------------------------------------ bölüm indirme

    override suspend fun fetchChapter(ref: ChapterRef, position: Int): NovelChapter? {
        ref.preloaded?.let { return it }
        val doc = try {
            loader.load(ref.url)
        } catch (_: IOException) {
            return null
        }
        return ChapterExtractor.extractChapter(doc, ref.title, "Bölüm $position")
    }

    private companion object {
        const val ENOUGH_RESULTS = 30

        /** Metin barındırmayan, sonuçları kirleten siteler. */
        val NOISE_HOSTS = listOf(
            "youtube.com", "youtu.be", "facebook.com", "instagram.com", "twitter.com", "x.com", "tiktok.com",
            "pinterest.com", "linkedin.com", "spotify.com", "music.apple.com", "play.google.com", "apps.apple.com",
        )
    }
}
