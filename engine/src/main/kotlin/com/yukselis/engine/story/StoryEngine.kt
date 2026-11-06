package com.yukselis.engine.story

import com.yukselis.engine.combat.CombatAction
import com.yukselis.engine.combat.CombatEngine
import com.yukselis.engine.combat.StatusEffects
import com.yukselis.engine.core.GameMode
import com.yukselis.engine.core.HistoryEntry
import com.yukselis.engine.core.PromptKind
import com.yukselis.engine.core.SystemPrompt
import com.yukselis.engine.core.WorldState
import com.yukselis.engine.i18n.LText
import com.yukselis.engine.i18n.Lang
import com.yukselis.engine.i18n.NameEntry
import com.yukselis.engine.i18n.TokenResolver
import com.yukselis.engine.system.ClassKind
import com.yukselis.engine.system.Grade
import com.yukselis.engine.system.ItemType
import com.yukselis.engine.system.Progression
import com.yukselis.engine.system.Rarity
import com.yukselis.engine.system.Stat

/**
 * Hikaye oynatıcısı. Saf fonksiyonlar: durum alır, yeni durum döndürür.
 * Aşama 4'te olay havuzu ve Hikaye Yönetmeni bunun yanına gelecek.
 */
class StoryEngine(val content: Content) {
    companion object {
        const val MEMORY_SHOWN = 5
        private const val DENIZ = "deniz_arman"
        const val RANK_SHOWN = 10
    }

    private val resolver = TokenResolver(content.names)
    private val effects = EffectApplier(content)
    private val actions = PlayerActions(content, effects)
    private val combat = CombatEngine(content, effects)
    val world = com.yukselis.engine.world.WorldEngine(content, effects, combat)

    // ---- Gün akışı ----

    fun hubAction(state: WorldState, action: com.yukselis.engine.world.HubAction, lang: Lang): Outcome {
        val r = world.act(state, action)
        return Outcome(r.state, r.notices.map { noticeView(it, r.state, lang) })
    }

    fun map(state: WorldState, lang: Lang): List<RegionView> {
        val here = state.region?.let { content.regions[it] }
        return content.regions.values.sortedBy { it.act * 1000 + it.baseDanger }.map { d ->
            val r = state.regions[d.id]
            RegionView(
                id = d.id,
                name = world.regionName(d.id)[lang],
                danger = r?.danger ?: d.baseDanger,
                control = controlName(r?.control ?: d.control, lang),
                gates = r?.gates?.map { gateView(it, state, lang) } ?: emptyList(),
                current = d.id == state.region,
                neighbor = here != null && d.id in here.neighbors,
            )
        }
    }

    fun journal(state: WorldState, lang: Lang): JournalView = JournalView(
        quests = state.quests.mapNotNull { (id, st) -> content.quests[id]?.let { QuestView(text(it.name[lang], state, lang), text(it.description[lang], state, lang), st) } },
        legend = state.legend.mapNotNull { (id, n) -> content.feats[id]?.let { LegendView(it.name[lang], n) } },
        pendingSeeds = state.seeds.size,
        knowledge = state.knowledge.reversed().mapNotNull { id ->
            content.knowledge[id]?.let { KnowledgeView(text(it.name[lang], state, lang), text(it.description[lang], state, lang), it.kind) }
        },
    )

    // ---- NPC'ler (Aşama 5) ----

    val npcEngine get() = effects.npc

    /** İlişkiler ekranı: tanınan NPC'ler, dört eksen, son hatıralar, durum ve izler. */
    fun relations(state: WorldState, lang: Lang): RelationsView {
        val t = content.texts
        val npcs = content.npcs.values.mapNotNull { d ->
            val n = state.npcs[d.id]?.takeIf { it.known } ?: return@mapNotNull null
            val status = buildList {
                when {
                    !n.alive -> add(t.format("npc_durum_olu")[lang])
                    n.status == com.yukselis.engine.npc.NpcStatus.GONE -> add(t.format("npc_durum_gitti")[lang])
                    n.companion -> add(t.format("npc_durum_yoldas")[lang])
                }
                n.faction?.let { add(npcEngine.factionName(it)[lang]) }
                if (com.yukselis.engine.npc.NpcEngine.MARK_RIVAL in n.marks) add(t.format("npc_durum_rakip")[lang])
            }.joinToString(" · ")
            NpcView(
                id = d.id,
                name = npcEngine.name(d.id)[lang],
                fullName = npcEngine.fullName(d.id)[lang],
                description = text(d.description[lang], state, lang),
                trust = n.relation.trust,
                respect = n.relation.respect,
                fear = n.relation.fear,
                bond = n.relation.bond,
                level = n.level,
                gradeName = Grade.forLevel(n.level.coerceAtLeast(1)).name,
                status = status,
                alive = n.alive,
                companion = n.companion,
                memories = n.memories.sortedWith(compareByDescending<com.yukselis.engine.npc.Memory> { it.day }.thenByDescending { kotlin.math.abs(it.weight) })
                    .mapNotNull { m -> npcEngine.memoryText(d.id, m)?.let { text(it[lang], state.copy(focusNpc = d.id), lang) } }
                    .distinct().take(MEMORY_SHOWN),
                traits = n.traits.mapNotNull { content.traits[it]?.name?.get(lang) },
                goal = npcEngine.goalName(state, d.id)?.get(lang)?.takeIf { n.alive },
            )
        }.sortedWith(compareByDescending<NpcView> { it.companion }.thenByDescending { it.alive }.thenBy { it.name })
        return RelationsView(
            npcs = npcs,
            companions = npcEngine.companions(state).size,
            capacity = npcEngine.capacity(state),
            armyPower = npcEngine.armyPower(state),
            army = state.player.army.entries.sortedBy { it.key }.mapNotNull { (id, k) -> content.army[id]?.let { "${it.name[lang]} ×$k" } },
        )
    }

