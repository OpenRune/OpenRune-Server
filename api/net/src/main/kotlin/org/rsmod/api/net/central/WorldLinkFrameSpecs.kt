package org.rsmod.api.net.central

import dev.or2.central.worldlink.protocol.CentralPushPackets
import dev.or2.central.worldlink.protocol.PacketLimits
import dev.or2.central.worldlink.protocol.PacketCatalog
import dev.or2.central.worldlink.protocol.WorldOpcodes

public typealias ServerRevokeLoginPayload = CentralPushPackets.RevokeLogin
public typealias ServerKickPayload = CentralPushPackets.Kick
public typealias ServerMuteUpdatePayload = CentralPushPackets.MuteUpdate
public typealias ServerRebootPayload = CentralPushPackets.Reboot
public typealias ServerBroadcastPayload = CentralPushPackets.Broadcast
public typealias ServerDisplayNameSyncPayload = CentralPushPackets.DisplayNameSync
public typealias ServerDiscordIdSyncPayload = CentralPushPackets.DiscordIdSync
public typealias ServerPrivateMessagePayload = dev.or2.central.worldlink.protocol.social.PrivateMessagePush
public typealias ServerFriendPresencePayload = dev.or2.central.worldlink.protocol.social.FriendPresencePush

/** Shared world-link constants and validation (see `central-worldlink`). */
public object WorldLinkFrameSpecs {
    public const val MAX_FRAMED_BODY: Int = PacketLimits.MAX_INBOUND_FRAMED_BODY
    public const val MAGIC: Int = WorldOpcodes.MAGIC
    public const val TOKEN_BYTES: Int = WorldOpcodes.TOKEN_BYTES
    public const val CLIENT_PROTOCOL_VERSION: Int = WorldOpcodes.PROTOCOL_VERSION
    public const val PRIVATE_MESSAGE_MAX_CHARS: Int = 255
    public const val PM_RELAY_SENDER_DISPLAY_MAX_UTF8: Int = 96
    public const val PM_RELAY_MESSAGE_MAX_UTF8: Int = PRIVATE_MESSAGE_MAX_CHARS * 4
    public const val WORLD_KEY_MAX_BYTES: Int = PacketLimits.WORLD_KEY_MAX_BYTES
    public const val LOGIN_FAIL_SCRIPT_LINE_MAX_UTF8_BYTES: Int = WorldOpcodes.LOGIN_FAIL_SCRIPT_LINE_MAX_UTF8_BYTES

    public const val OP_WORLD_HELLO: Int = WorldOpcodes.OP_WORLD_HELLO
    public const val OP_HELLO_ACK: Int = WorldOpcodes.OP_HELLO_ACK
    public const val OP_HELLO_REJECT: Int = WorldOpcodes.OP_HELLO_REJECT
    public const val OP_LOGIN: Int = WorldOpcodes.OP_LOGIN
    public const val OP_LOGIN_OK: Int = WorldOpcodes.OP_LOGIN_OK
    public const val OP_LOGIN_FAIL: Int = WorldOpcodes.OP_LOGIN_FAIL
    public const val OP_PUSH_SUBSCRIBE: Int = WorldOpcodes.OP_PUSH_SUBSCRIBE
    public const val OP_PUSH_SUBSCRIBE_ACK: Int = WorldOpcodes.OP_PUSH_SUBSCRIBE_ACK
    public const val OP_HEARTBEAT: Int = WorldOpcodes.OP_HEARTBEAT
    public const val OP_HEARTBEAT_ACK: Int = WorldOpcodes.OP_HEARTBEAT_ACK
    public const val OP_LOGOUT: Int = WorldOpcodes.OP_LOGOUT
    public const val OP_LOGOUT_ACK: Int = WorldOpcodes.OP_LOGOUT_ACK
    public const val OP_SERVER_REVOKE_LOGIN: Int = WorldOpcodes.OP_SERVER_REVOKE_LOGIN
    public const val OP_SERVER_MUTE_UPDATE: Int = WorldOpcodes.OP_SERVER_MUTE_UPDATE
    public const val OP_SERVER_KICK: Int = WorldOpcodes.OP_SERVER_KICK
    public const val OP_SERVER_REBOOT: Int = WorldOpcodes.OP_SERVER_REBOOT
    public const val OP_SERVER_BROADCAST: Int = WorldOpcodes.OP_SERVER_BROADCAST
    public const val OP_SERVER_DISPLAY_NAME_SYNC: Int = WorldOpcodes.OP_SERVER_DISPLAY_NAME_SYNC
    public const val OP_SERVER_DISCORD_ID_SYNC: Int = WorldOpcodes.OP_SERVER_DISCORD_ID_SYNC
    public const val OP_SERVER_PRIVATE_MESSAGE: Int = WorldOpcodes.OP_SERVER_PRIVATE_MESSAGE
    public const val OP_SERVER_FRIEND_PRESENCE: Int = WorldOpcodes.OP_SERVER_FRIEND_PRESENCE
    public const val OP_WORLD_SOCIAL_OK: Int = WorldOpcodes.OP_WORLD_SOCIAL_OK
    public const val OP_WORLD_SOCIAL_FAIL: Int = WorldOpcodes.OP_WORLD_SOCIAL_FAIL
    public const val OP_WORLD_SOCIAL_SYNC_OK: Int = WorldOpcodes.OP_WORLD_SOCIAL_SYNC_OK
    public const val OP_WORLD_SOCIAL_SYNC_FAIL: Int = WorldOpcodes.OP_WORLD_SOCIAL_SYNC_FAIL

