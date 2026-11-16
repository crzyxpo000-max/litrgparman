package com.upvotesaver.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.File

/** A Redgifs clip's best file (video with its sound, or an image) and who posted it. */
class RedgifsClip(val url: String, val user: String) {
    val extension: String
        get() = url.toHttpUrlOrNull()?.pathSegments?.lastOrNull()?.substringAfterLast('.', "")?.lowercase()
            ?.takeIf { it.isNotEmpty() && it.length <= 5 } ?: "mp4"
}

/**
 * Redgifs' public API: a temporary token, then the clip's file addresses. The HD file includes sound.
 * A token is tied to the device that asked for it (address and User-Agent), so the same [Http] must
 * be used for the lookup and the download.
 */
class Redgifs(private val http: Http, private val baseUrl: String = "https://api.redgifs.com") {
    private var token: String? = null

    suspend fun clip(id: String): RedgifsClip {
        suspend fun fetch(): String {
            val t = token ?: Json.parseToJsonElement(http.text("$baseUrl/v2/auth/temporary")).jsonObject.str("token")
                ?.also { token = it } ?: error("Redgifs gave no token")
            return http.text("$baseUrl/v2/gifs/$id") { header("Authorization", "Bearer $t") }
        }
        val body = try {
            fetch()
        } catch (e: HttpException) {
            // 401: the token expired or the device's address changed; ask for a new one once.
            if (e.status != 401) throw e
            token = null
            fetch()
        }
        val gif = Json.parseToJsonElement(body).jsonObject.obj("gif")
        val urls = gif?.obj("urls")
        val url = urls?.str("hd") ?: urls?.str("sd") ?: error("Redgifs clip $id has no file")
        return RedgifsClip(url, gif?.str("userName").orEmpty())
    }

    suspend fun videoUrl(id: String): String = clip(id).url
}

/** Finds Redgifs clip ids in shared or pasted text. */
object RedgifsLinks {
    private val URL = Regex("""https?://[^\s<>"']+""", RegexOption.IGNORE_CASE)
    private val PAGE_PATHS = setOf("watch", "ifr", "i")

    /** Clip ids (lowercase) of every Redgifs address in [text], in order, without repeats. */
    fun ids(text: String): List<String> = URL.findAll(text).mapNotNull { id(it.value.trimEnd('.', ',', ')', ']')) }.distinct().toList()

    private fun id(url: String): String? {
        val parsed = url.toHttpUrlOrNull() ?: return null
        if (parsed.host != "redgifs.com" && !parsed.host.endsWith(".redgifs.com")) return null
        val segments = parsed.pathSegments.filter { it.isNotBlank() }
        // Page links: /watch/<id>, /ifr/<id>, /i/<id>. File links: <Name>-mobile.mp4, <Name>.jpg.
        val raw = when {
            segments.size >= 2 && segments[segments.size - 2] in PAGE_PATHS -> segments.last()
            segments.size == 1 && '.' in segments[0] -> segments[0]
            else -> return null
        }
        return raw.substringBefore('.').substringBefore('-').lowercase().takeIf { it.matches(Regex("[a-z0-9]+")) }
    }
}

/** Saves Redgifs clips by id, recording each outcome in the [PostStore] under `rg_<id>`. */
class RedgifsSaver(
    private val http: Http,
    private val store: PostStore,
    private val sink: MediaSink,
    private val workDir: File,
    private val redgifs: Redgifs = Redgifs(http),
    private val now: () -> Long = System::currentTimeMillis,
    private val onProgress: (Progress) -> Unit = {},
) {
    suspend fun save(ids: List<String>): Progress = withContext(Dispatchers.IO) {
        var progress = Progress()
        for (id in ids) {
            val key = "rg_$id"
            val link = "https://www.redgifs.com/watch/$id"
            val attempts = (store.get(key)?.attempts ?: 0) + 1
            progress = progress.copy(scanned = progress.scanned + 1, current = id)
            onProgress(progress)
            workDir.mkdirs()
            var temp: File? = null
            try {
                val clip = redgifs.clip(id)
                val name = listOf("redgifs", clip.user, id).filter { it.isNotEmpty() }.joinToString("_") + "." + clip.extension
                temp = File(workDir, name)
                http.download(clip.url, temp)
                sink.publish(temp, name)
                store.put(PostRecord(key, clip.user.ifEmpty { id }, "", link, Status.DONE, 1, attempts, "Redgifs", now()))
                progress = progress.copy(saved = progress.saved + 1)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val note = if (e is HttpException && (e.status == 404 || e.status == 410)) "clip no longer exists" else (e.message ?: e.javaClass.simpleName)
                store.put(PostRecord(key, store.get(key)?.title ?: id, "", link, Status.FAILED, 0, attempts, note, now()))
                progress = progress.copy(failed = progress.failed + 1)
            } finally {
                temp?.delete()
            }
            onProgress(progress)
        }
        progress
    }
}
