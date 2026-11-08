package com.romanarsiv.domain.model

/** Aramada bulunan, romanın kopyasını barındıran bir sayfa. */
data class NovelSource(
    val url: String,
    /** Sitenin kısa adı (alan adı, `www.` olmadan). */
    val siteName: String,
    /** Sayfanın başlığı (arama sonucundaki ya da `<title>`). */
    val title: String,
    /** Arama motorunun verdiği kısa özet; olmayabilir. */
    val snippet: String = "",
)
