package com.romanarsiv.domain

import java.io.IOException

/**
 * Kullanıcıya gösterilecek, dile bağlı olmayan hata. Metin arayüzde [reason]'a göre seçilir
 * (background/ErrorMessages.kt), böylece Türkçe ve İngilizce aynı hatayı kendi dilinde gösterir.
 * [detail] isteğe bağlı ek bilgidir (alan adı, alt hata); mesaja olduğu gibi eklenir.
 */
class AppException(
    val reason: Reason,
    val detail: String? = null,
    cause: Throwable? = null,
) : IOException(reason.name + (detail?.let { ": $it" } ?: ""), cause) {

    enum class Reason {
        SEARCH_FAILED,
        NO_CHAPTERS_FOUND,
        NOTHING_DOWNLOADED,
        FILE_CREATE_FAILED,
        FILE_WRITE_FAILED,
        PAGE_FAILED,
        PAGE_TIMEOUT,
    }
}
