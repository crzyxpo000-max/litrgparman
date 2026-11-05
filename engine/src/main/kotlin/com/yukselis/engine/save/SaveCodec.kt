package com.yukselis.engine.save

import com.yukselis.engine.combat.CombatSpec
import com.yukselis.engine.combat.CombatState
import com.yukselis.engine.combat.EnemyMemory
import com.yukselis.engine.combat.EnemyState
import com.yukselis.engine.combat.LogEntry
import com.yukselis.engine.combat.Stance
import com.yukselis.engine.combat.StatusInstance
import com.yukselis.engine.core.DailyQuest
import com.yukselis.engine.i18n.LText
import kotlinx.serialization.json.JsonElement
import com.yukselis.engine.core.GameMode
import com.yukselis.engine.core.HistoryEntry
import com.yukselis.engine.core.Morality
import com.yukselis.engine.core.PlayerState
import com.yukselis.engine.core.ProfessionState
import com.yukselis.engine.core.PromptKind
import com.yukselis.engine.core.SkillState
import com.yukselis.engine.core.SystemPrompt
import com.yukselis.engine.core.WorldState
import com.yukselis.engine.system.Affinity
import com.yukselis.engine.system.AffinityVector
import com.yukselis.engine.system.Grade
import com.yukselis.engine.system.Stat
import com.yukselis.engine.system.Stats
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

class SaveFormatException(message: String) : RuntimeException(message)

data class SaveData(val state: WorldState, val savedAtMillis: Long)

/**
 * Kayıt dosyası biçimi (sürümlü JSON). Elle yazıldı: alan adları sabit kalır, eksik alanlar
 * varsayılana düşer, yeni sürümlere geçiş (migration) burada yapılır.
 *
 * Sürümler:
 *  - v1 (Aşama 1): temel durum. Eğilimler `egilim_*` değişkenleriydi.
 *  - v2 (Aşama 2): eğilim vektörü, sınıf, kademe kapısı, yetenekler, envanter, meslek, Öz,
 *    "İlk" unvan kaydı, istem kuyruğu, Günlük Görev, dönüş yığını. v1 → v2: `egilim_*`
 *    değişkenleri eğilim vektörüne taşınır ve değişkenlerden silinir.
 *    Aşama 3 (alan ekleme, sürüm aynı): süren savaş, düşman hafızası, İkinci Şans kademeleri, ölüm.
 */
object SaveCodec {
    const val VERSION = 2

    fun encode(state: WorldState, savedAtMillis: Long): String = buildJsonObject {
        put("v", VERSION)
        put("savedAt", savedAtMillis)
        put("seed", state.seed)
        put("rng", state.rngState)
        put("name", state.playerName)
        put("mode", state.mode.name)
        put("scene", state.sceneId)
        put("day", state.day)
        put("ended", state.ended)
        putJsonArray("flags") { state.flags.sorted().forEach { add(it) } }
        putIntMap("vars", state.vars)
        putIntMap("rep", state.reputation)
        putJsonObject("morality") {
            put("merhamet", state.morality.mercy)
            put("duzen", state.morality.order)
            put("insanlik", state.morality.humanity)
        }
        putJsonObject("player") {
            val p = state.player
            put("level", p.level)
            put("xp", p.xp)
            put("free", p.freePoints)
            put("hp", p.hp)
            put("mp", p.mp)
            putJsonObject("stats") { Stat.entries.forEach { put(it.key, p.stats[it]) } }
            putJsonArray("titles") { p.titles.forEach { add(it) } }
            putIntMap("affinity", p.affinity.values.mapKeys { it.key.key })
            p.classId?.let { put("class", it) }
            put("grade", p.unlockedGrade.name)
            putJsonArray("skills") {
                p.skills.forEach { s -> addJsonObject { put("id", s.id); put("lv", s.level); put("xp", s.xp) } }
            }
            putIntMap("inv", p.inventory)
            put("credits", p.credits)
            put("crystals", p.essenceCrystals)
            p.profession?.let { pr -> putJsonObject("prof") { put("id", pr.id); put("lv", pr.level); put("xp", pr.xp) } }
            putIntMap("insight", p.essenceInsight)
            putJsonArray("essences") { p.essences.forEach { add(it) } }
            p.path?.let { put("path", it) }
        }
        putJsonArray("history") {
            state.history.forEach { h -> addJsonObject { put("s", h.sceneId); put("c", h.choiceId) } }
        }
        putJsonObject("claims") { state.firstClaims.toSortedMap().forEach { (k, v) -> put(k, v) } }
        putJsonArray("prompts") {
            state.prompts.forEach { pr ->
                addJsonObject {
                    put("k", pr.kind.key)
                    putJsonArray("o") { pr.options.forEach { add(it) } }
                    pr.subject?.let { put("s", it) }
                }
            }
        }
        state.daily?.let { d ->
            putJsonObject("daily") {
                put("t", d.templateId); put("day", d.day); put("p", d.progress); put("n", d.steps); put("done", d.completed)
            }
        }
        put("streak", state.dailyStreak)
        putJsonArray("returns") { state.returnStack.forEach { add(it) } }
        put("penalty", state.penaltyPending)
        putJsonArray("tabs") { state.unlockedTabs.sorted().forEach { add(it) } }
        state.combat?.let { put("combat", encodeCombat(it)) }
        putJsonObject("memory") {
            state.enemyMemory.toSortedMap().forEach { (k, m) -> putJsonObject(k) { put("f", m.fled); put("s", m.spared) } }
        }
        putJsonArray("second") { state.secondChances.sortedBy { it.ordinal }.forEach { add(it.name) } }
        put("dead", state.dead)
    }.toString()

