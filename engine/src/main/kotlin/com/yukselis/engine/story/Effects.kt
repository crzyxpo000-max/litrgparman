package com.yukselis.engine.story

import com.yukselis.engine.core.DailyQuest
import com.yukselis.engine.core.GameClock
import com.yukselis.engine.core.PlayerState
import com.yukselis.engine.core.PromptKind
import com.yukselis.engine.core.Rng
import com.yukselis.engine.core.SkillState
import com.yukselis.engine.core.SystemPrompt
import com.yukselis.engine.core.WorldState
import com.yukselis.engine.i18n.LText
import com.yukselis.engine.system.Affinity
import com.yukselis.engine.system.ClassKind
import com.yukselis.engine.system.Grade
import com.yukselis.engine.system.Progression
import com.yukselis.engine.system.Rarity
import com.yukselis.engine.system.Stat

/**
 * Etkileri sırayla uygular ve oyuncuya gösterilecek Sistem bildirimlerini üretir.
 * Etkilerden sonra tetikleyicileri (sınıf teklifi, evrim, yetenek evrimi, kaynaştırma,
 * Öz bağlama, meslek teklifi) denetler ve istem kuyruğuna ekler.
 */
class EffectApplier(private val content: Content) {

    data class Result(val state: WorldState, val notices: List<Notice>)

    private val texts get() = content.texts

    /** NPC sistemi (Aşama 5). */
    val npc = com.yukselis.engine.npc.NpcEngine(content, this)

    fun applyAll(state: WorldState, effects: List<Effect>): Result {
        var current = state
        val notices = mutableListOf<Notice>()
        for (effect in effects) {
            val r = apply(current, effect)
            current = r.state
            notices += r.notices
        }
        val t = checkTriggers(current)
        return Result(t.state, notices + t.notices)
    }

    fun apply(state: WorldState, effect: Effect): Result = when (effect) {
        is Effect.SetFlag -> Result(state.copy(flags = state.flags + effect.flag), emptyList())
        is Effect.ClearFlag -> Result(state.copy(flags = state.flags - effect.flag), emptyList())
        is Effect.AddVar -> Result(
            state.copy(vars = state.vars + (effect.key to state.variable(effect.key) + effect.delta)),
            emptyList(),
        )
        is Effect.ShiftMorality -> Result(state.copy(morality = state.morality.shift(effect.axis, effect.delta)), emptyList())
        is Effect.ShiftReputation -> shiftReputation(state, effect.faction, effect.delta)
        is Effect.AdvanceDays -> advanceDays(state, effect.days)
        is Effect.AddStat -> addStat(state, effect.stat, effect.delta)
        is Effect.GainXp -> gainXp(state, effect.amount)
        is Effect.GrantTitle -> grantTitle(state, effect.titleId)
        is Effect.ShiftAffinity -> Result(
            state.withPlayer { it.copy(affinity = it.affinity.plus(effect.affinity, effect.delta)) },
            emptyList(),
        )
        is Effect.GrantSkill -> grantSkill(state, effect.skillId)
        is Effect.SkillXp -> skillXp(state, effect.skillId, effect.amount)
        is Effect.AddItem -> addItem(state, effect.itemId, effect.count)
        is Effect.AddCredits -> {
            val p = state.player
            val base = state.withPlayer { it.copy(credits = (p.credits + effect.amount).coerceAtLeast(0)) }
            val tab = if (effect.amount > 0) unlockTab(base, WorldState.TAB_BAG) else Result(base, emptyList())
            Result(tab.state, listOf(Notice(NoticeKind.SYSTEM, signed(texts.format("kredi", "n" to effect.amount), effect.amount))) + tab.notices)
        }
        is Effect.AddCrystals -> Result(
            state.withPlayer { it.copy(essenceCrystals = (it.essenceCrystals + effect.amount).coerceAtLeast(0)) },
            listOf(Notice(NoticeKind.SYSTEM, signed(texts.format("oz_kristali", "n" to effect.amount), effect.amount))),
        )
        is Effect.EssenceInsight -> essenceInsight(state, effect.essenceId, effect.amount)
        is Effect.DailyProgress -> dailyProgress(state, effect.kind, effect.amount)
        Effect.OfferProfession -> offerProfession(state)
        is Effect.UnlockTab -> unlockTab(state, effect.tab)
        // Savaş StoryEngine tarafından başlatılır (CombatEngine); burada etkisizdir.
        is Effect.StartCombat -> Result(state, emptyList())
        is Effect.PlantSeed -> plantSeed(state, effect)
        is Effect.ShiftAttention -> shiftAttention(state, effect.delta)
        is Effect.ShiftCorruption -> shiftCorruption(state, effect.delta)
        Effect.Reveal -> reveal(state)
        is Effect.Feat -> feat(state, effect.featId)
        is Effect.SetQuest -> setQuest(state, effect.questId, effect.status)
        is Effect.MoveTo -> Result(state.copy(region = effect.region), emptyList())
        is Effect.SetControl -> Result(
            state.regions[effect.region]?.let { r -> state.copy(regions = state.regions + (r.id to r.copy(control = effect.control))) } ?: state,
            emptyList(),
        )
        is Effect.ShiftDanger -> Result(
            state.regions[effect.region]?.let { r ->
                state.copy(regions = state.regions + (r.id to r.copy(danger = (r.danger + effect.delta).coerceIn(0, 100))))
            } ?: state,
            emptyList(),
        )
        is Effect.AddHp -> Result(state.withPlayer { it.copy(hp = (it.hp + effect.delta).coerceIn(1, it.maxHp)) }, emptyList())
        is Effect.AddMp -> Result(state.withPlayer { it.copy(mp = (it.mp + effect.delta).coerceIn(0, it.maxMp)) }, emptyList())
        is Effect.ShiftRelation, is Effect.AddMemory, is Effect.KillNpc, is Effect.JoinParty, is Effect.LeaveParty, is Effect.NpcGone,
        is Effect.SetNpcFaction, is Effect.MeetNpc, is Effect.NpcLevel, is Effect.NpcTrait, is Effect.NpcMark,
        is Effect.PlayerTrait, is Effect.Learn, is Effect.TellLie, is Effect.PersonalQuest, is Effect.NpcReward,
        is Effect.AddArmy, is Effect.GrowArmy,
        -> npc.apply(state, effect)
        // ---- Aşama 7 ----
        is Effect.GainLevelXp -> gainXp(state, Progression.xpToNext(state.player.level) * effect.percent / 100)
        is Effect.TowerFloor -> Result(
            state.copy(vars = state.vars + (TOWER_VAR to maxOf(state.variable(TOWER_VAR), effect.floor))),
            emptyList(),
        )
        is Effect.SetPath -> setPath(state, effect.path)
    }

