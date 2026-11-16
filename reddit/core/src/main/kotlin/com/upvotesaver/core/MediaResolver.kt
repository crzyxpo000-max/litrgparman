package com.upvotesaver.core

import kotlinx.serialization.json.JsonObject
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

/** Something a post points to that can be saved. */
sealed interface MediaRef {
    /** A file that is complete as served. */
    data class Direct(val url: String, val extension: String) : MediaRef

    /**
     * A v.redd.it video. Reddit serves picture and sound as separate streams listed in the DASH
     * manifest; [fallbackUrl] is the picture-only MP4 when the post names one.
     */
    data class RedditVideo(val dashUrl: String, val fallbackUrl: String?, val hasAudio: Boolean?) : MediaRef {
        /** `https://v.redd.it/<id>/` */
        val baseUrl: String get() = dashUrl.substringBefore('?').substringBeforeLast('/') + "/"
    }

    /** A Redgifs clip (sound included), looked up through its API; [fallback] is Reddit's silent preview. */
    data class Redgifs(val id: String, val fallback: Direct?) : MediaRef
}

class Post(
    val id: String,
    val title: String,
    val subreddit: String,
    val permalink: String,
    val media: List<MediaRef>,
)

object MediaResolver {
    private val IMAGE_EXT = setOf("jpg", "jpeg", "png", "gif", "webp")
    private val VIDEO_EXT = setOf("mp4", "webm", "mov")

    fun post(data: JsonObject): Post = Post(
        id = data.str("id").orEmpty(),
        title = data.str("title").orEmpty(),
        subreddit = data.str("subreddit").orEmpty(),
        permalink = data.str("permalink").orEmpty(),
        media = resolve(data),
    )

    fun resolve(data: JsonObject): List<MediaRef> {
        // A crosspost carries its media on the original post.
        val source = data.arr("crosspost_parent_list")?.firstOrNull() as? JsonObject
        if (source != null) resolve(source).takeIf { it.isNotEmpty() }?.let { return it }

        redditVideo(data)?.let { return listOf(it) }
        if (data.bool("is_gallery") == true || data.obj("gallery_data") != null) {
            gallery(data).takeIf { it.isNotEmpty() }?.let { return it }
        }

        val url = data.str("url_overridden_by_dest") ?: data.str("url") ?: return emptyList()
        val parsed = url.toHttpUrlOrNull() ?: return emptyList()
        val host = parsed.host.removePrefix("www.")
        val ext = extension(url)
        val preview = previewVideo(data)

        if (host.endsWith("redgifs.com")) {
            val id = parsed.pathSegments.lastOrNull { it.isNotBlank() }?.substringBefore('.')?.substringBefore('-')?.lowercase()
            if (!id.isNullOrBlank()) return listOf(MediaRef.Redgifs(id, preview))
        }
        if (host.endsWith("imgur.com") && ext == "gifv") {
            return listOf(MediaRef.Direct(url.substringBeforeLast('.') + ".mp4", "mp4"))
        }
        if (ext in IMAGE_EXT || ext in VIDEO_EXT) return listOf(MediaRef.Direct(url, ext!!))
        // Other hosts: Reddit's own copy, a silent MP4 for animations or the full-size preview image.
        if (preview != null) return listOf(preview)
        if (data.str("post_hint") == "image") {
            val image = (data.obj("preview")?.arr("images")?.firstOrNull() as? JsonObject)?.obj("source")?.str("url")
            if (image != null) return listOf(MediaRef.Direct(image, extension(image) ?: "jpg"))
        }
        return emptyList()
    }

    private fun redditVideo(data: JsonObject): MediaRef.RedditVideo? {
        val video = data.obj("secure_media")?.obj("reddit_video") ?: data.obj("media")?.obj("reddit_video") ?: return null
        val fallback = video.str("fallback_url")
        val dash = video.str("dash_url") ?: fallback?.let { it.substringBeforeLast('/') + "/DASHPlaylist.mpd" } ?: return null
        // "is_gif" videos are silent loops.
        val hasAudio = if (video.bool("is_gif") == true) false else video.bool("has_audio")
        return MediaRef.RedditVideo(dash, fallback, hasAudio)
    }

    private fun gallery(data: JsonObject): List<MediaRef> {
        val metadata = data.obj("media_metadata") ?: return emptyList()
        val order = data.obj("gallery_data")?.arr("items")?.mapNotNull { (it as? JsonObject)?.str("media_id") }
            ?: metadata.keys.toList()
        return order.mapNotNull { id ->
            val item = metadata.obj(id) ?: return@mapNotNull null
            if (item.str("status") != "valid") return@mapNotNull null
            item.str("dashUrl")?.let { return@mapNotNull MediaRef.RedditVideo(it, null, null) }
            val s = item.obj("s") ?: return@mapNotNull null
            s.str("u")?.let { MediaRef.Direct(it, extension(it) ?: mimeExtension(item.str("m")) ?: "jpg") }
                ?: s.str("gif")?.let { MediaRef.Direct(it, "gif") }
                ?: s.str("mp4")?.let { MediaRef.Direct(it, "mp4") }
        }
    }

    private fun previewVideo(data: JsonObject): MediaRef.Direct? =
        data.obj("preview")?.obj("reddit_video_preview")?.str("fallback_url")?.let { MediaRef.Direct(it, "mp4") }

    private fun extension(url: String): String? =
        url.toHttpUrlOrNull()?.pathSegments?.lastOrNull()?.substringAfterLast('.', "")?.lowercase()
            ?.takeIf { it.isNotEmpty() && it.length <= 5 }

    private fun mimeExtension(mime: String?): String? = when (mime?.substringAfter('/')) {
        "jpg", "jpeg" -> "jpg"
        "png" -> "png"
        "gif" -> "gif"
        "webp" -> "webp"
        else -> null
    }
}

/** The best picture and sound streams of a v.redd.it DASH manifest. */
class DashStreams(val videoUrl: String?, val audioUrl: String?) {
    companion object {
        /** Names Reddit has used for the sound stream, tried when the manifest cannot be read. */
        val AUDIO_NAMES = listOf("DASH_AUDIO_128.mp4", "DASH_AUDIO_64.mp4", "DASH_audio.mp4", "audio")

        fun parse(xml: String, baseUrl: String): DashStreams {
            val factory = DocumentBuilderFactory.newInstance()
            runCatching { factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            val doc = factory.newDocumentBuilder().parse(xml.byteInputStream())
            var video: Pair<Long, String>? = null
            var audio: Pair<Long, String>? = null
            val sets = doc.getElementsByTagName("AdaptationSet")
            for (i in 0 until sets.length) {
                val set = sets.item(i) as Element
                val reps = set.getElementsByTagName("Representation")
                for (j in 0 until reps.length) {
                    val rep = reps.item(j) as Element
                    val type = set.getAttribute("contentType") + set.getAttribute("mimeType") + rep.getAttribute("mimeType")
                    val name = rep.getElementsByTagName("BaseURL").item(0)?.textContent?.trim().orEmpty()
                    if (name.isEmpty()) continue
                    val url = if (name.startsWith("http")) name else baseUrl + name
                    val candidate = (rep.getAttribute("bandwidth").toLongOrNull() ?: 0) to url
                    if ("audio" in type) {
                        if (audio == null || candidate.first > audio.first) audio = candidate
                    } else if ("video" in type) {
                        if (video == null || candidate.first > video.first) video = candidate
                    }
                }
            }
            return DashStreams(video?.second, audio?.second)
        }
    }
}
