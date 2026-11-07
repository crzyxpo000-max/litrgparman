package com.yukselis.engine.story

import com.yukselis.engine.core.ProfessionState
import com.yukselis.engine.core.PromptKind
import com.yukselis.engine.core.WorldState
import com.yukselis.engine.i18n.LText
import com.yukselis.engine.system.Grade
import com.yukselis.engine.system.ItemType
import com.yukselis.engine.system.Progression
import com.yukselis.engine.system.Stat

class ActionRejected(message: String) : IllegalArgumentException(message)

/**
 * Hikaye seçimi dışındaki oyuncu eylemleri: Sistem istemlerini çözmek, serbest stat dağıtmak,
 * üretim, eşya kullanımı, Günlük Görev antrenmanı. Hepsi saf: durum alır, yeni durum döndürür.
 * Geçersiz eylem [ActionRejected] fırlatır; durum değişmez.
 */
class PlayerActions(private val content: Content, private val effects: EffectApplier) {

    private val texts get() = content.texts

    /**
     * İlk bekleyen istemi çözer. `kind`/`subject` verilirse istemle eşleşmeli: çift dokunuş bir
     * sonraki istemi yanlışlıkla çözmesin.
     */
    fun resolvePrompt(state: WorldState, optionId: String, kind: PromptKind? = null, subject: String? = null): EffectApplier.Result {
        val prompt = state.prompts.firstOrNull() ?: throw ActionRejected("Bekleyen istem yok")
        if (kind != null && (prompt.kind != kind || prompt.subject != subject)) throw ActionRejected("İstem değişti")
        if (optionId !in prompt.options) throw ActionRejected("'$optionId' bu istemin seçeneği değil")
        val popped = state.copy(prompts = state.prompts.drop(1))
        val r = when (prompt.kind) {
            PromptKind.CLASS_OFFER -> chooseClass(popped, optionId)
            PromptKind.EVOLUTION -> evolve(popped, Grade.fromKey(prompt.subject) ?: throw ActionRejected("Kademe yok"), optionId)
            PromptKind.PROFESSION -> chooseProfession(popped, optionId)
            PromptKind.SKILL_EVOLUTION -> if (optionId == PromptKind.DECLINE) {
                // Ertelendi: bir sonraki kademe evrimine kadar yeniden sorulmaz (kaynaştırma için tutulabilir).
                EffectApplier.Result(popped.copy(flags = popped.flags + EffectApplier.declinedFlag(prompt.kind, prompt.subject!!)), emptyList())
            } else {
                effects.evolveSkill(popped, prompt.subject!!, optionId)
            }
            PromptKind.FUSION -> if (optionId == PromptKind.ACCEPT) {
                effects.fuse(popped, prompt.subject!!)
            } else {
                EffectApplier.Result(popped.copy(flags = popped.flags + EffectApplier.declinedFlag(prompt.kind, prompt.subject!!)), emptyList())
            }
            PromptKind.ESSENCE_BIND -> bindEssence(popped, prompt.subject!!, optionId == PromptKind.ACCEPT)
        }
        return withTriggers(r)
    }

    private fun chooseClass(state: WorldState, classId: String): EffectApplier.Result {
        val def = content.classes[classId] ?: throw ActionRejected("Tanımsız sınıf: $classId")
        val withClass = state.withPlayer { it.copy(classId = classId) }
        val notice = Notice(NoticeKind.CLASS, texts.formatL("sinif_kazanildi", "name" to def.name), listOf(def.description))
        val skill = def.skill?.let { effects.grantSkill(withClass, it) } ?: EffectApplier.Result(withClass, emptyList())
        // OP MC: sınıfla birlikte oyun tarzına bağlı Benzersiz yetenek gelir.
        val unique = effects.grantUniqueSkill(skill.state)
        val first = effects.grantTitleIfDefined(unique.state, EffectApplier.TITLE_FIRST_CLASS)
        return EffectApplier.Result(first.state, listOf(notice) + skill.notices + unique.notices + first.notices)
    }

