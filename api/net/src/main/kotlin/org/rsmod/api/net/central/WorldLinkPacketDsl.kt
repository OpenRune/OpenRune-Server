package org.rsmod.api.net.central

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.charset.StandardCharsets

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
    crossinline onDisplayNameSync: (WorldLinkFrameSpecs.ServerDisplayNameSyncPayload) -> Unit = {},
    crossinline onOther: (Int) -> Unit = {},
) {
    when (val op = this[0].toInt() and 0xFF) {
        WorldLinkFrameSpecs.OP_SERVER_REVOKE_LOGIN -> onRevoke(WorldLinkFrameSpecs.decodeServerRevokeLogin(this))
        WorldLinkFrameSpecs.OP_SERVER_MUTE_UPDATE -> onMute(WorldLinkFrameSpecs.decodeServerMuteUpdate(this))
        WorldLinkFrameSpecs.OP_SERVER_KICK -> onKick(WorldLinkFrameSpecs.decodeServerKick(this))
        WorldLinkFrameSpecs.OP_SERVER_REBOOT -> onReboot(WorldLinkFrameSpecs.decodeServerReboot(this))
        WorldLinkFrameSpecs.OP_SERVER_BROADCAST -> onBroadcast(WorldLinkFrameSpecs.decodeServerBroadcast(this))
        WorldLinkFrameSpecs.OP_SERVER_DISPLAY_NAME_SYNC -> onDisplayNameSync(WorldLinkFrameSpecs.decodeServerDisplayNameSync(this))
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

internal fun validateGameToCentralFrameOrThrow(body: ByteArray) {
    require(body.isNotEmpty()) { "World-link frame must include an opcode byte." }
    val op = body[0].toInt() and 0xFF
    val bad = WorldLinkFrameSpecs.validateGameToCentralBody(op, body.size - 1)
    require(bad == null) {
        "Invalid outbound world-link frame: ${WorldLinkFrameSpecs.describeValidationFailure(bad!!)}"
    }
}
