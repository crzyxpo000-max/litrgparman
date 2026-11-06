package com.yukselis.engine.combat

import com.yukselis.engine.core.GameMode
import com.yukselis.engine.core.MoralAxis
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
import com.yukselis.engine.system.ItemType
import com.yukselis.engine.system.Progression
import com.yukselis.engine.system.Rarity
import com.yukselis.engine.system.SkillTarget
import com.yukselis.engine.system.SkillType
import com.yukselis.engine.system.Stat
import com.yukselis.engine.system.Stats
import com.yukselis.engine.system.StatusApplication
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** Oyuncunun savaş eylemleri. Ana eylemler turu tüketir; küçük eylem (eşya) ve ayarlar tüketmez. */
sealed interface CombatAction {
    data class Attack(val target: Int) : CombatAction
    data class UseSkill(val skillId: String, val target: Int) : CombatAction
    data object Defend : CombatAction
    data class Analyze(val target: Int) : CombatAction
    data class Environment(val tag: String) : CombatAction
    data class Talk(val target: Int, val threaten: Boolean) : CombatAction
    data object Flee : CombatAction
    data class UseItem(val itemId: String, val target: Int? = null) : CombatAction
    data class SetStance(val stance: Stance) : CombatAction
    data class SetEffort(val effort: Int) : CombatAction
    data class Execute(val option: String) : CombatAction
}

/**
 * Tur tabanlı savaş (GDD: Savaş sistemi + OP MC: iki savaş modu). Saf: durum alır, yeni durum döndürür.
 * Tüm rastgelelik durumdaki RNG'den gelir; aynı tohum aynı savaşı üretir.
 */
class CombatEngine(private val content: Content, private val effects: EffectApplier) {

    data class Result(val state: WorldState, val notices: List<Notice>)

    private val texts get() = content.texts

    /** Tek eylem boyunca değişen çalışma kopyası. */
    private inner class Ctx(var s: WorldState) {
        val rng = Rng(s.rngState)
        val notices = mutableListOf<Notice>()
        var c: CombatState = s.combat ?: throw ActionRejected("Savaş yok")
        /** Savaş bitti (zafer, kaçış, yenilgi): `c` artık yazılmaz. */
        var over = false

        fun log(prose: LText, numbers: LText? = null) {
            c = c.copy(log = (c.log + LogEntry(capitalize(prose), numbers)).takeLast(CombatState.LOG_LIMIT))
        }

        fun done(): Result {
            val combat = if (over) s.combat else c
            return Result(s.copy(combat = combat, rngState = rng.state), notices)
        }

        fun def(e: EnemyState): EnemyDef = enemyDef(e.id, s) ?: throw ActionRejected("Tanımsız düşman: ${e.id}")
        /** Düelloda sonucu işlenmiş NPC düşmanlar. */
        val npcHandled = mutableSetOf<String>()
        fun updateEnemy(uid: Int, f: (EnemyState) -> EnemyState) {
            c = c.copy(enemies = c.enemies.map { if (it.uid == uid) f(it) else it })
        }
        fun addVar(key: String, delta: Int) {
            s = s.copy(vars = s.vars + (key to s.variable(key) + delta))
        }

        /** Etki uygulamadan önce ve sonra RNG'yi eşitle: aynı rastgele akış iki kez kullanılmasın. */
        fun applyEffects(list: List<Effect>) {
            val r = effects.applyAll(s.copy(rngState = rng.state), list)
            s = r.state
            rng.state = r.state.rngState
            notices += r.notices
        }

        fun sync(block: (WorldState) -> EffectApplier.Result) {
            val r = block(s.copy(rngState = rng.state))
            s = r.state
            rng.state = r.state.rngState
            notices += r.notices
        }
    }

    /** Düzyazıda düşman adı: İngilizcede "the" ile ("the Sewer Rat"); cümle başı sonra büyütülür. */
    private fun proseName(d: EnemyDef): LText =
        // Adı olan NPC özel isimdir: artikel almaz ("Ash grins", "the Sewer Rat staggers").
        if (npcIdOf(d.id) != null) d.name else LText(d.name.tr, if (d.name.en.startsWith("The ")) d.name.en.replaceFirst("The ", "the ") else "the ${d.name.en}")

    /** İngilizce metinde cümle başlarını büyük harfe çevirir ("the Sewer Rat staggers." → "The ..."). */
    private fun capitalize(t: LText): LText = LText(t.tr, SENTENCE_START.replace(t.en) { m -> m.groupValues[1] + m.groupValues[2].uppercase() })

    // ---------------------------------------------------------------- NPC düşmanlar

    /**
     * Düşman tanımı: içerikteki düşman ya da `npc:<kimlik>` (düello, ihanet): NPC'nin o anki seviyesi ve
     * savaş bloğundan türetilir.
     */
    fun enemyDef(id: String, state: WorldState): EnemyDef? {
        content.enemies[id]?.let { return it }
        // Ölçekli başvuru: "id@seviye" (Aşama 7).
        EnemyScaling.parse(id).let { (base, level) -> if (level != null) return content.enemies[base]?.let { EnemyScaling.scale(it, level) } }
        if (!id.startsWith(com.yukselis.engine.npc.NpcEngine.ENEMY_PREFIX)) return null
        val npcId = id.removePrefix(com.yukselis.engine.npc.NpcEngine.ENEMY_PREFIX)
        val d = content.npcs[npcId] ?: return null
        val c = d.combat ?: return null
        val level = state.npcs[npcId]?.level?.takeIf { it > 0 } ?: d.startLevel
        val npc = effects.npc
        return EnemyDef(
            id = id,
            name = npc.name(npcId),
            description = d.description,
            hook = texts.formatL("savas_npc_kanca", "npc" to npc.name(npcId)),
            level = level,
            grade = Grade.forLevel(level),
            maxHp = npc.combatHp(d, level),
            stats = npc.combatStats(d, level),
            behavior = Behavior.CAUTIOUS,
            intelligent = true,
            weakness = null,
            weaknessText = null,
            attacks = listOf(EnemyAttack("npc_saldiri", c.attackName, c.attackStat, ALLY_BASE + 2, 1.0, prose = c.duelProse)),
            xp = 25L * level,
        )
    }

    private fun npcIdOf(enemyId: String): String? =
        enemyId.takeIf { it.startsWith(com.yukselis.engine.npc.NpcEngine.ENEMY_PREFIX) }?.removePrefix(com.yukselis.engine.npc.NpcEngine.ENEMY_PREFIX)

    // ---------------------------------------------------------------- başlangıç

