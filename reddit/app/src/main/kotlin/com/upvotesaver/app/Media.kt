package com.upvotesaver.app

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Environment
import android.provider.MediaStore
import com.upvotesaver.core.MediaSink
import com.upvotesaver.core.Muxer
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer

/** Saves files to the shared gallery folders through MediaStore (no storage permission needed on Android 10+). */
class MediaStoreSink(private val context: Context) : MediaSink {
    override fun publish(file: File, fileName: String) {
        val ext = fileName.substringAfterLast('.').lowercase()
        val video = ext in VIDEO_MIME
        val collection =
            if (video) MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            else MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val folder = (if (video) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES) + "/$FOLDER/"
        val resolver = context.contentResolver
        // A retried post replaces its earlier file instead of piling up "name (1)" copies.
        runCatching {
            resolver.delete(
                collection,
                "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.RELATIVE_PATH}=?",
                arrayOf(fileName, folder),
            )
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, VIDEO_MIME[ext] ?: IMAGE_MIME[ext] ?: "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, folder)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values) ?: throw IOException("Could not create $fileName")
        try {
            val out = resolver.openOutputStream(uri) ?: throw IOException("Could not write $fileName")
            out.use { file.inputStream().use { input -> input.copyTo(it) } }
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        } catch (e: Throwable) {
            runCatching { resolver.delete(uri, null, null) }
            throw e
        }
    }

    companion object {
        const val FOLDER = "UpvoteSaver"
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
