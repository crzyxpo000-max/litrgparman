package com.romanarsiv.domain.repository

/** EPUB baytlarını kalıcı bir yere yazar; Android tarafında MediaStore ile uygulanır. */
interface EpubStorage {
    /**
     * [fileName] adıyla kaydeder (önceden [com.romanarsiv.domain.FileNames.normalize] ile hazırlanmış)
     * ve gerçek adı ile kullanıcıya gösterilecek konumu döndürür (aynı ad varsa sistem "(1)" ekleyebilir).
     */
    suspend fun save(fileName: String, bytes: ByteArray): SavedFile
}

data class SavedFile(val fileName: String, val location: String)
