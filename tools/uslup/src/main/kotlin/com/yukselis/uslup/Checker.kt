package com.yukselis.uslup

import com.yukselis.engine.content.YamlTree
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale

/**
 * Türkçe üslup elden geçirmesinin bekçisi (docs/USLUP.md §7.1).
 *
 * Bir içerik dosyasının eski (git) ve yeni sürümünü karşılaştırır: yalnızca `tr` değerleri değişebilir.
 * Yapı, `en` ve diğer her değer, yorumlar ve satır düzeni aynı kalmalı; `tr` içindeki belirteçler (ek hariç)
 * ve bozuk Yankı yazısı korunmalı; Yankı'nın küçük harfli fısıltısı büyük harfe dönmemeli. Bunlar **hata**dır.
 * Uzunluk sapması ve üslup ölçümleri **uyarı**dır.
 */
object Checker {
    private val TR = Locale.forLanguageTag("tr")

    data class Finding(val error: Boolean, val where: String, val message: String, val text: String? = null)

    /** Bir dosyanın karşılaştırma sonucu. [styleOld]/[styleNew]: tüm `tr` değerleri üzerinde üslup sayımları. */
    data class FileReport(
        val file: String,
        val changed: Int,
        val total: Int,
        val findings: List<Finding>,
        val styleOld: Map<StyleRule, Int>,
        val styleNew: Map<StyleRule, Int>,
    ) {
        val errors get() = findings.count { it.error }
        val warnings get() = findings.count { !it.error }
    }

    // ------------------------------------------------------------------ üslup kuralları

    enum class StyleRule(val label: String) {
        SEMICOLON("noktalı virgül >1"),
        YOR_RUN("art arda 3 -yor"),
        SHORT_RUN("art arda kısa cümle"),
        BU_COMMA("\"Bu, \" cümlesi"),
        ETRAFINDA("\"etrafında kur\""),
        KOMIK("\"komik derecede\""),
        AYNI_ANDA("\", aynı anda\""),
        VE_COMMA("\", ve\""),
        BIR_SEKILDE("\"bir şekilde\""),
        MUHTEMELEN("\"muhtemelen\""),
        SECIYORSUN("\"-mayı seçiyorsun\""),
        FARK_EDIYORSUN("\"fark ediyorsun\""),
    }

    private val phraseRules = listOf(
        StyleRule.ETRAFINDA to Regex("etrafında\\s+kur"),
        StyleRule.KOMIK to Regex("komik\\s+derecede"),
        StyleRule.AYNI_ANDA to Regex(",\\s*aynı anda"),
        StyleRule.VE_COMMA to Regex(",\\s+ve\\s"),
        StyleRule.BIR_SEKILDE to Regex("\\bbir şekilde\\b"),
        StyleRule.MUHTEMELEN to Regex("\\bmuhtemelen\\b"),
        StyleRule.SECIYORSUN to Regex("m[ae]y[ıi] seçiyorsun"),
        StyleRule.FARK_EDIYORSUN to Regex("fark ediyorsun"),
    )

    private val sentenceEnd = Regex("(?<=[.!?…])[\"'”»)]*\\s+")
    private val buComma = Regex("(^|[.!?…][\"'”»)]*\\s+)Bu, ")
    private val yorEnd = Regex("yor(um|sun|uz|sunuz|lar)?$")
    private val token = Regex("\\{[^}]*\\}")

    /** Sistem sesi (köşeli parantezle başlayan satır) bilerek kısa ve kuru; ritim kuralları ona uygulanmaz. */
    private fun isSystemVoice(s: String) = s.trimStart().startsWith("[")

    /** Bir `tr` değerinde tetiklenen üslup kuralları (her kural değer başına en çok bir kez). */
    fun style(text: String): Set<StyleRule> {
        val out = linkedSetOf<StyleRule>()
        val lower = text.lowercase(TR)
        if (!isSystemVoice(text)) {
            if (text.count { it == ';' } > 1) out += StyleRule.SEMICOLON
            if (maxYorRun(text) >= 3) out += StyleRule.YOR_RUN
            if (maxShortRun(text) >= 2) out += StyleRule.SHORT_RUN
        }
        if (buComma.containsMatchIn(text)) out += StyleRule.BU_COMMA
        for ((rule, rx) in phraseRules) if (rx.containsMatchIn(lower)) out += rule
        return out
    }

