package com.upvotesaver.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import java.io.File
import java.io.IOException

/** Where finished files go (on Android: the shared Pictures/Movies folders). */
fun interface MediaSink {
    /** Stores [file] under [fileName]; replaces a file of the same name. The caller deletes [file] afterwards. */
    fun publish(file: File, fileName: String)
}

/** Joins a picture-only and a sound-only MP4 into one file. */
fun interface Muxer {
    fun mux(video: File, audio: File, out: File)
}

data class Progress(val scanned: Int = 0, val saved: Int = 0, val failed: Int = 0, val skipped: Int = 0, val current: String = "")

/**
 * Finds upvoted posts that have not been handled yet and saves their media.
 * Every outcome is written to the [PostStore], so an interrupted run continues where it stopped.
 */
class SyncEngine(
    private val api: RedditApi,
    private val http: Http,
    private val store: PostStore,
    private val sink: MediaSink,
    private val muxer: Muxer,
    private val workDir: File,
    private val redgifs: Redgifs = Redgifs(http),
    private val now: () -> Long = System::currentTimeMillis,
    private val onProgress: (Progress) -> Unit = {},
) {
    private var progress = Progress()

    /**
     * Walks the upvoted listing from the newest post. [full] reads every page Reddit offers
     * (about 1000 posts); otherwise it stops at the first page with nothing new.
     */
    suspend fun sync(user: String, full: Boolean): Progress = withContext(Dispatchers.IO) {
        retryFailed()
        var after: String? = null
        do {
            val page = api.upvoted(user, after)
            val fresh = page.posts.filter { !store.isSettled(it.str("id").orEmpty()) }
            page.posts.forEach { if (it !in fresh) scanned() }
            fresh.forEach { handle(it) }
            after = page.after
        } while (after != null && (full || fresh.isNotEmpty()))
        progress
    }

    /** Saves the given posts (ids from a data export) that have not been handled yet. */
    suspend fun import(ids: List<String>): Progress = withContext(Dispatchers.IO) {
        val todo = ids.filter { !store.isSettled(it) }
        repeat(ids.size - todo.size) { scanned() }
        fetchByIds(todo)
        progress
    }

    private suspend fun retryFailed() = fetchByIds(store.retryable().map { it.id })

    private suspend fun fetchByIds(ids: List<String>) {
        for (batch in ids.chunked(100)) {
            val posts = api.info(batch)
            val found = posts.mapNotNull { it.str("id") }.toSet()
            posts.forEach { handle(it) }
            // Deleted posts never come back; settle them so they are not asked for again.
            (batch - found).forEach { id ->
                val old = store.get(id)
                store.put(PostRecord(id, old?.title.orEmpty(), old?.subreddit.orEmpty(), old?.permalink.orEmpty(),
                    Status.SKIPPED, note = "post no longer exists", timeMs = now()))
                progress = progress.copy(scanned = progress.scanned + 1, skipped = progress.skipped + 1)
                onProgress(progress)
            }
        }
    }

    private fun scanned() {
        progress = progress.copy(scanned = progress.scanned + 1)
        onProgress(progress)
    }

    private suspend fun handle(data: JsonObject) {
        val post = MediaResolver.post(data)
        if (post.id.isEmpty()) return
        progress = progress.copy(scanned = progress.scanned + 1, current = post.title)
        onProgress(progress)
        val attempts = (store.get(post.id)?.attempts ?: 0) + 1
        fun record(status: Status, files: Int, note: String) = store.put(
            PostRecord(post.id, post.title, post.subreddit, post.permalink, status, files, attempts, note, now()),
        )
        if (post.media.isEmpty()) {
            record(Status.SKIPPED, 0, "no media")
            progress = progress.copy(skipped = progress.skipped + 1)
            return
        }
        val base = fileBase(post)
        val notes = mutableListOf<String>()
        try {
            post.media.forEachIndexed { i, ref ->
                val name = if (post.media.size > 1) "${base}_${i + 1}" else base
                save(ref, name)?.let { notes += it }
            }
            record(Status.DONE, post.media.size, notes.distinct().joinToString("; "))
            progress = progress.copy(saved = progress.saved + 1)
        } catch (e: CancellationException) {
            throw e
        } catch (e: NotSignedInException) {
            throw e
        } catch (e: Exception) {
            record(Status.FAILED, 0, e.message ?: e.javaClass.simpleName)
            progress = progress.copy(failed = progress.failed + 1)
        }
        onProgress(progress)
    }

    /** Downloads one item and hands it to the sink. Returns a note when something was left out. */
    private suspend fun save(ref: MediaRef, name: String): String? {
        workDir.mkdirs()
        val temps = mutableListOf<File>()
        fun temp(suffix: String) = File(workDir, "$name.$suffix").also { temps += it }
        try {
            when (ref) {
                is MediaRef.Direct -> {
                    val file = temp(ref.extension)
                    http.download(ref.url, file)
                    sink.publish(file, "$name.${ref.extension}")
                    return null
                }
                is MediaRef.Redgifs -> {
                    val url = try {
                        redgifs.videoUrl(ref.id)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        val fallback = ref.fallback ?: throw IOException("Redgifs: ${e.message}")
                        val file = temp("mp4")
                        http.download(fallback.url, file)
                        sink.publish(file, "$name.mp4")
                        return "Redgifs unavailable; saved Reddit's silent preview"
                    }
                    val file = temp("mp4")
                    http.download(url, file)
                    sink.publish(file, "$name.mp4")
                    return null
                }
                is MediaRef.RedditVideo -> return saveRedditVideo(ref, name, ::temp)
            }
        } finally {
            temps.forEach { it.delete() }
        }
    }

    private suspend fun saveRedditVideo(ref: MediaRef.RedditVideo, name: String, temp: (String) -> File): String? {
        val streams = try {
            DashStreams.parse(http.text(ref.dashUrl), ref.baseUrl)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        val videoUrl = streams?.videoUrl ?: ref.fallbackUrl ?: throw IOException("No video stream found")
        val video = temp("video.mp4")
        http.download(videoUrl, video)

        val audio = temp("audio.mp4")
        val audioUrls = when {
            streams != null -> listOfNotNull(streams.audioUrl)
            ref.hasAudio == false -> emptyList()
            else -> DashStreams.AUDIO_NAMES.map { ref.baseUrl + it }
        }
        val gotAudio = audioUrls.any { url ->
            try {
                http.download(url, audio)
                true
            } catch (e: HttpException) {
                // 403/404: this video has no stream of that name.
                if (e.status != 403 && e.status != 404) throw e
                false
            }
        }
        if (!gotAudio) {
            sink.publish(video, "$name.mp4")
            return null
        }
        val joined = temp("mp4")
        try {
            muxer.mux(video, audio, joined)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            sink.publish(video, "$name.mp4")
            return "sound could not be joined (${e.message}); saved without sound"
        }
        sink.publish(joined, "$name.mp4")
        return null
    }

    companion object {
        /** `subreddit_postid_title-words`, safe as a file name on Android and Windows. */
        fun fileBase(post: Post): String {
            val title = post.title.map { if (it.isLetterOrDigit()) it else '_' }.joinToString("")
                .replace(Regex("_+"), "_").trim('_').take(50).trim('_')
            return listOf(post.subreddit, post.id, title).filter { it.isNotEmpty() }.joinToString("_")
        }
    }
}