    /** Küresel Sıralama: dünyanın gördüğü liste (görünen seviyen) + gerçek yerin. */
    fun ranking(state: WorldState, lang: Lang): RankingView {
        val list = npcEngine.ranking(state, public = true)
        val rows = list.mapIndexed { i, e -> i to e }.filter { (i, e) -> i < RANK_SHOWN || e.kind == com.yukselis.engine.npc.NpcEngine.RankKind.PLAYER }
            .map { (i, e) ->
                val name = when (e.kind) {
                    com.yukselis.engine.npc.NpcEngine.RankKind.PLAYER -> state.playerName
                    com.yukselis.engine.npc.NpcEngine.RankKind.NPC -> npcEngine.fullName(e.id)[lang]
                    com.yukselis.engine.npc.NpcEngine.RankKind.HUNTER -> state.hunters.firstOrNull { "avci_${it.uid}" == e.id }?.let { npcEngine.hunterName(it)[lang] } ?: "?"
                }
                RankRowView(i + 1, name, e.level, Grade.forLevel(e.level.coerceAtLeast(1)).name, e.kind == com.yukselis.engine.npc.NpcEngine.RankKind.PLAYER, e.kind == com.yukselis.engine.npc.NpcEngine.RankKind.NPC)
            }
        val pub = npcEngine.playerRank(state, public = true)
        val real = npcEngine.playerRank(state, public = false)
        val note = if (real < pub) content.texts.format("sira_gizli", "n" to real)[lang] else content.texts.format("sira_acik")[lang]
        return RankingView(rows, pub, real, list.size, note)
    }

    private fun controlName(control: String, lang: Lang): String =
        content.names.find(NameEntry.Kind.FACTION, control)?.full?.get(lang) ?: content.texts.format("kontrol_$control")[lang]

    private fun gateView(g: com.yukselis.engine.world.Gate, state: WorldState, lang: Lang) = GateView(
        uid = g.uid,
        grade = g.grade.name,
        enemies = g.enemies.mapNotNull { content.enemies[it]?.name?.get(lang) }.groupingBy { it }.eachCount()
            .entries.joinToString(", ") { (n, k) -> if (k > 1) "$n ×$k" else n },
        daysLeft = g.deadline - state.day,
    )

    private fun hubView(state: WorldState, lang: Lang): HubView {
        val d = state.dayState
        val region = state.region
        val here = region?.let { content.regions[it] }
        return HubView(
            day = state.day,
            slotsLeft = d.slotsLeft,
            slotsTotal = d.slotsTotal,
            regionName = region?.let { world.regionName(it)[lang] } ?: "",
            danger = region?.let { state.regions[it]?.danger } ?: 0,
            cards = d.hand.mapNotNull { id -> content.events[id]?.let { CardView(id, text(it.card[lang], state, lang), it.tension) } },
            gates = region?.let { state.regions[it] }?.gates?.map { gateView(it, state, lang) } ?: emptyList(),
            travel = here?.neighbors?.map { RegionLinkView(it, world.regionName(it)[lang]) } ?: emptyList(),
            mustCamp = d.slotsLeft <= 0,
        )
    }

    // ---- Savaş ----

    /** Savaş eylemi (geçersizse ActionRejected). */
    fun combatAction(state: WorldState, action: CombatAction, lang: Lang): Outcome {
        val r = combat.act(state, action)
        var s = r.state
        val notices = r.notices.toMutableList()
        // Gün akışından açılan savaş bitti (keşif, kapı): akışa dön, kapıyı kapat.
        if (state.combat != null && s.combat == null && s.sceneId == WorldState.HUB && !s.ended) {
            val back = world.afterCombat(s)
            s = back.state
            notices += back.notices
        }
        return Outcome(s, notices.map { noticeView(it, s, lang) })
    }

    /** Zar kontrolünün görünür olasılığı (yüzde): P(d20 + stat ≥ zorluk). */
    fun odds(state: WorldState, c: com.yukselis.engine.world.DiceCheck): Int {
        val need = c.difficulty - state.player.stats[c.stat]
        return ((21 - need).coerceIn(0, 20) * 5)
    }

