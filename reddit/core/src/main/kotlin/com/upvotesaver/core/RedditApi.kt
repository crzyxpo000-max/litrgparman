package com.upvotesaver.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import okhttp3.HttpUrl.Companion.toHttpUrl

class Listing(val posts: List<JsonObject>, val after: String?)

/** The few OAuth API endpoints the app needs. Posts are returned as raw `data` objects. */
class RedditApi(
    private val http: Http,
    private val session: Session,
    private val baseUrl: String = "https://oauth.reddit.com",
) {
    suspend fun me(): String = get("$baseUrl/api/v1/me").str("name") ?: error("No user name in /api/v1/me")

    /** One page (up to 100 posts, newest first) of the posts [user] upvoted. Reddit lists about 1000 at most. */
    suspend fun upvoted(user: String, after: String?): Listing = listing(
        "$baseUrl/user/$user/upvoted".toHttpUrl().newBuilder()
            .addQueryParameter("limit", "100")
            .apply { if (after != null) addQueryParameter("after", after) }
            .toString(),
    )

    /** Posts by id (without the `t3_` prefix), at most 100 per call. Deleted posts are simply missing. */
    suspend fun info(ids: List<String>): List<JsonObject> {
        require(ids.size <= 100)
        if (ids.isEmpty()) return emptyList()
        return listing(
            "$baseUrl/api/info".toHttpUrl().newBuilder()
                .addQueryParameter("id", ids.joinToString(",") { "t3_$it" })
                .toString(),
        ).posts
    }

    private suspend fun listing(url: String): Listing {
        val data = get(url).obj("data")
        val posts = (data?.get("children") as? JsonArray).orEmpty()
            .mapNotNull { it as? JsonObject }
            .filter { it.str("kind") == "t3" }
            .mapNotNull { it.obj("data") }
        return Listing(posts, data?.str("after"))
    }

    private suspend fun get(url: String): JsonObject {
        // raw_json=1: Reddit otherwise HTML-escapes "&" in media URLs.
        val full = url.toHttpUrl().newBuilder().addQueryParameter("raw_json", "1").toString()
        suspend fun fetch(force: Boolean): String {
            val token = session.accessToken(force)
            return http.text(full) { header("Authorization", "bearer $token") }
        }
        val body = try {
            fetch(false)
        } catch (e: HttpException) {
            if (e.status != 401) throw e
            fetch(true)
        }
        return Json.parseToJsonElement(body).jsonObject
    }
}
