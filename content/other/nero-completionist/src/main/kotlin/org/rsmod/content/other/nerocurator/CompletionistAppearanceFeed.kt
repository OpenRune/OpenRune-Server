package org.rsmod.content.other.nerocurator

import com.google.gson.Gson
import com.sun.net.httpserver.HttpServer
import jakarta.inject.Singleton
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap

/** Immutable game-thread snapshots consumed by the optional local revision-240 renderer. */
@Singleton
class CompletionistAppearanceFeed {
    private val profiles = ConcurrentHashMap<String, Map<String, Any>>()
    private var server: HttpServer? = null

    fun publish(name: String, style: Map<String, Any>) { profiles[name] = style.toMap() }
    fun remove(name: String) { profiles.remove(name) }
    internal fun snapshot(): Map<String, Map<String, Any>> = profiles.toMap()

    fun start() {
        check(server == null)
        val options = Properties()
        val config = Path.of("nero-completionist.properties")
        if (Files.isRegularFile(config)) Files.newInputStream(config).use(options::load)
        val port = options.getProperty("appearance.port", "46602").toInt()
        require(port in 1024..65535)
        val address = InetAddress.getByName(options.getProperty("appearance.bind", "127.0.0.1"))
        require(address.isLoopbackAddress) { "The cape appearance feed must bind to loopback." }
        val gson = Gson()
        server = HttpServer.create(InetSocketAddress(address, port), 0).also { http ->
            http.createContext("/appearance") { exchange ->
                if (exchange.requestMethod != "GET" || exchange.requestURI.path != "/appearance") {
                    exchange.sendResponseHeaders(405, -1)
                    exchange.close()
                } else {
                    val body = gson.toJson(mapOf("schema" to 1, "profiles" to snapshot())).toByteArray(Charsets.UTF_8)
                    exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
                    exchange.responseHeaders.set("Cache-Control", "no-store")
                    exchange.sendResponseHeaders(200, body.size.toLong())
                    exchange.responseBody.use { it.write(body) }
                }
            }
            http.start()
        }
    }

    fun close() { server?.stop(0); server = null; profiles.clear() }
}
