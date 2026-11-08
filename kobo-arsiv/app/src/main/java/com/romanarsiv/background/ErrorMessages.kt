package com.romanarsiv.background

import com.romanarsiv.data.remote.ChallengeDeclinedException
import com.romanarsiv.data.remote.ChallengeRequiredException
import com.romanarsiv.data.remote.UrlUtils
import org.jsoup.HttpStatusException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/** Kullanıcıya gösterilecek Türkçe hata metni. */
fun describeError(e: Throwable): String = when (e) {
    is UnknownHostException -> "İnternet bağlantısı yok ya da site bulunamadı."
    is SocketTimeoutException -> "Sunucu zamanında yanıt vermedi."
    is SSLException -> "Güvenli bağlantı kurulamadı."
    is HttpStatusException -> {
        val host = UrlUtils.hostOf(e.url) ?: e.url
        if (e.statusCode == 403 || e.statusCode == 503) {
            "$host erişimi reddetti (HTTP ${e.statusCode}). Site otomatik erişimi engelliyor olabilir."
        } else {
            "$host HTTP ${e.statusCode} hatası döndürdü."
        }
    }
    is ChallengeRequiredException -> "${e.message}. Doğrulama sayfası geçilemedi."
    is ChallengeDeclinedException -> e.message ?: "Doğrulama tamamlanmadı."
    is OutOfMemoryError -> "Bellek yetmedi; kitap çok büyük olabilir."
    else -> e.message?.takeIf { it.isNotBlank() }?.let { "$it (${e.javaClass.simpleName})" } ?: e.javaClass.simpleName
}