    private fun setPath(state: WorldState, id: String): Result {
        val def = content.paths[id] ?: error("Tanımsız Yol: $id")
        return Result(
            state.withPlayer { it.copy(path = id) },
            listOf(Notice(NoticeKind.EVOLUTION, texts.formatL("yol_olustu", "path" to def.name), listOf(def.description), Rarity.UNIQUE)),
        )
    }

    // ---- Statlar, itibar ----

    private fun addStat(state: WorldState, stat: Stat, delta: Int): Result {
        val updated = state.player.copy(stats = state.player.stats.plus(stat, delta)).clampResources()
        val sign = if (delta >= 0) "+" else ""
        val notice = Notice(
            kind = NoticeKind.STATUS,
            title = texts.formatL("stat_degisti", "stat" to stat.label, "delta" to LText("$sign$delta", "$sign$delta")),
        )
        return Result(state.copy(player = updated), listOf(notice))
    }

    private fun shiftReputation(state: WorldState, faction: String, delta: Int): Result {
        var rep = state.reputation
        fun shift(f: String, d: Int) {
            rep = rep + (f to ((rep[f] ?: 0) + d).coerceIn(-100, reputationCap(state, f)))
        }
        shift(faction, delta)
        // Fraksiyonlar arası ilişki matrisi: dostların sevinir, düşmanların öfkelenir (DECISIONS #64).
        content.factions[faction]?.relations?.forEach { (other, rel) ->
            val ripple = (delta * rel * RIPPLE / 100.0).toInt()
            if (ripple != 0 && other != faction) shift(other, ripple)
        }
        return Result(state.copy(reputation = rep), emptyList())
    }

    // ---- OP MC: Dikkat, Yozlaşma, gizli güç, efsane ----

    fun shiftAttention(state: WorldState, delta: Int): Result {
        if (delta == 0) return Result(state, emptyList())
        val before = state.attention
        val after = (before + delta).coerceIn(0, 100)
        var s = state.copy(attention = after)
        val notices = mutableListOf<Notice>()
        // Eşik geçişleri: avcılar, Sistem düzeltmeleri, Kadim elçileri (olay `dikkat_<n>` varsa tohumlanır).
        // Aşama 7: eşikler her perdede yeniden kurulur; olay önce perdeye özel `dikkat_<n>_p<perde>`, yoksa
        // (yalnızca o perdeye uygunsa) genel `dikkat_<n>`.
        for (t in ATTENTION_THRESHOLDS) {
            val flag = attentionFlag(t, s.act)
            if (before < t && after >= t && !s.hasFlag(flag)) {
                s = s.copy(flags = s.flags + flag)
                val eventId = listOf("dikkat_${t}_p${s.act}", "dikkat_$t").firstOrNull { id ->
                    content.events[id]?.let { it.acts.isEmpty() || s.act in it.acts } == true
                }
                if (eventId != null) s = s.copy(seeds = s.seeds + com.yukselis.engine.world.Seed(eventId, s.day))
                notices += Notice(NoticeKind.ECHO, texts.format("dikkat_esik", "n" to t), listOf(texts.format("dikkat_esik_$t")))
            }
        }
        return Result(s, notices)
    }

