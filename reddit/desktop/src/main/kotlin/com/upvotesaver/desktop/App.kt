package com.upvotesaver.desktop

import com.upvotesaver.core.Http
import com.upvotesaver.core.MediaSink
import com.upvotesaver.core.PostStore
import com.upvotesaver.core.Progress
import com.upvotesaver.core.Redgifs
import com.upvotesaver.core.RedgifsLinks
import com.upvotesaver.core.RedgifsSaver
import com.upvotesaver.core.RedgifsSignedOutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Base64

/** Moves finished files into the download folder. */
class FolderSink(private val settings: DesktopSettings) : MediaSink {
    override fun publish(file: File, fileName: String) {
        val folder = prepareFolder(settings)
        Files.move(file.toPath(), File(folder, fileName).toPath(), StandardCopyOption.REPLACE_EXISTING)
    }

    companion object {
        /** Creates the download folder and applies the "hidden" setting to it (a Windows file attribute). */
        fun prepareFolder(settings: DesktopSettings): File {
            val folder = settings.folder
            folder.mkdirs()
            runCatching { Files.setAttribute(folder.toPath(), "dos:hidden", settings.hidden) }
            return folder
        }
    }
}

/** Starts the app with Windows through a small launcher in the user's Startup folder. */
object Autostart {
    private val startup: File? = System.getenv("APPDATA")?.takeIf { System.getProperty("os.name").startsWith("Windows") }
        ?.let { File(it, "Microsoft\\Windows\\Start Menu\\Programs\\Startup\\UpvoteSaver.bat") }

    val supported: Boolean get() = startup != null && jarPath() != null

    val enabled: Boolean get() = startup?.isFile == true

    private fun jarPath(): String? = runCatching {
        File(Autostart::class.java.protectionDomain.codeSource.location.toURI()).takeIf { it.isFile && it.name.endsWith(".jar") }?.path
    }.getOrNull()

    fun set(on: Boolean) {
        val file = startup ?: return
        if (!on) {
            file.delete()
            return
        }
        val javaw = File(System.getProperty("java.home"), "bin\\javaw.exe").path
        // chcp 65001: the paths below are written as UTF-8 (user names can hold non-ASCII letters).
        file.writeText("@echo off\r\nchcp 65001 >nul\r\nstart \"\" \"$javaw\" -jar \"${jarPath()}\" --tray\r\n", Charsets.UTF_8)
    }
}

/** The app's state and actions; the window only shows it. All work runs one job at a time. */
class App(val dir: File = DesktopSettings.dataDir()) {
    val settings = DesktopSettings(File(dir, "settings.properties"))
    val store = PostStore(File(dir, "posts.jsonl"))
    val http = Http(userAgent = { settings.userAgent })
    val redgifs = Redgifs(http, userToken = { settings.token }, onUserTokenRejected = { settings.token = null })
    private val login = BrowserLogin(File(dir, "browser"), { settings.browserPath })
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Mutex()
    private var job: Job? = null

    /** Called (on any thread) with a status line and whether work is going on. */
    var onStatus: (String, Boolean) -> Unit = { _, _ -> }

    /** Called (on any thread) when the account or the saved list changed. */
    var onChanged: () -> Unit = {}

    val signedIn: Boolean get() = settings.token != null

    init {
        // The schedule: check the liked list while the app runs (window open or in the tray).
        scope.launch {
            while (true) {
                val minutes = settings.intervalMinutes
                delay(if (minutes > 0) minutes * 60_000L else 60_000L)
                if (minutes > 0 && settings.user.isNotEmpty() && job?.isActive != true) syncLikes(full = false, quiet = true)
            }
        }
    }

    private fun run(quiet: Boolean = false, block: suspend () -> String) {
        job = scope.launch {
            lock.withLock {
                try {
                    val done = block()
                    onStatus(done, false)
                } catch (e: CancellationException) {
                    onStatus(t("Cancelled.", "İptal edildi."), false)
                    throw e
                } catch (e: Exception) {
                    onStatus(if (quiet) "" else t("Error: ", "Hata: ") + (e.message ?: e.javaClass.simpleName), false)
                } finally {
                    onChanged()
                }
            }
        }
    }

    fun cancel() { job?.cancel() }

    private fun saver() = RedgifsSaver(http, store, FolderSink(settings), File(dir, "work"), redgifs) { p ->
        onStatus(t("Working… ${p.scanned} checked, ${p.saved} saved, ${p.failed} failed",
            "Çalışıyor… ${p.scanned} denetlendi, ${p.saved} kaydedildi, ${p.failed} başarısız"), true)
        if (p.current.isNotEmpty()) onChanged()
    }

    private fun summary(p: Progress) = t("Done: ${p.saved} saved, ${p.failed} failed.", "Bitti: ${p.saved} kaydedildi, ${p.failed} başarısız.")

    /** Downloads the Redgifs clips linked in [text]. Returns false when it holds no Redgifs link. */
    fun saveLinks(text: String): Boolean {
        val ids = RedgifsLinks.ids(text)
        if (ids.isEmpty()) return false
        saveIds(ids)
        return true
    }

    fun saveIds(ids: List<String>) = run {
        onStatus(t("Downloading…", "İndiriliyor…"), true)
        summary(saver().save(ids))
    }

    /** Set while the sign-in browser is open; the window then offers a "I have signed in" button. */
    @Volatile
    var signInWaiting: CompletableDeferred<Unit>? = null
        private set

    /** The user says the sign-in is done (for browsers that keep running after their window is closed). */
    fun confirmSignIn() { signInWaiting?.complete(Unit) }

