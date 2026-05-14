package org.rsmod.api.net.central

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Inject
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentLinkedQueue
import net.rsprot.protocol.loginprot.outgoing.LoginResponse
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.MiscOutput
import org.rsmod.api.player.output.mes
import org.rsmod.api.net.central.netty.WorldLinkNettyBlockingClient
import org.rsmod.api.net.central.netty.WorldLinkNettyBlockingSession
import org.rsmod.api.registry.player.PlayerRegistry
import org.rsmod.api.server.config.OpenRuneCentralGameConfig
import org.rsmod.api.server.config.ServerConfig
import org.rsmod.game.GameUpdate
import org.rsmod.game.GameUpdate.Companion.isUpdating

public class OpenRuneCentralWorldLink
@Inject
constructor(
    private val serverConfig: ServerConfig,
    private val gameUpdate: GameUpdate,
) {
    private val logger = InlineLogger()

    private val settings: CentralSettings? = CentralSettings.resolve(serverConfig)

    private val pendingCentralRevokes =
        ConcurrentLinkedQueue<WorldLinkFrameSpecs.ServerRevokeLoginPayload>()

    private val pendingCentralKicks = ConcurrentLinkedQueue<WorldLinkFrameSpecs.ServerKickPayload>()

    private val pendingCentralMuteUpdates =
        ConcurrentLinkedQueue<WorldLinkFrameSpecs.ServerMuteUpdatePayload>()

    private val pendingCentralReboots = ConcurrentLinkedQueue<WorldLinkFrameSpecs.ServerRebootPayload>()

    private val pendingCentralBroadcasts =
        ConcurrentLinkedQueue<WorldLinkFrameSpecs.ServerBroadcastPayload>()

    private val pendingCentralDisplayNameSyncs =
        ConcurrentLinkedQueue<WorldLinkFrameSpecs.ServerDisplayNameSyncPayload>()

    @Volatile
    private var inboundWatchStop: Boolean = true

    private var inboundWatchThread: Thread? = null

    public val isEnabled: Boolean
        get() = settings != null

    public fun authenticate(
        loginUsername: String,
        password: CharArray,
        loginCharacterId: Int? = null,
    ): CentralAuthResult {
        val cfg = settings ?: return CentralAuthResult.Skipped
        repeat(MAX_AUTH_ATTEMPTS) { attempt ->
            try {
                return openCentralAuthSession(cfg, loginUsername, password, loginCharacterId)
            } catch (e: IllegalStateException) {
                return CentralAuthResult.Denied(LoginResponse.LoginServerNoReply)
            } catch (e: Exception) {
                if (!isRetryableCentralNetworkFailure(e)) {
                    return CentralAuthResult.Denied(LoginResponse.LoginServerNoReply)
                }
                if (attempt + 1 >= MAX_AUTH_ATTEMPTS) {
                    return@repeat
                }
                val backoff = RETRY_BASE_MS * (attempt + 1)
                try {
                    Thread.sleep(backoff)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return CentralAuthResult.Denied(LoginResponse.LoginServerOffline)
                }
            }
        }
        return CentralAuthResult.Denied(LoginResponse.LoginServerOffline)
    }

    public fun startInboundWatch() {
        val cfg = settings ?: return
        if (!inboundWatchStop) {
            return
        }
        inboundWatchStop = false
        inboundWatchThread =
            Thread(
                { runInboundWatchLoop(cfg) },
                "openrune-central-worldlink-inbound",
            ).apply {
                isDaemon = true
                start()
            }
    }

    public fun stopInboundWatch() {
        inboundWatchStop = true
        inboundWatchThread?.interrupt()
        inboundWatchThread = null
    }

    public fun drainInboundRevokesOnGameThread(
        playerRegistry: PlayerRegistry,
        gameUpdate: GameUpdate,
    ) {
        while (true) {
            val p = pendingCentralRevokes.poll() ?: break
            playerRegistry.disconnectPlayersForCentralRevoke(p.accountId, p.characterId)
        }
        while (true) {
            val p = pendingCentralKicks.poll() ?: break
            playerRegistry.disconnectPlayersForCentralKick(p.accountId, p.characterId)
        }
        while (true) {
            val update = pendingCentralMuteUpdates.poll() ?: break
            playerRegistry.applyCentralMuteUpdate(
                update.accountId,
                update.characterId,
                update.mutedUntilEpochMillis,
            )
        }
        while (true) {
            val op = pendingCentralReboots.poll() ?: break
            if (op.worldScope != 0 && op.worldScope != serverConfig.world) {
                continue
            }
            if (op.clear) {
                if (gameUpdate.state.isUpdating()) {
                    continue
                }
                try {
                    playerRegistry.forEachOnline { player ->
                        MiscOutput.clearUpdateRebootTimer(player)
                    }
                    gameUpdate.reset()
                } catch (_: Exception) {
                }
                continue
            }
            val now = System.currentTimeMillis()
            val msLeft = op.rebootAtMs - now
            if (msLeft <= 0L) {
                continue
            }
            val cycles = (msLeft / GAME_CYCLE_MS).toInt().coerceIn(1, 65535)
            try {
                if (!gameUpdate.state.isUpdating()) {
                    gameUpdate.startCountdown(cycles)
                }
            } catch (_: Exception) {
            }
            playerRegistry.forEachOnline { player ->
                MiscOutput.updateRebootTimer(player, cycles, op.message)
            }
        }
        while (true) {
            val b = pendingCentralBroadcasts.poll() ?: break
            if (b.worldScope != 0 && b.worldScope != serverConfig.world) {
                continue
            }
            playerRegistry.forEachOnline { player ->
                player.mes(b.message, ChatType.Broadcast)
            }
        }
        while (true) {
            val d = pendingCentralDisplayNameSyncs.poll() ?: break
            playerRegistry.applyCentralDisplayNameSync(d.accountId, d.characterId, d.newDisplayName, d.priorDisplayName)
        }
    }

    public fun notifyLogout(sessionToken: ByteArray) {
        val cfg = settings ?: return
        repeat(MAX_LOGOUT_ATTEMPTS) { attempt ->
            try {
                val session =
                    WorldLinkNettyBlockingClient.connect(
                        InetSocketAddress(cfg.host, cfg.port),
                        readIdleSeconds = SOCKET_TIMEOUT_SECONDS,
                    )
                try {
                    sendHello(session, cfg.worldKey, serverConfig.world)
                    sendLogout(session, sessionToken)
                } finally {
                    session.close()
                }
                return
            } catch (e: Exception) {
                if (!isRetryableCentralNetworkFailure(e) || attempt + 1 >= MAX_LOGOUT_ATTEMPTS) {
                    return
                }
                try {
                    Thread.sleep(RETRY_BASE_MS * (attempt + 1))
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return
                }
            }
        }
    }

    private fun openCentralAuthSession(
        cfg: CentralSettings,
        loginUsername: String,
        password: CharArray,
        loginCharacterId: Int?,
    ): CentralAuthResult {
        val session =
            WorldLinkNettyBlockingClient.connect(
                InetSocketAddress(cfg.host, cfg.port),
                readIdleSeconds = SOCKET_TIMEOUT_SECONDS,
            )
        return try {
            sendHello(session, cfg.worldKey, serverConfig.world)
            sendLogin(session, loginUsername, password, loginCharacterId)
        } finally {
            session.close()
        }
    }

    private fun sendHello(
        session: WorldLinkNettyBlockingSession,
        worldKey: ByteArray,
        worldId: Int,
    ) {
        session.send(WorldLinkPackets.worldHello(worldId, worldKey))
        val response = session.recvInbound(SOCKET_TIMEOUT_MS.toLong())
        when (val op = response[0].toInt() and 0xFF) {
            WorldLinkFrameSpecs.OP_HELLO_ACK -> return
            WorldLinkFrameSpecs.OP_HELLO_REJECT -> {
                val reason = if (response.size > 1) response[1].toInt() and 0xFF else -1
                error(helloRejectMessage(reason, worldId))
            }
            else ->
                unexpectedCentralOp(
                    op,
                    listOf(WorldLinkFrameSpecs.OP_HELLO_ACK, WorldLinkFrameSpecs.OP_HELLO_REJECT),
                )
        }
    }

    private fun sendPushSubscribe(session: WorldLinkNettyBlockingSession) {
        session.send(byteArrayOf(WorldLinkFrameSpecs.OP_PUSH_SUBSCRIBE.toByte()))
        val response = session.recvInbound(SOCKET_TIMEOUT_MS.toLong())
        val op = response[0].toInt() and 0xFF
        if (op != WorldLinkFrameSpecs.OP_PUSH_SUBSCRIBE_ACK) {
            unexpectedCentralOp(op, listOf(WorldLinkFrameSpecs.OP_PUSH_SUBSCRIBE_ACK))
        }
    }

    private fun sendLogin(
        session: WorldLinkNettyBlockingSession,
        username: String,
        password: CharArray,
        loginCharacterId: Int?,
    ): CentralAuthResult {
        session.send(WorldLinkPackets.login(username, password, loginCharacterId))
        val response = session.recvInbound(SOCKET_TIMEOUT_MS.toLong())
        val invalid = WorldLinkFrameSpecs.validateCentralToGameFrame(response)
        if (invalid != null) {
            logger.warn {
                "Central LOGIN reply failed validation: " +
                    WorldLinkFrameSpecs.describeValidationFailure(invalid)
            }
            return CentralAuthResult.Denied(LoginResponse.UnknownReplyFromLoginServer)
        }
        when (val op = response[0].toInt() and 0xFF) {
            WorldLinkFrameSpecs.OP_LOGIN_OK -> return parseLoginOk(response)
            WorldLinkFrameSpecs.OP_LOGIN_FAIL -> {
                val buf = ByteBuffer.wrap(response)
                buf.get() // opcode
                if (buf.remaining() < 4) {
                    return CentralAuthResult.Denied(LoginResponse.UnknownReplyFromLoginServer)
                }
                val code = buf.int
                val script =
                    if (!buf.hasRemaining()) {
                        null
                    } else {
                        val dup = buf.duplicate()
                        val parsed = readLoginFailScriptTrailer(dup)
                        if (parsed == null || dup.hasRemaining()) {
                            return CentralAuthResult.Denied(LoginResponse.UnknownReplyFromLoginServer)
                        }
                        buf.position(dup.position())
                        parsed
                    }
                if (buf.hasRemaining()) {
                    return CentralAuthResult.Denied(LoginResponse.UnknownReplyFromLoginServer)
                }
                return CentralAuthResult.Denied(mapLoginFail(code, script))
            }
            else -> return CentralAuthResult.Denied(LoginResponse.UnknownReplyFromLoginServer)
        }
    }

    private fun parseLoginOk(response: ByteArray): CentralAuthResult {
        val buf = ByteBuffer.wrap(response)
        buf.get() // opcode
        val tokenLen = buf.short.toInt() and 0xFFFF
        if (tokenLen != WorldLinkFrameSpecs.TOKEN_BYTES || buf.remaining() < tokenLen + Long.SIZE_BYTES) {
            return CentralAuthResult.Denied(LoginResponse.UnknownReplyFromLoginServer)
        }
        val token = ByteArray(tokenLen)
        buf.get(token)
        buf.long // central account id (game uses local account by username)
        val centralRights =
            if (buf.remaining() >= 2) {
                val rightsLen = buf.short.toInt() and 0xFFFF
                if (rightsLen > buf.remaining()) {
                    return CentralAuthResult.Denied(LoginResponse.UnknownReplyFromLoginServer)
                }
                if (rightsLen == 0) {
                    ""
                } else {
                    val rightsBytes = ByteArray(rightsLen)
                    buf.get(rightsBytes)
                    String(rightsBytes, StandardCharsets.UTF_8)
                }
            } else {
                ""
            }
        return CentralAuthResult.Ok(token, centralRights)
    }

    private fun helloRejectMessage(
        reason: Int,
        worldId: Int,
    ): String {
        val detail =
            when (reason) {
                1 ->
                    "protocol (magic, version, or key frame layout) — ensure game server and Central are compatible"
                2 ->
                    "bad world key — Central expects the same UTF-8 string as `central.world-key` " +
                        "(or `OPENRUNE_WORLD_KEY` on Central when `worlds.world_key_sha256` is NULL). " +
                        "Use the admin “world key generate” value for world_id=$worldId, or set matching keys on both sides."
                3 -> "unknown world_id=$worldId (no row in Central `world`)"
                4 -> "world disabled on Central"
                else -> "code=$reason"
            }
        return "HELLO_REJECT reason=$reason ($detail)"
    }

    private fun sendLogout(
        session: WorldLinkNettyBlockingSession,
        sessionToken: ByteArray,
    ) {
        session.send(WorldLinkPackets.logout(sessionToken))
        val response = session.recvInbound(SOCKET_TIMEOUT_MS.toLong())
        val op = response[0].toInt() and 0xFF
        if (op != WorldLinkFrameSpecs.OP_LOGOUT_ACK) {
            unexpectedCentralOp(op, listOf(WorldLinkFrameSpecs.OP_LOGOUT_ACK))
        }
    }

    private fun readLoginFailScriptTrailer(buf: ByteBuffer): Triple<String, String, String>? {
        fun readLine(): String? {
            if (buf.remaining() < 2) {
                return null
            }
            val len = buf.short.toInt() and 0xFFFF
            if (len > WorldLinkFrameSpecs.LOGIN_FAIL_SCRIPT_LINE_MAX_UTF8_BYTES) {
                return null
            }
            if (buf.remaining() < len) {
                return null
            }
            val bytes = ByteArray(len)
            buf.get(bytes)
            return String(bytes, StandardCharsets.UTF_8)
        }
        val l1 = readLine() ?: return null
        val l2 = readLine() ?: return null
        val l3 = readLine() ?: return null
        return Triple(l1, l2, l3)
    }

    private fun mapLoginFail(
        code: Int,
        script: Triple<String, String, String>?,
    ): LoginResponse =
        when (code) {
            1 -> LoginResponse.InvalidUsernameOrPassword
            2 -> LoginResponse.ServerFull
            3 -> LoginResponse.Duplicate
            8 -> LoginResponse.Banned
            9 -> LoginResponse.InvalidUsernameOrPassword
            10 -> LoginResponse.Locked
            11 -> LoginResponse.UpdateInProgress
            in 12..15 ->
                if (script != null) {
                    // Central (v5+) sends the three lines; do not duplicate wording on the game server.
                    LoginResponse.DisallowedByScript(script.first, script.second, script.third)
                } else {
                    // World-link v4 or older Central: body is code only — no per-denial copy from Central.
                    LoginResponse.DisallowedByScript(
                        "You cannot log in to this world.",
                        "",
                        "",
                    )
                }
            else -> LoginResponse.InvalidUsernameOrPassword
        }

    private fun runInboundWatchLoop(cfg: CentralSettings) {
        while (!inboundWatchStop && !Thread.currentThread().isInterrupted) {
            var session: WorldLinkNettyBlockingSession? = null
            try {
                session =
                    WorldLinkNettyBlockingClient.connect(
                        InetSocketAddress(cfg.host, cfg.port),
                        readIdleSeconds = null,
                    )
                sendHello(session, cfg.worldKey, serverConfig.world)
                sendPushSubscribe(session)
                while (!inboundWatchStop && !Thread.currentThread().isInterrupted) {
                    val frame = session.pollInbound(INBOUND_POLL_MS) ?: continue
                    if (frame.isEmpty()) {
                        break
                    }
                    val invalid = WorldLinkFrameSpecs.validateCentralToGameFrame(frame)
                    if (invalid != null) {
                        logger.warn {
                            "Dropping invalid Central push frame: " +
                                WorldLinkFrameSpecs.describeValidationFailure(invalid)
                        }
                        continue
                    }
                    frame.dispatchCentralServerPush(
                        onRevoke = { pendingCentralRevokes.add(it) },
                        onMute = { pendingCentralMuteUpdates.add(it) },
                        onKick = { pendingCentralKicks.add(it) },
                        onReboot = { pendingCentralReboots.add(it) },
                        onBroadcast = { pendingCentralBroadcasts.add(it) },
                        onDisplayNameSync = { pendingCentralDisplayNameSyncs.add(it) },
                        onOther = { op ->
                            logger.debug {
                                "Ignoring Central world-link opcode 0x${op.toString(16)} on push channel " +
                                    "(not a server push)"
                            }
                        },
                    )
                }
            } catch (e: Exception) {
                if (inboundWatchStop) {
                    break
                }
                try {
                    Thread.sleep(INBOUND_RECONNECT_MS)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            } finally {
                try {
                    session?.close()
                } catch (_: Exception) {
                }
            }
        }
    }

    private data class CentralSettings(
        val host: String,
        val port: Int,
        val worldKey: ByteArray,
    ) {
        companion object {
            fun resolve(config: ServerConfig): CentralSettings? {
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
                val worldKeyBytes =
                    (keyStr ?: "").toByteArray(StandardCharsets.UTF_8)
                return CentralSettings(host, port, worldKeyBytes)
            }
        }
    }

    private companion object {
        private const val SOCKET_TIMEOUT_MS: Int = 15_000

        private const val SOCKET_TIMEOUT_SECONDS: Int = SOCKET_TIMEOUT_MS / 1000

        private const val MAX_AUTH_ATTEMPTS: Int = 6

        private const val MAX_LOGOUT_ATTEMPTS: Int = 3
        private const val RETRY_BASE_MS: Long = 200L

        private const val INBOUND_RECONNECT_MS: Long = 1_500L

        private const val INBOUND_POLL_MS: Long = 1_000L

        private const val GAME_CYCLE_MS: Long = 600L
    }
}

private fun isRetryableCentralNetworkFailure(e: Throwable): Boolean =
    e is java.net.ConnectException ||
        e is java.net.SocketTimeoutException ||
        e is java.net.UnknownHostException ||
        (e is java.net.SocketException &&
            (e.message?.contains("reset", ignoreCase = true) == true ||
                e.message?.contains("broken pipe", ignoreCase = true) == true))

public sealed class CentralAuthResult {
    public data object Skipped : CentralAuthResult()

    public data class Ok(
        val sessionToken: ByteArray,
        val centralRights: String = "",
    ) : CentralAuthResult()

    public data class Denied(
        val response: LoginResponse,
    ) : CentralAuthResult()
}
