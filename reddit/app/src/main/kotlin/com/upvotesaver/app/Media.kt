package com.upvotesaver.app

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.upvotesaver.core.MediaSink
import com.upvotesaver.core.Muxer
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer

/**
 * Saves files to shared storage through MediaStore (no storage permission needed on Android 10+).
 *
 * Normally they go to the gallery folders (Pictures/UpvoteSaver, Movies/UpvoteSaver). With
 * [Settings.hideFromGallery] they go to Download/UpvoteSaver, which holds a `.nomedia` marker so
 * gallery apps skip it; the files stay reachable with a file manager.
 */
class MediaStoreSink(private val context: Context, private val settings: Settings) : MediaSink {
    private val resolver get() = context.contentResolver
    private val images = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    private val videos = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    private val downloads = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

    private class Target(val collection: Uri, val folder: String)

    private fun target(fileName: String, hidden: Boolean): Target = when {
        hidden -> Target(downloads, HIDDEN_FOLDER)
        isVideo(fileName) -> Target(videos, "${Environment.DIRECTORY_MOVIES}/$FOLDER/")
        else -> Target(images, "${Environment.DIRECTORY_PICTURES}/$FOLDER/")
    }

    private fun isVideo(fileName: String) = fileName.substringAfterLast('.').lowercase() in VIDEO_MIME

    private fun mime(fileName: String): String {
        val ext = fileName.substringAfterLast('.').lowercase()
        return VIDEO_MIME[ext] ?: IMAGE_MIME[ext] ?: "image/jpeg"
    }

    override fun publish(file: File, fileName: String) {
        val hidden = settings.hideFromGallery
        if (hidden) ensureMarker()
        file.inputStream().use { write(target(fileName, hidden), fileName, mime(fileName), it) }
    }

    private fun write(target: Target, fileName: String, mime: String, input: InputStream) {
        // A retried post replaces its earlier file instead of piling up "name (1)" copies.
        runCatching {
            resolver.delete(
                target.collection,
                "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.RELATIVE_PATH}=?",
                arrayOf(fileName, target.folder),
            )
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, target.folder)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(target.collection, values) ?: throw IOException("Could not create $fileName")
        try {
            val out = resolver.openOutputStream(uri) ?: throw IOException("Could not write $fileName")
            out.use { input.copyTo(it) }
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        } catch (e: Throwable) {
            runCatching { resolver.delete(uri, null, null) }
            throw e
        }
    }

    /** This app's files in [folder]: content address and name. */
    private fun list(collection: Uri, folder: String): List<Pair<Uri, String>> {
        val found = mutableListOf<Pair<Uri, String>>()
        resolver.query(
            collection,
            arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME),
            "${MediaStore.MediaColumns.RELATIVE_PATH}=?",
            arrayOf(folder),
            null,
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                found += ContentUris.withAppendedId(collection, cursor.getLong(0)) to cursor.getString(1).orEmpty()
            }
        }
        return found
    }

    /** Puts the `.nomedia` marker into the hidden folder if it is not there yet. */
    private fun ensureMarker() {
        if (list(downloads, HIDDEN_FOLDER).any { it.second == MARKER }) return
        ByteArray(0).inputStream().use { write(Target(downloads, HIDDEN_FOLDER), MARKER, "application/octet-stream", it) }
    }

    /**
     * Moves the files saved so far to where the current setting wants them: into the hidden folder,
     * or back to the gallery folders. Returns how many were moved.
     */
    fun relocate(): Int {
        val hidden = settings.hideFromGallery
        val sources = if (hidden) {
            ensureMarker()
            list(images, "${Environment.DIRECTORY_PICTURES}/$FOLDER/") + list(videos, "${Environment.DIRECTORY_MOVIES}/$FOLDER/")
        } else {
            list(downloads, HIDDEN_FOLDER).filter { it.second != MARKER }
        }
        var moved = 0
        for ((uri, name) in sources) {
            if (name.isEmpty()) continue
            val input = resolver.openInputStream(uri) ?: continue
            input.use { write(target(name, hidden), name, mime(name), it) }
            resolver.delete(uri, null, null)
            moved++
        }
        // Nothing left to hide: drop the marker so the folder is an ordinary one again.
        if (!hidden) list(downloads, HIDDEN_FOLDER).filter { it.second == MARKER }.forEach { runCatching { resolver.delete(it.first, null, null) } }
        return moved
    }

    companion object {
        const val FOLDER = "UpvoteSaver"
        const val HIDDEN_FOLDER = "Download/UpvoteSaver/"
        private const val MARKER = ".nomedia"
        private val VIDEO_MIME = mapOf("mp4" to "video/mp4", "webm" to "video/webm", "mov" to "video/quicktime")
        private val IMAGE_MIME = mapOf(
            "jpg" to "image/jpeg", "jpeg" to "image/jpeg", "png" to "image/png", "gif" to "image/gif", "webp" to "image/webp",
        )
    }
}

/** Copies the picture track of one MP4 and the sound track of another into a single MP4, without re-encoding. */
object AndroidMuxer : Muxer {
    private class Source(file: File, prefix: String) {
        val extractor = MediaExtractor()
        val format: MediaFormat

        init {
            try {
                extractor.setDataSource(file.path)
                val track = (0 until extractor.trackCount).firstOrNull {
                    extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith(prefix) == true
                } ?: throw IOException("No $prefix track in ${file.name}")
                extractor.selectTrack(track)
                format = extractor.getTrackFormat(track)
            } catch (e: Throwable) {
                extractor.release()
                throw e
            }
        }
    }

    override fun mux(video: File, audio: File, out: File) {
        val sources = mutableListOf<Source>()
        var muxer: MediaMuxer? = null
        try {
            sources += Source(video, "video/")
            sources += Source(audio, "audio/")
            muxer = MediaMuxer(out.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val videoFormat = sources[0].format
            if (videoFormat.containsKey(MediaFormat.KEY_ROTATION)) {
                muxer.setOrientationHint(videoFormat.getInteger(MediaFormat.KEY_ROTATION))
            }
            val tracks = sources.map { muxer.addTrack(it.format) }
            muxer.start()
            val size = sources.maxOf {
                if (it.format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) it.format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE) else 0
            }.coerceAtLeast(4 * 1024 * 1024)
            val buffer = ByteBuffer.allocate(size)
            val info = MediaCodec.BufferInfo()
            sources.forEachIndexed { i, source ->
                while (true) {
                    val n = source.extractor.readSampleData(buffer, 0)
                    if (n < 0) break
                    val key = source.extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0
                    info.set(0, n, source.extractor.sampleTime, if (key) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0)
                    muxer.writeSampleData(tracks[i], buffer, info)
                    source.extractor.advance()
                }
            }
            muxer.stop()
        } catch (e: IllegalStateException) {
            throw IOException("could not join tracks: ${e.message}", e)
        } catch (e: IllegalArgumentException) {
            throw IOException("could not join tracks: ${e.message}", e)
        } finally {
            runCatching { muxer?.release() }
            sources.forEach { it.extractor.release() }
        }
    }
}
