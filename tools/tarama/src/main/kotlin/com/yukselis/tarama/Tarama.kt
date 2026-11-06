package com.yukselis.tarama

import com.yukselis.engine.content.YamlTree
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.File
import java.util.Locale

/**
 * İçerik tarayıcı (docs/QA.md Adım 1). content/ ağacını ham YAML olarak dolaşır; motor modeline bağlı değildir.
 * Sonuç: kategori başına sayı + ayrıntı listesi (Markdown). Yeniden koşulup önce/sonra karşılaştırılmak içindir.
 */

data class Text(val file: String, val path: String, val tr: String?, val en: String?) {
    val isParagraph get() = ".paragraflar[" in path || path.startsWith("paragraflar[") || ".paragraflar." in path
    val isLabel get() = path.endsWith(".etiket")
}

data class Issue(val where: String, val detail: String)

class Scan(val root: File) {
    val texts = mutableListOf<Text>()
    val choices = mutableListOf<Triple<String, String, JsonObject>>() // file, path, choice
    val scenes = mutableListOf<Triple<String, String, JsonObject>>()  // sahne nesneleri (secenekler taşıyan)
    val events = mutableListOf<Triple<String, String, JsonObject>>()  // olaylar[]
    val flagsSet = mutableMapOf<String, MutableList<String>>()
    val flagsRead = mutableSetOf<String>()
    val seedTargets = mutableSetOf<String>()
    /** İçerikte herhangi bir yerde değer olarak geçen dizgeler (olay kimliği başka yerden anılıyor mu?). */
    val scalarRefs = mutableMapOf<String, Int>()

    fun load() {
        root.resolve("content").walkTopDown().filter { it.isFile && it.extension == "yaml" }.sortedBy { it.path }.forEach { f ->
            val rel = f.relativeTo(root).invariantSeparatorsPath
            val tree = YamlTree.parse(f.readText(Charsets.UTF_8).replace("\r\n", "\n"), rel)
            val isFlagDef = rel.startsWith("content/flags")
            walk(rel, tree, "", emptyList(), isFlagDef)
        }
    }

    private fun str(e: JsonElement?): List<String> = when (e) {
        is JsonPrimitive -> listOf(e.content)
        is JsonArray -> e.flatMap { str(it) }
        else -> emptyList()
    }

    private fun walk(file: String, e: JsonElement, path: String, keys: List<String>, flagDef: Boolean) {
        when (e) {
            is JsonObject -> {
                if ("tr" in e || "en" in e) {
                    if (e.keys.all { it == "tr" || it == "en" }) {
                        texts += Text(file, path, (e["tr"] as? JsonPrimitive)?.content, (e["en"] as? JsonPrimitive)?.content)
                        return
                    }
                }
                val parent = keys.lastOrNull()
                if (parent == "secenekler") choices += Triple(file, path, e)
                if (parent == "olaylar") events += Triple(file, path, e)
                if ("secenekler" in e) scenes += Triple(file, path, e)
                // En yakın bağlam: etki mi (bayrak koyar) yoksa koşul mu (bayrak okur)? Tohumun `kosul`'u etkinin içindedir.
                val ctx = keys.lastOrNull { it in setOf("etkiler", "basarisiz_etkiler", "kosul", "gerekli", "goster") }
                val inEffects = ctx == "etkiler" || ctx == "basarisiz_etkiler"
                for ((k, v) in e) {
                    if (!flagDef) {
                        if (k == "bayrak" && inEffects) str(v).forEach { flagsSet.getOrPut(it) { mutableListOf() } += "$file:$path" }
                        else if (k == "bayrak" || k == "bayrak_yok") flagsRead += str(v)
                        if (k == "tohum" && inEffects) (v as? JsonObject)?.get("olay")?.let { seedTargets += str(it) }
                    }
                    walk(file, v, if (path.isEmpty()) k else "$path.$k", keys + k, flagDef)
                }
            }
            is JsonArray -> e.forEachIndexed { i, x ->
                val id = ((x as? JsonObject)?.get("id") as? JsonPrimitive)?.content
                walk(file, x, "$path[${id ?: i}]", keys, flagDef)
            }
            is JsonPrimitive -> if (keys.lastOrNull() != "id") scalarRefs.merge(e.content, 1, Int::plus)
            else -> {}
        }
    }
}

