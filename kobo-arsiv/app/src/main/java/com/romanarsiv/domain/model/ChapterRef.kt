package com.romanarsiv.domain.model

/**
 * Tespit edilmiş ama (çoğunlukla) henüz indirilmemiş bir bölüm; seçim listesinde gösterilir.
 * "Sonraki" zincirinde bölüm metni tespit sırasında zaten indirildiği için [preloaded] doludur.
 */
data class ChapterRef(
    val url: String,
    val title: String,
    val preloaded: NovelChapter? = null,
)
