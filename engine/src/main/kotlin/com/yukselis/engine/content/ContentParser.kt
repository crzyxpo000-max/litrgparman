package com.yukselis.engine.content

import com.yukselis.engine.core.MoralAxis
import com.yukselis.engine.i18n.LText
import com.yukselis.engine.i18n.NameEntry
import com.yukselis.engine.i18n.NameRegistry
import com.yukselis.engine.story.ChoiceDef
import com.yukselis.engine.story.Condition
import com.yukselis.engine.story.Content
import com.yukselis.engine.story.Effect
import com.yukselis.engine.story.Notice
import com.yukselis.engine.story.NoticeKind
import com.yukselis.engine.story.Paragraph
import com.yukselis.engine.story.Scene
import com.yukselis.engine.story.SceneNotice
import com.yukselis.engine.story.TitleDef
import com.yukselis.engine.system.Rarity
import com.yukselis.engine.system.Stat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

class ContentFormatException(message: String) : RuntimeException(message)

/**
 * content/ altındaki JSON dosyalarını okur. Anahtarlar Türkçe ASCII'dir (GDD: Teknik mimari).
 * Hatalı içerik açık bir mesajla reddedilir; sessizce yutulmaz.
 */
object ContentParser {

    data class StoryFile(val start: String?, val scenes: List<Scene>)

    fun load(storyFiles: List<String>, namesJson: String, titlesJson: String): Content {
        val stories = storyFiles.map { parseStory(it) }
        val scenes = stories.flatMap { it.scenes }
        val duplicates = scenes.groupBy { it.id }.filterValues { it.size > 1 }.keys
        if (duplicates.isNotEmpty()) throw ContentFormatException("Aynı kimliğe sahip sahneler: $duplicates")
        val start = stories.mapNotNull { it.start }.distinct().singleOrNull()
            ?: throw ContentFormatException("Tam olarak bir 'baslangic' sahnesi tanımlanmalı")
        return Content(
            scenes = scenes.associateBy { it.id },
            names = NameRegistry(parseNames(namesJson)),
            titles = parseTitles(titlesJson).associateBy { it.id },
            startScene = start,
        )
    }

    fun parseStory(text: String): StoryFile {
        val root = Json.parseToJsonElement(text).jsonObject
        val scenes = root.array("sahneler").map { parseScene(it.jsonObject) }
        return StoryFile(root.string("baslangic"), scenes)
    }

    fun parseNames(text: String): List<NameEntry> {
        val root = Json.parseToJsonElement(text).jsonObject
        return NameEntry.Kind.entries.flatMap { kind ->
            val group = root[kind.tokenKey]?.jsonObject ?: JsonObject(emptyMap())
            group.map { (id, value) ->
                val o = value.jsonObject
                val full = ltext(o["ad"], "$id.ad")
                NameEntry(id, kind, full, o["kisa_ad"]?.let { ltext(it, "$id.kisa_ad") } ?: full)
            }
        }
    }

    fun parseTitles(text: String): List<TitleDef> {
        val root = Json.parseToJsonElement(text).jsonObject
        return root.map { (id, value) ->
            val o = value.jsonObject
            TitleDef(
                id = id,
                name = ltext(o["ad"], "$id.ad"),
                description = ltext(o["aciklama"], "$id.aciklama"),
                rarity = Rarity.fromKey(o.string("nadirlik")) ?: Rarity.COMMON,
                statBonus = o["bonus"]?.jsonObject?.map { (k, v) -> stat(k) to v.int(k) }?.toMap() ?: emptyMap(),
            )
        }
    }

    private fun parseScene(o: JsonObject): Scene {
        val id = o.string("id") ?: throw ContentFormatException("Sahnede 'id' eksik")
        return Scene(
            id = id,
            place = o.string("yer"),
            paragraphs = o.array("paragraflar").map { parseParagraph(it, id) },
            notices = o.array("bildirimler").map { parseNotice(it.jsonObject, id) },
            choices = o.array("secenekler").map { parseChoice(it.jsonObject, id) },
            isEnding = o["son"]?.jsonPrimitive?.booleanOrNull ?: false,
        )
    }

