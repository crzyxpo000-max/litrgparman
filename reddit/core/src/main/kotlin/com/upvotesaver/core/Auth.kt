package com.upvotesaver.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.IOException

data class Tokens(val access: String, val refresh: String?, val expiresAtMs: Long)

interface TokenStore {
    fun load(): Tokens?
    fun save(tokens: Tokens?)
}

class NotSignedInException : IOException("Not signed in to Reddit.")

/**
 * Reddit OAuth2 for an "installed app": no client secret, authorization-code flow with a
 * permanent refresh token. See https://github.com/reddit-archive/reddit/wiki/OAuth2.
 */
class RedditAuth(
    private val http: Http,
    private val clientId: String,
    private val redirectUri: String,
    private val baseUrl: String = "https://www.reddit.com",
    private val now: () -> Long = System::currentTimeMillis,
) {
    fun authorizeUrl(state: String): String = "$baseUrl/api/v1/authorize.compact".toHttpUrl().newBuilder()
        .addQueryParameter("client_id", clientId)
        .addQueryParameter("response_type", "code")
        .addQueryParameter("state", state)
        .addQueryParameter("redirect_uri", redirectUri)
        .addQueryParameter("duration", "permanent")
        .addQueryParameter("scope", SCOPES)
        .build().toString()

    suspend fun exchange(code: String): Tokens = token(
        FormBody.Builder().add("grant_type", "authorization_code").add("code", code).add("redirect_uri", redirectUri).build(),
        previousRefresh = null,
    )

    suspend fun refresh(refreshToken: String): Tokens = token(
        FormBody.Builder().add("grant_type", "refresh_token").add("refresh_token", refreshToken).build(),
        previousRefresh = refreshToken,
    )

    private suspend fun token(form: FormBody, previousRefresh: String?): Tokens {
        val body = http.text("$baseUrl/api/v1/access_token") {
            header("Authorization", Credentials.basic(clientId, ""))
            post(form)
        }
        val json = runCatching { Json.parseToJsonElement(body).jsonObject }.getOrNull()
        val access = json?.get("access_token")?.jsonPrimitive?.content
            ?: throw IOException("Reddit did not return a token: ${json?.get("error")?.jsonPrimitive?.content ?: "unexpected answer"}")
        val expiresIn = json["expires_in"]?.jsonPrimitive?.longOrNull ?: 3600
        return Tokens(
            access = access,
            // A refresh answer usually omits the refresh token; the old one stays valid.
            refresh = json["refresh_token"]?.jsonPrimitive?.content ?: previousRefresh,
            expiresAtMs = now() + expiresIn * 1000,
        )
    }

    companion object {
        /** identity: user name; history: the upvoted listing; read: post data by id. */
        const val SCOPES = "identity history read"
    }
}

/** Hands out a valid access token, refreshing it shortly before it expires. */
class Session(
    private val auth: RedditAuth,
    private val store: TokenStore,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val lock = Mutex()

    suspend fun accessToken(forceRefresh: Boolean = false): String = lock.withLock {
        val tokens = store.load() ?: throw NotSignedInException()
        if (!forceRefresh && tokens.expiresAtMs - now() > 60_000) return tokens.access
        val refresh = tokens.refresh ?: throw NotSignedInException()
        val fresh = try {
            auth.refresh(refresh)
        } catch (e: HttpException) {
            // 400/401 here means the user revoked the app's access.
            if (e.status == 400 || e.status == 401) {
                store.save(null)
                throw NotSignedInException()
            }
            throw e
        }
        store.save(fresh)
        fresh.access
    }
}
