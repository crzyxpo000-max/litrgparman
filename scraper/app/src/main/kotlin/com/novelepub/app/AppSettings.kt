package com.novelepub.app

import android.content.Context
import androidx.core.content.edit
import com.novelepub.core.GenericConfig
import com.novelepub.core.OkHttpFetcher
import org.json.JSONObject

/** User preferences, plus the generic-reader selectors remembered per site. */
class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var delaySeconds: Float
        get() = prefs.getFloat("delay_seconds", DEFAULT_DELAY_SECONDS)
        set(value) = prefs.edit { putFloat("delay_seconds", value.coerceIn(MIN_DELAY_SECONDS, 30f)) }

    var userAgent: String
        get() = prefs.getString("user_agent", null)?.takeIf { it.isNotBlank() } ?: OkHttpFetcher.DEFAULT_USER_AGENT
        set(value) = prefs.edit { putString("user_agent", value.trim()) }

    var includeAuthorNotes: Boolean
        get() = prefs.getBoolean("author_notes", false)
        set(value) = prefs.edit { putBoolean("author_notes", value) }

    /** The user's own Brave Search API key; blank = Brave is not used. */
    var braveApiKey: String
        get() = prefs.getString("brave_key", "").orEmpty()
        set(value) = prefs.edit { putString("brave_key", value.trim()) }

    /** Address of a SearXNG instance with JSON output; blank = not used. */
    var searxUrl: String
        get() = prefs.getString("searx_url", "").orEmpty()
        set(value) = prefs.edit { putString("searx_url", value.trim()) }

    val hasWebSearch: Boolean get() = braveApiKey.isNotBlank() || searxUrl.isNotBlank()

    fun genericConfig(host: String): GenericConfig? {
        val json = prefs.getString("generic:$host", null) ?: return null
        return runCatching {
            val o = JSONObject(json)
            GenericConfig(
                chapterLinkSelector = o.optString("links"),
                titleSelector = o.optString("title"),
                contentSelector = o.optString("content"),
                nextSelector = o.optString("next"),
                removeSelector = o.optString("remove"),
                language = o.optString("language", "en"),
                maxChapters = o.optInt("max", GenericConfig().maxChapters),
            )
        }.getOrNull()
    }

    /** Remembers selectors for [host]. Book title and author are per novel, so they are not stored. */
    fun saveGenericConfig(host: String, config: GenericConfig) {
        val o = JSONObject()
            .put("links", config.chapterLinkSelector)
            .put("title", config.titleSelector)
            .put("content", config.contentSelector)
            .put("next", config.nextSelector)
            .put("remove", config.removeSelector)
            .put("language", config.language)
            .put("max", config.maxChapters)
        prefs.edit { putString("generic:$host", o.toString()) }
    }

    companion object {
        const val DEFAULT_DELAY_SECONDS = 1.5f
        const val MIN_DELAY_SECONDS = 0.5f
    }
}
