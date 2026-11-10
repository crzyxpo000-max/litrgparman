package com.yukselis.simulator

import com.yukselis.engine.content.ContentLoader
import com.yukselis.engine.content.DirectoryContentSource
import com.yukselis.engine.core.PromptKind
import com.yukselis.engine.core.WorldState
import com.yukselis.engine.i18n.Lang
import com.yukselis.engine.save.SaveCodec
import com.yukselis.engine.story.StoryEngine
import java.io.File

/**
 * Arayüz incelemesi için kayıt üretici (docs/UI_REVIEW.md): bir oynanışı simüle eder ve ilginç anlarda (ilk savaş,
 * boss, infaz, sınıf teklifi, brifing, çok seçenekli sahne, son …) durumu kayıt dosyası olarak yazar. Dosyalar
 * emülatöre `files/saves/auto.json` olarak kopyalanıp ekran görüntüsü alınır.
 *
 * Kullanım: `./gradlew :tools:simulator:fixtures --args="<çıkış klasörü> [tohum]"`
 */
fun main(args: Array<String>) {
    val out = File(args.getOrNull(0) ?: "build/ui-fixtures").apply { mkdirs() }
    val seed = args.getOrNull(1)?.toLongOrNull() ?: 11L
    val content = ContentLoader.load(DirectoryContentSource(File("content")))
    val engine = StoryEngine(content)
    val sim = Simulator(content)
    val lang = Lang.TR
    val done = mutableSetOf<String>()
    val index = mutableListOf<String>()

    fun dump(name: String, s: WorldState, note: String = "") {
        if (!done.add(name)) return
        File(out, "$name.json").writeText(SaveCodec.encode(s, 1_700_000_000_000L))
        index += "$name\tgün ${s.day}\tsv ${s.player.level}\tsahne ${s.sceneId}\tolay ${s.dayState.currentEvent}\t$note"
    }

    fun inspect(before: WorldState, s: WorldState, action: String) {
        if (s.dead) return
        val combat = s.combat
        if (combat != null) {
            val view = runCatching { engine.combatView(s, lang) }.getOrNull()
            if (before.combat == null) {
                dump("savas_ilk", s)
                val boss = combat.enemies.any { engine.content.enemy(it.id)?.boss == true }
                if (boss) dump("savas_boss", s)
                if (s.act >= 2) dump("savas_taktik", s)
                if (combat.enemies.size >= 3) dump("savas_kalabalik", s)
            }
            if (view?.execution != null) dump("savas_infaz", s)
            if (combat.round >= 3 && combat.enemies.any { engine.content.enemy(it.id)?.boss == true }) dump("savas_boss_faz", s)
            return
        }
        if (before.combat != null) {
            // Savaş az önce bitti: zafer kartı ya da yenilgi sahnesi (bildirimler kayıtta yok; sahne kalır).
            if (s.variable("_savas_kazanildi") == 0 && s.sceneId != WorldState.HUB) dump("yenilgi_sahnesi", s)
        }
        if (s.prompts.isNotEmpty()) {
            when (s.prompts.first().kind) {
                PromptKind.CLASS_OFFER -> dump("istem_sinif", s)
                PromptKind.SKILL_EVOLUTION -> dump("istem_yetenek_evrimi", s)
                else -> dump("istem_" + s.prompts.first().kind.name.lowercase(), s)
            }
            return
        }
        if (s.ended) {
            dump("son", s, engine.endingOf(s)?.id ?: "?")
            return
        }
        if (s.sceneId == WorldState.HUB) {
            val hub = runCatching { engine.view(s, lang).hub }.getOrNull()
            if (s.mission == null) {
                if (s.day in 2..5) dump("us_erken", s)
                if (s.act == 2 && s.day >= 60) dump("us_perde2", s)
                if (s.act == 3) dump("us_perde3", s)
                if (hub?.operations?.isNotEmpty() == true) dump("us_operasyon_acik_b${s.chapter}", s)
            } else {
                if (s.act == 1) dump("gorev_erken", s)
                if (s.act >= 2) dump("gorev_perde2", s)
            }
            return
        }
        val view = runCatching { engine.view(s, lang) }.getOrNull() ?: return
        val event = s.dayState.currentEvent ?: ""
        val n = view.choices.size
        if (s.day == 0 && n >= 2) dump("prolog_sahne", s)
        if (view.choices.any { !it.enabled }) {
            dump("sahne_kilitli", s)
            if (s.act >= 2) dump("sahne_kilitli_p2", s)
        }
        if (n >= 8) dump("sahne_8_secenek", s, event)
        else if (n >= 6) dump("sahne_6_secenek", s, event)
        else if (n == 5) dump("sahne_5_secenek", s, event)
        else if (n == 3) dump("sahne_3_secenek", s, event)
        if (event.startsWith("brifing_")) dump("brifing_${event.removePrefix("brifing_")}", s)
        if (event.startsWith("darbogaz_")) dump("darbogaz_${event.removePrefix("darbogaz_")}", s)
        if (event.startsWith("varis_")) dump("varis", s, event)
        val longest = view.current.paragraphs.sumOf { it.length }
        if (longest > 1400) dump("sahne_uzun", s, "$longest karakter")
        if (view.current.insights.isNotEmpty()) dump("sahne_sezgi", s)
    }

    val profile = Simulator.PROFILES[0].copy(human = true)
    val result = sim.play(seed, profile, onStep = ::inspect)
    File(out, "index.tsv").writeText(index.joinToString("\n"))
    println("Kayıtlar: ${out.absolutePath} (${done.size}) · son: ${result.ending} · gün ${result.day}")
    index.forEach(::println)
}
