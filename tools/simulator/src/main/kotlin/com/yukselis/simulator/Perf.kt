package com.yukselis.simulator

import com.yukselis.engine.content.ContentLoader
import com.yukselis.engine.content.DirectoryContentSource
import com.yukselis.engine.core.WorldState
import com.yukselis.engine.save.SaveCodec
import java.io.File

/**
 * Masaüstü performans ölçümü (docs/QA.md): içerik yükleme süresi, oyun ortası/sonu kayıt boyutu ve kodlama süresi,
 * motor adım süresi. Cihaz değerleri bunun birkaç katıdır; göreli karşılaştırma içindir.
 *
 * `./gradlew :tools:simulator:perf`
 */
fun main() {
    val src = DirectoryContentSource(File("content"))
    val loads = (1..6).map { val t = System.nanoTime(); ContentLoader.load(src); (System.nanoTime() - t) / 1_000_000 }
    println("İçerik yükleme (ms): ilk ${loads.first()} · ısınmış en iyi ${loads.drop(1).min()}")
    val content = ContentLoader.load(src)
    val sim = Simulator(content)
    var mid: WorldState? = null
    val t0 = System.nanoTime()
    var steps = 0
    sim.play(seed = 4242L, profile = Simulator.populationProfile(3)) { s -> steps++; if (mid == null && s.day >= 180) mid = s }
    val playMs = (System.nanoTime() - t0) / 1_000_000
    println("Tam oynanış (simülatör, savaş başlangıcı sayısı $steps): $playMs ms")
    mid?.let { s ->
        val t = System.nanoTime(); val json = SaveCodec.encode(s, 0L); val enc = (System.nanoTime() - t) / 1_000
        val t2 = System.nanoTime(); SaveCodec.decode(json); val dec = (System.nanoTime() - t2) / 1_000
        println("Gün ${s.day} kaydı (savaş ortası): ${json.toByteArray().size / 1024} KB · kodlama $enc µs · çözme $dec µs")
    }
}
