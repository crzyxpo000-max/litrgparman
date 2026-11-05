package com.yukselis.engine.content

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.YamlNull
import com.charleskorn.kaml.YamlScalar
import com.charleskorn.kaml.YamlTaggedNode
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * İçerik YAML'la yazılır (GDD: kaml). kaml'ın düğüm API'siyle okunur ve ayrıştırıcının üzerinde
 * çalıştığı genel ağaca (JsonElement) çevrilir; derleyici eklentisi gerekmez (DECISIONS #61).
 * Skalerler metin olarak taşınır; sayı ve mantık değerleri ayrıştırıcıda okunurken dönüştürülür.
 */
object YamlTree {
    private val yaml = Yaml.default

    fun parse(text: String, where: String): JsonElement = try {
        convert(yaml.parseToYamlNode(text))
    } catch (e: ContentFormatException) {
        throw e
    } catch (e: Exception) {
        throw ContentFormatException("$where okunamadı: ${e.message}")
    }

    fun parseObject(text: String, where: String): JsonObject =
        parse(text, where) as? JsonObject ?: throw ContentFormatException("$where: kökte eşleme (map) bekleniyordu")

    private fun convert(node: YamlNode): JsonElement = when (node) {
        is YamlMap -> JsonObject(node.entries.entries.associate { (k, v) -> k.content to convert(v) })
        is YamlList -> JsonArray(node.items.map { convert(it) })
        is YamlScalar -> JsonPrimitive(node.content)
        is YamlNull -> JsonNull
        is YamlTaggedNode -> convert(node.innerNode)
        else -> throw ContentFormatException("Desteklenmeyen YAML düğümü: ${node::class.simpleName}")
    }
}