    private fun shiftCorruption(state: WorldState, delta: Int): Result {
        val before = state.corruption
        val after = (before + delta).coerceIn(0, 100)
        val notices = if (delta > 0) listOf(Notice(NoticeKind.ECHO, texts.format("yozlasma", "n" to after))) else emptyList()
        var s = state.copy(corruption = after)
        // Aşama 7: Yozlaşma eşikleri (bir kez): `yozlasma_<n>` olayı varsa tohumlanır (Yankı'nın virüs yolu, izler).
        for (t in CORRUPTION_THRESHOLDS) {
            val flag = "yozlasma_esik_$t"
            if (before < t && after >= t && !s.hasFlag(flag)) {
                s = s.copy(flags = s.flags + flag)
                val eventId = "yozlasma_$t"
                if (eventId in content.events) s = s.copy(seeds = s.seeds + com.yukselis.engine.world.Seed(eventId, s.day))
            }
        }
        return Result(s, notices)
    }

    /** [Açığa vur]: görünen seviye gerçeğe çekilir; Dikkat 5 + 2 · fark artar. */
    private fun reveal(state: WorldState): Result {
        val gap = (state.player.level - state.publicLevel).coerceAtLeast(0)
        val s = state.copy(publicLevel = state.player.level)
        val a = shiftAttention(s, 5 + 2 * gap)
        // NPC'ler artık gerçek seviyeye tepki verir; sıralama güncellenir.
        val n = npc.onReveal(a.state, gap)
        return Result(n.state, listOf(Notice(NoticeKind.WARNING, texts.format("aciga_vuruldu", "n" to state.player.level))) + a.notices + n.notices)
    }

    private fun feat(state: WorldState, id: String): Result {
        val def = content.feats[id] ?: error("Tanımsız başarı: $id")
        val count = (state.legend[id] ?: 0) + 1
        var s = npc.witnessFeat(state.copy(legend = state.legend + (id to count)), id)
        val lines = mutableListOf<LText>()
        def.reactions.forEach { (faction, d) ->
            s = shiftReputation(s, faction, d).state
            def.lines[faction]?.let { lines += it }
        }
        val notices = mutableListOf(Notice(NoticeKind.SYSTEM, texts.formatL("soylenti", "name" to def.name), lines))
        if (def.title != null && def.titleAt != null && count >= def.titleAt) {
            val t = grantTitleIfDefined(s, def.title)
            s = t.state
            notices += t.notices
        }
        // Tanıklı her başarı biraz dikkat çeker.
        val a = shiftAttention(s, FEAT_ATTENTION)
        return Result(a.state, notices + a.notices)
    }

    private fun setQuest(state: WorldState, id: String, status: com.yukselis.engine.world.QuestStatus): Result {
        val def = content.quests[id] ?: error("Tanımsız görev: $id")
        if (state.quests[id] == status) return Result(state, emptyList())
        val key = "gorev_${status.key}"
        return Result(
            state.copy(quests = state.quests + (id to status)),
            listOf(Notice(NoticeKind.QUEST, texts.formatL(key, "name" to def.name), listOf(def.description))),
        )
    }

    private fun plantSeed(state: WorldState, e: Effect.PlantSeed): Result {
        val rng = Rng(state.rngState)
        val delay = if (e.maxDelay > e.minDelay) rng.nextInt(e.minDelay, e.maxDelay) else e.minDelay
        val seed = com.yukselis.engine.world.Seed(e.eventId, state.day + delay, e.condition, e.npc?.let { state.npcId(it) })
        return Result(state.copy(seeds = state.seeds + seed, rngState = rng.state), emptyList())
    }

    /** Unvanların koyduğu en düşük tavan (yoksa 100). */
    fun reputationCap(state: WorldState, faction: String): Int =
        state.player.titles.mapNotNull { content.titles[it]?.reputationCap?.get(faction) }.minOrNull() ?: 100

    // ---- XP, seviye, kademe kapısı ----

    private fun gainXp(state: WorldState, amount: Long): Result {
        if (amount <= 0) return Result(state, emptyList())
        val withXp = state.withPlayer { it.copy(xp = it.xp + amount) }
        val r = levelUp(withXp)
        return Result(r.state, listOf(Notice(NoticeKind.SYSTEM, texts.format("xp_kazanildi", "n" to amount))) + r.notices)
    }

