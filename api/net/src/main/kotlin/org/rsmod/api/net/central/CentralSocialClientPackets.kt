package org.rsmod.api.net.central

import net.rsprot.protocol.game.outgoing.misc.player.ChatFilterSettingsPrivateChat
import net.rsprot.protocol.game.outgoing.social.FriendListLoaded
import net.rsprot.protocol.game.outgoing.social.UpdateFriendList
import net.rsprot.protocol.game.outgoing.social.UpdateIgnoreList
import org.rsmod.game.entity.Player

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
                    platform = 8,
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
