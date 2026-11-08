package com.romanarsiv.domain.repository

import com.romanarsiv.domain.model.NovelChapter
import com.romanarsiv.domain.model.NovelSource

interface NovelRepository {
    /** Roman adıyla arama yapar. [query] bir URL ise arama yapılmaz, o sayfa tek kaynak olarak döner. */
    suspend fun search(query: String): List<NovelSource>

    /**
     * Kaynağın bölümlerini sırayla kazır.
     * [onChapter] her bölüm indirildikten sonra çağrılır: 1 tabanlı sıra, toplam (dizin sayfasından
     * biliniyorsa; "sonraki" zincirinde 0) ve başlık.
     * İndirilemeyen bölümler atlanır; hiç bölüm çıkmazsa hata fırlatılır.
     */
    suspend fun fetchChapters(
        source: NovelSource,
        onChapter: suspend (index: Int, total: Int, title: String) -> Unit = { _, _, _ -> },
    ): ChapterFetchResult
}

data class ChapterFetchResult(
    val chapters: List<NovelChapter>,
    val skipped: Int,
)
