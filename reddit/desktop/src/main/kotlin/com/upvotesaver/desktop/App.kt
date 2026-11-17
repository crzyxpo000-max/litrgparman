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

    /** Opens the browser for the user to sign in to Redgifs. */
    fun signIn() = run {
        onStatus(t("Sign in to Redgifs in the browser window. It closes by itself.", "Açılan tarayıcıda Redgifs'e giriş yap. Pencere kendiliğinden kapanır."), true)
        val token = login.capture(headless = false, userAgent = null, timeoutMs = 15 * 60_000L, accept = ::accept)
        if (token != null) t("Signed in as ${settings.user}.", "Giriş yapıldı: ${settings.user}.")
        else t("Sign-in was not completed.", "Giriş tamamlanmadı.")
    }

    fun signOut() {
        settings.token = null
        settings.user = ""
        // Forget the browser profile too, or the next sign-in would reuse its session.
        runCatching { File(dir, "browser").deleteRecursively() }
        onChanged()
    }

    /** Keeps a token only if it belongs to a signed-in user (visitors get a client token too). */
    private suspend fun accept(candidate: BrowserToken): Boolean {
        if (jwtSubject(candidate.token)?.startsWith("client/") == true) return false
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

    private fun jwtSubject(token: String): String? = runCatching {
        val payload = String(Base64.getUrlDecoder().decode(token.split('.')[1]))
        Regex("\"sub\"\\s*:\\s*\"([^\"]*)\"").find(payload)?.groupValues?.get(1)
    }.getOrNull()

    /** Loads the site out of sight in the browser profile so its refreshed token is picked up. */
    private suspend fun refreshSignIn(): Boolean {
        if (settings.user.isEmpty()) return false
        onStatus(t("Refreshing the Redgifs sign-in…", "Redgifs oturumu yenileniyor…"), true)
        return runCatching {
            login.capture(headless = true, userAgent = settings.userAgent, timeoutMs = 45_000L, accept = ::accept)
        }.getOrNull() != null
    }

    fun syncLikes(full: Boolean, quiet: Boolean = false) = run(quiet) {
        val expired = t("The Redgifs sign-in has expired. Sign in again.", "Redgifs oturumunun süresi doldu. Yeniden giriş yap.")
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