    /**
     * Biriken XP'yi seviyeye çevirir. Bir sonraki seviye açılmamış bir kademedeyse seviye durur,
     * XP bir seviyelik tamponda tutulur (fazlası yanar) ve Evrim Görevi istemi açılır.
     */
    fun levelUp(state: WorldState): Result {
        var p = state.player
        val startLevel = p.level
        var blockedAt: Grade? = null
        while (p.xp >= Progression.xpToNext(p.level)) {
            val need = Progression.xpToNext(p.level)
            val nextGrade = Grade.forLevel(p.level + 1)
            if (nextGrade > p.unlockedGrade) {
                p = p.copy(xp = need)
                blockedAt = nextGrade
                break
            }
            var stats = p.stats
            val mult = Progression.growthMultiplier(nextGrade)
            classGrowth(p.classId).forEach { (stat, d) -> stats = stats.plus(stat, d * mult) }
            p = p.copy(
                xp = p.xp - need,
                level = p.level + 1,
                freePoints = p.freePoints + Progression.FREE_POINTS_PER_LEVEL,
                stats = stats,
            )
        }
        val notices = mutableListOf<Notice>()
        if (p.level > startLevel) {
            // Seviye atlayınca can ve mana dolar.
            p = p.copy(hp = p.maxHp, mp = p.maxMp)
            val gained = (p.level - startLevel) * Progression.FREE_POINTS_PER_LEVEL
            notices += Notice(
                kind = NoticeKind.LEVEL_UP,
                title = texts.format("seviye_atladin"),
                lines = listOf(
                    texts.format("seviye_satiri", "from" to startLevel, "to" to p.level),
                    texts.format("serbest_puan", "n" to gained),
                    texts.format("can_mana_yenilendi"),
                ),
            )
        }
        var next = state.copy(player = p)
        // OP MC: beklenenden hızlı büyüme Dikkat çeker (beklenen seviyenin üstündeki her yeni seviye +1).
        val expected = Progression.expectedLevel(next.day)
        val fast = (startLevel + 1..p.level).count { it > expected }
        if (fast > 0) {
            val a = shiftAttention(next, fast)
            next = a.state
            notices += a.notices
        }
        if (blockedAt != null && next.prompts.none { it.kind == PromptKind.EVOLUTION }) {
            next = next.copy(prompts = next.prompts + SystemPrompt(PromptKind.EVOLUTION, evolutionOptions(p.classId, blockedAt), blockedAt.name))
            notices += Notice(
                kind = NoticeKind.EVOLUTION,
                title = texts.format("evrim_gorevi", "grade" to blockedAt.name),
                lines = listOf(texts.format("seviye_durdu", "grade" to blockedAt.name)),
            )
        }
        return Result(next, notices)
    }

    private fun classGrowth(classId: String?): Map<Stat, Int> = classId?.let { content.classes[it]?.growth } ?: emptyMap()

    /** Sınıfın bu kademeye evrimleri; tanımlı değilse genel evrim. */
    fun evolutionOptions(classId: String?, grade: Grade): List<String> {
        val defined = content.classes.values
            .filter { it.kind == ClassKind.EVOLVED && it.evolvesFrom == classId && it.grade == grade }
            .map { it.id }
        return defined.ifEmpty { listOf(GENERIC_EVOLUTION) }
    }

    // ---- Unvanlar ----

    private fun grantTitle(state: WorldState, titleId: String): Result {
        val def = content.titles[titleId] ?: error("Tanımsız unvan: $titleId")
        if (titleId in state.player.titles) return Result(state, emptyList())
        if (def.first) {
            val holder = state.firstClaims[titleId]
            if (holder != null && holder != WorldState.PLAYER_CLAIM) {
                return Result(state, listOf(Notice(NoticeKind.WARNING, texts.formatL("unvan_alinmis", "name" to def.name))))
            }
        }
        var stats = state.player.stats
        def.statBonus.forEach { (stat, bonus) -> stats = stats.plus(stat, bonus) }
        val player = state.player.copy(titles = state.player.titles + titleId, stats = stats).clampResources()
        val claims = if (def.first) state.firstClaims + (titleId to WorldState.PLAYER_CLAIM) else state.firstClaims
        // Yeni tavan mevcut itibarı da kırpar.
        val reputation = state.reputation.mapValues { (f, v) -> def.reputationCap[f]?.let { v.coerceAtMost(it) } ?: v } +
            def.reputationCap.filterKeys { it !in state.reputation }.mapValues { (_, cap) -> 0.coerceAtMost(cap) }
        val lines = buildList {
            add(def.description)
            if (def.statBonus.isNotEmpty()) add(texts.formatL("unvan_bonus", "bonus" to statList(def.statBonus)))
        }
        val notice = Notice(NoticeKind.TITLE, texts.formatL("unvan_kazanildi", "name" to def.name), lines, def.rarity)
        return Result(state.copy(player = player, firstClaims = claims, reputation = reputation), listOf(notice))
    }

    /** İçerikte tanımlıysa unvanı verir (motor kancaları içerik dosyasına sıkı bağlı kalmasın). */
    fun grantTitleIfDefined(state: WorldState, titleId: String): Result =
        if (titleId in content.titles) grantTitle(state, titleId) else Result(state, emptyList())

    /** Bir NPC "İlk" unvanını kapar (Aşama 5'te rakipler kullanır). */
    fun claimFirstByNpc(state: WorldState, titleId: String, npcId: String): WorldState =
        if (state.firstClaims.containsKey(titleId)) state else state.copy(firstClaims = state.firstClaims + (titleId to npcId))

    // ---- Yetenekler ----