    private fun oneShotFlag(scene: String, choice: String) = "_zar_${scene}_$choice"

    /** Savaşı doğrudan başlatır (olay motoru ve testler için; hikayede `savas` etkisi kullanılır). */
    fun startCombat(state: WorldState, spec: com.yukselis.engine.combat.CombatSpec, lang: Lang): Outcome {
        val r = combat.start(state, spec)
        return Outcome(r.state, r.notices.map { noticeView(it, r.state, lang) })
    }

    /** Otomatik tur için önerilen eylem. */
    fun autoCombatAction(state: WorldState): CombatAction = combat.autoAction(state)

    fun combatView(state: WorldState, lang: Lang): CombatView? {
        val c = state.combat ?: return null
        val t = content.texts
        val p = state.player
        fun statuses(list: List<com.yukselis.engine.combat.StatusInstance>) =
            list.map { "${t.format("durum_${it.effect}")[lang]} (${it.turns})" }
        val enemies = c.enemies.map { e ->
            val d = combat.enemyDef(e.id, state) ?: return null
            EnemyView(
                uid = e.uid,
                name = d.name[lang],
                description = text(d.description[lang], state, lang),
                level = e.level,
                gradeName = d.grade.name,
                hp = e.hp,
                maxHp = e.maxHp,
                stats = Stat.entries.map { StatLine(it, it.abbr[lang], it.label[lang], e.stats[it]) },
                statuses = statuses(e.statuses),
                weakness = if (e.analyzed) d.weaknessText?.get(lang) else null,
                boss = d.boss,
                phase = e.phase,
                intelligent = d.intelligent,
                alive = e.alive,
            )
        }
        val skills = p.skills.mapNotNull { s ->
            val d = content.skills[s.id] ?: return@mapNotNull null
            if (d.type != com.yukselis.engine.system.SkillType.ACTIVE) return@mapNotNull null
            val cd = c.cooldowns[d.id] ?: 0
            CombatSkillView(
                id = d.id,
                name = d.name[lang],
                costMp = d.costMp,
                costStamina = d.costStamina,
                cooldownLeft = cd,
                usable = cd == 0 && p.mp >= d.costMp && c.stamina >= d.costStamina,
                targetsAll = d.target == com.yukselis.engine.system.SkillTarget.ALL,
                self = d.target == com.yukselis.engine.system.SkillTarget.SELF || d.heals,
            )
        }
        val items = p.inventory.entries.sortedBy { it.key }.mapNotNull { (id, n) ->
            val d = content.items[id] ?: return@mapNotNull null
            if (d.type != ItemType.CONSUMABLE || d.use == null) return@mapNotNull null
            ItemView(id, d.name[lang], d.description[lang], n, d.rarity, true)
        }
        val execution = c.pendingExecution?.let { uid -> c.enemy(uid) }?.let { pending ->
            val d = combat.enemyDef(pending.id, state) ?: return null
            val sfx = if (pending.id.startsWith(com.yukselis.engine.npc.NpcEngine.ENEMY_PREFIX)) "_npc" else ""
            ExecutionView(
                enemyName = d.name[lang],
                options = CombatEngine.EXECUTIONS.map { id ->
                    PromptOptionView(id, t.format("infaz_$id$sfx")[lang], t.format("infaz_$id${sfx}_aciklama")[lang])
                },
            )
        }
        return CombatView(
            round = c.round,
            enemies = enemies,
            stamina = c.stamina,
            maxStamina = c.maxStamina,
            stance = c.stance,
            effort = c.effort,
            playerStatuses = statuses(c.playerStatuses),
            log = c.log.map { CombatLogView(resolveLog(it.prose, state, lang), it.numbers?.get(lang)) },
            environment = c.spec.environment.map { EnvironmentView(it, t.format("ortam_$it")[lang], it in c.usedEnvironment) },
            skills = skills,
            items = items,
            execution = execution,
            minorUsed = c.minorUsed,
            extraActions = c.extraActions,
            dead = state.dead,
            allies = c.allies.map { a ->
                val role = content.npcs[a.npc]?.combat?.role?.key ?: ""
                AllyView(npcEngine.name(a.npc)[lang], a.level, a.hp, a.maxHp, a.down, t.format("rol_$role")[lang])
            },
        )
    }

    private fun resolveLog(text: LText, state: WorldState, lang: Lang) = this.text(text[lang], state, lang)

    /** Yeni oyun: Gün 0'ın Günlük Görevi de üretilir. */
    fun newGame(playerName: String, mode: GameMode, seed: Long): WorldState =
        npcEngine.ensure(effects.newDaily(WorldState.newGame(seed, playerName, mode, content.startScene)).state)

    // ---- Hikaye dışı eylemler (geçersizse ActionRejected) ----

