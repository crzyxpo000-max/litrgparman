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

    /** Bölümün ana operasyonuna gir (üste; `variant`: Bölüm 11'de iki dünyadan hangisi). */
    data class EnterOperation(val variant: Int = 0) : HubAction

    /** Keşif görevinden üsse dön. */
    data object ReturnToBase : HubAction

    /** Bölümün bir hazırlık eylemini yap (üste, operasyondan önce). */
    data class Prepare(val id: String) : HubAction

    /** Antrenman / zaman atlama: üste `days` günü bir odakla geçir (DECISIONS #142). */
    data class Skip(val focus: SkipFocus, val days: Int) : HubAction
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
        var s = effects.npc.ensure(
            state.copy(sceneId = WorldState.HUB, dayState = state.dayState.copy(currentEvent = null, reminder = null), focusNpc = null),
        )
        val notices = mutableListOf<Notice>()
        if (s.regions.isEmpty()) {
            val setup = setupWorld(s)
            s = setup.state
            notices += setup.notices
        }
        // Kuruyan tohumlar
        val withered = director.witheredSeeds(s)
        if (withered.isNotEmpty()) s = s.copy(seeds = s.seeds - withered.toSet())
        // Saha kampı sahnesi bitti: ertelenen gece şimdi işlenir.
        if (s.variable(CAMP_PENDING_VAR) > 0 && s.combat == null) {
            val n = night(s.copy(vars = s.vars - CAMP_PENDING_VAR))
            return Result(n.state, notices + n.notices)
        }
        if (chapterMode) return chapterHub(s, notices)
        // Bölüm yapısı olmayan içerik (küçük test içerikleri): eski gün akışı.
        // Zorunlu olay: yuva harcamadan değil, bir yuva yerine gelir (kalmadıysa yine gelir: hikaye beklemez).
        director.forced(s)?.let { f ->
            if (f.seed != null) s = s.copy(seeds = s.seeds.filterNot { it == f.seed })
            // Zorunlu olay yuva harcamaz: eylemin yuvası zaten harcandı (DECISIONS #73).
            val started = startEvent(s, f.eventId, consumeSlot = false, focus = f.seed?.npc, reminder = reminderOf(s, f.seed))
            return Result(started.state, notices + started.notices)
        }
        return Result(dealHand(s), notices)
    }

    // ---------------------------------------------------------------- bölüm yapısı (DECISIONS #130)

    private val chapters get() = content.chapters
    private val chapterMode get() = !content.chapters.isEmpty

    fun base(state: WorldState): String? = director.base(state)

    /** Koşullu ana yay düğümleri (bölümün brifingi/operasyonu/darboğazı olmayanlar): bölümü ve operasyondan önce mi. */
    private val arcNodes: List<Pair<EventDef, Pair<Int, Boolean>>> by lazy {
        content.events.values.filter { it.mainArcDay != null && it.id !in chapters.managed }.mapNotNull { e ->
            val ch = chapters.forDay(e.mainArcDay!!) ?: return@mapNotNull null
            e to (ch.no to (e.mainArcDay <= ch.deadline))
        }.sortedBy { it.first.mainArcDay }
    }

    private fun queued(s: WorldState, queue: List<String>): String? =
        queue.firstOrNull { id -> content.events[id]?.let { director.eligibleQueued(s, it) } == true }

    /**
     * Üs ve görev akışı. Görevde: operasyon/yan görev kuyruğu, keşifte bölgenin olayları. Üste: bölüm ilerlemesi
     * (brifing → operasyon → sonrası → darboğaz), koşullu ana yay düğümleri, tohumlar; ardından fırsat eli.
     */
    private fun chapterHub(state: WorldState, notices: MutableList<Notice>): Result {
        var s = state
        val home = base(s)
        val m0 = s.mission
        // Olay oyuncuyu başka bir bölgeye götürdü (`bolge` etkisi): o bölgeye bir keşif görevi başlar (varış sahnesiyle).
        if (m0 == null && home != null && s.region != null && s.region != home && s.region in content.regions) {
            val target = s.region!!
            // Yolculuğu olay kendisi anlattı: varış sahnesi yok.
            return beginMission(s.copy(region = home), MissionKind.EXPLORE, target, emptyList(), consumeSlot = false, notices = notices, arrival = false)
        }
        if (m0 != null && s.region != null && s.region != m0.region) {
            s = if (m0.kind == MissionKind.EXPLORE && s.region == home) {
                returnHome(s, auto = true, notices = notices)
            } else {
                s.copy(mission = m0.copy(region = s.region!!))
            }
        }
        s.mission?.let { m ->
            when (m.kind) {
                MissionKind.OPERATION -> {
                    val next = queued(s, m.queue)
                    if (next != null) {
                        val rest = m.queue.drop(m.queue.indexOf(next) + 1)
                        val started = startEvent(s.copy(mission = m.copy(queue = rest)), next, consumeSlot = false)
                        return Result(started.state, notices + started.notices)
                    }
                    // Operasyon bitti: sonrası üste.
                    val title = chapterTitle(s)
                    s = returnHome(s.copy(chapterPhase = ChapterPhase.AFTER), auto = true, notices = notices)
                    notices += Notice(NoticeKind.QUEST, texts.formatL("operasyon_bitti", "title" to title))
                }
                MissionKind.SIDE -> {
                    val next = queued(s, m.queue)
                    if (next != null) {
                        val seed = s.seeds.firstOrNull { it.eventId == next && it.dueDay <= s.day }
                        val left = m.copy(queue = m.queue.drop(m.queue.indexOf(next) + 1))
                        val started = startEvent(
                            s.copy(mission = left, seeds = if (seed != null) s.seeds - seed else s.seeds), next,
                            consumeSlot = false, focus = seed?.npc, reminder = reminderOf(s, seed),
                        )
                        return Result(started.state, notices + started.notices)
                    }
                    s = returnHome(s, auto = true, notices = notices)
                }
                MissionKind.EXPLORE -> {
                    if (!s.dayState.forcedLock) {
                        director.dueSeed(s)?.let { seed ->
                            val started = startEvent(s.copy(seeds = s.seeds - seed), seed.eventId, consumeSlot = false, focus = seed.npc, reminder = reminderOf(s, seed))
                            return Result(started.state, notices + started.notices)
                        }
                    }
                    return Result(dealHand(s), notices)
                }
            }
        }
        // ---- üs ----
        s = advanceChapter(s, notices)
        baseForced(s, s.variable(OP_REQUEST_VAR))?.let { return Result(it.state, notices + it.notices) }
        return Result(dealHand(s.copy(chapterPhase = phaseAfterChecks(s))), notices)
    }

    /** Brifingi biten bölüm hazırlığa geçer; darboğazı kalmayan bölüm biter. */
    private fun phaseAfterChecks(s: WorldState): ChapterPhase {
        val ch = chapters.get(s.chapter) ?: return s.chapterPhase
        return when (s.chapterPhase) {
            ChapterPhase.BRIEF -> if (ch.brief.none { pendingEvent(s, it) }) ChapterPhase.PREP else ChapterPhase.BRIEF
            ChapterPhase.AFTER -> if ((ch.bottleneckDay ?: 0) <= s.day && ch.bottleneck.none { pendingEvent(s, it) } &&
                arcNodes.none { (e, at) -> at.first == ch.no && !at.second && director.eligibleQueued(s, e) }
            ) ChapterPhase.DONE else ChapterPhase.AFTER
            else -> s.chapterPhase
        }
    }

    private fun pendingEvent(s: WorldState, id: String): Boolean =
        content.events[id]?.let { director.eligibleQueued(s, it) } == true

    /**
     * Üste zorunlu olan bir sonraki adım (yoksa null). Oyuncu bir eylem yapmadan arka arkaya ikinci zorunlu olay
     * gelmez (`forcedLock`); operasyon isteği ve son tarih bunu beklemez. `request`: operasyon varyantı + 1.
     */
    private fun baseForced(state: WorldState, request: Int): Result? {
        var s = state
        val lock = s.dayState.forcedLock
        val ch = chapters.get(s.chapter)
        if (ch != null) {
            if (s.chapterPhase == ChapterPhase.BRIEF && !lock) {
                ch.brief.firstOrNull { pendingEvent(s, it) }?.let { return startHere(s, content.events.getValue(it)) }
                // (yazılmamış brifing olayı atlanır: pendingEvent içerikte olmayanı false sayar)
                s = s.copy(chapterPhase = ChapterPhase.PREP)
            }
            if (s.chapterPhase == ChapterPhase.AFTER && !lock) {
                arcNodes.firstOrNull { (e, at) -> at.first == ch.no && !at.second && director.eligibleQueued(s, e) }
                    ?.let { return startHere(s, it.first) }
                if ((ch.bottleneckDay ?: 0) <= s.day) {
                    val neck = ch.bottleneck.firstOrNull { pendingEvent(s, it) }
                    s = s.copy(chapterPhase = ChapterPhase.DONE)
                    if (neck != null) {
                        val started = startHere(s, content.events.getValue(neck))
                        return Result(started.state, listOf(Notice(NoticeKind.QUEST, texts.formatL("darbogaz", "title" to ch.title))) + started.notices)
                    }
                }
            }
            // Son tarih günü operasyon kendiliğinden başlar (taşma, son tarih geçince sabah işlenir).
            val overdue = s.day >= ch.deadline
            // Oyuncu istediyse hemen; son tarih geldiyse oyuncu bir eylem yaptıktan sonra (olayın hemen ardına düşmesin).
            if ((s.chapterPhase == ChapterPhase.PREP || s.chapterPhase == ChapterPhase.BRIEF) && (request > 0 || (overdue && !lock))) {
                // Operasyon öncesi düğümler (ör. savaştan önceki gece) operasyondan önce gelir.
                arcNodes.firstOrNull { (e, at) -> at.first == ch.no && at.second && director.eligibleQueued(s, e) }
                    ?.let { return startHere(s, it.first) }
                val variant = (request - 1).coerceIn(0, (ch.operations.size - 1).coerceAtLeast(0))
                return startOperation(s.copy(vars = s.vars - OP_REQUEST_VAR), ch, variant)
            }
        }
        if (lock) return if (s != state) Result(dealHand(s), emptyList()) else null
        // Koşullu ana yay düğümleri: geçmiş bölümlerden kalan ya da bu bölümün operasyon öncesi (günü geldiyse).
        arcNodes.firstOrNull { (e, at) ->
            (at.first < s.chapter || (at.first == s.chapter && at.second && s.day >= e.mainArcDay!!)) && director.eligibleQueued(s, e)
        }?.let { return startHere(s, it.first) }
        director.dueSeed(s)?.let { seed ->
            return startEvent(s.copy(seeds = s.seeds - seed), seed.eventId, consumeSlot = false, focus = seed.npc, reminder = reminderOf(s, seed))
        }
        // Başka bölgede geçen tohum uzun süre oynanmadıysa (hikaye ipliği kaybolmasın) oraya bir yan görev açılır.
        director.dueElsewhere(s).firstOrNull { it.dueDay <= s.day - SEED_PATIENCE_DAYS }?.let { seed ->
            content.events[seed.eventId]?.let { e -> missionRegionOf(s, e) }?.let { region ->
                return beginMission(s, MissionKind.SIDE, region, listOf(seed.eventId), consumeSlot = false)
            }
        }
        return if (s != state) Result(dealHand(s), emptyList()) else null
    }

    /** Üste zorunlu gelen düğüm: burada geçiyorsa hemen, başka bölgedeyse oraya bir yan görev (varış + olay). */
    private fun startHere(s: WorldState, e: EventDef): Result {
        if (director.fitsLocation(s, e)) return startEvent(s, e.id, consumeSlot = false)
        val region = e.where.firstOrNull { it in content.regions && it != base(s) } ?: return startEvent(s, e.id, consumeSlot = false)
        return beginMission(s, MissionKind.SIDE, region, listOf(e.id), consumeSlot = false)
    }

    /** Bölüm bittiyse ve sonrakinin açılış günü geldiyse sonraki bölüm açılır. */
    private fun advanceChapter(state: WorldState, notices: MutableList<Notice>): WorldState {
        var s = state
        while (true) {
            val next = chapters.next(s.chapter) ?: return s
            val cur = chapters.get(s.chapter)
            // Takılmaya karşı: bölüm sonundan epey sonra hâlâ bitmediyse yine de ilerlenir.
            val stuck = cur != null && s.day > cur.end + STUCK_DAYS
            if (s.day < next.open || (s.chapterPhase != ChapterPhase.DONE && !stuck && cur != null)) return s
            s = s.copy(chapter = next.no, chapterPhase = ChapterPhase.BRIEF, vars = s.vars - OP_REQUEST_VAR)
            // Yeni perdenin üssü (Öğretici kampı → Sığınak): üsteyken oyuncu oraya geçer.
            if (s.mission == null) base(s)?.let { s = s.copy(region = it) }
            notices += Notice(NoticeKind.QUEST, texts.formatL("bolum_basladi", "n" to LText("${next.no}", "${next.no}"), "title" to next.title), listOf(next.goal))
        }
    }

    private fun chapterTitle(s: WorldState): LText = chapters.get(s.chapter)?.title ?: LText.EMPTY

    /** Operasyon görevi: bölgeye yolculuk + varış, sonra operasyonun olayları sırayla. */
    private fun startOperation(state: WorldState, ch: ChapterDef, variant: Int): Result {
        val op = ch.operations.getOrNull(variant) ?: return Result(state.copy(chapterPhase = ChapterPhase.AFTER), emptyList())
        val region = operationRegion(state, op)
        val notices = mutableListOf(Notice(NoticeKind.QUEST, texts.formatL("operasyon_basladi", "title" to (if (op.title == LText.EMPTY) ch.title else op.title))))
        // Hazırlıklar işe yarıyor: her yapılmış hazırlığın operasyon etkileri (daha az kayıp, birlik, tam can…).
        var s = state.copy(chapterPhase = ChapterPhase.OPERATION)
        ch.preparations.filter { s.hasFlag(it.flag) }.forEach { pr ->
            val r = effects.applyAll(s, pr.operationEffects)
            s = r.state
            notices += Notice(NoticeKind.QUEST, texts.formatL("hazirlik_ise_yaradi", "name" to pr.name))
            notices += r.notices
        }
        return beginMission(s, MissionKind.OPERATION, region, op.events, consumeSlot = false, notices = notices, variant = variant)
    }

    fun operationRegion(state: WorldState, op: OperationDef): String =
        op.region ?: op.events.firstNotNullOfOrNull { id -> content.events[id]?.where?.firstOrNull { it in content.regions } }
            ?: base(state) ?: state.region ?: ""

    /** Görev başlar: bölgeye geçilir, varış sahnesi (varsa ve üs bölgesi değilse) ilk sahnedir. */
    private fun beginMission(
        state: WorldState,
        kind: MissionKind,
        region: String,
        queue: List<String>,
        consumeSlot: Boolean,
        notices: MutableList<Notice> = mutableListOf(),
        variant: Int = 0,
        arrival: Boolean = true,
    ): Result {
        var s = if (consumeSlot) spend(state) else state
        s = s.copy(mission = Mission(kind, region, queue, s.day, variant), region = region)
        val arrivalEvent = ChapterTable.ARRIVAL_PREFIX + region
        // Varış sahnesi bölge başına günde bir kez (aynı gün ikinci gidişte yolculuk satırı yeter).
        val arrivedToday = ARRIVAL_DAY_PREFIX + region
        val fresh = s.vars[arrivedToday] != s.day
        if (arrival && fresh && region != base(s) && arrivalEvent in content.events) {
            s = s.copy(vars = s.vars + (arrivedToday to s.day))
            val started = startEvent(s, arrivalEvent, consumeSlot = false)
            return Result(started.state, notices + started.notices)
        }
        if (region != base(s)) notices += Notice(NoticeKind.SYSTEM, texts.formatL("yolculuk", "region" to regionName(region)))
        return chapterHub(s.copy(dayState = s.dayState.copy(forcedLock = true)), notices)
    }

    /** Üsse dönüş: görev biter, bölge üs olur. Kendiliğinden dönüşte (görev bitti) zorunlu olay kilidi kapalı kalır. */
    private fun returnHome(state: WorldState, auto: Boolean, notices: MutableList<Notice>): WorldState {
        val m = state.mission ?: return state
        val home = base(state) ?: m.region
        val visits = ChapterTable.VISIT_PREFIX + m.region
        if (m.region != home) notices += Notice(NoticeKind.SYSTEM, texts.formatL("usse_donus", "region" to regionName(home)))
        return state.copy(
            mission = null,
            region = home,
            vars = state.vars + (visits to state.variable(visits) + 1),
            dayState = state.dayState.copy(forcedLock = state.dayState.forcedLock || auto),
        )
    }

    /** Tohumun hatırlatma cümlesi: tohum etkisindeki `hatirlatma`, yoksa onu eken olayın kart başlığından genel cümle. */
    private fun reminderOf(state: WorldState, seed: Seed?): LText? {
        seed ?: return null
        seed.reminder?.let { return it }
        if (!chapterMode) return null
        val origin = seed.origin?.takeIf { it != seed.eventId }?.let { content.events[it] } ?: return null
        return texts.formatL("tohum_hatirlatma", "olay" to origin.card)
    }

    private fun missionRegionOf(state: WorldState, e: EventDef): String? =
        director.missionRegion(state, e) { r -> canTravel(state, r) && (e.regions.isEmpty() || r in e.regions) }

    /** Fırsat eli: buraya uyan olaylar; üste ayrıca başka bölgelerde geçen görev kartları (vadesi gelmiş tohumlar önce). */
    private fun dealHand(state: WorldState): WorldState {
        if (state.dayState.slotsLeft <= 0 || state.mission?.kind == MissionKind.OPERATION) {
            return state.copy(dayState = state.dayState.copy(hand = emptyList()))
        }
        val rng = Rng(state.rngState)
        val hand = director.deal(state, rng).toMutableList()
        if (chapterMode && state.mission == null) {
            val elsewhere = director.dueElsewhere(state).map { it.eventId }.distinct()
                .filter { id -> content.events[id]?.let { missionRegionOf(state, it) } != null }
                .take(MISSION_CARDS)
            hand += elsewhere
            val more = MISSION_CARDS - elsewhere.size
            if (more > 0) {
                hand += director.deal(state, rng, n = more, anywhere = true) { e ->
                    e.id !in hand && !director.fitsLocation(state, e) && missionRegionOf(state, e) != null
                }
            }
        }
        return state.copy(rngState = rng.state, dayState = state.dayState.copy(hand = hand.distinct()))
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
        // Bölüm yapısı: gün akışı Bölüm 1'in brifingiyle açılır (üs: perdenin üs bölgesi).
        if (chapterMode) {
            val first = chapters.forDay(s.day)
            s = s.copy(chapter = first?.no ?: 1, chapterPhase = ChapterPhase.BRIEF, region = base(s) ?: s.region, mission = null)
        }
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
        if (chapterMode && advance) {
            val d = deadlineCheck(s)
            s = d.state
            notices += d.notices
        }
        return Result(s, notices)
    }

    /**
     * Son tarih (DECISIONS #130): operasyona girilmeden son tarih geçerse taşma yaşanır (tehlike, hayatta kalan, bayrak)
     * ve oyuncu görevdeyse üsse çağrılır; operasyon o gün kendiliğinden başlar. Son üç gün her sabah uyarı.
     */
    private fun deadlineCheck(state: WorldState): Result {
        var s = state
        val ch = chapters.get(s.chapter) ?: return Result(s, emptyList())
        val waiting = s.chapterPhase == ChapterPhase.BRIEF || s.chapterPhase == ChapterPhase.PREP
        if (!waiting) return Result(s, emptyList())
        val notices = mutableListOf<Notice>()
        // Operasyon günü geldi: keşifteki oyuncu üsse çağrılır. Yan görev kesilmez; bitince operasyon başlar.
        if (ch.deadline - s.day <= 0 && s.mission?.kind == MissionKind.EXPLORE) {
            notices += Notice(NoticeKind.WARNING, texts.formatL("operasyon_gunu", "title" to ch.title))
            s = returnHome(s, auto = false, notices = notices)
        }
        val left = ch.deadline - s.day
        if (left in 0..DEADLINE_WARNING_DAYS && ch.earlyEntry) {
            notices += Notice(NoticeKind.WARNING, texts.formatL("bolum_son_tarih", "title" to ch.title, "n" to LText("$left", "$left")))
        }
        val flag = ChapterTable.overflowFlag(ch.no)
        // Tarihli operasyon oyuncunun seçemediği bir gündür: gecikmesi (zorunlu olaylar yüzünden) taşma sayılmaz.
        if (left < 0 && !s.hasFlag(flag) && ch.earlyEntry) {
            val region = ch.operations.firstOrNull()?.let { operationRegion(s, it) }
            val list = listOfNotNull(
                Effect.SetFlag(flag),
                Effect.AddVar(SURVIVORS_VAR, ChapterTable.OVERFLOW_SURVIVORS),
                region?.takeIf { it in s.regions }?.let { Effect.ShiftDanger(it, ChapterTable.OVERFLOW_DANGER) },
            ) + ch.overflowEffects
            val r = effects.applyAll(s, list)
            s = r.state
            notices += Notice(NoticeKind.WARNING, texts.formatL("bolum_tasti", "title" to ch.title), listOf(ch.overflowText).filter { it != LText.EMPTY })
            notices += r.notices
            if (s.mission?.kind == MissionKind.EXPLORE) {
                notices += Notice(NoticeKind.WARNING, texts.format("gorev_yarida"))
                s = returnHome(s, auto = false, notices = notices)
            }
        }
        return Result(s, notices)
    }

    // ---------------------------------------------------------------- eylemler

    fun act(state: WorldState, action: HubAction): Result {
        if (state.sceneId != WorldState.HUB) throw ActionRejected("Gün akışında değilsin")
        if (state.combat != null) throw ActionRejected("Savaş sürüyor")
        if (state.prompts.isNotEmpty()) throw ActionRejected("Önce Sistem istemi")
        val slots = state.dayState.slotsLeft
        if (action != HubAction.Camp && action != HubAction.ReturnToBase && slots <= 0) throw ActionRejected("Bugünün yuvaları bitti; kamp kur")
        if (chapterMode) checkChapterAction(state, action)
        // Oyuncu bir eylem yaptı: zorunlu olay kilidi açılır.
        val state = if (state.dayState.forcedLock) state.copy(dayState = state.dayState.copy(forcedLock = false)) else state
        // Bölüm açıldı ve brifing bekliyor: üsten ayrılan eylem (yolculuk, görev kartı, operasyon) önce brifingi açar.
        if (chapterMode && state.mission == null && state.chapterPhase == ChapterPhase.BRIEF &&
            (action is HubAction.Travel || action is HubAction.EnterOperation || action is HubAction.Card)
        ) {
            chapters.get(state.chapter)?.brief?.firstOrNull { pendingEvent(state, it) }?.let { return startHere(state, content.events.getValue(it)) }
        }
        return when (action) {
            is HubAction.Card -> {
                if (action.eventId !in state.dayState.hand) throw ActionRejected("Bu kart elde yok")
                val e = content.events.getValue(action.eventId)
                if (chapterMode && !director.fitsLocation(state, e)) {
                    // Başka bir bölgede geçen olay: oraya bir yan görev (varış sahnesi + olay, sonra üsse dönüş).
                    val region = missionRegionOf(state, e) ?: throw ActionRejected("Bu olaya şu an gidilemez")
                    return beginMission(state, MissionKind.SIDE, region, listOf(e.id), consumeSlot = true)
                }
                val seed = state.seeds.firstOrNull { it.eventId == e.id && it.dueDay <= state.day }
                startEvent(
                    if (seed != null) state.copy(seeds = state.seeds - seed) else state, action.eventId, consumeSlot = true,
                    focus = seed?.npc, reminder = reminderOf(state, seed),
                ).toResult()
            }
            is HubAction.EnterOperation -> {
                val r = spend(state).copy(vars = state.vars + (OP_REQUEST_VAR to action.variant + 1))
                finishSlot(r, emptyList())
            }
            is HubAction.Prepare -> prepare(state, action.id)
            is HubAction.Skip -> skip(state, action.focus, action.days)
            HubAction.ReturnToBase -> {
                // Dönüş yolu yuva harcamaz: görevin yolculuğu gidişte ödendi (DECISIONS #130).
                val notices = mutableListOf<Notice>()
                val back = returnHome(state, auto = true, notices = notices)
                finishSlot(back, notices)
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

    /** Bölüm yapısında eylemin yeri: keşif/Kapı/Kule görevde, operasyon ve yolculuk üste. */
    private fun checkChapterAction(state: WorldState, action: HubAction) {
        val m = state.mission
        when (action) {
            HubAction.Explore, is HubAction.EnterGate, HubAction.ClimbTower ->
                if (m?.kind != MissionKind.EXPLORE) throw ActionRejected("Önce bir bölgeye git")
            is HubAction.EnterOperation -> {
                if (m != null) throw ActionRejected("Önce üsse dön")
                if (operationChoices(state).none { it.first == action.variant }) throw ActionRejected("Şu an girilecek operasyon yok")
            }
            HubAction.ReturnToBase -> if (m == null || m.kind == MissionKind.OPERATION) throw ActionRejected("Üsse dönülecek görev yok")
            is HubAction.Prepare -> {
                if (m != null) throw ActionRejected("Hazırlık üste yapılır")
                val pr = preparationChoices(state).firstOrNull { it.id == action.id } ?: throw ActionRejected("Bu hazırlık şu an yapılamaz")
                if (state.dayState.slotsLeft < pr.cost) throw ActionRejected("Bugün bu hazırlık için yeterli vakit yok")
            }
            is HubAction.Travel -> if (m != null) throw ActionRejected("Önce üsse dön")
            is HubAction.Skip -> {
                if (m != null) throw ActionRejected("Zaman atlama üste yapılır")
                if (action.days < 1 || action.days > skipLimit(state)) throw ActionRejected("Bu kadar gün atlanamaz")
                if (action.focus !in skipFoci(state)) throw ActionRejected("Bu odak şu an seçilemez")
            }
            else -> if (m?.kind == MissionKind.OPERATION) throw ActionRejected("Operasyon sürüyor")
        }
    }

    /** Şu an geçerli akış eylemleri (testler, simülatör, arayüz denetimi). Kamp her zaman geçerlidir. */
    fun legalActions(state: WorldState): List<HubAction> {
        if (state.sceneId != WorldState.HUB || state.combat != null || state.prompts.isNotEmpty()) return emptyList()
        if (state.dayState.slotsLeft <= 0) {
            return listOfNotNull(HubAction.Camp, HubAction.ReturnToBase.takeIf { state.mission?.kind == MissionKind.EXPLORE })
        }
        val m = state.mission
        return buildList {
            state.dayState.hand.forEach { add(HubAction.Card(it)) }
            if (m?.kind != MissionKind.OPERATION) { add(HubAction.Train); add(HubAction.Rest) }
            if (!chapterMode) {
                add(HubAction.Explore)
                destinations(state).forEach { add(HubAction.Travel(it)) }
                state.region?.let { state.regions[it] }?.gates?.forEach { add(HubAction.EnterGate(it.uid)) }
                if (towerOpen(state)) add(HubAction.ClimbTower)
            } else if (m == null) {
                destinations(state).forEach { add(HubAction.Travel(it)) }
                operationChoices(state).forEach { add(HubAction.EnterOperation(it.first)) }
                preparationChoices(state).filter { it.cost <= state.dayState.slotsLeft }.forEach { add(HubAction.Prepare(it.id)) }
            } else if (m.kind == MissionKind.EXPLORE) {
                add(HubAction.Explore)
                state.region?.let { state.regions[it] }?.gates?.forEach { add(HubAction.EnterGate(it.uid)) }
                if (towerOpen(state)) add(HubAction.ClimbTower)
                add(HubAction.ReturnToBase)
            }
            add(HubAction.Camp)
        }
    }

    // ---------------------------------------------------------------- zaman atlama (DECISIONS #142)

    /**
     * Üste atlanabilecek en çok gün (0: atlama yok). Hazırlıkta operasyona [TimeSkip.SAFETY_DAYS] gün kalana kadar,
     * sonrasında darboğaz gününe, bölüm bittiyse sonraki bölümün açılışına kadar. Brifing beklerken ve görevde yok.
     */
    fun skipLimit(state: WorldState): Int {
        if (!chapterMode || state.mission != null || state.sceneId != WorldState.HUB) return 0
        val ch = chapters.get(state.chapter) ?: return 0
        val until = when (state.chapterPhase) {
            ChapterPhase.PREP -> ch.deadline - TimeSkip.SAFETY_DAYS
            ChapterPhase.AFTER -> ch.bottleneckDay ?: return 0
            ChapterPhase.DONE -> chapters.next(ch.no)?.open ?: return 0
            else -> return 0
        }
        return (until - state.day).coerceAtLeast(0)
    }

    /** Seçilebilir odaklar: her stat, kapı temizliği, (meslek varsa) meslek, her yaşayan yoldaş. */
    fun skipFoci(state: WorldState): List<SkipFocus> = buildList {
        com.yukselis.engine.system.Stat.entries.forEach { add(SkipFocus.Train(it)) }
        add(SkipFocus.Gates)
        if (state.player.profession != null) add(SkipFocus.Craft)
        effects.npc.companions(state).forEach { add(SkipFocus.Companion(it.id)) }
    }

    /** Atlama sırasında hikaye bir şey bekliyor mu (ana yay düğümü): atlama durur. */
    private fun storyWaiting(s: WorldState): Boolean =
        arcNodes.any { (e, at) ->
            (at.first < s.chapter || (at.first == s.chapter && at.second && s.day >= e.mainArcDay!!)) && director.eligibleQueued(s, e)
        }

    /**
     * Günler tek tek geçer (NPC adımları, kapılar, son tarih denetimi). Günlük Görev bu günlerde cezasız atlanır
     * (seri ne büyür ne kopar). Yeni bir tohumun vakti gelir ya da ana yay düğümü uyarsa atlama durur; ayrıca
     * %25 olasılıkla bir havuz olayı araya girer. Kesintisiz atlama odağın özet olayıyla biter.
     */
    private fun skip(state: WorldState, focus: SkipFocus, days: Int): Result {
        var s = state
        val notices = mutableListOf<Notice>()
        val rng = Rng(s.rngState)
        val cut = if (rng.chance(TimeSkip.INTERRUPT_CHANCE)) rng.nextInt(1, days) else 0
        val variant = rng.nextInt(TimeSkip.VARIANTS)
        s = s.copy(rngState = rng.state)
        val dueBefore = s.seeds.filter { it.dueDay <= s.day }.toSet()
        var passed = 0
        var seedStop: Seed? = null
        var storyStop = false
        while (passed < days) {
            s.daily?.let { if (!it.completed) s = s.copy(daily = it.copy(completed = true)) }
            if (s.day >= 30) s = effects.shiftAttention(s, -EffectApplier.nightDecay(s.attention)).state
            val day = startDay(s, advance = true)
            s = day.state
            passed++
            // Ara günlerin rutin bildirimleri (gün başladı, yeni Günlük Görev) gösterilmez; uyarılar kalır.
            notices += if (passed == days) day.notices else day.notices.filter { it.kind == NoticeKind.WARNING }
            val newSeed = (listOfNotNull(director.dueSeed(s)) + director.dueElsewhere(s)).firstOrNull { it !in dueBefore }
            if (newSeed != null) { seedStop = newSeed; break }
            if (storyWaiting(s)) { storyStop = true; break }
            if (passed == cut) break
        }
        val home = base(s)
        val gains = effects.applyAll(s, TimeSkip.effects(s, focus, passed, home))
        s = gains.state
        notices += gains.notices
        if (focus == SkipFocus.Craft) s.player.profession?.let { prof ->
            var p = prof.copy(xp = prof.xp + TimeSkip.CRAFT_XP_PER_DAY * passed)
            while (p.xp >= Progression.professionXpToNext(p.level)) p = p.copy(xp = p.xp - Progression.professionXpToNext(p.level), level = p.level + 1)
            if (p.level > prof.level) {
                val name = content.professions[p.id]?.name ?: LText(p.id, p.id)
                notices += Notice(NoticeKind.LEVEL_UP, texts.formatL("meslek_seviye", "name" to name, "level" to LText("${p.level}", "${p.level}")))
            }
            s = s.copy(player = s.player.copy(profession = p))
        }
        val p = s.player
        s = s.copy(
            player = p.copy(hp = p.maxHp, mp = p.maxMp),
            vars = s.vars + mapOf(TimeSkip.DAYS_VAR to passed, TimeSkip.VARIANT_VAR to variant, TimeSkip.CUT_VAR to 0),
        )
        notices += Notice(NoticeKind.SYSTEM, texts.format("atlama_bitti", "day" to s.day))
        val summary = texts.format("atlama_kesinti", "day" to s.day)
        // Burada geçen tohum: hemen başlar, hatırlatmasının önüne atlamanın özeti gelir.
        seedStop?.takeIf { seed -> content.events[seed.eventId]?.let { director.fitsLocation(s, it) } == true }?.let { seed ->
            val own = reminderOf(s, seed)
            val reminder = if (own == null) summary else LText(summary.tr + " " + own.tr, summary.en + " " + own.en)
            val started = startEvent(s.copy(seeds = s.seeds - seed), seed.eventId, consumeSlot = false, focus = seed.npc, reminder = reminder)
            return Result(started.state, notices + started.notices)
        }
        if (seedStop == null && !storyStop && passed == cut) {
            val r2 = Rng(s.rngState)
            val e = director.deal(s, r2, n = 1) { director.fitsLocation(s, it) }.firstOrNull()
            s = s.copy(rngState = r2.state)
            if (e != null) {
                val started = startEvent(s, e, consumeSlot = false, reminder = summary)
                return Result(started.state, notices + started.notices)
            }
        }
        // Başka yerde bekleyen bir iş ya da ana yay düğümü: özet sahnesi "yarıda kaldı" satırıyla.
        if (seedStop != null || storyStop) s = s.copy(vars = s.vars + (TimeSkip.CUT_VAR to 1))
        val event = TimeSkip.summaryEvent(focus)
        if (event !in content.events) return finishSlot(s, notices)
        val focusNpc = (focus as? SkipFocus.Companion)?.npc
        val started = startEvent(s, event, consumeSlot = false, focus = focusNpc)
        return Result(started.state, notices + started.notices)
    }

    /** Şu an yapılabilecek hazırlıklar: üste, operasyon beklerken, yapılmamış ve koşulu tutan. */
    fun preparationChoices(state: WorldState): List<PreparationDef> {
        if (!chapterMode || state.mission != null) return emptyList()
        val ch = chapters.get(state.chapter) ?: return emptyList()
        if (state.chapterPhase != ChapterPhase.PREP && state.chapterPhase != ChapterPhase.BRIEF) return emptyList()
        return ch.preparations.filter { !state.hasFlag(it.flag) && (it.condition?.test(state) ?: true) }
    }

    /** Hazırlık: yuva(lar) harcanır, bayrak konur, sonuç satırı gösterilir. */
    private fun prepare(state: WorldState, id: String): Result {
        val pr = preparationChoices(state).first { it.id == id }
        var s = state
        repeat(pr.cost) { s = spend(s) }
        s = s.copy(flags = s.flags + pr.flag)
        return finishSlot(s, listOf(Notice(NoticeKind.QUEST, texts.formatL("hazirlik_yapildi", "name" to pr.name), listOf(pr.result))))
    }

    /** Operasyon yaklaşırken üsten ayrılmak operasyonu geciktirebilir (son tarihe en çok 1 gün; DECISIONS #141). */
    fun deadlineRisk(state: WorldState): Boolean {
        if (!chapterMode || state.mission != null) return false
        val ch = chapters.get(state.chapter) ?: return false
        if (state.chapterPhase != ChapterPhase.PREP && state.chapterPhase != ChapterPhase.BRIEF) return false
        return ch.deadline - state.day <= DEADLINE_RISK_DAYS
    }

    /** Üste girilebilecek operasyonlar: (varyant, bölge). */
    fun operationChoices(state: WorldState): List<Pair<Int, String>> {
        if (!chapterMode || state.mission != null) return emptyList()
        val ch = chapters.get(state.chapter) ?: return emptyList()
        if (state.chapterPhase != ChapterPhase.PREP || !ch.earlyEntry) return emptyList()
        return ch.operations.mapIndexedNotNull { i, op ->
            if (op.events.none { id -> content.events[id]?.let { director.eligibleQueued(state, it) } == true }) null
            else i to operationRegion(state, op)
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
        val started = combat.start(s, CombatSpec(enemies, environment = band.environment, win = WorldState.HUB, flee = WorldState.HUB, lose = WorldState.HUB))
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
        val world = Progression.worldLevel(state.day) * (if (gate) GATE_LEVEL_FACTOR else REGION_LEVEL_FACTOR) * (1 + (state.regions[region.id]?.danger ?: 0) / 400.0)
        // Geride kalan oyuncu sıkışmasın: dünya, oyuncunun en fazla birkaç seviye üstüne çıkar (DECISIONS #111).
        val cap = state.player.level + if (gate) GATE_PLAYER_MARGIN else REGION_PLAYER_MARGIN
        return com.yukselis.engine.combat.EnemyScaling.ref(ref, minOf(world.toInt(), cap).coerceAtLeast(1))
    }

    /** Yolculuk: komşu ve koşulu tutan, bu perdede canlı bölgeler. */
    fun canTravel(state: WorldState, region: String): Boolean {
        val def = content.regions[region] ?: return false
        if (chapterMode) return def.liveIn(state.act) && (def.condition?.test(state) ?: true)
        val here = state.region?.let { content.regions[it] } ?: return false
        return region in here.neighbors && def.liveIn(state.act) && (def.condition?.test(state) ?: true)
    }

    /** Üsten gidilebilecek bölgeler (bölüm yapısı) ya da komşular (eski akış). */
    fun destinations(state: WorldState): List<String> {
        if (!chapterMode) return state.region?.let { content.regions[it] }?.neighbors?.filter { canTravel(state, it) } ?: emptyList()
        if (state.mission != null) return emptyList()
        return content.regions.keys.filter { canTravel(state, it) }
    }

    private fun Result.toResult() = this

    private fun spend(state: WorldState): WorldState =
        state.copy(dayState = state.dayState.copy(slotsLeft = (state.dayState.slotsLeft - 1).coerceAtLeast(0)))

    /** Olayı başlatır: giriş sahnesine geçer, görüldü olarak işaretler, gerilim/etiket geçmişini yazar. */
    fun startEvent(state: WorldState, eventId: String, consumeSlot: Boolean, focus: String? = null, reminder: LText? = null): Result {
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
                // Olaydan sonra oyuncu bir eylem yapmadan yeni zorunlu olay gelmez.
                forcedLock = true,
                reminder = reminder,
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
        val fight = def != null && def.enemies.isNotEmpty() && rng.chance(EXPLORE_FIGHT_CHANCE + danger / 300.0)
        s = s.copy(rngState = rng.state)
        if (fight) {
            val n = if (rng.chance(0.3)) 2 else 1
            val enemies = List(n) { scaledRef(def!!.enemies[rng.nextInt(def.enemies.size)], s) }
            s = s.copy(rngState = rng.state)
            notices += Notice(NoticeKind.WARNING, texts.formatL("kesif_karsilasma", "region" to regionName(s.region!!)))
            val env = def!!.environment ?: ENV_BY_REGION[s.region] ?: "karanlik"
            val started = combat.start(s, CombatSpec(enemies, environment = listOf(env), win = WorldState.HUB, flee = WorldState.HUB, lose = WorldState.HUB))
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
        if (chapterMode) {
            if (!canTravel(state, region)) throw ActionRejected("Bu bölgeye şu an gidilemez")
            return beginMission(state, MissionKind.EXPLORE, region, emptyList(), consumeSlot = true)
        }
        if (here != null && !canTravel(state, region)) throw ActionRejected("Bu bölgeye şu an gidilemez")
        val moved = spend(state).copy(region = region)
        return finishSlot(moved, listOf(Notice(NoticeKind.SYSTEM, texts.formatL("yolculuk", "region" to regionName(region)))))
    }

    private fun enterGate(state: WorldState, uid: Int): Result {
        val region = state.region?.let { state.regions[it] } ?: throw ActionRejected("Bölge yok")
        val gate = region.gates.firstOrNull { it.uid == uid } ?: throw ActionRejected("Kapı yok")
        val s = spend(state).copy(vars = state.vars + (PENDING_GATE_VAR to uid))
        val started = combat.start(s, CombatSpec(gate.enemies, environment = listOf("dar_gecit"), win = WorldState.HUB, flee = WorldState.HUB, lose = WorldState.HUB))
        return resolvedNow(started.state, listOf(Notice(NoticeKind.WARNING, texts.format("kapiya_girildi"))) + started.notices)
    }

    /** Ezme anında biten savaş: kapanış/ödül işlemleri hemen yapılır. */
    private fun resolvedNow(state: WorldState, notices: List<Notice>): Result {
        if (state.combat != null) return Result(state, notices)
        val back = afterCombat(state)
        return Result(back.state, notices + back.notices)
    }

    private fun camp(state: WorldState): Result {
        // Saha kampı (DECISIONS #144): keşif görevinde yanındaki bir yoldaşla bu bölgeye ait kısa bir sahne gece
        // inmeden oynanır; sahne bitince gece işlenir (enterHub → CAMP_PENDING_VAR).
        if (chapterMode && state.mission?.kind == MissionKind.EXPLORE && state.variable(CAMP_PENDING_VAR) == 0) {
            val companions = effects.npc.companions(state).map { it.id }.toSet()
            val scenes = director.campScenes(state, companions)
            if (scenes.isNotEmpty()) {
                val rng = Rng(state.rngState)
                val pick = scenes[rng.nextInt(scenes.size)]
                val s = state.copy(rngState = rng.state, vars = state.vars + (CAMP_PENDING_VAR to 1))
                return startEvent(s, pick.id, consumeSlot = false)
            }
        }
        return night(state)
    }

    /** Gece: kamp dinlenmesi, Dikkat sönmesi, yeni gün. */
    private fun night(state: WorldState): Result {
        val p = state.player
        var s = state.copy(player = p.copy(hp = (p.hp + p.maxHp / 2).coerceAtMost(p.maxHp), mp = p.maxMp))
        val notices = mutableListOf(Notice(NoticeKind.SYSTEM, texts.format("kamp")))
        // Göze batmayan her gece Dikkat biraz söner (DECISIONS #116).
        if (s.day >= 30) s = effects.shiftAttention(s, -EffectApplier.nightDecay(s.attention)).state
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
        // Savaşın hemen ardından zorunlu olay gelmez (STORY_FLOW c).
        if (chapterMode) s = s.copy(dayState = s.dayState.copy(forcedLock = true))
        val hub = enterHub(s)
        return Result(hub.state, notices + hub.notices)
    }

    fun regionName(id: String): LText =
        content.names.find(com.yukselis.engine.i18n.NameEntry.Kind.PLACE, id)?.full ?: LText(id, id)

    companion object {
        const val DAILY_WIDE_FLAG = "gunluk_genis_gorevler"

        /** Oyuncu üste "operasyona gir" dedi (varyant + 1); bir sonraki üs anında operasyon başlar. */
        const val OP_REQUEST_VAR = "_operasyon_istegi"
        const val SURVIVORS_VAR = "hayatta_kalan"
        const val DEADLINE_WARNING_DAYS = 3

        /** Bölüm sonundan bu kadar gün sonra hâlâ bitmemiş bölüm yine de ilerler (takılma koruması). */
        const val STUCK_DAYS = 5

        /** Üsteki fırsat elinde başka bölgede geçen görev kartı sayısı (en çok). */
        const val MISSION_CARDS = 2

        /** Başka bölgede geçen tohum bu kadar gün kart olarak bekledikten sonra yan görev olarak açılır. */
        const val SEED_PATIENCE_DAYS = 6

        /** Son tarihe bu kadar ya da daha az gün kala üsten ayrılmak için uyarı. */
        const val DEADLINE_RISK_DAYS = 1

        /** Varış sahnesinin o gün oynandığı (bölge başına) değişken öneki. */
        const val ARRIVAL_DAY_PREFIX = "_varis_gunu_"

        /** Saha kampı sahnesi oynanıyor; bitince gece işlenecek. */
        const val CAMP_PENDING_VAR = "_kamp_sahnesi"
        const val OVERFLOW_VAR = "kapi_tasma_sayisi"
        const val GATES_CLOSED_VAR = "kapatilan_kapi"
        const val PENDING_GATE_VAR = "_bekleyen_kapi"
        const val PENDING_TOWER_VAR = "_bekleyen_kat"

        /** Kapı düşmanları dünya seviyesinin bu katı. */
        const val GATE_LEVEL_FACTOR = 1.15

        /** Perde 2+ bölge (keşif) düşmanları dünya seviyesinin bu katı: OP MC sıradan karşılaşmaları ezer (QA P1-10). */
        const val REGION_LEVEL_FACTOR = 0.8

        /** Keşifte savaş olasılığı tabanı (tehlikeyle artar). 0,45'te oynanış başına ~520 savaş çıkıyordu (QA P1-11). */
        const val EXPLORE_FIGHT_CHANCE = 0.32
        const val REGION_PLAYER_MARGIN = 2
        const val GATE_PLAYER_MARGIN = 3
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