    fun grantSkill(state: WorldState, skillId: String): Result {
        val def = content.skills[skillId] ?: error("Tanımsız yetenek: $skillId")
        if (state.player.skill(skillId) != null) return Result(state, emptyList())
        var p = state.player.copy(skills = state.player.skills + SkillState(skillId))
        val lines = mutableListOf(def.description)
        var corruption = state.corruption
        if (def.penalty.isNotEmpty()) {
            p = applyPenalty(p, def.penalty)
            lines += texts.formatL("ceza_yetenegi", "penalty" to statList(def.penalty))
            corruption = (corruption + PENALTY_CORRUPTION).coerceAtMost(100)
        }
        val claimed = if (def.rarity == com.yukselis.engine.system.Rarity.UNIQUE) {
            state.firstClaims + (WorldState.UNIQUE_CLAIM_PREFIX + skillId to WorldState.PLAYER_CLAIM)
        } else {
            state.firstClaims
        }
        val tab = unlockTab(state.copy(player = p, firstClaims = claimed, corruption = corruption), WorldState.TAB_SKILLS)
        return Result(
            tab.state,
            listOf(Notice(NoticeKind.SKILL, texts.formatL("yetenek_kazanildi", "name" to def.name), lines, def.rarity)) + tab.notices,
        )
    }

    private fun applyPenalty(p: PlayerState, penalty: Map<Stat, Int>): PlayerState {
        var stats = p.stats
        penalty.forEach { (stat, d) -> stats = stats.plus(stat, d) }
        return p.copy(stats = stats).clampResources()
    }

    private fun skillXp(state: WorldState, skillId: String, amount: Int): Result {
        val current = state.player.skill(skillId) ?: return Result(state, emptyList())
        if (amount <= 0 || current.level >= Progression.SKILL_MAX_LEVEL) return Result(state, emptyList())
        var s = current.copy(xp = current.xp + amount)
        while (s.level < Progression.SKILL_MAX_LEVEL && s.xp >= Progression.skillXpToNext(s.level)) {
            s = s.copy(xp = s.xp - Progression.skillXpToNext(s.level), level = s.level + 1)
        }
        if (s.level >= Progression.SKILL_MAX_LEVEL) s = s.copy(xp = 0)
        val next = state.withPlayer { p -> p.copy(skills = p.skills.map { if (it.id == skillId) s else it }) }
        val notices = if (s.level > current.level) {
            val def = content.skills[skillId]
            listOf(Notice(NoticeKind.SKILL, texts.formatL("yetenek_seviye", "name" to (def?.name ?: LText(skillId, skillId)), "level" to num(s.level))))
        } else {
            emptyList()
        }
        return Result(next, notices)
    }

    /** Yetenek evrimi: eski yetenek kalkar, seçilen dal seviye 1'den başlar. */
    fun evolveSkill(state: WorldState, from: String, to: String): Result {
        val fromDef = content.skills[from] ?: error("Tanımsız yetenek: $from")
        val toDef = content.skills[to] ?: error("Tanımsız yetenek: $to")
        if (to !in fromDef.evolutions) throw ActionRejected("$from → $to evrimi tanımlı değil")
        if ((state.player.skill(from)?.level ?: 0) < Progression.SKILL_MAX_LEVEL) throw ActionRejected("$from maksimum seviyede değil")
        val removed = state.withPlayer { p -> p.copy(skills = p.skills.filterNot { it.id == from }) }
        val granted = grantSkill(removed, to)
        // grantSkill'in "kazanıldı" bildirimi yerine evrim bildirimi; ceza satırları korunur.
        val penaltyLines = granted.notices.firstOrNull()?.lines?.drop(1) ?: emptyList()
        val notice = Notice(NoticeKind.EVOLUTION, texts.formatL("yetenek_evrildi", "from" to fromDef.name, "to" to toDef.name), listOf(toDef.description) + penaltyLines, toDef.rarity)
        return Result(granted.state, listOf(notice) + granted.notices.drop(1))
    }

    fun fuse(state: WorldState, recipeId: String): Result {
        val recipe = content.fusions.firstOrNull { it.id == recipeId } ?: throw ActionRejected("Tanımsız kaynaştırma: $recipeId")
        if (!recipe.inputs.all { (state.player.skill(it)?.level ?: 0) >= Progression.SKILL_MAX_LEVEL }) {
            throw ActionRejected("Kaynaştırma girdileri artık yok ya da maksimum seviyede değil")
        }
        val removed = state.withPlayer { p -> p.copy(skills = p.skills.filterNot { it.id in recipe.inputs }) }
        val granted = grantSkill(removed, recipe.result)
        val result = content.skills.getValue(recipe.result)
        val penaltyLines = granted.notices.firstOrNull()?.lines?.drop(1) ?: emptyList()
        return Result(
            granted.state,
            listOf(Notice(NoticeKind.EVOLUTION, texts.formatL("kaynastirma_tamam", "result" to result.name), listOf(result.description) + penaltyLines, result.rarity)) +
                granted.notices.drop(1),
        )
    }

    // ---- Eşyalar ----

    fun addItem(state: WorldState, itemId: String, count: Int): Result {
        if (count == 0) return Result(state, emptyList())
        val def = content.items[itemId] ?: error("Tanımsız eşya: $itemId")
        val have = state.player.itemCount(itemId)
        val after = (have + count).coerceAtLeast(0)
        val inventory = if (after == 0) state.player.inventory - itemId else state.player.inventory + (itemId to after)
        val key = if (count > 0) "esya_kazanildi" else "esya_kaybedildi"
        val shown = if (count > 0) count else have - after
        val base = Result(
            state.withPlayer { it.copy(inventory = inventory) },
            listOf(Notice(NoticeKind.SYSTEM, texts.formatL(key, "name" to def.name, "n" to num(shown)))),
        )
        if (count <= 0) return base
        val tab = unlockTab(base.state, WorldState.TAB_BAG)
        return Result(tab.state, base.notices + tab.notices)
    }

