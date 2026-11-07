package com.yukselis.simulator

import com.yukselis.engine.story.Content
import com.yukselis.engine.system.Grade
import com.yukselis.engine.system.Progression
import java.util.Locale

/** `docs/BALANCE.md`: simülatör sonuçlarının raporu (GDD: Simülatör; görev dosyası Aşama 9). */
object Report {
    const val MIN_SHARE = 0.01
    const val MAX_SHARE = 0.40
    const val DOMINANCE_TARGET = 0.60

    private fun pct(x: Double) = String.format(Locale.ROOT, "%.1f%%", x * 100)
    private fun num(x: Double) = String.format(Locale.ROOT, "%.0f", x)

    private fun percentile(xs: List<Int>, q: Double): Int {
        if (xs.isEmpty()) return 0
        val sorted = xs.sorted()
        return sorted[((sorted.size - 1) * q).toInt()]
    }

    /** Dağılım sınırı sağlanıyor mu (gizli Döngü hariç)? */
    fun distributionOk(results: List<RunResult>, content: Content): List<String> {
        val n = results.size.toDouble()
        val problems = mutableListOf<String>()
        content.endings.endings.values.filter { !it.hidden }.forEach { e ->
            val share = results.count { it.ending == e.id } / n
            if (share < MIN_SHARE) problems += "${e.id}: ${pct(share)} < %1"
            if (share > MAX_SHARE) problems += "${e.id}: ${pct(share)} > %40"
        }
        return problems
    }

