package com.romanarsiv.epub

import com.romanarsiv.domain.model.NovelChapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

class EpubGeneratorTest {

    private val generator = EpubGenerator(
        kepub = true,
        clock = { Instant.parse("2026-11-08T10:15:30.123Z") },
        idFactory = { "00000000-0000-0000-0000-000000000001" },
    )

    private val chapters = listOf(
        NovelChapter("1. Bölüm: Işığın Çağrısı", "<p>Şimdi ılık rüzgâr esiyordu; İğdır'dan gelen yolcu gülümsedi.</p><p>İkinci &amp; son <em>paragraf</em>.<br>Satır.</p>"),
        NovelChapter("2. Bölüm: Öğle <Sıcağı>", "<p>Küçük bir çocuk \"merhaba\" dedi.&nbsp;Sonra güldü.</p><hr><blockquote>Alıntı</blockquote>"),
    )

    private fun unzip(bytes: ByteArray): List<Pair<ZipEntry, ByteArray>> {
        val result = ArrayList<Pair<ZipEntry, ByteArray>>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val e = zip.nextEntry ?: break
                result += e to zip.readBytes()
            }
        }
        return result
    }

    @Test
    fun `mimetype ilk girdi ve sıkıştırılmamış`() {
        val bytes = generator.generate(chapters, "Yükseliş", "Yazar Adı")
        // OCF: ilk yerel başlık "mimetype" adını ve ham içeriği taşır.
        val header = String(bytes, 30, 8, Charsets.US_ASCII)
        assertEquals("mimetype", header)
        assertEquals("application/epub+zip", String(bytes, 38, 20, Charsets.US_ASCII))

        val entries = unzip(bytes)
        assertEquals("mimetype", entries.first().first.name)
        assertEquals(ZipEntry.STORED, entries.first().first.method)
    }

    @Test
    fun `gerekli dosyalar var ve hepsi geçerli XML`() {
        val entries = unzip(generator.generate(chapters, "Yükseliş", "Yazar Adı")).associate { it.first.name to it.second }
        val required = listOf(
            "META-INF/container.xml", "OEBPS/content.opf", "OEBPS/toc.ncx", "OEBPS/nav.xhtml",
            "OEBPS/styles.css", "OEBPS/text/title.xhtml", "OEBPS/text/ch0001.xhtml", "OEBPS/text/ch0002.xhtml",
        )
        required.forEach { assertTrue("eksik: $it", entries.containsKey(it)) }

        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        entries.filterKeys { it.endsWith(".xml") || it.endsWith(".opf") || it.endsWith(".ncx") || it.endsWith(".xhtml") }
            .forEach { (name, data) ->
                try {
                    factory.newDocumentBuilder().parse(ByteArrayInputStream(data))
                } catch (e: Exception) {
                    throw AssertionError("$name geçerli XML değil: ${e.message}\n${String(data, Charsets.UTF_8)}", e)
                }
            }
    }

    @Test
    fun `Türkçe karakterler ham UTF-8 olarak kalır`() {
        val entries = unzip(generator.generate(chapters, "Yükseliş Protokolü", "Çağrı Işık")).associate { it.first.name to it.second }
        val ch1 = String(entries.getValue("OEBPS/text/ch0001.xhtml"), Charsets.UTF_8)
        assertTrue(ch1.contains("Şimdi ılık rüzgâr esiyordu; İğdır'dan"))
        assertTrue(ch1.startsWith("""<?xml version="1.0" encoding="UTF-8"?>"""))
        assertFalse("adlandırılmış HTML varlığı XHTML'de geçersiz", ch1.contains("&nbsp;"))
        assertTrue(ch1.contains("<br />") || ch1.contains("<br/>"))

        val opf = String(entries.getValue("OEBPS/content.opf"), Charsets.UTF_8)
        assertTrue(opf.contains("<dc:title>Yükseliş Protokolü</dc:title>"))
        assertTrue(opf.contains(">Çağrı Işık</dc:creator>"))
        assertTrue(opf.contains("<dc:language>tr</dc:language>"))
        assertTrue(opf.contains("""<meta property="dcterms:modified">2026-11-08T10:15:30Z</meta>"""))
    }

    @Test
    fun `içindekiler her bölümü sırayla içerir ve başlıklar kaçışlanır`() {
        val entries = unzip(generator.generate(chapters, "Kitap", "Yazar")).associate { it.first.name to it.second }
        val ncx = String(entries.getValue("OEBPS/toc.ncx"), Charsets.UTF_8)
        val nav = String(entries.getValue("OEBPS/nav.xhtml"), Charsets.UTF_8)
        assertTrue(ncx.indexOf("1. Bölüm: Işığın Çağrısı") < ncx.indexOf("2. Bölüm: Öğle &lt;Sıcağı&gt;"))
        assertTrue(ncx.contains("""playOrder="2""""))
        assertTrue(nav.contains("""<a href="text/ch0002.xhtml">2. Bölüm: Öğle &lt;Sıcağı&gt;</a>"""))

        val opf = String(entries.getValue("OEBPS/content.opf"), Charsets.UTF_8)
        assertTrue(opf.indexOf("""idref="ch0001"""") < opf.indexOf("""idref="ch0002""""))
    }

    @Test
    fun `kepub paragrafları koboSpan ile sarar`() {
        val entries = unzip(generator.generate(chapters, "Kitap", "Yazar")).associate { it.first.name to it.second }
        val ch1 = String(entries.getValue("OEBPS/text/ch0001.xhtml"), Charsets.UTF_8)
        assertTrue(ch1.contains("""<div id="book-columns"><div id="book-inner">"""))
        assertTrue(ch1.contains("""<span class="koboSpan" id="kobo.1.1">1. Bölüm: Işığın Çağrısı</span>"""))
        assertTrue(ch1.contains("""id="kobo.3.1""""))

        val plain = EpubGenerator(kepub = false).generate(chapters, "Kitap", "Yazar")
        val plainCh1 = String(unzip(plain).first { it.first.name == "OEBPS/text/ch0001.xhtml" }.second, Charsets.UTF_8)
        assertFalse(plainCh1.contains("koboSpan"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `bölümsüz kitap reddedilir`() {
        generator.generate(emptyList(), "Kitap", "Yazar")
    }
}
