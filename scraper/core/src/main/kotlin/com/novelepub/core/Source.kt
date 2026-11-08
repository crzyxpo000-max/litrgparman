package com.novelepub.core

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** A site the app knows how to read. */
interface NovelSource {
    val id: String
    val name: String

    /** Reads the novel's page (title, author, cover, table of contents). */
    suspend fun fetchNovel(url: String, fetcher: Fetcher): NovelInfo

    /** Downloads one chapter and returns its cleaned content. */
    suspend fun fetchChapter(ref: ChapterRef, fetcher: Fetcher): Chapter
}

/** A source with a dedicated parser for one site. */
interface SiteSource : NovelSource {
    val hosts: List<String>

    fun matches(url: String): Boolean {
        val host = url.toHttpUrlOrNull()?.host ?: return false
        return hosts.any { host == it || host.endsWith(".$it") }
    }
}

object Sources {
    /** Sites with a dedicated parser. Anything else uses [GenericSource]. */
    fun sites(includeAuthorNotes: Boolean = false): List<SiteSource> = listOf(
        RoyalRoadSource(includeAuthorNotes),
    )

    fun forUrl(url: String, includeAuthorNotes: Boolean = false): SiteSource? =
        sites(includeAuthorNotes).firstOrNull { it.matches(url) }

    /** Adds https:// when the scheme is missing and checks that the result is an http(s) URL. */
    fun normalizeUrl(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        val withScheme = if (Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://").containsMatchIn(trimmed)) trimmed else "https://$trimmed"
        val url = withScheme.toHttpUrlOrNull() ?: return null
        return url.toString()
    }
}
