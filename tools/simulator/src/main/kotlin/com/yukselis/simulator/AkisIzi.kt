package com.yukselis.simulator

import com.yukselis.engine.content.ContentLoader
import com.yukselis.engine.content.DirectoryContentSource
import com.yukselis.engine.core.WorldState
import com.yukselis.engine.world.Director
import com.yukselis.engine.world.QuestStatus
import java.io.File

/**
 * Olay sırası kaydı (docs/STORY_FLOW.md teşhisi): N oynanışta her olay başlangıcını, bölge değişimini, savaşı ve kampı
 * neden ve bağlamıyla JSON satırı olarak yazar. Çözümleme `tools/qa/akis/cozumle.py`.
 *
 * Kullanım: `./gradlew :tools:simulator:akis --args="20"` → `tools/qa/akis/iz/<n>.jsonl`
 */
fun main(args: Array<String>) {
    val runs = args.getOrNull(0)?.toIntOrNull() ?: 20
    val outDir = File(args.getOrNull(1) ?: "tools/qa/akis/iz").apply { mkdirs() }
    outDir.listFiles()?.filter { it.extension == "jsonl" }?.forEach { it.delete() }
    val content = ContentLoader.load(DirectoryContentSource(File("content")))
    val sim = Simulator(content)
    val director = Director(content)
    for (i in 0 until runs) {
        val profile = Simulator.populationProfile(i)
        val lines = mutableListOf<String>()
        var lastEvent: String? = null
        var lastEventTension = 0
        var lastEventDay = -1
        var lastEventRegion: String? = null
        var hubSince = 0
        var combatSinceEvent = false
        fun q(v: String?) = if (v == null) "null" else "\"" + v.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        fun arr(l: Collection<String>) = l.joinToString(",", "[", "]") { q(it) }
        val r = sim.play(seed = 10_000L + i, profile = profile, onStep = { before, after, action ->
            if (after.day != before.day) lines += """{"t":"gun","gun":${after.day},"perde":${after.act}}"""
            if (after.region != before.region) {
                lines += """{"t":"bolge","gun":${after.day},"eski":${q(before.region)},"yeni":${q(after.region)},"eylem":${q(action)},"olay":${q(before.dayState.currentEvent)}}"""
            }
            if (before.combat == null && after.combat != null) {
                lines += """{"t":"savas_bas","gun":${after.day},"eylem":${q(action)},"olay":${q(before.dayState.currentEvent)},"sahne":${q(before.sceneId)}}"""
            }
            if (before.combat != null && after.combat == null) {
                combatSinceEvent = true
                lines += """{"t":"savas_son","gun":${after.day},"kazandi":${after.variable("_savas_kazanildi") == 1},"boss":${after.variable("_savas_boss") == 1},"olay":${q(before.dayState.currentEvent)}}"""
            }
            if (action.startsWith("akis:")) hubSince++
            val cur = after.dayState.currentEvent
            val started = cur != null && after.sceneId != WorldState.HUB &&
                (cur != before.dayState.currentEvent || before.sceneId == WorldState.HUB)
            if (started) {
                val e = content.events.getValue(cur!!)
                val seed = before.seeds.firstOrNull { it.eventId == cur && it.dueDay <= after.day }
                val reason = when {
                    seed != null || (e.seedOnly && e.mainArcDay == null) -> when {
                        cur.startsWith("esik_") -> "npc_esik"
                        cur.startsWith("dikkat_") || cur.startsWith("yozlasma_") -> "dikkat_esik"
                        cur.startsWith("bildirim_") -> "bildirim"
                        cur.startsWith("kule_kat_") -> "kule"
                        else -> "tohum"
                    }
                    e.mainArcDay != null -> "ana_yay"
                    action.startsWith("akis:Card") -> "kart"
                    action == "akis:Explore" -> "kesif"
                    action == "akis:ClimbTower" -> "kule"
                    else -> "diger"
                }
                val score = if (reason == "kart") "%.3f".format(java.util.Locale.ROOT, director.score(before, e)) else "null"
                val quests = after.quests.filterValues { it == QuestStatus.ACTIVE }.keys.sorted()
                lines += buildString {
                    append("""{"t":"olay","gun":${after.day},"perde":${after.act},"bolge":${q(after.region)},"olay":${q(cur)},"neden":${q(reason)},""")
                    append(""""puan":$score,"ana_yay_gun":${e.mainArcDay ?: "null"},"tohum_gun":${seed?.dueDay ?: "null"},"tohum_npc":${q(seed?.npc)},""")
                    append(""""eylem":${q(action)},"gerilim":${e.tension},"hedef_gerilim":${director.desiredTension(before)},""")
                    append(""""onceki":${q(lastEvent)},"onceki_gerilim":$lastEventTension,"onceki_gun":$lastEventDay,"onceki_bolge":${q(lastEventRegion)},""")
                    append(""""arada_akis":$hubSince,"arada_savas":$combatSinceEvent,"savastan_hemen":${before.combat != null},""")
                    append(""""aktif_gorev":${arr(quests)},"npc":${arr(e.npcs)}}""")
                }
                lastEvent = cur
                lastEventTension = e.tension
                lastEventDay = after.day
                lastEventRegion = after.region
                hubSince = 0
                combatSinceEvent = false
            }
        })
        File(outDir, "%02d.jsonl".format(i)).writeText(
            """{"t":"oynanis","no":$i,"profil":${q(r.profile)},"son":${q(r.ending)},"gun":${r.day}}""" + "\n" + lines.joinToString("\n") + "\n",
            Charsets.UTF_8,
        )
        println("  oynanış $i (${r.profile}): ${lines.count { it.startsWith("{\"t\":\"olay\"") }} olay, son ${r.ending}")
    }
}
