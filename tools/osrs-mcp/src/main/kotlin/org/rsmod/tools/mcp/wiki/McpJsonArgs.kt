package org.rsmod.tools.mcp.wiki

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

internal fun JsonObject?.stringParam(key: String): String =
    (this?.get(key) as? JsonPrimitive)?.content?.trim().orEmpty()

internal fun JsonObject?.intParam(key: String): Int? = (this?.get(key) as? JsonPrimitive)?.intOrNull