    private fun encodeLText(t: LText) = buildJsonObject { put("tr", t.tr); put("en", t.en) }

    private fun decodeLText(e: JsonElement?): LText? =
        (e as? JsonObject)?.let { LText(it.string("tr") ?: "", it.string("en") ?: "") }

    private fun encodeStatuses(list: List<StatusInstance>) = JsonArray(
        list.map { st -> buildJsonObject { put("e", st.effect); put("t", st.turns); put("p", st.power); put("f", st.fresh) } },
    )

    private fun decodeStatuses(e: JsonElement?): List<StatusInstance> =
        (e as? JsonArray)?.mapNotNull { x ->
            val o = x.jsonObject
            o.string("e")?.let { StatusInstance(it, o.int("t") ?: 1, o.int("p") ?: 0, o.bool("f")) }
        } ?: emptyList()

    private fun encodeCombat(c: CombatState) = buildJsonObject {
        putJsonObject("spec") {
            putJsonArray("enemies") { c.spec.enemies.forEach { add(it) } }
            putJsonArray("env") { c.spec.environment.forEach { add(it) } }
            put("win", c.spec.win)
            c.spec.lose?.let { put("lose", it) }
            c.spec.flee?.let { put("flee", it) }
        }
        putJsonArray("enemies") {
            c.enemies.forEach { e ->
                addJsonObject {
                    put("uid", e.uid); put("id", e.id); put("hp", e.hp); put("max", e.maxHp); put("lv", e.level)
                    putJsonObject("stats") { Stat.entries.forEach { put(it.key, e.stats[it]) } }
                    put("st", encodeStatuses(e.statuses))
                    put("phase", e.phase)
                    putIntMap("cd", e.cooldowns)
                    put("an", e.analyzed); put("guard", e.guarding); put("gone", e.gone); put("fled", e.fled)
                    putJsonArray("extra") { e.extraAttacks.forEach { add(it) } }
                }
            }
        }
        put("round", c.round); put("stamina", c.stamina); put("maxStamina", c.maxStamina)
        put("stance", c.stance.key); put("effort", c.effort)
        put("pst", encodeStatuses(c.playerStatuses))
        putIntMap("cd", c.cooldowns)
        put("def", c.defending); put("minor", c.minorUsed); put("extra", c.extraActions)
        putJsonArray("usedEnv") { c.usedEnvironment.sorted().forEach { add(it) } }
        putJsonArray("log") {
            c.log.forEach { l -> addJsonObject { put("p", encodeLText(l.prose)); l.numbers?.let { put("n", encodeLText(it)) } } }
        }
        c.pendingExecution?.let { put("exec", it) }
        putJsonArray("struck") { c.struck.sorted().forEach { add(it) } }
        put("low", c.lowestHpPercent); put("kills", c.kills)
    }