    fun start(state: WorldState, specIn: CombatSpec): Result {
        // Düello/ihanet: "npc:@" olayın odak NPC'sine çözülür.
        val spec = specIn.copy(enemies = specIn.enemies.map { id ->
            if (id == com.yukselis.engine.npc.NpcEngine.ENEMY_PREFIX + "@") com.yukselis.engine.npc.NpcEngine.ENEMY_PREFIX + (state.focusNpc ?: "") else id
        })
        val defs = spec.enemies.map { enemyDef(it, state) ?: throw ActionRejected("Tanımsız düşman: $it") }
        val p = state.player
        val enemies = defs.mapIndexed { i, d ->
            val mem = state.enemyMemory[d.id]?.fled ?: 0
            // Kaçan zeki düşman güçlenmiş olarak döner: her kaçış +2 seviye, statlara ve cana +%15/%20.
            val stats = if (mem == 0) d.stats else Stats(d.stats.values.mapValues { (_, v) -> (v * (1 + 0.15 * mem)).roundToInt() })
            val hp = (d.maxHp * (1 + 0.2 * mem)).roundToInt()
            EnemyState(uid = i, id = d.id, hp = hp, maxHp = hp, level = d.level + 2 * mem, stats = stats)
        }
        val maxStamina = Progression.maxStamina(p.stats)
        // Yoldaşlar savaşa katılır (düelloda karşındaki NPC hariç).
        val opponents = spec.enemies.mapNotNull { npcIdOf(it) }.toSet()
        // Düello bire birdir: karşında bir NPC varsa yoldaşlar karışmaz.
        val allies = if (opponents.isNotEmpty()) emptyList() else effects.npc.companions(state).mapNotNull { n ->
            val d = content.npcs[n.id]?.takeIf { it.combat != null } ?: return@mapNotNull null
            val hp = effects.npc.combatHp(d, n.level)
            AllyState(n.id, hp, hp, n.level, effects.npc.combatStats(d, n.level))
        }
        var combat = CombatState(spec = spec, enemies = enemies, stamina = maxStamina, maxStamina = maxStamina, allies = allies)
        // Önceki savaşın bağlam değişkenleri yeni savaşa sızmasın.
        val clean = state.copy(combat = combat, dead = false, vars = state.vars - CTX_VARS)
        val ctx = Ctx(clean)
        ctx.c = combat

        val names = enemyNames(defs)
        // OP MC "Ezme": hepsi açıkça altta kalıyorsa tek satırla biter.
        val crush = enemies.all { e ->
            val d = ctx.def(e)
            // Adı olan NPC'yle düello her zaman gerçek savaştır (infaz seçimi ahlaki bir karardır).
            !d.boss && npcIdOf(d.id) == null && CombatMath.isCrush(p.level, p.grade, e.level, d.grade)
        }
        if (crush) {
            ctx.c = ctx.c.copy(enemies = ctx.c.enemies.map { it.copy(hp = 0) }, kills = enemies.size)
            ctx.notices += Notice(NoticeKind.SYSTEM, texts.formatL("ezme", "enemies" to names), listOf(texts.format("ezme_satir")))
            finishWin(ctx, crushed = true)
            return ctx.done()
        }

        ctx.notices += Notice(NoticeKind.WARNING, texts.formatL("savas_basladi", "enemies" to names))
        defs.distinctBy { it.id }.forEach { d ->
            ctx.log(d.hook)
            state.enemyMemory[d.id]?.takeIf { it.fled > 0 }?.let {
                ctx.log(texts.formatL("savas_hatirliyor", "enemy" to proseName(d)))
            }
        }
        // İnisiyatif: düşmanlardan biri oyuncuyu geçerse ilk vuruş onlarındır.
        val mine = CombatMath.initiative(p.stats, ctx.rng.d20())
        val theirs = enemies.maxOf { CombatMath.initiative(it.stats, ctx.rng.d20()) }
        if (theirs > mine) {
            ctx.log(texts.format("savas_inisiyatif_kayip"))
            enemyTurn(ctx)
        } else {
            ctx.log(texts.format("savas_inisiyatif_kazanc"))
        }
        if (ctx.c.playerHas(StatusEffects.HASTE)) ctx.c = ctx.c.copy(extraActions = 1)
        return ctx.done()
    }

    // ---------------------------------------------------------------- eylemler

    fun act(state: WorldState, action: CombatAction): Result {
        val ctx = Ctx(state)
        if (state.dead) throw ActionRejected("Öldün")
        val c = ctx.c
        if (c.pendingExecution != null && action !is CombatAction.Execute) throw ActionRejected("Önce infaz seçilmeli")

        when (action) {
            is CombatAction.SetStance -> {
                ctx.c = c.copy(stance = action.stance)
                return ctx.done()
            }
            is CombatAction.SetEffort -> {
                ctx.c = c.copy(effort = action.effort.coerceIn(0, CombatState.MAX_EFFORT))
                return ctx.done()
            }
            is CombatAction.UseItem -> {
                useItem(ctx, action)
                checkVictory(ctx)
                return ctx.done()
            }
            is CombatAction.Execute -> {
                execute(ctx, action.option)
                return ctx.done()
            }
            else -> Unit
        }

        // Sersem oyuncu eylem yapamaz (normalde tur sonunda atlanır; güvence).
        if (c.playerHas(StatusEffects.STUN)) {
            ctx.log(texts.format("savas_sersem"))
            endPlayerAction(ctx)
            return ctx.done()
        }

        when (action) {
            is CombatAction.Attack -> attack(ctx, action.target)
            is CombatAction.UseSkill -> useSkill(ctx, action.skillId, action.target)
            CombatAction.Defend -> {
                // "korunan_muttefik": savunurken düşük canlı yoldaşa gelen saldırı üstüne çekilince sayılır (enemyAttack).
                ctx.c = ctx.c.copy(defending = true, stamina = (ctx.c.stamina + 10).coerceAtMost(ctx.c.maxStamina))
                ctx.log(texts.format("savas_savunma"))
            }
            is CombatAction.Analyze -> analyze(ctx, action.target)
            is CombatAction.Environment -> environment(ctx, action.tag)
            is CombatAction.Talk -> talk(ctx, action.target, action.threaten)
            CombatAction.Flee -> flee(ctx)
            else -> Unit
        }
        if (!ctx.over) {
            checkVictory(ctx)
            if (!ctx.over && ctx.c.pendingExecution == null) endPlayerAction(ctx)
        }
        return ctx.done()
    }

    private fun endPlayerAction(ctx: Ctx) {
        if (ctx.c.extraActions > 0) {
            ctx.c = ctx.c.copy(extraActions = ctx.c.extraActions - 1)
            return
        }
        alliesTurn(ctx)
        if (ctx.over || ctx.c.pendingExecution != null) return
        checkVictory(ctx)
        if (ctx.over || ctx.c.pendingExecution != null) return
        enemyTurn(ctx)
        if (!ctx.over) endRound(ctx)
    }

    private fun requireTarget(ctx: Ctx, uid: Int): EnemyState =
        ctx.c.enemy(uid)?.takeIf { it.alive } ?: throw ActionRejected("Geçersiz hedef: $uid")

    private fun attack(ctx: Ctx, uid: Int) {
        val target = requireTarget(ctx, uid)
        if (!passesFear(ctx)) return
        strike(ctx, target, base = BASIC_BASE, stat = Stat.STR, multiplier = BASIC_MULT, tags = listOf("fiziksel", "yakin"), name = null)
    }

    private fun passesFear(ctx: Ctx): Boolean {
        if (!ctx.c.playerHas(StatusEffects.FEAR)) return true
        if (CombatMath.fearCheck(ctx.s.player.stats[Stat.WIL], ctx.rng.d20())) return true
        ctx.log(texts.format("savas_korku"))
        return false
    }

    /** Oyuncunun bir düşmana vuruşu: isabet, kritik, zaaf, duruş, efor, kademe farkı. */
    private fun strike(
        ctx: Ctx,
        target: EnemyState,
        base: Int,
        stat: Stat,
        multiplier: Double,
        tags: List<String>,
        name: LText?,
        status: StatusApplication? = null,
        usedEffort: Int = payEffort(ctx),
    ): Boolean {
        val p = ctx.s.player
        val def = ctx.def(target)

        val hitP = CombatMath.hitChance(p.stats, target.stats, ctx.c.playerHas(StatusEffects.BLIND), usedEffort * CombatMath.EFFORT_HIT)
        val firstStrike = target.uid !in ctx.c.struck
        ctx.c = ctx.c.copy(struck = ctx.c.struck + target.uid)
        if (!ctx.rng.chance(hitP)) {
            ctx.log(texts.formatL("savas_iskaladin", "target" to proseName(def)), pct(hitP))
            return false
        }
        // Sersem ya da korkmuş hedefe vuruş "arkadan vuruş" sayılır (kaynaştırma sayacı).
        if (target.has(StatusEffects.STUN) || target.has(StatusEffects.FEAR) || target.has(StatusEffects.BLIND)) ctx.addVar("arkadan_vurus", 1)

        val crit = ctx.rng.chance(CombatMath.critChance(p.stats))
        // Analiz edilen nişan alınabilir zaaf (göz) her vuruşta hedeflenebilir.
        val aimed = target.analyzed && def.weakness in AIMABLE_WEAKNESSES
        val weak = def.weakness != null && (def.weakness in tags || aimed)
        var extra = ctx.c.stance.damage * (1 + usedEffort * CombatMath.EFFORT_DAMAGE)
        if (crit) extra *= CombatMath.critMultiplier(p.stats)
        if (weak) extra *= CombatMath.WEAKNESS_MULTIPLIER
        if (target.guarding) extra *= CombatMath.DEFEND_MULTIPLIER
        val magical = stat in MAGIC_STATS
        val dmg = CombatMath.damage(
            base, p.stats[stat], multiplier,
            CombatMath.resistance(target.stats, magical, target.has(StatusEffects.BURN)),
            ctx.rng, CombatMath.gradeMultiplier(p.grade, def.grade), extra,
        )
        val prose = if (name == null) texts.formatL("savas_vurdun", "target" to proseName(def)) else texts.formatL("savas_yetenek_vurdu", "skill" to name, "target" to proseName(def))
        val tagsLine = listOfNotNull(
            if (crit) texts.format("savas_kritik") else null,
            if (weak) texts.format("savas_zaaf") else null,
        )
        ctx.log(prose, numbers(dmg, tagsLine))
        val killed = damageEnemy(ctx, target.uid, dmg)
        if (killed && firstStrike && crit) ctx.addVar("ilk_vurus_kritik_oldurme", 1)
        if (!killed && status != null && status.effect !in StatusEffects.SELF && ctx.rng.chance(status.chance)) {
            applyEnemyStatus(ctx, target.uid, status, ctx.s.player.level)
        }
        return true
    }