    // ---- Öz ----

    private fun essenceInsight(state: WorldState, essenceId: String, amount: Int): Result {
        val def = content.essences[essenceId] ?: error("Tanımsız Öz: $essenceId")
        val value = ((state.player.essenceInsight[essenceId] ?: 0) + amount).coerceIn(0, Progression.ESSENCE_INSIGHT_TO_BIND)
        return Result(
            state.withPlayer { it.copy(essenceInsight = it.essenceInsight + (essenceId to value)) },
            listOf(Notice(NoticeKind.STATUS, texts.formatL("oz_kavrayis", "name" to def.name, "n" to num(amount)))),
        )
    }

    // ---- Gün ve Günlük Görev ----

    private fun advanceDays(state: WorldState, days: Int): Result {
        if (days <= 0) return Result(state, emptyList())
        val notices = mutableListOf<Notice>()
        var s = state
        val old = s.daily
        if (days > 1 && old?.completed == true) {
            // Aradaki günlerde görev yapılmadı: seri kopar.
            s = s.copy(dailyStreak = 0)
            notices += Notice(NoticeKind.WARNING, texts.format("gunluk_kacirildi"))
        }
        if (old != null && !old.completed) {
            val rng = Rng(s.rngState)
            val penalty = rng.chance(PENALTY_CHANCE)
            s = s.copy(rngState = rng.state, dailyStreak = 0, penaltyPending = s.penaltyPending || penalty)
            notices += Notice(NoticeKind.WARNING, texts.format("gunluk_kacirildi"))
        }
        s = GameClock.advance(s, days)
        val fresh = newDaily(s)
        return Result(fresh.state, notices + fresh.notices)
    }

    /** O günün Günlük Görevini şablonlardan rastgele üretir. */
    fun newDaily(state: WorldState): Result {
        val templates = content.dailyTemplates.values.filter { it.condition?.test(state) ?: true }.sortedBy { it.id }
        if (templates.isEmpty()) return Result(state.copy(daily = null), emptyList())
        val rng = Rng(state.rngState)
        val t = templates[rng.nextInt(templates.size)]
        val quest = DailyQuest(templateId = t.id, day = state.day, steps = t.steps)
        return Result(
            state.copy(daily = quest, rngState = rng.state),
            listOf(Notice(NoticeKind.QUEST, texts.formatL("gunluk_yeni", "title" to t.title), listOf(t.description))),
        )
    }

    private fun dailyProgress(state: WorldState, kind: String, amount: Int): Result {
        val quest = state.daily ?: return Result(state, emptyList())
        val template = content.dailyTemplates[quest.templateId] ?: return Result(state, emptyList())
        if (quest.completed || template.kind != kind || amount <= 0) return Result(state, emptyList())
        val progress = (quest.progress + amount).coerceAtMost(quest.steps)
        if (progress < quest.steps) return Result(state.copy(daily = quest.copy(progress = progress)), emptyList())
        // Tamamlandı: +1 serbest puan ve şablon XP'si (GDD: Günlük Görev).
        val streak = state.dailyStreak + 1
        val points = Progression.dailyReward(streak)
        val done = state.copy(daily = quest.copy(progress = progress, completed = true), dailyStreak = streak)
            .withPlayer { it.copy(freePoints = it.freePoints + points) }
        val notice = Notice(NoticeKind.QUEST, texts.format("gunluk_tamam"), listOf(texts.format("gunluk_odul", "n" to points, "xp" to template.xp)))
        val xp = gainXp(done, template.xp.toLong())
        val title = when (streak) {
            STREAK_TITLE_WEEK -> grantTitleIfDefined(xp.state, TITLE_STREAK_WEEK)
            STREAK_TITLE_MONTH -> grantTitleIfDefined(xp.state, TITLE_STREAK_MONTH)
            else -> Result(xp.state, emptyList())
        }
        return Result(title.state, listOf(notice) + xp.notices + title.notices)
    }

    // ---- Tetikleyiciler ----

    private fun offerProfession(state: WorldState): Result {
        if (state.player.profession != null || content.professions.isEmpty() || state.prompts.any { it.kind == PromptKind.PROFESSION }) {
            return Result(state, emptyList())
        }
        val prompt = SystemPrompt(PromptKind.PROFESSION, content.professions.keys.toList())
        return Result(state.copy(prompts = state.prompts + prompt), listOf(Notice(NoticeKind.QUEST, texts.format("meslek_teklifi"))))
    }

