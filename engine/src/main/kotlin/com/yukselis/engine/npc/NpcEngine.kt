package com.yukselis.engine.npc

import com.yukselis.engine.core.MoralAxis
import com.yukselis.engine.core.Morality
import com.yukselis.engine.core.Rng
import com.yukselis.engine.core.WorldState
import com.yukselis.engine.i18n.LText
import com.yukselis.engine.i18n.NameEntry
import com.yukselis.engine.story.ChoiceDef
import com.yukselis.engine.story.Content
import com.yukselis.engine.story.Effect
import com.yukselis.engine.story.EffectApplier
import com.yukselis.engine.story.Notice
import com.yukselis.engine.story.NoticeKind
import com.yukselis.engine.story.Scene
import com.yukselis.engine.system.Grade
import com.yukselis.engine.system.Progression
import com.yukselis.engine.system.Stat
import com.yukselis.engine.system.Stats
import com.yukselis.engine.world.QuestStatus
import com.yukselis.engine.world.Seed
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * NPC sistemi (GDD: Dinamik karakterler). Saf: durum alır, yeni durum + bildirimler döndürür.
 *
 *  - Günlük gündem: her NPC her gün bir adım (hedef, seviye, ihmal, hafıza solması, yalanların açığa çıkması).
 *  - Eşik olayları: ihanet, kişisel görev, kaçış/itaat, rakiplik, ayrılma/fraksiyon değişimi.
 *  - Tanıklar: oyuncunun ahlaki seçimleri NPC'nin değerlerine ve kişiliğine göre yorumlanır.
 *  - OP MC: Küresel Sıralama, rakiplerin "İlk" unvanları, yoldaş görüşleri ve güce tepkiler.
 */
class NpcEngine(private val content: Content, private val effects: EffectApplier) {

    private val texts get() = content.texts

    fun name(id: String): LText = content.names.find(NameEntry.Kind.NPC, id)?.short ?: LText(id, id)
    fun fullName(id: String): LText = content.names.find(NameEntry.Kind.NPC, id)?.full ?: name(id)

    private fun r(s: WorldState) = EffectApplier.Result(s, emptyList())

    /** NPC durumunu dönüştürür (yoksa dokunmaz). */
    fun update(state: WorldState, id: String, f: (NpcState) -> NpcState): WorldState {
        val cur = state.npcs[id] ?: return state
        return state.copy(npcs = state.npcs + (id to f(cur)))
    }

    /** İzlerle değişmiş kişilik. */
    fun personality(state: WorldState, id: String): Personality {
        val def = content.npcs[id] ?: return Personality()
        val deltas = mutableMapOf<Trait, Int>()
        state.npcs[id]?.traits?.forEach { t -> content.traits[t]?.personality?.forEach { (k, v) -> deltas[k] = (deltas[k] ?: 0) + v } }
        return def.personality.plus(deltas)
    }

    // ---------------------------------------------------------------- kurulum

    /**
     * İçerikteki her NPC'nin durumu kurulur. Eski kayıttan gelen kısmi durum (yalnızca ilişki farkı)
     * içeriğin başlangıç değerleriyle birleşir.
     */
    fun ensure(state: WorldState): WorldState {
        if (content.npcs.isEmpty()) return state
        var npcs = state.npcs
        for (def in content.npcs.values.sortedBy { it.id }) {
            val cur0 = npcs[def.id] ?: NpcState(def.id)
            // Savaş bloğu olan NPC işaretlenir (`npc_savasabilir` koşulu durumdan okunur).
            val cur = if (def.combat != null && MARK_FIGHTER !in cur0.marks) cur0.copy(marks = cur0.marks + MARK_FIGHTER) else cur0
            if (cur.initialized) {
                if (cur !== cur0) npcs = npcs + (def.id to cur)
                continue
            }
            val base = cur.copy(
                level = def.startLevel.coerceAtLeast(1),
                faction = cur.faction ?: def.faction,
                relation = def.startRelation + cur.relation,
                known = cur.known || def.knownAtStart,
                lastContactDay = state.day,
            )
            npcs = npcs + (def.id to base.copy(goal = pickGoal(def, state, base)))
        }
        return state.copy(npcs = npcs)
    }

    /**
     * İlerleyen hedef: süresi (`gun`) olan, bitmemiş ve koşulu tutan en öncelikli hedef. Süresiz hedefler
     * NPC'nin kalıcı arka plan amacıdır (kişisel görev metni onları anar) ve gündemi tıkamaz.
     */
    private fun pickGoal(def: NpcDef, state: WorldState, npc: NpcState): String? =
        def.goals
            .filter { it.days > 0 && it.id !in npc.doneGoals && (it.condition?.test(state.copy(focusNpc = def.id)) ?: true) }
            .maxByOrNull { it.priority }?.id

    /** Küresel Sıralama'nın prosedürel avcılarını üretir (bir kez, dünya kurulurken). */
    fun setupRanking(state: WorldState): WorldState {
        val def = content.ranking
        if (state.hunters.isNotEmpty() || def.hunters <= 0 || def.firstNames.isEmpty() || def.lastNames.isEmpty()) {
            return updateRanking(state, announce = false).state
        }
        val rng = Rng(state.rngState)
        val hunters = List(def.hunters) { i ->
            Hunter(
                uid = i,
                first = rng.nextInt(def.firstNames.size),
                last = rng.nextInt(def.lastNames.size),
                level = rng.nextInt(def.levelMin, def.levelMax),
                progress = rng.nextInt(100),
                rate = def.rateMin + rng.nextDouble() * (def.rateMax - def.rateMin),
            )
        }
        return updateRanking(state.copy(hunters = hunters, rngState = rng.state), announce = false).state
    }

    // ---------------------------------------------------------------- etkiler

