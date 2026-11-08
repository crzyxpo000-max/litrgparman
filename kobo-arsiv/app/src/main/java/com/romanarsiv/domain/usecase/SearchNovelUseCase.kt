package com.romanarsiv.domain.usecase

import com.romanarsiv.domain.model.NovelSource
import com.romanarsiv.domain.repository.NovelRepository

class SearchNovelUseCase(private val repository: NovelRepository) {
    suspend operator fun invoke(query: String): List<NovelSource> {
        val trimmed = query.trim()
        require(trimmed.isNotEmpty()) { "Arama metni boş olamaz." }
        return repository.search(trimmed)
    }
}
