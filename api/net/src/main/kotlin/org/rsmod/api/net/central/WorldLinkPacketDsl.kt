package org.rsmod.api.net.central

import dev.or2.central.worldlink.protocol.GameToCentralPackets
import dev.or2.central.worldlink.protocol.discord.GameToCentralDiscordPackets
import dev.or2.central.worldlink.protocol.social.GameToCentralSocialPackets

/**
 * Dispatches a **validated** Central server-push frame (opcode in [ByteArray] index 0).
 * Unknown opcodes go to [onOther].
 */
public inline fun ByteArray.dispatchCentralServerPush(
    crossinline onRevoke: (ServerRevokeLoginPayload) -> Unit = {},
    crossinline onMute: (ServerMuteUpdatePayload) -> Unit = {},
    crossinline onKick: (ServerKickPayload) -> Unit = {},
    crossinline onReboot: (ServerRebootPayload) -> Unit = {},
    crossinline onBroadcast: (ServerBroadcastPayload) -> Unit = {},
    crossinline onDisplayNameSync: (ServerDisplayNameSyncPayload) -> Unit = {},
    crossinline onDiscordIdSync: (ServerDiscordIdSyncPayload) -> Unit = {},
    crossinline onPrivateMessage: (ServerPrivateMessagePayload) -> Unit = {},
    crossinline onFriendPresence: (ServerFriendPresencePayload) -> Unit = {},
    crossinline onOther: (Int) -> Unit = {},
) {
    when (val op = this[0].toInt() and 0xFF) {
        WorldLinkFrameSpecs.OP_SERVER_REVOKE_LOGIN -> onRevoke(WorldLinkFrameSpecs.decodeServerRevokeLogin(this))
        WorldLinkFrameSpecs.OP_SERVER_MUTE_UPDATE -> onMute(WorldLinkFrameSpecs.decodeServerMuteUpdate(this))
        WorldLinkFrameSpecs.OP_SERVER_KICK -> onKick(WorldLinkFrameSpecs.decodeServerKick(this))
        WorldLinkFrameSpecs.OP_SERVER_REBOOT -> onReboot(WorldLinkFrameSpecs.decodeServerReboot(this))
        WorldLinkFrameSpecs.OP_SERVER_BROADCAST -> onBroadcast(WorldLinkFrameSpecs.decodeServerBroadcast(this))
        WorldLinkFrameSpecs.OP_SERVER_DISPLAY_NAME_SYNC -> onDisplayNameSync(WorldLinkFrameSpecs.decodeServerDisplayNameSync(this))
        WorldLinkFrameSpecs.OP_SERVER_DISCORD_ID_SYNC -> onDiscordIdSync(WorldLinkFrameSpecs.decodeServerDiscordIdSync(this))
        WorldLinkFrameSpecs.OP_SERVER_PRIVATE_MESSAGE -> onPrivateMessage(WorldLinkFrameSpecs.decodeServerPrivateMessage(this))
        WorldLinkFrameSpecs.OP_SERVER_FRIEND_PRESENCE -> onFriendPresence(WorldLinkFrameSpecs.decodeServerFriendPresence(this))
        else -> onOther(op)
    }
}

internal object WorldLinkPackets {
    fun worldHello(worldId: Int, worldKey: ByteArray): ByteArray =
        GameToCentralPackets.worldHello(worldId, worldKey)

    fun login(username: String, password: CharArray, loginCharacterId: Int?): ByteArray =
        GameToCentralPackets.login(username, password, loginCharacterId)

    fun logout(sessionToken: ByteArray): ByteArray = GameToCentralPackets.logout(sessionToken)

    fun heartbeat(sessionToken: ByteArray): ByteArray = GameToCentralPackets.heartbeat(sessionToken)

    fun socialSync(characterId: Int): ByteArray = GameToCentralSocialPackets.socialSync(characterId)

    fun friendAdd(characterId: Int, targetName: String): ByteArray =
        GameToCentralSocialPackets.friendAdd(characterId, targetName)

    fun friendDel(characterId: Int, targetName: String): ByteArray =
        GameToCentralSocialPackets.friendDel(characterId, targetName)

    fun ignoreAdd(characterId: Int, targetName: String): ByteArray =
        GameToCentralSocialPackets.ignoreAdd(characterId, targetName)

    fun ignoreDel(characterId: Int, targetName: String): ByteArray =
        GameToCentralSocialPackets.ignoreDel(characterId, targetName)

    fun chatFilters(
        characterId: Int,
        publicChat: Int,
        privateChat: Int,
        tradeChat: Int,
    ): ByteArray =
        GameToCentralSocialPackets.chatFilters(characterId, publicChat, privateChat, tradeChat)

    fun privateChatFilter(
        characterId: Int,
        privateChat: Int,
    ): ByteArray =
        chatFilters(characterId, publicChat = 0, privateChat = privateChat, tradeChat = 0)

    fun pmRelay(payload: dev.or2.central.worldlink.protocol.social.SocialPackets.PmRelayPayload): ByteArray =
        GameToCentralSocialPackets.pmRelay(payload)

    fun gameDiscordLinkPending(accountId: Int, discordUsername: String): ByteArray =
        GameToCentralDiscordPackets.linkPending(accountId, discordUsername)

    fun gameDiscordLinkInvalidate(accountId: Int): ByteArray =
        GameToCentralDiscordPackets.linkInvalidate(accountId)
}

internal fun unexpectedCentralOp(actual: Int, expected: Collection<Int>): Nothing {
    val expectedStr = expected.joinToString(", ") { it.toString() }
    error(
        "Unexpected Central world-link opcode: got $actual, expected one of [$expectedStr]. " +
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
