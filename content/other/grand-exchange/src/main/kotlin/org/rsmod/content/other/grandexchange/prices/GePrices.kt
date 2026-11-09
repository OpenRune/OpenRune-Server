package org.rsmod.content.other.grandexchange.prices

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToLong
import org.rsmod.api.enums.NamedEnums.ge_guide_prices
import org.rsmod.api.server.config.GrandExchangeYaml

/**
 * Guide prices: the OSRS Wiki real-time prices when live prices are on and reachable, otherwise
 * the snapshot shipped in the cache (`enum.ge_guide_prices`). The wiki is only ever called from
 * this class's own daemon thread.
 */
@Singleton
class GePrices @Inject constructor() {
    private val logger = InlineLogger()

    private val snapshot: Map<Int, Long> by lazy {
        ge_guide_prices.backing.entries
            .mapNotNull { (obj, price) -> price?.let { obj.id to it.toLong() } }
            .toMap()
    }

    @Volatile private var live: Map<Int, Long> = emptyMap()

    private val refreshed = AtomicBoolean(false)
    private var executor: ScheduledExecutorService? = null

    val liveCount: Int
        get() = live.size

    val snapshotCount: Int
        get() = snapshot.size

    fun guide(obj: Int): Long? = live[obj] ?: snapshot[obj]

    /** True once after each successful live refresh; the GE re-checks waiting offers then. */
    fun consumeRefreshed(): Boolean = refreshed.getAndSet(false)

    fun start(config: GrandExchangeYaml) {
        if (!config.livePrices || executor != null) {
            return
        }
        val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
        val scheduler =
            Executors.newSingleThreadScheduledExecutor { runnable ->
                Thread(runnable, "ge-prices").apply { isDaemon = true }
            }
        val minutes = config.refreshMinutes.coerceAtLeast(1).toLong()
        scheduler.scheduleWithFixedDelay(
            { refresh(client, config.userAgent) },
            0,
            minutes,
            TimeUnit.MINUTES,
        )
        executor = scheduler
    }

    fun stop() {
        executor?.shutdownNow()
        executor = null
        live = emptyMap()
    }

    private fun refresh(client: HttpClient, userAgent: String) {
        try {
            val latest = fetch(client, userAgent, "latest")
            val hourly = fetch(client, userAgent, "1h")
            val prices = parse(latest, hourly)
            if (prices.isNotEmpty()) {
                live = prices
                refreshed.set(true)
                logger.info { "Grand Exchange: loaded ${prices.size} live prices." }
            }
        } catch (t: Throwable) {
            logger.warn { "Grand Exchange: live prices unavailable, using stored prices. (${t.message})" }
        }
    }

    private fun fetch(client: HttpClient, userAgent: String, endpoint: String): JsonNode {
        val request =
            HttpRequest.newBuilder(URI.create("$API/$endpoint"))
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", userAgent)
                .GET()
                .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        check(response.statusCode() == 200) { "$endpoint returned HTTP ${response.statusCode()}" }
        return MAPPER.readTree(response.body()).path("data")
    }

    companion object {
        private const val API = "https://prices.runescape.wiki/api/v1/osrs"
        private val MAPPER = ObjectMapper()

        /** 1h averages where an item traded in the last hour, its latest trades otherwise. */
        fun parse(latest: JsonNode, hourly: JsonNode): Map<Int, Long> {
            val prices = HashMap<Int, Long>()
            for ((key, node) in latest.properties()) {
                val obj = key.toIntOrNull() ?: continue
                average(node.path("high"), node.path("low"))?.let { prices[obj] = it }
            }
            for ((key, node) in hourly.properties()) {
                val obj = key.toIntOrNull() ?: continue
                average(node.path("avgHighPrice"), node.path("avgLowPrice"))?.let { prices[obj] = it }
            }
            return prices
        }

        private fun average(a: JsonNode, b: JsonNode): Long? {
            val values = listOf(a, b).filter { it.isNumber && it.asLong() > 0 }.map { it.asLong() }
            if (values.isEmpty()) {
                return null
            }
            return (values.sum().toDouble() / values.size).roundToLong()
        }
    }
}