    fun checkTriggers(state: WorldState): Result {
        var s = state
        val notices = mutableListOf<Notice>()
        fun enqueue(prompt: SystemPrompt, notice: Notice) {
            if (s.prompts.none { it.kind == prompt.kind && it.subject == prompt.subject }) {
                s = s.copy(prompts = s.prompts + prompt)
                notices += notice
            }
        }
        val p = s.player

        if (p.classId == null && p.level >= Progression.CLASS_OFFER_LEVEL && content.classes.isNotEmpty()) {
            val options = classOffer(s)
            if (options.isNotEmpty()) enqueue(SystemPrompt(PromptKind.CLASS_OFFER, options), Notice(NoticeKind.CLASS, texts.format("sinif_teklifi")))
        }

        if (p.level >= Progression.PROFESSION_OFFER_LEVEL) {
            val r = offerProfession(s)
            s = r.state
            notices += r.notices
        }

        for (skill in p.skills) {
            val def = content.skills[skill.id] ?: continue
            if (skill.level >= Progression.SKILL_MAX_LEVEL && def.evolutions.size == 2 && !s.hasFlag(declinedFlag(PromptKind.SKILL_EVOLUTION, skill.id))) {
                enqueue(
                    SystemPrompt(PromptKind.SKILL_EVOLUTION, def.evolutions + PromptKind.DECLINE, skill.id),
                    Notice(NoticeKind.SKILL, texts.formatL("yetenek_evrim_teklifi", "name" to def.name), rarity = def.rarity),
                )
            }
        }

        for (recipe in content.fusions) {
            val ready = recipe.inputs.all { (p.skill(it)?.level ?: 0) >= Progression.SKILL_MAX_LEVEL } &&
                (recipe.condition?.test(s) ?: true) && !s.hasFlag(declinedFlag(PromptKind.FUSION, recipe.id))
            if (ready) {
                val a = content.skills[recipe.inputs[0]]?.name ?: LText.EMPTY
                val b = content.skills[recipe.inputs.getOrNull(1)]?.name ?: LText.EMPTY
                val result = content.skills[recipe.result]?.name ?: LText.EMPTY
                enqueue(
                    SystemPrompt(PromptKind.FUSION, listOf(PromptKind.ACCEPT, PromptKind.DECLINE), recipe.id),
                    Notice(NoticeKind.SKILL, texts.formatL("kaynastirma_teklifi", "a" to a, "b" to b, "result" to result)),
                )
            }
        }

        val slots = Progression.essenceSlots(p.grade)
        if (p.essences.size < slots) {
            p.essenceInsight.filter { (id, v) ->
                v >= Progression.ESSENCE_INSIGHT_TO_BIND && id !in p.essences && !s.hasFlag(declinedFlag(PromptKind.ESSENCE_BIND, id))
            }.keys.sorted().firstOrNull()?.let { id ->
                val name = content.essences[id]?.name ?: LText(id, id)
                enqueue(
                    SystemPrompt(PromptKind.ESSENCE_BIND, listOf(PromptKind.ACCEPT, PromptKind.DECLINE), id),
                    Notice(NoticeKind.SKILL, texts.formatL("oz_baglama_teklifi", "name" to name)),
                )
            }
        }
        return Result(s, notices)
    }

    /**
     * Seviye 10 sınıf teklifi (OP MC: gizli sınıf garantisi). En güçlü iki eğilimin temel
     * sınıfları + her zaman bir gizli sınıf: koşulu sağlanan ilki, yoksa en güçlü eğilime en
     * yakın gizli sınıf ("Yankı'nın önerisi"). Koşulu sağlanan nadir sınıf dördüncü seçenek olur.
     */
    fun classOffer(state: WorldState): List<String> {
        val ranked = state.player.affinity.ranked()
        val all = content.classes.values.sortedBy { it.id }
        val bases = all.filter { it.kind == ClassKind.BASE }
        fun baseFor(a: Affinity) = bases.firstOrNull { it.affinity == a }?.id
        val picks = mutableListOf<String>()
        for (a in ranked) {
            if (picks.size == 2) break
            baseFor(a)?.let { if (it !in picks) picks += it }
        }
        val hidden = all.filter { it.kind == ClassKind.HIDDEN }
        val unlocked = hidden.firstOrNull { it.unlock?.test(state) == true }
        (unlocked ?: echoSuggestion(state))?.let { picks += it.id }
        all.firstOrNull { it.kind == ClassKind.RARE && it.unlock?.test(state) == true }?.let { picks += it.id }
        return picks
    }

    /** Koşulu sağlanmamış gizli sınıflar arasından oyun tarzına en yakın olanı. */
    fun echoSuggestion(state: WorldState): com.yukselis.engine.system.ClassDef? {
        val hidden = content.classes.values.filter { it.kind == ClassKind.HIDDEN }.sortedBy { it.id }
        val ranked = state.player.affinity.ranked()
        return ranked.firstNotNullOfOrNull { a -> hidden.firstOrNull { a in it.affinities } } ?: hidden.firstOrNull()
    }

    /** Gizli sınıf, koşulu sağlanmadan Yankı tarafından mı önerildi? */
    fun isEchoSuggestion(state: WorldState, classId: String): Boolean {
        val c = content.classes[classId] ?: return false
        return c.kind == ClassKind.HIDDEN && c.unlock?.test(state) != true
    }