    private fun evolve(state: WorldState, grade: Grade, optionId: String): EffectApplier.Result {
        val notices = mutableListOf<Notice>()
        var s = state.withPlayer { it.copy(unlockedGrade = maxOf(it.unlockedGrade, grade)) }
        if (optionId == EffectApplier.GENERIC_EVOLUTION) {
            s = s.withPlayer { p ->
                var stats = p.stats
                Stat.entries.forEach { stats = stats.plus(it, Progression.GENERIC_EVOLUTION_BONUS) }
                p.copy(stats = stats)
            }
            val className = s.player.classId?.let { content.classes[it]?.name } ?: LText("—", "—")
            notices += Notice(
                NoticeKind.EVOLUTION,
                texts.formatL("evrim_tamam", "name" to className, "grade" to LText(grade.name, grade.name)),
                listOf(texts.format("genel_evrim_aciklama", "n" to Progression.GENERIC_EVOLUTION_BONUS)),
                from = grade.ordinal - 1,
                to = grade.ordinal,
            )
        } else {
            val def = content.classes[optionId] ?: throw ActionRejected("Tanımsız evrim: $optionId")
            s = s.withPlayer { p ->
                var stats = p.stats
                def.bonus.forEach { (stat, d) -> stats = stats.plus(stat, d) }
                p.copy(classId = def.id, stats = stats)
            }
            notices += Notice(
                NoticeKind.EVOLUTION,
                texts.formatL("evrim_tamam", "name" to def.name, "grade" to LText(grade.name, grade.name)),
                listOf(def.description),
                from = grade.ordinal - 1,
                to = grade.ordinal,
            )
            def.skill?.let {
                val g = effects.grantSkill(s, it)
                s = g.state
                notices += g.notices
            }
        }
        // Ceza yeteneklerinin bedeli her kademede yeniden ödenir.
        s.player.skills.mapNotNull { content.skills[it.id] }.filter { it.penalty.isNotEmpty() }.forEach { def ->
            s = s.withPlayer { p ->
                var stats = p.stats
                def.penalty.forEach { (stat, d) -> stats = stats.plus(stat, d) }
                p.copy(stats = stats).clampResources()
            }
        }
        s = s.withPlayer { it.copy(hp = it.maxHp, mp = it.maxMp) }
        // Ertelenen yetenek evrimleri yeni kademede yeniden sorulur.
        s = s.copy(flags = s.flags.filterNot { it.startsWith(EffectApplier.declinedFlag(PromptKind.SKILL_EVOLUTION, "")) }.toSet())
        val first = effects.grantTitleIfDefined(s, EffectApplier.TITLE_FIRST_EVOLUTION)
        // Tampondaki XP şimdi seviyeye dönüşür.
        val leveled = effects.levelUp(first.state)
        return EffectApplier.Result(leveled.state, notices + first.notices + leveled.notices)
    }

    private fun chooseProfession(state: WorldState, id: String): EffectApplier.Result {
        val def = content.professions[id] ?: throw ActionRejected("Tanımsız meslek: $id")
        val tab = effects.unlockTab(state.withPlayer { it.copy(profession = ProfessionState(id)) }, WorldState.TAB_BAG)
        return EffectApplier.Result(
            tab.state,
            listOf(Notice(NoticeKind.QUEST, texts.formatL("meslek_secildi", "name" to def.name), listOf(def.description))) + tab.notices,
        )
    }

    private fun bindEssence(state: WorldState, id: String, accept: Boolean): EffectApplier.Result {
        if (!accept) {
            return EffectApplier.Result(state.copy(flags = state.flags + EffectApplier.declinedFlag(PromptKind.ESSENCE_BIND, id)), emptyList())
        }
        val def = content.essences[id] ?: throw ActionRejected("Tanımsız Öz: $id")
        return EffectApplier.Result(
            state.withPlayer { it.copy(essences = it.essences + id) },
            listOf(Notice(NoticeKind.EVOLUTION, texts.formatL("oz_baglandi", "name" to def.name), listOf(def.description))),
        )
    }

    /** Serbest stat puanı dağıtımı. Onaylanan dağıtım geri alınamaz. */
    fun allocate(state: WorldState, points: Map<Stat, Int>): EffectApplier.Result {
        if (points.values.any { it < 0 }) throw ActionRejected("Negatif puan dağıtılamaz")
        val total = points.values.sum()
        if (total == 0) throw ActionRejected("Dağıtılacak puan yok")
        if (total > state.player.freePoints) throw ActionRejected("Yetersiz serbest puan: $total > ${state.player.freePoints}")
        val next = state.withPlayer { p ->
            var stats = p.stats
            points.forEach { (stat, d) -> stats = stats.plus(stat, d) }
            // Dağıtım üst sınırları yükseltir; mevcut can ve mana artan kısım kadar yükselir.
            val raised = p.copy(stats = stats, freePoints = p.freePoints - total)
            raised.copy(hp = p.hp + (raised.maxHp - p.maxHp), mp = p.mp + (raised.maxMp - p.maxMp))
        }
        val notices = points.filterValues { it > 0 }.map { (stat, d) ->
            Notice(NoticeKind.STATUS, texts.formatL("stat_degisti", "stat" to stat.label, "delta" to LText("+$d", "+$d")))
        }
        return EffectApplier.Result(next, notices)
    }

