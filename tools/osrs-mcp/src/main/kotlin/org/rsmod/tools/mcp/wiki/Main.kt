package org.rsmod.tools.mcp.wiki

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered

private const val MCP_NAME = "osrs-mcp"
private const val MCP_VERSION = "0.1.0"
private const val WIKI_USER_AGENT = "OpenRune-osrs-mcp/$MCP_VERSION"

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
                                defaultRequest { headers.append("User-Agent", WIKI_USER_AGENT) }
                            }.also { httpClient = it }
                    WikiClient(client, mapper)
                },
            )

        val server =
            Server(
                serverInfo = Implementation(name = MCP_NAME, version = MCP_VERSION),
                options =
                    ServerOptions(
                        capabilities =
                            ServerCapabilities(
                                tools = ServerCapabilities.Tools(listChanged = false),
                            ),
                    ),
            )

        server.registerOsrsMcpTools(toolService)

        server.createSession(
            transport =
                StdioServerTransport(
                    inputStream = System.`in`.asSource().buffered(),
                    outputStream = System.out.asSink().buffered(),
                ),
        )

        try {
            System.err.println("[$MCP_NAME] Server starting...")
            awaitCancellation()
        } finally {
            httpClient?.close()
        }
    }
}
