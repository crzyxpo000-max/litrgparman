package com.yukselis.engine.content

import com.yukselis.engine.story.Content
import java.io.File

/**
 * İçeriğin nereden okunduğunu soyutlar: testlerde ve doğrulayıcıda klasör, Android'de assets.
 * Yerleşim: names.json, titles.json, story/<dosya>.json
 */
interface ContentSource {
    fun read(path: String): String
    fun list(dir: String): List<String>
}

class DirectoryContentSource(private val root: File) : ContentSource {
    override fun read(path: String): String = File(root, path).readText(Charsets.UTF_8)
    override fun list(dir: String): List<String> = File(root, dir).list()?.toList() ?: emptyList()
}

object ContentLoader {
    const val NAMES = "names.json"
    const val TITLES = "titles.json"
    const val STORY_DIR = "story"

    fun load(source: ContentSource): Content {
        val storyFiles = source.list(STORY_DIR)
            .filter { it.endsWith(".json") }
            .sorted()
            .map { source.read("$STORY_DIR/$it") }
        return ContentParser.load(storyFiles, source.read(NAMES), source.read(TITLES))
    }
}
