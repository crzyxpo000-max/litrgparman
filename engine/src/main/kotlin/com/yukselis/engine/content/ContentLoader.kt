package com.yukselis.engine.content

import com.yukselis.engine.story.Content
import java.io.File

/**
 * İçeriğin nereden okunduğunu soyutlar: testlerde ve doğrulayıcıda klasör, Android'de assets.
 * Yerleşim: names.json, titles.json, system.json, classes.json, skills.json, items.json,
 * professions.json, essences.json, daily.json, story/<dosya>.json
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
    const val NAMES = "names.json"
    const val TITLES = "titles.json"
    const val STORY_DIR = "story"

    /** Zorunlu olmayan sistem dosyaları; yoksa ilgili mekanik boş kalır. */
    val SYSTEM_FILES = listOf(
        ContentParser.SYSTEM, ContentParser.CLASSES, ContentParser.SKILLS, ContentParser.ITEMS,
        ContentParser.PROFESSIONS, ContentParser.ESSENCES, ContentParser.DAILY,
    )

    fun load(source: ContentSource): Content {
        val storyFiles = source.list(STORY_DIR)
            .filter { it.endsWith(".json") }
            .sorted()
            .map { source.read("$STORY_DIR/$it") }
        val files = SYSTEM_FILES.mapNotNull { name -> source.readOrNull(name)?.let { name to it } }.toMap()
        return ContentParser.load(storyFiles, source.read(NAMES), source.read(TITLES), files)
    }
}
