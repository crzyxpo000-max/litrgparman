package com.yukselis.engine.save

import com.yukselis.engine.core.GameMode
import com.yukselis.engine.core.HistoryEntry
import com.yukselis.engine.core.Morality
import com.yukselis.engine.core.PlayerState
import com.yukselis.engine.core.WorldState
import com.yukselis.engine.system.Stat
import com.yukselis.engine.system.Stats
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
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
 */
object SaveCodec {
    const val VERSION = 1

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
        putJsonObject("vars") { state.vars.toSortedMap().forEach { (k, v) -> put(k, v) } }
        putJsonObject("rep") { state.reputation.toSortedMap().forEach { (k, v) -> put(k, v) } }
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
        }
        putJsonArray("history") {
            state.history.forEach { h -> addJsonObject { put("s", h.sceneId); put("c", h.choiceId) } }
        }
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
        val defaults = PlayerState(level = level, stats = stats)
        val player = defaults.copy(
            xp = p.long("xp") ?: 0,
            freePoints = p.int("free") ?: 0,
            hp = p.int("hp") ?: defaults.maxHp,
            mp = p.int("mp") ?: defaults.maxMp,
            titles = p.strings("titles"),
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
            vars = o.intMap("vars"),
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
            ended = (o["ended"] as? JsonPrimitive)?.booleanOrNull ?: false,
        )
        return SaveData(state, o.long("savedAt") ?: 0)
    }

    private fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.intOrNull
    private fun JsonObject.long(key: String): Long? = (this[key] as? JsonPrimitive)?.longOrNull
    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
    private fun JsonObject.strings(key: String): List<String> =
        (this[key] as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
    private fun JsonObject.intMap(key: String): Map<String, Int> =
        (this[key] as? JsonObject)?.mapNotNull { (k, v) -> v.jsonPrimitive.intOrNull?.let { k to it } }?.toMap() ?: emptyMap()
}
