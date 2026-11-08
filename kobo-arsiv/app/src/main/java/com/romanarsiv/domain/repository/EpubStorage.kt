package com.romanarsiv.domain.repository

/** EPUB baytlarını kalıcı bir yere yazar; Android tarafında MediaStore ile uygulanır. */
interface EpubStorage {
    /** Dosyayı kaydeder ve kullanıcıya gösterilecek konumu (ör. "Download/Kitap.kepub.epub") döndürür. */
    suspend fun save(bookTitle: String, bytes: ByteArray): SavedFile
}

data class SavedFile(val fileName: String, val location: String)