    private fun decodeCombat(o: JsonObject): CombatState {
        val spec = o["spec"] as? JsonObject ?: throw SaveFormatException("Savaşta 'spec' yok")
        return CombatState(
            spec = CombatSpec(
                enemies = spec.strings("enemies"),
                environment = spec.strings("env"),
                win = spec.string("win") ?: throw SaveFormatException("Savaşta kazan sahnesi yok"),
                lose = spec.string("lose"),
                flee = spec.string("flee"),
            ),
            enemies = (o["enemies"] as? JsonArray)?.map { x ->
                val e = x.jsonObject
                val st = e["stats"]?.jsonObject
                EnemyState(
                    uid = e.int("uid") ?: 0,
                    id = e.string("id") ?: "",
                    hp = e.int("hp") ?: 0,
                    maxHp = e.int("max") ?: 1,
                    level = e.int("lv") ?: 1,
                    stats = Stats(Stat.entries.associateWith { st?.int(it.key) ?: Stats.BASE_VALUE }),
                    statuses = decodeStatuses(e["st"]),
                    phase = e.int("phase") ?: 0,
                    cooldowns = e.intMap("cd"),
                    analyzed = e.bool("an"),
                    guarding = e.bool("guard"),
                    extraAttacks = e.strings("extra"),
                    gone = e.bool("gone"),
                    fled = e.bool("fled"),
                )
            } ?: emptyList(),
            round = o.int("round") ?: 1,
            stamina = o.int("stamina") ?: 0,
            maxStamina = o.int("maxStamina") ?: 1,
            stance = Stance.fromKey(o.string("stance")),
            effort = o.int("effort") ?: 0,
            playerStatuses = decodeStatuses(o["pst"]),
            cooldowns = o.intMap("cd"),
            defending = o.bool("def"),
            minorUsed = o.bool("minor"),
            extraActions = o.int("extra") ?: 0,
            usedEnvironment = o.strings("usedEnv").toSet(),
            log = (o["log"] as? JsonArray)?.mapNotNull { x ->
                val l = x.jsonObject
                decodeLText(l["p"])?.let { LogEntry(it, decodeLText(l["n"])) }
            } ?: emptyList(),
            pendingExecution = o.int("exec"),
            struck = (o["struck"] as? JsonArray)?.mapNotNull { it.jsonPrimitive.intOrNull }?.toSet() ?: emptySet(),
            lowestHpPercent = o.int("low") ?: 100,
            kills = o.int("kills") ?: 0,
        )
    }

