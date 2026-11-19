package org.rsmod.content.events.demonicpacts

import com.github.michaelbull.logging.InlineLogger
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import jakarta.inject.Inject
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.Executors
import org.rsmod.api.server.config.ServerConfig
import org.rsmod.game.world.WorldType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Serves this world's `jav_config.ws` and `world_list.ws` with the seasonal (league) world flag
 * set. The client decides whether it is on a league world from these flags, and only then shows
 * its league interfaces. Point RSProx's `jav_config_url` here; nothing is fetched from the
 * internet, so it also keeps the setup working offline.
 */
class LeagueWebScript @Inject constructor(private val config: ServerConfig) : PluginScript() {
    private val logger = InlineLogger()

    override val worldTypes = listOf(WorldType.DEMONIC_PACTS_LEAGUE)

    override fun ScriptContext.startup() {
        val port = DemonicPactsData.settings.webPort
        try {
            val server = HttpServer.create(InetSocketAddress(LOCALHOST, port), 0)
            server.executor = Executors.newSingleThreadExecutor { Thread(it, "league-web").apply { isDaemon = true } }
            server.createContext("/jav_config.ws") { it.respond(TEXT) { javConfig(port) } }
            server.createContext("/world_list.ws") { it.respond(BINARY) { worldList() } }
            server.start()
            logger.info {
                "Demonic Pacts League: league world config at http://$LOCALHOST:$port/jav_config.ws"
            }
        } catch (e: Exception) {
            logger.error(e) { "Demonic Pacts League: could not serve the league world config on port $port" }
        }
    }

    private fun javConfig(port: Int): ByteArray {
        val base = fetchCentralJavConfig()
        val replacements =
            mapOf(
                MEMBERS_PARAM to "true",
                WORLD_PROPERTIES_PARAM to WORLD_FLAGS.toString(),
                INITIAL_WORLD_PARAM to config.world.toString(),
                REVISION_PARAM to config.revision.toString(),
                WORLD_LIST_PARAM to "http://$LOCALHOST:$port/world_list.ws",
            )
        val seen = HashSet<Int>()
        val lines =
            base.lineSequence()
                .filter { it.isNotEmpty() }
                .map { line ->
                    val param = paramId(line) ?: return@map line
                    val value = replacements[param] ?: return@map line
                    seen += param
                    "param=$param=$value"
                }
                .toMutableList()
        for ((param, value) in replacements) {
            if (param !in seen) {
                lines += "param=$param=$value"
            }
        }
        return lines.joinToString(separator = "\r\n", postfix = "\r\n").toByteArray(Charsets.ISO_8859_1)
    }

    private fun fetchCentralJavConfig(): String {
        val httpPort = config.central?.httpPort ?: DEFAULT_CENTRAL_PORT
        val request =
            HttpRequest.newBuilder(URI("http://$LOCALHOST:$httpPort/jav_config.ws"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build()
        val client = HttpClient.newBuilder().proxy(HttpClient.Builder.NO_PROXY).build()
        return client.send(request, HttpResponse.BodyHandlers.ofString(Charsets.ISO_8859_1)).body()
    }

    private fun paramId(line: String): Int? {
        if (!line.startsWith("param=")) {
            return null
        }
        return line.removePrefix("param=").substringBefore('=').toIntOrNull()
    }

    private fun worldList(): ByteArray {
        val body = ByteArrayOutputStream()
        DataOutputStream(body).use { out ->
            out.writeShort(1)
            out.writeShort(config.world)
            out.writeInt(WORLD_FLAGS)
            out.writeJagString(LOCALHOST)
            out.writeJagString(ACTIVITY)
            out.writeByte(LOCATION)
            out.writeShort(POPULATION)
        }
        val payload = body.toByteArray()
        val framed = ByteArrayOutputStream()
        DataOutputStream(framed).use { out ->
            out.writeInt(payload.size)
            out.write(payload)
        }
        return framed.toByteArray()
    }

    private fun DataOutputStream.writeJagString(value: String) {
        write(value.toByteArray(Charsets.ISO_8859_1))
        writeByte(0)
    }

    private fun HttpExchange.respond(contentType: String, build: () -> ByteArray) {
        try {
            val body = build()
            responseHeaders.add("Content-Type", contentType)
            sendResponseHeaders(200, body.size.toLong())
            responseBody.use { it.write(body) }
        } catch (e: Exception) {
            logger.error(e) { "Demonic Pacts League: failed to answer ${requestURI}" }
            runCatching { sendResponseHeaders(500, -1) }
            close()
        }
    }

    private companion object {
        const val LOCALHOST = "127.0.0.1"
        const val ACTIVITY = "Demonic Pacts League"
        const val LOCATION = 0
        const val POPULATION = 1
        const val DEFAULT_CENTRAL_PORT = 8080
        const val TEXT = "text/plain; charset=ISO-8859-1"
        const val BINARY = "application/octet-stream"

        const val MEMBERS_FLAG = 0x1
        const val SEASONAL_FLAG = 0x40000000
        const val WORLD_FLAGS = MEMBERS_FLAG or SEASONAL_FLAG

        const val MEMBERS_PARAM = 3
        const val WORLD_PROPERTIES_PARAM = 5
        const val INITIAL_WORLD_PARAM = 12
        const val WORLD_LIST_PARAM = 17
        const val REVISION_PARAM = 25
    }
}