private val TOKEN = Regex("\\{[^}]*\\}")
private val SENT = Regex("(?<=[.!?…])[\"'”»)]*\\s+(?=[\"'“«(]*[\\p{Lu}\\p{Ll}{])")
private val TR = Locale.forLanguageTag("tr")

fun sentences(s: String): Int = TOKEN.replace(s.trim(), "X").split(SENT).count { it.any(Char::isLetter) }

fun normalize(s: String) = TOKEN.replace(s, " ").lowercase(TR).replace(Regex("[^\\p{L}\\p{N} ]"), " ").replace(Regex("\\s+"), " ").trim()

fun main(args: Array<String>) {
    val root = File(".").canonicalFile
    val out = File(root, args.getOrNull(0) ?: "docs/TARAMA.md")
    val scan = Scan(root).also { it.load() }
    val cats = linkedMapOf<String, MutableList<Issue>>()
    fun add(cat: String, where: String, detail: String) { cats.getOrPut(cat) { mutableListOf() } += Issue(where, detail) }
    fun short(s: String?, n: Int = 110) = s.orEmpty().let { if (it.length > n) it.take(n - 1) + "…" else it }
    val texts = scan.texts

    // 1. Eksik/boş çeviri ve uzunluk oranı
    for (t in texts) {
        val w = "${t.file} ${t.path}"
        if (t.tr.isNullOrBlank() || t.en.isNullOrBlank()) add("Eksik/boş çeviri", w, "tr=\"${short(t.tr, 50)}\" en=\"${short(t.en, 50)}\"")
    }
    fun bare(s: String) = TOKEN.replace(s, "X")
    val ratios = texts.filter { !it.tr.isNullOrBlank() && !it.en.isNullOrBlank() && bare(it.tr).length >= 40 }
        .map { it to bare(it.en!!).length.toDouble() / bare(it.tr!!).length }
    val sortedR = ratios.map { it.second }.sorted()
    fun q(p: Double) = if (sortedR.isEmpty()) 0.0 else sortedR[((sortedR.size - 1) * p).toInt()]
    for ((t, r) in ratios) if (r < 0.55 || r > 2.0) add("TR/EN uzunluk oranı aşırı (EN/TR <0.55 ya da >2.0)", "${t.file} ${t.path}", "oran ${"%.2f".format(Locale.ROOT, r)} · TR: ${short(t.tr, 80)} · EN: ${short(t.en, 80)}")

    // 2. Düz yazılmış NPC adı, İngilizcede metrik birim
    val names = File(root, "content/names.yaml").readText().let { YamlTree.parse(it.replace("\r\n", "\n"), "names") as JsonObject }
    val trNames = mutableSetOf<String>(); val enNames = mutableSetOf<String>()
    (names["npc"] as? JsonObject)?.values?.forEach { n ->
        for (k in listOf("ad", "kisa_ad")) ((n as? JsonObject)?.get(k) as? JsonObject)?.let { o ->
            (o["tr"] as? JsonPrimitive)?.content?.let { trNames += it }
            (o["en"] as? JsonPrimitive)?.content?.let { enNames += it }
        }
    }
    val nameFiles = setOf("content/names.yaml")
    // Ad alanları (eşya/düşman/öz adları) özel addır; anlatı ve etiketler taranır.
    for (t in texts.filter { it.file !in nameFiles && !it.path.endsWith(".ad") }) {
        for ((lang, s, set) in listOf(Triple("tr", t.tr, trNames), Triple("en", t.en, enNames))) {
            val plain = TOKEN.replace(s.orEmpty(), " ")
            set.filter { it.length >= 3 && Regex("(?<![\\p{L}])${Regex.escape(it)}(?![\\p{Ll}])").containsMatchIn(plain) }.forEach {
                add("Düz yazılmış NPC adı", "${t.file} ${t.path}", "$lang \"$it\" · ${short(s)}")
            }
        }
    }
    val metric = Regex("\\b\\d+([.,]\\d+)?\\s?(km|kg|cm|mm|m|°C|kilomet(er|re)s?|kilograms?|met(er|re)s?|liters?|litres?)\\b|celsius|kilomet(er|re)|kilogram|centimet", RegexOption.IGNORE_CASE)
    for (t in texts) if (t.en != null && metric.containsMatchIn(t.en)) add("İngilizcede metrik birim", "${t.file} ${t.path}", short(t.en))

    // 3. Uzun paragraf, uzun seçenek
    for (t in texts.filter { it.isParagraph }) {
        val st = t.tr?.let(::sentences) ?: 0; val se = t.en?.let(::sentences) ?: 0
        if (st > 3 || se > 3) add("3 cümleyi aşan paragraf", "${t.file} ${t.path}", "tr $st · en $se cümle · ${short(t.tr, 80)}")
    }
    for (t in texts.filter { it.isLabel }) {
        val lt = t.tr?.length ?: 0; val le = t.en?.length ?: 0
        if (lt > 60 || le > 60) add("60 karakteri aşan seçenek", "${t.file} ${t.path}", "tr $lt · en $le · ${short(t.tr, 90)}")
    }

    // 4. Aynı / neredeyse aynı metin (≥50 karakter TR; 5 sözcüklük parçalarla Jaccard ≥0.6)
    val long = texts.filter { (it.tr?.length ?: 0) >= 50 }
    long.groupBy { normalize(it.tr!!) }.values.filter { it.size > 1 }.forEach { g ->
        add("Birebir aynı metin (TR)", g.joinToString(" | ") { "${it.file} ${it.path}" }, "${g.size}× · ${short(g.first().tr, 90)}")
    }
    val shingles = long.map { t -> normalize(t.tr!!).split(' ').windowed(5) { it.joinToString(" ") }.toSet() }
    val index = mutableMapOf<String, MutableList<Int>>()
    shingles.forEachIndexed { i, sh -> sh.forEach { index.getOrPut(it) { mutableListOf() } += i } }
    val pairs = mutableSetOf<Pair<Int, Int>>()
    for (lst in index.values) if (lst.size in 2..15) for (a in lst.indices) for (b in a + 1 until lst.size) pairs += lst[a] to lst[b]
    for ((a, b) in pairs) {
        val sa = shingles[a]; val sb = shingles[b]
        if (sa.isEmpty() || sb.isEmpty()) continue
        val j = sa.intersect(sb).size.toDouble() / sa.union(sb).size
        if (j >= 0.6 && j < 1.0) add("Neredeyse aynı metin (TR, Jaccard ≥0.6)", "${long[a].file} ${long[a].path} ≈ ${long[b].path}", "${"%.2f".format(Locale.ROOT, j)} · ${short(long[a].tr, 70)}")
    }

    // 5. Etkisiz seçenek: sahnedeki tüm seçenekler aynı yere gidiyor ve bu seçeneğin hiçbir etkisi/savaşı/zarı yok
    val consequential = setOf("etkiler", "savas", "zar", "tur", "kronik")
    for ((file, path, scene) in scan.scenes) {
        val opts = (scene["secenekler"] as? JsonArray)?.mapNotNull { it as? JsonObject } ?: continue
        if (opts.size < 2) continue
        val targets = opts.map { (it["sonraki"] as? JsonPrimitive)?.content }.toSet()
        if (targets.size != 1 || opts.any { it["savas"] != null }) continue
        val inert = opts.filter { o -> o.keys.none { it in consequential } || (o["etkiler"] as? JsonArray)?.isEmpty() == true && o.keys.intersect(consequential - "etkiler").isEmpty() }
        if (inert.size == opts.size) add("Sonucu olmayan seçim (tüm seçenekler aynı yere, hiçbirinin etkisi yok)", "$file $path", "${opts.size} seçenek → ${targets.first()}")
        else inert.forEach { o -> add("Etkisiz seçenek (kardeşleri etkili, aynı hedef)", "$file $path", "etiket: ${short(((o["etiket"] as? JsonObject)?.get("tr") as? JsonPrimitive)?.content, 60)}") }
    }

    // 6. Konulup hiç okunmayan bayrak (motor kaynağındaki dizgeler de okuma sayılır)
    val engineStrings = File(root, "engine/src/main/kotlin").walkTopDown().filter { it.extension == "kt" }
        .flatMap { Regex("\"([a-z0-9_]+)\"").findAll(it.readText()).map { m -> m.groupValues[1] } }.toSet()
    val appStrings = File(root, "app/src/main").walkTopDown().filter { it.extension == "kt" }
        .flatMap { Regex("\"([a-z0-9_]+)\"").findAll(it.readText()).map { m -> m.groupValues[1] } }.toSet()
    val prefixes = listOf("yenildi_", "uyanis_", "olay_", "dikkat_esik_", "_red_", "_zar_")
    for ((flag, where) in scan.flagsSet.toSortedMap()) {
        if (flag in scan.flagsRead || flag in engineStrings || flag in appStrings || prefixes.any { flag.startsWith(it) }) continue
        add("Konulup hiç okunmayan bayrak", flag, "${where.size} yerde konuyor · ${where.first()}")
    }

    // 7. Hiç tohumlanmayan yalnız-tohum olayı
    for ((file, path, ev) in scan.events) {
        val id = (ev["id"] as? JsonPrimitive)?.content ?: continue
        // Motorun kendisinin tetiklediği aileler (eşik, Dikkat/Yozlaşma eşiği) ve başka yerden anılan kimlikler (kule katları) hariç.
        val engineFamily = listOf("esik_", "dikkat_", "yozlasma_").any { id.startsWith(it) }
        if ((ev["yalniz_tohum"] as? JsonPrimitive)?.content == "true" && id !in scan.seedTargets && id !in engineStrings &&
            !engineFamily && (scan.scalarRefs[id] ?: 0) == 0)
            add("Hiç tohumlanmayan yalnız-tohum olayı", "$file $id", "")
    }

    // 8. Kilitli seçeneği olmayan olay; ana NPC'nin ilişkisini etkilemeyen olay
    val mains = File(root, "content/npcs").listFiles()!!.map { it.nameWithoutExtension }.toSet()
    for ((file, _, ev) in scan.events) {
        val id = (ev["id"] as? JsonPrimitive)?.content ?: continue
        val json = ev.toString()
        if ("\"gerekli\"" !in json) add("Kilitli seçeneği (gerekli) olmayan olay", "$file $id", "")
        val npcs = (ev["npc"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.content }.orEmpty().filter { it in mains }
        if (npcs.isNotEmpty() && "\"iliski\"" !in json && "\"hafiza\"" !in json)
            add("Ana NPC olayı ilişki/hafıza etkisi taşımıyor", "$file $id", "npc: ${npcs.joinToString()}")
    }

    // Rapor
    val sb = StringBuilder()
    sb.appendLine("# İçerik taraması")
    sb.appendLine()
    sb.appendLine("`./gradlew :tools:tarama:run` ile üretilir (tools/tarama). ${texts.size} metin, ${scan.choices.size} seçenek, ${scan.events.size} olay tarandı.")
    sb.appendLine("EN/TR uzunluk oranı dağılımı (TR ≥40 karakter): p1 ${"%.2f".format(Locale.ROOT, q(0.01))} · p50 ${"%.2f".format(Locale.ROOT, q(0.5))} · p99 ${"%.2f".format(Locale.ROOT, q(0.99))}.")
    sb.appendLine()
    sb.appendLine("| Kategori | Sayı |")
    sb.appendLine("| --- | ---: |")
    val allCats = listOf(
        "Eksik/boş çeviri", "TR/EN uzunluk oranı aşırı (EN/TR <0.55 ya da >2.0)", "Düz yazılmış NPC adı", "İngilizcede metrik birim",
        "3 cümleyi aşan paragraf", "60 karakteri aşan seçenek", "Birebir aynı metin (TR)", "Neredeyse aynı metin (TR, Jaccard ≥0.6)",
        "Sonucu olmayan seçim (tüm seçenekler aynı yere, hiçbirinin etkisi yok)", "Etkisiz seçenek (kardeşleri etkili, aynı hedef)",
        "Konulup hiç okunmayan bayrak", "Hiç tohumlanmayan yalnız-tohum olayı", "Kilitli seçeneği (gerekli) olmayan olay",
        "Ana NPC olayı ilişki/hafıza etkisi taşımıyor",
    )
    for (c in allCats) sb.appendLine("| $c | ${cats[c]?.size ?: 0} |")
    for (c in allCats) {
        val l = cats[c] ?: continue
        sb.appendLine()
        sb.appendLine("## $c (${l.size})")
        sb.appendLine()
        for (i in l) sb.appendLine("- `${i.where}`" + if (i.detail.isNotEmpty()) " — ${i.detail.replace("|", "\\|")}" else "")
    }
    out.writeText(sb.toString(), Charsets.UTF_8)
    println(sb.lines().takeWhile { !it.startsWith("## ") }.joinToString("\n"))
    println("Ayrıntı: ${out.relativeTo(root).invariantSeparatorsPath}")
}
