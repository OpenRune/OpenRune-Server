package org.rsmod.api.social
/*TODO
    *previous_display_name = old display_name
    *display_name = new display_name
    *display_name_changed_at = CURRENT_TIMESTAMP
    * After Account Management UI actually changes a display name
 */
import net.rsprot.protocol.game.outgoing.misc.player.ChatFilterSettings
import net.rsprot.protocol.game.outgoing.misc.player.ChatFilterSettingsPrivateChat
import net.rsprot.protocol.game.outgoing.misc.player.MessageGame
import net.rsprot.protocol.game.outgoing.social.MessagePrivate
import net.rsprot.protocol.game.outgoing.social.MessagePrivateEcho
import net.rsprot.protocol.game.outgoing.social.UpdateFriendList
import net.rsprot.protocol.game.outgoing.social.UpdateIgnoreList
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList

public fun Player.pushPrivateChatMode() {
    client.write(ChatFilterSettingsPrivateChat(social.privateChatMode.id))
}

public fun Player.pushChatModes() {
    client.write(
        ChatFilterSettings(
            publicChatFilter = social.publicChatMode.id,
            tradeChatFilter = social.tradeChatMode.id,
        )
    )

    pushPrivateChatMode()
}

public fun Player.pushFriends(playerList: PlayerList? = null) {
    val entries =
        social.friends().map { canonicalName ->
            val online = playerList?.firstOrNull { it.matchesSocialName(canonicalName) }
            val record = online?.socialNameRecord()
                ?: social.nameRecord(canonicalName)
                ?: SocialNameRecord(
                    canonicalName = canonicalName,
                    currentName = canonicalName,
                    previousName = null,
                )

            if (online != null) {
                UpdateFriendList.OnlineFriend(
                    added = false,
                    name = record.currentName,
                    previousName = record.previousName,
                    worldId = 255,
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
                    name = record.currentName,
                    previousName = record.previousName,
                    rank = 0,
                    properties = 0,
                    notes = "",
                )
            }
        }

    client.write(UpdateFriendList(entries))
}

public fun Player.pushIgnores(playerList: PlayerList? = null) {
    val entries =
        social.ignores().map { canonicalName ->
            val online = playerList?.firstOrNull { it.matchesSocialName(canonicalName) }
            val record = online?.socialNameRecord()
                ?: social.nameRecord(canonicalName)
                ?: SocialNameRecord(
                    canonicalName = canonicalName,
                    currentName = canonicalName,
                    previousName = null,
                )

            UpdateIgnoreList.AddedIgnoredEntry(
                name = record.currentName,
                previousName = record.previousName,
                note = "",
                added = false,
            )
        }

    client.write(UpdateIgnoreList(entries))
}

public fun Player.addSocialFriend(
    name: String,
    playerList: PlayerList,
    resolvedName: SocialNameRecord? = null,
) {
    val cleaned = cleanSocialName(name)
    if (cleaned.isBlank()) {
        return
    }

    val online = playerList.firstOrNull { it.matchesSocialName(cleaned) }
    val record =
        resolvedName
            ?: online?.socialNameRecord()
            ?: SocialNameRecord(
                canonicalName = cleaned.lowercase(),
                currentName = cleaned,
                previousName = null,
            )

    if (matchesSocialName(record.canonicalName) ||
        matchesSocialName(record.currentName) ||
        record.previousName?.let(::matchesSocialName) == true
    ) {
        writeSocialMessage("You can't add yourself to your own friend list.")
        return
    }

    if (social.isFriend(record.canonicalName)) {
        return
    }

    social.removeIgnore(record.canonicalName)
    social.removeIgnore(record.currentName)
    record.previousName?.let { social.removeIgnore(it) }

    social.rememberName(record)
    social.addFriend(record.canonicalName)
    persistSocial()

    client.write(
        UpdateFriendList(
            listOf(
                UpdateFriendList.OfflineFriend(
                    added = true,
                    name = record.currentName,
                    previousName = record.previousName,
                    rank = 0,
                    properties = 0,
                    notes = "",
                )
            )
        )
    )

    pushFriends(playerList)
    pushIgnores(playerList)
}

public fun Player.deleteSocialFriend(
    name: String,
    playerList: PlayerList,
    resolvedName: SocialNameRecord? = null,
) {
    val cleaned = cleanSocialName(name)
    if (cleaned.isBlank()) {
        return
    }

    val canonicalName = resolvedName?.canonicalName ?: cleaned.lowercase()

    if (!social.removeFriend(canonicalName)) {
        return
    }

    persistSocial()
    pushFriends(playerList)
}