    /** Eforu öder (yetmezse 0); alan yeteneğinde bir kez ödenir. */
    private fun payEffort(ctx: Ctx): Int {
        val effort = ctx.c.effort
        val used = if (ctx.c.stamina >= effort * CombatMath.EFFORT_STAMINA) effort else 0
        ctx.c = ctx.c.copy(stamina = ctx.c.stamina - used * CombatMath.EFFORT_STAMINA)
        return used
    }

    private fun useSkill(ctx: Ctx, skillId: String, uid: Int) {
        val owned = ctx.s.player.skill(skillId) ?: throw ActionRejected("Yetenek yok: $skillId")
        val def = content.skills[skillId] ?: throw ActionRejected("Tanımsız yetenek: $skillId")
        if (def.type != SkillType.ACTIVE) throw ActionRejected("Pasif yetenek kullanılamaz")
        if ((ctx.c.cooldowns[skillId] ?: 0) > 0) throw ActionRejected("Bekleme süresinde")
        if (ctx.s.player.mp < def.costMp || ctx.c.stamina < def.costStamina) {
            throw ActionRejected("Yetersiz MP/Dayanım")
        }
        val damaging = !def.heals && def.target != SkillTarget.SELF
        if (damaging && !passesFear(ctx)) return
        ctx.s = ctx.s.copy(player = ctx.s.player.copy(mp = ctx.s.player.mp - def.costMp))
        ctx.c = ctx.c.copy(
            stamina = ctx.c.stamina - def.costStamina,
            cooldowns = ctx.c.cooldowns + (skillId to def.cooldown),
        )
        val stat = def.stat ?: Stat.STR
        // Yetenek seviyesi gücü artırır: seviye başı +%5.
        val levelBoost = 1 + 0.05 * (owned.level - 1)
        val base = (def.base * levelBoost).roundToInt()

        when {
            def.heals -> {
                val raw = (base + ctx.s.player.stats[stat] * def.multiplier).roundToInt()
                // Zehir iyileşmeyi yarıya indirir (GDD).
                val amount = if (ctx.c.playerHas(StatusEffects.POISON)) raw / 2 else raw
                val p = ctx.s.player
                ctx.s = ctx.s.copy(player = p.copy(hp = (p.hp + amount).coerceAtMost(p.maxHp)))
                ctx.log(texts.formatL("savas_yetenek_kendine", "skill" to def.name), texts.format("savas_iyilesme", "n" to amount))
            }
            def.target == SkillTarget.SELF -> {
                val power = (base + ctx.s.player.stats[stat] * def.multiplier).roundToInt()
                def.status?.let { applyPlayerStatus(ctx, StatusInstance(it.effect, it.duration, power)) }
                ctx.log(texts.formatL("savas_yetenek_kendine", "skill" to def.name), texts.format("savas_kalkan", "n" to power))
            }
            def.target == SkillTarget.ALL -> {
                val effort = payEffort(ctx)
                ctx.c.living.forEach { e ->
                    if (!ctx.over) strike(ctx, e, base, stat, def.multiplier, def.tags, def.name, def.status, effort)
                }
            }
            else -> strike(ctx, requireTarget(ctx, uid), base, stat, def.multiplier, def.tags, def.name, def.status)
        }
        // Kendine uygulanan durum (ör. hızlanma) hasar veren yetenekte de işler.
        if (def.target != SkillTarget.SELF && def.status != null && def.status.effect in StatusEffects.SELF) {
            applyPlayerStatus(ctx, StatusInstance(def.status.effect, def.status.duration, base))
        }
        if (("menzilli" in def.tags || "ok" in def.tags) && ctx.c.effort > 0) ctx.addVar("hareketli_atis", 1)
        ctx.applyEffects(listOf(Effect.SkillXp(skillId, 1)))
    }

    private fun analyze(ctx: Ctx, uid: Int) {
        val target = requireTarget(ctx, uid)
        val def = ctx.def(target)
        val p = ctx.s.player
        ctx.addVar("analiz_sayisi", 1)
        if (ctx.rng.d20() + p.stats[Stat.PER] >= 8 + target.level / 2) {
            ctx.updateEnemy(uid) { it.copy(analyzed = true) }
            val lines = listOfNotNull(
                texts.formatL("savas_analiz_satir", "level" to num(target.level), "hp" to LText("${target.hp}/${target.maxHp}", "${target.hp}/${target.maxHp}")),
                def.weaknessText,
            )
            ctx.notices += Notice(NoticeKind.ANALYZE, texts.formatL("savas_analiz_basarili", "enemy" to def.name), lines, Rarity.RARE)
            ctx.log(texts.formatL("savas_analiz_basarili", "enemy" to def.name))
        } else {
            ctx.log(texts.formatL("savas_analiz_basarisiz", "enemy" to proseName(def)))
        }
    }

    private fun environment(ctx: Ctx, tag: String) {
        if (tag !in ctx.c.spec.environment) throw ActionRejected("Ortamda yok: $tag")
        if (tag in ctx.c.usedEnvironment) throw ActionRejected("Zaten kullanıldı: $tag")
        ctx.c = ctx.c.copy(usedEnvironment = ctx.c.usedEnvironment + tag)
        val p = ctx.s.player
        ctx.log(texts.format("ortam_${tag}_metin"))
        when (tag) {
            ENV_CEILING -> ctx.c.living.forEach { e ->
                if (ctx.over) return@forEach
                val d = ctx.def(e)
                val dmg = CombatMath.damage(20, p.stats[Stat.STR], 1.5, 0.0, ctx.rng, CombatMath.gradeMultiplier(p.grade, d.grade))
                ctx.log(texts.formatL("savas_ortam_vurdu", "target" to proseName(d)), numbers(dmg, emptyList()))
                if (!damageEnemy(ctx, e.uid, dmg) && ctx.rng.chance(0.5)) applyEnemyStatus(ctx, e.uid, StatusApplication(StatusEffects.STUN, 1.0, 1), p.level)
            }
            ENV_WATER -> {
                val shock = p.skills.any { s -> content.skills[s.id]?.tags?.any { it == "simsek" || it == "buz" } == true }
                val effect = if (shock) StatusEffects.STUN else StatusEffects.BLIND
                ctx.c.living.forEach { applyEnemyStatus(ctx, it.uid, StatusApplication(effect, 1.0, if (shock) 1 else 2), p.level) }
            }
            ENV_FLAMMABLE -> {
                ctx.c.living.forEach { applyEnemyStatus(ctx, it.uid, StatusApplication(StatusEffects.BURN, 1.0, 3), p.level) }
                if (ctx.rng.chance(0.3)) applyPlayerStatus(ctx, StatusInstance(StatusEffects.BURN, 2, CombatMath.dotPower(p.level)))
            }
            ENV_DARK -> ctx.c.living.forEach { applyEnemyStatus(ctx, it.uid, StatusApplication(StatusEffects.BLIND, 1.0, 2), p.level) }
            ENV_NARROW -> Unit // Sürü bonusu kalkar ve tur başına en fazla iki düşman saldırır (enemyTurn).
            else -> Unit
        }
    }

