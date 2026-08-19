package org.rsmod.tools.wiki.dumping.dropfill

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

/**
 * Targeted client for the wiki's Bucket API, which serves post-template-expansion drop data as
 * structured JSON — the same rows the wikitext `{{DropsLine}}` templates render, without the
 * wikitext parsing.
 *
 * Cache-first: raw responses land in `<cacheDir>/<bucket>/<key>.json` so reruns and offline
 * verification work without network. Responses are validated before the cache is written; schema
 * drift throws rather than emitting partial data.
 */
class BucketClient(private val cacheDir: Path, private val offline: Boolean = false) : Closeable {
    private val mapper = ObjectMapper()
    private val http by lazy {
        HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 30_000
            }
        }
    }

    override fun close() {
        http.close()
    }

    suspend fun dropsForPage(page: String): List<DropsRow> {
        val spaced = page.replace('_', ' ').replace("'", "\\'")
        val query =
            "bucket('dropsline').select('drop_json','page_name_sub')" +
                ".where('page_name','$spaced').limit(5000).run()"
        val json = fetch("dropsline", page, query)
        val bucket = json.get("bucket")
        check(bucket != null && bucket.isArray) {
            "Dropsline response has no bucket array for $page"
        }
        return bucket.map { row ->
            val dropJson = row.get("drop_json")
            check(dropJson != null && dropJson.isTextual) {
                "Dropsline row missing drop_json for $page — schema drift"
            }
            val drop = mapper.readTree(dropJson.asText())
            val item = drop.get("Dropped item")
            val rarity = drop.get("Rarity")
            check(item != null && item.isTextual && rarity != null && rarity.isTextual) {
                "drop_json missing \"Dropped item\"/\"Rarity\" for $page — schema drift"
            }
            val qtyLow = drop.get("Quantity Low")
            val qtyHigh = drop.get("Quantity High")
            check(qtyLow != null && qtyLow.isNumber && qtyHigh != null && qtyHigh.isNumber) {
                "drop_json missing numeric \"Quantity Low\"/\"Quantity High\" for $page — schema drift"
            }
            DropsRow(
                item = item.asText(),
                rarity = rarity.asText(),
                qtyLow = qtyLow.asInt(),
                qtyHigh = qtyHigh.asInt(),
                noted = drop.get("Drop Quantity")?.asText()?.contains("(noted)") == true,
            )
        }
    }

    /**
     * The `infobox_item` bucket rows for a wiki page name: one row per infobox version, each with
     * the version's exact item name and its ids (an array of strings; one version can carry several
     * ids). The names let the resolver pick the version a drop row actually names when a page
     * defines variants — e.g. "Dragon knife" vs "Dragon knife(p)".
     */
    suspend fun itemRowsForPageName(pageName: String): List<InfoboxItemRow> {
        val escaped = pageName.replace("'", "\\'")
        val query =
            "bucket('infobox_item').select('item_id','item_name','page_name')" +
                ".where('page_name','$escaped').limit(500).run()"
        val json = fetch("infobox_item", pageName, query)
        val bucket = json.get("bucket")
        check(bucket != null && bucket.isArray) {
            "infobox_item response has no bucket array for $pageName"
        }
        return bucket.map { row ->
            val raw = row.get("item_id")
            val values =
                when {
                    raw == null -> emptyList()
                    raw.isArray -> raw.toList()
                    else -> listOf(raw)
                }
            InfoboxItemRow(
                itemName = row.get("item_name")?.takeIf { it.isTextual }?.asText(),
                ids = values.mapNotNull { it.asText().toIntOrNull() }.distinct(),
            )
        }
    }

    private suspend fun fetch(bucket: String, key: String, query: String): JsonNode {
        val dir = cacheDir.resolve(bucket)
        val cachePath = dir.resolve(key.replace(Regex("[^A-Za-z0-9_-]"), "_") + ".json")
        if (Files.exists(cachePath)) {
            return mapper.readTree(Files.readString(cachePath))
        }
        if (offline) {
            error("offline mode and no cache for $bucket/$key ($cachePath)")
        }
        Files.createDirectories(dir)
        val url = "$API?action=bucket&format=json&query=${query.encodeURLParameter()}"
        var lastError: Exception? = null
        for (attempt in 0 until 3) {
            delay(if (attempt == 0) THROTTLE_MILLIS else 4_000L * attempt)
            try {
                val response =
                    http.get(url) { headers { append(HttpHeaders.UserAgent, USER_AGENT) } }
                check(response.status.value in 200..299) {
                    "HTTP ${response.status.value} for $key"
                }
                val text = response.bodyAsText()
                val json = mapper.readTree(text)
                check(json.get("bucket")?.isArray == true) { "no bucket array for $key" }
                Files.writeString(cachePath, text)
                return json
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw IllegalStateException(
            "Bucket fetch failed for $bucket/$key after 3 attempts: ${lastError?.message}",
            lastError,
        )
    }

    companion object {
        private const val THROTTLE_MILLIS = 1_000L
        private const val API = "https://oldschool.runescape.wiki/api.php"
        private const val USER_AGENT =
            "OpenRune-wiki-dumping/0.1 (github.com/OpenRune/OpenRune-Server)"
    }
}

/** One `infobox_item` version row: the version's exact item name and its ids. */
data class InfoboxItemRow(val itemName: String?, val ids: List<Int>)

data class DropsRow(
    val item: String,
    val rarity: String,
    val qtyLow: Int,
    val qtyHigh: Int,
    /** The wiki marks noted drops inside "Drop Quantity", e.g. "7–13 (noted)". */
    val noted: Boolean,
)
