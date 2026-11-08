package com.romanarsiv.data.storage

import com.romanarsiv.domain.AppException
import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import com.romanarsiv.domain.repository.EpubStorage
import com.romanarsiv.domain.repository.SavedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * EPUB'ı MediaStore ile `Download/` klasörüne verilen adla yazar (API 29+, izin gerekmez).
 * Ad kuralları (`.kepub.epub`, yasak karakterler) [com.romanarsiv.domain.FileNames]'tedir.
 * Yazma sürerken dosya `IS_PENDING` ile gizli tutulur; yarım dosya başka uygulamalara görünmez.
 */
class FileHelper(private val context: Context) : EpubStorage {

    override suspend fun save(fileName: String, bytes: ByteArray): SavedFile = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, EPUB_MIME)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val uri = resolver.insert(collection, values) ?: throw AppException(AppException.Reason.FILE_CREATE_FAILED)
        try {
            resolver.openOutputStream(uri, "w")?.use { it.write(bytes) }
                ?: throw AppException(AppException.Reason.FILE_WRITE_FAILED)
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }

        // Aynı adlı dosya varsa sistem "Ad (1).kepub.epub" gibi yeni bir ad verir; gerçek adı oku.
        val actualName = resolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        } ?: fileName

        SavedFile(fileName = actualName, location = "${Environment.DIRECTORY_DOWNLOADS}/$actualName", uri = uri.toString())
    }

    companion object {
        const val EPUB_MIME = "application/epub+zip"
    }
}