    private fun talk(ctx: Ctx, uid: Int, threaten: Boolean) {
        val target = requireTarget(ctx, uid)
        val def = ctx.def(target)
        if (!def.intelligent) {
            ctx.log(texts.formatL("savas_konus_anlamiyor", "enemy" to proseName(def)))
            return
        }
        val p = ctx.s.player
        val success = if (threaten) {
            p.grade > def.grade || p.level >= target.level + 5 ||
                ctx.rng.d20() + (p.stats[Stat.STR] + p.stats[Stat.CHA]) / 2 >= 14 + target.level / 2
        } else {
            val wounded = if (target.hp * 100 / target.maxHp < 30) 4 else 0
            ctx.rng.d20() + p.stats[Stat.CHA] + wounded >= 12 + target.level / 2
        }
        if (!success) {
            ctx.log(texts.formatL("savas_konus_basarisiz", "enemy" to proseName(def)))
            return
        }
        ctx.updateEnemy(uid) { it.copy(gone = true) }
        remember(ctx, def, spared = true)
        if (threaten) {
            ctx.s = ctx.s.copy(morality = ctx.s.morality.shift(MoralAxis.MERCY, -1))
            ctx.log(texts.formatL("savas_tehdit", "enemy" to proseName(def)))
        } else {
            ctx.s = ctx.s.copy(morality = ctx.s.morality.shift(MoralAxis.MERCY, 2))
            ctx.log(texts.formatL("savas_konus_basarili", "enemy" to proseName(def)))
        }
    }

    private fun flee(ctx: Ctx) {
        val living = ctx.c.living
        if (living.any { ctx.def(it).boss }) {
            ctx.log(texts.format("savas_kac_boss"))
            return
        }
        if (ctx.rng.chance(CombatMath.fleeChance(ctx.s.player.stats, living.map { it.stats }))) {
            ctx.notices += Notice(NoticeKind.SYSTEM, texts.format("savas_kac_basarili"))
            val target = ctx.c.spec.flee ?: ctx.c.spec.win
            leave(ctx, target)
        } else {
            ctx.log(texts.format("savas_kac_basarisiz"))
        }
    }

    private fun useItem(ctx: Ctx, action: CombatAction.UseItem) {
        if (ctx.c.minorUsed) throw ActionRejected("Bu tur küçük eylem kullanıldı")
        val def = content.items[action.itemId] ?: throw ActionRejected("Tanımsız eşya")
        val use = def.use
        if (def.type != ItemType.CONSUMABLE || use == null) throw ActionRejected("Kullanılamaz")
        if (ctx.s.player.itemCount(def.id) <= 0) throw ActionRejected("Envanterde yok")
        ctx.sync { effects.addItem(it, def.id, -1) }
        ctx.c = ctx.c.copy(minorUsed = true)
        val poisoned = ctx.c.playerHas(StatusEffects.POISON)
        if (use.hp > 0 || use.mp > 0) {
            val heal = if (poisoned) use.hp / 2 else use.hp
            val p = ctx.s.player
            ctx.s = ctx.s.copy(player = p.copy(hp = (p.hp + heal).coerceAtMost(p.maxHp), mp = (p.mp + use.mp).coerceAtMost(p.maxMp)))
            ctx.log(texts.formatL("savas_esya", "item" to def.name), texts.format("savas_iyilesme", "n" to heal))
        }
        if (use.cures.isNotEmpty()) {
            ctx.c = ctx.c.copy(playerStatuses = ctx.c.playerStatuses.filterNot { it.effect in use.cures })
            if (use.hp == 0) ctx.log(texts.formatL("savas_esya", "item" to def.name))
        }
        if (use.damage > 0) {
            val target = action.target?.let { ctx.c.enemy(it) }?.takeIf { it.alive } ?: ctx.c.living.firstOrNull() ?: return
            val d = ctx.def(target)
            val dmg = CombatMath.damage(
                use.damage, 0, 0.0, CombatMath.resistance(target.stats, true, false), ctx.rng,
                CombatMath.gradeMultiplier(ctx.s.player.grade, d.grade),
            )
            ctx.log(texts.formatL("savas_esya_firlat", "item" to def.name, "target" to proseName(d)), numbers(dmg, emptyList()))
            val tags = listOfNotNull(use.inflicts?.let { if (it == StatusEffects.BURN) "ates" else null })
            val weak = d.weakness != null && d.weakness in tags
            val killed = damageEnemy(ctx, target.uid, if (weak) (dmg * CombatMath.WEAKNESS_MULTIPLIER).roundToInt() else dmg)
            if (!killed) use.inflicts?.let { applyEnemyStatus(ctx, target.uid, StatusApplication(it, 1.0, 3), ctx.s.player.level) }
        }
    }

    // ---------------------------------------------------------------- hasar ve durumlar

    /** Düşmana hasar uygular; kalkan, faz ve ölüm. Son düşman düşerse infaz bekler. Öldüyse true. */
    private fun damageEnemy(ctx: Ctx, uid: Int, amount: Int): Boolean {
        val e = ctx.c.enemy(uid) ?: return false
        if (!e.alive) return false
        var dmg = amount
        val shield = e.statuses.firstOrNull { it.effect == StatusEffects.SHIELD }
        var statuses = e.statuses
        if (shield != null) {
            val absorbed = minOf(shield.power, dmg)
            dmg -= absorbed
            statuses = statuses.map { if (it === shield) it.copy(power = it.power - absorbed) else it }.filter { it.effect != StatusEffects.SHIELD || it.power > 0 }
        }
        val hp = (e.hp - dmg).coerceAtLeast(0)
        ctx.updateEnemy(uid) { it.copy(hp = hp, statuses = statuses) }
        if (hp == 0) {
            val def = ctx.def(e)
            ctx.c = ctx.c.copy(kills = ctx.c.kills + 1)
            if (ctx.c.living.isEmpty()) {
                ctx.c = ctx.c.copy(pendingExecution = uid)
                ctx.notices += Notice(NoticeKind.WARNING, capitalize(texts.formatL("savas_infaz_teklif", "enemy" to proseName(def))))
            } else {
                ctx.log(texts.formatL("savas_dustu", "enemy" to proseName(def)))
            }
            return true
        }
        checkPhase(ctx, uid)
        return false
    }

    private fun checkPhase(ctx: Ctx, uid: Int) {
        val e = ctx.c.enemy(uid) ?: return
        val def = ctx.def(e)
        var phase = e.phase
        val pct = e.hp * 100 / e.maxHp
        while (phase < def.phases.size && pct <= def.phases[phase].threshold) {
            val ph = def.phases[phase]
            phase++
            var stats = ctx.c.enemy(uid)!!.stats
            ph.buff.forEach { (s, d) -> stats = stats.plus(s, d) }
            val newPhase = phase
            ctx.updateEnemy(uid) { it.copy(phase = newPhase, stats = stats, extraAttacks = it.extraAttacks + listOfNotNull(ph.attack?.id)) }
            ctx.log(ph.text)
            ctx.notices += Notice(NoticeKind.WARNING, texts.formatL("savas_faz", "enemy" to def.name, "n" to num(newPhase)), listOf(ph.text))
        }
    }

