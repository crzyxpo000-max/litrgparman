package com.upvotesaver.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.File
import java.io.IOException

/** One Redgifs clip: its best file (video with its sound, or an image) and who posted it. */
data class RedgifsGif(
    val id: String,
    val user: String,
    val url: String,
    val thumbnail: String = "",
    val likes: Int = 0,
    val durationSeconds: Double = 0.0,
    val hasAudio: Boolean = false,
) {
    val extension: String
        get() = url.toHttpUrlOrNull()?.pathSegments?.lastOrNull()?.substringAfterLast('.', "")?.lowercase()
            ?.takeIf { it.isNotEmpty() && it.length <= 5 } ?: "mp4"
}

class RedgifsPage(val gifs: List<RedgifsGif>, val page: Int, val pages: Int) {
    val hasMore: Boolean get() = gifs.isNotEmpty() && page < pages
}

data class RedgifsNiche(val id: String, val name: String, val gifs: Int, val subscribers: Int, val thumbnail: String)

/** The Redgifs sign-in is missing or no longer accepted. */
class RedgifsSignedOutException : IOException("Not signed in to Redgifs.")

/**
 * Redgifs' API. Public data is read with a temporary token; the signed-in user's likes need the
 * user's own token ([userToken]). A token is tied to the device that asked for it (address and
 * User-Agent), so the same [Http] must be used for lookups and downloads.
 */
