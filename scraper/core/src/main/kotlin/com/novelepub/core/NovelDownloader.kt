package com.novelepub.core

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.OutputStream
import java.security.MessageDigest

/** Stores downloaded chapters so an interrupted download resumes where it stopped. */
interface ChapterCache {
    fun get(url: String): Chapter?
    fun put(chapter: Chapter)
}

class MemoryChapterCache : ChapterCache {
    private val map = HashMap<String, Chapter>()
    override fun get(url: String) = map[url]
    override fun put(chapter: Chapter) { map[chapter.url] = chapter }
}

/** One JSON file per chapter, named by the SHA-1 of its URL. */
class FileChapterCache(private val dir: File) : ChapterCache {
    init { dir.mkdirs() }

    override fun get(url: String): Chapter? {
        val file = fileFor(url)
        if (!file.isFile) return null
        return runCatching {
            val obj = Json.parseToJsonElement(file.readText()).jsonObject
            Chapter(
                title = obj.getValue("title").jsonPrimitive.content,
                url = obj.getValue("url").jsonPrimitive.content,
                html = obj.getValue("html").jsonPrimitive.content,
                nextUrl = obj["next"]?.jsonPrimitive?.content?.takeIf { it.isNotEmpty() },
            )
        }.getOrNull()
    }

    override fun put(chapter: Chapter) {
        val obj = JsonObject(
            mapOf(
                "title" to JsonPrimitive(chapter.title),
                "url" to JsonPrimitive(chapter.url),
                "html" to JsonPrimitive(chapter.html),
                "next" to JsonPrimitive(chapter.nextUrl.orEmpty()),
            ),
        )
        val tmp = File(dir, fileFor(chapter.url).name + ".tmp")
        tmp.writeText(obj.toString())
        tmp.renameTo(fileFor(chapter.url))
    }

    fun clear() { dir.listFiles()?.forEach { it.delete() } }

    private fun fileFor(url: String): File {
        val hash = MessageDigest.getInstance("SHA-1").digest(url.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return File(dir, "$hash.json")
    }
}

data class Progress(
    val done: Int,
    /** Null while crawling, when the total is not known. */
    val total: Int?,
    val currentTitle: String,
    val fromCache: Boolean,
)

/**
 * Downloads chapters politely (one at a time, [delayMs] between network requests), caching each,
 * and writes the EPUB. Cancelling the coroutine stops between chapters; already downloaded
 * chapters stay in the cache.
 */
class NovelDownloader(
    private val fetcher: Fetcher,
    private val cache: ChapterCache,
    private val delayMs: Long = 1_000,
) {
    /** Downloads [chapters] (a slice of the table of contents) and returns them in order. */
    suspend fun downloadList(
        source: NovelSource,
        chapters: List<ChapterRef>,
        onProgress: (Progress) -> Unit = {},
    ): List<ChapterRef> {
        var lastWasNetwork = false
        chapters.forEachIndexed { i, ref ->
            currentCoroutineContext().ensureActive()
            val cached = cache.get(ref.url)
            if (cached == null) {
                if (lastWasNetwork) delay(delayMs)
                val chapter = source.fetchChapter(ref, fetcher)
                // Keyed by the TOC URL, which is what the next run looks up.
                cache.put(chapter.copy(url = ref.url))
                lastWasNetwork = true
            } else {
                lastWasNetwork = false
            }
            onProgress(Progress(i + 1, chapters.size, cached?.title ?: ref.title, fromCache = cached != null))
        }
        return chapters
    }

    /**
     * Follows "next" links from [startUrl]. Stops at the last chapter, at a link already visited,
     * or after [maxChapters]. Chapters already in the cache are not downloaded again.
     */
    suspend fun crawl(
        source: NovelSource,
        startUrl: String,
        maxChapters: Int,
        onProgress: (Progress) -> Unit = {},
    ): List<ChapterRef> {
        val visited = HashSet<String>()
        val result = ArrayList<ChapterRef>()
        var url: String? = startUrl
        var lastWasNetwork = false
        while (url != null && result.size < maxChapters && visited.add(normalize(url))) {
            currentCoroutineContext().ensureActive()
            var chapter = cache.get(url)
            val fromCache = chapter != null
            if (chapter == null) {
                if (lastWasNetwork) delay(delayMs)
                chapter = source.fetchChapter(ChapterRef("Chapter ${result.size + 1}", url), fetcher).copy(url = url)
                cache.put(chapter)
                lastWasNetwork = true
            } else {
                lastWasNetwork = false
            }
            result += ChapterRef(chapter.title, url)
            onProgress(Progress(result.size, null, chapter.title, fromCache))
            url = chapter.nextUrl
        }
        return result
    }

    /** Writes the EPUB from cached chapters. Every chapter in [chapters] must have been downloaded. */
    fun writeEpub(novel: NovelInfo, chapters: List<ChapterRef>, cover: Cover?, out: OutputStream) {
        EpubWriter(out, novel, cover).use { writer ->
            for (ref in chapters) {
                val chapter = cache.get(ref.url) ?: throw IllegalStateException("Chapter missing from cache: ${ref.url}")
                writer.addChapter(chapter)
            }
        }
    }

    /** Downloads the cover image, or returns null if it is missing or not a supported image. */
    suspend fun fetchCover(url: String?): Cover? {
        if (url.isNullOrBlank()) return null
        return try {
            Cover.detect(fetcher.get(url).body)
        } catch (e: java.io.IOException) {
            null // A missing cover should not fail the whole book.
        }
    }

    private fun normalize(url: String) = url.substringBefore('#').trimEnd('/')
}
