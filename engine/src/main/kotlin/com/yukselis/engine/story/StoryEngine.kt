package com.yukselis.engine.story

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
    private val resolver = TokenResolver(content.names)
    private val effects = EffectApplier(content)
    private val actions = PlayerActions(content, effects)

    /** Yeni oyun: Gün 0'ın Günlük Görevi de üretilir. */
    fun newGame(playerName: String, mode: GameMode, seed: Long): WorldState =
        effects.newDaily(WorldState.newGame(seed, playerName, mode, content.startScene)).state

    // ---- Hikaye dışı eylemler (geçersizse ActionRejected) ----

    fun resolvePrompt(state: WorldState, optionId: String, lang: Lang): Outcome = outcome(actions.resolvePrompt(state, optionId), lang)

    fun allocate(state: WorldState, points: Map<Stat, Int>, lang: Lang): Outcome = outcome(actions.allocate(state, points), lang)

    fun craft(state: WorldState, recipeId: String, lang: Lang): Outcome = outcome(actions.craft(state, recipeId), lang)

    fun useItem(state: WorldState, itemId: String, lang: Lang): Outcome = outcome(actions.useItem(state, itemId), lang)

    fun train(state: WorldState, lang: Lang): Outcome = outcome(actions.train(state), lang)

    /** Etkileri doğrudan uygular (testler ve ileride olay motoru için). */
    fun applyEffects(state: WorldState, list: List<Effect>, lang: Lang): Outcome = outcome(effects.applyAll(state, list), lang)

    private fun outcome(r: EffectApplier.Result, lang: Lang) = Outcome(r.state, r.notices.map { noticeView(it, r.state, lang) })

    // ---- Hikaye ----

    fun view(state: WorldState, lang: Lang): SceneView {
        val scene = scene(state.sceneId)
        val history = state.history.mapNotNull { entry ->
            val past = content.scenes[entry.sceneId] ?: return@mapNotNull null
            val chosen = past.choices.firstOrNull { it.id == entry.choiceId }
            passage(past, state, lang).copy(chosenLabel = chosen?.let { text(it.label[lang], state, lang) })
        }
        val choices = scene.choices
            .filter { it.showWhen?.test(state) ?: true }
            .map { choice ->
                ChoiceView(
                    id = choice.id,
                    label = text(choice.label[lang], state, lang),
                    requirementTag = requirementTag(choice.requires, lang),
                    enabled = choice.requires?.test(state) ?: true,
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
        )
    }

    fun choose(state: WorldState, choiceId: String, lang: Lang): Outcome {
        val scene = scene(state.sceneId)
        val choice = scene.choices.firstOrNull { it.id == choiceId }
            ?: throw IllegalArgumentException("'${scene.id}' sahnesinde '$choiceId' seçeneği yok")
        require(choice.showWhen?.test(state) ?: true) { "Gizli seçenek seçilemez: $choiceId" }
        require(choice.requires?.test(state) ?: true) { "Koşulu karşılanmayan seçenek: $choiceId" }
        require(state.prompts.isEmpty()) { "Önce bekleyen Sistem istemi çözülmeli" }

        val applied = effects.applyAll(state, choice.effects)
        var next = applied.state.copy(
            history = (applied.state.history + HistoryEntry(scene.id, choice.id)).takeLast(WorldState.HISTORY_LIMIT),
        )
        var nextId = choice.next
        if (nextId == Content.RETURN) {
            nextId = next.returnStack.lastOrNull() ?: content.startScene
            next = next.copy(returnStack = next.returnStack.dropLast(1))
        }
        if (nextId != null) {
            // Ceza kancası: ihmal edilen Günlük Görev, bir sonraki geçişte Ceza Bölgesi'ne götürür.
            val target = scene(nextId)
            if (next.penaltyPending && !target.isEnding && Content.PENALTY_SCENE in content.scenes && nextId != Content.PENALTY_SCENE) {
                next = next.copy(returnStack = next.returnStack + nextId, penaltyPending = false)
                nextId = Content.PENALTY_SCENE
            }
            next = next.copy(sceneId = nextId, ended = scene(nextId).isEnding)
        }
        return Outcome(next, applied.notices.map { noticeView(it, next, lang) })
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
            mercy = state.morality.mercy,
            order = state.morality.order,
            humanity = state.morality.humanity,
            day = state.day,
            act = state.act,
            mode = state.mode,
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
        fun acceptOptions() = listOf(
            PromptOptionView(PromptKind.ACCEPT, t.format("kabul")[lang], ""),
            PromptOptionView(PromptKind.DECLINE, t.format("red")[lang], ""),
        )
        fun skillLine(id: String?): List<String> =
            id?.let { content.skills[it] }?.let { listOf("${it.name[lang]} · ${it.rarity.label[lang]}") } ?: emptyList()
        fun skillName(id: String?) = content.skills[id]?.name ?: LText.EMPTY

        return when (prompt.kind) {
            PromptKind.CLASS_OFFER -> PromptView(
                prompt.kind, l(t.format("sinif_teklifi")), listOf(l(t.format("sinif_teklifi_satir"))),
                prompt.options.mapNotNull { id ->
                    val c = content.classes[id] ?: return@mapNotNull null
                    val rarity = when (c.kind) {
                        ClassKind.HIDDEN -> Rarity.LEGENDARY
                        ClassKind.RARE -> Rarity.EPIC
                        else -> null
                    }
                    val echo = if (effects.isEchoSuggestion(state, id)) listOf(l(t.format("yanki_onerisi"))) else emptyList()
                    PromptOptionView(id, c.name[lang], l(c.description), echo + listOf(growthLine(c.growth, lang)) + skillLine(c.skill), rarity)
                },
            )
            PromptKind.EVOLUTION -> {
                val g = prompt.subject ?: ""
                PromptView(
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
            PromptKind.PROFESSION -> PromptView(
                prompt.kind, l(t.format("meslek_teklifi")), listOf(l(t.format("meslek_teklifi_satir"))),
                prompt.options.mapNotNull { id -> content.professions[id]?.let { PromptOptionView(id, it.name[lang], l(it.description)) } },
            )
            PromptKind.SKILL_EVOLUTION -> {
                val from = content.skills[prompt.subject]
                PromptView(
                    prompt.kind,
                    l(t.formatL("yetenek_evrim_teklifi", "name" to (from?.name ?: LText.EMPTY))),
                    listOfNotNull(from?.description?.let(::l)),
                    prompt.options.mapNotNull { id ->
                        content.skills[id]?.let { PromptOptionView(id, it.name[lang], l(it.description), rarity = it.rarity) }
                    },
                )
            }
            PromptKind.FUSION -> {
                val r = content.fusions.firstOrNull { it.id == prompt.subject }
                PromptView(
                    prompt.kind,
                    l(t.formatL("kaynastirma_teklifi", "a" to skillName(r?.inputs?.getOrNull(0)), "b" to skillName(r?.inputs?.getOrNull(1)), "result" to skillName(r?.result))),
                    listOfNotNull(content.skills[r?.result]?.description?.let(::l)),
                    acceptOptions(),
                )
            }
            PromptKind.ESSENCE_BIND -> {
                val e = content.essences[prompt.subject]
                PromptView(
                    prompt.kind,
                    l(t.formatL("oz_baglama_teklifi", "name" to (e?.name ?: LText.EMPTY))),
                    listOfNotNull(e?.description?.let(::l)),
                    acceptOptions(),
                )
            }
        }
    }

    private fun growthLine(growth: Map<Stat, Int>, lang: Lang): String {
        val perLevel = if (lang == Lang.TR) "/ seviye" else "/ level"
        return "${statList(growth, lang)} $perLevel"
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
            .map { i -> listOfNotNull(insightPrefix(i.condition, lang), text(i.text[lang], state, lang)).joinToString(" ") },
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

    private fun text(raw: String, state: WorldState, lang: Lang): String =
        resolver.resolve(raw, lang, state.playerName)

    private fun requirementTag(condition: Condition?, lang: Lang): String? = when (condition) {
        null -> null
        is Condition.StatAtLeast -> "[${condition.stat.abbr[lang]} ${condition.value}]"
        is Condition.LevelAtLeast -> if (lang == Lang.TR) "[Sv. ${condition.value}]" else "[Lv. ${condition.value}]"
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