public fun Player.addSocialIgnore(
    name: String,
    playerList: PlayerList,
    resolvedName: SocialNameRecord? = null,
) {
    val cleaned = cleanSocialName(name)
    if (cleaned.isBlank()) {
        return
    }

    val online = playerList.firstOrNull { it.matchesSocialName(cleaned) }
    val record =
        resolvedName
            ?: online?.socialNameRecord()
            ?: SocialNameRecord(
                canonicalName = cleaned.lowercase(),
                currentName = cleaned,
                previousName = null,
            )

    if (matchesSocialName(record.canonicalName) ||
        matchesSocialName(record.currentName) ||
        record.previousName?.let(::matchesSocialName) == true
    ) {
        writeSocialMessage("You can't add yourself to your own ignore list.")
        return
    }

    if (social.isIgnoring(record.canonicalName)) {
        return
    }

    social.removeFriend(record.canonicalName)
    social.removeFriend(record.currentName)
    record.previousName?.let { social.removeFriend(it) }

    social.rememberName(record)
    social.addIgnore(record.canonicalName)
    persistSocial()

    client.write(
        UpdateIgnoreList(
            listOf(
                UpdateIgnoreList.AddedIgnoredEntry(
                    name = record.currentName,
                    previousName = record.previousName,
                    note = "",
                    added = true,
                )
            )
        )
    )

    pushFriends(playerList)
    pushIgnores(playerList)
}

public fun Player.deleteSocialIgnore(
    name: String,
    resolvedName: SocialNameRecord? = null,
) {
    val cleaned = cleanSocialName(name)
    if (cleaned.isBlank()) {
        return
    }

    val canonicalName = resolvedName?.canonicalName ?: cleaned.lowercase()

    if (!social.removeIgnore(canonicalName)) {
        return
    }

    persistSocial()

    client.write(
        UpdateIgnoreList(
            listOf(
                UpdateIgnoreList.RemovedIgnoredEntry(
                    resolvedName?.currentName ?: cleaned
                )
            )
        )
    )

    pushIgnores()
}

public fun Player.writeSocialMessage(message: String) {
    client.write(MessageGame(type = 0, message = message))
}

private fun cleanSocialName(name: String): String {
    return name.trim()
}

public fun Player.sendPrivateMessageTo(target: Player, message: String) {
    val cleaned = message.trim()
    if (cleaned.isBlank()) {
        return
    }

    if (target.social.isIgnoring(username) || target.social.isIgnoring(displayName)) {
        writeSocialMessage("${target.displayName} is not accepting messages from you.")
        return
    }

    when (target.social.privateChatMode) {
        SocialData.PrivateChatMode.OFF -> {
            writeSocialMessage("${target.displayName} is not accepting private messages.")
            return
        }

        SocialData.PrivateChatMode.FRIENDS -> {
            if (!target.social.isFriend(username) && !target.social.isFriend(displayName)) {
                writeSocialMessage("${target.displayName} is not accepting private messages.")
                return
            }
        }

        SocialData.PrivateChatMode.ON -> {}
    }

    val counter = nextWorldMessageCounter()

    target.client.write(
        MessagePrivate(
            sender = displayName,
            worldId = 255,
            worldMessageCounter = counter,
            chatCrownType = 0,
            message = cleaned,
        )
    )

    client.write(
        MessagePrivateEcho(
            recipient = target.displayName,
            message = cleaned,
        )
    )
}

private fun Player.socialNameRecord(): SocialNameRecord {
    val current = displayName.ifBlank { username }
    return SocialNameRecord(
        canonicalName = username.lowercase(),
        currentName = current,
        previousName = previousDisplayName.takeIf(String::isNotBlank),
    )
}

private fun Player.matchesSocialName(name: String): Boolean {
    return username.equals(name, ignoreCase = true) ||
        displayName.equals(name, ignoreCase = true) ||
        previousDisplayName.equals(name, ignoreCase = true)
}

private var worldMessageCounter: Int = 1

private fun nextWorldMessageCounter(): Int {
    worldMessageCounter = (worldMessageCounter + 1) and 0xFFFFFF
    if (worldMessageCounter <= 0) {
        worldMessageCounter = 1
    }
    return worldMessageCounter
}