    private fun applyEnemyStatus(ctx: Ctx, uid: Int, st: StatusApplication, sourceLevel: Int) {
        val e = ctx.c.enemy(uid) ?: return
        if (!e.alive) return
        val power = if (st.effect == StatusEffects.POISON || st.effect == StatusEffects.BURN) CombatMath.dotPower(sourceLevel) else 0
        ctx.updateEnemy(uid) { it.copy(statuses = it.statuses.filterNot { s -> s.effect == st.effect } + StatusInstance(st.effect, st.duration, power)) }
        ctx.log(texts.formatL("savas_durum_uygulandi", "target" to proseName(ctx.def(e)), "effect" to texts.format("durum_${st.effect}")))
    }

    private fun applyPlayerStatus(ctx: Ctx, st: StatusInstance) {
        ctx.c = ctx.c.copy(playerStatuses = ctx.c.playerStatuses.filterNot { it.effect == st.effect } + st.copy(fresh = true))
        if (st.effect == StatusEffects.HASTE && ctx.c.extraActions == 0) ctx.c = ctx.c.copy(extraActions = 1)
    }

    /** Oyuncuya hasar: kalkan, duruş, savunma; ölüm ve İkinci Şans. */
    private fun damagePlayer(ctx: Ctx, amount: Int) {
        var dmg = amount
        val shield = ctx.c.playerStatuses.firstOrNull { it.effect == StatusEffects.SHIELD }
        if (shield != null) {
            val absorbed = minOf(shield.power, dmg)
            dmg -= absorbed
            ctx.c = ctx.c.copy(
                playerStatuses = ctx.c.playerStatuses
                    .map { if (it === shield) it.copy(power = it.power - absorbed) else it }
                    .filter { it.effect != StatusEffects.SHIELD || it.power > 0 },
            )
        }
        val p = ctx.s.player
        val hp = (p.hp - dmg).coerceAtLeast(0)
        ctx.s = ctx.s.copy(player = p.copy(hp = hp))
        val pct = hp * 100 / p.maxHp
        if (pct < ctx.c.lowestHpPercent) ctx.c = ctx.c.copy(lowestHpPercent = pct)
        if (hp == 0) playerDown(ctx)
    }

    private fun playerDown(ctx: Ctx) {
        val s = ctx.s
        val grade = s.player.grade
        // Yazılmış yenilgi sahnesi ölüm değildir (her iki modda): İkinci Şans harcanmaz (DECISIONS #52).
        ctx.c.spec.lose?.let { lose ->
            ctx.s = ctx.s.copy(player = ctx.s.player.copy(hp = 1))
            ctx.notices += Notice(NoticeKind.WARNING, texts.format("savas_yenildin"))
            leave(ctx, lose)
            return
        }
        if (s.mode == GameMode.NORMAL && grade !in s.secondChances) {
            // İkinci Şans: bedeli rastgele bir yeteneğin −2 seviyesi ya da bir unvan.
            var p = s.player.copy(hp = (s.player.maxHp * CombatMath.SECOND_CHANCE_HP).roundToInt().coerceAtLeast(1))
            val candidates = p.skills.filter { content.skills[it.id]?.rarity != Rarity.UNIQUE && it.level > 1 }
            val cost: LText = if (candidates.isNotEmpty()) {
                val pick = candidates[ctx.rng.nextInt(candidates.size)]
                p = p.copy(skills = p.skills.map { if (it.id == pick.id) it.copy(level = (it.level - 2).coerceAtLeast(1), xp = 0) else it })
                texts.formatL("ikinci_sans_yetenek", "skill" to (content.skills[pick.id]?.name ?: LText(pick.id, pick.id)))
            } else if (p.titles.isNotEmpty()) {
                val pick = p.titles[ctx.rng.nextInt(p.titles.size)]
                p = p.copy(titles = p.titles - pick)
                texts.formatL("ikinci_sans_unvan", "title" to (content.titles[pick]?.name ?: LText(pick, pick)))
            } else {
                texts.format("ikinci_sans_bedelsiz")
            }
            ctx.s = s.copy(player = p, secondChances = s.secondChances + grade)
            ctx.notices += Notice(NoticeKind.ECHO, texts.format("ikinci_sans"), listOf(cost))
            ctx.log(texts.format("ikinci_sans"))
            return
        }
        when {
            s.mode == GameMode.IRONMAN -> {
                ctx.notices += Notice(NoticeKind.WARNING, texts.format("savas_oldun"))
                val harvest = Content.HARVEST_SCENE.takeIf { it in content.scenes }
                if (harvest != null) leave(ctx, harvest) else { ctx.s = ctx.s.copy(combat = null, ended = true); ctx.over = true }
            }
            else -> {
                ctx.notices += Notice(NoticeKind.WARNING, texts.format("savas_oldun"), listOf(texts.format("savas_kayit_noktasi")))
                ctx.s = ctx.s.copy(dead = true)
                ctx.over = true
                ctx.s = ctx.s.copy(combat = ctx.c)
            }
        }
    }

    // ---------------------------------------------------------------- düşman turu

    private fun enemyTurn(ctx: Ctx) {
        val narrow = ENV_NARROW in ctx.c.usedEnvironment
        var attacked = 0
        for (snapshot in ctx.c.living) {
            if (ctx.over) return
            val e = ctx.c.enemy(snapshot.uid) ?: continue
            if (!e.alive) continue
            val def = ctx.def(e)
            ctx.updateEnemy(e.uid) { it.copy(guarding = false) }
            if (e.has(StatusEffects.STUN)) {
                ctx.log(texts.formatL("savas_dusman_sersem", "enemy" to proseName(def)))
                continue
            }
            val hpPct = e.hp * 100 / e.maxHp
            if (def.behavior == Behavior.FLEEING && hpPct < 30 && ctx.rng.chance(0.6)) {
                ctx.updateEnemy(e.uid) { it.copy(gone = true, fled = true) }
                remember(ctx, def, spared = false)
                ctx.log(texts.formatL("savas_dusman_kacti", "enemy" to proseName(def)))
                continue
            }
            if (def.behavior == Behavior.CAUTIOUS && hpPct < 50 && ctx.rng.chance(0.35)) {
                ctx.updateEnemy(e.uid) { it.copy(guarding = true) }
                ctx.log(texts.formatL("savas_dusman_savunma", "enemy" to proseName(def)))
                continue
            }
            if (narrow && attacked >= 2) continue
            if (e.has(StatusEffects.FEAR) && !CombatMath.fearCheck(e.stats[Stat.WIL], ctx.rng.d20())) {
                ctx.log(texts.formatL("savas_dusman_korku", "enemy" to proseName(def)))
                continue
            }
            enemyAttack(ctx, e, def, narrow)
            attacked++
            // Hızlanmış düşman iki kez saldırır.
            if (!ctx.over && ctx.c.enemy(e.uid)?.has(StatusEffects.HASTE) == true) enemyAttack(ctx, ctx.c.enemy(e.uid)!!, def, narrow)
        }
    }