    fun render(results: List<RunResult>, content: Content, seconds: Long): String {
        val n = results.size
        val sb = StringBuilder()
        sb.appendLine("# Denge raporu (simülatör)")
        sb.appendLine()
        sb.appendLine("`./gradlew :tools:simulator:run --args=\"$n\"` ile üretildi; $n oynanış, $seconds sn. Nüfus: %30 rastgele oyuncu, %70 sekiz hedefli")
        sb.appendLine("oyun tarzı (%25 rastgele seçim gürültüsüyle); her 10 oynanıştan biri Demir Mod. Ayrıntı: `tools/simulator`.")
        sb.appendLine()

        // ---- Sonlar
        sb.appendLine("## Son dağılımı")
        sb.appendLine()
        sb.appendLine("Hedef: her son %1–%40 (gizli Döngü hariç).")
        sb.appendLine()
        sb.appendLine("| Son | Oynanış | Pay |")
        sb.appendLine("| --- | ---: | ---: |")
        val byEnding = results.groupingBy { it.ending ?: "(bitmedi)" }.eachCount()
        (content.endings.endings.keys + "(bitmedi)").forEach { id ->
            val k = byEnding[id] ?: 0
            if (id == "(bitmedi)" && k == 0) return@forEach
            sb.appendLine("| $id | $k | ${pct(k.toDouble() / n)} |")
        }
        val problems = distributionOk(results, content)
        sb.appendLine()
        sb.appendLine(if (problems.isEmpty()) "**Sonuç:** dağılım sınırı sağlanıyor." else "**Sınır dışı:** ${problems.joinToString("; ")}")
        sb.appendLine()
        sb.appendLine("### Oyun tarzına göre")
        sb.appendLine()
        sb.appendLine("| Tarz | Oynanış | En sık son | Hedefe ulaşma |")
        sb.appendLine("| --- | ---: | --- | ---: |")
        results.groupBy { it.profile.removeSuffix(" (Demir)") }.toSortedMap().forEach { (name, rs) ->
            val top = rs.groupingBy { it.ending ?: "—" }.eachCount().maxByOrNull { it.value }
            val hit = if (name == "rastgele") "—" else pct(rs.count { it.ending == name }.toDouble() / rs.size)
            sb.appendLine("| $name | ${rs.size} | ${top?.key} (${top?.value}) | $hit |")
        }
        sb.appendLine()

        // ---- Seviye eğrisi
        sb.appendLine("Kadim istilası (Perde 4 finali) kazanma oranı ve Gün 330 güç oranı (oyuncu ÷ dünya) tarza göre: " +
            results.groupBy { it.profile.removeSuffix(" (Demir)") }.toSortedMap().entries.joinToString(" · ") { (p, rs) ->
                "$p ${pct(rs.count { it.invasionWon }.toDouble() / rs.size)} ×${String.format(Locale.ROOT, "%.2f", rs.map { it.finalPowerRatio }.average())}"
            })
        sb.appendLine()
        sb.appendLine("Hedefe ulaşamayan tarzlarda tutmayan alt koşullar (en sık 3):")
        sb.appendLine()
        results.filter { it.missedConditions.isNotEmpty() }.groupBy { it.profile.removeSuffix(" (Demir)") }.toSortedMap().forEach { (p, rs) ->
            val top = rs.flatMap { it.missedConditions }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(3)
            sb.appendLine("- **$p** (${rs.size}): " + top.joinToString(" · ") { "`${it.key}` ${it.value}" })
        }
        sb.appendLine()
        sb.appendLine("## Seviye eğrisi")
        sb.appendLine()
        sb.appendLine("| Gün | Dünya seviyesi | Oyuncu p10 | p50 | p90 | Kademe (p50) | NPC ort. (perde sonu) |")
        sb.appendLine("| ---: | ---: | ---: | ---: | ---: | --- | ---: |")
        Simulator.CHECKPOINTS.forEach { d ->
            val lv = results.mapNotNull { it.levels[d] }
            val p50 = percentile(lv, 0.5)
            val npc = results.mapNotNull { it.npcMean[d] }.takeIf { it.isNotEmpty() }?.average()?.let { num(it) } ?: ""
            sb.appendLine("| $d | ${num(Progression.worldLevel(d))} | ${percentile(lv, 0.1)} | $p50 | ${percentile(lv, 0.9)} | ${Grade.forLevel(p50.coerceAtLeast(1))} | $npc |")
        }
        sb.appendLine()

        // ---- OP MC
        sb.appendLine("## OP MC ölçüleri")
        sb.appendLine()
        val dom = results.map { it.dominance }.average()
        sb.appendLine("- **Hakimiyet evresi payı** (Gün 31+'de oyuncu seviyesinin dünya seviyesine eşit ya da üstünde olduğu günler): ortalama ${pct(dom)} " +
            "(hedef %60+; ${if (dom >= DOMINANCE_TARGET) "sağlanıyor" else "sağlanmıyor"}).")
        sb.appendLine("- **Ezme payı** (kazanılan savaşların tek satırla çözülen kısmı): ortalama ${pct(results.map { it.crushShare }.average())}.")
        sb.appendLine("- **Perde başına oyuncu / NPC güç farkı** (oyuncu seviyesi p50 ÷ ana NPC'lerin ortalama seviyesi; en güçlü NPC ortalaması):")
        Simulator.ACT_ENDS.forEach { d ->
            val ratio = results.mapNotNull { r -> r.levels.entries.firstOrNull { it.key >= d }?.value?.let { lv -> r.npcMean[d]?.takeIf { it > 0 }?.let { lv / it } } }
            val maxNpc = results.mapNotNull { it.npcMax[d] }.average()
            sb.appendLine("  - Gün $d: ×${String.format(Locale.ROOT, "%.1f", ratio.sorted().getOrElse(ratio.size / 2) { 0.0 })} (en güçlü NPC ortalaması Sv. ${num(maxNpc)})")
        }
        sb.appendLine("- **Kule:** ortalama en yüksek kat ${num(results.map { it.towerFloor.toDouble() }.average())}; 100'e ulaşan ${pct(results.count { it.towerFloor >= 100 }.toDouble() / n)}.")
        sb.appendLine()
        sb.appendLine("### Zaman içinde Dikkat")
        sb.appendLine()
        sb.appendLine("| Gün | p10 | p50 | p90 |")
        sb.appendLine("| ---: | ---: | ---: | ---: |")
        Simulator.CHECKPOINTS.forEach { d ->
            val a = results.mapNotNull { it.attention[d] }
            sb.appendLine("| $d | ${percentile(a, 0.1)} | ${percentile(a, 0.5)} | ${percentile(a, 0.9)} |")
        }
        sb.appendLine()

        // ---- Ölümler
        sb.appendLine("## Ölüm oranları")
        sb.appendLine()
        val normal = results.filter { !it.ironman }
        val iron = results.filter { it.ironman }
        sb.appendLine("- Normal mod: oynanış başına ortalama ${String.format(Locale.ROOT, "%.2f", normal.map { it.deaths }.average())} ölüm (kayıt noktasına dönüş); " +
            "en az bir ölüm ${pct(normal.count { it.deaths > 0 }.toDouble() / normal.size.coerceAtLeast(1))}; p90 ${percentile(normal.map { it.deaths }, 0.9)}.")
        sb.appendLine("- Demir Mod: ${iron.size} oynanış; ölümle Hasat ${pct(iron.count { it.ironDeath }.toDouble() / iron.size.coerceAtLeast(1))}. " +
            "(Simülatörde haddini aşan, bilerek her şeyi açığa vuran bir tarz yok: Demir Mod ölümleri hikaye savaşı ve bosslardan gelir.)")
        val tot = results.sumOf { it.fightsTotal }.coerceAtLeast(1)
        sb.appendLine("- Savaş: oynanış başına ortalama ${num(results.map { it.fightsTotal.toDouble() }.average())}; yere düşülen (yazılmış yenilgi ya da bayılma) " +
            "${pct(results.sumOf { it.fightsDown }.toDouble() / tot)}; İkinci Şans kullanan oynanış ${pct(results.count { it.secondChances > 0 }.toDouble() / results.size)}.")
        sb.appendLine("- Bitmeyen oynanış: ${results.count { !it.finished }}." + results.filter { !it.finished }.take(5).joinToString("") { " [${it.seed} ${it.profile}: ${it.stuck}]" })
        val causes = results.flatMap { it.deathCauses }
        if (causes.isNotEmpty()) {
            sb.appendLine("- Ölüm kaynağı: " + causes.groupingBy { it.substringBefore(':') }.eachCount().entries.sortedByDescending { it.value }
                .joinToString(", ") { "${it.key} ${pct(it.value.toDouble() / causes.size)}" } + ".")
            sb.appendLine("- En çok öldüren: " + causes.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(12)
                .joinToString(", ") { "${it.key} (${it.value})" } + ".")
            val g = results.flatMap { it.deathGaps }
            sb.appendLine("- Ölümde oyuncu − düşman seviyesi: p10 ${percentile(g, 0.1)}, p50 ${percentile(g, 0.5)}, p90 ${percentile(g, 0.9)}.")
        }
        sb.appendLine()
        sb.appendLine("## Kapsam")
        sb.appendLine()
        sb.appendLine("- Oynanış başına görülen olay: ortalama ${num(results.map { it.events.toDouble() }.average())}.")
        val freq = content.events.keys.associateWith { id -> results.count { id in it.seenIds } }
        val never = freq.filter { it.value == 0 }.keys.sorted()
        sb.appendLine("- Hiç görülmeyen olay: ${never.size} / ${freq.size}" + if (never.isEmpty()) "." else ": " + never.joinToString(", ") { "`$it`" } + ".")
        val seenSorted = freq.filter { it.value > 0 }.entries.sortedBy { it.value }
        sb.appendLine("- En nadir 10 olay: " + seenSorted.take(10).joinToString(", ") { "`${it.key}` ${pct(it.value.toDouble() / results.size)}" } + ".")
        sb.appendLine("- En sık 10 olay: " + seenSorted.takeLast(10).reversed().joinToString(", ") { "`${it.key}` ${pct(it.value.toDouble() / results.size)}" } + ".")
        sb.appendLine("- Bitiş seviyesi: p10 ${percentile(results.map { it.level }, 0.1)}, p50 ${percentile(results.map { it.level }, 0.5)}, p90 ${percentile(results.map { it.level }, 0.9)}.")
        return sb.toString()
    }
}