    fun craft(state: WorldState, recipeId: String): EffectApplier.Result {
        val recipe = content.recipes[recipeId] ?: throw ActionRejected("Tanımsız tarif: $recipeId")
        val prof = state.player.profession
        if (prof?.id != recipe.profession) throw ActionRejected("Bu tarif için ${recipe.profession} gerekli")
        if (prof.level < recipe.minLevel) throw ActionRejected("Meslek seviyesi yetersiz")
        recipe.inputs.forEach { (item, n) ->
            if (state.player.itemCount(item) < n) throw ActionRejected("Eksik malzeme: $item")
        }
        var s = state
        recipe.inputs.forEach { (item, n) ->
            s = s.withPlayer { p ->
                val left = p.itemCount(item) - n
                p.copy(inventory = if (left == 0) p.inventory - item else p.inventory + (item to left))
            }
        }
        val added = effects.addItem(s, recipe.output, recipe.count)
        s = added.state
        val output = content.items.getValue(recipe.output)
        val notices = mutableListOf(Notice(NoticeKind.SYSTEM, texts.formatL("uretildi", "name" to output.name, "n" to LText("${recipe.count}", "${recipe.count}"))))

        var p = prof.copy(xp = prof.xp + recipe.xp)
        while (p.xp >= Progression.professionXpToNext(p.level)) {
            p = p.copy(xp = p.xp - Progression.professionXpToNext(p.level), level = p.level + 1)
        }
        if (p.level > prof.level) {
            val name = content.professions[p.id]?.name ?: LText(p.id, p.id)
            notices += Notice(NoticeKind.LEVEL_UP, texts.formatL("meslek_seviye", "name" to name, "level" to LText("${p.level}", "${p.level}")))
        }
        s = s.withPlayer { it.copy(profession = p) }.withVar(CRAFT_COUNT_VAR, 1)
        val first = effects.grantTitleIfDefined(s, EffectApplier.TITLE_FIRST_CRAFT)
        return withTriggers(EffectApplier.Result(first.state, notices + first.notices))
    }

    fun useItem(state: WorldState, itemId: String): EffectApplier.Result {
        val def = content.items[itemId] ?: throw ActionRejected("Tanımsız eşya: $itemId")
        val use = def.use
        if (def.type != ItemType.CONSUMABLE || use == null) throw ActionRejected("Bu eşya kullanılamaz")
        if (use.combatOnly) throw ActionRejected("Bu eşya yalnızca savaşta kullanılır")
        if (state.player.itemCount(itemId) <= 0) throw ActionRejected("Envanterde yok")
        val next = state.withPlayer { p ->
            val left = p.itemCount(itemId) - 1
            p.copy(
                hp = (p.hp + use.hp).coerceIn(0, p.maxHp),
                mp = (p.mp + use.mp).coerceIn(0, p.maxMp),
                inventory = if (left == 0) p.inventory - itemId else p.inventory + (itemId to left),
            )
        }
        return EffectApplier.Result(next, listOf(Notice(NoticeKind.SYSTEM, texts.formatL("kullanildi", "name" to def.name))))
    }

    /**
     * Günlük Görev antrenmanından bir set. Aşama 4'te gün döngüsünün serbest eylemi olur;
     * şimdilik Daha fazla → Günlük Görev'den yapılır (DECISIONS #22).
     */
    fun train(state: WorldState): EffectApplier.Result {
        val quest = state.daily ?: throw ActionRejected("Günlük Görev yok")
        val template = content.dailyTemplates[quest.templateId] ?: throw ActionRejected("Şablon yok")
        if (quest.completed) throw ActionRejected("Görev zaten tamam")
        if (template.kind != TRAINING) throw ActionRejected("Bu görev antrenmanla ilerlemez")
        return effects.applyAll(state, listOf(Effect.DailyProgress(TRAINING, 1)))
    }

    private fun withTriggers(r: EffectApplier.Result): EffectApplier.Result {
        val t = effects.checkTriggers(r.state)
        return EffectApplier.Result(t.state, r.notices + t.notices)
    }

    private fun WorldState.withVar(key: String, delta: Int) = copy(vars = vars + (key to variable(key) + delta))

    companion object {
        const val TRAINING = "antrenman"
        const val CRAFT_COUNT_VAR = "uretim_sayisi"
    }
}
