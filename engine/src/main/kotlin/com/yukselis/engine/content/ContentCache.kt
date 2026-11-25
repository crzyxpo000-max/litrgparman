package com.yukselis.engine.content

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * Ayrıştırılmış içerik ağaçları: YAML'dan okunup (ya da ikili önbellekten yüklenip) doğrudan [ContentParser.loadTrees]
 * ile [com.yukselis.engine.story.Content] nesnesine çevrilir.
 */
data class ContentBundle(
    val stories: List<JsonObject>,
    val names: JsonObject,
    val titles: JsonObject,
    val files: Map<String, JsonObject>,
    val events: List<JsonObject>,
    val npcs: List<JsonObject>,
)

/**
 * İçerik ağaçlarının ikili önbelleği (UI_REVIEW B5, DECISIONS #175): Android açılışında 6,6 MB YAML'ı her seferinde
 * kaml ile ayrıştırmak yerine derleme zamanında üretilen tek bir ikili dosyadan (`content.bin`) okur.
 *
 * Biçim:
 * - sihirli sayı (`YPCB`), sürüm (`VERSION`)
 * - ortak anahtar sözlüğü (tekrarlanan `tr`, `en`, `id`, `etiket`, `etkiler` … dizinle saklanır, bellek paylaşılır)
 * - sırasıyla: `stories`, `names`, `titles`, `files`, `events`, `npcs`
 */
object ContentCache {
    const val FILE_NAME = "content.bin"
    private const val MAGIC = 0x59504342 // "YPCB"
    private const val VERSION = 1

    private const val TAG_NULL = 0
    private const val TAG_PRIMITIVE = 1
    private const val TAG_ARRAY = 2
    private const val TAG_OBJECT = 3
    private const val BUFFER_BYTES = 64 * 1024

    fun write(bundle: ContentBundle, output: OutputStream) {
        val keyList = ArrayList<String>(512)
        val keyIndex = HashMap<String, Int>(512)
        fun collect(e: JsonElement) {
            when (e) {
                is JsonObject -> e.forEach { (k, v) ->
                    if (k !in keyIndex) {
                        keyIndex[k] = keyList.size
                        keyList.add(k)
                    }
                    collect(v)
                }
                is JsonArray -> e.forEach(::collect)
                else -> Unit
            }
        }
        bundle.stories.forEach(::collect)
        collect(bundle.names)
        collect(bundle.titles)
        bundle.files.values.forEach(::collect)
        bundle.events.forEach(::collect)
        bundle.npcs.forEach(::collect)

        val out = DataOutputStream(BufferedOutputStream(output, BUFFER_BYTES))
        out.writeInt(MAGIC)
        out.writeInt(VERSION)
        writeVarInt(out, keyList.size)
        for (k in keyList) writeUtf(out, k)

        writeObjectList(out, bundle.stories, keyIndex)
        writeObject(out, bundle.names, keyIndex)
        writeObject(out, bundle.titles, keyIndex)
        writeVarInt(out, bundle.files.size)
        for ((name, obj) in bundle.files) {
            writeUtf(out, name)
            writeObject(out, obj, keyIndex)
        }
        writeObjectList(out, bundle.events, keyIndex)
        writeObjectList(out, bundle.npcs, keyIndex)
        out.flush()
    }

    fun read(input: InputStream): ContentBundle {
        val inp = DataInputStream(BufferedInputStream(input, BUFFER_BYTES))
        val magic = inp.readInt()
        if (magic != MAGIC) throw ContentFormatException("İçerik önbelleği tanınmadı (sihirli sayı uyumsuz)")
        val version = inp.readInt()
        if (version != VERSION) throw ContentFormatException("İçerik önbelleği sürümü uyumsuz: $version (beklenen $VERSION)")

        val keyCount = readVarInt(inp)
        val keys = Array(keyCount) { readUtf(inp) }

        val stories = readObjectList(inp, keys)
        val names = readObject(inp, keys)
        val titles = readObject(inp, keys)
        val fileCount = readVarInt(inp)
        val files = LinkedHashMap<String, JsonObject>(fileCount * 2)
        repeat(fileCount) {
            val name = readUtf(inp)
            files[name] = readObject(inp, keys)
        }
        val events = readObjectList(inp, keys)
        val npcs = readObjectList(inp, keys)
        return ContentBundle(stories, names, titles, files, events, npcs)
    }