class Redgifs(
    private val http: Http,
    private val baseUrl: String = "https://api.redgifs.com",
    private val userToken: () -> String? = { null },
    /** Called when Redgifs answers 401 to the user's token. */
    private val onUserTokenRejected: () -> Unit = {},
) {
    private var tempToken: String? = null

    suspend fun clip(id: String): RedgifsGif =
        gif(get("/v2/gifs/$id").obj("gif") ?: error("Redgifs clip $id not found"), id) ?: error("Redgifs clip $id has no file")

    suspend fun videoUrl(id: String): String = clip(id).url

    /** The signed-in user's name. */
    suspend fun me(): String {
        val json = get("/v1/me", needsUser = true)
        return json.str("username") ?: json.obj("user")?.str("username") ?: json.str("name") ?: ""
    }

    /** One page of the clips the signed-in user liked, newest first. */
    suspend fun liked(page: Int, count: Int = 40): RedgifsPage =
        page(get(url("/v2/feeds/liked", "page" to page, "count" to count), needsUser = true), page)

    /** Niches matching [query]; a blank query lists the most followed ones. */
    suspend fun niches(query: String, page: Int = 1, count: Int = 30): List<RedgifsNiche> {
        val json = get(url("/v2/niches/search", "order" to if (query.isBlank()) "subscribers" else "best_match",
            "page" to page, "count" to count, "query" to query.trim().ifEmpty { null }))
        return json.arr("niches").orEmpty().mapNotNull { it as? JsonObject }.mapNotNull { n ->
            val id = n.str("id") ?: return@mapNotNull null
            RedgifsNiche(id, n.str("name") ?: id, n.int("gifs"), n.int("subscribers"), n.str("thumbnail").orEmpty())
        }
    }

    /** One page of a niche's clips in the given [order] (see [NICHE_ORDERS]). */
    suspend fun nicheGifs(id: String, order: String, page: Int, count: Int = 40): RedgifsPage =
        page(get(url("/v2/niches/$id/gifs", "count" to count, "page" to page, "order" to order)), page)

    private fun url(path: String, vararg params: Pair<String, Any?>): String =
        (baseUrl + path).toHttpUrl().newBuilder()
            .apply { params.forEach { (k, v) -> if (v != null) addQueryParameter(k, v.toString()) } }
            .build().toString().removePrefix(baseUrl)

    private fun page(json: JsonObject, asked: Int): RedgifsPage = RedgifsPage(
        gifs = json.arr("gifs").orEmpty().mapNotNull { (it as? JsonObject)?.let { g -> gif(g) } },
        page = json.int("page").takeIf { it > 0 } ?: asked,
        pages = json.int("pages"),
    )

    private fun gif(g: JsonObject, knownId: String? = null): RedgifsGif? {
        val urls = g.obj("urls")
        return RedgifsGif(
            id = g.str("id")?.lowercase() ?: knownId ?: return null,
            user = g.str("userName").orEmpty(),
            url = urls?.str("hd") ?: urls?.str("sd") ?: return null,
            thumbnail = urls?.str("thumbnail") ?: urls?.str("poster").orEmpty(),
            likes = g.int("likes"),
            durationSeconds = (g["duration"] as? JsonPrimitive)?.doubleOrNull ?: 0.0,
            hasAudio = g.bool("hasAudio") ?: false,
        )
    }

    private fun JsonObject.int(key: String): Int = (this[key] as? JsonPrimitive)?.intOrNull ?: 0

    private suspend fun get(path: String, needsUser: Boolean = false): JsonObject {
        suspend fun fetch(token: String) =
            Json.parseToJsonElement(http.text(baseUrl + path) { header("Authorization", "Bearer $token") }).jsonObject

        val user = userToken()
        if (user != null) {
            try {
                return fetch(user)
            } catch (e: HttpException) {
                if (e.status != 401) throw e
                onUserTokenRejected()
            }
        }
        if (needsUser) throw RedgifsSignedOutException()
        suspend fun temp(): String = tempToken
            ?: Json.parseToJsonElement(http.text("$baseUrl/v2/auth/temporary")).jsonObject.str("token")?.also { tempToken = it }
            ?: error("Redgifs gave no token")
        return try {
            fetch(temp())
        } catch (e: HttpException) {
            // 401: the token expired or the device's address changed; ask for a new one once.
            if (e.status != 401) throw e
            tempToken = null
            fetch(temp())
        }
    }

    companion object {
        /** Sort orders of a niche's clips; "best" is the most liked. */
        val NICHE_ORDERS = listOf("best", "hot", "latest")
    }
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

/** Saves Redgifs clips, recording each outcome in the [PostStore] under `rg_<id>`. */
class RedgifsSaver(
    private val http: Http,
    private val store: PostStore,
    private val sink: MediaSink,
    private val workDir: File,
    private val redgifs: Redgifs = Redgifs(http),
    private val now: () -> Long = System::currentTimeMillis,
    private val onProgress: (Progress) -> Unit = {},
) {
    private var progress = Progress()

    /** Saves the clips with these ids (from shared links or picked in a niche). */
    suspend fun save(ids: List<String>): Progress = withContext(Dispatchers.IO) {
        progress = Progress()
        for (id in ids) {
            // A clip that was already saved is not downloaded again.
            if (store.get(key(id))?.status == Status.DONE) {
                progress = progress.copy(scanned = progress.scanned + 1)
                onProgress(progress)
            } else {
                saveOne(id) { redgifs.clip(id) }
            }
        }
        progress
    }

    /**
     * Saves the signed-in user's liked clips that have not been handled yet, newest first.
     * [full] reads every page; otherwise it stops at the first page with nothing new.
     */
    suspend fun syncLikes(full: Boolean): Progress = withContext(Dispatchers.IO) {
        progress = Progress()
        var page = 1
        while (true) {
            val result = redgifs.liked(page)
            val fresh = result.gifs.filter { !store.isSettled(key(it.id)) }
            progress = progress.copy(scanned = progress.scanned + result.gifs.size - fresh.size)
            onProgress(progress)
            for (gif in fresh) saveOne(gif.id) { gif }
            if (!result.hasMore || (!full && fresh.isEmpty())) break
            page++
        }
        progress
    }

    private fun key(id: String) = "rg_$id"

    private suspend fun saveOne(id: String, resolve: suspend () -> RedgifsGif) {
        val key = key(id)
        val link = "https://www.redgifs.com/watch/$id"
        val attempts = (store.get(key)?.attempts ?: 0) + 1
        progress = progress.copy(scanned = progress.scanned + 1, current = id)
        onProgress(progress)
        workDir.mkdirs()
        var temp: File? = null
        try {
            val clip = resolve()
            val name = listOf("redgifs", clip.user, id).filter { it.isNotEmpty() }.joinToString("_") + "." + clip.extension
            temp = File(workDir, name)
            http.download(clip.url, temp)
            sink.publish(temp, name)
            store.put(PostRecord(key, clip.user.ifEmpty { id }, "", link, Status.DONE, 1, attempts, "Redgifs", now()))
            progress = progress.copy(saved = progress.saved + 1)
        } catch (e: CancellationException) {
            throw e
        } catch (e: RedgifsSignedOutException) {
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
}
