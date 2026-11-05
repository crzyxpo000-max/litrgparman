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
) {
    enum class Kind(val tokenKey: String) {
        NPC("npc"),
        PLACE("yer"),
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
 * Ekler zincirlenebilir: {npc:deniz_arman|tam|e} → Deniz Arman'a
 */
class TokenResolver(private val names: NameRegistry) {

    fun resolve(text: String, lang: Lang, playerName: String): String =
        TOKEN.replace(text) { match ->
            val kindKey = match.groupValues[1]
            val id = match.groupValues[2]
            val mods = match.groupValues[3].split('|').filter { it.isNotEmpty() }
            val wantsFull = "tam" in mods || "full" in mods
            val base = if (kindKey == PLAYER) {
                playerName
            } else {
                val entry = lookup(kindKey, id) ?: return@replace "[?$kindKey:$id]"
                if (wantsFull) entry.full[lang] else entry.short[lang]
            }
            applyModifiers(base, mods, lang)
        }

    /** Çözülemeyen belirteçleri döndürür; içerik doğrulayıcı kullanır. */
    fun unresolvedTokens(text: String): List<String> =
        TOKEN.findAll(text)
            .filter { m ->
                val kindKey = m.groupValues[1]
                kindKey != PLAYER && lookup(kindKey, m.groupValues[2]) == null
            }
            .map { it.value }
            .toList()

    private fun lookup(kindKey: String, id: String): NameEntry? {
        val kind = NameEntry.Kind.entries.firstOrNull { it.tokenKey == kindKey } ?: return null
        return names.find(kind, id)
    }

    private fun applyModifiers(base: String, mods: List<String>, lang: Lang): String {
        var result = base
        for (mod in mods) {
            result = when (lang) {
                Lang.TR -> TurkishSuffix.Case.fromCode(mod)?.let { TurkishSuffix.apply(result, it) } ?: result
                Lang.EN -> if (mod == "pos") EnglishGrammar.possessive(result) else result
            }
        }
        return result
    }

    companion object {
        private const val PLAYER = "oyuncu"
        val TOKEN = Regex("""\{(npc|yer|oyuncu)(?::([a-z0-9_]+))?((?:\|[a-z]+)*)\}""")
    }
}
