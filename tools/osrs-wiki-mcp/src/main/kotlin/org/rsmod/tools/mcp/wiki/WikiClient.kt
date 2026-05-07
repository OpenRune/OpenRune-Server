package org.rsmod.tools.mcp.wiki

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import org.jsoup.Jsoup

data class WikiSearchHit(
    val title: String,
    val snippet: String,
)

data class WikiPage(
    val title: String,
    val text: String,
    val url: String,
)

class WikiClient(
    private val client: HttpClient,
    private val mapper: ObjectMapper,
    private val apiBaseUrl: String = "https://oldschool.runescape.wiki/api.php",
) {
    suspend fun search(query: String, limit: Int): List<WikiSearchHit> {
        val body =
            client
                .get(apiBaseUrl) {
                    parameter("action", "query")
                    parameter("list", "search")
                    parameter("srsearch", query)
                    parameter("srlimit", limit)
                    parameter("format", "json")
                    parameter("formatversion", "2")
                    parameter("utf8", "1")
                }
                .bodyAsText()

        val root = mapper.readTree(body)
        val error = root.path("error")
        if (!error.isMissingNode && !error.isNull) {
            throw IllegalStateException(error.path("info").asText("Wiki search error"))
        }

        return root
            .path("query")
            .path("search")
            .map {
                WikiSearchHit(
                    title = it.path("title").asText(""),
                    snippet = htmlToText(it.path("snippet").asText("")),
                )
            }
            .filter { it.title.isNotBlank() }
    }

    suspend fun page(title: String, maxChars: Int): WikiPage {
        val body =
            client
                .get(apiBaseUrl) {
                    parameter("action", "parse")
                    parameter("page", title)
                    parameter("prop", "text|displaytitle")
                    parameter("format", "json")
                    parameter("formatversion", "2")
                    parameter("utf8", "1")
                }
                .bodyAsText()

        val root = mapper.readTree(body)
        val error = root.path("error")
        if (!error.isMissingNode && !error.isNull) {
            throw IllegalStateException(error.path("info").asText("Wiki page lookup error"))
        }

        val parse = root.path("parse")
        val resolvedTitle = parse.path("title").asText(title)
        val html = parseTextField(parse.path("text"))
        val cleaned = htmlToText(html)
        val clipped = if (cleaned.length > maxChars) cleaned.take(maxChars) + "..." else cleaned

        return WikiPage(
            title = resolvedTitle,
            text = clipped,
            url = wikiUrlForTitle(resolvedTitle),
        )
    }

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

    private fun parseTextField(node: JsonNode): String {
        if (node.isTextual) {
            return node.asText()
        }
        if (node.isObject) {
            return node.path("*").asText("")
        }
        return ""
    }

    private fun wikiUrlForTitle(title: String): String {
        val slug = title.replace(' ', '_')
        val encoded = URLEncoder.encode(slug, StandardCharsets.UTF_8).replace("+", "%20")
        return "https://oldschool.runescape.wiki/w/$encoded"
    }

    private fun htmlToText(html: String): String =
        Jsoup.parse(html).text().replace(Regex("\\s+"), " ").trim()
}