    /** Art arda gelen, en çok üç sözcüklük cümle dizisi ("Üç adım. Dört."): kesik gerilim romanı ritmi. */
    fun maxShortRun(text: String): Int {
        var best = 0
        var run = 0
        for (sentence in text.split(sentenceEnd)) {
            val words = token.replace(sentence, "x").split(Regex("\\s+")).count { w -> w.any { it.isLetterOrDigit() } }
            if (words in 1..3) {
                run++
                if (run > best) best = run
            } else if (words > 3) {
                run = 0
            }
        }
        return best
    }

    /** Art arda "-yor" ile biten en uzun cümle dizisi. */
    fun maxYorRun(text: String): Int {
        var best = 0
        var run = 0
        for (sentence in text.split(sentenceEnd)) {
            val last = token.replace(sentence, " ").trim()
                .trimEnd { !it.isLetter() }
                .substringAfterLast(' ')
                .lowercase(TR)
            if (last.isNotEmpty() && yorEnd.containsMatchIn(last)) {
                run++
                if (run > best) best = run
            } else if (last.isNotEmpty()) {
                run = 0
            }
        }
        return best
    }

    // ------------------------------------------------------------------ karşılaştırma

    private val trLiteral = Regex("\\btr:\\s*\"(?:[^\"\\\\]|\\\\.)*\"")

    private val enLiteral = Regex("\\ben:\\s*\"(?:[^\"\\\\]|\\\\.)*\"")

    /**
     * Kronik satırlarında `en` de değişebilir (QA P1-5: kronik yalnız kesinleşeni anlatır; iki dil birlikte düzeltilir).
     * `--kronik-en` ile açılır; belirteç kümesi yine korunmalı.
     */
    var allowKronikEn = false

    /** Satırdaki `tr: "..."` değerlerini maskeler; geri kalan her şey (en, anahtarlar, yorumlar) aynı kalmalı. */
    fun maskTr(line: String): String {
        val m = trLiteral.replace(line, "tr: \"…\"")
        return if (allowKronikEn && "kronik:" in line) enLiteral.replace(m, "en: \"…\"") else m
    }

    fun compare(file: String, oldText: String, newText: String): FileReport {
        val findings = mutableListOf<Finding>()
        val oldLines = normalize(oldText).split('\n')
        val newLines = normalize(newText).split('\n')
        if (oldLines.size != newLines.size) {
            findings += Finding(true, file, "satır sayısı değişti (${oldLines.size} → ${newLines.size}); değerler tek satırda kalmalı")
        } else {
            for (i in oldLines.indices) {
                if (maskTr(oldLines[i]) != maskTr(newLines[i])) {
                    findings += Finding(true, "$file:${i + 1}", "tr dışında bir şey değişmiş (en, anahtar, yorum ya da biçim)", newLines[i].trim())
                }
            }
        }

        val oldTree = parse(file, oldText, findings, "eski") ?: return FileReport(file, 0, 0, findings, emptyMap(), emptyMap())
        val newTree = parse(file, newText, findings, "yeni") ?: return FileReport(file, 0, 0, findings, emptyMap(), emptyMap())

        val pairs = mutableListOf<Triple<String, String, String>>()
        walk(oldTree, newTree, "", findings, pairs)

        var changed = 0
        for ((path, old, new) in pairs) {
            if (old == new) continue
            changed++
            checkPair(path, old, new, findings)
        }
        return FileReport(
            file = file,
            changed = changed,
            total = pairs.size,
            findings = findings,
            styleOld = countStyle(pairs.map { it.second }),
            styleNew = countStyle(pairs.map { it.third }),
        )
    }

    fun countStyle(texts: List<String>): Map<StyleRule, Int> {
        val counts = StyleRule.entries.associateWith { 0 }.toMutableMap()
        for (t in texts) for (r in style(t)) counts[r] = counts.getValue(r) + 1
        return counts
    }

    /** Bir dosyadaki tüm `tr` değerleri (yol, metin). */
    fun trValues(file: String, text: String): List<Pair<String, String>> {
        val tree = YamlTree.parse(text, file)
        val pairs = mutableListOf<Triple<String, String, String>>()
        walk(tree, tree, "", mutableListOf(), pairs)
        return pairs.map { it.first to it.second }
    }

    private fun normalize(s: String) = s.replace("\r\n", "\n").removePrefix("﻿")

    private fun parse(file: String, text: String, findings: MutableList<Finding>, which: String): JsonElement? = try {
        YamlTree.parse(normalize(text), file)
    } catch (e: Exception) {
        findings += Finding(true, file, "$which sürüm ayrıştırılamadı: ${e.message}")
        null
    }

