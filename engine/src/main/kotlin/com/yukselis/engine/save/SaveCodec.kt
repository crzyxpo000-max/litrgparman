package com.yukselis.engine.save

import com.yukselis.engine.core.DailyQuest
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
    }.toString()

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
