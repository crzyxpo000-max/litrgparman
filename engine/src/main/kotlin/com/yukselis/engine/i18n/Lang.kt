package com.yukselis.engine.i18n

/** Oyunun desteklediği diller. Kayıt dosyası dilden bağımsızdır. */
enum class Lang(val code: String) {
    TR("tr"),
    EN("en");

    companion object {
        /** "tr", "tr-TR", "en_US" gibi kodları çözer; bilinmeyen her şey İngilizceye düşer. */
        fun fromCode(code: String?): Lang {
            val prefix = code?.lowercase()?.take(2)
            return entries.firstOrNull { it.code == prefix } ?: EN
        }
    }
}

/**
 * Oyuncuya görünen her metin iki dilde tutulur.
 * Eksik bir çeviri diğer dile düşer; ContentValidator eksikleri ayrıca raporlar.
 */
data class LText(val tr: String, val en: String) {
    operator fun get(lang: Lang): String = when (lang) {
        Lang.TR -> tr.ifBlank { en }
        Lang.EN -> en.ifBlank { tr }
    }

    val isComplete: Boolean get() = tr.isNotBlank() && en.isNotBlank()

    companion object {
        val EMPTY = LText("", "")
    }
}
