package com.yukselis.simulator

import com.yukselis.engine.content.ContentLoader
import com.yukselis.engine.content.DirectoryContentSource
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Aşama 9 denge sınırları (görev dosyası): hiçbir son %1'in altında ya da %40'ın üstünde değil (gizli Döngü hariç),
 * hakimiyet evresi oyun süresinin %60+'ı, oynanışlar biter. Tam rapor için simülatörü 1000 oynanışla çalıştır.
 */
class DengeTest {
    private val content = ContentLoader.load(DirectoryContentSource(File("../../content")))

    @Test
    fun `denge sinirlari 200 oynanista saglanir`() {
        val sim = Simulator(content)
        val results = (0 until RUNS).toList().parallelStream()
            .map { i -> sim.play(seed = 50_000L + i, profile = Simulator.populationProfile(i)) }
            .toList()
        val unfinished = results.filter { !it.finished }
        assertTrue("Bitmeyen oynanışlar: ${unfinished.map { "${it.seed}: ${it.stuck}" }}", unfinished.size <= RUNS / 100)
        val problems = Report.distributionOk(results, content)
        assertTrue("Son dağılımı sınır dışı: $problems", problems.isEmpty())
        val dominance = results.map { it.dominance }.average()
        assertTrue("Hakimiyet payı %${(dominance * 100).toInt()} < %60", dominance >= Report.DOMINANCE_TARGET)
        val ironDeaths = results.count { it.ironDeath }
        assertTrue("Demir Mod'da erken Hasat çok: $ironDeaths", ironDeaths <= results.count { it.ironman } / 4)
        // Dikkat bir gerilim ölçeğidir: Perde 2–3'ün ortasında herkes tavanda olmamalı.
        val mid = results.mapNotNull { it.attention[150] }.sorted()
        if (mid.isNotEmpty()) assertTrue("Gün 150 Dikkat medyanı ${mid[mid.size / 2]} ≥ 95", mid[mid.size / 2] < 95)
    }

    companion object {
        const val RUNS = 200
    }
}
