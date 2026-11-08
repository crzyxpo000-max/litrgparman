package com.novelepub.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class EpubWriterTest {
    private val novel = NovelInfo(
        title = "Tom & Jerry <Novel>",
        author = "O'Brien",
        url = "https://example.com/novel",
        description = "Line one\n\nLine \"two\"",
        language = "en",
    )

    private fun build(cover: Cover?): Pair<List<ZipEntry>, Map<String, ByteArray>> {
        val out = ByteArrayOutputStream()
        EpubWriter(out, novel, cover, Instant.parse("2026-01-02T03:04:05.678Z")).use { w ->
            w.addChapter(Chapter("Chapter 1 & intro", "https://example.com/1", "<p>Hello&#xa0;world</p>"))
            w.addChapter(Chapter("Chapter 2", "https://example.com/2", "<p>Bye</p><table><tbody><tr><td>1</td></tr></tbody></table>"))
        }
        val entries = ArrayList<ZipEntry>()
        val files = LinkedHashMap<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(out.toByteArray())).use { zip ->
            while (true) {
                val e = zip.nextEntry ?: break
                entries += e
                files[e.name] = zip.readBytes()
            }
        }
        return entries to files
    }

    @Test
    fun `mimetype is first and stored`() {
        val (entries, files) = build(null)
        assertEquals("mimetype", entries.first().name)
        assertEquals(ZipEntry.STORED, entries.first().method)
        assertEquals("application/epub+zip", String(files.getValue("mimetype")))
    }

    @Test
    fun `all xml files are well formed`() {
        val (_, files) = build(Cover.detect(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0, 0)))
        val xml = files.filterKeys { it.endsWith(".xhtml") || it.endsWith(".opf") || it.endsWith(".ncx") || it.endsWith(".xml") }
        assertEquals(
            setOf(
                "META-INF/container.xml", "OEBPS/content.opf", "OEBPS/nav.xhtml", "OEBPS/toc.ncx", "OEBPS/title.xhtml",
                "OEBPS/cover.xhtml", "OEBPS/text/c00001.xhtml", "OEBPS/text/c00002.xhtml",
            ),
            xml.keys,
        )
        xml.forEach { (name, bytes) -> assertWellFormed(String(bytes), name) }
    }

    @Test
    fun `package lists chapters in order with metadata`() {
        val (_, files) = build(Cover.detect(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)))
        val opf = String(files.getValue("OEBPS/content.opf"))
        assertTrue(opf, opf.contains("<dc:title>Tom &amp; Jerry &lt;Novel&gt;</dc:title>"))
        assertTrue(opf, opf.contains("<dc:creator>O&apos;Brien</dc:creator>"))
        assertTrue(opf, opf.contains("<meta property=\"dcterms:modified\">2026-01-02T03:04:05Z</meta>"))
        assertTrue(opf, opf.contains("href=\"images/cover.png\" media-type=\"image/png\" properties=\"cover-image\""))
        assertTrue(opf.indexOf("idref=\"c1\"") < opf.indexOf("idref=\"c2\""))
        val nav = String(files.getValue("OEBPS/nav.xhtml"))
        assertTrue(nav, nav.contains("<a href=\"text/c00001.xhtml\">Chapter 1 &amp; intro</a>"))
        assertArrayEquals(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47), files.getValue("OEBPS/images/cover.png"))
    }

    @Test
    fun `book id is stable per source url`() {
        val a = String(build(null).second.getValue("OEBPS/content.opf"))
        val b = String(build(null).second.getValue("OEBPS/content.opf"))
        val id = Regex("<dc:identifier id=\"bookid\">([^<]+)</dc:identifier>")
        assertEquals(id.find(a)!!.groupValues[1], id.find(b)!!.groupValues[1])
    }

    @Test
    fun `cover detection by magic bytes`() {
        assertEquals("image/jpeg", Cover.detect(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()))?.mediaType)
        assertEquals("image/gif", Cover.detect("GIF89a".toByteArray())?.mediaType)
        assertEquals("image/webp", Cover.detect("RIFF\u0000\u0000\u0000\u0000WEBPVP8 ".toByteArray())?.mediaType)
        assertNull(Cover.detect("<html>".toByteArray()))
        assertNull(Cover.detect(ByteArray(0)))
    }

    @Test
    fun `file names are sanitized`() {
        assertEquals("A B C.epub", epubFileName("A/B: C?"))
        assertEquals("novel.epub", epubFileName("  "))
    }
}
