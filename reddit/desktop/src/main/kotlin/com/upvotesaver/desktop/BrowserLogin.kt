package com.upvotesaver.desktop

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/** A bearer token the site sent to its API, and the User-Agent of the browser that sent it. */
class BrowserToken(val token: String, val userAgent: String)

/**
 * Reads a sign-in the user copied from their everyday browser: a request to the Redgifs API copied
 * from the developer tools ("Copy as cURL", any shell flavour), a bare `Authorization` header, or
 * just the token. The User-Agent is taken along when the text holds one.
 */
object PastedSignIn {
    private val HEADER = Regex("""authorization["']?\s*[:=]\s*\^?["']?\s*Bearer\s+([A-Za-z0-9._~+/=-]+)""", RegexOption.IGNORE_CASE)
    private val BEARER = Regex("""Bearer\s+([A-Za-z0-9._~+/=-]+)""", RegexOption.IGNORE_CASE)
    private val JWT = Regex("""[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}""")
    private val AGENT_HEADER = Regex("""user-agent["']?\s*[:=]\s*\^?["']?\s*([^"'\r\n^]+)""", RegexOption.IGNORE_CASE)
    private val AGENT_FLAG = Regex("""-UserAgent\s+["']([^"'\r\n]+)["']""", RegexOption.IGNORE_CASE)

    fun parse(text: String): BrowserToken? {
        val token = HEADER.find(text)?.groupValues?.get(1)
            ?: BEARER.find(text)?.groupValues?.get(1)
            ?: JWT.find(text)?.value
            ?: return null
        val agent = (AGENT_FLAG.find(text) ?: AGENT_HEADER.find(text))?.groupValues?.get(1)?.trim()?.trimEnd('\\').orEmpty()
        return BrowserToken(token, agent.trim())
    }
}

/**
 * Sign-in through the user's own browser (Edge, Chrome or Brave). The browser is started with a
 * profile folder of its own and its debugging interface; the app watches the site's requests to
 * its API and notes the bearer token they carry. The password never reaches the app.
 */
