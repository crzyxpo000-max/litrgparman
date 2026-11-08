package com.romanarsiv.domain.repository

import com.romanarsiv.domain.model.ChapterRef
import com.romanarsiv.domain.model.NovelChapter
import com.romanarsiv.domain.model.NovelSource

interface NovelRepository {
    /** Roman adıyla arama yapar. [query] bir URL ise arama yapılmaz, o sayfa tek kaynak olarak döner. */
    suspend fun search(query: String): List<NovelSource>

    /**
     * Kaynağın bölümlerini okuma sırasıyla tespit eder (seçim listesi için).
     * Dizin sayfasında yalnızca bağlantılar toplanır; "sonraki" zincirinde bölümler tespit sırasında
     * indirilir ([ChapterRef.preloaded]). [onFound] bulunan bölüm sayısı her arttığında çağrılır.
     * Hiç bölüm bulunamazsa hata fırlatır.
     */
    suspend fun listChapters(
        source: NovelSource,
        onFound: suspend (count: Int) -> Unit = {},
    ): List<ChapterRef>

    /**
     * Tek bir bölümü indirir. Okunabilir metin yoksa ya da sayfa alınamadıysa null (atlanır).
     * [position] 1 tabanlı sıra; başlık bulunamazsa "Bölüm [position]" kullanılır.
     */
    suspend fun fetchChapter(ref: ChapterRef, position: Int): NovelChapter?
}
