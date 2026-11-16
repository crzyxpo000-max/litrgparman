package com.upvotesaver.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import java.io.File

enum class Status {
    /** All media saved. */
    DONE,
    /** Download failed; retried on later runs up to [PostStore.MAX_ATTEMPTS] times. */
    FAILED,
    /** Nothing to save: a text or link post, or a host the app cannot read. */
    SKIPPED,
}

data class PostRecord(
    val id: String,
    val title: String,
    val subreddit: String,
    val permalink: String,
    val status: Status,
    val files: Int = 0,
    val attempts: Int = 1,
    val note: String = "",
    val timeMs: Long = 0,
)

/**
 * What happened to each upvoted post, kept as an append-only JSON-lines file (the last line for an
 * id wins). It is how the app knows which posts are new.
 */
class PostStore(private val file: File) {
    private val records = LinkedHashMap<String, PostRecord>()

    init {
        if (file.exists()) file.forEachLine { line ->
            runCatching { decode(Json.parseToJsonElement(line).jsonObject) }.getOrNull()?.let { add(it) }
        }
    }

    @Synchronized
    fun get(id: String): PostRecord? = records[id]

    /** True when the post needs no more work. */
    @Synchronized
    fun isSettled(id: String): Boolean {
        val r = records[id] ?: return false
        return r.status != Status.FAILED || r.attempts >= MAX_ATTEMPTS
    }

    @Synchronized
    fun put(record: PostRecord) {
        add(record)
        file.parentFile?.mkdirs()
        file.appendText(encode(record).toString() + "\n")
    }

    /** Failed posts that still have attempts left. */
    @Synchronized
    fun retryable(): List<PostRecord> = records.values.filter { it.status == Status.FAILED && it.attempts < MAX_ATTEMPTS }

    /** Most recently handled first. */
    @Synchronized
    fun recent(limit: Int): List<PostRecord> = records.values.toList().takeLast(limit).reversed()

    @Synchronized
    fun counts(): Map<Status, Int> = records.values.groupingBy { it.status }.eachCount()

    @Synchronized
    fun clear() {
        records.clear()
        file.delete()
    }

    private fun add(record: PostRecord) {
        // Re-insert so iteration order is "last handled".
        records.remove(record.id)
        records[record.id] = record
    }

    private fun encode(r: PostRecord): JsonObject = buildJsonObject {
        put("id", r.id); put("title", r.title); put("sub", r.subreddit); put("link", r.permalink)
        put("status", r.status.name); put("files", r.files); put("attempts", r.attempts)
        put("note", r.note); put("time", r.timeMs)
    }

    private fun decode(o: JsonObject) = PostRecord(
        id = o.str("id")!!,
        title = o.str("title").orEmpty(),
        subreddit = o.str("sub").orEmpty(),
        permalink = o.str("link").orEmpty(),
        status = Status.valueOf(o.str("status")!!),
        files = o["files"]?.jsonPrimitive?.int ?: 0,
        attempts = o["attempts"]?.jsonPrimitive?.int ?: 1,
        note = o.str("note").orEmpty(),
        timeMs = o["time"]?.jsonPrimitive?.long ?: 0,
    )

    companion object {
        const val MAX_ATTEMPTS = 3
    }
}
