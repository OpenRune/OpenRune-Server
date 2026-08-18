package org.rsmod.tools.wiki.dumping.wiki.bucket

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.encodeURLParameter
import java.io.Closeable
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.delay

private const val API = "https://oldschool.runescape.wiki/api.php"
private const val USER_AGENT =
    "OpenRune-Server wiki-dumping (https://github.com/OpenRune/OpenRune-Server)"

private val sharedHttp by lazy {
    HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 30_000
        }
    }
}

internal suspend fun ktorFetch(url: String): String =
    sharedHttp.get(url) { headers { append(HttpHeaders.UserAgent, USER_AGENT) } }.bodyAsText()

/**
 * Generic client for the wiki's Bucket API (`action=bucket`): builds queries, pages via
 * `.offset()`, caches concatenated row arrays on disk, and validates responses. Cache-first so
 * reruns are offline-safe; `offline = true` forbids network entirely (replay verification).
 */
class BucketSource(
    private val cacheDir: Path,
    private val offline: Boolean = false,
    private val throttleMillis: Long = 500,
    private val fetch: suspend (String) -> String = ::ktorFetch,
) : Closeable {
    private val mapper = ObjectMapper()
    private var lastRequest = 0L

    override fun close() = Unit

    suspend fun rows(
        bucket: String,
        select: List<String>,
        where: Pair<String, String>? = null,
        cacheKey: String,
        limit: Int = 5000,
    ): List<JsonNode> {
        val cached = cacheFile(bucket, cacheKey)
        if (Files.isRegularFile(cached)) {
            return mapper.readTree(Files.readString(cached)).toList()
        }
        check(!offline) { "offline mode: no cached response for bucket=$bucket key=$cacheKey" }
        val all = mutableListOf<JsonNode>()
        var offset = 0
        while (true) {
            val page = fetchPage(bucket, select, where, limit, offset)
            all += page
            if (page.size < limit) break
            offset += limit
        }
        Files.createDirectories(cached.parent)
        Files.writeString(cached, mapper.writeValueAsString(all))
        return all
    }

    private suspend fun fetchPage(
        bucket: String,
        select: List<String>,
        where: Pair<String, String>?,
        limit: Int,
        offset: Int,
    ): List<JsonNode> {
        val selectArgs = select.joinToString(",") { "'$it'" }
        val whereClause = where?.let { ".where('${it.first}','${escape(it.second)}')" } ?: ""
        val query =
            "bucket('$bucket').select($selectArgs)$whereClause.limit($limit).offset($offset).run()"
        val url = "$API?action=bucket&format=json&query=${query.encodeURLParameter()}"
        throttle()
        val json = mapper.readTree(fetch(url))
        json.get("error")?.let { error("bucket $bucket query failed: ${it.asText()}") }
        val rows = json.get("bucket")
        check(rows != null && rows.isArray) { "bucket $bucket: response has no bucket array" }
        return rows.toList()
    }

    private suspend fun throttle() {
        val since = System.currentTimeMillis() - lastRequest
        if (since < throttleMillis) delay(throttleMillis - since)
        lastRequest = System.currentTimeMillis()
    }

    private fun cacheFile(bucket: String, key: String): Path =
        cacheDir.resolve(bucket).resolve(sanitize(key) + ".json")

    private fun escape(value: String): String = value.replace("\\", "\\\\").replace("'", "\\'")

    private fun sanitize(key: String): String = key.replace(Regex("[^A-Za-z0-9._-]"), "_")
}

fun JsonNode.requireText(field: String, context: String): String {
    val node = get(field)
    check(node != null && node.isTextual) { "$context: field '$field' missing or non-text" }
    return node.asText()
}
