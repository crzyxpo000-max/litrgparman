package com.romanarsiv.domain

object AppConfig {
    /**
     * EPUB'ların `dc:creator` alanına yazılan sabit yazar adı.
     * Kendi adınızla değiştirin.
     */
    const val AUTHOR_NAME = "Yazar Adı"

    /** EPUB dil kodu (Kobo'da tireleme ve sözlük seçimi buna göre yapılır). */
    const val BOOK_LANGUAGE = "tr"

    /** Tek seferde indirilebilecek en fazla bölüm; sonsuz sayfalama döngülerine karşı sigorta. */
    const val MAX_CHAPTERS = 3000

    /** Aynı siteye art arda iki istek arasındaki bekleme (ms). Sunucuyu yormamak için. */
    const val REQUEST_DELAY_MS = 600L
}
