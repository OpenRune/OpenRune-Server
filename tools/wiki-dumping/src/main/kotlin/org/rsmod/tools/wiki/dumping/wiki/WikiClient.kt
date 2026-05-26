package org.rsmod.tools.wiki.dumping.wiki

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import java.io.Closeable
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText

class WikiClient(
    private val client: HttpClient,
    private val mapper: ObjectMapper = jacksonObjectMapper(),
    private val apiBaseUrl: String = "https://oldschool.runescape.wiki/api.php",
) : Closeable {
    override fun close() {
        client.close()
    }

    suspend fun fetchText(url: String): String =
        client.get(url).bodyAsText()

    suspend fun rawPageSource(title: String): String {
        val body =
            client
                .get(apiBaseUrl) {
                    parameter("action", "query")
                    parameter("prop", "revisions")
                    parameter("rvprop", "content")
                    parameter("rvslots", "main")
                    parameter("titles", title)
                    parameter("format", "json")
                    parameter("formatversion", "2")
                    parameter("utf8", "1")
                }
                .bodyAsText()

        val root = mapper.readTree(body)
        val error = root.path("error")
        if (!error.isMissingNode && !error.isNull) {
            throw IllegalStateException(error.path("info").asText("Wiki source lookup error"))
        }

        val page = root.path("query").path("pages").firstOrNull()
        val missing = page?.path("missing")?.asBoolean(false) ?: true
        if (missing) {
            throw IllegalStateException("The page '$title' does not exist.")
        }

        val revision = page.path("revisions").firstOrNull()
        val fromMainSlot = revision?.path("slots")?.path("main")?.path("content")?.asText("").orEmpty()
        val fallback = revision?.path("content")?.asText("").orEmpty()
        val content = if (fromMainSlot.isNotBlank()) fromMainSlot else fallback
        if (content.isBlank()) {
            throw IllegalStateException("No wikitext source available for '$title'.")
        }
        return content
    }

    companion object {
        fun create(): WikiClient = WikiClient(HttpClient(CIO))
    }
}
