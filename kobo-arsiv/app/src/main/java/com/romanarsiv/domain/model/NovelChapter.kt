package com.romanarsiv.domain.model

/**
 * Kazınmış bir bölüm.
 *
 * [contentHtml] temizlenmiş bir HTML parçasıdır: yalnızca `p`, `br`, `em`, `strong`, `i`, `b`, `u`,
 * `h2`–`h4`, `blockquote`, `hr` etiketleri. EPUB üreticisi bunu XHTML'e dönüştürür.
 */
data class NovelChapter(
    val title: String,
    val contentHtml: String,
    val sourceUrl: String = "",
)
