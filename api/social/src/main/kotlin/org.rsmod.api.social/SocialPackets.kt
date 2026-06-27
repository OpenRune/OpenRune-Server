package org.rsmod.api.social

import net.rsprot.protocol.game.outgoing.misc.player.ChatFilterSettingsPrivateChat
import net.rsprot.protocol.game.outgoing.misc.player.MessageGame
import net.rsprot.protocol.game.outgoing.social.FriendListLoaded
import net.rsprot.protocol.game.outgoing.social.UpdateFriendList
import net.rsprot.protocol.game.outgoing.social.UpdateIgnoreList
import org.rsmod.game.entity.Player

private const val GAME_MESSAGE_TYPE = 0
private const val LOGIN_LOGOUT_NOTIFICATION_TYPE = 5

public fun Player.writeSocialMessage(message: String) {
    client.write(MessageGame(type = GAME_MESSAGE_TYPE, message = message))
}

public fun Player.writeLoginLogoutMessage(message: String) {
    client.write(MessageGame(type = LOGIN_LOGOUT_NOTIFICATION_TYPE, message = message))
}

public fun Player.writeSocialLoaded() {
    client.write(FriendListLoaded)
}

public fun Player.writePrivateChatFilter(mode: Int) {
    client.write(ChatFilterSettingsPrivateChat(mode))
}

public fun Player.writeFriendPresenceDelta(
    name: String,
    previousName: String?,
    worldId: Int,
) {
    val cleaned = name.trim()
    if (cleaned.isBlank()) {
        return
    }

    val entry =
        if (worldId > 0) {
            UpdateFriendList.OnlineFriend(
                added = false,
                name = cleaned,
                previousName = previousName,
                worldId = worldId,
                rank = 0,
                properties = 0,
                notes = "",
                worldName = "OpenRune",
                platform = 8,
                worldFlags = 0,
            )
        } else {
            UpdateFriendList.OfflineFriend(
                added = false,
                name = cleaned,
                previousName = previousName,
                rank = 0,
                properties = 0,
                notes = "",
            )
        }

    client.write(UpdateFriendList(listOf(entry)))

    if (worldId > 0) {
        writeLoginLogoutMessage("$cleaned has logged in.")
    } else {
        writeLoginLogoutMessage("$cleaned has logged out.")
    }
}

public fun Player.writeFriendAddedDelta(
    name: String,
    previousName: String? = null,
) {
    val cleaned = name.trim()
    if (cleaned.isBlank()) {
        return
    }

    client.write(
        UpdateFriendList(
            listOf(
                UpdateFriendList.OfflineFriend(
                    added = true,
                    name = cleaned,
                    previousName = previousName,
                    rank = 0,
                    properties = 0,
                    notes = "",
                )
            )
        )
    )
}

public fun Player.writeIgnoreAddedDelta(
    name: String,
    previousName: String? = null,
) {
    val cleaned = name.trim()
    if (cleaned.isBlank()) {
        return
    }

    client.write(
        UpdateIgnoreList(
            listOf(
                UpdateIgnoreList.AddedIgnoredEntry(
                    name = cleaned,
                    previousName = previousName,
                    note = "",
                    added = true,
                )
            )
        )
    )
}

public fun Player.writeIgnoreRemovedDelta(name: String) {
    val cleaned = name.trim()
    if (cleaned.isBlank()) {
        return
    }

    client.write(
        UpdateIgnoreList(
            listOf(
                UpdateIgnoreList.RemovedIgnoredEntry(cleaned)
            )
        )
    )
}