    fun resolvePrompt(state: WorldState, optionId: String, lang: Lang, kind: PromptKind? = null, subject: String? = null): Outcome =
        outcome(actions.resolvePrompt(state, optionId, kind, subject), lang)

    /**
     * Yüklenen kaydı içerikle uzlaştırır: artık var olmayan içeriğe işaret eden istemleri atar,
     * Günlük Görevi olmayan (eski) kayda o günün görevini üretir. Kayıt yüklenince çağrılır.
     */
    fun sanitize(state: WorldState): WorldState {
        val valid = state.prompts.filter { p ->
            when (p.kind) {
                PromptKind.CLASS_OFFER -> p.options.all { it in content.classes }
                PromptKind.EVOLUTION -> p.options.all { it == EffectApplier.GENERIC_EVOLUTION || it in content.classes }
                PromptKind.PROFESSION -> p.options.all { it in content.professions }
                PromptKind.SKILL_EVOLUTION -> p.subject in content.skills && p.options.all { it == PromptKind.DECLINE || it in content.skills }
                PromptKind.FUSION -> content.fusions.any { it.id == p.subject }
                PromptKind.ESSENCE_BIND -> p.subject in content.essences
            }
        }
        var s = state.copy(prompts = valid)
        s.combat?.let { c ->
            val scenesOk = listOfNotNull(c.spec.win, c.spec.lose, c.spec.flee).all { it == WorldState.HUB || it in content.scenes }
            if (!scenesOk || c.enemies.any { combat.enemyDef(it.id, s) == null } || c.allies.any { it.npc !in content.npcs }) s = s.copy(combat = null)
        }
        if (s.daily == null || s.daily?.templateId !in content.dailyTemplates) s = effects.newDaily(s).state
        if (s.sceneId != WorldState.HUB && s.sceneId !in content.scenes) s = s.copy(sceneId = if (s.regions.isNotEmpty()) WorldState.HUB else content.startScene)
        s = s.copy(seeds = s.seeds.filter { it.eventId in content.events }, dayState = s.dayState.copy(hand = s.dayState.hand.filter { it in content.events }))
        // NPC'ler: tanımsızlar atılır, eksikler (eski kayıt) içerikten kurulur.
        val fresh = content.npcs.keys.filter { s.npcs[it]?.initialized != true }.toSet()
        s = npcEngine.ensure(s.copy(npcs = s.npcs.filterKeys { it in content.npcs }))
        // v2 kaydı: Deniz'le birlikte uyanılmış ya da bulunmuşsa yoldaş olarak kurulur (Aşama 5 içeriğiyle uyumlu).
        if (DENIZ in fresh && (s.hasFlag("deniz_el_tutuldu") || s.hasFlag("deniz_bulundu"))) s = npcEngine.join(s, DENIZ).state
        // Gün akışına girmiş eski kayıtta Küresel Sıralama'nın avcıları yok: kurulur.
        if (s.regions.isNotEmpty() && s.hunters.isEmpty()) s = npcEngine.setupRanking(s)
        return s
    }

    fun allocate(state: WorldState, points: Map<Stat, Int>, lang: Lang): Outcome = outcome(actions.allocate(state, points), lang)

    fun craft(state: WorldState, recipeId: String, lang: Lang): Outcome = outcome(actions.craft(state, recipeId), lang)

    fun useItem(state: WorldState, itemId: String, lang: Lang): Outcome = outcome(actions.useItem(state, itemId), lang)

    fun train(state: WorldState, lang: Lang): Outcome = outcome(actions.train(state), lang)

    /** Etkileri doğrudan uygular (testler ve ileride olay motoru için). */
    fun applyEffects(state: WorldState, list: List<Effect>, lang: Lang): Outcome = outcome(effects.applyAll(state, list), lang)

    private fun outcome(r: EffectApplier.Result, lang: Lang) = Outcome(r.state, r.notices.map { noticeView(it, r.state, lang) })

    // ---- Hikaye ----