    /**
     * Sign-in in two steps. First an ordinary browser window, with nothing attached, where the user
     * signs in and then closes the window. Then the same profile is opened briefly to read the token
     * the site now uses.
     */
    fun signIn() = run {
        val waiting = CompletableDeferred<Unit>()
        val process = login.open()
        signInWaiting = waiting
        onChanged()
        onStatus(t("Sign in to Redgifs in the browser, then close the browser window.",
            "Tarayıcıda Redgifs'e giriş yap, sonra tarayıcı penceresini kapat."), true)
        try {
            while (process.isAlive && !waiting.isCompleted) delay(500)
            // Give the browser a moment to write its session before it is closed for good.
            if (process.isAlive) { delay(3_000); process.destroy() }
        } finally {
            signInWaiting = null
            if (process.isAlive) process.destroy()
            onChanged()
        }
        delay(1_500)
        onStatus(t("Reading the sign-in… (a browser window opens for a moment)", "Giriş okunuyor… (bir tarayıcı penceresi kısa süre açılır)"), true)
        val token = login.capture(headless = false, userAgent = null, timeoutMs = 60_000L, accept = ::accept)
        if (token != null) settings.pasted = false
        if (token != null) t("Signed in as ${settings.user}.", "Giriş yapıldı: ${settings.user}.")
        else t("No sign-in was found. Sign in, close the browser window and try again.",
            "Giriş bulunamadı. Giriş yap, tarayıcı penceresini kapat ve yeniden dene.")
    }

    /**
     * Signs in with a request (or token) the user copied from their own browser, where they are
     * already signed in. Nothing is opened by the app.
     */
    fun signInWithPaste(text: String) = run {
        val candidate = PastedSignIn.parse(text)
            ?: return@run t("No sign-in token found in that text.", "Bu metinde oturum jetonu bulunamadı.")
        if (jwtField(candidate.token, "sub")?.startsWith("client/") == true) {
            return@run t("That is a visitor's token. Sign in on redgifs.com first, then copy a request again.",
                "Bu bir ziyaretçi jetonu. Önce redgifs.com'da giriş yap, sonra isteği yeniden kopyala.")
        }
        onStatus(t("Checking the sign-in…", "Giriş denetleniyor…"), true)
        if (!accept(candidate)) {
            return@run t("Redgifs did not accept that token. Copy a fresh request and try again.",
                "Redgifs bu jetonu kabul etmedi. Yeni bir istek kopyalayıp yeniden dene.")
        }
        settings.pasted = true
        t("Signed in as ${settings.user}.", "Giriş yapıldı: ${settings.user}.")
    }

    /** When the current token stops being valid (from the token itself), or null if it does not say. */
    val tokenExpiresAtMs: Long?
        get() = settings.token?.let { jwtField(it, "exp") }?.toLongOrNull()?.times(1000)

    fun signOut() {
        settings.token = null
        settings.user = ""
        settings.pasted = false
        // Forget the browser profile too, or the next sign-in would reuse its session.
        runCatching { File(dir, "browser").deleteRecursively() }
        onChanged()
    }

    /** Keeps a token only if it belongs to a signed-in user (visitors get a client token too). */
    private suspend fun accept(candidate: BrowserToken): Boolean {
        if (jwtField(candidate.token, "sub")?.startsWith("client/") == true) return false
        val agent = candidate.userAgent.ifEmpty { settings.userAgent }
        val name = try {
            Redgifs(Http(userAgent = { agent }), userToken = { candidate.token }).me()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return false
        }
        settings.userAgent = agent
        settings.token = candidate.token
        settings.user = name.ifEmpty { "Redgifs" }
        onChanged()
        return true
    }

    /** A field of the token's payload, if the token is a readable JWT. */
    private fun jwtField(token: String, name: String): String? = runCatching {
        val payload = String(Base64.getUrlDecoder().decode(token.split('.')[1]))
        Regex("\"$name\"\\s*:\\s*\"?([^\",}]*)").find(payload)?.groupValues?.get(1)
    }.getOrNull()

    /** Opens the site briefly in the browser profile so its refreshed token is picked up. */
    private suspend fun refreshSignIn(): Boolean {
        // A pasted sign-in lives in the user's own browser; the app has no session of its own to reopen.
        if (settings.user.isEmpty() || settings.pasted) return false
        onStatus(t("Refreshing the Redgifs sign-in…", "Redgifs oturumu yenileniyor…"), true)
        return runCatching {
            login.capture(headless = false, userAgent = null, timeoutMs = 45_000L, accept = ::accept)
        }.getOrNull() != null
    }

    fun syncLikes(full: Boolean, quiet: Boolean = false) = run(quiet) {
        val expired = if (settings.pasted) {
            t("The Redgifs sign-in has expired. Copy a request from your browser and paste it again.",
                "Redgifs oturumunun süresi doldu. Tarayıcından bir isteği kopyalayıp yeniden yapıştır.")
        } else {
            t("The Redgifs sign-in has expired. Sign in again.", "Redgifs oturumunun süresi doldu. Yeniden giriş yap.")
        }
        if (!signedIn && !refreshSignIn()) return@run expired
        onStatus(t("Reading your likes…", "Beğenilerin okunuyor…"), true)
        val result = try {
            saver().syncLikes(full)
        } catch (e: RedgifsSignedOutException) {
            if (!refreshSignIn()) return@run expired
            saver().syncLikes(full)
        }
        summary(result)
    }
}
