package com.romanarsiv.domain.usecase

import com.romanarsiv.domain.model.ChapterRef
import com.romanarsiv.domain.model.NovelSource
import com.romanarsiv.domain.repository.NovelRepository

/** Seçim ekranı için kaynağın bölüm listesini çıkarır. */
class DetectChaptersUseCase(private val repository: NovelRepository) {
    suspend operator fun invoke(
        source: NovelSource,
        onFound: suspend (count: Int) -> Unit = {},
    ): List<ChapterRef> = repository.listChapters(source, onFound)
}
