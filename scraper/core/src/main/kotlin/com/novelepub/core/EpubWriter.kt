package com.novelepub.core

import com.novelepub.core.ContentCleaner.escapeXml
import java.io.OutputStream
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** A cover image with its detected media type. */
class Cover(val bytes: ByteArray, val mediaType: String, val extension: String) {
    companion object {
        /** Recognizes JPEG, PNG, GIF and WebP by their magic bytes; anything else is dropped. */
        fun detect(bytes: ByteArray): Cover? {
            fun at(i: Int) = if (i < bytes.size) bytes[i].toInt() and 0xFF else -1
            return when {
                at(0) == 0xFF && at(1) == 0xD8 -> Cover(bytes, "image/jpeg", "jpg")
                at(0) == 0x89 && at(1) == 0x50 && at(2) == 0x4E && at(3) == 0x47 -> Cover(bytes, "image/png", "png")
                at(0) == 'G'.code && at(1) == 'I'.code && at(2) == 'F'.code -> Cover(bytes, "image/gif", "gif")
                bytes.size > 12 && String(bytes, 0, 4, Charsets.US_ASCII) == "RIFF" &&
                    String(bytes, 8, 4, Charsets.US_ASCII) == "WEBP" -> Cover(bytes, "image/webp", "webp")
                else -> null
            }
        }
    }
}

/**
 * Writes an EPUB 3 file (with an EPUB 2 NCX for older readers). Chapters are streamed into the
 * archive one by one, so a long novel never has to be held in memory.
 *
 * Usage: `EpubWriter(out, novel, cover).use { w -> chapters.forEach(w::addChapter) }`.
 */
