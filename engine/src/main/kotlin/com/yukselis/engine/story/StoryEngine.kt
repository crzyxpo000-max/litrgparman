package com.yukselis.engine.story

import com.yukselis.engine.core.ChronicleEntry

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
        /** İlişki bildiriminde NPC adı ile değişim listesi arasındaki ayraç (NpcEngine.relationNotice ile aynı). */
        const val RELATION_SEP = ": "
        /** Gizli ölçüler Durum'da bu Dikkat düzeyinden itibaren görünür (ilk eşik). */
        const val HIDDEN_ATTENTION = 25
        /** Dönüş özetinde gösterilen son kritik seçim sayısı. */
        const val RECAP_CHOICES = 3
        const val MEMORY_SHOWN = 5
        private const val DENIZ = "deniz_arman"
        const val RANK_SHOWN = 10
        const val CHRONICLE_SHOWN = 5
        const val CHRONICLE_KEPT = 40
        const val PREVIOUS_GAMES_VAR = "onceki_oyun"
        const val NG_PLUS_FLAG = "yeni_dongu"
        const val NG_PLUS_STATS = 3
        const val LOOP_ENDING = "dongu"
    }

    private val resolver = TokenResolver(content.names)
    private val effects = EffectApplier(content)
    private val actions = PlayerActions(content, effects)
    private val combat = CombatEngine(content, effects)
    val world = com.yukselis.engine.world.WorldEngine(content, effects, combat)

    /** Savaşların `kaybet` hedefi olan ama `kazan` hedefi olmayan sahneler (UI_REVIEW E5). */
    private val defeatScenes: Set<String> by lazy {
        val starts = content.scenes.values.flatMap { it.choices }.flatMap { it.effects }.filterIsInstance<Effect.StartCombat>()
        val wins = starts.map { it.spec.win }.toSet()
        starts.mapNotNull { it.spec.lose }.filter { it != WorldState.HUB && it !in wins }.toSet()
    }

    // ---- Gün akışı ----

    fun hubAction(state: WorldState, action: com.yukselis.engine.world.HubAction, lang: Lang): Outcome {
        val r = world.act(state, action)
        return Outcome(r.state, views(r.notices, r.state, lang))
    }

    fun map(state: WorldState, lang: Lang): List<RegionView> {
        val here = state.region?.let { content.regions[it] }
        return content.regions.values
            .sortedWith(compareByDescending<com.yukselis.engine.world.RegionDef> { it.id == state.region }.thenBy { it.act * 1000 + it.baseDanger })
            .map { d ->
                val r = state.regions[d.id]
                RegionView(
                    id = d.id,
                    name = world.regionName(d.id)[lang],
                    danger = r?.danger ?: d.baseDanger,
                    control = controlName(r?.control ?: d.control, lang),
                    gates = r?.gates?.map { gateView(it, state, lang) } ?: emptyList(),
                    current = d.id == state.region,
                    neighbor = here != null && world.canTravel(state, d.id),
                    act = d.act,
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
        chapter = content.chapters.get(state.chapter)?.let { ch ->
            ChapterGoalView(
                no = ch.no, title = text(ch.title[lang], state, lang), goal = text(ch.goal[lang], state, lang),
                threat = text(ch.threat[lang], state, lang), daysLeft = chapterDaysLeft(state, ch),
                phase = content.texts.format("bolum_evre_${state.chapterPhase.key}")[lang],
            )
        },
    )

    // ---- Bölüm yapısı (DECISIONS #130) ----

    /** Kalan gün: operasyon beklerken son tarihe, sonra bölüm sonuna. */
    private fun chapterDaysLeft(state: WorldState, ch: com.yukselis.engine.world.ChapterDef): Int {
        val waiting = state.chapterPhase == com.yukselis.engine.world.ChapterPhase.BRIEF ||
            state.chapterPhase == com.yukselis.engine.world.ChapterPhase.PREP
        return ((if (waiting) ch.deadline else ch.end) - state.day).coerceAtLeast(0)
    }

    /** Her zaman görünen "Bölüm N · Hedef · Kalan gün" satırı (gün akışından önce null). */
    fun chapterLine(state: WorldState, lang: Lang): com.yukselis.engine.world.ChapterLine? {
        val ch = content.chapters.get(state.chapter) ?: return null
        val m = state.mission
        val label = m?.let {
            content.texts.formatL("gorev_etiket_${it.kind.key}", "region" to world.regionName(it.region))[lang]
        }
        val highlights = state.chronicle
            .filter { it.day in ch.open..ch.end }
            .sortedWith(compareByDescending<ChronicleEntry> { it.weight }.thenBy { it.day })
            .mapNotNull { c ->
                val choice = content.scenes[c.scene]?.choices?.firstOrNull { it.id == c.choice }
                val raw = choice?.chronicle ?: content.events[c.event]?.card
                raw?.let { text(it[lang], state, lang) }
            }
            .distinct()
            .take(3)
        val next = content.chapters.next(ch.no)
        return com.yukselis.engine.world.ChapterLine(
            no = ch.no,
            title = text(ch.title[lang], state, lang),
            goal = text(ch.goal[lang], state, lang),
            daysLeft = chapterDaysLeft(state, ch),
            phase = state.chapterPhase,
            onMission = m != null,
            missionLabel = label,
            isFinal = next == null,
            countdown = ch.countdown?.takeIf {
                state.chapterPhase == com.yukselis.engine.world.ChapterPhase.BRIEF || state.chapterPhase == com.yukselis.engine.world.ChapterPhase.PREP
            }?.let { cd ->
                val n = chapterDaysLeft(state, ch)
                text(LText(cd.tr.replace("{n}", "$n"), cd.en.replace("{n}", "$n"))[lang], state, lang)
            },
            highlights = highlights,
            nextTitle = next?.let { text(it.title[lang], state, lang) },
        )
    }

    /**
     * Dönüş özeti (uzun aradan sonra): bölüm ve hedef, kalan gün, yanındakiler, öne çıkan fraksiyon, Dikkat ve son
     * kritik seçimler (kronik). Metin şablonları `system.yaml` `ozet_*`.
     */
    fun recap(state: WorldState, lang: Lang): com.yukselis.engine.story.RecapView? {
        val ch = content.chapters.get(state.chapter) ?: return null
        val t = content.texts
        fun f(key: String, vararg args: Pair<String, LText>) = text(t.formatL(key, *args)[lang], state, lang)
        fun n(v: Int) = LText("$v", "$v")
        val lines = mutableListOf<String>()
        lines += f("ozet_bolum", "n" to n(ch.no), "title" to ch.title, "goal" to ch.goal)
        val phase = state.chapterPhase
        lines += when (phase) {
            com.yukselis.engine.world.ChapterPhase.BRIEF, com.yukselis.engine.world.ChapterPhase.PREP ->
                f("ozet_son_tarih", "n" to n(chapterDaysLeft(state, ch)))
            com.yukselis.engine.world.ChapterPhase.OPERATION -> f("ozet_operasyonda")
            else -> f("ozet_sonrasi", "n" to n(chapterDaysLeft(state, ch)))
        }
        state.mission?.takeIf { it.kind != com.yukselis.engine.world.MissionKind.OPERATION }?.let {
            lines += f("ozet_gorevde", "region" to world.regionName(it.region))
        }
        val party = state.npcs.values.filter { it.companion && it.alive }.map { npcEngine.name(it.id) }
        lines += if (party.isEmpty()) f("ozet_yalniz") else f("ozet_yaninda", "names" to LText(party.joinToString(", ") { it.tr }, party.joinToString(", ") { it.en }))
        val leader = listOf("lonca", "sessiz_ag", "siginak", "yukselenler").maxByOrNull { state.reputation[it] ?: 0 }
        if (leader != null && (state.reputation[leader] ?: 0) > 0) {
            lines += f("ozet_fraksiyon", "faction" to (content.names.find(com.yukselis.engine.i18n.NameEntry.Kind.FACTION, leader)?.full ?: LText(leader, leader)))
        }
        if (state.attention >= 50) lines += f("ozet_dikkat", "n" to n(state.attention))
        val recent = state.chronicle.sortedByDescending { it.day }.take(RECAP_CHOICES)
        recent.mapNotNull { c -> content.scenes[c.scene]?.choices?.firstOrNull { it.id == c.choice }?.chronicle?.let { text(it[lang], state, lang) } }
            .forEach { lines += "• $it" }
        return com.yukselis.engine.story.RecapView(f("ozet_baslik"), lines)
    }

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
        enemies = g.enemies.mapNotNull { content.enemy(it)?.name?.get(lang) }.groupingBy { it }.eachCount()
            .entries.joinToString(", ") { (n, k) -> if (k > 1) "$n ×$k" else n },
        daysLeft = g.deadline - state.day,
    )

    private fun hubView(state: WorldState, lang: Lang): HubView {
        val d = state.dayState
        val region = state.region
        return HubView(
            day = state.day,
            slotsLeft = d.slotsLeft,
            slotsTotal = d.slotsTotal,
            regionName = region?.let { world.regionName(it)[lang] } ?: "",
            danger = region?.let { state.regions[it]?.danger } ?: 0,
            cards = d.hand.mapNotNull { id ->
                content.events[id]?.let { e ->
                    val away = !content.chapters.isEmpty && state.mission == null && !world.director.fitsLocation(state, e)
                    val where = if (away) e.where.firstOrNull { it in content.regions && world.canTravel(state, it) && it != world.base(state) } else null
                    CardView(id, text(e.card[lang], state, lang), e.tension, where?.let { world.regionName(it)[lang] })
                }
            },
            gates = region?.let { state.regions[it] }?.gates?.map { gateView(it, state, lang) } ?: emptyList(),
            travel = world.destinations(state).map { RegionLinkView(it, world.regionName(it)[lang]) },
            mustCamp = d.slotsLeft <= 0,
            tower = content.tower?.takeIf { world.towerOpen(state) }?.let { t ->
                val f = world.nextFloor(state)
                TowerView(f, t.levelOf(f), f in t.storyFloors, world.fastClimbRange(state)?.last)
            },
            atBase = state.mission == null,
            mission = state.mission?.let { content.texts.formatL("gorev_etiket_${it.kind.key}", "region" to world.regionName(it.region))[lang] },
            operations = world.operationChoices(state).mapNotNull { (i, region) ->
                val ch = content.chapters.get(state.chapter) ?: return@mapNotNull null
                val op = ch.operations[i]
                val info = world.operationInfo(state, i)
                OperationChoiceView(
                    index = i,
                    title = text((if (op.title == LText.EMPTY) ch.title else op.title)[lang], state, lang),
                    region = world.regionName(region)[lang],
                    daysLeft = (ch.deadline - state.day).coerceAtLeast(0),
                    threatLevel = info?.threatLevel ?: 0,
                    playerLevel = state.player.level,
                    preparations = info?.preparations?.map { (n, done) -> PreparationStatusView(text(n[lang], state, lang), done) } ?: emptyList(),
                    opinions = info?.opinions?.map { text(it[lang], state, lang) } ?: emptyList(),
                    warning = info?.warning?.let { text(it[lang], state, lang) },
                    emphasized = info?.emphasized ?: true,
                )
            },
            canReturn = state.mission != null && state.mission.kind != com.yukselis.engine.world.MissionKind.OPERATION,
            canExplore = content.chapters.isEmpty || state.mission?.kind == com.yukselis.engine.world.MissionKind.EXPLORE,
            preparations = world.preparationChoices(state).map { pr ->
                PreparationView(pr.id, text(pr.name[lang], state, lang), text(pr.description[lang], state, lang), pr.cost, pr.cost <= d.slotsLeft)
            },
            deadlineWarning = if (world.deadlineRisk(state)) content.chapters.get(state.chapter)?.let { ch ->
                text(content.texts.formatL("son_tarih_uyari", "title" to ch.title, "n" to LText("${(ch.deadline - state.day).coerceAtLeast(0)}", "${(ch.deadline - state.day).coerceAtLeast(0)}"))[lang], state, lang)
            } else null,
            skip = world.skipLimit(state).takeIf { it > 0 && d.slotsLeft > 0 }?.let { max ->
                SkipView(max, world.skipFoci(state).map { f -> SkipFocusView(f, f.key, skipFocusName(f, state, lang)) })
            },
        )
    }

    private fun skipFocusName(f: com.yukselis.engine.world.SkipFocus, state: WorldState, lang: Lang): String = when (f) {
        is com.yukselis.engine.world.SkipFocus.Train -> f.stat.label[lang]
        com.yukselis.engine.world.SkipFocus.Gates -> content.texts.format("atlama_odak_kapi")[lang]
        com.yukselis.engine.world.SkipFocus.Craft -> state.player.profession?.let { content.professions[it.id]?.name?.get(lang) } ?: ""
        is com.yukselis.engine.world.SkipFocus.Companion -> text("{npc:${f.npc}}", state, lang)
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
        } else if (state.combat != null && s.combat == null && !s.ended && s.variable(com.yukselis.engine.combat.CombatEngine.CTX_WON) == 1) {
            // Hikaye savaşı kazanıldı: Günlük Görev "savaş" adımı (QA P1-21).
            val d = effects.applyAll(s, listOf(Effect.DailyProgress("savas", 1)))
            s = d.state
            notices += d.notices
        }
        return Outcome(s, views(notices, s, lang))
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
        return Outcome(r.state, views(r.notices, r.state, lang))
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
                phaseThresholds = d.phases.map { it.threshold },
                weaknessKey = if (e.analyzed) d.weakness else null,
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
                tags = d.tags,
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
            tacticsUnlocked = state.act >= 2 || state.variable(CombatEngine.VAR_FIGHTS) >= CombatEngine.TACTICS_UNLOCK_FIGHTS,
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

    /**
     * Yeni oyun: Gün 0'ın Günlük Görevi de üretilir. `previousGames`: bu cihazda bitirilmiş oyun sayısı (Döngü sonunun
     * koşulu, değişken `onceki_oyun`). `newGamePlus`: Döngü sonundan sonra yeni bir Entegrasyon (bayrak `yeni_dongu`).
     */
    fun newGame(playerName: String, mode: GameMode, seed: Long, previousGames: Int = 0, newGamePlus: Boolean = false): WorldState {
        var s = WorldState.newGame(seed, playerName, mode, content.startScene)
        if (previousGames > 0) s = s.copy(vars = s.vars + (PREVIOUS_GAMES_VAR to previousGames))
        if (newGamePlus) {
            // Döngüyü hatırlayan bir anomali: her stat +NG_PLUS_STATS.
            s = s.copy(flags = s.flags + NG_PLUS_FLAG)
            s = s.copy(player = s.player.copy(stats = Stat.entries.fold(s.player.stats) { st, k -> st.plus(k, NG_PLUS_STATS) }))
        }
        return npcEngine.ensure(effects.newDaily(s).state)
    }

    // ---- Bildirimler (Aşama 9) ----

    val notifier by lazy { Notifier(content, npcEngine) }

    /** Kayıtlı oyun için oturumlar arası bildirim önerisi (dile çözülmüş başlık ve metin). */
    fun pendingNotification(state: WorldState, lang: Lang, rivalWasAhead: Boolean = false): Triple<String, String, String>? =
        notifier.pick(state, rivalWasAhead)?.let { Triple(it.id, it.title[lang], text(it.text[lang], state, lang)) }

    /** Rakip şu an sıralamada önde mi (uygulama geçişleri saptamak için saklar). */
    fun rivalAhead(state: WorldState): Boolean = notifier.rivalAhead(state)

    /** Bildirime dokunuldu: küçük olayı bir sonraki akış girişine tohumlar. */
    fun openNotification(state: WorldState, noteId: String): WorldState = notifier.open(state, noteId)

    // ---- Sonlar (Aşama 7) ----

    /** Ulaşılan son (son sahnesinden türetilir); son değilse null. */
    fun endingOf(state: WorldState): EndingDef? =
        if (!state.ended) null else content.endings.forScene(state.sceneId)

    /** Son ekranı: epilog kartları (her ana NPC'nin akıbeti, fraksiyonlar, en kritik 5 seçim). */
    fun endingView(state: WorldState, lang: Lang): EndingView? {
        val def = endingOf(state) ?: return null
        val s = state.copy(ending = def.id)
        fun first(lines: List<EpilogueLine>?) = lines?.firstOrNull { it.condition?.test(s) ?: true }?.text
        val archetype = content.endings.archetypes.firstOrNull { it.condition?.test(s) ?: true }
        val npcs = content.endings.npcFates.mapNotNull { (id, lines) ->
            val n = s.npcs[id] ?: return@mapNotNull null
            if (!n.known) return@mapNotNull null
            first(lines)?.let { EpilogueCard(npcEngine.fullName(id)[lang], text(it[lang], s.copy(focusNpc = id), lang)) }
        }
        val factions = content.endings.factionFates.mapNotNull { (id, lines) ->
            first(lines)?.let { EpilogueCard(npcEngine.factionName(id)[lang], text(it[lang], s, lang)) }
        }
        val choices = topChronicle(s).mapNotNull { c ->
            val event = content.events[c.event] ?: return@mapNotNull null
            val choice = content.scenes[c.scene]?.choices?.firstOrNull { it.id == c.choice } ?: return@mapNotNull null
            ChronicleView(c.day, text(event.card[lang], s, lang), text(choice.label[lang], s, lang), choice.chronicle?.let { text(it[lang], s, lang) })
        }
        return EndingView(
            id = def.id,
            name = def.name[lang],
            summary = text(def.summary[lang], s, lang),
            archetype = archetype?.name?.get(lang),
            archetypeText = archetype?.description?.let { text(it[lang], s, lang) },
            npcs = npcs,
            factions = factions,
            choices = choices,
            level = s.player.level,
            grade = s.player.grade.name,
            day = s.day,
            unlocksNewGamePlus = def.id == LOOP_ENDING,
            archetypeId = archetype?.id,
        )
    }

    // ---- Paket 7: kalıcı Arşiv / Kodeks ----

    /** Oyuncunun şu anki gizli sınıf kimliği (varsa); evrilmişse temel gizli sınıfı da içerir. */
    fun unlockedHiddenClasses(state: WorldState): Set<String> {
        val cid = state.player.classId ?: return emptySet()
        val cls = content.classes[cid] ?: return emptySet()
        return buildSet {
            if (cls.kind == ClassKind.HIDDEN) add(cls.id)
            cls.evolvesFrom?.let { parent -> if (content.classes[parent]?.kind == ClassKind.HIDDEN) add(parent) }
        }
    }

    /** Oyuncunun sahip olduğu Benzersiz yetenek kimlikleri. */
    fun unlockedUniqueSkills(state: WorldState): Set<String> =
        state.player.skills.mapNotNull { s -> content.skills[s.id]?.takeIf { it.rarity == Rarity.UNIQUE }?.id }.toSet()

    /** Başlık ekranı Arşiv görünümü: kilitli girdiler "???" olarak saklanır (spoiler yok). */
    fun archive(
        unlockedEndings: Set<String>,
        unlockedArchetypes: Set<String>,
        unlockedClasses: Set<String>,
        unlockedSkills: Set<String>,
        completedGames: Int,
        lang: Lang,
        playerName: String,
    ): ArchiveView {
        fun plain(raw: String) = resolver.resolve(raw, lang, playerName)
        fun <T> section(items: List<T>, idOf: (T) -> String, nameOf: (T) -> String, descOf: (T) -> String, unlocked: Set<String>, rarityOf: (T) -> Rarity? = { null }): ArchiveSectionView {
            val entries = items.map { item ->
                val id = idOf(item)
                val open = id in unlocked
                ArchiveEntryView(
                    id = id,
                    name = if (open) nameOf(item) else "???",
                    description = if (open) descOf(item) else "",
                    unlocked = open,
                    rarity = if (open) rarityOf(item) else null,
                )
            }
            return ArchiveSectionView(entries.count { it.unlocked }, entries.size, entries)
        }
        val endings = section(
            content.endings.endings.values.sortedBy { it.order },
            idOf = { it.id },
            nameOf = { it.name[lang] },
            descOf = { plain(it.summary[lang]) },
            unlocked = unlockedEndings,
            rarityOf = { if (it.hidden) Rarity.UNIQUE else Rarity.EPIC },
        )
        val archetypes = section(
            content.endings.archetypes,
            idOf = { it.id },
            nameOf = { it.name[lang] },
            descOf = { plain(it.description[lang]) },
            unlocked = unlockedArchetypes,
            rarityOf = { Rarity.RARE },
        )
        val hiddenClasses = section(
            content.classes.values.filter { it.kind == ClassKind.HIDDEN }.sortedBy { it.id },
            idOf = { it.id },
            nameOf = { it.name[lang] },
            descOf = { plain(it.description[lang]) },
            unlocked = unlockedClasses,
            rarityOf = { Rarity.LEGENDARY },
        )
        val uniqueSkills = section(
            content.skills.values.filter { it.rarity == Rarity.UNIQUE }.sortedBy { it.id },
            idOf = { it.id },
            nameOf = { it.name[lang] },
            descOf = { plain(it.description[lang]) },
            unlocked = unlockedSkills,
            rarityOf = { Rarity.UNIQUE },
        )
        return ArchiveView(completedGames, endings, archetypes, hiddenClasses, uniqueSkills)
    }

    /** En kritik 5 seçim: ağırlığa göre, eşitlikte erken olan; gün sırasıyla gösterilir. */
    fun topChronicle(state: WorldState): List<ChronicleEntry> =
        state.chronicle.sortedWith(compareByDescending<ChronicleEntry> { it.weight }.thenBy { it.day })
            .take(CHRONICLE_SHOWN).sortedBy { it.day }

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
        // İçerikten silinmiş bir olayın ortasında alınmış kayıt (QA P2-9).
        if (s.dayState.currentEvent != null && s.dayState.currentEvent !in content.events) s = s.copy(dayState = s.dayState.copy(currentEvent = null))
        // NPC'ler: tanımsızlar atılır, eksikler (eski kayıt) içerikten kurulur.
        val fresh = content.npcs.keys.filter { s.npcs[it]?.initialized != true }.toSet()
        s = npcEngine.ensure(s.copy(npcs = s.npcs.filterKeys { it in content.npcs }))
        // v2 kaydı: Deniz'le birlikte uyanılmış ya da bulunmuşsa yoldaş olarak kurulur (Aşama 5 içeriğiyle uyumlu).
        if (DENIZ in fresh && (s.hasFlag("deniz_el_tutuldu") || s.hasFlag("deniz_bulundu"))) s = npcEngine.join(s, DENIZ).state
        // Gün akışına girmiş eski kayıtta Küresel Sıralama'nın avcıları yok: kurulur.
        if (s.regions.isNotEmpty() && s.hunters.isEmpty()) s = npcEngine.setupRanking(s)
        s = migrateChapters(s)
        return s
    }

    /**
     * v4 → v5 (bölüm yapısı): gün akışına girmiş ama bölümü olmayan kayıtta bölüm günden kurulur. Evre görülen
     * olaylardan: operasyon görüldüyse darboğaza göre sonrası/bitti, değilse hazırlık (brifing görülmüş sayılır).
     * Yarım görev olmaz: oyuncu üste açılır (olayın ortasındaysa olay sürer, bitince üsse dönülür).
     */
    fun migrateChapters(state: WorldState): WorldState {
        val table = content.chapters
        if (table.isEmpty || state.regions.isEmpty() || state.chapter != 0) return state
        val ch = table.forDay(state.day) ?: return state
        val seen = state.dayState.seen
        val opSeen = ch.operationEvents.any { it in seen }
        val neckSeen = ch.bottleneck.isEmpty() || ch.bottleneck.any { it in seen } || ch.operationEvents.any { it in ch.bottleneck && it in seen }
        val phase = when {
            opSeen && neckSeen -> com.yukselis.engine.world.ChapterPhase.DONE
            opSeen -> com.yukselis.engine.world.ChapterPhase.AFTER
            else -> com.yukselis.engine.world.ChapterPhase.PREP
        }
        val base = world.base(state) ?: state.region
        val inEvent = state.sceneId != WorldState.HUB
        return state.copy(
            chapter = ch.no,
            chapterPhase = phase,
            mission = null,
            // Olayın ortasında alınmış kayıtta bölge olay bitince üs olur; akıştaysa hemen.
            region = if (inEvent) state.region else base,
        )
    }

    fun allocate(state: WorldState, points: Map<Stat, Int>, lang: Lang): Outcome = outcome(actions.allocate(state, points), lang)

    fun craft(state: WorldState, recipeId: String, lang: Lang): Outcome = outcome(actions.craft(state, recipeId), lang)

    fun useItem(state: WorldState, itemId: String, lang: Lang): Outcome = outcome(actions.useItem(state, itemId), lang)

    fun train(state: WorldState, lang: Lang): Outcome = outcome(actions.train(state), lang)

    /** Etkileri doğrudan uygular (testler ve ileride olay motoru için). */
    fun applyEffects(state: WorldState, list: List<Effect>, lang: Lang): Outcome = outcome(effects.applyAll(state, list), lang)

    /**
     * Bildirimler ekrana: aynı `group` değerini taşıyanlar (bir NPC'nin aynı sahnedeki ilişki değişimleri) tek karta iner;
     * değişim listeleri virgülle birleştirilir, tekrarlar atılır (Paket 2: "[BAĞ] Deniz · Güven +15" kartları yerine tek satır).
     */
    private fun views(notices: List<Notice>, state: WorldState, lang: Lang): List<NoticeView> = collapse(notices).map { noticeView(it, state, lang) }

    internal fun collapse(notices: List<Notice>): List<Notice> {
        if (notices.none { it.group != null }) return notices
        val out = mutableListOf<Notice>()
        val index = mutableMapOf<String, Int>()
        fun merge(a: String, b: String): String {
            val headA = a.substringBefore(RELATION_SEP)
            val restA = a.substringAfter(RELATION_SEP, "")
            val restB = b.substringAfter(RELATION_SEP, "")
            val parts = (restA.split(", ") + restB.split(", ")).filter { it.isNotBlank() }.distinct()
            return headA + RELATION_SEP + parts.joinToString(", ")
        }
        for (n in notices) {
            val g = n.group
            if (g == null) { out += n; continue }
            val at = index[g]
            if (at == null) { index[g] = out.size; out += n; continue }
            val prev = out[at]
            out[at] = prev.copy(title = LText(merge(prev.title.tr, n.title.tr), merge(prev.title.en, n.title.en)), lines = prev.lines + n.lines)
        }
        return out
    }

    private fun outcome(r: EffectApplier.Result, lang: Lang) = Outcome(r.state, views(r.notices, r.state, lang))

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
                chapter = chapterLine(state, lang),
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
            current = passage(scene, state, lang).let { p ->
                val ev = state.dayState.currentEvent?.let { content.events[it] }
                val reminder = state.dayState.reminder
                if (reminder != null && ev?.entry == scene.id) p.copy(paragraphs = listOf(text(reminder[lang], state, lang)) + p.paragraphs) else p
            },
            choices = choices,
            isEnding = scene.isEnding,
            prompt = state.prompts.firstOrNull()?.let { promptView(it, state, lang) },
            opinions = opinions,
            chapter = chapterLine(state, lang),
            isDefeat = scene.id in defeatScenes,
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
            chronicle = chronicleAfter(applied.state, state, scene.id, choice, chosenEffects),
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
            return Outcome(started.state, views(notices, started.state, lang))
        }
        var nextId = if (success) choice.next else (choice.check?.failNext ?: choice.next)
        if (nextId == Content.RETURN) {
            nextId = next.returnStack.lastOrNull() ?: content.startScene
            next = next.copy(returnStack = next.returnStack.dropLast(1))
        }
        val extra = mutableListOf<Notice>()
        if (nextId != null) {
            // Ceza Bölgesi hikaye sahnesini kesmez: ihmal edilen Günlük Görev yalnız üs/kamp anında işlenir (WorldEngine.enterHub).
            if (nextId == WorldState.HUB) {
                // Olay bitti: gün akışına dön (zorunlu olay varsa hemen başlar).
                val hub = world.enterHub(next)
                next = hub.state
                extra += hub.notices
            } else {
                next = next.copy(sceneId = nextId, ended = scene(nextId).isEnding)
            }
        }
        return Outcome(next, views(checkNotices + applied.notices + extra, next, lang))
    }

    /**
     * Kronik: önemli olaylardaki seçimler (ya da `kronik` metni olan seçimler) ağırlıklarıyla saklanır.
     * Ağırlık yazılmadıysa: ahlak kaymaları + itibar/2 + NPC ölümü 25, fraksiyon değişimi/ayrılık 12, tohum 6;
     * `kronik` metni +10.
     */
    private fun chronicleAfter(after: WorldState, before: WorldState, sceneId: String, choice: ChoiceDef, chosen: List<Effect>): List<ChronicleEntry> {
        val eventId = before.dayState.currentEvent ?: return after.chronicle
        if (before.returnStack.isNotEmpty() || sceneId == Content.PENALTY_SCENE) return after.chronicle
        val event = content.events[eventId] ?: return after.chronicle
        if (!event.important && choice.chronicle == null) return after.chronicle
        val weight = choice.chronicleWeight ?: (chosen.sumOf { e ->
            when (e) {
                is Effect.ShiftMorality -> kotlin.math.abs(e.delta)
                is Effect.ShiftReputation -> kotlin.math.abs(e.delta) / 2
                is Effect.KillNpc -> 25
                is Effect.SetNpcFaction, is Effect.LeaveParty, is Effect.NpcGone -> 12
                is Effect.PlantSeed -> 6
                else -> 0
            }
        } + if (choice.chronicle != null) 10 else 0)
        val list = after.chronicle + ChronicleEntry(before.day, eventId, sceneId, choice.id, weight)
        // En fazla CHRONICLE_KEPT kayıt: en hafifler düşer.
        return if (list.size <= CHRONICLE_KEPT) list
        else list.sortedWith(compareByDescending<ChronicleEntry> { it.weight }.thenBy { it.day }).take(CHRONICLE_KEPT).sortedBy { it.day }
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
            showHidden = state.act >= 2 || state.attention >= HIDDEN_ATTENTION || state.publicLevel != p.level || state.flags.any { it.startsWith("dikkat_esik_") },
            showRank = state.act >= 2 || state.hasFlag("olay_siralama_gunu"),
            showRelations = state.npcs.values.any { it.known },
            showMap = state.act >= 2,
            showArmy = npcEngine.armyPower(state) > 0 || state.variable(WorldState.UI_ARMY_VAR) > 0,
            showKnowledge = state.knowledge.isNotEmpty() || state.variable(WorldState.UI_KNOWLEDGE_VAR) > 0,
            showMana = state.act >= 2 || p.mp < p.maxMp || p.skills.any { (content.skills[it.id]?.costMp ?: 0) > 0 },
            showGrade = state.act >= 2 || p.grade > Grade.F || state.prompts.any { it.kind == PromptKind.EVOLUTION } || p.level >= Progression.CLASS_OFFER_LEVEL,
            showStamina = state.act >= 2 || state.variable(CombatEngine.VAR_FIGHTS) > 0,
            showDanger = state.act >= 2 || state.regions.values.any { it.gates.isNotEmpty() } ||
                state.variable(com.yukselis.engine.world.WorldEngine.GATES_CLOSED_VAR) > 0 ||
                state.variable(com.yukselis.engine.world.WorldEngine.OVERFLOW_VAR) > 0,
            showCrystals = state.act >= 2 || p.essenceCrystals > 0 || state.variable(WorldState.UI_CRYSTALS_VAR) > 0,
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
        // Paket 4 (DECISIONS #166): meslek kapalı; eski kayıttaki meslek de arayüzde görünmez.
        val prof = if (Progression.PROFESSIONS_ENABLED) p.profession else null
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
                                c.bonus.takeIf { it.isNotEmpty() }?.let { bonusLine(it, lang) },
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

    private fun bonusLine(bonus: Map<Stat, Int>, lang: Lang): String {
        return content.texts.format("evrim_simdi", "stats" to statList(bonus, lang))[lang]
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
        // Anlatı sezgisi (koşullu ama Sistem/Yankı sesi olmayan satır) paragrafların sonuna eklenir.
        paragraphs = scene.paragraphs
            .filter { it.condition?.test(state) ?: true }
            .map { text(it.text[lang], state, lang) } +
            scene.insights.filter { it.narration && it.condition.test(state) }.map { text(it.text[lang], state, lang) },
        notices = scene.notices
            .filter { it.condition?.test(state) ?: true }
            .map { noticeView(it.notice, state, lang) },
        insights = scene.insights
            .filter { !it.narration && it.condition.test(state) }
            .map { i ->
                // Stat koşullu sezgi "[ALG]" öneki almaz (Paket 2): kilit etiketi "[ALG 6]" ile karışıyordu; arayüz
                // turkuaz çizgiyle ayırır. Öneksiz satır Yankı'nın sesidir.
                InsightView(text(i.text[lang], state, lang), echo = !isStatGate(i.condition))
            },
    )

    private fun isStatGate(c: Condition): Boolean = when (c) {
        is Condition.StatAtLeast -> true
        is Condition.All -> c.conditions.any { isStatGate(it) }
        else -> false
    }

    private fun noticeView(notice: Notice, state: WorldState, lang: Lang) = NoticeView(
        kind = notice.kind,
        title = text(notice.title[lang], state, lang),
        lines = notice.lines.map { text(it[lang], state, lang) },
        rarity = notice.rarity,
        moment = notice.moment,
        from = notice.from,
        to = notice.to,
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