    fun view(state: WorldState, lang: Lang): SceneView {
        if (state.sceneId == WorldState.HUB) {
            val hub = hubView(state, lang)
            val t = content.texts
            val paragraphs = listOf(text(t.formatL("akis_metin", "day" to LText("${state.day}", "${state.day}"), "region" to LText(hub.regionName, hub.regionName))[lang], state, lang)) +
                if (hub.mustCamp) listOf(t.format("akis_kamp_vakti")[lang]) else emptyList()
            return SceneView(
                day = state.day,
                act = state.act,
                placeName = hub.regionName,
                history = emptyList(),
                current = PassageView(WorldState.HUB, hub.regionName, paragraphs, emptyList()),
                choices = emptyList(),
                isEnding = false,
                prompt = state.prompts.firstOrNull()?.let { promptView(it, state, lang) },
                hub = hub,
            )
        }
        val scene = scene(state.sceneId)
        val history = state.history.mapNotNull { entry ->
            val past = content.scenes[entry.sceneId] ?: return@mapNotNull null
            val chosen = past.choices.firstOrNull { it.id == entry.choiceId }
            // Geçmiş pasaj kendi odak NPC'siyle çözülür ("@" sonraki olaya sızmasın).
            val then = state.copy(focusNpc = entry.focus)
            passage(past, then, lang).copy(chosenLabel = chosen?.let { text(it.label[lang], then, lang) })
        }
        val visibleChoices = scene.choices.filter { it.showWhen?.test(state) ?: true }
        val important = state.dayState.currentEvent?.let { content.events[it]?.important } == true
        val opinions = npcEngine.opinions(state, scene, visibleChoices, important).map { o ->
            CompanionLineView(npcEngine.name(o.npc)[lang], text(o.text[lang], state, lang), o.favored, echo = content.npcs[o.npc]?.presentWhen != null)
        }
        val choices = visibleChoices
            .map { choice ->
                val failed = choice.check?.oneShot == true && state.hasFlag(oneShotFlag(scene.id, choice.id))
                val baseTag = requirementTag(choice.requires, lang)
                val tag = if (choice.kind == com.yukselis.engine.world.ChoiceKind.FLEX && baseTag != null) {
                    baseTag.removeSuffix("]") + " · " + content.texts.format("gosteris")[lang] + "]"
                } else {
                    baseTag
                }
                ChoiceView(
                    id = choice.id,
                    label = text(choice.label[lang], state, lang),
                    requirementTag = tag,
                    enabled = (choice.requires?.test(state) ?: true) && !failed,
                    kindTag = when (choice.kind) {
                        com.yukselis.engine.world.ChoiceKind.CONCEAL -> content.texts.format("secenek_gizle")[lang]
                        com.yukselis.engine.world.ChoiceKind.REVEAL -> content.texts.format("secenek_aciga_vur")[lang]
                        else -> null
                    },
                    kind = choice.kind,
                    odds = choice.check?.let { odds(state, it) },
                    oneShot = choice.check?.oneShot == true,
                )
            }
        return SceneView(
            day = state.day,
            act = state.act,
            placeName = placeName(scene.place, lang),
            history = history,
            current = passage(scene, state, lang),
            choices = choices,
            isEnding = scene.isEnding,
            prompt = state.prompts.firstOrNull()?.let { promptView(it, state, lang) },
            opinions = opinions,
        )
    }

    fun choose(state: WorldState, choiceId: String, lang: Lang): Outcome {
        val scene = scene(state.sceneId)
        val choice = scene.choices.firstOrNull { it.id == choiceId }
            ?: throw IllegalArgumentException("'${scene.id}' sahnesinde '$choiceId' seçeneği yok")
        require(choice.showWhen?.test(state) ?: true) { "Gizli seçenek seçilemez: $choiceId" }
        require(choice.requires?.test(state) ?: true) { "Koşulu karşılanmayan seçenek: $choiceId" }
        require(state.prompts.isEmpty()) { "Önce bekleyen Sistem istemi çözülmeli" }

        require(state.combat == null) { "Savaş sürerken hikaye seçimi yapılamaz" }

        if (choice.check?.oneShot == true && state.hasFlag(oneShotFlag(scene.id, choice.id))) {
            throw IllegalArgumentException("Tek seferlik kontrol zaten başarısız: $choiceId")
        }
        // Zar kontrolü: d20 + stat ≥ zorluk. Başarısızlık başka sahneye ve başka etkilere gider.
        var start = state
        var success = true
        choice.check?.let { c ->
            val rng = com.yukselis.engine.core.Rng(state.rngState)
            success = rng.d20() + state.player.stats[c.stat] >= c.difficulty
            start = state.copy(rngState = rng.state)
        }
        val chosenEffects = if (success) choice.effects else choice.check!!.failEffects
        val fight = chosenEffects.filterIsInstance<Effect.StartCombat>().firstOrNull()
        val applied0 = effects.applyAll(start, chosenEffects - setOfNotNull(fight))
        // Tanıklar: seçimin ahlakını yanındaki NPC'ler kendi değerlerine göre yorumlar (GDD: NPC modeli).
        val moral = chosenEffects.filterIsInstance<Effect.ShiftMorality>().groupBy { it.axis }.mapValues { (_, l) -> l.sumOf { it.delta } }
        val witnessed = npcEngine.interpret(applied0.state, moral, state.dayState.currentEvent)
        val applied = EffectApplier.Result(witnessed.state, applied0.notices + witnessed.notices)
        var next = applied.state.copy(
            history = (applied.state.history + HistoryEntry(scene.id, choice.id, state.focusNpc)).takeLast(WorldState.HISTORY_LIMIT),
        )
        val checkNotices = mutableListOf<Notice>()
        choice.check?.let { c ->
            checkNotices += Notice(
                if (success) NoticeKind.SYSTEM else NoticeKind.WARNING,
                content.texts.format(if (success) "zar_basarili" else "zar_basarisiz", "n" to odds(state, c)),
            )
            if (!success && c.oneShot) next = next.copy(flags = next.flags + oneShotFlag(scene.id, choice.id))
        }
        if (fight != null) {
            // Savaş başlar; sahne, savaş bitince kazan/kaybet/kaç sahnesine geçer.
            val started = combat.start(next, fight.spec)
            val notices = checkNotices + applied.notices + started.notices
            return Outcome(started.state, notices.map { noticeView(it, started.state, lang) })
        }
        var nextId = if (success) choice.next else (choice.check?.failNext ?: choice.next)
        if (nextId == Content.RETURN) {
            nextId = next.returnStack.lastOrNull() ?: content.startScene
            next = next.copy(returnStack = next.returnStack.dropLast(1))
        }
        val extra = mutableListOf<Notice>()
        if (nextId != null) {
            // Ceza kancası: ihmal edilen Günlük Görev, bir sonraki geçişte Ceza Bölgesi'ne götürür.
            val endingTarget = nextId != WorldState.HUB && scene(nextId).isEnding
            if (next.penaltyPending && !endingTarget && Content.PENALTY_SCENE in content.scenes && nextId != Content.PENALTY_SCENE) {
                next = next.copy(returnStack = next.returnStack + nextId, penaltyPending = false)
                nextId = Content.PENALTY_SCENE
            }
            if (nextId == WorldState.HUB) {
                // Olay bitti: gün akışına dön (zorunlu olay varsa hemen başlar).
                val hub = world.enterHub(next)
                next = hub.state
                extra += hub.notices
            } else {
                next = next.copy(sceneId = nextId, ended = scene(nextId).isEnding)
            }
        }
        return Outcome(next, (checkNotices + applied.notices + extra).map { noticeView(it, next, lang) })
    }

