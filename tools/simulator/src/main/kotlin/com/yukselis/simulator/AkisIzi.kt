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
 * Kullanım: `./gradlew :tools:simulator:akis --args="20"` → `tools/qa/akis/iz/<n>.jsonl`; üçüncü argüman `insan` →
 * insan benzeri gün akışı (Profile.human).
 */
fun main(args: Array<String>) {
    val runs = args.getOrNull(0)?.toIntOrNull() ?: 20
    val outDir = File(args.getOrNull(1) ?: "tools/qa/akis/iz").apply { mkdirs() }
    outDir.listFiles()?.filter { it.extension == "jsonl" }?.forEach { it.delete() }
    val content = ContentLoader.load(DirectoryContentSource(File("content")))
    val sim = Simulator(content)
    val director = Director(content)
    val human = args.getOrNull(2) == "insan"
    for (i in 0 until runs) {
        val profile = Simulator.populationProfile(i).copy(human = human)
        val lines = mutableListOf<String>()
        var lastEvent: String? = null
        var lastEventTension = 0
        var lastEventDay = -1
        var lastEventRegion: String? = null
        var hubSince = 0
        var combatSinceEvent = false
        fun q(v: String?) = if (v == null) "null" else "\"" + v.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        fun arr(l: Collection<String>) = l.joinToString(",", "[", "]") { q(it) }
        // Gün özeti (serbest gün ölçümü): o gün başlayan olay (varış ve bölüm sahneleri hariç), biten savaş, üste görülen
        // en büyük fırsat eli, görevde geçip geçmediği.
        // Hikaye anı: o gün olay ya da Sistem isteminde bir seçim yapıldı (varış ve zaman atlama özeti hariç). Savaş, Ezme ile
        // biten tek satırlık savaş da, hikaye anı sayılmaz.
        var dEvents = 0; var dFights = 0; var dHand = 0; var dMission = false; var dPhase = ""; var dChapter = 0; var dStory = false; var dTower = 0
        // Bitiş kaydı (yay düğümü erişimi, ör. Elif denetimi): son durum ve oynanış boyunca yoldaş olanlar.
        var last: WorldState? = null
        val everCompanion = mutableSetOf<String>()
        fun flushDay(day: Int, skipped: Boolean = false) {
            lines += """{"t":"gunozet","gun":$day,"bolum":$dChapter,"evre":${q(dPhase)},"olay":$dEvents,"savas":$dFights,"el":$dHand,"gorevde":$dMission,"atla":$skipped,"hikaye":$dStory,"kule":$dTower}"""
            dEvents = 0; dFights = 0; dHand = 0; dMission = false; dStory = false; dTower = 0
        }
        val r = sim.play(seed = 10_000L + i, profile = profile, onStep = { before, after, action ->
            if (after.sceneId == WorldState.HUB && after.mission == null) dHand = maxOf(dHand, after.dayState.hand.size)
            if (after.mission != null) dMission = true
            if (action == "akis:ClimbTower" || action == "akis:FastClimb") dTower++
            // Seçim kaydı (yay ve ölüm çözümlemesi): hangi olayın hangi sahnesinde hangi seçenek.
            if (action.startsWith("secim:")) {
                lines += """{"t":"secim","gun":${before.day},"olay":${q(before.dayState.currentEvent)},"sahne":${q(before.sceneId)},"secim":${q(action.removePrefix("secim:"))}}"""
            }
            last = after
            after.npcs.values.filter { it.companion }.forEach { everCompanion += it.id }
            dPhase = before.chapterPhase.key; dChapter = before.chapter
            if (before.combat != null && after.combat == null) dFights++
            val ev = before.dayState.currentEvent
            if (action.startsWith("secim:") && (ev == null || (!ev.startsWith(com.yukselis.engine.world.ChapterTable.ARRIVAL_PREFIX) && !ev.startsWith(com.yukselis.engine.world.TimeSkip.EVENT_PREFIX)))) dStory = true
            if (action.startsWith("istem") || action.startsWith("sistem")) dStory = true
            val ce = after.dayState.currentEvent
            if (ce != null && ce != before.dayState.currentEvent && !ce.startsWith(com.yukselis.engine.world.ChapterTable.ARRIVAL_PREFIX) &&
                ce !in content.chapters.managed && !ce.startsWith(com.yukselis.engine.world.TimeSkip.EVENT_PREFIX)) dEvents++
            // Zaman atlama: atlamanın başladığı gün ve aradaki günler "atlanan" sayılır (olay/savaş yok).
            // Kamp anı (saha kampı sahnesi ölçümü): nerede, görevde mi, yanındaki yoldaşlar, sahne oynandı mı.
            if (action == "akis:Camp") {
                val comp = before.npcs.values.filter { it.companion && it.alive }.map { it.id }
                lines += """{"t":"kamp","gun":${before.day},"bolum":${before.chapter},"bolge":${q(before.region)},"gorev":${q(before.mission?.kind?.name)},"yoldaslar":${arr(comp)},"sahne":${q(after.dayState.currentEvent?.takeIf { after.variable(com.yukselis.engine.world.WorldEngine.CAMP_PENDING_VAR) > 0 })}}"""
            }
            val skip = action == "akis:Skip"
            if (skip) {
                for (d in before.day until after.day) { flushDay(d, skipped = true); dHand = 0 }
                if (after.day != before.day) lines += """{"t":"gun","gun":${after.day},"perde":${after.act}}"""
            }
            if (after.day != before.day && !skip) { flushDay(before.day); lines += """{"t":"gun","gun":${after.day},"perde":${after.act}}""" }
            if (after.region != before.region) {
                lines += """{"t":"bolge","gun":${after.day},"eski":${q(before.region)},"yeni":${q(after.region)},"eylem":${q(action)},"olay":${q(before.dayState.currentEvent)},"gorev_degisti":${before.mission != after.mission || before.chapter != after.chapter}}"""
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
                    cur in content.chapters.managed || cur.startsWith(com.yukselis.engine.world.ChapterTable.ARRIVAL_PREFIX) -> "bolum"
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
                    action == "akis:ClimbTower" || action == "akis:FastClimb" -> "kule"
                    else -> "diger"
                }
                val m = after.mission
                val arrival = cur.startsWith(com.yukselis.engine.world.ChapterTable.ARRIVAL_PREFIX)
                val own = when {
                    m == null -> null
                    arrival -> true
                    m.kind == com.yukselis.engine.world.MissionKind.EXPLORE -> director.fitsLocation(after, e)
                    // Görevin kendi olayı: kuyrukta bekleyen (varış sahnesi yoksa aynı adımda başlar) ya da bölümün operasyonu.
                    else -> before.mission?.queue?.contains(cur) == true || action == "akis:Card:$cur" ||
                        // Aynı adımda başlayan yan görev (varış sahnesi yoksa): kuyruğun tek olayı hemen açılır.
                        (m.kind == com.yukselis.engine.world.MissionKind.SIDE && m.queue.isEmpty() &&
                            before.mission?.let { it.kind != m.kind || it.region != m.region || it.startDay != m.startDay } != false) ||
                        cur in (content.chapters.get(after.chapter)?.operationEvents ?: emptyList())
                }
                val fits = arrival || own == true || director.fitsLocation(after, e)
                val score = if (reason == "kart") "%.3f".format(java.util.Locale.ROOT, director.score(before, e)) else "null"
                val quests = after.quests.filterValues { it == QuestStatus.ACTIVE }.keys.sorted()
                lines += buildString {
                    append("""{"t":"olay","gun":${after.day},"perde":${after.act},"bolge":${q(after.region)},"olay":${q(cur)},"neden":${q(reason)},""")
                    append(""""puan":$score,"ana_yay_gun":${e.mainArcDay ?: "null"},"tohum_gun":${seed?.dueDay ?: "null"},"tohum_npc":${q(seed?.npc)},""")
                    append(""""eylem":${q(action)},"gerilim":${e.tension},"hedef_gerilim":${director.desiredTension(before)},""")
                    append(""""onceki":${q(lastEvent)},"onceki_gerilim":$lastEventTension,"onceki_gun":$lastEventDay,"onceki_bolge":${q(lastEventRegion)},""")
                    append(""""arada_akis":$hubSince,"arada_savas":$combatSinceEvent,"savastan_hemen":${before.combat != null},""")
                    append(""""aktif_gorev":${arr(quests)},"npc":${arr(e.npcs)},""")
                    append(""""gorev":${q(m?.kind?.key)},"gorev_bolge":${q(m?.region)},"kendi":${own ?: "null"},"konum_uyar":$fits,""")
                    append(""""bolum":${after.chapter},"evre":${q(after.chapterPhase.key)}}""")
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
        last?.let { s ->
            val arcFlags = s.flags.filter { it.startsWith("elif_") || it.startsWith("demir_gece_") || it in setOf("insan_ittifaki", "siginak_1_goruldu", "konsey_kurtarildi") }.sorted()
            File(outDir, "%02d.jsonl".format(i)).appendText(
                """{"t":"bitis","bayraklar":${arr(arcFlags)},"yoldas_olan":${arr(everCompanion.sorted())},"olu":${arr(s.npcs.values.filter { !it.alive }.map { it.id }.sorted())}}""" + "\n",
            )
        }
        println("  oynanış $i (${r.profile}): ${lines.count { it.startsWith("{\"t\":\"olay\"") }} olay, son ${r.ending}")
    }
}
