package com.yukselis.engine.world

import com.yukselis.engine.combat.CombatEngine
import com.yukselis.engine.combat.CombatSpec
import com.yukselis.engine.core.Rng
import com.yukselis.engine.core.WorldState
import com.yukselis.engine.i18n.LText
import com.yukselis.engine.story.ActionRejected
import com.yukselis.engine.story.Content
import com.yukselis.engine.story.Effect
import com.yukselis.engine.story.EffectApplier
import com.yukselis.engine.story.Notice
import com.yukselis.engine.story.NoticeKind
import com.yukselis.engine.system.Grade
import com.yukselis.engine.system.Progression

/** Gün akışında (olay dışında) yapılabilen eylemler. Her biri bir yuva harcar; Kamp günü bitirir. */
sealed interface HubAction {
    data class Card(val eventId: String) : HubAction
    data object Explore : HubAction
    data object Train : HubAction
    data class EnterGate(val uid: Int) : HubAction
    data object Rest : HubAction
    data class Travel(val region: String) : HubAction
    data object Camp : HubAction
    /** Kule'nin bir sonraki katına çık (Aşama 7). */
    data object ClimbTower : HubAction
}

/**
 * Gün döngüsü (GDD): sabah Günlük Görev, gün içinde 2–4 yuva (olay kartı ya da serbest eylem),
 * akşam kamp. Bölge durumları, kapılar ve kapı taşmaları. Saf: durum alır, yeni durum döndürür.
 */
