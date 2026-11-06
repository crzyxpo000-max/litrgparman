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

    /** Zamir ve fiil uyumu: `p` he/she/they/it; kipler bkz. TokenResolver. */
    fun pronoun(p: String, mods: List<String>): String {
        val plural = p == "they"
        val m = mods.firstOrNull { it != "b" } ?: "o"
        val word = when (m) {
            "o" -> p
            "onu" -> when (p) { "he" -> "him"; "she" -> "her"; "it" -> "it"; else -> "them" }
            "onun" -> when (p) { "he" -> "his"; "she" -> "her"; "it" -> "its"; else -> "their" }
            "kendi" -> when (p) { "he" -> "himself"; "she" -> "herself"; "it" -> "itself"; else -> "themselves" }
            "s" -> if (plural) "" else "s"
            "is" -> if (plural) "are" else "is"
            "has" -> if (plural) "have" else "has"
            "does" -> if (plural) "do" else "does"
            "was" -> if (plural) "were" else "was"
            else -> p
        }
        return if ("b" in mods) word.replaceFirstChar { it.uppercase() } else word
    }
}
