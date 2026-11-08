package com.romanarsiv.data.remote

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChapterExtractorTest {

    private val longText = "Bu cümle romanın bir parçasıdır, okur onu dikkatle izler. ".repeat(12)

    @Test
    fun `dizin sayfasındaki bölüm bağlantıları eskiden yeniye dizilir`() {
        val html = """
            <html><body>
              <nav><a href="/">Ana sayfa</a></nav>
              <ul class="chapters">
                <li><a href="/roman/bolum-3">Bölüm 3: Son</a></li>
                <li><a href="/roman/bolum-2">Bölüm 2: Orta</a></li>
                <li><a href="/roman/bolum-1">1. Bölüm - Başlangıç</a></li>
              </ul>
              <a href="https://baska-site.com/bolum-9">Bölüm 9</a>
              <a href="/roman/bolum-1#yorumlar">Bölüm 1 yorumları</a>
            </body></html>
        """.trimIndent()
        val doc = Jsoup.parse(html, "https://ornek.com/roman")
        val links = ChapterExtractor.findChapterLinks(doc)
        assertEquals(listOf(1, 2, 3), links.map { it.number })
        assertEquals("https://ornek.com/roman/bolum-1", links.first().url)
        assertTrue(links.none { it.url.contains("baska-site") })
    }

    @Test
    fun `numara İngilizce metinden ve adresten okunur`() {
        assertEquals(12, ChapterExtractor.chapterNumberFromText("Chapter 12 - The Gate"))
        assertEquals(7, ChapterExtractor.chapterNumberFromText("7. Bölüm"))
        assertEquals(41, ChapterExtractor.chapterNumberFromHref("https://x.com/novel/chapter-41/"))
        assertNull(ChapterExtractor.chapterNumberFromText("Yorumlar (12)"))
    }

    @Test
    fun `sonraki bağlantısı metinden, rel'den ve sınıftan bulunur`() {
        val byText = Jsoup.parse("""<a href="/b/1">« Önceki</a><a href="/b/3">Sonraki Bölüm »</a>""", "https://s.com/b/2")
        assertEquals("https://s.com/b/3", ChapterExtractor.findNextLink(byText))

        val byRel = Jsoup.parse("""<head><link rel="next" href="/b/9"></head><body></body>""", "https://s.com/b/8")
        assertEquals("https://s.com/b/9", ChapterExtractor.findNextLink(byRel))

        val byClass = Jsoup.parse("""<a class="btn btn-next" href="/b/5"><i></i></a>""", "https://s.com/b/4")
        assertEquals("https://s.com/b/5", ChapterExtractor.findNextLink(byClass))

        val none = Jsoup.parse("""<a href="/b/1">« Önceki</a><a href="https://diger.com/x">Sonraki</a>""", "https://s.com/b/2")
        assertNull(ChapterExtractor.findNextLink(none))
    }

    @Test
    fun `ana metin çöpten ayrılır ve br ile ayrılmış satırlar paragraf olur`() {
        val html = """
            <html><head><title>Bölüm 5 | Site</title></head><body>
              <header><h1 class="site">Site Adı</h1></header>
              <div class="sidebar"><p>${"Reklam metni burada duruyor, tıklayın. ".repeat(20)}</p></div>
              <h1 class="chapter-title">Bölüm 5: Fırtına</h1>
              <div id="chapter-content">
                Bölüm 5: Fırtına<br><br>
                ${longText}<br>
                <em>İkinci</em> satır geldi.<br>
                <a href="/b/4">Önceki</a> <a href="/b/6">Sonraki</a>
              </div>
              <div class="comments"><p>${"Harika bölüm! ".repeat(40)}</p></div>
              <script>var x = 1;</script>
            </body></html>
        """.trimIndent()
        val doc = Jsoup.parse(html, "https://s.com/b/5")
        val chapter = ChapterExtractor.extractChapter(doc, titleHint = null, fallbackTitle = "Bölüm 5")
        assertNotNull(chapter)
        chapter!!
        assertEquals("Bölüm 5: Fırtına", chapter.title)
        assertFalse(chapter.contentHtml.contains("Reklam"))
        assertFalse(chapter.contentHtml.contains("Harika"))
        assertFalse(chapter.contentHtml.contains("Sonraki"))
        assertFalse(chapter.contentHtml.contains("var x"))
        assertFalse("başlık içerikte tekrar etmemeli", chapter.contentHtml.contains("Fırtına"))
        val paragraphs = Jsoup.parseBodyFragment(chapter.contentHtml).select("p")
        assertEquals(2, paragraphs.size)
        assertTrue(paragraphs[1].html().startsWith("<em>İkinci</em>"))
    }

    @Test
    fun `bilinen seçici yoksa paragraf yoğunluğu kazanır`() {
        val html = """
            <body>
              <div class="menu-x"><p>Kısa.</p></div>
              <div class="wrap"><div class="inner">
                ${(1..6).joinToString("") { "<p>Paragraf $it: $longText</p>" }}
              </div></div>
              <div class="footer-ish"><p>Telif hakkı, tüm hakları saklıdır.</p></div>
            </body>
        """.trimIndent()
        val doc = Jsoup.parse(html, "https://s.com/x")
        val chapter = ChapterExtractor.extractChapter(doc, "Bölüm 1: Giriş", "Bölüm 1")!!
        assertEquals("Bölüm 1: Giriş", chapter.title)
        assertEquals(6, Jsoup.parseBodyFragment(chapter.contentHtml).select("p").size)
        assertFalse(chapter.contentHtml.contains("Telif"))
    }

    @Test
    fun `metni olmayan sayfa bölüm sayılmaz`() {
        val doc = Jsoup.parse("<body><p>Sayfa bulunamadı.</p></body>", "https://s.com/404")
        assertNull(ChapterExtractor.extractChapter(doc, null, "Bölüm 1"))
    }

    @Test
    fun `arama yönlendirmeleri çözülür`() {
        assertEquals(
            "https://ornek.com/roman?a=1",
            UrlUtils.unwrapRedirect("//duckduckgo.com/l/?uddg=https%3A%2F%2Fornek.com%2Froman%3Fa%3D1&rut=abc"),
        )
        val b64 = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("https://ornek.com/bolum-1".toByteArray())
        assertEquals("https://ornek.com/bolum-1", UrlUtils.unwrapRedirect("https://www.bing.com/ck/a?!&&p=x&u=a1$b64&ntb=1"))
        assertEquals("https://düz.com/", UrlUtils.unwrapRedirect("https://düz.com/"))
    }

    @Test
    fun `Bing sonuçlarının eski ve yeni biçimi ayrıştırılır`() {
        val html = """
            <ol>
              <li class="b_algo"><div><a href="https://yeni.com/roman" h="ID=1"><h2><strong>Roman</strong> - Tüm Bölümler</h2></a></div>
                <div class="b_caption"><p class="b_lineclamp3">Yeni biçim özet</p></div></li>
              <li class="b_algo"><h2><a href="https://eski.com/roman">Roman Oku</a></h2><div class="b_caption"><p>Eski</p></div></li>
            </ol>
        """.trimIndent()
        val results = BingSearchProvider().parse(Jsoup.parse(html, "https://www.bing.com/search"))
        assertEquals(listOf("https://yeni.com/roman", "https://eski.com/roman"), results.map { it.url })
        assertEquals("Roman - Tüm Bölümler", results[0].title)
        assertEquals("Yeni biçim özet", results[0].snippet)
    }

    @Test
    fun `DuckDuckGo sonuçları ayrıştırılır, reklamlar atlanır`() {
        val html = """
            <div class="result result--ad"><a class="result__a" href="//duckduckgo.com/l/?uddg=https%3A%2F%2Freklam.com">Reklam</a></div>
            <div class="result"><a class="result__a" href="//duckduckgo.com/l/?uddg=https%3A%2F%2Fwww.roman.com%2Fyukselis">Yükseliş - Oku</a>
              <a class="result__snippet">Tüm bölümler</a></div>
        """.trimIndent()
        val results = DuckDuckGoSearchProvider().parse(Jsoup.parse(html, "https://html.duckduckgo.com/html/"))
        assertEquals(1, results.size)
        assertEquals("https://www.roman.com/yukselis", results[0].url)
        assertEquals("roman.com", results[0].siteName)
        assertEquals("Tüm bölümler", results[0].snippet)
    }
}
