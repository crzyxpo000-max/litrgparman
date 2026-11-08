package com.romanarsiv.presentation.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.romanarsiv.data.storage.FileHelper

/** İndirilen EPUB'ı açma, paylaşma ve silme (MediaStore içerik adresiyle). */
object FileActions {

    /** Bir okuyucu uygulamayla açar; açacak uygulama yoksa false. */
    fun open(context: Context, uri: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri.toUri(), FileHelper.EPUB_MIME)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    fun share(context: Context, uri: String, title: String): Boolean {
        val send = Intent(Intent.ACTION_SEND)
            .setType(FileHelper.EPUB_MIME)
            .putExtra(Intent.EXTRA_STREAM, uri.toUri())
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return try {
            context.startActivity(Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    /** Dosyayı İndirilenler'den siler. Uygulamanın kendi yazdığı dosyalar izin istemeden silinir. */
    fun delete(context: Context, uri: String): Boolean = try {
        context.contentResolver.delete(uri.toUri(), null, null) > 0
    } catch (_: SecurityException) {
        false
    } catch (_: IllegalArgumentException) {
        false
    }
}