    // ---- Ekran görünümleri ----

    fun status(state: WorldState, lang: Lang): StatusView {
        val p = state.player
        return StatusView(
            playerName = state.playerName,
            level = p.level,
            grade = Grade.forLevel(p.level),
            xp = p.xp,
            xpToNext = Progression.xpToNext(p.level),
            hp = p.hp,
            maxHp = p.maxHp,
            mp = p.mp,
            maxMp = p.maxMp,
            stamina = Progression.maxStamina(p.stats),
            freePoints = p.freePoints,
            stats = Stat.entries.map { StatLine(it, it.abbr[lang], it.label[lang], p.stats[it]) },
            titles = p.titles.mapNotNull { id ->
                content.titles[id]?.let { TitleLine(it.name[lang], text(it.description[lang], state, lang), it.rarity) }
            },
            className = p.classId?.let { content.classes[it]?.name?.get(lang) },
            evolutionPending = state.prompts.any { it.kind == PromptKind.EVOLUTION },
            essences = p.essences.mapNotNull { content.essences[it]?.name?.get(lang) },
            essenceSlots = Progression.essenceSlots(p.grade),
            humanityTopPercent = Progression.humanityTopPercent(p.level, state.day),
            tabs = state.unlockedTabs,
            attention = state.attention,
            corruption = state.corruption,
            publicLevel = state.publicLevel,
            mercy = state.morality.mercy,
            order = state.morality.order,
            humanity = state.morality.humanity,
            day = state.day,
            act = state.act,
            mode = state.mode,
            traits = p.traits.mapNotNull { id -> content.traits[id]?.let { TitleLine(it.name[lang], text(it.description[lang], state, lang), Rarity.EPIC) } },
            armyPower = npcEngine.armyPower(state),
            publicRank = state.variable(com.yukselis.engine.npc.NpcEngine.VAR_RANK),
        )
    }

    fun skills(state: WorldState, lang: Lang): SkillsView {
        val cls = state.player.classId?.let { content.classes[it] }
        return SkillsView(
            className = cls?.name?.get(lang),
            classDescription = cls?.description?.let { text(it[lang], state, lang) },
            skills = state.player.skills.mapNotNull { s ->
                val d = content.skills[s.id] ?: return@mapNotNull null
                SkillView(
                    id = d.id,
                    name = d.name[lang],
                    description = text(d.description[lang], state, lang),
                    type = d.type,
                    rarity = d.rarity,
                    rarityName = d.rarity.label[lang],
                    level = s.level,
                    xp = s.xp,
                    xpToNext = if (s.level >= Progression.SKILL_MAX_LEVEL) 0 else Progression.skillXpToNext(s.level),
                    costMp = d.costMp,
                    costStamina = d.costStamina,
                    cooldown = d.cooldown,
                    evolutions = d.evolutions.mapNotNull { content.skills[it]?.name?.get(lang) },
                    penalty = d.penalty.takeIf { it.isNotEmpty() }?.let { statList(it, lang) },
                )
            },
        )
    }