    private fun enemyAttack(ctx: Ctx, e: EnemyState, def: EnemyDef, narrow: Boolean) {
        val pool = def.attacks + def.phases.mapNotNull { it.attack }.filter { it.id in e.extraAttacks }
        if (pool.isEmpty()) return
        // Hepsi beklemedeyse en kısa beklemeli saldırı (içerik her zaman en az bir beklemesiz saldırı vermeli).
        val ready = pool.filter { (e.cooldowns[it.id] ?: 0) <= 0 }.ifEmpty { listOf(pool.minBy { e.cooldowns[it.id] ?: 0 }) }
        val atk = if (def.behavior == Behavior.AGGRESSIVE || def.boss) {
            ready.maxBy { it.base + e.stats[it.stat] * it.multiplier }
        } else {
            ready[ctx.rng.nextInt(ready.size)]
        }
        ctx.updateEnemy(e.uid) { it.copy(cooldowns = it.cooldowns + (atk.id to atk.cooldown)) }
        // Yoldaşlar da hedef olabilir; savunan oyuncu düşük canlı yoldaşa gelen saldırıyı üstüne çeker.
        val standing = ctx.c.standingAllies
        if (standing.isNotEmpty() && ctx.rng.chance(ALLY_TARGET_CHANCE)) {
            val ally = standing[ctx.rng.nextInt(standing.size)]
            if (ctx.c.defending && ally.hp * 100 / ally.maxHp < 50) {
                ctx.c = ctx.c.copy(protectedAllies = ctx.c.protectedAllies + 1)
                ctx.addVar("korunan_muttefik", 1)
                ctx.log(texts.formatL("savas_koruma", "ally" to effects.npc.name(ally.npc), "enemy" to proseName(def)))
            } else {
                attackAlly(ctx, e, def, atk, ally, narrow)
                return
            }
        }
        val p = ctx.s.player
        val pack = if (def.behavior == Behavior.PACK && !narrow) 0.05 * (ctx.c.living.count { it.uid != e.uid && ctx.def(it).behavior == Behavior.PACK }) else 0.0
        val hitP = CombatMath.hitChance(e.stats, p.stats, e.has(StatusEffects.BLIND), pack)
        val n = proseName(def)
        val prose = LText(atk.prose.tr.replace("{dusman}", n.tr), atk.prose.en.replace("{dusman}", n.en))
        if (!ctx.rng.chance(hitP)) {
            ctx.log(texts.formatL("savas_dusman_iskaladi", "enemy" to n), pct(hitP))
            return
        }
        val crit = ctx.rng.chance(CombatMath.critChance(e.stats))
        var extra = ctx.c.stance.incoming
        if (ctx.c.defending) extra *= CombatMath.DEFEND_MULTIPLIER
        if (crit) extra *= CombatMath.critMultiplier(e.stats)
        val magical = atk.stat in MAGIC_STATS
        val dmg = CombatMath.damage(
            atk.base, e.stats[atk.stat], atk.multiplier,
            CombatMath.resistance(p.stats, magical, ctx.c.playerHas(StatusEffects.BURN)),
            ctx.rng, CombatMath.gradeMultiplier(def.grade, p.grade), extra,
        )
        ctx.log(prose, numbers(dmg, listOfNotNull(if (crit) texts.format("savas_kritik") else null)))
        atk.status?.let { st ->
            if (ctx.rng.chance(st.chance)) {
                if (st.effect in StatusEffects.SELF) {
                    ctx.updateEnemy(e.uid) { it.copy(statuses = it.statuses.filterNot { s -> s.effect == st.effect } + StatusInstance(st.effect, st.duration, atk.base)) }
                } else {
                    val power = if (st.effect == StatusEffects.POISON || st.effect == StatusEffects.BURN) CombatMath.dotPower(e.level) else 0
                    applyPlayerStatus(ctx, StatusInstance(st.effect, st.duration, power))
                    ctx.log(texts.formatL("savas_durum_sana", "effect" to texts.format("durum_${st.effect}")))
                }
            }
        }
        damagePlayer(ctx, dmg)
    }

    // ---------------------------------------------------------------- yoldaşlar

    /** Yoldaşların turu: role göre saldırır, oyuncuyu kalkanla korur ya da iyileştirir. */
    private fun alliesTurn(ctx: Ctx) {
        for (snapshot in ctx.c.allies) {
            if (ctx.over || ctx.c.pendingExecution != null) return
            val a = ctx.c.allies.firstOrNull { it.npc == snapshot.npc } ?: continue
            if (a.down) continue
            val d = content.npcs[a.npc] ?: continue
            val cb = d.combat ?: continue
            val name = effects.npc.name(a.npc)
            fun setAlly(f: (AllyState) -> AllyState) {
                ctx.c = ctx.c.copy(allies = ctx.c.allies.map { if (it.npc == a.npc) f(it) else it })
            }
            setAlly { it.copy(cooldown = (it.cooldown - 1).coerceAtLeast(0)) }
            val p = ctx.s.player
            val hpPct = p.hp * 100 / p.maxHp
            if (cb.role == com.yukselis.engine.npc.CombatRole.GUARD && hpPct < 50 && a.cooldown <= 0) {
                val power = 5 + 2 * a.level
                applyPlayerStatus(ctx, StatusInstance(StatusEffects.SHIELD, 2, power))
                setAlly { it.copy(cooldown = 3) }
                ctx.log(texts.formatL("savas_yoldas_kalkan", "ally" to name), texts.format("savas_kalkan", "n" to power))
                continue
            }
            if (cb.role == com.yukselis.engine.npc.CombatRole.HEALER && hpPct < 60 && a.cooldown <= 0) {
                val raw = 10 + 2 * a.level + a.stats[Stat.WIS]
                val heal = if (ctx.c.playerHas(StatusEffects.POISON)) raw / 2 else raw
                ctx.s = ctx.s.copy(player = p.copy(hp = (p.hp + heal).coerceAtMost(p.maxHp)))
                setAlly { it.copy(cooldown = 2) }
                ctx.log(texts.formatL("savas_yoldas_sifa", "ally" to name), texts.format("savas_iyilesme", "n" to heal))
                continue
            }
            val target = ctx.c.living.minByOrNull { it.hp } ?: return
            val td = ctx.def(target)
            val hitP = CombatMath.hitChance(a.stats, target.stats, false, 0.0)
            if (!ctx.rng.chance(hitP)) {
                ctx.log(texts.formatL("savas_yoldas_iskaladi", "ally" to name, "target" to proseName(td)), pct(hitP))
                continue
            }
            val dmg = CombatMath.damage(
                ALLY_BASE, a.stats[cb.attackStat], 1.0,
                CombatMath.resistance(target.stats, cb.attackStat in MAGIC_STATS, target.has(StatusEffects.BURN)),
                ctx.rng, CombatMath.gradeMultiplier(Grade.forLevel(a.level), td.grade),
            )
            val n = proseName(td)
            ctx.log(
                LText(cb.prose.tr.replace("{npc}", name.tr).replace("{dusman}", n.tr), cb.prose.en.replace("{npc}", name.en).replace("{dusman}", n.en)),
                numbers(dmg, emptyList()),
            )
            damageEnemy(ctx, target.uid, dmg)
        }
    }

    private fun attackAlly(ctx: Ctx, e: EnemyState, def: EnemyDef, atk: EnemyAttack, ally: AllyState, narrow: Boolean) {
        val pack = if (def.behavior == Behavior.PACK && !narrow) 0.05 * (ctx.c.living.count { it.uid != e.uid && ctx.def(it).behavior == Behavior.PACK }) else 0.0
        val name = effects.npc.name(ally.npc)
        val n = proseName(def)
        val hitP = CombatMath.hitChance(e.stats, ally.stats, e.has(StatusEffects.BLIND), pack)
        if (!ctx.rng.chance(hitP)) {
            ctx.log(texts.formatL("savas_dusman_yoldas_iskaladi", "enemy" to n, "ally" to name), pct(hitP))
            return
        }
        val dmg = CombatMath.damage(
            atk.base, e.stats[atk.stat], atk.multiplier,
            CombatMath.resistance(ally.stats, atk.stat in MAGIC_STATS, false),
            ctx.rng, CombatMath.gradeMultiplier(def.grade, Grade.forLevel(ally.level)),
        )
        val hp = (ally.hp - dmg).coerceAtLeast(0)
        ctx.c = ctx.c.copy(allies = ctx.c.allies.map { if (it.npc == ally.npc) it.copy(hp = hp, down = hp == 0) else it })
        ctx.log(texts.formatL("savas_dusman_yoldasa", "enemy" to n, "ally" to name), numbers(dmg, emptyList()))
        if (hp == 0) {
            ctx.log(texts.formatL("savas_yoldas_dustu", "ally" to name))
            ctx.s = effects.npc.allyDowned(ctx.s, ally.npc)
        }
    }

    // ---------------------------------------------------------------- tur sonu

