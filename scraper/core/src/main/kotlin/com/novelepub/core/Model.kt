package com.novelepub.core

/** A chapter link found on a novel's table of contents. */
data class ChapterRef(
    val title: String,
    val url: String,
)

/** A downloaded chapter. [html] is an XHTML-safe body fragment produced by [ContentCleaner]. */
data class Chapter(
    val title: String,
    val url: String,
    val html: String,
    /** Link to the following chapter, used when a source is crawled chapter by chapter. */
    val nextUrl: String? = null,
)

/** What a source knows about a novel before any chapter is downloaded. */
data class NovelInfo(
    val title: String,
    val author: String,
    val url: String,
    val description: String = "",
    val coverUrl: String? = null,
    /** BCP 47 tag written to the EPUB, e.g. "en" or "tr". */
    val language: String = "en",
    /** The table of contents. Empty when [crawlFrom] is set. */
    val chapters: List<ChapterRef> = emptyList(),
    /** Set when the chapter list is unknown and chapters are found by following "next" links from this URL. */
    val crawlFrom: String? = null,
) {
    val isCrawl: Boolean get() = crawlFrom != null
}
