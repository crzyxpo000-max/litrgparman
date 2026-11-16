package com.upvotesaver.app

import android.app.Application
import android.content.Context
import com.upvotesaver.core.Http
import com.upvotesaver.core.PostStore
import com.upvotesaver.core.Progress
import com.upvotesaver.core.RedditApi
import com.upvotesaver.core.RedditAuth
import com.upvotesaver.core.Redgifs
import com.upvotesaver.core.RedgifsSaver
import com.upvotesaver.core.Session
import com.upvotesaver.core.SyncEngine
import kotlinx.coroutines.sync.Mutex
import java.io.File

class UpvoteApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Graph.init(this)
    }
}

/** The app's few shared objects. */
object Graph {
    const val REDIRECT_URI = "upvotesaver://auth"

    lateinit var settings: Settings
        private set
    lateinit var store: PostStore
        private set

    /** Reddit asks for a descriptive, unique User-Agent: platform:app id:version (by /u/user). */
    val http = Http(userAgent = {
        val user = settings.username.ifEmpty { "unknown" }
        "android:com.upvotesaver:v${BuildInfo.VERSION} (by /u/$user)"
    })

    /** Redgifs traffic uses the browser's User-Agent, the one its sign-in token was issued to. */
    lateinit var redgifsUserAgent: String
        private set
    val redgifsHttp = Http(userAgent = { redgifsUserAgent })
    val redgifs = Redgifs(
        http = redgifsHttp,
        userToken = { settings.redgifsToken },
        onUserTokenRejected = { settings.redgifsToken = null },
    )

    /** Only one run at a time, whether started by hand or by the schedule. */
    val syncLock = Mutex()

    fun init(context: Context) {
        settings = Settings(context)
        store = PostStore(File(context.filesDir, "posts.jsonl"))
        redgifsUserAgent = runCatching { android.webkit.WebSettings.getDefaultUserAgent(context) }
            .getOrDefault("Mozilla/5.0 (Linux; Android) Mobile")
    }

    fun auth() = RedditAuth(http, settings.clientId, REDIRECT_URI)

    fun api() = RedditApi(http, Session(auth(), settings))

    fun engine(context: Context, onProgress: (Progress) -> Unit) = SyncEngine(
        api = api(),
        http = http,
        store = store,
        sink = MediaStoreSink(context.applicationContext, settings),
        muxer = AndroidMuxer,
        workDir = File(context.cacheDir, "work"),
        onProgress = onProgress,
    )

    fun redgifsSaver(context: Context, onProgress: (Progress) -> Unit) = RedgifsSaver(
        http = redgifsHttp,
        redgifs = redgifs,
        store = store,
        sink = MediaStoreSink(context.applicationContext, settings),
        workDir = File(context.cacheDir, "work"),
        onProgress = onProgress,
    )
}

object BuildInfo {
    const val VERSION = "0.1.0"
}
