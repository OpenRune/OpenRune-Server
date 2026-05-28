package org.rsmod.api.npc.owner

import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.PlayerUid

public val Npc.hasSpawnOwner: Boolean
    get() = spawnOwner != PlayerUid.NULL

public fun Npc.assignSpawnOwner(owner: Player, nearCycle: Int) {
    spawnOwner = owner.uid
    spawnOwnerLastNearCycle = nearCycle
}

public fun Npc.clearSpawnOwner() {
    spawnOwner = PlayerUid.NULL
    spawnOwnerLastNearCycle = -1
}

public fun Npc.isSpawnOwnedBy(player: Player): Boolean = spawnOwner == player.uid

public fun Npc.isSpawnOwnedBy(owner: PlayerUid): Boolean = spawnOwner == owner

/** True when [hasSpawnOwner] and the owner is not [player] (e.g. block attacking another player's spawn). */
public fun Npc.isSpawnOwnedByOther(player: Player): Boolean =
    hasSpawnOwner && spawnOwner != player.uid

public fun NpcList.ownedBy(owner: PlayerUid): Sequence<Npc> =
    asSequence().filter { it.isSpawnOwnedBy(owner) }

public fun NpcList.ownedBy(player: Player): Sequence<Npc> = ownedBy(player.uid)
