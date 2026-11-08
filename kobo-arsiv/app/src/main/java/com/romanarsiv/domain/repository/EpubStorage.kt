package com.romanarsiv.domain.repository

/** EPUB baytlarını kalıcı bir yere yazar; Android tarafında MediaStore ile uygulanır. */
interface EpubStorage {
    /**
     * [fileName] adıyla kaydeder (önceden [com.romanarsiv.domain.FileNames.normalize] ile hazırlanmış)
     * ve gerçek adı ile kullanıcıya gösterilecek konumu döndürür (aynı ad varsa sistem "(1)" ekleyebilir).
     */
    suspend fun save(fileName: String, bytes: ByteArray): SavedFile
}

/** [uri] dosyanın açılıp paylaşılabileceği içerik adresidir (Android: MediaStore); bilinmiyorsa null. */
data class SavedFile(val fileName: String, val location: String, val uri: String? = null)
