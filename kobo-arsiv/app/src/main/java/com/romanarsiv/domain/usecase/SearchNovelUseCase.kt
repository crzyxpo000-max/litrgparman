package com.romanarsiv.domain.usecase

import com.romanarsiv.domain.NovelSiteFilter
import com.romanarsiv.domain.model.NovelSource
import com.romanarsiv.domain.repository.NovelRepository

/** [novelSites] gösterilir; [hidden] roman sitesi sayılmayıp gizlenenlerdir (kullanıcı isterse görür). */
data class SearchResults(
    val novelSites: List<NovelSource>,
    val hidden: List<NovelSource>,
)

class SearchNovelUseCase(private val repository: NovelRepository) {
    suspend operator fun invoke(query: String): SearchResults {
        val trimmed = query.trim()
        require(trimmed.isNotEmpty()) { "Arama metni boş olamaz." }
        val results = repository.search(trimmed)
        // Doğrudan URL yapıştırıldıysa kullanıcı siteyi zaten seçmiştir: süzme.
        if (isUrl(trimmed)) return SearchResults(results, emptyList())

        val (novel, other) = results.partition { NovelSiteFilter.isNovelSite(it) }
        // Puanı yüksek olan (bilinen roman siteleri) üstte; eşitlikte arama motorunun sırası korunur.
        return SearchResults(novel.sortedByDescending { NovelSiteFilter.score(it) }, other)
    }

    private fun isUrl(text: String) = text.startsWith("http://", ignoreCase = true) || text.startsWith("https://", ignoreCase = true)
}
