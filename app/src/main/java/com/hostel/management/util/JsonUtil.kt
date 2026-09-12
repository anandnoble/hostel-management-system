package com.hostel.management.util

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonNull

fun Map<String, Any?>.toJsonObject(): JsonObject {
    return buildJsonObject {
        this@toJsonObject.forEach { (key, value) ->
            when (value) {
                is String -> put(key, value)
                is Int -> put(key, value)
                is Double -> put(key, value)
                is Float -> put(key, value)
                is Boolean -> put(key, value)
                null -> put(key, JsonNull)
                else -> put(key, value.toString())
            }
        }
    }
}