    fun apply(state: WorldState, e: Effect): EffectApplier.Result = when (e) {
        is Effect.ShiftRelation -> shiftRelation(state, e.npc, e.deltas, notify = true)
        is Effect.AddMemory -> withId(state, e.npc) { s, id -> r(remember(s, id, e.memory, e.weight)) }
        is Effect.KillNpc -> withId(state, e.npc) { s, id -> kill(s, id) }
        is Effect.JoinParty -> withId(state, e.npc) { s, id -> join(s, id) }
        is Effect.LeaveParty -> withId(state, e.npc) { s, id -> leave(s, id) }
        is Effect.NpcGone -> withId(state, e.npc) { s, id -> leave(s, id, gone = true) }
        is Effect.SetNpcFaction -> withId(state, e.npc) { s, id -> setFaction(s, id, e.faction) }
        is Effect.MeetNpc -> withId(state, e.npc) { s, id -> r(touch(s, id)) }
        is Effect.NpcLevel -> withId(state, e.npc) { s, id -> r(update(s, id) { it.copy(level = (it.level + e.delta).coerceAtLeast(1)) }) }
        is Effect.NpcMark -> withId(state, e.npc) { s, id -> r(update(s, id) { it.copy(marks = it.marks + e.mark) }) }
        is Effect.NpcTrait -> withId(state, e.npc) { s, id -> npcTrait(s, id, e.trait) }
        is Effect.PlayerTrait -> playerTrait(state, e.trait)
        is Effect.Learn -> learn(state, e.knowledge)
        is Effect.TellLie -> withId(state, e.npc) { s, id -> r(touch(remember(s, id, Memory.LIE_PREFIX + e.topic, LIE_WEIGHT), id)) }
        is Effect.PersonalQuest -> withId(state, e.npc) { s, id -> effects.applyAll(s, listOf(Effect.SetQuest(personalQuestId(id), e.status))) }
        is Effect.NpcReward -> withId(state, e.npc) { s, id -> reward(s, id) }
        is Effect.AddArmy -> addArmy(state, e.unit, e.count)
        is Effect.GrowArmy -> armyUnitFor(state)?.let { addArmy(state, it, e.count) } ?: r(state)
        else -> r(state)
    }

    private fun withId(state: WorldState, raw: String, block: (WorldState, String) -> EffectApplier.Result): EffectApplier.Result {
        val id = state.npcId(raw) ?: return r(state)
        if (id !in state.npcs) return r(state)
        return block(state, id)
    }

    /** NPC ile temas: tanınır, ihmal sayacı sıfırlanır. */
    fun touch(state: WorldState, id: String): WorldState =
        update(state, id) { it.copy(known = true, lastContactDay = state.day, marks = it.marks - MARK_NEGLECT) }

    fun shiftRelation(state: WorldState, raw: String, deltas: Map<RelAxis, Int>, notify: Boolean): EffectApplier.Result {
        val id = state.npcId(raw) ?: return r(state)
        val npc = state.npcs[id] ?: return r(state)
        if (!npc.alive) return r(state)
        val clean = deltas.filterValues { it != 0 }
        val s = touch(update(state, id) { it.copy(relation = it.relation.plus(clean)) }, id)
        if (!notify || clean.isEmpty()) return r(s)
        return EffectApplier.Result(s, listOf(relationNotice(id, clean)))
    }

    private fun relationNotice(id: String, deltas: Map<RelAxis, Int>): Notice {
        fun part(a: RelAxis, d: Int): LText {
            val label = texts.format("eksen_${a.key}")
            val sign = if (d > 0) "+$d" else "−${-d}"
            return LText("${label.tr} $sign", "${label.en} $sign")
        }
        val parts = RelAxis.entries.filter { (deltas[it] ?: 0) != 0 }.map { part(it, deltas.getValue(it)) }
        val joined = LText(parts.joinToString(" · ") { it.tr }, parts.joinToString(" · ") { it.en })
        return Notice(NoticeKind.COMPANION, texts.formatL("iliski_degisti", "npc" to name(id), "changes" to joined))
    }

    /** Hatıra ekler (aynı kimlik varsa ağırlığı büyük olanla değiştirir); sınır aşılırsa en zayıf solan silinir. */
    fun remember(state: WorldState, id: String, memory: String, weight: Int): WorldState = update(state, id) { npc ->
        val old = npc.memories.firstOrNull { it.id == memory }
        val w = if (old != null && abs(old.weight) > abs(weight)) old.weight else weight
        var list = npc.memories.filterNot { it.id == memory } + Memory(memory, state.day, w)
        while (list.size > Memory.LIMIT) {
            val weakest = list.filterNot { it.permanent || it.id.startsWith(Memory.LIE_PREFIX) }.minByOrNull { abs(it.weight) } ?: list.first()
            list = list - weakest
        }
        npc.copy(memories = list)
    }

    fun kill(state: WorldState, id: String): EffectApplier.Result {
        val npc = state.npcs[id] ?: return r(state)
        if (!npc.alive) return r(state)
        var s = update(state, id) { it.copy(status = NpcStatus.DEAD, companion = false) }
        // Yay düğümleri kapanır: bu NPC'ye bağlı tohumlar atılır (olaylar Yönetmen'de elenir).
        s = s.copy(seeds = s.seeds.filterNot { it.npc == id || content.events[it.eventId]?.npcs?.contains(id) == true })
        // Yas Tutan: yoldaşın ölümünden sonra 3 gün savaşmadan yas (gizli sınıf koşulu).
        if (npc.companion) s = s.copy(vars = s.vars + (VAR_MOURNING to s.day + 1))
        // Ölenin kişisel görevi yarım kalır.
        if (s.quests[personalQuestId(id)] == QuestStatus.ACTIVE) s = s.copy(quests = s.quests + (personalQuestId(id) to QuestStatus.FAILED))
        val notices = if (npc.known) listOf(Notice(NoticeKind.WARNING, texts.formatL("npc_oldu", "npc" to fullName(id)))) else emptyList()
        return EffectApplier.Result(updateRanking(s, announce = false).state, notices)
    }

    fun companions(state: WorldState): List<NpcState> = state.npcs.values.filter { it.companion && it.alive }.sortedBy { it.id }

    fun capacity(state: WorldState): Int = Progression.companionCapacity(state.player.stats, state.player.grade)

