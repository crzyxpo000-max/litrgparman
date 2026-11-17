package com.upvotesaver.desktop

import java.io.File
import java.util.Locale
import java.util.Properties

/** Turkish when the system language is Turkish, English otherwise. */
private val TURKISH = Locale.getDefault().language == "tr"
fun t(en: String, tr: String): String = if (TURKISH) tr else en

/** Preferences and the Redgifs sign-in, kept as a properties file in the app's data folder. */
class DesktopSettings(private val file: File) {
    private val props = Properties()

    init {
        if (file.exists()) file.inputStream().use { props.load(it) }
    }

    @Synchronized
    private fun get(key: String): String? = props.getProperty(key)?.takeIf { it.isNotEmpty() }

    @Synchronized
    private fun set(key: String, value: String?) {
        if (value.isNullOrEmpty()) props.remove(key) else props.setProperty(key, value)
        file.parentFile?.mkdirs()
        file.outputStream().use { props.store(it, "Upvote Saver") }
    }

    /** The Redgifs user token taken from the browser sign-in; null when signed out or expired. */
    var token: String?
        get() = get("token")
        set(value) = set("token", value)

    /** The Redgifs user name; stays set when the token expires so the app can refresh the sign-in. */
    var user: String
        get() = get("user").orEmpty()
        set(value) = set("user", value)

    /** The User-Agent of the browser the token was issued to; Redgifs requests must use the same one. */
    var userAgent: String
        get() = get("userAgent") ?: DEFAULT_USER_AGENT
        set(value) = set("userAgent", value)

    /** True when the sign-in was pasted by the user; such a sign-in cannot be refreshed by the app. */
    var pasted: Boolean
        get() = get("pasted") == "true"
        set(value) = set("pasted", value.toString())

    var folder: File
        get() = File(get("folder") ?: File(System.getProperty("user.home"), "Videos/UpvoteSaver").path)
        set(value) = set("folder", value.path)

    /** Mark the download folder as hidden in Windows. */
    var hidden: Boolean
        get() = get("hidden") == "true"
        set(value) = set("hidden", value.toString())

    /** Minutes between automatic checks of the liked list; 0 = off. */
    var intervalMinutes: Int
        get() = get("intervalMinutes")?.toIntOrNull() ?: 60
        set(value) = set("intervalMinutes", value.toString())

    /** Browser used for sign-in; blank = find Edge, Chrome or Brave. */
    var browserPath: String
        get() = get("browserPath").orEmpty()
        set(value) = set("browserPath", value.trim())

    companion object {
        const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"

        /** %APPDATA%\UpvoteSaver on Windows, ~/.upvotesaver elsewhere. */
        fun dataDir(): File {
            val appData = System.getenv("APPDATA")
            return if (!appData.isNullOrBlank()) File(appData, "UpvoteSaver") else File(System.getProperty("user.home"), ".upvotesaver")
        }
    }
}
