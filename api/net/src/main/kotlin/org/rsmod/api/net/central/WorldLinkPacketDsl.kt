package org.rsmod.api.net.central

import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.charset.StandardCharsets

public data class WorldLinkSession(
    public val outgoing: WorldLinkOutgoing,
    public val incoming: WorldLinkIncoming,
)

public inline fun <T> worldLinkSession(
    out: DataOutputStream,
    input: DataInputStream,
    block: WorldLinkSession.() -> T,
): T {
    val session = WorldLinkSession(WorldLinkOutgoing(out), WorldLinkIncoming(input))
    return session.block()
}

public inline fun <T> DataOutputStream.worldLinkOutgoing(block: WorldLinkOutgoing.() -> T): T =
    WorldLinkOutgoing(this).block()

public inline fun <T> DataInputStream.worldLinkIncoming(block: WorldLinkIncoming.() -> T): T =
    WorldLinkIncoming(this).block()

public class WorldLinkOutgoing(private val out: DataOutputStream) {
    public fun send(frame: ByteArray) {
        out.writeWorldLinkFrame(frame)
    }

    public fun worldHello(
        worldId: Int,
        worldKey: ByteArray,
    ) {
        send(WorldLinkPackets.worldHello(worldId, worldKey))
    }

    public fun pushSubscribe() {
        send(byteArrayOf(WorldLinkFrameSpecs.OP_PUSH_SUBSCRIBE.toByte()))
    }

    public fun login(
        username: String,
        password: CharArray,
        loginCharacterId: Int?,
    ) {
        send(WorldLinkPackets.login(username, password, loginCharacterId))
    }

    public fun logout(sessionToken: ByteArray) {
        require(sessionToken.size == WorldLinkFrameSpecs.TOKEN_BYTES)
        send(WorldLinkPackets.logout(sessionToken))
    }
}

public class WorldLinkIncoming(private val input: DataInputStream) {
    /** Next length-prefixed frame (no validation). */
    public fun recv(): ByteArray = input.readWorldLinkFrame()

    /**
     * Next frame validated as Central→game layout; throws [IllegalStateException] with
     * [WorldLinkFrameSpecs.describeValidationFailure] on failure.
     */
    public fun recvCentralValidated(subject: String): ByteArray {
        val frame = recv()
        WorldLinkFrameSpecs.validateCentralToGameFrame(frame)?.let { reason ->
            error("Invalid $subject: ${WorldLinkFrameSpecs.describeValidationFailure(reason)}")
        }
        return frame
    }

    /** [Pair.first] is the frame; [Pair.second] is a validation slug if invalid, else `null`. */
    public fun recvCentralOrInvalid(): Pair<ByteArray, String?> {
        val frame = recv()
        val bad = WorldLinkFrameSpecs.validateCentralToGameFrame(frame)
        return frame to bad
    }
}

/**
 * Dispatches a **validated** Central server-push frame (opcode in [ByteArray] index 0).
 * Unknown opcodes go to [onOther].
 */
public inline fun ByteArray.dispatchCentralServerPush(
    crossinline onRevoke: (WorldLinkFrameSpecs.ServerRevokeLoginPayload) -> Unit = {},
    crossinline onMute: (WorldLinkFrameSpecs.ServerMuteUpdatePayload) -> Unit = {},
    crossinline onKick: (WorldLinkFrameSpecs.ServerKickPayload) -> Unit = {},
    crossinline onReboot: (WorldLinkFrameSpecs.ServerRebootPayload) -> Unit = {},
    crossinline onBroadcast: (WorldLinkFrameSpecs.ServerBroadcastPayload) -> Unit = {},
    crossinline onOther: (Int) -> Unit = {},
) {
    when (val op = this[0].toInt() and 0xFF) {
        WorldLinkFrameSpecs.OP_SERVER_REVOKE_LOGIN -> onRevoke(WorldLinkFrameSpecs.decodeServerRevokeLogin(this))
        WorldLinkFrameSpecs.OP_SERVER_MUTE_UPDATE -> onMute(WorldLinkFrameSpecs.decodeServerMuteUpdate(this))
        WorldLinkFrameSpecs.OP_SERVER_KICK -> onKick(WorldLinkFrameSpecs.decodeServerKick(this))
        WorldLinkFrameSpecs.OP_SERVER_REBOOT -> onReboot(WorldLinkFrameSpecs.decodeServerReboot(this))
        WorldLinkFrameSpecs.OP_SERVER_BROADCAST -> onBroadcast(WorldLinkFrameSpecs.decodeServerBroadcast(this))
        else -> onOther(op)
    }
}