    fun join(state: WorldState, id: String): EffectApplier.Result {
        val npc = state.npcs[id] ?: return r(state)
        if (!npc.alive || npc.companion) return r(state)
        if (companions(state).size >= capacity(state)) {
            return EffectApplier.Result(touch(state, id), listOf(Notice(NoticeKind.WARNING, texts.formatL("yoldas_dolu", "npc" to name(id)))))
        }
        val s = touch(update(state, id) { it.copy(companion = true, status = NpcStatus.ACTIVE) }, id)
        return EffectApplier.Result(s, listOf(Notice(NoticeKind.COMPANION, texts.formatL("yoldas_katildi", "npc" to name(id)))))
    }

    fun leave(state: WorldState, id: String, gone: Boolean = false): EffectApplier.Result {
        val npc = state.npcs[id] ?: return r(state)
        if (!npc.companion && !gone) return r(state)
        val s = update(state, id) { it.copy(companion = false, status = if (gone && it.alive) NpcStatus.GONE else it.status) }
        return EffectApplier.Result(s, listOf(Notice(NoticeKind.WARNING, texts.formatL("yoldas_ayrildi", "npc" to name(id)))))
    }

    fun setFaction(state: WorldState, id: String, faction: String): EffectApplier.Result {
        val npc = state.npcs[id] ?: return r(state)
        if (npc.faction == faction) return r(state)
        val s = update(state, id) { it.copy(faction = faction) }
        val notices = if (npc.known) listOf(Notice(NoticeKind.SYSTEM, texts.formatL("npc_fraksiyon", "npc" to name(id), "faction" to factionName(faction)))) else emptyList()
        return EffectApplier.Result(s, notices)
    }

    fun factionName(id: String): LText = content.names.find(NameEntry.Kind.FACTION, id)?.full ?: LText(id, id)

    private fun npcTrait(state: WorldState, id: String, trait: String): EffectApplier.Result {
        val def = content.traits[trait] ?: error("Tanımsız iz: $trait")
        val npc = state.npcs[id] ?: return r(state)
        if (trait in npc.traits) return r(state)
        val s = update(state, id) { it.copy(traits = it.traits + trait) }
        val notices = if (npc.known) listOf(Notice(NoticeKind.COMPANION, texts.formatL("npc_iz", "npc" to name(id), "name" to def.name), listOf(def.description))) else emptyList()
        return EffectApplier.Result(s, notices)
    }

    private fun playerTrait(state: WorldState, trait: String): EffectApplier.Result {
        val def = content.traits[trait] ?: error("Tanımsız iz: $trait")
        if (trait in state.player.traits) return r(state)
        var stats = state.player.stats
        (def.statBonus.entries + def.statPenalty.entries).forEach { (st, d) -> stats = stats.plus(st, d) }
        val p = state.player.copy(traits = state.player.traits + trait, stats = stats)
        val s = state.copy(player = p.copy(hp = p.hp.coerceAtMost(p.maxHp), mp = p.mp.coerceAtMost(p.maxMp)), corruption = (state.corruption + def.corruption).coerceIn(0, 100))
        return EffectApplier.Result(s, listOf(Notice(NoticeKind.ECHO, texts.formatL("iz_kazanildi", "name" to def.name), listOf(def.description))))
    }

    private fun learn(state: WorldState, id: String): EffectApplier.Result {
        val def = content.knowledge[id] ?: error("Tanımsız bilgi: $id")
        if (id in state.knowledge) return r(state)
        return EffectApplier.Result(
            state.copy(knowledge = state.knowledge + id),
            listOf(Notice(NoticeKind.ANALYZE, texts.formatL("bilgi_eklendi", "name" to def.name), listOf(def.description))),
        )
    }

    fun personalQuestId(npc: String) = "$PERSONAL_QUEST_PREFIX$npc"

    private fun reward(state: WorldState, id: String): EffectApplier.Result {
        val def = content.npcs[id] ?: return r(state)
        val npc = state.npcs[id] ?: return r(state)
        if (MARK_REWARDED in npc.marks) return r(state)
        var s = update(state, id) { it.copy(marks = it.marks + MARK_REWARDED) }
        s = remember(s, id, MEM_PERSONAL_DONE, 60)
        val rel = shiftRelation(s, id, mapOf(RelAxis.BOND to 10, RelAxis.TRUST to 5), notify = true)
        val applied = effects.applyAll(rel.state.copy(focusNpc = id), def.personalReward)
        return EffectApplier.Result(applied.state.copy(focusNpc = state.focusNpc), rel.notices + applied.notices)
    }

    private fun addArmy(state: WorldState, unit: String, count: Int): EffectApplier.Result {
        val def = content.army[unit] ?: error("Tanımsız birlik: $unit")
        if (count == 0) return r(state)
        val n = ((state.player.army[unit] ?: 0) + count).coerceAtLeast(0)
        val army = if (n == 0) state.player.army - unit else state.player.army + (unit to n)
        val s = state.copy(player = state.player.copy(army = army))
        val notices = if (count > 0) listOf(Notice(NoticeKind.SYSTEM, texts.formatL("ordu_katildi", "name" to def.name, "n" to LText("$count", "$count")))) else emptyList()
        return EffectApplier.Result(s, notices)
    }

    /** Oyuncunun en güçlü eğilimine uygun birlik (gölge asker, canavar, lonca …). */
    fun armyUnitFor(state: WorldState): String? {
        val units = content.army.values.sortedBy { it.id }
        return state.player.affinity.ranked().firstNotNullOfOrNull { a -> units.firstOrNull { a in it.affinities }?.id } ?: units.firstOrNull()?.id
    }

    fun armyPower(state: WorldState): Int = state.player.army.entries.sumOf { (id, n) ->
        Progression.armyPower(content.army[id]?.power ?: 0, n, state.player.level, state.player.grade)
    }

    // ---------------------------------------------------------------- tanıklar

    /** Bu sahnede oyuncunun yanında olan NPC'ler: yoldaşlar, `yaninda` koşulu tutanlar ve olayın NPC'leri. */
    fun present(state: WorldState, eventNpcs: List<String> = emptyList()): List<String> {
        val ids = linkedSetOf<String>()
        companions(state).forEach { ids += it.id }
        content.npcs.values.sortedBy { it.id }.forEach { d ->
            if (d.presentWhen?.test(state) == true && state.npcs[d.id]?.alive == true) ids += d.id
        }
        eventNpcs.forEach { id -> if (state.npcs[id]?.alive == true) ids += id }
        return ids.toList()
    }

