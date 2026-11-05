package com.yukselis.engine.i18n

import java.util.Locale

/**
 * Türkçe özel isimlere kesme işaretiyle hal eki ekler: Deniz'e, Kaya'nın, Aras'tan, Yankı'yla.
 * Büyük ve küçük ünlü uyumunu, kaynaştırma harflerini ve sertleşmeyi (d → t) uygular.
 */
object TurkishSuffix {
    private val TR = Locale.forLanguageTag("tr")
    private const val BACK = "aıou"
    private const val FRONT = "eiöü"
    private const val VOWELS = BACK + FRONT
    private const val VOICELESS = "fstkçşhp"

    enum class Case(val code: String) {
        DATIVE("e"),        // yönelme: Deniz'e
        ACCUSATIVE("i"),    // belirtme: Deniz'i
        GENITIVE("in"),     // tamlayan: Deniz'in
        LOCATIVE("de"),     // bulunma: Deniz'de
        ABLATIVE("den"),    // ayrılma: Deniz'den
        INSTRUMENTAL("le"); // vasıta: Deniz'le

        companion object {
            fun fromCode(code: String): Case? = entries.firstOrNull { it.code == code }
        }
    }

    fun apply(word: String, case: Case): String {
        if (word.isBlank()) return word
        val lower = word.trim().lowercase(TR)
        val last = lower.last()
        val lastVowel = lower.lastOrNull { it in VOWELS } ?: 'e'
        val endsWithVowel = last in VOWELS
        val two = if (lastVowel in BACK) "a" else "e"
        val four = when (lastVowel) {
            'a', 'ı' -> "ı"
            'e', 'i' -> "i"
            'o', 'u' -> "u"
            else -> "ü"
        }
        val d = if (last in VOICELESS) "t" else "d"
        val suffix = when (case) {
            Case.DATIVE -> if (endsWithVowel) "y$two" else two
            Case.ACCUSATIVE -> if (endsWithVowel) "y$four" else four
            Case.GENITIVE -> if (endsWithVowel) "n${four}n" else "${four}n"
            Case.LOCATIVE -> d + two
            Case.ABLATIVE -> d + two + "n"
            Case.INSTRUMENTAL -> (if (endsWithVowel) "yl" else "l") + two
        }
        return "${word.trim()}'$suffix"
    }
}
