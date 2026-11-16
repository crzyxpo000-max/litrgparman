package com.upvotesaver.core

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SyncEngineTest {
    @get:Rule val tmp = TemporaryFolder()
    private val server = MockWebServer()
    private val routes = mutableMapOf<String, (RecordedRequest) -> MockResponse>()
    private val hits = mutableListOf<String>()
    private val saved = mutableMapOf<String, String>()
    private var tokens: Tokens? = Tokens("good", "refresh", Long.MAX_VALUE)
    private val tokenStore = object : TokenStore {
        override fun load() = tokens
        override fun save(tokens: Tokens?) { this@SyncEngineTest.tokens = tokens }
    }
    private lateinit var store: PostStore
    private lateinit var engine: SyncEngine

    private fun url(path: String) = server.url(path).toString()
    private fun ok(body: String) = MockResponse().setBody(body)
    private fun route(path: String, body: String) { routes[path] = { ok(body) } }
    private fun listing(after: String?, vararg posts: String) =
        """{"data":{"after":${after?.let { "\"$it\"" }},"children":[${posts.joinToString(",") { """{"kind":"t3","data":$it}""" }}]}}"""
    private fun image(id: String) = """{"id":"$id","title":"T $id","subreddit":"pics","url":"${url("/img/$id.jpg")}"}"""

    @Before
    fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path!!.replace("&raw_json=1", "").replace("?raw_json=1", "")
                hits += path
                if (path.startsWith("/img/")) return ok("image:$path")
                return routes[path]?.invoke(request) ?: MockResponse().setResponseCode(404)
            }
        }
        server.start()
        val http = Http(userAgent = { "test" }, baseBackoffMs = 1)
        val auth = RedditAuth(http, "client", "upvotesaver://auth", baseUrl = url("/www").trimEnd('/'))
        store = PostStore(File(tmp.root, "posts.jsonl"))
        engine = SyncEngine(
            api = RedditApi(http, Session(auth, tokenStore), baseUrl = url("/api").trimEnd('/')),
            http = http,
            store = store,
            sink = { file, name -> saved[name] = file.readText() },
            muxer = { video, audio, out -> out.writeText(video.readText() + "+" + audio.readText()) },
            workDir = tmp.newFolder("work"),
            redgifs = Redgifs(http, url("/redgifs").trimEnd('/')),
        )
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun `full sync walks every page and joins video with sound`() = runBlocking {
        val video = """{"id":"v1","title":"Clip","subreddit":"videos","media":{"reddit_video":{
            "fallback_url":"${url("/v/v1/DASH_720.mp4")}","dash_url":"${url("/v/v1/DASHPlaylist.mpd")}","has_audio":true}}}"""
        route("/api/user/me/upvoted?limit=100", listing("t3_p2", image("p1"), video))
        route("/api/user/me/upvoted?limit=100&after=t3_p2", listing(null, """{"id":"s1","title":"Text","subreddit":"ask","is_self":true,"url":"https://www.reddit.com/x"}"""))
        route("/v/v1/DASHPlaylist.mpd", """<MPD><Period>
            <AdaptationSet contentType="video"><Representation bandwidth="1"><BaseURL>DASH_720.mp4</BaseURL></Representation></AdaptationSet>
            <AdaptationSet contentType="audio"><Representation bandwidth="1"><BaseURL>DASH_AUDIO_128.mp4</BaseURL></Representation></AdaptationSet>
            </Period></MPD>""")
        route("/v/v1/DASH_720.mp4", "PICTURE")
        route("/v/v1/DASH_AUDIO_128.mp4", "SOUND")

        val result = engine.sync("me", full = true)

        assertEquals(Progress(scanned = 3, saved = 2, skipped = 1, current = "Text"), result)
        assertEquals("image:/img/p1.jpg", saved["pics_p1_T_p1.jpg"])
        assertEquals("PICTURE+SOUND", saved["videos_v1_Clip.mp4"])
        assertEquals(Status.SKIPPED, store.get("s1")?.status)
        assertTrue(File(tmp.root, "work").list()!!.isEmpty())
    }

    @Test
    fun `video without a manifest tries the known sound names and silent video is saved as is`() = runBlocking {
        val video = """{"id":"v2","title":"Old","subreddit":"v","media":{"reddit_video":{"fallback_url":"${url("/v/v2/DASH_480.mp4")}"}}}"""
        val silent = """{"id":"v3","title":"Mute","subreddit":"v","media":{"reddit_video":{"fallback_url":"${url("/v/v3/DASH_480.mp4")}"}}}"""
        route("/api/user/me/upvoted?limit=100", listing(null, video, silent))
        route("/v/v2/DASH_480.mp4", "P2")
        route("/v/v2/audio", "S2")
        route("/v/v3/DASH_480.mp4", "P3")

        engine.sync("me", full = false)

        assertEquals("P2+S2", saved["v_v2_Old.mp4"])
        assertEquals("P3", saved["v_v3_Mute.mp4"])
        assertEquals(Status.DONE, store.get("v3")?.status)
    }

    @Test
    fun `incremental sync stops at the first page with nothing new`() = runBlocking {
        route("/api/user/me/upvoted?limit=100", listing("t3_a", image("a")))
        route("/api/user/me/upvoted?limit=100&after=t3_a", listing("t3_b", image("b")))
        route("/api/user/me/upvoted?limit=100&after=t3_b", listing(null, image("c")))
        engine.sync("me", full = true)
        assertEquals(3, saved.size)

        hits.clear(); saved.clear()
        route("/api/user/me/upvoted?limit=100", listing("t3_a", image("new"), image("a")))
        route("/api/user/me/upvoted?limit=100&after=t3_a", listing("t3_b", image("b")))
        engine.sync("me", full = false)

        assertEquals(setOf("pics_new_T_new.jpg"), saved.keys)
        assertTrue("/api/user/me/upvoted?limit=100&after=t3_b" !in hits)
    }

    @Test
    fun `failed downloads are retried on later runs and then given up`() = runBlocking {
        val broken = """{"id":"x","title":"Gone","subreddit":"pics","url":"${url("/missing/x.jpg")}"}"""
        route("/api/user/me/upvoted?limit=100", listing(null, broken))
        route("/api/api/info?id=t3_x", listing(null, broken))

        engine.sync("me", full = false)
        assertEquals(Status.FAILED, store.get("x")?.status)
        assertEquals(1, store.get("x")?.attempts)
        engine.sync("me", full = false)
        engine.sync("me", full = false)
        assertEquals(PostStore.MAX_ATTEMPTS, store.get("x")?.attempts)
        assertTrue(store.isSettled("x"))

        // The record survives a restart.
        assertEquals(Status.FAILED, PostStore(File(tmp.root, "posts.jsonl")).get("x")?.status)
    }

    @Test
    fun `import fetches posts by id and settles deleted ones`() = runBlocking {
        route("/api/api/info?id=t3_i1%2Ct3_dead", listing(null, image("i1")))
        val result = engine.import(listOf("i1", "dead"))
        assertEquals(1, result.saved)
        assertEquals(Status.SKIPPED, store.get("dead")?.status)
        hits.clear()
        engine.import(listOf("i1", "dead"))
        assertTrue(hits.isEmpty())
    }

    @Test
    fun `expired token is refreshed once`() = runBlocking {
        routes["/api/user/me/upvoted?limit=100"] = { request ->
            if (request.getHeader("Authorization") == "bearer fresh") ok(listing(null)) else MockResponse().setResponseCode(401)
        }
        routes["/www/api/v1/access_token"] = { request ->
            assertEquals("grant_type=refresh_token&refresh_token=refresh", request.body.readUtf8())
            ok("""{"access_token":"fresh","expires_in":3600}""")
        }
        engine.sync("me", full = false)
        assertEquals("fresh", tokens?.access)
        assertEquals("refresh", tokens?.refresh)
    }

    @Test
    fun `redgifs clip comes from its api`() = runBlocking {
        route("/api/user/me/upvoted?limit=100", listing(null, """{"id":"r1","title":"R","subreddit":"g","url":"https://redgifs.com/watch/abc"}"""))
        route("/redgifs/v2/auth/temporary", """{"token":"tok"}""")
        route("/redgifs/v2/gifs/abc", """{"gif":{"urls":{"hd":"${url("/img/abc.mp4")}"}}}""")
        engine.sync("me", full = false)
        assertEquals("image:/img/abc.mp4", saved["g_r1_R.mp4"])
        assertNull(store.get("r1")?.note?.takeIf { it.isNotEmpty() })
    }

    @Test
    fun `redgifs links are found in shared text`() {
        val text = "look https://www.redgifs.com/watch/HappyCat?rel=x and https://v3.redgifs.com/ifr/sadDog, " +
            "https://media.redgifs.com/BigBird-mobile.mp4 https://i.redgifs.com/i/tinyfox.jpg https://redgifs.com/watch/happycat " +
            "https://www.reddit.com/r/a/s/xyz https://notredgifs.com/watch/nope https://www.redgifs.com/users/someone"
        assertEquals(listOf("happycat", "saddog", "bigbird", "tinyfox"), RedgifsLinks.ids(text))
    }

    @Test
    fun `shared redgifs clip is saved under the poster's name and a missing one is recorded`() = runBlocking {
        route("/redgifs/v2/auth/temporary", """{"token":"tok"}""")
        route("/redgifs/v2/gifs/abc", """{"gif":{"userName":"maker","urls":{"sd":"${url("/img/abc-mobile.mp4")}","hd":"${url("/img/abc.mp4")}"}}}""")
        val http = Http(userAgent = { "test" }, baseBackoffMs = 1)
        val saver = RedgifsSaver(http, store, { file, name -> saved[name] = file.readText() }, File(tmp.root, "work"),
            Redgifs(http, url("/redgifs").trimEnd('/')))

        val result = saver.save(listOf("abc", "gone"))

        assertEquals(1, result.saved)
        assertEquals(1, result.failed)
        assertEquals("image:/img/abc.mp4", saved["redgifs_maker_abc.mp4"])
        assertEquals("https://www.redgifs.com/watch/abc", store.get("rg_abc")?.permalink)
        assertEquals("clip no longer exists", store.get("rg_gone")?.note)

        // Asking for the same clip again does not download it a second time.
        saved.clear(); hits.clear()
        val again = saver.save(listOf("abc"))
        assertEquals(0, again.saved)
        assertTrue(saved.isEmpty() && hits.isEmpty())
        assertTrue(File(tmp.root, "work").list()!!.isEmpty())
    }

    private fun rgGif(id: String) = """{"id":"$id","userName":"u","likes":7,"duration":3.5,"hasAudio":true,"urls":{"hd":"${url("/img/$id.mp4")}","thumbnail":"${url("/img/$id.jpg")}"}}"""

    @Test
    fun `liked clips are read with the user's token and only new ones are saved`() = runBlocking {
        var userToken: String? = "user-token"
        val seen = mutableListOf<String?>()
        routes["/redgifs/v2/feeds/liked?page=1&count=40"] = { r -> seen += r.getHeader("Authorization"); ok("""{"page":1,"pages":2,"gifs":[${rgGif("l1")},${rgGif("l2")}]}""") }
        routes["/redgifs/v2/feeds/liked?page=2&count=40"] = { ok("""{"page":2,"pages":2,"gifs":[${rgGif("l3")}]}""") }
        val http = Http(userAgent = { "test" }, baseBackoffMs = 1)
        val redgifs = Redgifs(http, url("/redgifs").trimEnd('/'), userToken = { userToken }, onUserTokenRejected = { userToken = null })
        fun saver() = RedgifsSaver(http, store, { file, name -> saved[name] = file.readText() }, File(tmp.root, "work"), redgifs)

        assertEquals(3, saver().syncLikes(full = true).saved)
        assertEquals("Bearer user-token", seen.first())
        assertEquals("image:/img/l3.mp4", saved["redgifs_u_l3.mp4"])

        // Next check: one new like on top; the second page is not read again.
        saved.clear(); hits.clear()
        routes["/redgifs/v2/feeds/liked?page=1&count=40"] = { ok("""{"page":1,"pages":2,"gifs":[${rgGif("l0")},${rgGif("l1")}]}""") }
        routes["/redgifs/v2/feeds/liked?page=2&count=40"] = { ok("""{"page":2,"pages":2,"gifs":[${rgGif("l2")}]}""") }
        saver().syncLikes(full = false)
        assertEquals(setOf("redgifs_u_l0.mp4"), saved.keys)
        saver().syncLikes(full = false)
        assertTrue("/redgifs/v2/feeds/liked?page=2&count=40" !in hits.drop(1).dropWhile { it != "/redgifs/v2/feeds/liked?page=1&count=40" })

        // A rejected token ends the sign-in instead of looping.
        routes["/redgifs/v2/feeds/liked?page=1&count=40"] = { MockResponse().setResponseCode(401) }
        val error = runCatching { saver().syncLikes(full = false) }.exceptionOrNull()
        assertTrue(error is RedgifsSignedOutException)
        assertNull(userToken)
    }

    @Test
    fun `niches and their clips are listed with a temporary token`() = runBlocking {
        route("/redgifs/v2/auth/temporary", """{"token":"tok"}""")
        route("/redgifs/v2/niches/search?order=subscribers&page=1&count=30", """{"niches":[{"id":"cats","name":"Cats","gifs":120,"subscribers":9000,"thumbnail":"t"}]}""")
        route("/redgifs/v2/niches/search?order=best_match&page=1&count=30&query=dog", """{"niches":[]}""")
        route("/redgifs/v2/niches/cats/gifs?count=40&page=1&order=best", """{"page":1,"pages":3,"gifs":[${rgGif("N1")},{"id":"broken"}]}""")
        val redgifs = Redgifs(Http(userAgent = { "test" }, baseBackoffMs = 1), url("/redgifs").trimEnd('/'))

        assertEquals(listOf(RedgifsNiche("cats", "Cats", 120, 9000, "t")), redgifs.niches(""))
        assertTrue(redgifs.niches(" dog ").isEmpty())
        val page = redgifs.nicheGifs("cats", "best", 1)
        assertEquals(listOf("n1"), page.gifs.map { it.id })
        assertEquals(7, page.gifs[0].likes)
        assertTrue(page.hasMore)
    }

    @Test
    fun `authorize url carries the scopes and redirect`() {
        val auth = RedditAuth(Http({ "t" }), "cid", "upvotesaver://auth")
        assertEquals(
            "https://www.reddit.com/api/v1/authorize.compact?client_id=cid&response_type=code&state=s1" +
                "&redirect_uri=upvotesaver%3A%2F%2Fauth&duration=permanent&scope=identity%20history%20read",
            auth.authorizeUrl("s1"),
        )
    }
}