    /**
     * GDD: oyuncunun eylemleri NPC'nin değerlerine göre yorumlanır. Vicdanlı NPC acımasızlığa güven kaybeder,
     * hırslı NPC aynı şeye saygı duyar, korkak NPC korkar. Büyük değişim olayın hatırası olarak kalır.
     */
    fun interpret(state: WorldState, deltas: Map<MoralAxis, Int>, eventId: String?): EffectApplier.Result {
        if (deltas.values.sumOf { abs(it) } < WITNESS_MIN) return r(state)
        val eventNpcs = eventId?.let { content.events[it]?.npcs } ?: emptyList()
        var s = state
        val notices = mutableListOf<Notice>()
        for (id in present(state, eventNpcs)) {
            val def = content.npcs[id] ?: continue
            // Bedensiz ses (Yankı) her seçimi "görür"; ilişkisi yalnızca yazılmış içerikle değişir.
            if (def.presentWhen != null) continue
            val p = personality(s, id)
            val align = MoralAxis.entries.sumOf { (deltas[it] ?: 0) * def.values[it] } / 50.0
            val trust = (align * (0.5 + p[Trait.CONSCIENCE] / 10.0)).roundToInt().coerceIn(-WITNESS_CAP, WITNESS_CAP)
            val mercy = deltas[MoralAxis.MERCY] ?: 0
            val respect = if (mercy < 0) (-mercy * (p[Trait.AMBITION] - 5) / 5.0).roundToInt().coerceIn(-WITNESS_CAP, WITNESS_CAP) else 0
            val fear = if (mercy < 0) (-mercy * (10 - p[Trait.COURAGE]) / 20.0).roundToInt().coerceIn(0, WITNESS_CAP) else 0
            val change = mapOf(RelAxis.TRUST to trust, RelAxis.RESPECT to respect, RelAxis.FEAR to fear).filterValues { it != 0 }
            if (change.isEmpty()) continue
            val res = shiftRelation(s, id, change, notify = true)
            s = res.state
            notices += res.notices
            if (abs(trust) >= WITNESS_MEMORY && eventId != null) s = remember(s, id, Memory.EVENT_PREFIX + eventId, trust * 3)
        }
        return EffectApplier.Result(s, notices)
    }

    /** [Açığa vur]: tanıyanlar saygı duyar, korkaklar korkar; yoldaşlar hemen tepki verir. */
    fun onReveal(state: WorldState, gap: Int): EffectApplier.Result {
        if (gap <= 0) return updateRanking(state, announce = true)
        var s = state
        val notices = mutableListOf<Notice>()
        val witnesses = present(state).toSet() + state.npcs.values.filter { it.known && it.alive && content.npcs[it.id]?.ranked == true }.map { it.id }
        for (id in witnesses.sorted()) {
            val p = personality(s, id)
            val change = mapOf(
                RelAxis.RESPECT to (2 * gap).coerceAtMost(20),
                RelAxis.FEAR to (gap * (10 - p[Trait.COURAGE]) / 5).coerceIn(0, 15),
            )
            s = shiftRelation(s, id, change, notify = false).state
            if (s.npcs[id]?.companion == true) {
                val pr = powerReaction(s, id, force = true)
                s = pr.state
                notices += pr.notices
            }
        }
        val rank = updateRanking(s, announce = true)
        return EffectApplier.Result(rank.state, notices + rank.notices)
    }

    // ---------------------------------------------------------------- yoldaş görüşleri

    data class Opinion(val npc: String, val text: LText, val favored: String?)

    /**
     * Büyük seçimden önce yanındakilerin tek satırlık görüşü. Sahneye yazılmış satır önceliklidir;
     * yoksa NPC, değerlerine en uygun seçeneğin baskın ekseni için kendi sesinden bir satır söyler.
     */
    fun opinions(state: WorldState, scene: Scene, visible: List<ChoiceDef>, important: Boolean): List<Opinion> {
        val eventNpcs = state.dayState.currentEvent?.let { content.events[it]?.npcs } ?: emptyList()
        val out = mutableListOf<Opinion>()
        for (id in present(state, eventNpcs)) {
            val def = content.npcs[id] ?: continue
            val authored = scene.companionLines.firstOrNull { state.npcId(it.npc) == id && (it.condition?.test(state) ?: true) }
            if (authored != null) {
                out += Opinion(id, authored.text, null)
                continue
            }
            if (!important || visible.size < 2) continue
            if (id !in companions(state).map { it.id } && def.presentWhen?.test(state) != true) continue
            val scored = visible.map { c -> c to moralDeltas(c).let { d -> d to MoralAxis.entries.sumOf { (d[it] ?: 0) * def.values[it] } / 50.0 } }
            val (best, pair) = scored.maxByOrNull { it.second.second } ?: continue
            val (deltas, score) = pair
            if (score < OPINION_MIN || scored.all { abs(it.second.second - score) < 0.5 }) continue
            val axis = MoralAxis.entries.maxByOrNull { (deltas[it] ?: 0) * def.values[it] } ?: continue
            val d = deltas[axis] ?: 0
            if (d == 0) continue
            val lines = def.opinions[NpcDef.opinionKey(axis, d > 0)].orEmpty()
            if (lines.isEmpty()) continue
            out += Opinion(id, lines[pick(state, scene.id + id, lines.size)], best.id)
        }
        return out
    }

    fun moralDeltas(c: ChoiceDef): Map<MoralAxis, Int> =
        c.effects.filterIsInstance<Effect.ShiftMorality>().groupBy { it.axis }.mapValues { (_, l) -> l.sumOf { it.delta } }

    private fun pick(state: WorldState, salt: String, n: Int): Int = Math.floorMod((salt.hashCode() * 31 + state.day), n)

    // ---------------------------------------------------------------- güce tepkiler

