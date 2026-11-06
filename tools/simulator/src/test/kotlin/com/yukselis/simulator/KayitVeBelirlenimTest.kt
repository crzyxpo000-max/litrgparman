package com.yukselis.simulator

import com.yukselis.engine.content.ContentLoader
import com.yukselis.engine.content.DirectoryContentSource
import com.yukselis.engine.save.SaveCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * QA (docs/QA.md Adım 2): tam oynanışlarda kayıt ve belirlenimcilik.
 * - Her savaş başında (savaş ortası kayıt dahil) `encode → decode` aynı `WorldState`'i verir.
 * - Aynı tohum ve aynı tarz aynı oynanışı üretir (son, gün, seviye, görülen olaylar).
 */
class KayitVeBelirlenimTest {
    private val content = ContentLoader.load(DirectoryContentSource(File("../../content")))

    @Test
    fun `tam oynanista her savas basinda kayit kayipsiz geri yuklenir`() {
        val sim = Simulator(content)
        var checked = 0
        val failures = mutableListOf<String>()
        for (i in 0 until 6) {
            sim.play(seed = 70_000L + i, profile = Simulator.populationProfile(i)) { s ->
                val back = SaveCodec.decode(SaveCodec.encode(s, 0L)).state
                checked++
                if (back != s && failures.size < 5) {
                    val diff = s.javaClass.declaredFields.filter { f ->
                        f.isAccessible = true
                        f.get(s) != f.get(back)
                    }.map { it.name }
                    failures += "tohum ${70_000L + i} gün ${s.day} sahne ${s.sceneId}: farklı alanlar $diff"
                }
            }
        }
        assertTrue("Hiç savaş başı denetlenmedi", checked > 100)
        assertTrue("Kayıt geri yüklemesi kayıplı:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun `ayni tohum ayni oynanisi uretir`() {
        val sim = Simulator(content)
        for (i in listOf(1, 4, 7)) {
            val a = sim.play(seed = 80_000L + i, profile = Simulator.populationProfile(i))
            val b = sim.play(seed = 80_000L + i, profile = Simulator.populationProfile(i))
            assertEquals(a.ending, b.ending)
            assertEquals(a.day, b.day)
            assertEquals(a.level, b.level)
            assertEquals(a.seenIds, b.seenIds)
            assertEquals(a.levels, b.levels)
        }
    }
}