    private fun endRound(ctx: Ctx) {
        // Oyuncunun durumları
        var statuses = mutableListOf<StatusInstance>()
        var dot = 0
        val p = ctx.s.player
        for (st in ctx.c.playerStatuses) {
            dot += when (st.effect) {
                StatusEffects.BLEED -> CombatMath.bleedDamage(p.maxHp)
                StatusEffects.POISON, StatusEffects.BURN -> st.power
                else -> 0
            }
            when {
                st.fresh -> statuses += st.copy(fresh = false)
                st.turns > 1 -> statuses += st.copy(turns = st.turns - 1)
            }
        }
        ctx.c = ctx.c.copy(
            playerStatuses = statuses,
            cooldowns = ctx.c.cooldowns.mapValues { (_, v) -> (v - 1).coerceAtLeast(0) },
            stamina = (ctx.c.stamina + ctx.c.stance.staminaRegen).coerceAtMost(ctx.c.maxStamina),
            defending = false,
            minorUsed = false,
            round = ctx.c.round + 1,
        )
        if (dot > 0) {
            ctx.log(texts.format("savas_durum_hasar_sana"), numbers(dot, emptyList()))
            damagePlayer(ctx, dot)
            if (ctx.over) return
        }
        // Düşmanların durumları
        for (e in ctx.c.living) {
            var eDot = 0
            val kept = mutableListOf<StatusInstance>()
            for (st in e.statuses) {
                eDot += when (st.effect) {
                    StatusEffects.BLEED -> CombatMath.bleedDamage(e.maxHp)
                    StatusEffects.POISON, StatusEffects.BURN -> st.power
                    else -> 0
                }
                if (st.turns > 1) kept += st.copy(turns = st.turns - 1)
            }
            ctx.updateEnemy(e.uid) { it.copy(statuses = kept, cooldowns = it.cooldowns.mapValues { (_, v) -> (v - 1).coerceAtLeast(0) }) }
            if (eDot > 0) {
                ctx.log(texts.formatL("savas_durum_hasar", "target" to proseName(ctx.def(e))), numbers(eDot, emptyList()))
                damageEnemy(ctx, e.uid, eDot)
            }
        }
        if (ctx.c.playerHas(StatusEffects.HASTE)) ctx.c = ctx.c.copy(extraActions = 1)
        checkVictory(ctx)
        // Sersem oyuncunun turu atlanır.
        if (!ctx.over && ctx.c.pendingExecution == null && ctx.c.playerHas(StatusEffects.STUN)) {
            ctx.log(texts.format("savas_sersem"))
            // Sersemleme bu atlanan turla tükenir (süresi 1'den uzunsa bir azalır).
            ctx.c = ctx.c.copy(
                playerStatuses = ctx.c.playerStatuses.mapNotNull {
                    if (it.effect != StatusEffects.STUN) it else if (it.turns > 1) it.copy(turns = it.turns - 1) else null
                },
            )
            enemyTurn(ctx)
            if (!ctx.over) endRound(ctx)
        }
    }

    private fun checkVictory(ctx: Ctx) {
        if (ctx.over || ctx.c.pendingExecution != null) return
        if (ctx.c.living.isEmpty()) finishWin(ctx, crushed = false)
    }

    // ---------------------------------------------------------------- infaz ve zafer

    private fun execute(ctx: Ctx, option: String) {
        val uid = ctx.c.pendingExecution ?: throw ActionRejected("İnfaz bekleyen düşman yok")
        val e = ctx.c.enemy(uid)!!
        val def = ctx.def(e)
        when (option) {
            EXEC_BRUTAL -> {
                ctx.s = ctx.s.copy(morality = ctx.s.morality.shift(MoralAxis.MERCY, -3).shift(MoralAxis.HUMANITY, -2))
                ctx.addVar("acimasiz_infaz", 1)
            }
            EXEC_CLEAN -> ctx.s = ctx.s.copy(morality = ctx.s.morality.shift(MoralAxis.ORDER, 2))
            EXEC_SPARE -> {
                ctx.s = ctx.s.copy(morality = ctx.s.morality.shift(MoralAxis.MERCY, 5))
                ctx.updateEnemy(uid) { it.copy(gone = true, hp = 1) }
                ctx.c = ctx.c.copy(kills = ctx.c.kills - 1)
                remember(ctx, def, spared = true)
            }
            else -> throw ActionRejected("Bilinmeyen infaz: $option")
        }
        val npcSuffix = if (npcIdOf(e.id) != null) "_npc" else ""
        ctx.log(texts.formatL("infaz_${option}${npcSuffix}_metin", "enemy" to proseName(def)))
        ctx.c = ctx.c.copy(pendingExecution = null)
        // Tanıklar (yoldaşlar) infazı kendi değerlerine göre yorumlar.
        val deltas = when (option) {
            EXEC_BRUTAL -> mapOf(MoralAxis.MERCY to -3, MoralAxis.HUMANITY to -2)
            EXEC_SPARE -> mapOf(MoralAxis.MERCY to 5)
            else -> mapOf(MoralAxis.ORDER to 2)
        }
        // Akılsız bir canavarın infazı tanıkları sarsmaz; zeki düşman ve insan sarsar.
        if (def.intelligent) ctx.sync { effects.npc.interpret(it, deltas, null) }
        // Düelloda yenilen NPC: acımasız infaz onu öldürür (kalıcı), temiz infaz ezer, bağışlamak bağ kurar.
        npcIdOf(e.id)?.let { npcId ->
            ctx.npcHandled += npcId
            ctx.sync { effects.npc.duelResult(it, npcId, option) }
        }
        finishWin(ctx, crushed = false)
    }

    private fun finishWin(ctx: Ctx, crushed: Boolean) {
        val p0 = ctx.s.player
        val killed = ctx.c.enemies.filter { it.hp <= 0 && !it.gone }
        val spared = ctx.c.enemies.filter { it.gone && !it.fled }
        var xp = 0.0
        var gapBreak = 0
        var bossDown = false
        for (e in ctx.c.enemies) {
            val d = ctx.def(e)
            val share = when {
                e in killed -> 1.0
                e in spared && e.hp > 0 && !d.boss -> 0.5
                else -> 0.0
            }
            var x = d.xp * Progression.xpMultiplier(e.level, p0.level) * share
            if (share > 0 && d.grade > p0.grade) {
                x *= CombatMath.GAP_BREAK_XP
                gapBreak = maxOf(gapBreak, d.grade.ordinal - p0.grade.ordinal)
            }
            xp += x
            if (e in killed && d.boss) {
                bossDown = true
                if ("kapi" in d.tags && e.level >= p0.level + 5) ctx.addVar("kapi_boss_solo_ust5", 1)
            }
            if (e in killed) ctx.s = ctx.s.copy(flags = ctx.s.flags + "yenildi_${EnemyScaling.baseId(d.id)}")
        }
        ctx.addVar("oldurme_sayisi", killed.size)
        // Düelloda (infazsız) yenilen NPC'ler ezilmiş sayılır; ölmezler.
        killed.mapNotNull { npcIdOf(it.id) }.filter { it !in ctx.npcHandled }.forEach { npcId ->
            ctx.npcHandled += npcId
            ctx.sync { effects.npc.duelResult(it, npcId, null) }
        }
        // Yas Tutan koşulu için son savaş günü.
        ctx.s = ctx.s.copy(vars = ctx.s.vars + (com.yukselis.engine.npc.NpcEngine.VAR_LAST_FIGHT to ctx.s.day + 1))
        // Savaş bağlamı değişkenleri (uyanış koşulları için; her savaşta üzerine yazılır).
        val beaten = killed.isNotEmpty() || spared.isNotEmpty()
        ctx.s = ctx.s.copy(
            vars = ctx.s.vars + mapOf(
                CTX_WON to if (beaten) 1 else 0,
                CTX_LOWEST_HP to ctx.c.lowestHpPercent,
                CTX_GRADE_GAP to gapBreak,
                CTX_BOSS to if (bossDown) 1 else 0,
                CTX_CRUSH to if (crushed) 1 else 0,
                CTX_PROTECTED to ctx.c.protectedAllies,
            ),
        )
        ctx.over = true
        val target = ctx.c.spec.win
        ctx.s = ctx.s.copy(combat = null, sceneId = target, ended = content.scenes[target]?.isEnding ?: false)

        ctx.notices += Notice(
            NoticeKind.SYSTEM,
            when {
                crushed -> texts.format("ezme_zafer")
                beaten -> texts.format("savas_zafer")
                else -> texts.format("savas_hepsi_kacti")
            },
        )
        val rewards = mutableListOf<Effect>(Effect.GainXp(xp.roundToLong()))
        killed.forEach { e ->
            val d = ctx.def(e)
            if (d.credits > 0) rewards += Effect.AddCredits(d.credits)
            d.loot.forEach { (item, chance) -> if (item in content.items && ctx.rng.chance(chance)) rewards += Effect.AddItem(item, 1) }
        }
        ctx.applyEffects(rewards)

        if (gapBreak > 0) {
            ctx.addVar(VAR_GAP_BREAKS, 1)
            val n = ctx.s.variable(VAR_GAP_BREAKS)
            ctx.notices += Notice(NoticeKind.EVOLUTION, texts.format("kademe_kirma"), listOf(texts.format("kademe_kirma_satir")), Rarity.LEGENDARY)
            GAP_TITLES.filter { (count, _) -> n >= count }.forEach { (_, title) ->
                ctx.sync { effects.grantTitleIfDefined(it, title) }
            }
            // Kademe Kırma: Benzersiz yetenek şansı.
            if (ctx.rng.chance(GAP_UNIQUE_CHANCE)) ctx.sync { effects.grantExtraUniqueSkill(it) }
        }
        ctx.sync { st -> checkAwakenings(st).let { EffectApplier.Result(it.state, it.notices) } }
        ctx.sync { effects.checkTriggers(it) }
    }

