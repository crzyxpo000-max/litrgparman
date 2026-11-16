package com.upvotesaver.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaResolverTest {
    private fun resolve(json: String) = MediaResolver.resolve(Json.parseToJsonElement(json).jsonObject)

    @Test
    fun `reddit video keeps the manifest and the sound flag`() {
        val refs = resolve(
            """{"url":"https://v.redd.it/abc","secure_media":{"reddit_video":{
               "fallback_url":"https://v.redd.it/abc/DASH_720.mp4?source=fallback",
               "dash_url":"https://v.redd.it/abc/DASHPlaylist.mpd?a=1","has_audio":true,"is_gif":false}}}""",
        )
        val video = refs.single() as MediaRef.RedditVideo
        assertEquals("https://v.redd.it/abc/", video.baseUrl)
        assertEquals(true, video.hasAudio)
    }

    @Test
    fun `silent reddit gif video is marked as having no sound`() {
        val video = resolve(
            """{"media":{"reddit_video":{"fallback_url":"https://v.redd.it/g1/DASH_480.mp4","is_gif":true}}}""",
        ).single() as MediaRef.RedditVideo
        assertEquals(false, video.hasAudio)
        assertEquals("https://v.redd.it/g1/DASHPlaylist.mpd", video.dashUrl)
    }

    @Test
    fun `gallery items come in gallery order with their own type`() {
        val refs = resolve(
            """{"is_gallery":true,
               "gallery_data":{"items":[{"media_id":"b"},{"media_id":"a"},{"media_id":"gone"}]},
               "media_metadata":{
                 "a":{"status":"valid","e":"AnimatedImage","m":"image/gif","s":{"gif":"https://i.redd.it/a.gif","mp4":"https://preview.redd.it/a.mp4"}},
                 "b":{"status":"valid","e":"Image","m":"image/jpg","s":{"u":"https://preview.redd.it/b.jpg?width=1&s=x"}},
                 "gone":{"status":"failed"}}}""",
        )
        assertEquals(
            listOf(MediaRef.Direct("https://preview.redd.it/b.jpg?width=1&s=x", "jpg"), MediaRef.Direct("https://i.redd.it/a.gif", "gif")),
            refs,
        )
    }

    @Test
    fun `direct image, imgur gifv and crosspost`() {
        assertEquals(listOf(MediaRef.Direct("https://i.redd.it/x.PNG", "png")), resolve("""{"url":"https://i.redd.it/x.PNG"}"""))
        assertEquals(
            listOf(MediaRef.Direct("https://i.imgur.com/q.mp4", "mp4")),
            resolve("""{"url_overridden_by_dest":"https://i.imgur.com/q.gifv"}"""),
        )
        assertEquals(
            listOf(MediaRef.Direct("https://i.redd.it/orig.jpg", "jpg")),
            resolve("""{"url":"https://reddit.com/r/a/comments/1","crosspost_parent_list":[{"url":"https://i.redd.it/orig.jpg"}]}"""),
        )
    }

    @Test
    fun `redgifs keeps the silent preview as fallback and other hosts use reddit's copy`() {
        val redgifs = resolve(
            """{"url":"https://www.redgifs.com/watch/HappyCat","preview":{"reddit_video_preview":{"fallback_url":"https://v.redd.it/p/DASH_480.mp4"}}}""",
        ).single() as MediaRef.Redgifs
        assertEquals("happycat", redgifs.id)
        assertEquals("https://v.redd.it/p/DASH_480.mp4", redgifs.fallback?.url)

        assertEquals(
            listOf(MediaRef.Direct("https://preview.redd.it/z.jpg?s=1", "jpg")),
            resolve("""{"url":"https://example.com/page","post_hint":"image","preview":{"images":[{"source":{"url":"https://preview.redd.it/z.jpg?s=1"}}]}}"""),
        )
    }

    @Test
    fun `text and link posts have nothing to save`() {
        assertTrue(resolve("""{"url":"https://www.reddit.com/r/a/comments/1/t/","is_self":true}""").isEmpty())
        assertTrue(resolve("""{"url":"https://example.com/article"}""").isEmpty())
    }

    @Test
    fun `dash manifest picks the best picture and sound`() {
        val xml = """<?xml version="1.0"?><MPD><Period>
            <AdaptationSet contentType="audio"><Representation bandwidth="66000" mimeType="audio/mp4"><BaseURL>DASH_AUDIO_64.mp4</BaseURL></Representation>
              <Representation bandwidth="129000" mimeType="audio/mp4"><BaseURL>DASH_AUDIO_128.mp4</BaseURL></Representation></AdaptationSet>
            <AdaptationSet contentType="video"><Representation bandwidth="800000" mimeType="video/mp4"><BaseURL>DASH_360.mp4</BaseURL></Representation>
              <Representation bandwidth="2400000" mimeType="video/mp4"><BaseURL>DASH_720.mp4</BaseURL></Representation></AdaptationSet>
            </Period></MPD>"""
        val streams = DashStreams.parse(xml, "https://v.redd.it/abc/")
        assertEquals("https://v.redd.it/abc/DASH_720.mp4", streams.videoUrl)
        assertEquals("https://v.redd.it/abc/DASH_AUDIO_128.mp4", streams.audioUrl)
    }

    @Test
    fun `votes csv keeps only upvotes`() {
        val csv = "id,permalink,direction\nabc123,https://www.reddit.com/r/a/comments/abc123/,up\nzzz,https://x,down\nt3_def,https://y,up\nabc123,dup,up\n"
        assertEquals(listOf("abc123", "def"), VotesCsv.upvotedIds(csv))
    }

    @Test
    fun `file names are safe`() {
        val post = Post("1ab", "What a day: 100% \"fun\" / İyi günler!", "pics", "", emptyList())
        assertEquals("pics_1ab_What_a_day_100_fun_İyi_günler", SyncEngine.fileBase(post))
    }
}
