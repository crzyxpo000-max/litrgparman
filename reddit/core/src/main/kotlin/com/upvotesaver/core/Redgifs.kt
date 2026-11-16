package com.upvotesaver.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

/** Redgifs' public API: a temporary token, then the clip's file addresses. The HD file includes sound. */
class Redgifs(private val http: Http, private val baseUrl: String = "https://api.redgifs.com") {
    private var token: String? = null

    suspend fun videoUrl(id: String): String {
        suspend fun fetch(): String {
            val t = token ?: Json.parseToJsonElement(http.text("$baseUrl/v2/auth/temporary")).jsonObject.str("token")
                ?.also { token = it } ?: error("Redgifs gave no token")
            return http.text("$baseUrl/v2/gifs/$id") { header("Authorization", "Bearer $t") }
        }
        val body = try {
            fetch()
        } catch (e: HttpException) {
            if (e.status != 401) throw e
            token = null
            fetch()
        }
        val urls = Json.parseToJsonElement(body).jsonObject.obj("gif")?.obj("urls")
        return urls?.str("hd") ?: urls?.str("sd") ?: error("Redgifs clip $id has no video file")
    }
}