internal object WorldLinkPackets {
    fun worldHello(
        worldId: Int,
        worldKey: ByteArray,
    ): ByteArray {
        val bos = ByteArrayOutputStream(128)
        DataOutputStream(bos).use { d ->
            d.writeByte(WorldLinkFrameSpecs.OP_WORLD_HELLO)
            d.writeInt(WorldLinkFrameSpecs.MAGIC)
            d.writeShort(WorldLinkFrameSpecs.CLIENT_PROTOCOL_VERSION)
            d.writeInt(worldId)
            d.writeShort(worldKey.size)
            d.write(worldKey)
        }
        return bos.toByteArray()
    }

    fun login(
        username: String,
        password: CharArray,
        loginCharacterId: Int?,
    ): ByteArray {
        val u = username.toByteArray(StandardCharsets.UTF_8)
        val p = password.concatToString().toByteArray(StandardCharsets.UTF_8)
        val bos = ByteArrayOutputStream(128)
        DataOutputStream(bos).use { d ->
            d.writeByte(WorldLinkFrameSpecs.OP_LOGIN)
            d.writeShort(u.size)
            d.write(u)
            d.writeShort(p.size)
            d.write(p)
            val cid = loginCharacterId?.takeIf { it > 0 }
            if (cid != null && WorldLinkFrameSpecs.CLIENT_PROTOCOL_VERSION >= 4) {
                d.writeInt(cid)
            }
        }
        return bos.toByteArray()
    }

    fun logout(sessionToken: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream(64)
        DataOutputStream(bos).use { d ->
            d.writeByte(WorldLinkFrameSpecs.OP_LOGOUT)
            d.writeShort(sessionToken.size)
            d.write(sessionToken)
        }
        return bos.toByteArray()
    }
}

internal fun unexpectedCentralOp(
    actual: Int,
    expected: Collection<Int>,
): Nothing {
    val expectedStr = expected.joinToString(", ") { "0x${it.toString(16)}" }
    error(
        "Unexpected Central world-link opcode: got 0x${actual.toString(16)}, expected one of [$expectedStr]. " +
            "Game server and Central may be on mismatched protocol versions.",
    )
}

internal fun DataInputStream.readWorldLinkFrame(): ByteArray {
    val len = readInt()
    if (len <= 0 || len > WorldLinkFrameSpecs.MAX_FRAMED_BODY) {
        error(
            "Invalid world-link framed length $len from peer (allowed 1..${WorldLinkFrameSpecs.MAX_FRAMED_BODY}). " +
                "Usually indicates a truncated stream or incompatible protocol.",
        )
    }
    return ByteArray(len).also { readFully(it) }
}

internal fun DataOutputStream.writeWorldLinkFrame(body: ByteArray) {
    require(body.isNotEmpty()) { "World-link frame must include an opcode byte." }
    val op = body[0].toInt() and 0xFF
    val bad = WorldLinkFrameSpecs.validateGameToCentralBody(op, body.size - 1)
    require(bad == null) {
        "Invalid outbound world-link frame: ${WorldLinkFrameSpecs.describeValidationFailure(bad!!)}"
    }
    writeInt(body.size)
    write(body)
    flush()
}