    private fun writeObjectList(out: DataOutputStream, list: List<JsonObject>, keys: Map<String, Int>) {
        writeVarInt(out, list.size)
        for (o in list) writeObject(out, o, keys)
    }

    private fun readObjectList(inp: DataInputStream, keys: Array<String>): List<JsonObject> {
        val n = readVarInt(inp)
        return List(n) { readObject(inp, keys) }
    }

    private fun writeObject(out: DataOutputStream, obj: JsonObject, keys: Map<String, Int>) {
        writeVarInt(out, obj.size)
        for ((k, v) in obj) {
            writeVarInt(out, keys.getValue(k))
            writeElement(out, v, keys)
        }
    }

    private fun readObject(inp: DataInputStream, keys: Array<String>): JsonObject {
        val n = readVarInt(inp)
        val map = LinkedHashMap<String, JsonElement>(n * 2)
        repeat(n) {
            val k = keys[readVarInt(inp)]
            map[k] = readElement(inp, keys)
        }
        return JsonObject(map)
    }

    private fun writeElement(out: DataOutputStream, elem: JsonElement, keys: Map<String, Int>) {
        when (elem) {
            is JsonNull -> out.writeByte(TAG_NULL)
            is JsonPrimitive -> {
                out.writeByte(TAG_PRIMITIVE)
                writeUtf(out, elem.content)
            }
            is JsonArray -> {
                out.writeByte(TAG_ARRAY)
                writeVarInt(out, elem.size)
                for (child in elem) writeElement(out, child, keys)
            }
            is JsonObject -> {
                out.writeByte(TAG_OBJECT)
                writeObject(out, elem, keys)
            }
        }
    }

    private fun readElement(inp: DataInputStream, keys: Array<String>): JsonElement = when (val tag = inp.readUnsignedByte()) {
        TAG_NULL -> JsonNull
        TAG_PRIMITIVE -> JsonPrimitive(readUtf(inp))
        TAG_ARRAY -> {
            val n = readVarInt(inp)
            JsonArray(List(n) { readElement(inp, keys) })
        }
        TAG_OBJECT -> readObject(inp, keys)
        else -> throw ContentFormatException("İçerik önbelleğinde bilinmeyen etiket: $tag")
    }

    private fun writeVarInt(out: DataOutputStream, value: Int) {
        var v = value
        while ((v and 0x7F.inv()) != 0) {
            out.writeByte((v and 0x7F) or 0x80)
            v = v ushr 7
        }
        out.writeByte(v)
    }

    private fun readVarInt(inp: DataInputStream): Int {
        var result = 0
        var shift = 0
        while (true) {
            val b = inp.readUnsignedByte()
            result = result or ((b and 0x7F) shl shift)
            if ((b and 0x80) == 0) return result
            shift += 7
            if (shift > 28) throw ContentFormatException("İçerik önbelleği bozuk (varint)")
        }
    }

    private fun writeUtf(out: DataOutputStream, s: String) {
        val bytes = s.toByteArray(Charsets.UTF_8)
        writeVarInt(out, bytes.size)
        out.write(bytes)
    }

    private fun readUtf(inp: DataInputStream): String {
        val len = readVarInt(inp)
        if (len == 0) return ""
        val bytes = ByteArray(len)
        inp.readFully(bytes)
        return String(bytes, Charsets.UTF_8)
    }
}

/** Derleme aracı: `content/` YAML klasörünü ikili önbelleğe (`content.bin`) derler. */
fun main(args: Array<String>) {
    require(args.size == 2) { "Kullanım: ContentCacheKt <content-klasörü> <çıktı-dosyası>" }
    val contentDir = File(args[0])
    val outFile = File(args[1])
    val bundle = ContentLoader.loadBundle(DirectoryContentSource(contentDir))
    // Önbelleğe yazmadan önce içeriğin ayrıştırılabilir olduğunu doğrula.
    ContentParser.loadTrees(bundle)
    outFile.parentFile?.mkdirs()
    val tmp = File(outFile.parentFile, "${outFile.name}.tmp")
    tmp.outputStream().use { ContentCache.write(bundle, it) }
    if (outFile.exists()) outFile.delete()
    if (!tmp.renameTo(outFile)) {
        tmp.copyTo(outFile, overwrite = true)
        tmp.delete()
    }
}
