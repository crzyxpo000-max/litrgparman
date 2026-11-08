package com.romanarsiv.domain

import com.romanarsiv.domain.model.ChapterRef
import com.romanarsiv.domain.model.NovelChapter
import com.romanarsiv.domain.model.NovelSource
import com.romanarsiv.domain.repository.NovelRepository
import com.romanarsiv.domain.usecase.SearchNovelUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NovelSiteFilterTest {

    private fun src(url: String, title: String, snippet: String = "") =
        NovelSource(url, java.net.URI(url).host.removePrefix("www."), title, snippet)

    @Test
    fun `roman siteleri geçer`() {
        assertTrue(NovelSiteFilter.isNovelSite(src("https://www.wattpad.com/story/123-yukselis", "Yükseliş - Wattpad")))
        assertTrue(NovelSiteFilter.isNovelSite(src("https://www.royalroad.com/fiction/1/ascension", "Ascension Protocol")))
        assertTrue(NovelSiteFilter.isNovelSite(src("https://noveltr.com/yukselis", "Yükseliş Protokolü")))
        assertTrue(NovelSiteFilter.isNovelSite(src("https://blog.example.com/seri/yukselis", "Yükseliş Protokolü 1. Bölüm Oku")))
        assertTrue(NovelSiteFilter.isNovelSite(src("https://ornek.net/yukselis-protokolu/bolum-12", "Yükseliş Protokolü")))
    }

    @Test
    fun `mağaza, ansiklopedi, dizi ve sosyal siteler elenir`() {
        assertFalse(NovelSiteFilter.isNovelSite(src("https://www.kitapyurdu.com/kitap/yukselis/1.html", "Yükseliş Protokolü - Roman")))
        assertFalse(NovelSiteFilter.isNovelSite(src("https://tr.wikipedia.org/wiki/Yükseliş", "Yükseliş - Vikipedi, novel")))
        assertFalse(NovelSiteFilter.isNovelSite(src("https://www.showtv.com.tr/dizi/baba", "Baba - Tüm Bölümler")))
        assertFalse(NovelSiteFilter.isNovelSite(src("https://dizisitesi.com/baba", "Baba 12. Bölüm izle", "Full HD izle")))
        assertFalse(NovelSiteFilter.isNovelSite(src("https://haber.example.com/kultur/yeni-kitap", "Yazarın yeni romanı çıktı")))
    }

    @Test
    fun `arama sonuçları süzülür, bilinen roman siteleri üste çıkar, URL araması süzülmez`() = runTest {
        val all = listOf(
            src("https://www.kitapyurdu.com/kitap/x", "Kitap"),
            src("https://blog.example.com/x/bolum-1", "X 1. Bölüm"),
            src("https://www.wattpad.com/story/1-x", "X"),
        )
        val repo = object : NovelRepository {
            override suspend fun search(query: String) = all
            override suspend fun listChapters(source: NovelSource, onFound: suspend (Int) -> Unit) = emptyList<ChapterRef>()
            override suspend fun fetchChapter(ref: ChapterRef, position: Int): NovelChapter? = null
        }
        val result = SearchNovelUseCase(repo)("X")
        assertEquals(listOf("wattpad.com", "blog.example.com"), result.novelSites.map { it.siteName })
        assertEquals(listOf("kitapyurdu.com"), result.hidden.map { it.siteName })

        val byUrl = SearchNovelUseCase(repo)("https://www.kitapyurdu.com/kitap/x")
        assertEquals(3, byUrl.novelSites.size)
        assertTrue(byUrl.hidden.isEmpty())
    }
}
