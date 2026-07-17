package org.rsmod.api.net.central

import java.nio.charset.StandardCharsets
import org.rsmod.api.server.config.OpenRuneCentralGameConfig
import org.rsmod.api.server.config.ServerConfig

public data class CentralWorldLinkSettings(
    val host: String,
    val port: Int,
    val worldKey: ByteArray,
    val worldId: Int,
) {
    public companion object {
        public fun resolve(config: ServerConfig): CentralWorldLinkSettings? {
            val envHost =
                System.getenv("OPENRUNE_CENTRAL_HOST")?.trim()?.takeIf { it.isNotEmpty() }
            val envKey =
                System.getenv("OPENRUNE_WORLD_KEY")?.trim()?.takeIf { it.isNotEmpty() }
            val envPort = System.getenv("OPENRUNE_CENTRAL_PORT")?.trim()?.toIntOrNull()

            val yml: OpenRuneCentralGameConfig? = config.central
            val sameInstance = yml?.sameInstance == true
            val hasRemoteYamlAuth =
                yml != null &&
                    yml.host.trim().isNotEmpty() &&
                    yml.worldKey.trim().isNotEmpty()
            val ymlOn = sameInstance || hasRemoteYamlAuth

            val host =
                envHost
                    ?: yml?.host?.trim()?.takeIf { it.isNotEmpty() && ymlOn }
                    ?: if (sameInstance && ymlOn) "127.0.0.1" else null
            val keyStr =
                envKey
                    ?: yml?.worldKey?.trim()?.takeIf { it.isNotEmpty() }
            if (host == null) {
                return null
            }
            if (!sameInstance && keyStr == null) {
                return null
            }
            val port = envPort ?: yml?.takeIf { ymlOn }?.linkPort ?: 9091
            val worldKeyBytes = (keyStr ?: "").toByteArray(StandardCharsets.UTF_8)
            return CentralWorldLinkSettings(
                host = host,
                port = port,
                worldKey = worldKeyBytes,
                worldId = config.world,
            )
        }
    }
}
