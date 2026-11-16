package com.upvotesaver.app

import android.content.Context
import androidx.core.content.edit
import com.upvotesaver.core.TokenStore
import com.upvotesaver.core.Tokens

/** Preferences and the Reddit sign-in, kept in the app's private storage (excluded from backups). */
class Settings(context: Context) : TokenStore {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var clientId: String
        get() = prefs.getString("client_id", "").orEmpty()
        set(value) = prefs.edit { putString("client_id", value.trim()) }

    var username: String
        get() = prefs.getString("username", "").orEmpty()
        set(value) = prefs.edit { putString("username", value) }

    /** The `state` value of a sign-in that is waiting for Reddit's redirect. */
    var pendingState: String?
        get() = prefs.getString("pending_state", null)
        set(value) = prefs.edit { putString("pending_state", value) }

    /** Minutes between automatic checks; 0 = off. */
    var intervalMinutes: Int
        get() = prefs.getInt("interval_minutes", 60)
        set(value) = prefs.edit { putInt("interval_minutes", value) }

    var wifiOnly: Boolean
        get() = prefs.getBoolean("wifi_only", true)
        set(value) = prefs.edit { putBoolean("wifi_only", value) }

    var lastSyncMs: Long
        get() = prefs.getLong("last_sync", 0)
        set(value) = prefs.edit { putLong("last_sync", value) }

    /** The Redgifs user token captured from the in-app sign-in; null when signed out or expired. */
    var redgifsToken: String?
        get() = prefs.getString("rg_token", null)
        set(value) = prefs.edit { putString("rg_token", value) }

    /** The Redgifs user name; stays set when the token expires so the app can ask to sign in again. */
    var redgifsUser: String
        get() = prefs.getString("rg_user", "").orEmpty()
        set(value) = prefs.edit { putString("rg_user", value) }

    val redgifsSignedIn: Boolean get() = redgifsToken != null

    val signedIn: Boolean get() = load() != null && username.isNotEmpty()

    override fun load(): Tokens? {
        val access = prefs.getString("access", null) ?: return null
        return Tokens(access, prefs.getString("refresh", null), prefs.getLong("expires", 0))
    }

    override fun save(tokens: Tokens?) = prefs.edit {
        if (tokens == null) {
            remove("access"); remove("refresh"); remove("expires")
        } else {
            putString("access", tokens.access); putString("refresh", tokens.refresh); putLong("expires", tokens.expiresAtMs)
        }
    }
}
