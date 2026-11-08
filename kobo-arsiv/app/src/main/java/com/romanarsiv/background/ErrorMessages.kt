package com.romanarsiv.background

import android.content.Context
import com.romanarsiv.R
import com.romanarsiv.data.remote.ChallengeDeclinedException
import com.romanarsiv.data.remote.ChallengeRequiredException
import com.romanarsiv.data.remote.UrlUtils
import com.romanarsiv.domain.AppException
import org.jsoup.HttpStatusException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/** Kullanıcıya gösterilecek hata metni, uygulamanın seçili dilinde ([context] yerelleştirilmiş olmalı). */
fun describeError(context: Context, e: Throwable): String = when (e) {
    is UnknownHostException -> context.getString(R.string.err_no_internet)
    is SocketTimeoutException -> context.getString(R.string.err_timeout)
    is SSLException -> context.getString(R.string.err_ssl)
    is HttpStatusException -> {
        val host = UrlUtils.hostOf(e.url) ?: e.url
        if (e.statusCode == 403 || e.statusCode == 503) {
            context.getString(R.string.err_http_blocked, host, e.statusCode)
        } else {
            context.getString(R.string.err_http, host, e.statusCode)
        }
    }
    is ChallengeRequiredException -> context.getString(R.string.err_challenge_required, UrlUtils.hostOf(e.url) ?: e.url)
    is ChallengeDeclinedException -> context.getString(R.string.err_challenge_declined, e.host)
    is AppException -> when (e.reason) {
        AppException.Reason.SEARCH_FAILED -> context.getString(R.string.err_search_failed, e.detail.orEmpty())
        AppException.Reason.NO_CHAPTERS_FOUND -> context.getString(R.string.err_no_chapters)
        AppException.Reason.NOTHING_DOWNLOADED -> context.getString(R.string.err_nothing_downloaded)
        AppException.Reason.FILE_CREATE_FAILED -> context.getString(R.string.err_file_create)
        AppException.Reason.FILE_WRITE_FAILED -> context.getString(R.string.err_file_write)
        AppException.Reason.PAGE_FAILED -> context.getString(R.string.err_page_failed, e.detail.orEmpty())
        AppException.Reason.PAGE_TIMEOUT -> context.getString(R.string.err_page_timeout, e.detail.orEmpty())
    }
    is OutOfMemoryError -> context.getString(R.string.err_memory)
    else -> e.message?.takeIf { it.isNotBlank() }?.let { "$it (${e.javaClass.simpleName})" } ?: e.javaClass.simpleName
}
