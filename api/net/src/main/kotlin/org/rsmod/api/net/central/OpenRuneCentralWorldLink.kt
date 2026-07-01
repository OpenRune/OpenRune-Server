package org.rsmod.api.net.central

import com.github.michaelbull.logging.InlineLogger
import dev.or2.central.auth.PasswordAuthConfig
import dev.or2.central.auth.PasswordHasher
import dev.or2.central.worldlink.protocol.WorldOpcodes
import dev.or2.central.worldlink.protocol.packets.outgoing.impl.HelloAckPacket
import dev.or2.central.worldlink.protocol.social.PmTraceLog
import dev.or2.central.worldlink.protocol.social.SocialPackets
import dev.or2.central.worldlink.protocol.social.SocialSyncSnapshot
import jakarta.inject.Inject
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
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
        ConcurrentLinkedQueue<ServerRevokeLoginPayload>()

    private val pendingCentralKicks = ConcurrentLinkedQueue<ServerKickPayload>()

    private val pendingCentralMuteUpdates =
        ConcurrentLinkedQueue<ServerMuteUpdatePayload>()

    private val pendingCentralReboots = ConcurrentLinkedQueue<ServerRebootPayload>()

    private val pendingCentralBroadcasts =
        ConcurrentLinkedQueue<ServerBroadcastPayload>()

    private val pendingCentralDisplayNameSyncs =
        ConcurrentLinkedQueue<ServerDisplayNameSyncPayload>()

    private val pendingCentralDiscordIdSyncs =
        ConcurrentLinkedQueue<ServerDiscordIdSyncPayload>()

    private val pendingCentralPrivateMessages =
        ConcurrentLinkedQueue<ServerPrivateMessagePayload>()

    /** PMs received before the recipient is on the game thread player list — retried each tick. */
    private val deferredPrivateMessagesByCharacter =
        ConcurrentHashMap<Int, ConcurrentLinkedQueue<ServerPrivateMessagePayload>>()

    private val pendingCentralFriendPresence =
        ConcurrentLinkedQueue<ServerFriendPresencePayload>()

    @Volatile
    private var passwordAuthConfig: PasswordAuthConfig? = null

    @Volatile
    private var inboundWatchStop: Boolean = true

    private var inboundWatchThread: Thread? = null

    private val authExecutor =
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "openrune-central-auth").apply { isDaemon = true }
        }

    public val isEnabled: Boolean
        get() = settings != null

    /** Seeds password hashing before the first HELLO (embedded Central uses the same config as Central). */
    public fun applyPasswordAuth(config: PasswordAuthConfig) {
        passwordAuthConfig = config
    }

    public fun passwordHasher(): PasswordHasher = (passwordAuthConfig ?: PasswordAuthConfig.DEFAULT).toHasher()

    /**
     * Starts Central auth on a background thread so account DB loading can overlap wall-clock time.
     * [awaitInflight] must be called exactly once on the returned handle.
     */
    public fun beginAuthenticate(
        loginUsername: String,
        password: CharArray,
        loginCharacterId: Int? = null,
    ): InflightCentralAuth? {
        if (settings == null) {
            return null
        }
        val passwordCopy = password.copyOf()
        val future =
            CompletableFuture.supplyAsync(
                {
                    try {
                        authenticate(loginUsername, passwordCopy, loginCharacterId)
                    } finally {
                        passwordCopy.fill('\u0000')
                    }
                },
                authExecutor,
            )
        return InflightCentralAuth(future)
    }

    public fun awaitInflight(inflight: InflightCentralAuth): CentralAuthResult = inflight.future.join()

    public fun authenticate(
        loginUsername: String,
        password: CharArray,
        loginCharacterId: Int? = null,
    ): CentralAuthResult {
        val cfg = settings ?: return CentralAuthResult.Skipped
        val startedAt = System.nanoTime()
        var attempts = 0
        repeat(MAX_AUTH_ATTEMPTS) { attempt ->
            attempts = attempt + 1
            try {
                val result = openCentralAuthSession(cfg, loginUsername, password, loginCharacterId)
                logAuthTiming(loginUsername, elapsedMs(startedAt), attempts, result)
                return result
            } catch (e: IllegalStateException) {
                logAuthTiming(loginUsername, elapsedMs(startedAt), attempts, null, e)
                return CentralAuthResult.Denied(LoginResponse.LoginServerNoReply)
            } catch (e: Exception) {
                if (!isRetryableCentralNetworkFailure(e)) {
                    logAuthTiming(loginUsername, elapsedMs(startedAt), attempts, null, e)
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
                    logAuthTiming(loginUsername, elapsedMs(startedAt), attempts, null, e)
                    return CentralAuthResult.Denied(LoginResponse.LoginServerOffline)
                }
            }
        }
        logAuthTiming(loginUsername, elapsedMs(startedAt), attempts, null)
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

    public fun pollPrivateMessage(): ServerPrivateMessagePayload? = pendingCentralPrivateMessages.poll()

    public fun pollFriendPresence(): ServerFriendPresencePayload? = pendingCentralFriendPresence.poll()

    public fun addFriend(
        characterId: Int,
        targetName: String,
    ): CentralSocialResult = runSocialAction(WorldLinkPackets.friendAdd(characterId, targetName))

    public fun deleteFriend(
        characterId: Int,
        targetName: String,
    ): CentralSocialResult = runSocialAction(WorldLinkPackets.friendDel(characterId, targetName))

    public fun addIgnore(
        characterId: Int,
        targetName: String,
    ): CentralSocialResult = runSocialAction(WorldLinkPackets.ignoreAdd(characterId, targetName))

    public fun deleteIgnore(
        characterId: Int,
        targetName: String,
    ): CentralSocialResult = runSocialAction(WorldLinkPackets.ignoreDel(characterId, targetName))

    public fun sendPrivateMessage(
        fromCharacterId: Int,
        targetName: String,
        senderDisplayName: String,
        senderCrown: Int,
        message: String,
    ): CentralSocialResult {
        pmTrace(
            stage = PmTraceLog.STAGE_GAME_SEND,
            fromCharacterId = fromCharacterId,
            targetName = targetName,
            messageLen = message.length,
        )
        val result =
            runSocialAction(
                WorldLinkPackets.pmRelay(
                    SocialPackets.PmRelayPayload(
                        fromCharacterId = fromCharacterId,
                        senderCrown = senderCrown,
                        targetName = targetName,
                        senderDisplayName = senderDisplayName,
                        message = message,
                    ),
                ),
            )
        tracePmSendResult(result, fromCharacterId, targetName, message.length)
        return result
    }

    public fun setPrivateChatFilter(
        characterId: Int,
        privateChatFilter: Int,
    ): CentralSocialResult =
        runSocialAction(
            WorldLinkPackets.privateChatFilter(characterId, privateChatFilter),
        )

    public fun setChatFilters(
        characterId: Int,
        publicChat: Int,
        privateChat: Int,
        tradeChat: Int,
    ): CentralSocialResult =
        runSocialAction(
            WorldLinkPackets.chatFilters(characterId, publicChat, privateChat, tradeChat),
        )

    public fun socialSnapshot(
        characterId: Int,
    ): CentralSocialSnapshotResult {
        if (!isEnabled || characterId <= 0) {
            return CentralSocialSnapshotResult.Failed("Social is not available right now.")
        }
        return when (val result = requestSocialSync(characterId)) {
            is SocialSyncResult.Ok -> CentralSocialSnapshotResult.Ok(result.snapshot.toClientSnapshot())
            is SocialSyncResult.Fail ->
                CentralSocialSnapshotResult.Failed(socialFailMessage(result.reason))
            SocialSyncResult.Unavailable ->
                CentralSocialSnapshotResult.Failed("Unable to load social list right now.")
        }
    }

    public fun requestSocialSync(
        characterId: Int,
    ): SocialSyncResult {
        val cfg = settings ?: return SocialSyncResult.Unavailable
        if (characterId <= 0) {
            return SocialSyncResult.Unavailable
        }
        repeat(MAX_AUTH_ATTEMPTS) { attempt ->
            try {
                return runCentralSocialRoundTrip(cfg) { session ->
                    session.send(WorldLinkPackets.socialSync(characterId))
                    parseSocialSyncResponse(session.recvInbound(SOCKET_TIMEOUT_MS.toLong()))
                }
            } catch (e: Exception) {
                if (!isRetryableCentralNetworkFailure(e) || attempt + 1 >= MAX_AUTH_ATTEMPTS) {
                    logger.warn(e) { "Central social sync failed for characterId=$characterId" }
                    return SocialSyncResult.Unavailable
                }
                try {
                    Thread.sleep(RETRY_BASE_MS * (attempt + 1))
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return SocialSyncResult.Unavailable
                }
            }
        }
        return SocialSyncResult.Unavailable
    }

    public fun requestSocialAction(frame: ByteArray): SocialActionResult {
        val cfg = settings ?: return SocialActionResult.Unavailable
        return try {
            runCentralSocialRoundTrip(cfg) { session ->
                session.send(frame)
                parseSocialActionResponse(session.recvInbound(SOCKET_TIMEOUT_MS.toLong()))
            }
        } catch (e: Exception) {
            logger.warn(e) { "Central social action failed" }
            SocialActionResult.Unavailable
        }
    }

    public fun drainInboundSocialOnGameThread(playerRegistry: PlayerRegistry) {
        drainDeferredPrivateMessages(playerRegistry)
        while (true) {
            val push = pollPrivateMessage() ?: break
            deliverPrivateMessage(playerRegistry, push)
        }
        while (true) {
            val push = pollFriendPresence() ?: break
            val player = playerRegistry.findOnlineByCharacterId(push.ownerCharacterId) ?: continue
            player.writeCentralFriendPresence(push, serverConfig)
        }
    }

    private fun drainDeferredPrivateMessages(playerRegistry: PlayerRegistry) {
        for ((characterId, queue) in deferredPrivateMessagesByCharacter) {
            val player = playerRegistry.findOnlineByCharacterId(characterId) ?: continue
            while (true) {
                val push = queue.poll() ?: break
                player.writeCentralPrivateMessage(push, serverConfig)
            }
            if (queue.isEmpty()) {
                deferredPrivateMessagesByCharacter.remove(characterId, queue)
            }
        }
    }

    private fun deliverPrivateMessage(
        playerRegistry: PlayerRegistry,
        push: ServerPrivateMessagePayload,
    ) {
        val player = playerRegistry.findOnlineByCharacterId(push.toCharacterId)
        if (player == null) {
            pmTrace(
                stage = PmTraceLog.STAGE_GAME_DEFERRED,
                fromCharacterId = push.fromCharacterId,
                toCharacterId = push.toCharacterId,
                messageLen = push.message.length,
            )
            val queue =
                deferredPrivateMessagesByCharacter.computeIfAbsent(push.toCharacterId) {
                    ConcurrentLinkedQueue()
                }
            if (queue.size < MAX_DEFERRED_PRIVATE_MESSAGES_PER_CHARACTER) {
                queue.add(push)
            } else {
                logger.warn {
                    "Dropping private message to characterId=${push.toCharacterId}: deferred queue full"
                }
            }
            return
        }
        player.writeCentralPrivateMessage(push, serverConfig)
    }

    private fun runSocialAction(frame: ByteArray): CentralSocialResult {
        if (!isEnabled) {
            return CentralSocialResult.Failed("Social is not available right now.")
        }
        return when (val result = requestSocialAction(frame)) {
            SocialActionResult.Ok -> CentralSocialResult.Ok
            is SocialActionResult.Fail -> CentralSocialResult.Failed(socialFailMessage(result.reason))
            SocialActionResult.Unavailable ->
                CentralSocialResult.Failed("Unable to contact friends service.")
        }
    }

    public sealed class CentralSocialSnapshotResult {
        public data class Ok(
            val snapshot: WorldLinkFrameSpecs.CentralSocialSnapshot,
        ) : CentralSocialSnapshotResult()

        public data class Failed(
            val message: String,
        ) : CentralSocialSnapshotResult()
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
        while (true) {
            val d = pendingCentralDiscordIdSyncs.poll() ?: break
            playerRegistry.applyCentralDiscordSync(d.accountId, d.discordId)
        }
    }

    public fun touchSession(sessionToken: ByteArray): Boolean {
        val cfg = settings ?: return false
        return try {
            runCentralSocialRoundTrip(cfg) { session ->
                session.send(WorldLinkPackets.heartbeat(sessionToken))
                val response = session.recvInbound(SOCKET_TIMEOUT_MS.toLong())
                response[0].toInt() and 0xFF == WorldLinkFrameSpecs.OP_HEARTBEAT_ACK
            }
        } catch (e: Exception) {
            logger.debug(e) { "Central session heartbeat failed" }
            false
        }
    }

    public fun heartbeatOnlineSessions(sessionTokens: Collection<ByteArray>) {
        if (!isEnabled || sessionTokens.isEmpty()) {
            return
        }
        authExecutor.execute {
            for (token in sessionTokens) {
                touchSession(token)
            }
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

    private fun <T> runCentralSocialRoundTrip(
        cfg: CentralSettings,
        block: (WorldLinkNettyBlockingSession) -> T,
    ): T {
        val session =
            WorldLinkNettyBlockingClient.connect(
                InetSocketAddress(cfg.host, cfg.port),
                readIdleSeconds = SOCKET_TIMEOUT_SECONDS,
            )
        return try {
            sendHello(session, cfg.worldKey, serverConfig.world)
            block(session)
        } finally {
            session.close()
        }
    }

    private fun decodeSocialSyncWire(response: ByteArray): SocialSyncSnapshot =
        dev.or2.central.worldlink.protocol.readInboundFrame(response).use { reader ->
            SocialPackets.decodeSocialSyncOk(reader)
        }

    private fun parseSocialSyncResponse(response: ByteArray): SocialSyncResult {
        val invalid = WorldLinkFrameSpecs.validateCentralToGameFrame(response)
        if (invalid != null) {
            logger.warn {
                "Central SOCIAL_SYNC reply failed validation: " +
                    WorldLinkFrameSpecs.describeValidationFailure(invalid)
            }
            return SocialSyncResult.Unavailable
        }
        return when (val op = response[0].toInt() and 0xFF) {
            WorldLinkFrameSpecs.OP_WORLD_SOCIAL_SYNC_OK ->
                SocialSyncResult.Ok(decodeSocialSyncWire(response))
            WorldLinkFrameSpecs.OP_WORLD_SOCIAL_SYNC_FAIL -> {
                val reason = if (response.size > 1) response[1].toInt() and 0xFF else WorldOpcodes.SOCIAL_FAIL_NOT_ALLOWED
                SocialSyncResult.Fail(reason)
            }
            WorldLinkFrameSpecs.OP_WORLD_SOCIAL_FAIL -> {
                val reason = if (response.size > 1) response[1].toInt() and 0xFF else WorldOpcodes.SOCIAL_FAIL_NOT_ALLOWED
                SocialSyncResult.Fail(reason)
            }
            else -> {
                unexpectedCentralOp(
                    op,
                    listOf(
                        WorldLinkFrameSpecs.OP_WORLD_SOCIAL_SYNC_OK,
                        WorldLinkFrameSpecs.OP_WORLD_SOCIAL_SYNC_FAIL,
                    ),
                )
            }
        }
    }

    private fun parseSocialActionResponse(response: ByteArray): SocialActionResult {
        val invalid = WorldLinkFrameSpecs.validateCentralToGameFrame(response)
        if (invalid != null) {
            logger.warn {
                "Central social action reply failed validation: " +
                    WorldLinkFrameSpecs.describeValidationFailure(invalid)
            }
            return SocialActionResult.Unavailable
        }
        return when (val op = response[0].toInt() and 0xFF) {
            WorldLinkFrameSpecs.OP_WORLD_SOCIAL_OK -> SocialActionResult.Ok
            WorldLinkFrameSpecs.OP_WORLD_SOCIAL_FAIL -> {
                val reason = if (response.size > 1) response[1].toInt() and 0xFF else WorldOpcodes.SOCIAL_FAIL_NOT_ALLOWED
                SocialActionResult.Fail(reason)
            }
            else -> {
                unexpectedCentralOp(
                    op,
                    listOf(WorldLinkFrameSpecs.OP_WORLD_SOCIAL_OK, WorldLinkFrameSpecs.OP_WORLD_SOCIAL_FAIL),
                )
            }
        }
    }

    private fun openCentralAuthSession(
        cfg: CentralSettings,
        loginUsername: String,
        password: CharArray,
        loginCharacterId: Int?,
    ): CentralAuthResult {
        val connectStart = System.nanoTime()
        val session =
            WorldLinkNettyBlockingClient.connect(
                InetSocketAddress(cfg.host, cfg.port),
                readIdleSeconds = SOCKET_TIMEOUT_SECONDS,
            )
        val connectMs = elapsedMs(connectStart)
        return try {
            val helloStart = System.nanoTime()
            sendHello(session, cfg.worldKey, serverConfig.world)
            val helloMs = elapsedMs(helloStart)
            val loginStart = System.nanoTime()
            val result = sendLogin(session, loginUsername, password, loginCharacterId)
            val loginMs = elapsedMs(loginStart)
            logWorldLinkRoundTrip(loginUsername, connectMs, helloMs, loginMs)
            result
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
            WorldLinkFrameSpecs.OP_HELLO_ACK -> {
                passwordAuthConfig = HelloAckPacket.decodeAuthConfig(response)
                return
            }
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
                        onDiscordIdSync = { pendingCentralDiscordIdSyncs.add(it) },
                        onPrivateMessage = { push ->
                            pmTrace(
                                stage = PmTraceLog.STAGE_GAME_INBOUND,
                                fromCharacterId = push.fromCharacterId,
                                toCharacterId = push.toCharacterId,
                                messageLen = push.message.length,
                            )
                            pendingCentralPrivateMessages.add(push)
                        },
                        onFriendPresence = { pendingCentralFriendPresence.add(it) },
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

        private const val MAX_DEFERRED_PRIVATE_MESSAGES_PER_CHARACTER: Int = 64

        private const val GAME_CYCLE_MS: Long = 600L

        private const val AUTH_TIMING_INFO_MS: Long = 100L

        private const val AUTH_TIMING_WARN_MS: Long = 500L
    }

    private fun elapsedMs(startNanos: Long): Long =
        java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos)

    private fun pmTrace(
        stage: String,
        fromCharacterId: Int,
        toCharacterId: Int? = null,
        targetName: String? = null,
        messageLen: Int,
        extra: String = "",
    ) {
        if (!serverConfig.socialPmTraceLogs) {
            return
        }
        logger.info {
            PmTraceLog.format(
                stage = stage,
                fromCharacterId = fromCharacterId,
                toCharacterId = toCharacterId,
                targetName = targetName,
                messageLen = messageLen,
                extra = extra,
            )
        }
    }

    private fun tracePmSendResult(
        result: CentralSocialResult,
        fromCharacterId: Int,
        targetName: String,
        messageLen: Int,
    ) {
        when (result) {
            CentralSocialResult.Ok ->
                pmTrace(
                    stage = PmTraceLog.STAGE_GAME_CENTRAL_OK,
                    fromCharacterId = fromCharacterId,
                    targetName = targetName,
                    messageLen = messageLen,
                )
            is CentralSocialResult.Failed ->
                pmTrace(
                    stage = PmTraceLog.STAGE_GAME_CENTRAL_FAIL,
                    fromCharacterId = fromCharacterId,
                    targetName = targetName,
                    messageLen = messageLen,
                    extra = result.message,
                )
            CentralSocialResult.Ignored -> Unit
        }
    }

    private fun logWorldLinkRoundTrip(
        username: String,
        connectMs: Long,
        helloMs: Long,
        loginMs: Long,
    ) {
        if (!serverConfig.loginTimingLogs) {
            return
        }
        val totalMs = connectMs + helloMs + loginMs
        val message =
            "Central world-link auth user=$username connect=${connectMs}ms hello=${helloMs}ms " +
                "login=${loginMs}ms total=${totalMs}ms"
        when {
            totalMs >= AUTH_TIMING_WARN_MS -> logger.warn { message }
            totalMs >= AUTH_TIMING_INFO_MS -> logger.info { message }
            else -> logger.debug { message }
        }
    }

    private fun logAuthTiming(
        username: String,
        totalMs: Long,
        attempts: Int,
        result: CentralAuthResult?,
        error: Throwable? = null,
    ) {
        if (!serverConfig.loginTimingLogs) {
            return
        }
        if (attempts <= 1 && error == null && (result is CentralAuthResult.Ok || result is CentralAuthResult.Skipped)) {
            return
        }
        val outcome =
            when (result) {
                is CentralAuthResult.Ok -> "ok"
                is CentralAuthResult.Denied -> "denied"
                CentralAuthResult.Skipped -> "skipped"
                null -> "failed"
            }
        val message =
            "Central world-link authenticate user=$username outcome=$outcome attempts=$attempts total=${totalMs}ms"
        if (error != null || totalMs >= AUTH_TIMING_WARN_MS) {
            logger.warn(error) { message }
        } else {
            logger.info { message }
        }
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

/** In-flight Central auth started via [OpenRuneCentralWorldLink.beginAuthenticate]. */
public class InflightCentralAuth internal constructor(
    internal val future: CompletableFuture<CentralAuthResult>,
)

public sealed class SocialSyncResult {
    public data class Ok(val snapshot: SocialSyncSnapshot) : SocialSyncResult()

    public data class Fail(val reason: Int) : SocialSyncResult()

    public data object Unavailable : SocialSyncResult()
}

public sealed class SocialActionResult {
    public data object Ok : SocialActionResult()

    public data class Fail(val reason: Int) : SocialActionResult()

    public data object Unavailable : SocialActionResult()
}

private fun SocialSyncSnapshot.toClientSnapshot(): WorldLinkFrameSpecs.CentralSocialSnapshot =
    WorldLinkFrameSpecs.CentralSocialSnapshot(
        publicChat = publicChat,
        privateChat = privateChat,
        tradeChat = tradeChat,
        friends =
            friends.map { friend ->
                WorldLinkFrameSpecs.CentralSocialFriend(
                    displayName = friend.displayName,
                    previousDisplayName = friend.previousDisplayName,
                    worldId = friend.worldId,
                )
            },
        ignores =
            ignores.map { ignore ->
                WorldLinkFrameSpecs.CentralSocialIgnore(
                    displayName = ignore.displayName,
                    previousDisplayName = ignore.previousDisplayName,
                )
            },
    )
