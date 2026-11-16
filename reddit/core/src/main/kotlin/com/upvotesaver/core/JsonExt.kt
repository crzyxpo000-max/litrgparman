package com.upvotesaver.core

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

internal fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject
internal fun JsonObject.arr(key: String): JsonArray? = this[key] as? JsonArray
internal fun JsonObject.str(key: String): String? =
    (this[key] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content
internal fun JsonObject.bool(key: String): Boolean? = (this[key] as? JsonPrimitive)?.booleanOrNull
