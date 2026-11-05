package com.yukselis.engine.i18n

/** İngilizce için küçük dilbilgisi yardımcıları: a/an, çoğul, iyelik. */
object EnglishGrammar {
    private val anPrefixes = listOf("hour", "honest", "honor", "heir")
    private val aPrefixes = listOf("uni", "use", "user", "one", "once", "euro", "ewe")

    fun article(word: String): String {
        val w = word.trim()
        if (w.isEmpty()) return w
        val lower = w.lowercase()
        val an = when {
            anPrefixes.any { lower.startsWith(it) } -> true
            aPrefixes.any { lower.startsWith(it) } -> false
            else -> lower.first() in "aeiou"
        }
        return (if (an) "an " else "a ") + w
    }

    fun plural(word: String, count: Int): String {
        if (count == 1) return word
        val lower = word.lowercase()
        return when {
            lower.length > 1 && lower.endsWith("y") && lower[lower.length - 2] !in "aeiou" -> word.dropLast(1) + "ies"
            lower.endsWith("s") || lower.endsWith("x") || lower.endsWith("ch") || lower.endsWith("sh") -> word + "es"
            else -> word + "s"
        }
    }

    /** Devon → Devon's, Miles → Miles's (Chicago Manual of Style). */
    fun possessive(name: String): String = "$name's"
}
