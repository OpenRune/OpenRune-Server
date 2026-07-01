package org.rsmod.api.net.central

import jakarta.inject.Inject
import java.net.InetSocketAddress
import org.rsmod.api.net.central.netty.WorldLinkNettyBlockingClient
import org.rsmod.api.net.central.netty.WorldLinkNettyBlockingSession
import org.rsmod.api.server.config.ServerConfig

public class CentralDiscordWorldLink
@Inject
constructor(
    private val serverConfig: ServerConfig,
) {
    public fun createGamePending(
        accountId: Int,
        discordUsername: String,
    ): GamePendingResult {
        val settings = CentralWorldLinkSettings.resolve(serverConfig) ?: return GamePendingResult.Unavailable
        return runCatching {
            withHandshake(settings) { session ->
                session.send(WorldLinkPackets.gameDiscordLinkPending(accountId, discordUsername))
                val frame = session.recvInbound(SOCKET_TIMEOUT_MS.toLong())
                when (val op = frame[0].toInt() and 0xFF) {
                    WorldLinkFrameSpecs.OP_GAME_DISCORD_LINK_PENDING_OK -> {
                        val payload = WorldLinkFrameSpecs.decodeGameDiscordLinkPendingOk(frame)
                        GamePendingResult.Ok(
                            code = payload.code,
                            dmSent = payload.dmSent,
                        )
                    }
                    WorldLinkFrameSpecs.OP_GAME_DISCORD_LINK_PENDING_FAIL -> {
                        val reason = frame[1].toInt() and 0xFF
                        when (reason) {
                            WorldLinkFrameSpecs.GAME_DISCORD_LINK_PENDING_FAIL_ALREADY_LINKED ->
                                GamePendingResult.AlreadyLinked
                            WorldLinkFrameSpecs.GAME_DISCORD_LINK_PENDING_FAIL_DISCORD_NOT_FOUND ->
                                GamePendingResult.DiscordNotFound
                            else -> GamePendingResult.Unavailable
                        }
                    }
                    else -> GamePendingResult.Unavailable
                }
            }
        }.getOrElse { GamePendingResult.Unavailable }
    }

    public fun invalidatePending(accountId: Int): Boolean {
        val settings = CentralWorldLinkSettings.resolve(serverConfig) ?: return false
        return runCatching {
            withHandshake(settings) { session ->
                session.send(WorldLinkPackets.gameDiscordLinkInvalidate(accountId))
                val frame = session.recvInbound(SOCKET_TIMEOUT_MS.toLong())
                frame.isNotEmpty() &&
                    frame[0].toInt() and 0xFF == WorldLinkFrameSpecs.OP_GAME_DISCORD_LINK_INVALIDATE_ACK
            }
        }.getOrDefault(false)
    }

    private inline fun <T> withHandshake(
        settings: CentralWorldLinkSettings,
        block: (WorldLinkNettyBlockingSession) -> T,
    ): T {
        val session =
            WorldLinkNettyBlockingClient.connect(
                InetSocketAddress(settings.host, settings.port),
                readIdleSeconds = SOCKET_TIMEOUT_SECONDS,
            )
        return try {
            session.send(WorldLinkPackets.worldHello(settings.worldId, settings.worldKey))
            val hello = session.recvInbound(SOCKET_TIMEOUT_MS.toLong())
            val helloOp = hello[0].toInt() and 0xFF
            if (helloOp != WorldLinkFrameSpecs.OP_HELLO_ACK) {
                error("Central rejected world-link hello")
            }
            block(session)
        } finally {
            session.close()
        }
    }

    public sealed class GamePendingResult {
        public data class Ok(
            val code: Int,
            val dmSent: Boolean,
        ) : GamePendingResult()

        public data object AlreadyLinked : GamePendingResult()

        public data object DiscordNotFound : GamePendingResult()

        public data object Unavailable : GamePendingResult()
    }

    private companion object {
        private const val SOCKET_TIMEOUT_MS: Int = 15_000

        private const val SOCKET_TIMEOUT_SECONDS: Int = SOCKET_TIMEOUT_MS / 1000
    }
}
