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
    crossinline onPrivateMessage: (WorldLinkFrameSpecs.ServerPrivateMessagePayload) -> Unit = {},
    crossinline onFriendPresence: (WorldLinkFrameSpecs.ServerFriendPresencePayload) -> Unit = {},
    crossinline onOther: (Int) -> Unit = {},
) {
    when (val op = this[0].toInt() and 0xFF) {
        WorldLinkFrameSpecs.OP_SERVER_REVOKE_LOGIN -> onRevoke(WorldLinkFrameSpecs.decodeServerRevokeLogin(this))
        WorldLinkFrameSpecs.OP_SERVER_MUTE_UPDATE -> onMute(WorldLinkFrameSpecs.decodeServerMuteUpdate(this))
        WorldLinkFrameSpecs.OP_SERVER_KICK -> onKick(WorldLinkFrameSpecs.decodeServerKick(this))
        WorldLinkFrameSpecs.OP_SERVER_REBOOT -> onReboot(WorldLinkFrameSpecs.decodeServerReboot(this))
        WorldLinkFrameSpecs.OP_SERVER_BROADCAST -> onBroadcast(WorldLinkFrameSpecs.decodeServerBroadcast(this))
        WorldLinkFrameSpecs.OP_SERVER_DISPLAY_NAME_SYNC -> onDisplayNameSync(WorldLinkFrameSpecs.decodeServerDisplayNameSync(this))
        WorldLinkFrameSpecs.OP_SERVER_PRIVATE_MESSAGE -> onPrivateMessage(WorldLinkFrameSpecs.decodeServerPrivateMessage(this))
        WorldLinkFrameSpecs.OP_SERVER_FRIEND_PRESENCE -> {
            onFriendPresence(WorldLinkFrameSpecs.decodeServerFriendPresence(this))
        }
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

    fun privateMessageRelay(
        sessionToken: ByteArray,
        fromCharacterId: Int,
        targetName: String,
        senderDisplayName: String,
        senderCrown: Int,
        message: String,
    ): ByteArray {
        val bos = ByteArrayOutputStream(256)
        DataOutputStream(bos).use { d ->
            d.writeByte(WorldLinkFrameSpecs.OP_WORLD_PM_RELAY)
            d.writeToken(sessionToken)
            d.writeInt(fromCharacterId)
            d.writeByte(senderCrown.coerceIn(0, 255))
            d.writeUtf8(targetName, WorldLinkFrameSpecs.SOCIAL_NAME_MAX_UTF8)
            d.writeUtf8(senderDisplayName, WorldLinkFrameSpecs.PM_RELAY_SENDER_DISPLAY_MAX_UTF8)
            d.writeUtf8(message, WorldLinkFrameSpecs.PM_RELAY_MESSAGE_MAX_UTF8)
        }
        return bos.toByteArray()
    }

    fun friendAdd(
        sessionToken: ByteArray,
        characterId: Int,
        targetName: String,
    ): ByteArray = socialNameAction(WorldLinkFrameSpecs.OP_WORLD_FRIEND_ADD, sessionToken, characterId, targetName)

    fun friendDelete(
        sessionToken: ByteArray,
        characterId: Int,
        targetName: String,
    ): ByteArray = socialNameAction(WorldLinkFrameSpecs.OP_WORLD_FRIEND_DEL, sessionToken, characterId, targetName)

    fun ignoreAdd(
        sessionToken: ByteArray,
        characterId: Int,
        targetName: String,
    ): ByteArray = socialNameAction(WorldLinkFrameSpecs.OP_WORLD_IGNORE_ADD, sessionToken, characterId, targetName)

    fun ignoreDelete(
        sessionToken: ByteArray,
        characterId: Int,
        targetName: String,
    ): ByteArray = socialNameAction(WorldLinkFrameSpecs.OP_WORLD_IGNORE_DEL, sessionToken, characterId, targetName)

    fun privateChatFilter(
        sessionToken: ByteArray,
        characterId: Int,
        privateChatFilter: Int,
    ): ByteArray {
        val bos = ByteArrayOutputStream(64)
        DataOutputStream(bos).use { d ->
            d.writeByte(WorldLinkFrameSpecs.OP_WORLD_CHAT_FILTERS)
            d.writeToken(sessionToken)
            d.writeInt(characterId)

            // Public/trade are intentionally ignored by Central social.
            // Keep these bytes for wire compatibility with the existing frame shape.
            d.writeByte(0)
            d.writeByte(privateChatFilter.coerceIn(0, 2))
            d.writeByte(0)
        }
        return bos.toByteArray()
    }

    private fun socialNameAction(
        opcode: Int,
        sessionToken: ByteArray,
        characterId: Int,
        targetName: String,
    ): ByteArray {
        val bos = ByteArrayOutputStream(128)
        DataOutputStream(bos).use { d ->
            d.writeByte(opcode)
            d.writeToken(sessionToken)
            d.writeInt(characterId)
            d.writeUtf8(targetName, WorldLinkFrameSpecs.SOCIAL_NAME_MAX_UTF8)
        }
        return bos.toByteArray()
    }

    private fun DataOutputStream.writeToken(sessionToken: ByteArray) {
        writeShort(sessionToken.size)
        write(sessionToken)
    }

    private fun DataOutputStream.writeUtf8(
        value: String,
        maxBytes: Int,
    ) {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        require(bytes.size <= maxBytes) {
            "World-link UTF-8 value exceeds max length: ${bytes.size} > $maxBytes"
        }
        writeShort(bytes.size)
        write(bytes)
    }

    fun socialSync(
        sessionToken: ByteArray,
        characterId: Int,
    ): ByteArray {
        val bos = ByteArrayOutputStream(64)
        DataOutputStream(bos).use { d ->
            d.writeByte(WorldLinkFrameSpecs.OP_WORLD_SOCIAL_SYNC)
            d.writeToken(sessionToken)
            d.writeInt(characterId)
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
