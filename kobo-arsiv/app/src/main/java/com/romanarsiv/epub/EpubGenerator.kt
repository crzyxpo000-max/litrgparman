package com.romanarsiv.epub

import com.romanarsiv.domain.AppConfig
import com.romanarsiv.domain.model.NovelChapter
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Entities
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * EPUB 3 üreticisi (EPUB 2 okuyucular için `toc.ncx` de eklenir). Harici kütüphane kullanmaz.
 *
 * - Tüm dosyalar UTF-8 ve XML bildirimiyle yazılır; Türkçe karakterler (ç ğ ı İ ö ş ü) varlık
 *   (entity) olmadan ham UTF-8 kalır, Kobo'da bozulmaz.
 * - [kepub] açıkken her paragraf `koboSpan` ile sarılır ve gövde `book-columns/book-inner`
 *   kapsayıcılarına alınır; Kobo'nun kepub motoru (sayfa sayımı, okuma istatistiği, vurgulama) bunu bekler.
 *   Dosya `.kepub.epub` uzantısıyla kaydedildiğinde Kobo kepub motorunu kullanır.
 */
class EpubGenerator(
    private val kepub: Boolean = true,
    private val clock: () -> Instant = Instant::now,
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
) {

    fun generate(
        chapters: List<NovelChapter>,
        bookTitle: String,
        author: String = AppConfig.AUTHOR_NAME,
        language: String = AppConfig.BOOK_LANGUAGE,
    ): ByteArray {
        val buffer = ByteArrayOutputStream()
        writeTo(buffer, chapters, bookTitle, author, language)
        return buffer.toByteArray()
    }

    /** Geçici dosyaya yazar (büyük kitaplarda belleği korumak için). */
    fun generateToFile(
        directory: File,
        chapters: List<NovelChapter>,
        bookTitle: String,
        author: String = AppConfig.AUTHOR_NAME,
        language: String = AppConfig.BOOK_LANGUAGE,
    ): File {
        val file = File.createTempFile("book_", ".epub", directory)
        file.outputStream().buffered().use { writeTo(it, chapters, bookTitle, author, language) }
        return file
    }

    fun writeTo(
        out: OutputStream,
        chapters: List<NovelChapter>,
        bookTitle: String,
        author: String,
        language: String,
    ) {
        require(chapters.isNotEmpty()) { "EPUB için en az bir bölüm gerekir." }
        val title = clean(bookTitle).trim().ifEmpty { "Adsız" }
        val creator = clean(author).trim().ifEmpty { "Bilinmiyor" }
        val lang = language.ifBlank { "tr" }
        val uid = "urn:uuid:${idFactory()}"
        val now = clock().truncatedTo(ChronoUnit.SECONDS)
        val modified = DateTimeFormatter.ISO_INSTANT.format(now)
        val date = DateTimeFormatter.ISO_LOCAL_DATE.format(now.atOffset(ZoneOffset.UTC))

        val entries = chapters.mapIndexed { i, ch ->
            val id = "ch%04d".format(i + 1)
            ChapterEntry(id = id, href = "text/$id.xhtml", title = clean(ch.title).trim().ifEmpty { "Bölüm ${i + 1}" }, chapter = ch)
        }

        ZipOutputStream(out).use { zip ->
            // mimetype ilk girdi olmalı ve sıkıştırılmamalı (OCF şartı).
            val mimetype = "application/epub+zip".toByteArray(Charsets.US_ASCII)
            zip.putNextEntry(
                ZipEntry("mimetype").apply {
                    method = ZipEntry.STORED
                    size = mimetype.size.toLong()
                    compressedSize = mimetype.size.toLong()
                    crc = CRC32().apply { update(mimetype) }.value
                },
            )
            zip.write(mimetype)
            zip.closeEntry()

            zip.setMethod(ZipOutputStream.DEFLATED)
            zip.text("META-INF/container.xml", CONTAINER_XML)
            zip.text("OEBPS/content.opf", opf(uid, title, creator, lang, date, modified, entries))
            zip.text("OEBPS/toc.ncx", ncx(uid, title, creator, entries))
            zip.text("OEBPS/nav.xhtml", nav(title, lang, entries))
            zip.text("OEBPS/styles.css", STYLES_CSS)
            zip.text("OEBPS/text/title.xhtml", titlePage(title, creator, lang))
            entries.forEach { zip.text("OEBPS/${it.href}", chapterPage(it, lang)) }
        }
    }

    private class ChapterEntry(val id: String, val href: String, val title: String, val chapter: NovelChapter)

    private fun ZipOutputStream.text(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    // ------------------------------------------------------------------ paket dosyaları

    private fun opf(
        uid: String,
        title: String,
        author: String,
        lang: String,
        date: String,
        modified: String,
        entries: List<ChapterEntry>,
    ): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8"?>""").append('\n')
        append("""<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="bookid" xml:lang="${esc(lang)}">""").append('\n')
        append("""  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">""").append('\n')
        append("""    <dc:identifier id="bookid">${esc(uid)}</dc:identifier>""").append('\n')
        append("""    <dc:title>${esc(title)}</dc:title>""").append('\n')
        append("""    <dc:creator id="creator">${esc(author)}</dc:creator>""").append('\n')
        append("""    <meta refines="#creator" property="role" scheme="marc:relators">aut</meta>""").append('\n')
        append("""    <dc:language>${esc(lang)}</dc:language>""").append('\n')
        append("""    <dc:date>$date</dc:date>""").append('\n')
        append("""    <meta property="dcterms:modified">$modified</meta>""").append('\n')
        append("  </metadata>\n")
        append("  <manifest>\n")
        append("""    <item id="ncx" href="toc.ncx" media-type="application/x-dtbncx+xml"/>""").append('\n')
        append("""    <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>""").append('\n')
        append("""    <item id="css" href="styles.css" media-type="text/css"/>""").append('\n')
        append("""    <item id="titlepage" href="text/title.xhtml" media-type="application/xhtml+xml"/>""").append('\n')
        entries.forEach {
            append("""    <item id="${it.id}" href="${it.href}" media-type="application/xhtml+xml"/>""").append('\n')
        }
        append("  </manifest>\n")
        append("""  <spine toc="ncx">""").append('\n')
        append("""    <itemref idref="titlepage"/>""").append('\n')
        entries.forEach { append("""    <itemref idref="${it.id}"/>""").append('\n') }
        append("  </spine>\n")
        append("</package>\n")
    }

    private fun ncx(uid: String, title: String, author: String, entries: List<ChapterEntry>): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8"?>""").append('\n')
        append("""<ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1">""").append('\n')
        append("  <head>\n")
        append("""    <meta name="dtb:uid" content="${esc(uid)}"/>""").append('\n')
        append("""    <meta name="dtb:depth" content="1"/>""").append('\n')
        append("""    <meta name="dtb:totalPageCount" content="0"/>""").append('\n')
        append("""    <meta name="dtb:maxPageNumber" content="0"/>""").append('\n')
        append("  </head>\n")
        append("  <docTitle><text>${esc(title)}</text></docTitle>\n")
        append("  <docAuthor><text>${esc(author)}</text></docAuthor>\n")
        append("  <navMap>\n")
        entries.forEachIndexed { i, e ->
            append("""    <navPoint id="nav-${e.id}" playOrder="${i + 1}">""").append('\n')
            append("      <navLabel><text>${esc(e.title)}</text></navLabel>\n")
            append("""      <content src="${e.href}"/>""").append('\n')
            append("    </navPoint>\n")
        }
        append("  </navMap>\n")
        append("</ncx>\n")
    }

    private fun nav(title: String, lang: String, entries: List<ChapterEntry>): String {
        val items = entries.joinToString("\n") { """        <li><a href="${it.href}">${esc(it.title)}</a></li>""" }
        val body = """
            |  <nav epub:type="toc" id="toc">
            |    <h1>İçindekiler</h1>
            |    <ol>
            |$items
            |    </ol>
            |  </nav>
        """.trimMargin()
        return xhtml(title = title, lang = lang, cssHref = "styles.css", body = body)
    }

    private fun titlePage(title: String, author: String, lang: String): String {
        val body = """
            |  <section class="titlepage" epub:type="titlepage">
            |    <h1 class="book-title">${esc(title)}</h1>
            |    <p class="book-author">${esc(author)}</p>
            |  </section>
        """.trimMargin()
        return xhtml(title = title, lang = lang, cssHref = "../styles.css", body = body)
    }

    private fun chapterPage(entry: ChapterEntry, lang: String): String {
        val doc: Document = Jsoup.parseBodyFragment(clean(entry.chapter.contentHtml))
        doc.outputSettings()
            .syntax(Document.OutputSettings.Syntax.xml)
            .escapeMode(Entities.EscapeMode.xhtml)
            .charset(Charsets.UTF_8)
            .prettyPrint(false)

        val body = doc.body()
        val heading = Element("h2").addClass("chapter-title").text(entry.title)
        body.prependChild(heading)

        if (kepub) {
            var paragraph = 0
            body.select("p, h1, h2, h3, h4, blockquote:not(:has(p))").forEach { block ->
                if (block.childNodeSize() == 0) return@forEach
                paragraph++
                val span = Element("span").addClass("koboSpan").attr("id", "kobo.$paragraph.1")
                val children = block.childNodes().toList()
                block.empty()
                span.appendChildren(children)
                block.appendChild(span)
            }
            val inner = Element("div").attr("id", "book-inner")
            inner.appendChildren(body.childNodes().toList())
            body.empty()
            body.appendChild(Element("div").attr("id", "book-columns").appendChild(inner))
        }

        return xhtml(
            title = entry.title,
            lang = lang,
            cssHref = "../styles.css",
            body = body.html(),
            sectionType = "chapter",
        )
    }

    private fun xhtml(title: String, lang: String, cssHref: String, body: String, sectionType: String? = null): String {
        val bodyAttr = sectionType?.let { """ epub:type="bodymatter $it"""" } ?: ""
        return buildString {
            append("""<?xml version="1.0" encoding="UTF-8"?>""").append('\n')
            append("<!DOCTYPE html>\n")
            append("""<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" xml:lang="${esc(lang)}" lang="${esc(lang)}">""").append('\n')
            append("<head>\n")
            append("""  <meta charset="UTF-8"/>""").append('\n')
            append("  <title>${esc(title)}</title>\n")
            append("""  <link rel="stylesheet" type="text/css" href="$cssHref"/>""").append('\n')
            append("</head>\n")
            append("<body$bodyAttr>\n")
            append(body).append('\n')
            append("</body>\n")
            append("</html>\n")
        }
    }

    companion object {
        /** XML 1.0'da geçersiz denetim karakterlerini atar (kazınan sayfalarda nadiren bulunur). */
        private val INVALID_XML = Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F\\uFFFE\\uFFFF]")

        internal fun clean(s: String): String = INVALID_XML.replace(s, "")

        internal fun esc(s: String): String = buildString(s.length) {
            for (c in s) {
                when (c) {
                    '&' -> append("&amp;")
                    '<' -> append("&lt;")
                    '>' -> append("&gt;")
                    '"' -> append("&quot;")
                    '\'' -> append("&apos;")
                    else -> append(c)
                }
            }
        }

        private val CONTAINER_XML = """
            |<?xml version="1.0" encoding="UTF-8"?>
            |<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
            |  <rootfiles>
            |    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
            |  </rootfiles>
            |</container>
            |
        """.trimMargin()

        private val STYLES_CSS = """
            |@charset "UTF-8";
            |body { margin: 0 4%; line-height: 1.45; text-align: justify; hyphens: auto; -webkit-hyphens: auto; }
            |p { margin: 0; text-indent: 1.4em; }
            |h1, h2, h3, h4 { text-align: center; text-indent: 0; hyphens: none; -webkit-hyphens: none; }
            |h2.chapter-title { margin: 2em 0 1.5em; font-size: 1.4em; }
            |blockquote { margin: 1em 2em; font-style: italic; }
            |hr { border: 0; border-top: 1px solid #888; margin: 1.5em 30%; }
            |.titlepage { text-align: center; margin-top: 30%; }
            |.book-title { font-size: 2em; margin-bottom: 0.5em; }
            |.book-author { text-indent: 0; text-align: center; font-size: 1.2em; }
            |nav ol { list-style: none; padding-left: 0; }
            |nav li { margin: 0.3em 0; }
            |
        """.trimMargin()
    }
}