    private fun walk(
        o: JsonElement,
        n: JsonElement,
        path: String,
        findings: MutableList<Finding>,
        pairs: MutableList<Triple<String, String, String>>,
    ) {
        when {
            o is JsonObject && n is JsonObject -> {
                if (o.keys.toList() != n.keys.toList()) {
                    findings += Finding(true, path.ifEmpty { "/" }, "anahtarlar değişmiş: ${o.keys} → ${n.keys}")
                    return
                }
                for ((k, ov) in o) {
                    val nv = n.getValue(k)
                    val p = if (path.isEmpty()) k else "$path.$k"
                    if (k == "tr" && ov is JsonPrimitive && nv is JsonPrimitive) {
                        pairs += Triple(p, ov.content, nv.content)
                    } else if (k == "en" && allowKronikEn && ".kronik" in p && ov is JsonPrimitive && nv is JsonPrimitive) {
                        if (tokens(ov.content) != tokens(nv.content)) findings += Finding(true, p, "kronik en belirteçleri değişmiş", nv.content)
                        if (nv.content.isBlank()) findings += Finding(true, p, "kronik en boş", nv.content)
                    } else {
                        walk(ov, nv, p, findings, pairs)
                    }
                }
            }
            o is JsonArray && n is JsonArray -> {
                if (o.size != n.size) {
                    findings += Finding(true, path, "liste uzunluğu değişmiş (${o.size} → ${n.size}); paragraf birleştirme/bölme/silme yok")
                    return
                }
                for (i in o.indices) {
                    val id = (o[i] as? JsonObject)?.get("id")?.let { runCatching { it.jsonPrimitive.content }.getOrNull() }
                    walk(o[i], n[i], "$path[${id ?: i}]", findings, pairs)
                }
            }
            o != n -> findings += Finding(true, path, "değer değişmiş: ${short(o.toString())} → ${short(n.toString())}")
        }
    }

    // ------------------------------------------------------------------ tek bir tr çiftinin denetimi

    /** Belirteç kimliği, ek kısmı hariç: `{npc:x|e}` → `npc:x`. */
    fun tokens(s: String): List<String> = token.findAll(s).map { it.value.drop(1).dropLast(1).substringBefore('|') }.sorted().toList()

    /** Bozuk Yankı yazısı: birleşen işaret (U+0300–U+036F) ya da blok karakter içeren sözcükler. */
    fun corrupted(s: String): List<String> = s.split(Regex("\\s+"))
        .filter { w -> w.any { it in '̀'..'ͯ' || it in '▀'..'▟' } }
        .map { w -> w.trim { !it.isLetterOrDigit() && it !in '̀'..'ͯ' && it !in '▀'..'▟' } }
        .sorted()

    /** Satırın ilk harfi; satır bir belirteçle (ör. `{npc:x}`) başlıyorsa büyük/küçük harf bir şey söylemez → null. */
    fun firstLetter(s: String): Char? {
        val c = s.firstOrNull { it.isLetter() || it == '{' } ?: return null
        return if (c == '{') null else c
    }

    fun checkPair(path: String, old: String, new: String, findings: MutableList<Finding>) {
        if (new.isBlank()) {
            findings += Finding(true, path, "tr boş", new)
            return
        }
        val ot = tokens(old)
        val nt = tokens(new)
        if (ot != nt) {
            findings += Finding(true, path, "belirteçler değişmiş: ${ot - nt.toSet()} eksik, ${nt - ot.toSet()} fazla ($ot → $nt)", new)
        }
        val oc = corrupted(old)
        val nc = corrupted(new)
        if (oc != nc) {
            findings += Finding(true, path, "bozuk yazı değişmiş: $oc → $nc", new)
        }
        val of = firstLetter(old)
        val nf = firstLetter(new)
        if (of != null && nf != null) {
            if (of.isLowerCase() && nf.isUpperCase()) {
                findings += Finding(true, path, "küçük harfli (Yankı) satır büyük harfle başlıyor", new)
            } else if (of.isUpperCase() && nf.isLowerCase()) {
                findings += Finding(false, path, "büyük harfli satır küçük harfe dönmüş (Yankı değilse düzelt)", new)
            }
        }
        if (old.length >= 20) {
            val ratio = new.length.toDouble() / old.length
            if (ratio < 0.6) findings += Finding(false, path, "çok kısa: eski uzunluğun %${(ratio * 100).toInt()} kadarı (bilgi kaybı?)", new)
            if (ratio > 1.6) findings += Finding(false, path, "çok uzun: eski uzunluğun %${(ratio * 100).toInt()} kadarı (şişme?)", new)
        }
        for (r in style(new)) findings += Finding(false, path, "üslup: ${r.label}", new)
    }

    private fun short(s: String) = if (s.length > 80) s.take(77) + "…" else s
}