    /**
     * OP MC: yoldaş, oyuncunun kendisinden çok güçlendiğini görünce kişiliğine ve ilişkisine göre
     * hayranlık, endişe, kıskançlık ya da bağımlılıkla tepki verir (7 günde bir; açığa vurmada hemen).
     */
    fun powerReaction(state: WorldState, id: String, force: Boolean = false): EffectApplier.Result {
        val def = content.npcs[id] ?: return r(state)
        val npc = state.npcs[id] ?: return r(state)
        if (!npc.alive) return r(state)
        val gap = state.player.level - npc.level
        if (!force && (gap < POWER_GAP || state.day - npc.lastPowerDay < POWER_COOLDOWN)) return r(state)
        val p = personality(state, id)
        val rel = npc.relation
        val kind = when {
            state.corruption >= 20 || state.attention >= 50 || (p[Trait.CONSCIENCE] >= 7 && state.morality.humanity < 0) -> PowerReaction.WORRY
            p[Trait.AMBITION] >= 7 && rel.trust < 40 -> PowerReaction.ENVY
            p[Trait.COURAGE] <= 4 || rel.fear >= 40 -> PowerReaction.DEPENDENCE
            else -> PowerReaction.ADMIRATION
        }
        val change = when (kind) {
            PowerReaction.ADMIRATION -> mapOf(RelAxis.RESPECT to 3)
            PowerReaction.WORRY -> mapOf(RelAxis.TRUST to -1, RelAxis.BOND to 1)
            PowerReaction.ENVY -> mapOf(RelAxis.RESPECT to 2, RelAxis.TRUST to -3)
            PowerReaction.DEPENDENCE -> mapOf(RelAxis.BOND to 3, RelAxis.FEAR to 2)
        }
        var s = update(state, id) { it.copy(lastPowerDay = state.day, relation = it.relation.plus(change)) }
        s = remember(s, id, "guc_${kind.key}", 10)
        val lines = def.powerLines[kind].orEmpty()
        val notices = if (lines.isEmpty()) {
            emptyList()
        } else {
            listOf(Notice(NoticeKind.COMPANION, texts.formatL("yoldas_tepki", "npc" to name(id)), listOf(lines[pick(state, id + kind.key, lines.size)])))
        }
        return EffectApplier.Result(s, notices)
    }

    // ---------------------------------------------------------------- günlük gündem

    /** Yeni günün NPC adımı: her NPC kendi gündemini ilerletir; ardından sıralama güncellenir. */
    fun dailyTick(state: WorldState): EffectApplier.Result {
        var s = ensure(state)
        val notices = mutableListOf<Notice>()
        for (def in content.npcs.values.sortedBy { it.id }) {
            val npc0 = s.npcs[def.id] ?: continue
            if (!npc0.alive) continue
            // 1. Büyüme: yoldaşlıkta ya da ihmal edilince daha hızlı ("başka bir yoldan güçlenir").
            val neglected = !npc0.companion && npc0.known && s.day - npc0.lastContactDay >= Progression.NPC_NEGLECT_DAYS
            var rate = def.growthRate
            if (npc0.companion) rate *= Progression.NPC_COMPANION_GROWTH
            if (neglected) rate *= Progression.NPC_NEGLECT_GROWTH
            var level = npc0.level
            var progress = npc0.progress + Progression.npcDailyGrowth(rate, level)
            while (progress >= 100) {
                progress -= 100
                level++
            }
            val gradeUp = Grade.forLevel(level) > Grade.forLevel(npc0.level)
            s = update(s, def.id) { n ->
                n.copy(
                    level = level,
                    progress = progress,
                    marks = if (neglected) n.marks + MARK_NEGLECT else n.marks,
                    // 2. Hafıza solar: küçük olaylar her gün biraz silikleşir; büyükler ve yalanlar kalır.
                    memories = n.memories.mapNotNull { m ->
                        when {
                            m.permanent || m.id.startsWith(Memory.LIE_PREFIX) || m.id.startsWith(Memory.LIE_REVEALED_PREFIX) -> m
                            abs(m.weight) <= 1 -> null
                            else -> m.copy(weight = m.weight - Integer.signum(m.weight))
                        }
                    },
                )
            }
            if (gradeUp && def.ranked) {
                notices += Notice(NoticeKind.SYSTEM, texts.formatL("npc_kademe", "npc" to fullName(def.id), "grade" to LText(Grade.forLevel(level).name, Grade.forLevel(level).name)))
            }
            // 3. Hedef
            val g = goalStep(s, def)
            s = g.state
            notices += g.notices
            // 4. Rakipler "İlk" unvanlarını kapar.
            for (fc in def.firstClaims) {
                if (level >= fc.level && !s.firstClaims.containsKey(fc.title) && fc.title in content.titles) {
                    s = effects.claimFirstByNpc(s, fc.title, def.id)
                    notices += Notice(NoticeKind.WARNING, texts.formatL("npc_ilk_unvan", "npc" to fullName(def.id), "title" to content.titles.getValue(fc.title).name))
                }
            }
            // 5. Söylenen yalanlar şüpheciliğe göre ortaya çıkar.
            val lie = s.npcs[def.id]?.memories?.firstOrNull { it.id.startsWith(Memory.LIE_PREFIX) }
            // RNG durumu her alt çağrıda ileri taşınır (aynı sayı iki kez kullanılmasın).
            val rng = Rng(s.rngState)
            val surfaced = lie != null && rng.chance(personality(s, def.id)[Trait.SKEPTICISM] * LIE_CHANCE_PER_POINT)
            if (lie != null) s = s.copy(rngState = rng.state)
            if (lie != null && surfaced) {
                val topic = lie.id.removePrefix(Memory.LIE_PREFIX)
                s = update(s, def.id) { n -> n.copy(memories = n.memories - lie + Memory(Memory.LIE_REVEALED_PREFIX + topic, s.day, LIE_REVEALED_WEIGHT)) }
                s = shiftRelation(s, def.id, mapOf(RelAxis.TRUST to -20, RelAxis.RESPECT to -5), notify = false).state
                notices += Notice(NoticeKind.WARNING, texts.formatL("yalan_ortaya_cikti", "npc" to name(def.id)))
                s = plant(s, ThresholdKind.LIE, def.id, 0, 0, null)
            }
            // 6. Eşikler
            val t = thresholds(s, def)
            s = t.state
            notices += t.notices
        }
        // 7. Yoldaşların güce tepkisi
        for (c in companions(s)) {
            val pr = powerReaction(s, c.id)
            s = pr.state
            notices += pr.notices
        }
        // 8. Yas (Yas Tutan gizli sınıfı)
        val mourning = s.variable(VAR_MOURNING)
        if (mourning > 0 && !s.hasFlag(FLAG_MOURNED) && s.day - (mourning - 1) >= MOURNING_DAYS && s.variable(VAR_LAST_FIGHT) < mourning) {
            s = s.copy(flags = s.flags + FLAG_MOURNED)
        }
        // 9. Prosedürel avcılar ve sıralama
        s = s.copy(hunters = s.hunters.map { h ->
            var lv = h.level
            var pr = h.progress + Progression.npcDailyGrowth(h.rate, lv)
            while (pr >= 100) { pr -= 100; lv++ }
            h.copy(level = lv, progress = pr)
        })
        val rank = updateRanking(s, announce = true)
        return EffectApplier.Result(rank.state, notices + rank.notices)
    }