    fun decode(text: String): SaveData {
        val o = try {
            Json.parseToJsonElement(text).jsonObject
        } catch (e: Exception) {
            throw SaveFormatException("Kayıt okunamadı: ${e.message}")
        }
        val version = o.int("v") ?: throw SaveFormatException("Kayıt sürümü yok")
        if (version > VERSION) throw SaveFormatException("Bu kayıt daha yeni bir oyun sürümüyle yapılmış (v$version)")

        val p = o["player"]?.jsonObject ?: JsonObject(emptyMap())
        val statsObj = p["stats"]?.jsonObject
        val stats = Stats(Stat.entries.associateWith { statsObj?.int(it.key) ?: Stats.BASE_VALUE })
        val level = p.int("level") ?: 1

        var vars = o.intMap("vars")
        var affinity = AffinityVector(
            p.intMap("affinity").mapNotNull { (k, v) -> Affinity.fromKey(k)?.let { it to v } }.toMap(),
        )
        if (version < 2) {
            // v1 → v2: eğilim değişkenleri vektöre taşınır.
            Affinity.LEGACY_VARS.forEach { (key, a) -> vars[key]?.let { affinity = affinity.plus(a, it) } }
            vars = vars - Affinity.LEGACY_VARS.keys
        }

        val defaults = PlayerState(level = level, stats = stats)
        val player = defaults.copy(
            xp = p.long("xp") ?: 0,
            freePoints = p.int("free") ?: 0,
            hp = p.int("hp") ?: defaults.maxHp,
            mp = p.int("mp") ?: defaults.maxMp,
            titles = p.strings("titles"),
            affinity = affinity,
            classId = p.string("class"),
            // Eski kayıtta kademe kapısı yok: oyuncu hangi kademedeyse o kademe açıktır.
            unlockedGrade = Grade.fromKey(p.string("grade")) ?: Grade.forLevel(level),
            skills = (p["skills"] as? JsonArray)?.mapNotNull { e ->
                val s = e.jsonObject
                s.string("id")?.let { SkillState(it, s.int("lv") ?: 1, s.int("xp") ?: 0) }
            } ?: emptyList(),
            inventory = p.intMap("inv"),
            credits = p.int("credits") ?: 0,
            essenceCrystals = p.int("crystals") ?: 0,
            profession = p["prof"]?.jsonObject?.let { pr -> pr.string("id")?.let { ProfessionState(it, pr.int("lv") ?: 1, pr.int("xp") ?: 0) } },
            essenceInsight = p.intMap("insight"),
            essences = p.strings("essences"),
            path = p.string("path"),
        )
        val morality = o["morality"]?.jsonObject
        val state = WorldState(
            seed = o.long("seed") ?: 0,
            rngState = o.long("rng") ?: o.long("seed") ?: 0,
            playerName = o.string("name") ?: "?",
            mode = runCatching { GameMode.valueOf(o.string("mode") ?: "") }.getOrDefault(GameMode.NORMAL),
            sceneId = o.string("scene") ?: throw SaveFormatException("Kayıtta sahne yok"),
            day = o.int("day") ?: 0,
            player = player,
            flags = o.strings("flags").toSet(),
            vars = vars,
            reputation = o.intMap("rep"),
            morality = Morality(
                mercy = morality?.int("merhamet") ?: 0,
                order = morality?.int("duzen") ?: 0,
                humanity = morality?.int("insanlik") ?: 0,
            ),
            history = (o["history"] as? JsonArray)?.mapNotNull { e ->
                val h = e.jsonObject
                val s = h.string("s")
                val c = h.string("c")
                if (s != null && c != null) HistoryEntry(s, c) else null
            } ?: emptyList(),
            ended = o.bool("ended"),
            firstClaims = (o["claims"] as? JsonObject)?.mapNotNull { (k, v) -> v.jsonPrimitive.contentOrNull?.let { k to it } }?.toMap() ?: emptyMap(),
            prompts = (o["prompts"] as? JsonArray)?.mapNotNull { e ->
                val pr = e.jsonObject
                PromptKind.fromKey(pr.string("k"))?.let { SystemPrompt(it, pr.strings("o"), pr.string("s")) }
            } ?: emptyList(),
            daily = (o["daily"] as? JsonObject)?.let { d ->
                d.string("t")?.let { DailyQuest(it, d.int("day") ?: 0, d.int("p") ?: 0, d.int("n") ?: 1, d.bool("done")) }
            },
            dailyStreak = o.int("streak") ?: 0,
            returnStack = o.strings("returns"),
            penaltyPending = o.bool("penalty"),
            combat = (o["combat"] as? JsonObject)?.let { decodeCombat(it) },
            enemyMemory = (o["memory"] as? JsonObject)?.mapNotNull { (k, v) ->
                (v as? JsonObject)?.let { m -> k to EnemyMemory(m.int("f") ?: 0, m.int("s") ?: 0) }
            }?.toMap() ?: emptyMap(),
            secondChances = o.strings("second").mapNotNull { Grade.fromKey(it) }.toSet(),
            dead = o.bool("dead"),
            // Sekme kaydı olmayan eski kayıtlarda sahip olunanlara göre açılır.
            unlockedTabs = if (o["tabs"] is JsonArray) {
                o.strings("tabs").toSet()
            } else {
                buildSet {
                    if (player.skills.isNotEmpty()) add(WorldState.TAB_SKILLS)
                    if (player.inventory.isNotEmpty() || player.credits > 0 || player.profession != null) add(WorldState.TAB_BAG)
                }
            },
        )
        return SaveData(state, o.long("savedAt") ?: 0)
    }

    private fun JsonObjectBuilder.putIntMap(key: String, map: Map<String, Int>) =
        putJsonObject(key) { map.toSortedMap().forEach { (k, v) -> put(k, v) } }

    private fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.intOrNull
    private fun JsonObject.long(key: String): Long? = (this[key] as? JsonPrimitive)?.longOrNull
    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
    private fun JsonObject.bool(key: String): Boolean = (this[key] as? JsonPrimitive)?.booleanOrNull ?: false
    private fun JsonObject.strings(key: String): List<String> =
        (this[key] as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
    private fun JsonObject.intMap(key: String): Map<String, Int> =
        (this[key] as? JsonObject)?.mapNotNull { (k, v) -> v.jsonPrimitive.intOrNull?.let { k to it } }?.toMap() ?: emptyMap()
}
