package com.romanarsiv.domain

/** Kaydedilecek EPUB'ın dosya adı kuralları (Android'den bağımsız; arayüz ve depolama ortak kullanır). */
object FileNames {
    const val KEPUB_SUFFIX = ".kepub.epub"
    private const val MAX_BASE_LENGTH = 120

    // Android'in ICU motoru POSIX sınıflarını (\p{Cntrl}) desteklemez: açık aralık.
    private val FORBIDDEN = Regex("[\\\\/:*?\"<>|\\u0000-\\u001F\\u007F]")
    private val SPACES = Regex("\\s+")

    /** Kitap adından önerilen ad: `Kitap Adı.kepub.epub`. */
    fun suggest(bookTitle: String): String = clean(bookTitle).ifEmpty { "Kitap" } + KEPUB_SUFFIX

    /**
     * Kullanıcının yazdığı adı güvenli hale getirir. `.epub` ile bitmiyorsa Kobo uyumlu `.kepub.epub`
     * eklenir; `.epub` ile bitiyorsa (ör. düz `Kitap.epub` isteniyorsa) olduğu gibi bırakılır.
     * Boşsa [fallbackTitle]'dan önerilir.
     */
    fun normalize(input: String, fallbackTitle: String): String {
        val trimmed = input.trim()
        val lower = trimmed.lowercase()
        val (base, suffix) = when {
            lower.endsWith(KEPUB_SUFFIX) -> trimmed.dropLast(KEPUB_SUFFIX.length) to KEPUB_SUFFIX
            lower.endsWith(".epub") -> trimmed.dropLast(".epub".length) to ".epub"
            else -> trimmed to KEPUB_SUFFIX
        }
        val cleaned = clean(base)
        return if (cleaned.isEmpty()) suggest(fallbackTitle) else cleaned + suffix
    }

    private fun clean(s: String): String = s
        .replace(FORBIDDEN, "_")
        .replace(SPACES, " ")
        .trim()
        .trim('.', ' ')
        .take(MAX_BASE_LENGTH)
        .trim()
}