    fun bag(state: WorldState, lang: Lang): BagView {
        val p = state.player
        val prof = p.profession
        val recipes = if (prof == null) {
            emptyList()
        } else {
            content.recipes.values
                .filter { it.profession == prof.id }
                .sortedWith(compareBy({ it.minLevel }, { it.id }))
                .mapNotNull { r ->
                    val out = content.items[r.output] ?: return@mapNotNull null
                    val ingredients = r.inputs.map { (id, n) -> IngredientView(content.items[id]?.name?.get(lang) ?: id, n, p.itemCount(id)) }
                    RecipeView(
                        id = r.id,
                        name = out.name[lang],
                        description = out.description[lang],
                        count = r.count,
                        minLevel = r.minLevel,
                        ingredients = ingredients,
                        craftable = prof.level >= r.minLevel && ingredients.all { it.have >= it.need },
                    )
                }
        }
        return BagView(
            credits = p.credits,
            crystals = p.essenceCrystals,
            items = p.inventory.entries.sortedBy { it.key }.mapNotNull { (id, n) ->
                val d = content.items[id] ?: return@mapNotNull null
                ItemView(id, d.name[lang], d.description[lang], n, d.rarity, d.type == ItemType.CONSUMABLE && d.use != null && !d.use.combatOnly)
            },
            professionName = prof?.let { content.professions[it.id]?.name?.get(lang) },
            professionLevel = prof?.level ?: 0,
            professionXp = prof?.xp ?: 0,
            professionXpToNext = prof?.let { Progression.professionXpToNext(it.level) } ?: 0,
            recipes = recipes,
        )
    }

    fun daily(state: WorldState, lang: Lang): DailyView? {
        val q = state.daily ?: return null
        val t = content.dailyTemplates[q.templateId] ?: return null
        return DailyView(
            title = t.title[lang],
            description = t.description[lang],
            progress = q.progress,
            steps = q.steps,
            completed = q.completed,
            trainable = t.kind == PlayerActions.TRAINING && !q.completed,
            streak = state.dailyStreak,
        )
    }

    private fun promptView(prompt: SystemPrompt, state: WorldState, lang: Lang): PromptView {
        val t = content.texts
        fun l(x: LText) = text(x[lang], state, lang)
        fun pv(kind: PromptKind, title: String, lines: List<String>, options: List<PromptOptionView>) =
            PromptView(kind, prompt.subject, title, lines, options)
        fun acceptOptions() = listOf(
            PromptOptionView(PromptKind.ACCEPT, t.format("kabul")[lang], ""),
            PromptOptionView(PromptKind.DECLINE, t.format("red")[lang], ""),
        )
        fun skillLine(id: String?): List<String> =
            id?.let { content.skills[it] }?.let { listOf("${it.name[lang]} · ${it.rarity.label[lang]}") } ?: emptyList()
        fun skillName(id: String?) = content.skills[id]?.name ?: LText.EMPTY

        return when (prompt.kind) {
            PromptKind.CLASS_OFFER -> pv(
                prompt.kind, l(t.format("sinif_teklifi")), listOf(l(t.format("sinif_teklifi_satir"))),
                prompt.options.mapNotNull { id ->
                    val c = content.classes[id] ?: return@mapNotNull null
                    val rarity = when (c.kind) {
                        ClassKind.HIDDEN -> Rarity.LEGENDARY
                        ClassKind.RARE -> Rarity.EPIC
                        else -> null
                    }
                    val echo = if (effects.isEchoSuggestion(state, id)) l(t.format("yanki_onerisi")) else null
                    PromptOptionView(id, c.name[lang], l(c.description), listOf(growthLine(c.growth, lang)) + skillLine(c.skill), rarity, echo)
                },
            )
            PromptKind.EVOLUTION -> {
                val g = prompt.subject ?: ""
                pv(
                    prompt.kind, l(t.format("evrim_gorevi", "grade" to g)), listOf(l(t.format("evrim_gorevi_satir", "grade" to g))),
                    prompt.options.map { id ->
                        val c = content.classes[id]
                        if (c == null) {
                            PromptOptionView(
                                id,
                                l(t.format("genel_evrim", "grade" to g)),
                                l(t.format("genel_evrim_aciklama", "n" to Progression.GENERIC_EVOLUTION_BONUS)),
                            )
                        } else {
                            val details = listOfNotNull(
                                c.bonus.takeIf { it.isNotEmpty() }?.let { statList(it, lang) },
                                growthLine(c.growth, lang),
                            ) + skillLine(c.skill)
                            PromptOptionView(id, c.name[lang], l(c.description), details, Rarity.EPIC)
                        }
                    },
                )
            }
            PromptKind.PROFESSION -> pv(
                prompt.kind, l(t.format("meslek_teklifi")), listOf(l(t.format("meslek_teklifi_satir"))),
                prompt.options.mapNotNull { id -> content.professions[id]?.let { PromptOptionView(id, it.name[lang], l(it.description)) } },
            )
            PromptKind.SKILL_EVOLUTION -> {
                val from = content.skills[prompt.subject]
                pv(
                    prompt.kind,
                    l(t.formatL("yetenek_evrim_teklifi", "name" to (from?.name ?: LText.EMPTY))),
                    listOfNotNull(from?.description?.let(::l)),
                    prompt.options.mapNotNull { id ->
                        if (id == PromptKind.DECLINE) {
                            PromptOptionView(id, l(t.format("yetenek_evrimi_ertele")), "")
                        } else {
                            content.skills[id]?.let { PromptOptionView(id, it.name[lang], l(it.description), rarity = it.rarity) }
                        }
                    },
                )
            }
            PromptKind.FUSION -> {
                val r = content.fusions.firstOrNull { it.id == prompt.subject }
                pv(
                    prompt.kind,
                    l(t.formatL("kaynastirma_teklifi", "a" to skillName(r?.inputs?.getOrNull(0)), "b" to skillName(r?.inputs?.getOrNull(1)), "result" to skillName(r?.result))),
                    listOfNotNull(content.skills[r?.result]?.description?.let(::l)),
                    acceptOptions(),
                )
            }
            PromptKind.ESSENCE_BIND -> {
                val e = content.essences[prompt.subject]
                pv(
                    prompt.kind,
                    l(t.formatL("oz_baglama_teklifi", "name" to (e?.name ?: LText.EMPTY))),
                    listOfNotNull(e?.description?.let(::l)),
                    acceptOptions(),
                )
            }
        }
    }

