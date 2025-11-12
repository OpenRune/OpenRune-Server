package org.alter.game.model.queue

import org.alter.game.model.Tile
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player
import org.rsmod.coroutine.GameCoroutine

typealias QueueTask = GameCoroutine

/**
 * Wait for our [pawn] to reach [coords] before permitting the coroutine to continue its logic.
 * Note that the height of the [coords] and [Pawn.coords] must be equal
 * as well as the x and z coordinates.
 */
suspend fun QueueTask.waitTile(pawn: Pawn, coords: Tile): Unit {
    val src = pawn.tile
    pause { src == coords }
}

/**
 * Wait for our [Player] to close the [interfaceId].
 */
suspend fun QueueTask.waitInterfaceClose(player: Player, interfaceId: Int): Unit = pause { !player.interfaces.isVisible(interfaceId) }