    private fun goalStep(state: WorldState, def: NpcDef): EffectApplier.Result {
        val npc = state.npcs[def.id] ?: return r(state)
        val goal = def.goals.firstOrNull { it.id == npc.goal }
        if (goal == null) {
            val next = pickGoal(def, state, npc)
            return r(if (next != null) update(state, def.id) { it.copy(goal = next, goalProgress = 0) } else state)
        }
        if (goal.condition?.test(state.copy(focusNpc = def.id)) == false) {
            // İmkansızlaştı: bırakılır, yenisi seçilir.
            val dropped = npc.copy(doneGoals = npc.doneGoals + goal.id)
            return r(update(state, def.id) { dropped.copy(goal = pickGoal(def, state, dropped), goalProgress = 0) })
        }
        val progress = npc.goalProgress + 1
        if (progress < goal.days) return r(update(state, def.id) { it.copy(goalProgress = progress) })
        // Tamamlandı: etkiler NPC odağıyla uygulanır, yeni hedef seçilir.
        val done = npc.copy(doneGoals = npc.doneGoals + goal.id, goalProgress = 0)
        var s = update(state, def.id) { done.copy(goal = null) }
        val applied = effects.applyAll(s.copy(focusNpc = def.id), goal.effects)
        s = applied.state.copy(focusNpc = state.focusNpc)
        val after = s.npcs.getValue(def.id)
        s = update(s, def.id) { it.copy(goal = pickGoal(def, s, after)) }
        return EffectApplier.Result(s, applied.notices.filter { npc.known })
    }

    /** GDD eşik tablosu. Her eşik NPC başına bir kez çalışır ve bir olay tohumlar. */
    private fun thresholds(state: WorldState, def: NpcDef): EffectApplier.Result {
        val npc = state.npcs[def.id] ?: return r(state)
        if (!npc.known || npc.status != NpcStatus.ACTIVE) return r(state)
        val p = personality(state, def.id)
        val rel = npc.relation
        var s = state
        val notices = mutableListOf<Notice>()
        fun once(kind: ThresholdKind): Boolean = "esik_${kind.key}" !in (s.npcs[def.id]?.marks ?: emptySet())
        if (rel.trust < -40 && p[Trait.AMBITION] >= 7 && once(ThresholdKind.BETRAYAL)) {
            // İhanet tohumu: 5–15 gün içinde, güven hâlâ dipteyse.
            s = plant(s, ThresholdKind.BETRAYAL, def.id, 5, 15, """{"iliski_max":{"${def.id}":{"guven":-40}}}""")
        }
        // Kişisel görev, kaçış ve itaat sahneleri yanında yürüyen birini anlatır: yalnızca yoldaşlarda.
        if (npc.companion && rel.bond >= 70 && once(ThresholdKind.PERSONAL)) s = plant(s, ThresholdKind.PERSONAL, def.id, 0, 1, null)
        if (npc.companion && rel.fear >= 60 && p[Trait.COURAGE] <= 3 && once(ThresholdKind.FLEE) && once(ThresholdKind.OBEY)) {
            // İtaat: sadık ya da oyuncuya bağlanmış korkak boyun eğer; değilse kaçar. Yalnız sadakate bakınca
            // hiçbir yoldaş itaat edemiyordu (QA P1-2: tek korkak yoldaş Mert'in sadakati 5).
            val obeys = p[Trait.LOYALTY] >= 6 || rel.bond >= OBEY_BOND
            s = plant(s, if (obeys) ThresholdKind.OBEY else ThresholdKind.FLEE, def.id, 0, 1, null)
        }
        if (rel.respect >= 60 && rel.trust <= 0 && once(ThresholdKind.RIVAL)) {
            s = update(plant(s, ThresholdKind.RIVAL, def.id, 1, 3, null), def.id) { it.copy(marks = it.marks + MARK_RIVAL) }
        }
        if (valueDrift(def.values, s.morality) > DEPARTURE_DISTANCE && once(ThresholdKind.DEPARTURE)) {
            if (npc.companion) {
                s = plant(s, ThresholdKind.DEPARTURE, def.id, 0, 1, null)
            } else if (npc.faction != null) {
                // Gruptan değilse fraksiyon değiştirir: değerlerine en yakın (ve farklı) fraksiyona geçer.
                val f = nearestFaction(def.values, exclude = npc.faction)
                if (f != null) {
                    s = update(s, def.id) { it.copy(marks = it.marks + "esik_${ThresholdKind.DEPARTURE.key}") }
                    val r = setFaction(s, def.id, f)
                    s = r.state
                    notices += r.notices
                }
            }
        }
        return EffectApplier.Result(s, notices)
    }

