package org.rsmod.tools.mcp.wiki

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

fun main() {
    runBlocking {
        val mapper = jacksonObjectMapper()
        var httpClient: HttpClient? = null
        val toolService =
            WikiTool(
                wikiProvider = {
                    val client =
                        httpClient
                            ?: HttpClient(CIO) {
                                install(HttpTimeout) {
                                    requestTimeoutMillis = 20_000
                                    connectTimeoutMillis = 10_000
                                    socketTimeoutMillis = 20_000
                                }
                                defaultRequest {
                                    headers.append("User-Agent", "rsmod-osrs-wiki-mcp/0.1.0")
                                }
                            }.also { httpClient = it }
                    WikiClient(client, mapper)
                },
            )

        val server =
            Server(
                serverInfo = Implementation(name = "osrs-wiki-mcp", version = "0.1.0"),
                options =
                    ServerOptions(
                        capabilities =
                            ServerCapabilities(
                                tools = ServerCapabilities.Tools(listChanged = false),
                            ),
                    ),
            )

        server.addTool(
            name = "wiki_search",
            description = "Searches the Old School RuneScape Wiki for relevant pages.",
            inputSchema =
                ToolSchema(
                    properties =
                        buildJsonObject {
                            put("query", buildJsonObject { put("type", "string") })
                            put(
                                "limit",
                                buildJsonObject {
                                    put("type", "integer")
                                    put("minimum", 1)
                                    put("maximum", 10)
                                    put("default", 5)
                                },
                            )
                        },
                    required = listOf("query"),
                ),
        ) { request ->
            val query = request.arguments?.get("query")?.jsonPrimitive?.content?.trim().orEmpty()
            if (query.isBlank()) {
                return@addTool CallToolResult(
                    isError = true,
                    content = listOf(TextContent(text = "'query' is required and must be non-empty.")),
                )
            }

            val limit = request.arguments?.get("limit")?.jsonPrimitive?.intOrNull?.coerceIn(1, 10) ?: 5
            val result =
                try {
                    toolService.wikiSearch(query, limit)
                } catch (e: Exception) {
                    return@addTool CallToolResult(
                        isError = true,
                        content = listOf(TextContent(text = "Tool call failed: ${e.message ?: "unknown error"}")),
                    )
                }

            CallToolResult(content = listOf(TextContent(text = result)))
        }

        server.addTool(
            name = "wiki_page",
            description = "Fetches a wiki page by title and returns cleaned text.",
            inputSchema =
                ToolSchema(
                    properties =
                        buildJsonObject {
                            put("title", buildJsonObject { put("type", "string") })
                            put(
                                "maxChars",
                                buildJsonObject {
                                    put("type", "integer")
                                    put("minimum", 500)
                                    put("maximum", 20000)
                                    put("default", 6000)
                                },
                            )
                        },
                    required = listOf("title"),
                ),
        ) { request ->
            val title = request.arguments?.get("title")?.jsonPrimitive?.content?.trim().orEmpty()
            if (title.isBlank()) {
                return@addTool CallToolResult(
                    isError = true,
                    content = listOf(TextContent(text = "'title' is required and must be non-empty.")),
                )
            }

            val maxChars = request.arguments?.get("maxChars")?.jsonPrimitive?.intOrNull?.coerceIn(500, 20000) ?: 6000
            val result =
                try {
                    toolService.wikiPage(title, maxChars)
                } catch (e: Exception) {
                    return@addTool CallToolResult(
                        isError = true,
                        content = listOf(TextContent(text = "Tool call failed: ${e.message ?: "unknown error"}")),
                    )
                }

            CallToolResult(content = listOf(TextContent(text = result)))
        }

        server.addTool(
            name = "wiki_npc_spawns",
            description =
                "Retrieves NPC spawn locations from raw wiki source LocLine entries for a page.",
            inputSchema =
                ToolSchema(
                    properties =
                        buildJsonObject {
                            put("title", buildJsonObject { put("type", "string") })
                            put("npcName", buildJsonObject { put("type", "string") })
                            put("location", buildJsonObject { put("type", "string") })
                        },
                    required = listOf("title"),
                ),
        ) { request ->
            val title = request.arguments?.get("title")?.jsonPrimitive?.content?.trim().orEmpty()
            if (title.isBlank()) {
                return@addTool CallToolResult(
                    isError = true,
                    content = listOf(TextContent(text = "'title' is required and must be non-empty.")),
                )
            }

            val npcName = request.arguments?.get("npcName")?.jsonPrimitive?.content?.trim().orEmpty()
            val location = request.arguments?.get("location")?.jsonPrimitive?.content?.trim().orEmpty()

            val result =
                try {
                    toolService.wikiNpcSpawns(
                        title = title,
                        npcName = npcName.ifBlank { null },
                        location = location.ifBlank { null },
                    )
                } catch (e: Exception) {
                    return@addTool CallToolResult(
                        isError = true,
                        content = listOf(TextContent(text = "Tool call failed: ${e.message ?: "unknown error"}")),
                    )
                }

            CallToolResult(content = listOf(TextContent(text = result)))
        }

        server.addTool(
            name = "gameval_search",
            description = "Searches gamevals.dat mappings to resolve config keys to IDs.",
            inputSchema =
                ToolSchema(
                    properties =
                        buildJsonObject {
                            put("query", buildJsonObject { put("type", "string") })
                            put("table", buildJsonObject { put("type", "string") })
                            put("id", buildJsonObject { put("type", "integer") })
                            put(
                                "limit",
                                buildJsonObject {
                                    put("type", "integer")
                                    put("minimum", 1)
                                    put("maximum", 50)
                                    put("default", 10)
                                },
                            )
                        },
                    required = emptyList(),
                ),
        ) { request ->
            val query = request.arguments?.get("query")?.jsonPrimitive?.content?.trim().orEmpty()
            val table = request.arguments?.get("table")?.jsonPrimitive?.content?.trim().orEmpty()
            val id = request.arguments?.get("id")?.jsonPrimitive?.intOrNull
            val limit = request.arguments?.get("limit")?.jsonPrimitive?.intOrNull?.coerceIn(1, 50) ?: 10

            if (query.isBlank() && id == null) {
                return@addTool CallToolResult(
                    isError = true,
                    content =
                        listOf(
                            TextContent(
                                text = "Provide at least one filter: 'query' or 'id'.",
                            ),
                        ),
                )
            }

            val result =
                try {
                    toolService.gamevalSearch(
                        query = query.ifBlank { null },
                        table = table.ifBlank { null },
                        id = id,
                        limit = limit,
                    )
                } catch (e: Exception) {
                    return@addTool CallToolResult(
                        isError = true,
                        content = listOf(TextContent(text = "Tool call failed: ${e.message ?: "unknown error"}")),
                    )
                }

            CallToolResult(content = listOf(TextContent(text = result)))
        }

        server.addTool(
            name = "cache_search",
            description = "Searches decoded cache definitions in LIVE or SERVER cache.",
            inputSchema =
                ToolSchema(
                    properties =
                        buildJsonObject {
                            put("cache", buildJsonObject { put("type", "string") })
                            put("type", buildJsonObject { put("type", "string") })
                            put("query", buildJsonObject { put("type", "string") })
                            put("id", buildJsonObject { put("type", "integer") })
                            put(
                                "limit",
                                buildJsonObject {
                                    put("type", "integer")
                                    put("minimum", 1)
                                    put("maximum", 100)
                                    put("default", 25)
                                },
                            )
                        },
                    required = listOf("cache", "type"),
                ),
        ) { request ->
            val cacheRaw = request.arguments?.get("cache")?.jsonPrimitive?.content?.trim().orEmpty()
            if (cacheRaw.isBlank()) {
                return@addTool CallToolResult(
                    isError = true,
                    content = listOf(TextContent(text = "'cache' is required and must be 'LIVE' or 'SERVER'.")),
                )
            }

            val cacheKind = CacheKind.parse(cacheRaw)
            if (cacheKind == null) {
                return@addTool CallToolResult(
                    isError = true,
                    content = listOf(TextContent(text = "Invalid 'cache' value '$cacheRaw'. Expected 'LIVE' or 'SERVER'.")),
                )
            }

            val typeRaw = request.arguments?.get("type")?.jsonPrimitive?.content?.trim().orEmpty()
            if (typeRaw.isBlank()) {
                return@addTool CallToolResult(
                    isError = true,
                    content = listOf(TextContent(text = "'type' is required (for example npc, obj, item, anim, all).")),
                )
            }

            val searchType = CacheSearchType.parse(typeRaw)
            if (searchType == null) {
                return@addTool CallToolResult(
                    isError = true,
                    content = listOf(TextContent(text = "Invalid 'type' value '$typeRaw'. Use npc, obj, item, anim, enum, struct, healthbar, hitsplat, varbit, varp, dbrow, dbtable, or all.")),
                )
            }

            val query = request.arguments?.get("query")?.jsonPrimitive?.content?.trim().orEmpty()
            val id = request.arguments?.get("id")?.jsonPrimitive?.intOrNull
            val limit = request.arguments?.get("limit")?.jsonPrimitive?.intOrNull?.coerceIn(1, 100) ?: 25

            if (query.isBlank() && id == null) {
                return@addTool CallToolResult(
                    isError = true,
                    content = listOf(TextContent(text = "Provide at least one filter: 'query' or 'id'.")),
                )
            }

            val result =
                try {
                    toolService.cacheSearch(
                        cache = cacheKind,
                        type = searchType,
                        query = query.ifBlank { null },
                        id = id,
                        limit = limit,
                    )
                } catch (e: Exception) {
                    return@addTool CallToolResult(
                        isError = true,
                        content = listOf(TextContent(text = "Tool call failed: ${e.message ?: "unknown error"}")),
                    )
                }

            CallToolResult(content = listOf(TextContent(text = result)))
        }

        server.createSession(
            transport =
                StdioServerTransport(
                    inputStream = System.`in`.asSource().buffered(),
                    outputStream = System.out.asSink().buffered(),
                ),
        )

        try {
            System.err.println("[osrs-wiki-mcp] Server starting...")
            awaitCancellation()
        } finally {
            httpClient?.close()
        }
    }
}