    /**
     * OP MC garantisi: oyuncunun oyun tarzına (en güçlü eğilim) bağlı Benzersiz yetenek.
     * Dünyada tek sahiplidir; zaten bir Benzersiz yeteneği varsa verilmez.
     */
    fun grantUniqueSkill(state: WorldState): Result {
        val owned = state.player.skills.any { content.skills[it.id]?.rarity == com.yukselis.engine.system.Rarity.UNIQUE }
        if (owned) return Result(state, emptyList())
        val uniques = content.skills.values.filter { it.rarity == com.yukselis.engine.system.Rarity.UNIQUE && it.affinity != null }
        val pick = state.player.affinity.ranked().firstNotNullOfOrNull { a ->
            uniques.firstOrNull { it.affinity == a && !state.firstClaims.containsKey(WorldState.UNIQUE_CLAIM_PREFIX + it.id) }
        } ?: return Result(state, emptyList())
        val r = grantSkill(state, pick.id)
        val echo = Notice(NoticeKind.ECHO, texts.format("benzersiz_yetenek"), listOf(texts.format("benzersiz_yetenek_satir")))
        return Result(r.state, listOf(echo) + r.notices)
    }

    /** Kademe Kırma ödülü: sahibi olmayan bir Benzersiz yetenek (zaten bir tane olsa bile). */
    fun grantExtraUniqueSkill(state: WorldState): Result {
        val uniques = content.skills.values.filter { it.rarity == com.yukselis.engine.system.Rarity.UNIQUE && it.affinity != null }
        val pick = state.player.affinity.ranked().firstNotNullOfOrNull { a ->
            uniques.firstOrNull {
                it.affinity == a && state.player.skill(it.id) == null && !state.firstClaims.containsKey(WorldState.UNIQUE_CLAIM_PREFIX + it.id)
            }
        } ?: return Result(state, emptyList())
        val r = grantSkill(state, pick.id)
        val echo = Notice(NoticeKind.ECHO, texts.format("benzersiz_yetenek"), listOf(texts.format("benzersiz_yetenek_satir")))
        return Result(r.state, listOf(echo) + r.notices)
    }

    /** Kendini açan arayüz: sekmeyi verir ve kendi bildirimini üretir; zaten açıksa sessiz. */
    fun unlockTab(state: WorldState, tab: String): Result {
        if (tab in state.unlockedTabs) return Result(state, emptyList())
        val key = "sekme_$tab".takeIf { texts[it] != null } ?: "sekme_bilinmeyen"
        val notice = Notice(NoticeKind.SYSTEM, texts.format(key))
        return Result(state.copy(unlockedTabs = state.unlockedTabs + tab), listOf(notice))
    }

    // ---- yardımcılar ----

    private fun statList(stats: Map<Stat, Int>): LText {
        fun s(v: Int) = if (v >= 0) "+$v" else "$v"
        return LText(
            stats.entries.joinToString(", ") { "${it.key.abbr.tr} ${s(it.value)}" },
            stats.entries.joinToString(", ") { "${it.key.abbr.en} ${s(it.value)}" },
        )
    }

    private fun num(n: Int) = LText(n.toString(), n.toString())

    private fun signed(text: LText, amount: Int): LText =
        if (amount >= 0) LText("+${text.tr}", "+${text.en}") else text

    companion object {
        const val GENERIC_EVOLUTION = "genel"

        /** Kule'de en yüksek temizlenen kat. */
        const val TOWER_VAR = "kule_kat"

        val ATTENTION_THRESHOLDS = listOf(25, 50, 75, 100)
        val CORRUPTION_THRESHOLDS = listOf(25, 50, 75, 100)

        /** Dikkat eşiği işareti: Perde 1'de eski ad (kayıt uyumu), sonra perde başına. */
        fun attentionFlag(threshold: Int, act: Int): String = if (act <= 1) "dikkat_esik_$threshold" else "dikkat_esik_${threshold}_p$act"
        const val FEAT_ATTENTION = 2
        const val PENALTY_CORRUPTION = 10

        /** İtibar dalgası: A'ya d itibar → B'ye d · ilişki(A,B)/100 · 0,3. */
        const val RIPPLE = 0.3

        const val STREAK_TITLE_WEEK = 7
        const val STREAK_TITLE_MONTH = 30
        const val TITLE_STREAK_WEEK = "disiplinli"
        const val TITLE_STREAK_MONTH = "kirilmaz_rutin"
        const val TITLE_FIRST_CLASS = "ilk_siniflanan"
        const val TITLE_FIRST_EVOLUTION = "ilk_evrilen"
        const val TITLE_FIRST_CRAFT = "ilk_zanaatkar"

        /** İhmal edilen Günlük Görevde Ceza Bölgesi'ne ışınlanma olasılığı (DECISIONS #23). */
        const val PENALTY_CHANCE = 0.5

        fun declinedFlag(kind: PromptKind, subject: String) = "_red_${kind.key}_$subject"
    }
}

internal fun WorldState.withPlayer(transform: (PlayerState) -> PlayerState): WorldState = copy(player = transform(player))

internal fun PlayerState.clampResources(): PlayerState = copy(hp = hp.coerceAtMost(maxHp), mp = mp.coerceAtMost(maxMp))