    /** Eşik olayını tohumlar: NPC'ye özel `esik_<tur>_<npc>` varsa o, yoksa genel `esik_<tur>`. */
    private fun plant(state: WorldState, kind: ThresholdKind, id: String, min: Int, max: Int, condition: String?): WorldState {
        // Bedensiz NPC'ler (Yankı) genel sahnelere uymaz: yalnızca kendilerine yazılmış eşik olayı varsa.
        if (content.npcs[id]?.combat == null && "${kind.genericEvent}_$id" !in content.events) return state
        val marked = update(state, id) { it.copy(marks = it.marks + "esik_${kind.key}") }
        val event = thresholdEvent(kind, id) ?: return marked
        val rng = Rng(marked.rngState)
        val delay = if (max > min) rng.nextInt(min, max) else min
        return marked.copy(seeds = marked.seeds + Seed(event, marked.day + delay, condition, id), rngState = rng.state)
    }

    fun thresholdEvent(kind: ThresholdKind, id: String): String? =
        "${kind.genericEvent}_$id".takeIf { it in content.events } ?: kind.genericEvent.takeIf { it in content.events }

    /** Değerler ile oyuncunun ahlak ekseni arasındaki fark (üç eksenin mutlak farkları toplamı). */
    fun valueDistance(values: Morality, player: Morality): Int = MoralAxis.entries.sumOf { abs(values[it] - player[it]) }

    /**
     * GDD "değerler ile oyuncu ekseni arası fark > 120": uzaklığın, oyuncu nötrken zaten var olan
     * uzaklığı ne kadar aştığı (nötr oyuncu kimseyi kaçırmaz; yakınlaşmak eksi sayılır).
     */
    fun valueDrift(values: Morality, player: Morality): Int = valueDistance(values, player) - valueDistance(values, Morality())

    fun nearestFaction(values: Morality, exclude: String? = null): String? =
        content.factionValues.entries.filter { it.key != exclude }.sortedBy { it.key }.minByOrNull { valueDistance(values, it.value) }?.key

    // ---------------------------------------------------------------- Küresel Sıralama

    enum class RankKind { PLAYER, NPC, HUNTER }

    data class RankEntry(val kind: RankKind, val id: String, val level: Int)

    /** Sıralama: seviyeye göre; eşitlikte oyuncu geride kalır. `public`: dünyanın gördüğü (görünen seviye). */
    fun ranking(state: WorldState, public: Boolean): List<RankEntry> {
        val list = mutableListOf(RankEntry(RankKind.PLAYER, WorldState.PLAYER_CLAIM, if (public) state.publicLevel else state.player.level))
        content.npcs.values.filter { it.ranked }.forEach { d ->
            val n = state.npcs[d.id] ?: return@forEach
            if (n.alive) list += RankEntry(RankKind.NPC, d.id, n.level)
        }
        state.hunters.forEach { list += RankEntry(RankKind.HUNTER, "avci_${it.uid}", it.level) }
        return list.sortedWith(compareByDescending<RankEntry> { it.level }.thenBy { if (it.kind == RankKind.PLAYER) 1 else 0 }.thenBy { it.id })
    }

    fun playerRank(state: WorldState, public: Boolean): Int = ranking(state, public).indexOfFirst { it.kind == RankKind.PLAYER } + 1

    fun hunterName(h: Hunter): LText {
        val f = content.ranking.firstNames.getOrNull(h.first) ?: LText("?", "?")
        val l = content.ranking.lastNames.getOrNull(h.last) ?: LText("?", "?")
        return LText("${f.tr} ${l.tr}", "${f.en} ${l.en}")
    }

    /**
     * Sırayı günceller (`sira`, `gercek_sira` değişkenleri). Sistem duyurur: bir ana NPC seni geçti /
     * sen onu geçtin; ilk 10 / 3 / 1'e ilk girişte fraksiyonlar tepki verir. Rakipler geçilince saygı duyar.
     */
    fun updateRanking(state: WorldState, announce: Boolean): EffectApplier.Result {
        if (content.npcs.isEmpty() && state.hunters.isEmpty()) return r(state)
        var s = state
        val notices = mutableListOf<Notice>()
        val rank = playerRank(s, public = true)
        val before = s.variable(VAR_RANK)
        s = s.copy(vars = s.vars + (VAR_RANK to rank) + (VAR_TRUE_RANK to playerRank(s, public = false)))
        for (d in content.npcs.values.filter { it.ranked }.sortedBy { it.id }) {
            val n = s.npcs[d.id] ?: continue
            if (!n.alive) continue
            val above = n.level >= s.publicLevel
            val wasAbove = MARK_ABOVE in n.marks
            if (above == wasAbove && (MARK_RANK_SEEN in n.marks)) continue
            s = update(s, d.id) { it.copy(marks = (if (above) it.marks + MARK_ABOVE else it.marks - MARK_ABOVE) + MARK_RANK_SEEN) }
            if (!announce || MARK_RANK_SEEN !in n.marks) continue
            if (above) {
                notices += Notice(NoticeKind.WARNING, texts.formatL("sira_gecti", "npc" to fullName(d.id)))
            } else {
                notices += Notice(NoticeKind.SYSTEM, texts.formatL("sira_geride", "npc" to fullName(d.id)))
                // Hırslı rakip geride kalınca saygı duyar ama güvenmez.
                if (personality(s, d.id)[Trait.AMBITION] >= 7) s = shiftRelation(s, d.id, mapOf(RelAxis.RESPECT to 5, RelAxis.TRUST to -3), notify = false).state
            }
        }
        if (announce && before > 0 && rank < before && rank <= TOP_ANNOUNCE) {
            notices += Notice(NoticeKind.SYSTEM, texts.format("sira_yukseldi", "n" to rank))
        }
        // Fraksiyon tepkileri yalnızca duyurulan değişimlerde (kurulumda ilk sıra "tırmanış" sayılmaz).
        if (announce) {
            for (reaction in content.ranking.reactions.sortedByDescending { it.rank }) {
                val flag = "$RANK_FLAG_PREFIX${reaction.rank}"
                if (rank <= reaction.rank && before > reaction.rank && !s.hasFlag(flag)) {
                    s = s.copy(flags = s.flags + flag)
                    val applied = effects.applyAll(s, reaction.effects)
                    s = applied.state
                    notices += applied.notices
                }
            }
        }
        return EffectApplier.Result(s, notices)
    }

    // ---------------------------------------------------------------- savaş