class EpubWriter(
    out: OutputStream,
    private val novel: NovelInfo,
    private val cover: Cover? = null,
    private val modified: Instant = Instant.now(),
) : AutoCloseable {
    private val zip = ZipOutputStream(out)
    private val chapterTitles = ArrayList<String>()
    private val lang = escapeXml(novel.language.ifBlank { "en" })
    private var closed = false

    init {
        // The mimetype must be the first entry and stored uncompressed.
        val mimetype = "application/epub+zip".toByteArray(Charsets.US_ASCII)
        val entry = ZipEntry("mimetype").apply {
            method = ZipEntry.STORED
            size = mimetype.size.toLong()
            compressedSize = mimetype.size.toLong()
            crc = CRC32().apply { update(mimetype) }.value
        }
        zip.putNextEntry(entry)
        zip.write(mimetype)
        zip.closeEntry()

        put("META-INF/container.xml", CONTAINER)
        put("OEBPS/style.css", STYLE)
        if (cover != null) {
            putBytes("OEBPS/images/cover.${cover.extension}", cover.bytes)
            put("OEBPS/cover.xhtml", page("Cover", "<div class=\"cover\"><img src=\"images/cover.${cover.extension}\" alt=\"Cover\"/></div>", ""))
        }
        put("OEBPS/title.xhtml", titlePage())
    }

    val chapterCount: Int get() = chapterTitles.size

    fun addChapter(chapter: Chapter) {
        check(!closed) { "EPUB already finished" }
        chapterTitles += chapter.title
        val body = "<section epub:type=\"chapter\"><h1>${escapeXml(chapter.title)}</h1>\n${chapter.html}\n</section>"
        put("OEBPS/text/${chapterFile(chapterTitles.size)}", page(chapter.title, body, "../"))
    }

    /** Writes the navigation and package files and closes the archive and the underlying stream. */
    override fun close() {
        if (closed) return
        closed = true
        put("OEBPS/nav.xhtml", nav())
        put("OEBPS/toc.ncx", ncx())
        put("OEBPS/content.opf", opf())
        zip.finish()
        zip.close()
    }

    private val bookId: String = "urn:uuid:" + UUID.nameUUIDFromBytes(novel.url.toByteArray(Charsets.UTF_8))

    private fun chapterFile(n: Int) = "c%05d.xhtml".format(n)

    private fun put(name: String, text: String) = putBytes(name, text.toByteArray(Charsets.UTF_8))

    private fun putBytes(name: String, bytes: ByteArray) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(bytes)
        zip.closeEntry()
    }

    private fun page(title: String, body: String, root: String) = """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" xml:lang="$lang" lang="$lang">
<head>
<title>${escapeXml(title)}</title>
<link rel="stylesheet" type="text/css" href="${root}style.css"/>
</head>
<body>
$body
</body>
</html>
"""

    private fun titlePage(): String {
        val description = novel.description.split(Regex("\\n\\s*\\n|\\n"))
            .map { it.trim() }.filter { it.isNotEmpty() }
            .joinToString("\n") { "<p>${escapeXml(it)}</p>" }
        val body = """<section epub:type="titlepage" class="titlepage">
<h1>${escapeXml(novel.title)}</h1>
<p class="author">${escapeXml(novel.author)}</p>
<div class="description">
$description
</div>
<p class="source">${escapeXml(novel.url)}</p>
</section>"""
        return page(novel.title, body, "")
    }

    private fun nav(): String {
        val items = chapterTitles.mapIndexed { i, t ->
            "<li><a href=\"text/${chapterFile(i + 1)}\">${escapeXml(t)}</a></li>"
        }.joinToString("\n")
        val body = """<nav epub:type="toc" id="toc">
<h1>Contents</h1>
<ol>
<li><a href="title.xhtml">${escapeXml(novel.title)}</a></li>
$items
</ol>
</nav>"""
        return page("Contents", body, "")
    }

    private fun ncx(): String {
        var order = 0
        fun point(label: String, src: String): String {
            order++
            return "<navPoint id=\"np$order\" playOrder=\"$order\"><navLabel><text>${escapeXml(label)}</text></navLabel><content src=\"$src\"/></navPoint>"
        }
        val points = buildList {
            add(point(novel.title, "title.xhtml"))
            chapterTitles.forEachIndexed { i, t -> add(point(t, "text/${chapterFile(i + 1)}")) }
        }.joinToString("\n")
        return """<?xml version="1.0" encoding="UTF-8"?>
<ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1">
<head>
<meta name="dtb:uid" content="${escapeXml(bookId)}"/>
<meta name="dtb:depth" content="1"/>
<meta name="dtb:totalPageCount" content="0"/>
<meta name="dtb:maxPageNumber" content="0"/>
</head>
<docTitle><text>${escapeXml(novel.title)}</text></docTitle>
<navMap>
$points
</navMap>
</ncx>
"""
    }

    private fun opf(): String {
        val manifest = StringBuilder()
        val spine = StringBuilder()
        manifest.appendLine("<item id=\"nav\" href=\"nav.xhtml\" media-type=\"application/xhtml+xml\" properties=\"nav\"/>")
        manifest.appendLine("<item id=\"ncx\" href=\"toc.ncx\" media-type=\"application/x-dtbncx+xml\"/>")
        manifest.appendLine("<item id=\"css\" href=\"style.css\" media-type=\"text/css\"/>")
        if (cover != null) {
            manifest.appendLine("<item id=\"cover-image\" href=\"images/cover.${cover.extension}\" media-type=\"${cover.mediaType}\" properties=\"cover-image\"/>")
            manifest.appendLine("<item id=\"cover\" href=\"cover.xhtml\" media-type=\"application/xhtml+xml\"/>")
            spine.appendLine("<itemref idref=\"cover\"/>")
        }
        manifest.appendLine("<item id=\"title\" href=\"title.xhtml\" media-type=\"application/xhtml+xml\"/>")
        spine.appendLine("<itemref idref=\"title\"/>")
        for (i in 1..chapterTitles.size) {
            manifest.appendLine("<item id=\"c$i\" href=\"text/${chapterFile(i)}\" media-type=\"application/xhtml+xml\"/>")
            spine.appendLine("<itemref idref=\"c$i\"/>")
        }
        val coverMeta = if (cover != null) "<meta name=\"cover\" content=\"cover-image\"/>\n" else ""
        val description = if (novel.description.isNotBlank()) "<dc:description>${escapeXml(novel.description.trim())}</dc:description>\n" else ""
        return """<?xml version="1.0" encoding="UTF-8"?>
<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="bookid" xml:lang="$lang">
<metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
<dc:identifier id="bookid">${escapeXml(bookId)}</dc:identifier>
<dc:title>${escapeXml(novel.title)}</dc:title>
<dc:creator>${escapeXml(novel.author)}</dc:creator>
<dc:language>$lang</dc:language>
<dc:source>${escapeXml(novel.url)}</dc:source>
$description<meta property="dcterms:modified">${modified.truncatedTo(ChronoUnit.SECONDS)}</meta>
$coverMeta</metadata>
<manifest>
$manifest</manifest>
<spine toc="ncx">
$spine</spine>
</package>
"""
    }

    private companion object {
        const val CONTAINER = """<?xml version="1.0" encoding="UTF-8"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
<rootfiles>
<rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
</rootfiles>
</container>
"""
        const val STYLE = """body { margin: 0 4%; line-height: 1.5; }
h1 { font-size: 1.4em; margin: 1em 0; }
p { margin: 0 0 0.8em 0; }
table { border-collapse: collapse; margin: 1em 0; }
td, th { border: 1px solid #888; padding: 0.2em 0.5em; }
blockquote.author-note { margin: 1em 0; padding: 0.5em 1em; border-left: 3px solid #888; font-size: 0.9em; }
.titlepage { text-align: center; margin-top: 20%; }
.titlepage .author { font-style: italic; }
.titlepage .description { text-align: left; margin-top: 2em; }
.titlepage .source { font-size: 0.8em; margin-top: 2em; word-break: break-all; }
.cover { text-align: center; }
.cover img { max-width: 100%; max-height: 100%; }
"""
    }
}

/** A file name that is safe on Android and desktop file systems. */
fun epubFileName(title: String): String {
    val cleaned = title.replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), " ").replace(Regex("\\s+"), " ").trim().take(100)
    return cleaned.ifEmpty { "novel" } + ".epub"
}
