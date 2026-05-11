package org.rsmod.api.social

import org.rsmod.api.db.DatabaseConnection
import org.rsmod.game.entity.Player

/*
 * Note: UpdateFriendList has no server->client remove-entry packet in rsprot.
 * Login refresh dedupes stale cached names cleanly before the full friend list is sent.
 * During a live rename, online viewers can receive the updated current/previous name row,
 * but any stale row already present in the client may remain visible until relog.
 */

public fun Player.refreshCachedSocialNames(
    connection: DatabaseConnection,
    names: SocialNameRepository,
): Int {
    val friendRecords = linkedMapOf<String, SocialNameRecord>()
    val ignoreRecords = linkedMapOf<String, SocialNameRecord>()

    for (name in social.friends()) {
        val record =
            names.selectByCanonicalName(connection, name)
                ?: names.selectByAnyName(connection, name)

        if (record != null) {
            friendRecords[record.canonicalName] = record
        }
    }

    for (name in social.ignores()) {
        val record =
            names.selectByCanonicalName(connection, name)
                ?: names.selectByAnyName(connection, name)

        if (record != null) {
            ignoreRecords[record.canonicalName] = record
        }
    }

    social.setFriends(friendRecords.keys)
    social.setIgnores(ignoreRecords.keys)

    for (record in friendRecords.values) {
        social.rememberName(record)
    }

    for (record in ignoreRecords.values) {
        social.rememberName(record)
    }

    persistSocial()

    return friendRecords.size + ignoreRecords.size
}