    public const val OP_GAME_DISCORD_LINK_PENDING: Int = WorldOpcodes.OP_GAME_DISCORD_LINK_PENDING
    public const val OP_GAME_DISCORD_LINK_PENDING_OK: Int = WorldOpcodes.OP_GAME_DISCORD_LINK_PENDING_OK
    public const val OP_GAME_DISCORD_LINK_PENDING_FAIL: Int = WorldOpcodes.OP_GAME_DISCORD_LINK_PENDING_FAIL
    public const val OP_GAME_DISCORD_LINK_INVALIDATE: Int = WorldOpcodes.OP_GAME_DISCORD_LINK_INVALIDATE
    public const val OP_GAME_DISCORD_LINK_INVALIDATE_ACK: Int = WorldOpcodes.OP_GAME_DISCORD_LINK_INVALIDATE_ACK

    public const val GAME_DISCORD_LINK_PENDING_FAIL_ALREADY_LINKED: Int =
        WorldOpcodes.GAME_DISCORD_LINK_PENDING_FAIL_ALREADY_LINKED
    public const val GAME_DISCORD_LINK_PENDING_FAIL_DISCORD_NOT_FOUND: Int =
        WorldOpcodes.GAME_DISCORD_LINK_PENDING_FAIL_DISCORD_NOT_FOUND

    public fun decodeServerDiscordIdSync(frame: ByteArray): CentralPushPackets.DiscordIdSync =
        CentralPushPackets.decodeDiscordIdSync(frame)

    public data class GameDiscordLinkPendingOkPayload(
        val code: Int,
        val dmSent: Boolean,
    )

    public fun decodeGameDiscordLinkPendingOk(frame: ByteArray): GameDiscordLinkPendingOkPayload {
        val payload = dev.or2.central.worldlink.protocol.discord.GameDiscordLinkPendingOkPacket.decode(frame)
        return GameDiscordLinkPendingOkPayload(code = payload.code, dmSent = payload.dmSent)
    }

    public fun describeValidationFailure(reason: String): String = PacketCatalog.describeFailure(reason)

    public fun validateGameToCentralBody(opcode: Int, bodyLen: Int): String? =
        PacketCatalog.validateGameToCentralBody(opcode, bodyLen)

    public fun validateCentralToGameFrame(frame: ByteArray): String? =
        PacketCatalog.validateCentralToGameFrame(frame)

    public fun decodeServerRevokeLogin(frame: ByteArray): ServerRevokeLoginPayload =
        CentralPushPackets.decodeRevokeLogin(frame)

    public fun decodeServerKick(frame: ByteArray): ServerKickPayload = CentralPushPackets.decodeKick(frame)

    public fun decodeServerMuteUpdate(frame: ByteArray): ServerMuteUpdatePayload =
        CentralPushPackets.decodeMuteUpdate(frame)

    public fun decodeServerReboot(frame: ByteArray): ServerRebootPayload = CentralPushPackets.decodeReboot(frame)

    public fun decodeServerBroadcast(frame: ByteArray): ServerBroadcastPayload =
        CentralPushPackets.decodeBroadcast(frame)

    public fun decodeServerDisplayNameSync(frame: ByteArray): ServerDisplayNameSyncPayload =
        CentralPushPackets.decodeDisplayNameSync(frame)

    public fun decodeServerPrivateMessage(frame: ByteArray): ServerPrivateMessagePayload =
        CentralPushPackets.decodePrivateMessage(frame)

    public fun decodeServerFriendPresence(frame: ByteArray): ServerFriendPresencePayload =
        CentralPushPackets.decodeFriendPresence(frame)

    public data class CentralSocialFriend(
        val displayName: String,
        val previousDisplayName: String?,
        val worldId: Int,
    )

    public data class CentralSocialIgnore(
        val displayName: String,
        val previousDisplayName: String?,
    )

    public data class CentralSocialSnapshot(
        val publicChat: Int,
        val privateChat: Int,
        val tradeChat: Int,
        val friends: List<CentralSocialFriend>,
        val ignores: List<CentralSocialIgnore>,
    )
}
