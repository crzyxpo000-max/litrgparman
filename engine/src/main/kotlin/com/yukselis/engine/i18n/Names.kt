package com.yukselis.engine.i18n

/**
 * Kişi ve mekan adları dile göre değişir (Deniz Arman ↔ Devon Hale, Kıyıkent ↔ Port Harlow).
 * İçerik metinleri isim yazmaz; {npc:id} ve {yer:id} belirteçleri kullanır.
 */
data class NameEntry(
    val id: String,
    val kind: Kind,
    val full: LText,
    val short: LText,
    /** İngilizce zamir: he, she, they, it (DECISIONS #10: Deniz "they"). */
    val pronoun: String = "they",
    /** Türkçe ad iyelik ekli tamlamayla bitiyor: hal eki n'li gelir (Kule Dibi'ne). */
    val possessive: Boolean = false,
) {
    enum class Kind(val tokenKey: String) {
        NPC("npc"),
        PLACE("yer"),
        FACTION("fraksiyon"),
    }
}

class NameRegistry(entries: Collection<NameEntry>) {
    private val byKey = entries.associateBy { it.kind to it.id }

    val all: List<NameEntry> = entries.toList()

    fun find(kind: NameEntry.Kind, id: String): NameEntry? = byKey[kind to id]
}

/**
 * Metindeki belirteçleri seçili dile göre çözer.
 *
 * Söz dizimi:
 *  - {npc:deniz_arman}        → kısa ad (Deniz / Devon)
 *  - {npc:deniz_arman|tam}    → tam ad (Deniz Arman / Devon Hale)
 *  - {npc:deniz_arman|e}      → Türkçe hal eki (Deniz'e); İngilizcede yok sayılır
 *  - {npc:deniz_arman|pos}    → İngilizce iyelik (Devon's); Türkçede yok sayılır
 *  - {yer:kiyikent}           → mekan adı
 *  - {oyuncu} / {oyuncu|in}   → oyuncunun adı
 *  - {zamir:aras_kurt|o}      → İngilizce zamir (he/she/they/it); kipler: o (özne), onu (nesne), onun (iyelik),
 *                               kendi (dönüşlü), b (büyük harf), ve fiil uyumu s (says/say), is (is/are),
 *                               has (has/have), does (does/do), was (was/were). Türkçede boş döner (Türkçe zamir düşer).
 * Ekler zincirlenebilir: {npc:deniz_arman|tam|e} → Deniz Arman'a
 */
class TokenResolver(private val names: NameRegistry) {

    fun resolve(text: String, lang: Lang, playerName: String): String =
        TOKEN.replace(text) { match ->
            val kindKey = match.groupValues[1]
            val id = match.groupValues[2]
            val mods = match.groupValues[3].split('|').filter { it.isNotEmpty() }
            val wantsFull = "tam" in mods || "full" in mods
            if (kindKey == PRONOUN) {
                val entry = names.find(NameEntry.Kind.NPC, id) ?: return@replace "[?zamir:$id]"
                return@replace if (lang == Lang.TR) "" else EnglishGrammar.pronoun(entry.pronoun, mods)
            }
            val base = if (kindKey == PLAYER) {
                playerName
            } else {
                val entry = lookup(kindKey, id) ?: return@replace "[?$kindKey:$id]"
                if (wantsFull) entry.full[lang] else entry.short[lang]
            }
            val possessive = kindKey != PLAYER && lookup(kindKey, id)?.possessive == true
            applyModifiers(base, mods, lang, possessive)
        }

    /** Çözülemeyen belirteçleri döndürür; içerik doğrulayıcı kullanır. */
    fun unresolvedTokens(text: String): List<String> =
        TOKEN.findAll(text)
            .filter { m ->
                val kindKey = m.groupValues[1]
                // Kimliksiz "{npc}" bir Sistem metni yer tutucusudur (formatL ile doldurulur), belirteç değil.
                kindKey != PLAYER && m.groupValues[2].isNotEmpty() && lookup(kindKey, m.groupValues[2]) == null ||
                    kindKey != PLAYER && m.groupValues[2].isEmpty() && m.groupValues[3].isNotEmpty()
            }
            .map { it.value }
            .toList()

    private fun lookup(kindKey: String, id: String): NameEntry? {
        if (kindKey == PRONOUN) return names.find(NameEntry.Kind.NPC, id)
        val kind = NameEntry.Kind.entries.firstOrNull { it.tokenKey == kindKey } ?: return null
        return names.find(kind, id)
    }

    private fun applyModifiers(base: String, mods: List<String>, lang: Lang, possessive: Boolean = false): String {
        var result = base
        for (mod in mods) {
            result = when (lang) {
                Lang.TR -> TurkishSuffix.Case.fromCode(mod)?.let { TurkishSuffix.apply(result, it, possessive) } ?: result
                // "b": cümle başında büyük harf ("the Esplanade" → "The Esplanade").
                Lang.EN -> when (mod) {
                    "pos" -> EnglishGrammar.possessive(result)
                    "b" -> result.replaceFirstChar { it.uppercase() }
                    // "k": cümle içinde artikel küçük ("The Quiet Network" → "the Quiet Network").
                    "k" -> if (result.startsWith("The ")) "the " + result.substring(4) else result
                    else -> result
                }
            }
        }
        return result
    }

    companion object {
        private const val PLAYER = "oyuncu"
        private const val PRONOUN = "zamir"
        val TOKEN = Regex("""\{(npc|yer|fraksiyon|oyuncu|zamir)(?::([a-z0-9_]+))?((?:\|[a-z]+)*)\}""")
    }
}
