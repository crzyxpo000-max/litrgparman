package com.yukselis.uslup

import java.io.File
import kotlin.system.exitProcess

/**
 * Türkçe üslup doğrulayıcı (docs/USLUP.md §7.1). Proje kökünde çalışır.
 *
 *   uslup [--ref REV] [--ayrinti] [dosya…]   REV'e (varsayılan HEAD) göre değişen content/ dosyalarını karşılaştırır.
 *   uslup --olcum [--ayrinti] [dosya…]       Karşılaştırmasız üslup ölçümü (varsayılan: tüm content/).
 *
 * Hata varsa çıkış kodu 1.
 */
fun main(args: Array<String>) {
    var ref = "HEAD"
    var detail = false
    var measure = false
    val files = mutableListOf<String>()
    var i = 0
    while (i < args.size) {
        when (val a = args[i]) {
            "--ref" -> ref = args.getOrNull(++i) ?: fail("--ref bir sürüm bekliyor")
            "--ayrinti" -> detail = true
            "--olcum" -> measure = true
            "-h", "--help" -> { println(HELP); return }
            else -> files += a.replace('\\', '/')
        }
        i++
    }
    val root = File(".").canonicalFile
    if (measure) measureMode(root, files, detail) else exitProcess(compareMode(root, ref, files, detail))
}

private const val HELP = """Kullanım:
  ./gradlew :tools:uslup:run [--args="--ref REV --ayrinti dosya…"]   değişen tr metinlerini REV'e (HEAD) göre denetler
  ./gradlew :tools:uslup:run --args="--olcum [--ayrinti] [dosya…]"   üslup ölçümü (karşılaştırmasız)"""

private fun fail(msg: String): Nothing {
    System.err.println("uslup: $msg")
    exitProcess(2)
}

private fun git(root: File, vararg args: String): String? {
    val p = ProcessBuilder(listOf("git", *args)).directory(root).redirectErrorStream(false).start()
    val out = p.inputStream.readBytes().toString(Charsets.UTF_8)
    p.errorStream.readBytes()
    return if (p.waitFor() == 0) out else null
}

private fun compareMode(root: File, ref: String, given: List<String>, detail: Boolean): Int {
    val files = given.ifEmpty {
        val changed = git(root, "diff", "--name-only", ref, "--", "content") ?: fail("git diff çalışmadı (ref: $ref)")
        val untracked = git(root, "ls-files", "--others", "--exclude-standard", "--", "content").orEmpty()
        (changed.lines() + untracked.lines()).filter { it.endsWith(".yaml") }.distinct()
    }
    if (files.isEmpty()) {
        println("uslup: $ref sürümüne göre değişen content/ dosyası yok.")
        return 0
    }

    var errors = 0
    var warnings = 0
    var changedTotal = 0
    val styleOld = Checker.StyleRule.entries.associateWith { 0 }.toMutableMap()
    val styleNew = Checker.StyleRule.entries.associateWith { 0 }.toMutableMap()

    for (f in files.sorted()) {
        val file = File(root, f)
        if (!file.exists()) {
            println("✗ $f: dosya silinmiş")
            errors++
            continue
        }
        val old = git(root, "show", "$ref:$f")
        if (old == null) {
            println("✗ $f: $ref içinde yok (yeni dosya); üslup işinde dosya eklenmez")
            errors++
            continue
        }
        val r = Checker.compare(f, old, file.readText(Charsets.UTF_8))
        errors += r.errors
        warnings += r.warnings
        changedTotal += r.changed
        r.styleOld.forEach { (k, v) -> styleOld[k] = styleOld.getValue(k) + v }
        r.styleNew.forEach { (k, v) -> styleNew[k] = styleNew.getValue(k) + v }

        val mark = if (r.errors > 0) "✗" else "✓"
        println("$mark $f: ${r.changed}/${r.total} tr değişti · ${r.errors} hata · ${r.warnings} uyarı")
        println("    üslup (eski → yeni): ${styleLine(r.styleOld, r.styleNew)}")
        val shown = if (detail) r.findings else r.findings.filter { it.error }.take(10)
        for (fd in shown) printFinding(fd)
        val hidden = r.findings.size - shown.size
        if (hidden > 0) println("    … ${hidden} bulgu daha (--ayrinti ile hepsi)")
    }
    println()
    println("Toplam: ${files.size} dosya · $changedTotal tr değişti · $errors hata · $warnings uyarı")
    println("Üslup (eski → yeni): ${styleLine(styleOld, styleNew)}")
    return if (errors > 0) 1 else 0
}

private fun measureMode(root: File, given: List<String>, detail: Boolean) {
    val files = given.ifEmpty {
        File(root, "content").walkTopDown().filter { it.isFile && it.extension == "yaml" }
            .map { it.relativeTo(root).invariantSeparatorsPath }.toList()
    }
    val total = Checker.StyleRule.entries.associateWith { 0 }.toMutableMap()
    var values = 0
    for (f in files.sorted()) {
        val tr = Checker.trValues(f, File(root, f).readText(Charsets.UTF_8).replace("\r\n", "\n"))
        if (tr.isEmpty()) continue
        values += tr.size
        val counts = Checker.countStyle(tr.map { it.second })
        counts.forEach { (k, v) -> total[k] = total.getValue(k) + v }
        println("$f: ${tr.size} tr · ${countLine(counts)}")
        if (detail) for ((path, text) in tr) {
            val rules = Checker.style(text)
            if (rules.isNotEmpty()) printFinding(Checker.Finding(false, path, rules.joinToString(", ") { it.label }, text))
        }
    }
    println()
    println("Toplam: ${files.size} dosya · $values tr · ${countLine(total)}")
}

private fun styleLine(old: Map<Checker.StyleRule, Int>, new: Map<Checker.StyleRule, Int>) =
    Checker.StyleRule.entries.filter { old.getValue(it) + new.getValue(it) > 0 }
        .joinToString(" · ") { "${it.label} ${old.getValue(it)}→${new.getValue(it)}" }.ifEmpty { "temiz" }

private fun countLine(c: Map<Checker.StyleRule, Int>) =
    Checker.StyleRule.entries.filter { c.getValue(it) > 0 }.joinToString(" · ") { "${it.label} ${c.getValue(it)}" }.ifEmpty { "temiz" }

private fun printFinding(f: Checker.Finding) {
    println("    ${if (f.error) "HATA" else "uyarı"} ${f.where}: ${f.message}")
    f.text?.let { println("        » ${if (it.length > 160) it.take(157) + "…" else it}") }
}
