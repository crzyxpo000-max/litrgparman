package com.yukselis.engine.content

import com.yukselis.engine.story.Content
import java.io.File

/**
 * İçeriğin nereden okunduğunu soyutlar: testlerde ve doğrulayıcıda klasör, Android'de assets.
 * Yerleşim: names.yaml, titles.yaml, system.yaml, classes.yaml, skills.yaml, items.yaml,
 * professions.yaml, essences.yaml, daily.yaml, enemies.yaml, awakenings.yaml, story/<dosya>.yaml
 */
interface ContentSource {
    fun read(path: String): String
    fun list(dir: String): List<String>

    /** Dosya yoksa null. */
    fun readOrNull(path: String): String? = runCatching { read(path) }.getOrNull()
}

class DirectoryContentSource(private val root: File) : ContentSource {
    override fun read(path: String): String = File(root, path).readText(Charsets.UTF_8)
    override fun list(dir: String): List<String> = File(root, dir).list()?.toList() ?: emptyList()
    override fun readOrNull(path: String): String? = File(root, path).takeIf { it.isFile }?.readText(Charsets.UTF_8)
}

object ContentLoader {
    const val NAMES = "names.yaml"
    const val TITLES = "titles.yaml"
    const val STORY_DIR = "story"
    const val EVENTS_DIR = "events"
    const val NPCS_DIR = "npcs"

    /** Zorunlu olmayan sistem dosyaları; yoksa ilgili mekanik boş kalır. */
    val SYSTEM_FILES = listOf(
        ContentParser.SYSTEM, ContentParser.CLASSES, ContentParser.SKILLS, ContentParser.ITEMS,
        ContentParser.PROFESSIONS, ContentParser.ESSENCES, ContentParser.DAILY, ContentParser.ENEMIES, ContentParser.AWAKENINGS,
        ContentParser.REGIONS, ContentParser.FACTIONS, ContentParser.FEATS, ContentParser.QUESTS, ContentParser.FLAGS,
        ContentParser.TRAITS, ContentParser.KNOWLEDGE, ContentParser.ARMY, ContentParser.RANKING,
    )

    /**
     * Kayıt dosyaları (bayraklar, görevler, bilgiler, başarılar) parçalanabilir: `flags.yaml` + `flags/` klasöründeki parçalar.
     * Parçaların üst düzey eşlemleri (ör. `bayraklar`) birleştirilir; aynı kimlik iki kez tanımlanırsa hata.
     */
    val MERGEABLE = setOf(ContentParser.FLAGS, ContentParser.QUESTS, ContentParser.KNOWLEDGE, ContentParser.FEATS)

    private fun mergedFile(source: ContentSource, name: String): String? {
        val main = source.readOrNull(name)
        if (name !in MERGEABLE) return main
        val dir = name.removeSuffix(".yaml")
        val parts = listOfNotNull(main) + source.list(dir).filter { it.endsWith(".yaml") }.sorted().map { source.read("$dir/$it") }
        if (parts.size <= 1) return parts.firstOrNull()
        val merged = mutableMapOf<String, MutableMap<String, kotlinx.serialization.json.JsonElement>>()
        parts.forEachIndexed { i, text ->
            YamlTree.parseObject(text, "$dir parça ${i + 1}").forEach { (top, v) ->
                val group = merged.getOrPut(top) { linkedMapOf() }
                (v as? kotlinx.serialization.json.JsonObject)?.forEach { (k, x) ->
                    if (k in group) throw ContentFormatException("$dir: '$k' iki kez tanımlı")
                    group[k] = x
                }
            }
        }
        return kotlinx.serialization.json.JsonObject(merged.mapValues { kotlinx.serialization.json.JsonObject(it.value) }).toString()
    }

    fun load(source: ContentSource): Content {
        val storyFiles = source.list(STORY_DIR)
            .filter { it.endsWith(".yaml") }
            .sorted()
            .map { source.read("$STORY_DIR/$it") }
        val files = SYSTEM_FILES.mapNotNull { name -> mergedFile(source, name)?.let { name to it } }.toMap()
        val eventFiles = source.list(EVENTS_DIR).filter { it.endsWith(".yaml") }.sorted().map { source.read("$EVENTS_DIR/$it") }
        val npcFiles = source.list(NPCS_DIR).filter { it.endsWith(".yaml") }.sorted().map { source.read("$NPCS_DIR/$it") }
        return ContentParser.load(storyFiles, source.read(NAMES), source.read(TITLES), files, eventFiles, npcFiles)
    }
}