    /** Yoldaşın ya da düellodaki NPC'nin o seviyedeki statları. */
    fun combatStats(def: NpcDef, level: Int): Stats {
        val c = def.combat ?: return Stats()
        var stats = c.stats
        val ups = (level - def.startLevel).coerceAtLeast(0)
        c.growth.forEach { (st, d) -> stats = stats.plus(st, d * ups) }
        return stats
    }

    fun combatHp(def: NpcDef, level: Int): Int = Progression.maxHp(combatStats(def, level), level)

    /** Yoldaş savaşta düşünce: hatırası kalır, biraz korkar. */
    fun allyDowned(state: WorldState, id: String): WorldState =
        remember(update(state, id) { it.copy(relation = it.relation.shift(RelAxis.FEAR, 3)) }, id, MEM_DOWNED, 15)

    /** Düelloda yenilen NPC: infaz biçimine göre ölür, ezilir ya da bağışlanır. */
    fun duelResult(state: WorldState, id: String, execution: String?): EffectApplier.Result = when (execution) {
        EXEC_BRUTAL -> kill(remember(state, id, MEM_DUEL_LOST, -60), id)
        EXEC_SPARE -> shiftRelation(remember(state, id, MEM_DUEL_SPARED, 45), id, mapOf(RelAxis.TRUST to 10, RelAxis.RESPECT to 8), notify = true)
        else -> shiftRelation(remember(state, id, MEM_DUEL_LOST, -30), id, mapOf(RelAxis.RESPECT to 10, RelAxis.FEAR to 10), notify = true)
    }

    /** Tanıklı başarıyı yoldaşlar da hatırlar. */
    fun witnessFeat(state: WorldState, featId: String): WorldState =
        present(state).fold(state) { s, id -> remember(s, id, "basari:$featId", 12) }

    // ---------------------------------------------------------------- görünüm yardımcıları

    /** Hatıranın metni: NPC'ye özgü, olay kartı, sistem kalıbı ya da bilgi kaydı. */
    fun memoryText(id: String, memory: Memory): LText? {
        val def = content.npcs[id]
        def?.memories?.get(memory.id)?.let { return it }
        if (memory.id.startsWith(Memory.EVENT_PREFIX)) {
            val ev = content.events[memory.id.removePrefix(Memory.EVENT_PREFIX)] ?: return null
            val key = if (memory.weight >= 0) "hafiza_olay_iyi" else "hafiza_olay_kotu"
            return texts.formatL(key, "event" to ev.card)
        }
        if (memory.id.startsWith("basari:")) {
            return content.feats[memory.id.removePrefix("basari:")]?.let { texts.formatL("hafiza_basari", "name" to it.name) }
        }
        if (memory.id.startsWith(Memory.LIE_REVEALED_PREFIX)) {
            val topic = memory.id.removePrefix(Memory.LIE_REVEALED_PREFIX)
            return content.knowledge[topic]?.let { texts.formatL("hafiza_yalan_acik", "name" to it.name) } ?: texts.format("hafiza_yalan_acik_genel")
        }
        // Söylenmiş ama ortaya çıkmamış yalanı NPC "hatırlar" ama bunu göstermez.
        if (memory.id.startsWith(Memory.LIE_PREFIX)) return null
        return texts["hafiza_${memory.id}"]
    }

    /** Odak NPC'nin aktif hedefinin adı ({npc_hedef:@}). */
    fun goalName(state: WorldState, id: String): LText? {
        val def = content.npcs[id] ?: return null
        val npc = state.npcs[id] ?: return null
        val g = def.goals.filter { it.id !in npc.doneGoals && (it.condition?.test(state.copy(focusNpc = id)) ?: true) }.maxByOrNull { it.priority }
            ?: def.goals.maxByOrNull { it.priority } ?: return null
        // Metin içinde kullanılır: sondaki nokta atılır.
        return LText(g.name.tr.trimEnd('.', ' '), g.name.en.trimEnd('.', ' '))
    }

    companion object {
        const val MARK_NEGLECT = "ihmal"
        const val MARK_FIGHTER = "_savasci"
        const val MARK_RIVAL = "rakip"
        const val MARK_REWARDED = "odullendi"
        const val MARK_ABOVE = "_ustte"
        const val MARK_RANK_SEEN = "_sirada"

        const val VAR_RANK = "sira"
        const val VAR_TRUE_RANK = "gercek_sira"
        const val VAR_MOURNING = "_yas_gunu"
        const val VAR_LAST_FIGHT = "_son_savas_gunu"
        const val FLAG_MOURNED = "yas_tutuldu"
        const val MOURNING_DAYS = 3
        const val RANK_FLAG_PREFIX = "_sira_"
        const val TOP_ANNOUNCE = 10

        const val PERSONAL_QUEST_PREFIX = "kisisel_"

        const val MEM_PERSONAL_DONE = "kisisel_tamam"
        const val MEM_DOWNED = "savasta_dustu"
        const val MEM_DUEL_LOST = "duelloda_yenildi"
        const val MEM_DUEL_SPARED = "duelloda_bagislandi"

        const val LIE_WEIGHT = -10
        const val LIE_REVEALED_WEIGHT = -35
        /** Şüphecilik puanı başına günlük açığa çıkma olasılığı (10 → %15). */
        const val LIE_CHANCE_PER_POINT = 0.015

        /** Tanık yorumu için gereken en küçük ahlak oynaması (Σ|Δ|) ve tek seçimde eksen başına tavan. */
        const val WITNESS_MIN = 3
        const val WITNESS_CAP = 15
        const val WITNESS_MEMORY = 6

        const val OPINION_MIN = 1.5
        const val POWER_GAP = 5
        const val POWER_COOLDOWN = 7
        const val DEPARTURE_DISTANCE = 60
        /** İtaat eşiği: sadakati düşük korkak yoldaş bu bağlılıkla da boyun eğer (QA P1-2). */
        const val OBEY_BOND = 40

        const val EXEC_BRUTAL = "acimasiz"
        const val EXEC_SPARE = "bagisla"

        /** Düelloda NPC düşman kimliği öneki: `npc:aras_kurt`. */
        const val ENEMY_PREFIX = "npc:"
    }
}