    private fun parseParagraph(e: JsonElement, sceneId: String): Paragraph {
        val o = e.jsonObject
        // Kısa biçim: {"tr": ..., "en": ...}; uzun biçim: {"metin": {...}, "kosul": {...}}
        return if ("metin" in o) {
            Paragraph(ltext(o["metin"], "$sceneId.paragraf"), o["kosul"]?.let { condition(it.jsonObject) })
        } else {
            Paragraph(ltext(o, "$sceneId.paragraf"))
        }
    }

    private fun parseNotice(o: JsonObject, sceneId: String): SceneNotice = SceneNotice(
        notice = Notice(
            kind = NoticeKind.fromKey(o.string("tur")),
            title = ltext(o["baslik"], "$sceneId.bildirim.baslik"),
            lines = o.array("satirlar").map { ltext(it, "$sceneId.bildirim.satir") },
            rarity = Rarity.fromKey(o.string("nadirlik")),
        ),
        condition = o["kosul"]?.let { condition(it.jsonObject) },
    )

    private fun parseChoice(o: JsonObject, sceneId: String): ChoiceDef {
        val id = o.string("id") ?: throw ContentFormatException("$sceneId: seçenekte 'id' eksik")
        return ChoiceDef(
            id = id,
            label = ltext(o["etiket"], "$sceneId.$id.etiket"),
            requires = o["gerekli"]?.let { condition(it.jsonObject) },
            showWhen = o["goster"]?.let { condition(it.jsonObject) },
            effects = o.array("etkiler").flatMap { effects(it.jsonObject, "$sceneId.$id") },
            next = o.string("sonraki"),
        )
    }

    private fun condition(o: JsonObject): Condition {
        val parts = o.flatMap { (key, value) ->
            when (key) {
                "stat" -> value.jsonObject.map { (k, v) -> Condition.StatAtLeast(stat(k), v.int(k)) }
                "bayrak" -> listOf(Condition.HasFlag(value.str(key)))
                "bayrak_yok" -> listOf(Condition.LacksFlag(value.str(key)))
                "degisken_min" -> value.jsonObject.map { (k, v) -> Condition.VarAtLeast(k, v.int(k)) }
                else -> throw ContentFormatException("Bilinmeyen koşul: $key")
            }
        }
        return parts.singleOrNull() ?: Condition.All(parts)
    }

    private fun effects(o: JsonObject, where: String): List<Effect> = o.flatMap { (key, value) ->
        when (key) {
            "bayrak" -> listOf(Effect.SetFlag(value.str(key)))
            "bayrak_kaldir" -> listOf(Effect.ClearFlag(value.str(key)))
            "degisken" -> value.jsonObject.map { (k, v) -> Effect.AddVar(k, v.int(k)) }
            "ahlak" -> value.jsonObject.map { (k, v) ->
                Effect.ShiftMorality(MoralAxis.fromKey(k) ?: throw ContentFormatException("$where: bilinmeyen ahlak ekseni $k"), v.int(k))
            }
            "itibar" -> value.jsonObject.map { (k, v) -> Effect.ShiftReputation(k, v.int(k)) }
            "xp" -> listOf(Effect.GainXp(value.jsonPrimitive.longOrNull ?: throw ContentFormatException("$where: xp sayı olmalı")))
            "gun" -> listOf(Effect.AdvanceDays(value.int(key)))
            "stat" -> value.jsonObject.map { (k, v) -> Effect.AddStat(stat(k), v.int(k)) }
            "unvan" -> listOf(Effect.GrantTitle(value.str(key)))
            else -> throw ContentFormatException("$where: bilinmeyen etki '$key'")
        }
    }

    private fun ltext(e: JsonElement?, where: String): LText {
        val o = (e as? JsonObject) ?: throw ContentFormatException("$where: {tr, en} metni bekleniyordu")
        return LText(o.string("tr") ?: "", o.string("en") ?: "")
    }

    private fun stat(key: String): Stat = Stat.fromKey(key) ?: throw ContentFormatException("Bilinmeyen stat: $key")

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

    private fun JsonObject.array(key: String): JsonArray = this[key]?.jsonArray ?: JsonArray(emptyList())

    private fun JsonElement.int(where: String): Int =
        jsonPrimitive.intOrNull ?: throw ContentFormatException("$where: tam sayı bekleniyordu")

    private fun JsonElement.str(where: String): String =
        jsonPrimitive.contentOrNull ?: throw ContentFormatException("$where: metin bekleniyordu")
}
