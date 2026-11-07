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
        // Arayüzün her eylemde kurduğu görünüm modelleri (GameViewModel.render) — QA P2-5.
        val engine = com.yukselis.engine.story.StoryEngine(content)
        val lang = com.yukselis.engine.i18n.Lang.TR
        val views = listOf<Pair<String, () -> Any?>>(
            "view" to { engine.view(s, lang) }, "status" to { engine.status(s, lang) }, "skills" to { engine.skills(s, lang) },
            "bag" to { engine.bag(s, lang) }, "daily" to { engine.daily(s, lang) }, "combat" to { engine.combatView(s, lang) },
            "map" to { engine.map(s, lang) }, "journal" to { engine.journal(s, lang) }, "relations" to { engine.relations(s, lang) },
            "ranking" to { engine.ranking(s, lang) }, "ending" to { engine.endingView(s, lang) },
        )
        repeat(20) { views.forEach { it.second() } } // ısınma
        val costs = views.map { (n, f) -> val t = System.nanoTime(); repeat(50) { f() }; n to (System.nanoTime() - t) / 50_000 }
        println("render görünümleri (µs, ısınmış): " + costs.joinToString(" · ") { "${it.first} ${it.second}" } + " · toplam ${costs.sumOf { it.second }}")
    }
}