class WorldEngine(
    private val content: Content,
    private val effects: EffectApplier,
    private val combat: CombatEngine,
    val director: Director = Director(content),
) {
    data class Result(val state: WorldState, val notices: List<Notice>)

    private val texts get() = content.texts

    // ---------------------------------------------------------------- akışa giriş

    /**
     * Olay ya da savaş bitti, gün akışına dönülüyor. İlk girişte dünya kurulur. Zorunlu olay
     * (ana yay, vadesi gelen tohum) varsa hemen başlar; yoksa fırsat eli dağıtılır.
     */
    fun enterHub(state: WorldState): Result {
        var s = effects.npc.ensure(state.copy(sceneId = WorldState.HUB, dayState = state.dayState.copy(currentEvent = null), focusNpc = null))
        val notices = mutableListOf<Notice>()
        if (s.regions.isEmpty()) {
            val setup = setupWorld(s)
            s = setup.state
            notices += setup.notices
        }
        // Kuruyan tohumlar
        val withered = director.witheredSeeds(s)
        if (withered.isNotEmpty()) s = s.copy(seeds = s.seeds - withered.toSet())
        // Zorunlu olay: yuva harcamadan değil, bir yuva yerine gelir (kalmadıysa yine gelir: hikaye beklemez).
        director.forced(s)?.let { f ->
            if (f.seed != null) s = s.copy(seeds = s.seeds.filterNot { it == f.seed })
            // Zorunlu olay yuva harcamaz: eylemin yuvası zaten harcandı (DECISIONS #73).
            val started = startEvent(s, f.eventId, consumeSlot = false, focus = f.seed?.npc)
            return Result(started.state, notices + started.notices)
        }
        val rng = Rng(s.rngState)
        val hand = if (s.dayState.slotsLeft > 0) director.deal(s, rng) else emptyList()
        s = s.copy(rngState = rng.state, dayState = s.dayState.copy(hand = hand))
        return Result(s, notices)
    }

    private fun setupWorld(state: WorldState): Result {
        val regions = content.regions.values.associate { it.id to RegionState(it.id, it.baseDanger, it.control) }
        var s = state.copy(
            regions = regions,
            region = state.region ?: content.startRegion ?: content.regions.keys.firstOrNull(),
            // Gün akışı geldi: keşif/savaş/kapı türü Günlük Görevler artık çıkabilir (DECISIONS #31).
            flags = state.flags + DAILY_WIDE_FLAG,
            publicLevel = state.player.level,
        )
        // NPC'ler ve Küresel Sıralama (Aşama 5)
        s = effects.npc.setupRanking(effects.npc.ensure(s))
        val day = startDay(s, advance = false)
        s = day.state
        return Result(s, listOf(Notice(NoticeKind.SYSTEM, texts.format("akis_basladi"))) + day.notices)
    }

    /** Yeni gün: (gerekirse günü ilerletir), yuvalar, kapı doğuşu ve taşması. */
    fun startDay(state: WorldState, advance: Boolean): Result {
        var s = state
        val notices = mutableListOf<Notice>()
        if (advance) {
            val r = effects.applyAll(s, listOf(Effect.AdvanceDays(1)))
            s = r.state
            notices += r.notices
            // Her NPC her gün bir adım atar (GDD: Dinamik karakterler).
            val n = effects.npc.dailyTick(s)
            s = n.state
            notices += n.notices
        }
        val rng = Rng(s.rngState)
        val slots = rng.nextInt(Progression.MIN_SLOTS, Progression.MAX_SLOTS)
        var uid = s.dayState.nextGateUid
        // Perde geçişinde yeni bölgeler kayda eklenir (eski kayıtlar ve Perde 2+ bölgeleri).
        val missing = content.regions.values.filter { it.id !in s.regions }
        if (missing.isNotEmpty()) s = s.copy(regions = s.regions + missing.associate { it.id to RegionState(it.id, it.baseDanger, it.control) })
        val regions = s.regions.mapValues { (id, r) ->
            val def = content.regions[id] ?: return@mapValues r
            // Yalnızca bu perdede canlı bölgelerde kapı doğar ve taşar (Öğretici Katman Perde 1'de kapanır).
            if (!def.liveIn(s.act)) return@mapValues if (r.gates.isEmpty()) r else r.copy(gates = emptyList())
            var region = r
            // Taşma: vaktinde kapatılmayan kapı bölgeye canavar salar.
            val overflowed = region.gates.filter { it.deadline < s.day }
            if (overflowed.isNotEmpty()) {
                region = region.copy(gates = region.gates - overflowed.toSet(), danger = (region.danger + OVERFLOW_DANGER * overflowed.size).coerceAtMost(100))
                s = s.copy(vars = s.vars + (OVERFLOW_VAR to s.variable(OVERFLOW_VAR) + overflowed.size))
                notices += Notice(NoticeKind.WARNING, texts.formatL("kapi_tasti", "region" to regionName(id)))
            }
            // Doğuş: tehlike arttıkça kapı olasılığı artar.
            val chance = def.gateChance * (1 + region.danger / 100.0)
            if (def.gateEnemies.isNotEmpty() && region.gates.size < MAX_GATES && rng.chance(chance)) {
                val n = rng.nextInt(1, 3)
                val enemies = List(n) { scaledRef(def.gateEnemies[rng.nextInt(def.gateEnemies.size)], s.copy(region = id), gate = true) }
                val grade = enemies.mapNotNull { content.enemy(it)?.grade }.maxOrNull() ?: Grade.F
                val gate = Gate(uid++, grade, enemies, s.day, s.day + rng.nextInt(GATE_MIN_DAYS, GATE_MAX_DAYS))
                region = region.copy(gates = region.gates + gate)
                if (id == s.region) notices += Notice(NoticeKind.QUEST, texts.formatL("kapi_acildi", "region" to regionName(id), "grade" to LText(grade.name, grade.name)))
            }
            region
        }
        s = s.copy(
            regions = regions,
            rngState = rng.state,
            dayState = s.dayState.copy(slotsLeft = slots, slotsTotal = slots, nextGateUid = uid, hand = emptyList()),
        )
        notices += Notice(NoticeKind.SYSTEM, texts.format("gun_basladi", "day" to s.day, "n" to slots))
        return Result(s, notices)
    }

    // ---------------------------------------------------------------- eylemler

    fun act(state: WorldState, action: HubAction): Result {
        if (state.sceneId != WorldState.HUB) throw ActionRejected("Gün akışında değilsin")
        if (state.combat != null) throw ActionRejected("Savaş sürüyor")
        if (state.prompts.isNotEmpty()) throw ActionRejected("Önce Sistem istemi")
        val slots = state.dayState.slotsLeft
        if (action != HubAction.Camp && slots <= 0) throw ActionRejected("Bugünün yuvaları bitti; kamp kur")
        return when (action) {
            is HubAction.Card -> {
                if (action.eventId !in state.dayState.hand) throw ActionRejected("Bu kart elde yok")
                startEvent(state, action.eventId, consumeSlot = true).toResult()
            }
            HubAction.Explore -> explore(state)
            HubAction.Train -> train(state)
            is HubAction.EnterGate -> enterGate(state, action.uid)
            HubAction.Rest -> rest(state)
            is HubAction.Travel -> travel(state, action.region)
            HubAction.Camp -> camp(state)
            HubAction.ClimbTower -> climbTower(state)
        }
    }

    // ---------------------------------------------------------------- Kule (Aşama 7)

    /** Kule burada ve şimdi tırmanılabilir mi? */
    fun towerOpen(state: WorldState): Boolean {
        val t = content.tower ?: return false
        return state.region == t.region && (t.condition?.test(state) ?: true) && state.variable(EffectApplier.TOWER_VAR) < t.floors
    }

    fun nextFloor(state: WorldState): Int = state.variable(EffectApplier.TOWER_VAR) + 1

    /**
     * Bir sonraki kat: her 10. kat (ve tanımlı her hikaye katı) yazılmış bir olaydır; diğerleri bandın düşmanlarından
     * kat seviyesine ölçeklenmiş prosedürel bir savaştır. Zaferde `kule_kat` bir artar.
     */
    private fun climbTower(state: WorldState): Result {
        val t = content.tower ?: throw ActionRejected("Kule yok")
        if (!towerOpen(state)) throw ActionRejected("Kule şu an tırmanılamaz")
        val floor = nextFloor(state)
        t.storyFloors[floor]?.let { eventId ->
            if (eventId in content.events) return startEvent(state, eventId, consumeSlot = true)
        }
        val band = t.band(floor) ?: throw ActionRejected("Kat $floor için bant yok")
        var s = spend(state)
        val rng = Rng(s.rngState)
        val level = t.levelOf(floor)
        val n = if (floor % 5 == 0) 1 else rng.nextInt(1, 3)
        val enemies = List(n) { com.yukselis.engine.combat.EnemyScaling.ref(band.enemies[rng.nextInt(band.enemies.size)], level) }
        val line = band.lines.takeIf { it.isNotEmpty() }?.let { it[rng.nextInt(it.size)] }
        s = s.copy(rngState = rng.state, vars = s.vars + (PENDING_TOWER_VAR to floor))
        val notices = mutableListOf(Notice(NoticeKind.QUEST, texts.format("kule_kat", "n" to floor, "level" to level), listOfNotNull(line)))
        val started = combat.start(s, CombatSpec(enemies, environment = band.environment, win = WorldState.HUB, flee = WorldState.HUB))
        return resolvedNow(started.state, notices + started.notices)
    }

    /**
     * Perde 2+ bölge düşmanları dünyanın seviyesine ölçeklenir (gün ilerledikçe yükselir; Kule'yi hızla tırmanan
     * oyuncu onları ezer: OP MC hakimiyet evresi). Kapılar dünyanın biraz üstündedir.
     */
    private fun scaledRef(ref: String, state: WorldState, gate: Boolean = false): String {
        val region = state.region?.let { content.regions[it] }
        if (region == null || region.act <= 1 || '@' in ref) return ref
        val base = content.enemies[ref] ?: return ref
        val world = Progression.worldLevel(state.day) * (if (gate) GATE_LEVEL_FACTOR else 1.0) * (1 + (state.regions[region.id]?.danger ?: 0) / 400.0)
        // Geride kalan oyuncu sıkışmasın: dünya, oyuncunun en fazla birkaç seviye üstüne çıkar (DECISIONS #111).
        val cap = state.player.level + if (gate) GATE_PLAYER_MARGIN else REGION_PLAYER_MARGIN
        return com.yukselis.engine.combat.EnemyScaling.ref(ref, minOf(world.toInt(), cap).coerceAtLeast(1))
    }

    /** Yolculuk: komşu ve koşulu tutan, bu perdede canlı bölgeler. */
    fun canTravel(state: WorldState, region: String): Boolean {
        val here = state.region?.let { content.regions[it] } ?: return false
        val def = content.regions[region] ?: return false
        return region in here.neighbors && def.liveIn(state.act) && (def.condition?.test(state) ?: true)
    }

    private fun Result.toResult() = this

    private fun spend(state: WorldState): WorldState =
        state.copy(dayState = state.dayState.copy(slotsLeft = (state.dayState.slotsLeft - 1).coerceAtLeast(0)))

    /** Olayı başlatır: giriş sahnesine geçer, görüldü olarak işaretler, gerilim/etiket geçmişini yazar. */
    fun startEvent(state: WorldState, eventId: String, consumeSlot: Boolean, focus: String? = null): Result {
        val e = content.events[eventId] ?: throw ActionRejected("Tanımsız olay: $eventId")
        var s0 = if (consumeSlot) spend(state) else state
        // Olaydaki NPC'lerle temas: tanınırlar, ihmal sayacı sıfırlanır. Odak: tohumun NPC'si ya da olayın ilk NPC'si.
        e.npcs.forEach { s0 = effects.npc.touch(s0, it) }
        s0 = s0.copy(focusNpc = focus ?: e.npcs.firstOrNull())
        val d = s0.dayState
        val s = s0.copy(
            sceneId = e.entry,
            flags = s0.flags + "olay_${e.id}",
            dayState = d.copy(
                currentEvent = e.id,
                seen = d.seen + e.id,
                hand = emptyList(),
                recentTension = (d.recentTension + e.tension).takeLast(Director.RECENT_TENSION),
                recentTags = (d.recentTags + e.tags).takeLast(Director.RECENT_TAGS),
            ),
        )
        return Result(s, emptyList())
    }

    private fun explore(state: WorldState): Result {
        var s = spend(state)
        val notices = mutableListOf<Notice>()
        val prog = effects.applyAll(s, listOf(Effect.DailyProgress("kesif", 1)))
        s = prog.state
        notices += prog.notices
        val rng = Rng(s.rngState)
        // Önce bölgeye uygun bir keşif olayı; yoksa karşılaşma ya da toplama.
        val event = director.deal(s, rng, n = 1, tag = "kesif").firstOrNull()
        s = s.copy(rngState = rng.state)
        if (event != null && rng.chance(EXPLORE_EVENT_CHANCE)) {
            s = s.copy(rngState = rng.state)
            val started = startEvent(s, event, consumeSlot = false)
            return Result(started.state, notices + started.notices)
        }
        val def = s.region?.let { content.regions[it] }
        val danger = s.region?.let { s.regions[it]?.danger } ?: 0
        val fight = def != null && def.enemies.isNotEmpty() && rng.chance(0.45 + danger / 300.0)
        s = s.copy(rngState = rng.state)
        if (fight) {
            val n = if (rng.chance(0.3)) 2 else 1
            val enemies = List(n) { scaledRef(def!!.enemies[rng.nextInt(def.enemies.size)], s) }
            s = s.copy(rngState = rng.state)
            notices += Notice(NoticeKind.WARNING, texts.formatL("kesif_karsilasma", "region" to regionName(s.region!!)))
            val env = def!!.environment ?: ENV_BY_REGION[s.region] ?: "karanlik"
            val started = combat.start(s, CombatSpec(enemies, environment = listOf(env), win = WorldState.HUB, flee = WorldState.HUB))
            return resolvedNow(started.state, notices + started.notices)
        }
        val found = def?.forage?.takeIf { it.isNotEmpty() }?.let { it[rng.nextInt(it.size)] }
        s = s.copy(rngState = rng.state)
        val reward = listOfNotNull(found?.let { Effect.AddItem(it, 1 + rng.nextInt(2)) }, Effect.GainXp(10))
        s = s.copy(rngState = rng.state)
        val r = effects.applyAll(s, reward)
        notices += Notice(NoticeKind.SYSTEM, texts.format("kesif_sakin"))
        return finishSlot(r.state, notices + r.notices)
    }

    private fun train(state: WorldState): Result {
        val r = effects.applyAll(spend(state), listOf(Effect.DailyProgress("antrenman", 1), Effect.GainXp(5)))
        return finishSlot(r.state, listOf(Notice(NoticeKind.SYSTEM, texts.format("antrenman_yapildi"))) + r.notices)
    }

    private fun rest(state: WorldState): Result {
        val p = state.player
        val healed = spend(state).copy(
            player = p.copy(hp = (p.hp + p.maxHp * 4 / 10).coerceAtMost(p.maxHp), mp = (p.mp + p.maxMp * 4 / 10).coerceAtMost(p.maxMp)),
        )
        // Göze batmamak Dikkat'i biraz düşürür.
        val a = effects.shiftAttention(healed, -1)
        return finishSlot(a.state, listOf(Notice(NoticeKind.SYSTEM, texts.format("dinlenildi"))) + a.notices)
    }

    private fun travel(state: WorldState, region: String): Result {
        val here = state.region?.let { content.regions[it] }
        if (region !in content.regions) throw ActionRejected("Tanımsız bölge")
        if (here != null && !canTravel(state, region)) throw ActionRejected("Bu bölgeye şu an gidilemez")
        val moved = spend(state).copy(region = region)
        return finishSlot(moved, listOf(Notice(NoticeKind.SYSTEM, texts.formatL("yolculuk", "region" to regionName(region)))))
    }

    private fun enterGate(state: WorldState, uid: Int): Result {
        val region = state.region?.let { state.regions[it] } ?: throw ActionRejected("Bölge yok")
        val gate = region.gates.firstOrNull { it.uid == uid } ?: throw ActionRejected("Kapı yok")
        val s = spend(state).copy(vars = state.vars + (PENDING_GATE_VAR to uid))
        val started = combat.start(s, CombatSpec(gate.enemies, environment = listOf("dar_gecit"), win = WorldState.HUB, flee = WorldState.HUB))
        return resolvedNow(started.state, listOf(Notice(NoticeKind.WARNING, texts.format("kapiya_girildi"))) + started.notices)
    }

    /** Ezme anında biten savaş: kapanış/ödül işlemleri hemen yapılır. */
    private fun resolvedNow(state: WorldState, notices: List<Notice>): Result {
        if (state.combat != null) return Result(state, notices)
        val back = afterCombat(state)
        return Result(back.state, notices + back.notices)
    }

    private fun camp(state: WorldState): Result {
        val p = state.player
        var s = state.copy(player = p.copy(hp = (p.hp + p.maxHp / 2).coerceAtMost(p.maxHp), mp = p.maxMp))
        val notices = mutableListOf(Notice(NoticeKind.SYSTEM, texts.format("kamp")))
        val day = startDay(s, advance = true)
        s = day.state
        notices += day.notices
        val hub = enterHub(s)
        return Result(hub.state, notices + hub.notices)
    }

    /** Yuva bitti: zorunlu olay kontrolü ve yeni el. */
    private fun finishSlot(state: WorldState, notices: List<Notice>): Result {
        val hub = enterHub(state)
        return Result(hub.state, notices + hub.notices)
    }

    /**
     * Savaş gün akışına döndü: kapı savaşıysa ve kazanıldıysa kapı kapanır (İlk Kapı Kapatan, Öz Kristali,
     * kapı türü Günlük Görev). Sonra akışa girilir.
     */
    fun afterCombat(state: WorldState): Result {
        var s = state
        val notices = mutableListOf<Notice>()
        val gateUid = s.variable(PENDING_GATE_VAR)
        if (gateUid > 0) {
            s = s.copy(vars = s.vars - PENDING_GATE_VAR)
            if (s.variable(CombatEngine.CTX_WON) == 1) {
                val regionId = s.region
                val region = regionId?.let { s.regions[it] }
                val gate = region?.gates?.firstOrNull { it.uid == gateUid }
                if (region != null && gate != null) {
                    s = s.copy(regions = s.regions + (region.id to region.copy(gates = region.gates - gate, danger = (region.danger - 10).coerceAtLeast(0))))
                    val r = effects.applyAll(
                        s,
                        listOf(Effect.DailyProgress("kapi", 1), Effect.AddCrystals(1 + gate.grade.ordinal * 2), Effect.AddVar(GATES_CLOSED_VAR, 1)),
                    )
                    s = r.state
                    notices += Notice(NoticeKind.QUEST, texts.format("kapi_kapandi"))
                    notices += r.notices
                    val t = effects.grantTitleIfDefined(s, FIRST_GATE_TITLE)
                    s = t.state
                    notices += t.notices
                }
            }
        }
        val floor = s.variable(PENDING_TOWER_VAR)
        if (floor > 0) {
            s = s.copy(vars = s.vars - PENDING_TOWER_VAR)
            if (s.variable(CombatEngine.CTX_WON) == 1) {
                val r = effects.applyAll(s, listOf(Effect.TowerFloor(floor), Effect.AddCrystals(1 + floor / 10)))
                s = r.state
                notices += Notice(NoticeKind.QUEST, texts.format("kule_kat_temiz", "n" to floor))
                notices += r.notices
            }
        }
        if (s.variable(CombatEngine.CTX_WON) == 1) {
            val r = effects.applyAll(s, listOf(Effect.DailyProgress("savas", 1)))
            s = r.state
            notices += r.notices
        }
        val hub = enterHub(s)
        return Result(hub.state, notices + hub.notices)
    }

    fun regionName(id: String): LText =
        content.names.find(com.yukselis.engine.i18n.NameEntry.Kind.PLACE, id)?.full ?: LText(id, id)

    companion object {
        const val DAILY_WIDE_FLAG = "gunluk_genis_gorevler"
        const val OVERFLOW_VAR = "kapi_tasma_sayisi"
        const val GATES_CLOSED_VAR = "kapatilan_kapi"
        const val PENDING_GATE_VAR = "_bekleyen_kapi"
        const val PENDING_TOWER_VAR = "_bekleyen_kat"

        /** Kapı düşmanları dünya seviyesinin bu katı. */
        const val GATE_LEVEL_FACTOR = 1.15
        const val REGION_PLAYER_MARGIN = 2
        const val GATE_PLAYER_MARGIN = 5
        const val FIRST_GATE_TITLE = "ilk_kapi_kapatan"
        const val OVERFLOW_DANGER = 15
        const val MAX_GATES = 3
        const val GATE_MIN_DAYS = 3
        const val GATE_MAX_DAYS = 6
        const val EXPLORE_EVENT_CHANCE = 0.5

        /** Keşif savaşında bölgeye göre ortam etiketi. */
        val ENV_BY_REGION = mapOf("ogretici_orman" to "karanlik", "harabe_carsi" to "tavan", "kirik_istasyon" to "su")
    }
}