    private fun growthLine(growth: Map<Stat, Int>, lang: Lang): String {
        return content.texts.format("seviye_basi", "stats" to statList(growth, lang))[lang]
    }

    private fun statList(stats: Map<Stat, Int>, lang: Lang): String =
        stats.entries.joinToString(", ") { (s, v) -> "${s.abbr[lang]} ${if (v >= 0) "+$v" else "−${-v}"}" }

    private fun scene(id: String): Scene =
        content.scenes[id] ?: throw IllegalStateException("Tanımsız sahne: $id")

    private fun passage(scene: Scene, state: WorldState, lang: Lang) = PassageView(
        sceneId = scene.id,
        placeName = placeName(scene.place, lang),
        paragraphs = scene.paragraphs
            .filter { it.condition?.test(state) ?: true }
            .map { text(it.text[lang], state, lang) },
        notices = scene.notices
            .filter { it.condition?.test(state) ?: true }
            .map { noticeView(it.notice, state, lang) },
        insights = scene.insights
            .filter { it.condition.test(state) }
            .map { i ->
                val prefix = insightPrefix(i.condition, lang)
                InsightView(listOfNotNull(prefix, text(i.text[lang], state, lang)).joinToString(" "), echo = prefix == null)
            },
    )

    /** "[ALG]" gibi önek: sezginin stat koşulundan. */
    private fun insightPrefix(c: Condition, lang: Lang): String? = when (c) {
        is Condition.StatAtLeast -> "[${c.stat.abbr[lang]}]"
        is Condition.All -> c.conditions.firstNotNullOfOrNull { insightPrefix(it, lang) }
        else -> null
    }

    private fun noticeView(notice: Notice, state: WorldState, lang: Lang) = NoticeView(
        kind = notice.kind,
        title = text(notice.title[lang], state, lang),
        lines = notice.lines.map { text(it[lang], state, lang) },
        rarity = notice.rarity,
    )

    private fun placeName(placeId: String?, lang: Lang): String? =
        placeId?.let { content.names.find(NameEntry.Kind.PLACE, it)?.full?.get(lang) }

    private fun text(raw: String, state: WorldState, lang: Lang): String {
        var t = raw
        if ("@" in t) {
            val focus = state.focusNpc
            if (focus != null) {
                t = t.replace("{npc:@", "{npc:$focus").replace("{zamir:@", "{zamir:$focus")
                if ("{npc_hedef:@}" in t) t = t.replace("{npc_hedef:@}", npcEngine.goalName(state, focus)?.get(lang) ?: "")
            }
        }
        return resolver.resolve(t, lang, state.playerName)
    }

    private fun requirementTag(condition: Condition?, lang: Lang): String? = when (condition) {
        null -> null
        is Condition.StatAtLeast -> "[${condition.stat.abbr[lang]} ${condition.value}]"
        is Condition.LevelAtLeast -> content.texts.format("seviye_kosulu", "n" to condition.value)[lang]
        is Condition.HasSkill -> content.skills[condition.skillId]?.let { "[${it.name[lang]}]" }
        is Condition.HasTitle -> content.titles[condition.titleId]?.let { "[${it.name[lang]}]" }
        is Condition.HasItem -> content.items[condition.itemId]?.let {
            "[${it.name[lang]}${if (condition.count > 1) " ×${condition.count}" else ""}]"
        }
        is Condition.All -> condition.conditions.mapNotNull { requirementTag(it, lang) }
            .joinToString(" ").ifEmpty { null }
        else -> null
    }
}
