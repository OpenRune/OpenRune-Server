package org.rsmod.api.net.central

import com.github.michaelbull.logging.InlineLogger
import dev.or2.central.worldlink.protocol.social.PmTraceLog
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicInteger
import net.rsprot.protocol.game.outgoing.misc.player.ChatFilterSettingsPrivateChat
import net.rsprot.protocol.game.outgoing.social.FriendListLoaded
import net.rsprot.protocol.game.outgoing.social.MessagePrivate
import net.rsprot.protocol.game.outgoing.social.UpdateFriendList
import net.rsprot.protocol.game.outgoing.social.UpdateIgnoreList
import org.rsmod.api.server.config.ServerConfig
import org.rsmod.game.entity.Player

private val socialPacketLogger = InlineLogger()

private val privateMessageCounter = AtomicInteger(privateMessageCounterSeed())

/** Hour-of-year × 50k seed; counter sent as (worldId + value) & 0xFFFFFF per client dedup rules. */
private fun privateMessageCounterSeed(): Int {
    val now = ZonedDateTime.now(ZoneOffset.UTC)
    val hourOfYear = (now.dayOfYear - 1) * 24 + now.hour
    return (hourOfYear * 50_000) and 0xFFFFFF
}

private fun nextPrivateMessageCounter(worldId: Int): Int {
    while (true) {
        val current = privateMessageCounter.get()
        val next = (current + 1) and 0xFFFFFF
        val resolved = if (next <= 0) 1 else next
        if (privateMessageCounter.compareAndSet(current, resolved)) {
            return (worldId + resolved) and 0xFFFFFF
        }
    }
}

/**
 * Packet order matches the original OpenRune-Central integration (commit bbf8e6a31):
 * [FriendListLoaded] first, then private chat filter, then bulk friend/ignore lists.
 */
fun Player.writeCentralSocialSnapshot(snapshot: WorldLinkFrameSpecs.CentralSocialSnapshot) {
    client.write(FriendListLoaded)
    client.write(ChatFilterSettingsPrivateChat(snapshot.privateChat))

    val friendEntries =
        snapshot.friends.map { friend ->
            if (friend.worldId > 0) {
                UpdateFriendList.OnlineFriend(
                    added = false,
                    name = friend.displayName,
                    previousName = friend.previousDisplayName,
                    worldId = friend.worldId,
                    rank = 0,
                    properties = 0,
                    notes = "",
                    worldName = "OpenRune",
                    platform = OSRS_PLATFORM,
                    worldFlags = 0,
                )
            } else {
                UpdateFriendList.OfflineFriend(
                    added = false,
                    name = friend.displayName,
                    previousName = friend.previousDisplayName,
                    rank = 0,
                    properties = 0,
                    notes = "",
                )
            }
        }

    client.write(UpdateFriendList(friendEntries))

    val ignoreEntries =
        snapshot.ignores.map { ignore ->
            UpdateIgnoreList.AddedIgnoredEntry(
                name = ignore.displayName,
                previousName = ignore.previousDisplayName,
                note = "",
                added = false,
            )
        }

    client.write(UpdateIgnoreList(ignoreEntries))
}

fun Player.writeCentralSocialSnapshotEmpty() {
    client.write(FriendListLoaded)
    client.write(ChatFilterSettingsPrivateChat(0))
    client.write(UpdateFriendList(emptyList()))
    client.write(UpdateIgnoreList(emptyList()))
}

fun Player.writeCentralFriendPresence(
    push: ServerFriendPresencePayload,
    serverConfig: ServerConfig,
) {
    val friend =
        if (push.friendWorldId > 0) {
            UpdateFriendList.OnlineFriend(
                added = false,
                name = push.friendDisplayName,
                previousName = push.friendPreviousDisplayName,
                worldId = push.friendWorldId,
                rank = 0,
                properties = 0,
                notes = "",
                worldName = formatWorldName(push.friendWorldId, serverConfig),
                platform = OSRS_PLATFORM,
                worldFlags = 0,
            )
        } else {
            UpdateFriendList.OfflineFriend(
                added = false,
                name = push.friendDisplayName,
                previousName = push.friendPreviousDisplayName,
                rank = 0,
                properties = 0,
                notes = "",
            )
        }
    client.write(UpdateFriendList(listOf(friend)))
}

fun Player.writeCentralPrivateMessage(
    push: ServerPrivateMessagePayload,
    serverConfig: ServerConfig,
) {
    val worldId = push.senderWorldId
    val counter = nextPrivateMessageCounter(worldId)
    if (serverConfig.socialPmTraceLogs) {
        socialPacketLogger.info {
            PmTraceLog.formatPacket(
                ownerLabel = displayName.ifBlank { username },
                ownerCharacterId = characterId,
                push = push,
                worldMessageCounter = counter,
            )
        }
    }
    client.write(
        MessagePrivate(
            sender = push.senderDisplayName,
            worldId = worldId,
            worldMessageCounter = counter,
            chatCrownType = push.senderCrown,
            message = push.message,
        ),
    )
}

private fun formatWorldName(
    worldId: Int,
    serverConfig: ServerConfig? = null,
): String {
    val label = if (worldId > 300) worldId - 300 else worldId
    val prefix = serverConfig?.name ?: "OpenRune"
    return "$prefix $label"
}

private const val OSRS_PLATFORM: Int = 8
