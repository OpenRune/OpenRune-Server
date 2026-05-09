package org.rsmod.tools.mcp.wiki

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private fun jsonStringProp(): JsonObject = buildJsonObject { put("type", "string") }

private fun jsonIntProp(min: Int, max: Int, default: Int): JsonObject =
    buildJsonObject {
        put("type", "integer")
        put("minimum", min)
        put("maximum", max)
        put("default", default)
    }

private fun toolError(message: String): CallToolResult =
    CallToolResult(isError = true, content = listOf(TextContent(text = message)))

private fun toolOk(text: String): CallToolResult =
    CallToolResult(content = listOf(TextContent(text = text)))

private suspend inline fun runTool(crossinline block: suspend () -> String): CallToolResult =
    try {
        toolOk(block())
    } catch (e: Exception) {
        toolError("Tool call failed: ${e.message ?: "unknown error"}")
    }

internal suspend fun Server.registerOsrsMcpTools(toolService: WikiTool) {
    addTool(
        name = "wiki_search",
        description = "Searches the Old School RuneScape Wiki for relevant pages.",
        inputSchema =
            ToolSchema(
                properties =
                    buildJsonObject {
                        put("query", jsonStringProp())
                        put("limit", jsonIntProp(min = 1, max = 10, default = 5))
                    },
                required = listOf("query"),
            ),
    ) { request ->
        val args = request.arguments
        val query = args.stringParam("query")
        if (query.isBlank()) {
            return@addTool toolError("'query' is required and must be non-empty.")
        }
        val limit = args.intParam("limit")?.coerceIn(1, 10) ?: 5
        runTool { toolService.wikiSearch(query, limit) }
    }

    addTool(
        name = "wiki_page",
        description = "Fetches a wiki page by title and returns cleaned text.",
        inputSchema =
            ToolSchema(
                properties =
                    buildJsonObject {
                        put("title", jsonStringProp())
                        put("maxChars", jsonIntProp(min = 500, max = 20000, default = 6000))
                    },
                required = listOf("title"),
            ),
    ) { request ->
        val args = request.arguments
        val title = args.stringParam("title")
        if (title.isBlank()) {
            return@addTool toolError("'title' is required and must be non-empty.")
        }
        val maxChars = args.intParam("maxChars")?.coerceIn(500, 20000) ?: 6000
        runTool { toolService.wikiPage(title, maxChars) }
    }

    addTool(
        name = "wiki_npc_spawns",
        description = "Retrieves NPC spawn locations from raw wiki source LocLine entries for a page.",
        inputSchema =
            ToolSchema(
                properties =
                    buildJsonObject {
                        put("title", jsonStringProp())
                        put("npcName", jsonStringProp())
                        put("location", jsonStringProp())
                    },
                required = listOf("title"),
            ),
    ) { request ->
        val args = request.arguments
        val title = args.stringParam("title")
        if (title.isBlank()) {
            return@addTool toolError("'title' is required and must be non-empty.")
        }
        val npcName = args.stringParam("npcName")
        val location = args.stringParam("location")
        runTool {
            toolService.wikiNpcSpawns(
                title = title,
                npcName = npcName.ifBlank { null },
                location = location.ifBlank { null },
            )
        }
    }

    addTool(
        name = "gameval_search",
        description = "Searches gamevals.dat mappings to resolve config keys to IDs.",
        inputSchema =
            ToolSchema(
                properties =
                    buildJsonObject {
                        put("query", jsonStringProp())
                        put("table", jsonStringProp())
                        put("id", buildJsonObject { put("type", "integer") })
                        put("limit", jsonIntProp(min = 1, max = 50, default = 10))
                    },
                required = emptyList(),
            ),
    ) { request ->
        val args = request.arguments
        val query = args.stringParam("query")
        val table = args.stringParam("table")
        val id = args.intParam("id")
        val limit = args.intParam("limit")?.coerceIn(1, 50) ?: 10
        if (query.isBlank() && id == null) {
            return@addTool toolError("Provide at least one filter: 'query' or 'id'.")
        }
        runTool {
            toolService.gamevalSearch(
                query = query.ifBlank { null },
                table = table.ifBlank { null },
                id = id,
                limit = limit,
            )
        }
    }

    addTool(
        name = "cache_search",
        description = "Searches decoded cache definitions in LIVE or SERVER cache.",
        inputSchema =
            ToolSchema(
                properties =
                    buildJsonObject {
                        put("cache", jsonStringProp())
                        put("type", jsonStringProp())
                        put("query", jsonStringProp())
                        put("id", buildJsonObject { put("type", "integer") })
                        put("limit", jsonIntProp(min = 1, max = 100, default = 25))
                    },
                required = listOf("cache", "type"),
            ),
    ) { request ->
        val args = request.arguments
        val cacheRaw = args.stringParam("cache")
        if (cacheRaw.isBlank()) {
            return@addTool toolError("'cache' is required and must be 'LIVE' or 'SERVER'.")
        }
        val cacheKind = CacheKind.parse(cacheRaw)
        if (cacheKind == null) {
            return@addTool toolError("Invalid 'cache' value '$cacheRaw'. Expected 'LIVE' or 'SERVER'.")
        }

        val typeRaw = args.stringParam("type")
        if (typeRaw.isBlank()) {
            return@addTool toolError("'type' is required (for example npc, obj, item, anim, all).")
        }
        val searchType = CacheSearchType.parse(typeRaw)
        if (searchType == null) {
            return@addTool toolError(
                "Invalid 'type' value '$typeRaw'. Use npc, obj, item, anim, enum, struct, healthbar, hitsplat, varbit, varp, dbrow, dbtable, or all.",
            )
        }

        val query = args.stringParam("query")
        val id = args.intParam("id")
        val limit = args.intParam("limit")?.coerceIn(1, 100) ?: 25
        if (query.isBlank() && id == null) {
            return@addTool toolError("Provide at least one filter: 'query' or 'id'.")
        }
        runTool {
            toolService.cacheSearch(
                cache = cacheKind,
                type = searchType,
                query = query.ifBlank { null },
                id = id,
                limit = limit,
            )
        }
    }
}