    /** Uyanış tetikleyicileri: koşulu sağlanan her uyanış bir kez çalışır. */
    fun checkAwakenings(state: WorldState): Result {
        var s = state
        val notices = mutableListOf<Notice>()
        for (a in content.awakenings) {
            val flag = "uyanis_${a.id}"
            if (s.hasFlag(flag) || !a.condition.test(s)) continue
            s = s.copy(flags = s.flags + flag)
            notices += Notice(NoticeKind.ECHO, a.title, a.lines, Rarity.MYTHIC)
            val r = effects.applyAll(s, a.effects)
            s = r.state
            notices += r.notices
        }
        return Result(s, notices)
    }

    private fun leave(ctx: Ctx, scene: String) {
        ctx.over = true
        ctx.s = ctx.s.copy(combat = null, sceneId = scene, ended = content.scenes[scene]?.isEnding ?: false)
    }

    private fun remember(ctx: Ctx, def: EnemyDef, spared: Boolean) {
        if (!def.intelligent) return
        val m = ctx.s.enemyMemory[def.id] ?: EnemyMemory()
        val updated = if (spared) m.copy(spared = m.spared + 1) else m.copy(fled = m.fled + 1)
        ctx.s = ctx.s.copy(enemyMemory = ctx.s.enemyMemory + (def.id to updated))
    }

    // ---------------------------------------------------------------- otomatik tur

    /** "Otomatik tur": makul bir eylem önerir (infaz → temiz, düşük canda iksir, en güçlü yetenek, en zayıf hedef). */
    fun autoAction(state: WorldState): CombatAction {
        val c = state.combat ?: throw ActionRejected("Savaş yok")
        if (c.pendingExecution != null) return CombatAction.Execute(EXEC_CLEAN)
        val p = state.player
        if (p.hp * 100 / p.maxHp < 35 && !c.minorUsed) {
            content.items.values.filter { it.type == ItemType.CONSUMABLE && (it.use?.hp ?: 0) > 0 && p.itemCount(it.id) > 0 }.maxByOrNull { it.use!!.hp }?.let {
                return CombatAction.UseItem(it.id)
            }
        }
        val target = c.living.minByOrNull { it.hp } ?: return CombatAction.Defend
        val skill = p.skills.mapNotNull { s -> content.skills[s.id]?.let { s to it } }
            .filter { (_, d) ->
                d.type == SkillType.ACTIVE && !d.heals && d.target != SkillTarget.SELF &&
                    (c.cooldowns[d.id] ?: 0) == 0 && p.mp >= d.costMp && c.stamina >= d.costStamina
            }
            .maxByOrNull { (s, d) -> d.base + p.stats[d.stat ?: Stat.STR] * d.multiplier + s.level }
        return if (skill != null) CombatAction.UseSkill(skill.second.id, target.uid) else CombatAction.Attack(target.uid)
    }

    // ---------------------------------------------------------------- yardımcılar

    private fun enemyNames(defs: List<EnemyDef>): LText {
        val groups = defs.groupBy { it.id }.values.map { g -> g.first().name to g.size }
        return LText(
            groups.joinToString(", ") { (n, k) -> if (k > 1) "${n.tr} ×$k" else n.tr },
            groups.joinToString(", ") { (n, k) -> if (k > 1) "${n.en} ×$k" else n.en },
        )
    }

    private fun num(n: Int) = LText("$n", "$n")

    private fun numbers(dmg: Int, tags: List<LText>): LText {
        val tr = (listOf("−$dmg") + tags.map { it.tr }).joinToString(" · ")
        val en = (listOf("−$dmg") + tags.map { it.en }).joinToString(" · ")
        return LText(tr, en)
    }

    private fun pct(p: Double): LText {
        val v = (p * 100).roundToInt()
        return LText("%$v", "$v%")
    }

    companion object {
        const val BASIC_BASE = 5
        const val BASIC_MULT = 1.0
        val MAGIC_STATS = setOf(Stat.INT, Stat.WIS, Stat.WIL)

        const val EXEC_BRUTAL = "acimasiz"
        const val EXEC_CLEAN = "temiz"
        const val EXEC_SPARE = "bagisla"
        val EXECUTIONS = listOf(EXEC_BRUTAL, EXEC_CLEAN, EXEC_SPARE)

        const val ENV_CEILING = "tavan"
        const val ENV_WATER = "su"
        const val ENV_FLAMMABLE = "yanici"
        const val ENV_DARK = "karanlik"
        const val ENV_NARROW = "dar_gecit"
        val ENVIRONMENTS = listOf(ENV_CEILING, ENV_WATER, ENV_FLAMMABLE, ENV_DARK, ENV_NARROW)

        // Savaş bağlamı değişkenleri (uyanış koşulları)
        const val CTX_WON = "_savas_kazanildi"
        const val CTX_LOWEST_HP = "_savas_en_dusuk_can"
        const val CTX_GRADE_GAP = "_savas_kademe_farki"
        const val CTX_BOSS = "_savas_boss"
        const val CTX_CRUSH = "_savas_ezme"

        const val CTX_PROTECTED = "_savas_korunan"

        val CTX_VARS = setOf(CTX_WON, CTX_LOWEST_HP, CTX_GRADE_GAP, CTX_BOSS, CTX_CRUSH, CTX_PROTECTED)

        /** Yoldaşın temel saldırısı ve düşmanın yoldaşı hedef alma olasılığı. */
        const val ALLY_BASE = 4
        const val ALLY_TARGET_CHANCE = 0.35

        /** Analizden sonra her vuruşta hedeflenebilen zaaflar. */
        val AIMABLE_WEAKNESSES = setOf("goz")

        /** İçerikte izin verilen zaaf etiketleri. */
        val WEAKNESS_TAGS = setOf("ates", "buz", "simsek", "isik", "karanlik", "delici", "fiziksel", "zihin", "ses", "ruzgar", "goz")

        private val SENTENCE_START = Regex("(^|[.!?]\\s+)([a-z])")

        const val VAR_GAP_BREAKS = "kademe_kirma"
        /** "Kademe Kıran" unvan zinciri: kaçıncı kademe kırmada hangi unvan. */
        val GAP_TITLES = listOf(1 to "kademe_kiran", 3 to "kademe_kiran_2", 10 to "kademe_kiran_3")
        const val GAP_UNIQUE_CHANCE = 0.25
    }
}