class BrowserLogin(
    private val profileDir: File,
    private val browserPath: () -> String?,
    private val apiHost: String = "api.redgifs.com",
    private val startUrl: String = "https://www.redgifs.com/",
    private val extraArgs: List<String> = emptyList(),
) {
    private fun executable(): String = browserPath()?.takeIf { it.isNotBlank() } ?: findBrowser()
        ?: throw IOException(t("No browser found. Set its path in Settings.", "Tarayıcı bulunamadı. Ayarlar'dan yolunu gir."))

    /**
     * Opens the site in an ordinary browser window on the app's profile, with nothing attached to it,
     * for the user to sign in (site checks such as Cloudflare's see a normal browser). The session
     * stays in the profile; [capture] reads its token afterwards.
     */
    fun open(): Process {
        profileDir.mkdirs()
        val args = listOf(
            executable(), "--user-data-dir=${profileDir.absolutePath}", "--no-first-run", "--no-default-browser-check",
            "--disable-background-mode",
        ) + extraArgs + startUrl
        return ProcessBuilder(args).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start()
    }

    /**
     * Opens the browser and waits until [accept] takes one of the tokens seen, the browser is
     * closed, or [timeoutMs] passes. [headless] runs it out of sight (to refresh an existing sign-in).
     */
    suspend fun capture(
        headless: Boolean,
        userAgent: String?,
        timeoutMs: Long,
        accept: suspend (BrowserToken) -> Boolean,
    ): BrowserToken? = withContext(Dispatchers.IO) {
        val exe = executable()
        profileDir.mkdirs()
        val portFile = File(profileDir, "DevToolsActivePort").also { it.delete() }
        val args = mutableListOf(
            exe, "--remote-debugging-port=0", "--remote-allow-origins=*", "--user-data-dir=${profileDir.absolutePath}",
            "--no-first-run", "--no-default-browser-check", "--disable-background-mode",
        )
        if (headless) {
            args += "--headless=new"
            if (userAgent != null) args += "--user-agent=$userAgent"
        }
        args += extraArgs
        // Start on an empty page; the site is opened once the app is listening, so no request is missed.
        args += "about:blank"
        val process = ProcessBuilder(args).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start()
        val client = OkHttpClient.Builder().readTimeout(0, TimeUnit.MILLISECONDS).build()
        var socket: WebSocket? = null
        try {
            // The browser writes its debugging port and address here once it is up.
            var lines: List<String> = emptyList()
            for (i in 0 until 100) {
                if (!process.isAlive) throw IOException(t("The browser closed right away.", "Tarayıcı hemen kapandı."))
                lines = runCatching { portFile.readLines() }.getOrDefault(emptyList())
                if (lines.size >= 2) break
                delay(200)
            }
            if (lines.size < 2) throw IOException(t("The browser did not start its debugging interface.", "Tarayıcı hata ayıklama arayüzünü açmadı."))

            val found = Channel<BrowserToken>(Channel.UNLIMITED)
            val ids = AtomicInteger(1)
            val navigated = AtomicBoolean(false)
            var browserAgent = userAgent.orEmpty()
            fun WebSocket.command(method: String, session: String? = null, params: JsonObject = JsonObject(emptyMap())) {
                send(buildJsonObject {
                    put("id", ids.getAndIncrement()); put("method", method); put("params", params)
                    if (session != null) put("sessionId", session)
                }.toString())
            }
            socket = client.newWebSocket(
                Request.Builder().url("ws://127.0.0.1:${lines[0].trim()}${lines[1].trim()}").build(),
                object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        webSocket.command("Browser.getVersion")
                        // Attach to every tab and popup, now and later, on this one connection.
                        webSocket.command("Target.setAutoAttach", params = buildJsonObject {
                            put("autoAttach", true); put("waitForDebuggerOnStart", false); put("flatten", true)
                        })
                        // The tab that is already open has to be attached to by hand.
                        webSocket.command("Target.getTargets")
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        val message = runCatching { Json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return
                        val result = message["result"] as? JsonObject
                        result?.string("userAgent")?.let { if (browserAgent.isEmpty()) browserAgent = it }
                        (result?.get("targetInfos") as? JsonArray)?.forEach { info ->
                            val target = info as? JsonObject ?: return@forEach
                            if (target.string("type") == "page") {
                                webSocket.command("Target.attachToTarget", params = buildJsonObject {
                                    put("targetId", target.string("targetId")); put("flatten", true)
                                })
                            }
                        }
                        val params = message["params"] as? JsonObject ?: return
                        when (message.string("method")) {
                            "Target.attachedToTarget" -> params.string("sessionId")?.let { session ->
                                webSocket.command("Network.enable", session)
                                val isPage = (params["targetInfo"] as? JsonObject)?.string("type") == "page"
                                if (isPage && navigated.compareAndSet(false, true)) {
                                    webSocket.command("Page.navigate", session, buildJsonObject { put("url", startUrl) })
                                }
                            }
                            "Network.requestWillBeSent" -> {
                                val request = params["request"] as? JsonObject ?: return
                                val host = request.string("url")?.let { runCatching { java.net.URI(it).host }.getOrNull() }
                                if (host == apiHost) token(request["headers"] as? JsonObject)
                            }
                            "Network.requestWillBeSentExtraInfo" -> {
                                val headers = params["headers"] as? JsonObject ?: return
                                val host = (headers.header(":authority") ?: headers.header("host"))?.substringBefore(':')
                                if (host == apiHost) token(headers)
                            }
                        }
                    }

                    private fun token(headers: JsonObject?) {
                        val value = headers?.header("authorization") ?: return
                        if (!value.startsWith("Bearer ", ignoreCase = true)) return
                        val agent = headers.header("user-agent") ?: browserAgent
                        found.trySend(BrowserToken(value.substring(7).trim(), agent))
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { found.close() }
                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { found.close() }
                },
            )
            val seen = HashSet<String>()
            withTimeoutOrNull(timeoutMs) {
                for (candidate in found) {
                    if (seen.add(candidate.token) && accept(candidate)) return@withTimeoutOrNull candidate
                }
                null
            }
        } finally {
            // Ask the browser to close itself so it writes its cookies; then make sure it is gone.
            runCatching { socket?.send("""{"id":0,"method":"Browser.close"}""") }
            runCatching { if (!process.waitFor(5, TimeUnit.SECONDS)) process.destroyForcibly() }
            runCatching { socket?.cancel() }
            client.dispatcher.executorService.shutdown()
        }
    }

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonObject.header(name: String): String? =
        entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.let { (it.value as? JsonPrimitive)?.content }

    companion object {
        /** Edge, Chrome or Brave in their usual places (Edge ships with Windows). */
        fun findBrowser(): String? {
            System.getenv("UPVOTESAVER_BROWSER")?.takeIf { File(it).isFile }?.let { return it }
            val roots = listOf("ProgramFiles(x86)", "ProgramFiles", "LocalAppData").mapNotNull { System.getenv(it) }
            val windows = listOf(
                "Microsoft\\Edge\\Application\\msedge.exe",
                "Google\\Chrome\\Application\\chrome.exe",
                "BraveSoftware\\Brave-Browser\\Application\\brave.exe",
            ).flatMap { path -> roots.map { File(it, path) } }
            val other = listOf(
                "/usr/bin/google-chrome", "/usr/bin/chromium", "/usr/bin/chromium-browser", "/usr/bin/microsoft-edge",
                "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
            ).map(::File)
            return (windows + other).firstOrNull { it.isFile }?.path
        }
    }
}
