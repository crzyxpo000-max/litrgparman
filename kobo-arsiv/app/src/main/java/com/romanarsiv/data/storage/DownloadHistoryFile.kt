package com.romanarsiv.data.storage

import com.romanarsiv.background.DownloadHistory
import com.romanarsiv.background.DownloadStatus
import com.romanarsiv.background.DownloadTask
import com.romanarsiv.domain.model.ArchiveResult
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Biten indirmelerin listesi (İndirilenler sekmesi), uygulamanın özel klasöründe bir JSON dosyası.
 * Yalnızca özet tutulur (ad, konum, içerik adresi); EPUB'ın kendisi İndirilenler klasöründedir.
 */
class DownloadHistoryFile(private val file: File) : DownloadHistory {

    override fun load(): List<DownloadTask> = runCatching {
        if (!file.isFile) return emptyList()
        val array = JSONArray(file.readText())
        (0 until array.length()).mapNotNull { i -> array.optJSONObject(i)?.let(::fromJson) }
    }.getOrDefault(emptyList())

    override fun save(tasks: List<DownloadTask>) {
        val array = JSONArray()
        tasks.filter { it.status == DownloadStatus.DONE && it.result != null }.take(MAX_ENTRIES).forEach { array.put(toJson(it)) }
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(array.toString())
        if (!tmp.renameTo(file)) {
            file.writeText(array.toString())
            tmp.delete()
        }
    }

    private fun toJson(task: DownloadTask): JSONObject {
        val r = task.result!!
        return JSONObject()
            .put("id", task.id)
            .put("title", task.bookTitle)
            .put("site", task.siteName)
            .put("requested", task.requestedChapters)
            .put("created", task.createdAt)
            .put("finished", task.finishedAt ?: 0L)
            .put("fileName", r.fileName)
            .put("location", r.location)
            .put("chapters", r.chapterCount)
            .put("skipped", r.skippedChapters)
            .put("uri", r.uri ?: "")
    }

    private fun fromJson(o: JSONObject): DownloadTask? {
        val id = o.optLong("id", -1L).takeIf { it > 0 } ?: return null
        return DownloadTask(
            id = id,
            bookTitle = o.optString("title"),
            siteName = o.optString("site"),
            requestedChapters = o.optInt("requested"),
            status = DownloadStatus.DONE,
            createdAt = o.optLong("created"),
            finishedAt = o.optLong("finished").takeIf { it > 0 },
            result = ArchiveResult(
                fileName = o.optString("fileName"),
                location = o.optString("location"),
                chapterCount = o.optInt("chapters"),
                skippedChapters = o.optInt("skipped"),
                uri = o.optString("uri").takeIf { it.isNotBlank() },
            ),
        )
    }

    companion object {
        const val MAX_ENTRIES = 200
    }
}
