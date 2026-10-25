package org.rsmod.api.account.saver.request

import org.rsmod.game.entity.Player

/**
 * [player] is held live, not snapshotted, and carries no world type: the schema has to be resolved
 * from it at execution time, or a mode captured here could go stale before the save runs.
 */
public data class AccountSaveRequest(
    val accountId: Int,
    val characterId: Int,
    val player: Player,
    val callback: AccountSaveCallback,
    var attempts: Int = 0,
)
