package org.rsmod.api.player.protect

import org.rsmod.api.player.hook.TeleportType
import org.rsmod.game.movement.MoveSpeed
import org.rsmod.map.CoordGrid

private const val ARRIVAL_GRACE_TICKS = 2

public suspend fun ProtectedAccess.forcedWalk(dest: CoordGrid, crossTiles: Int): Unit =
    forcedWalk(listOf(dest), crossTiles)

/** Bounded scripted crossings bypass collision and restore normal movement on cancellation. */
public suspend fun ProtectedAccess.forcedWalk(waypoints: List<CoordGrid>, crossTiles: Int) {
    require(waypoints.isNotEmpty()) { "Waypoints must not be empty." }
    require(crossTiles >= 0) { "Crossing duration must not be negative." }
    require(waypoints.all { it.level == player.coords.level }) {
        "Scripted walking cannot change floors."
    }
    val dest = waypoints.last()
    player.forcedRoute = true
    player.resetPendingFaceSquare()
    try {
        player.routeRequest = null
        player.routeDestination.clear()
        player.routeDestination.addAll(waypoints)
        var elapsed = 0
        while (player.coords != dest && elapsed < crossTiles + ARRIVAL_GRACE_TICKS) {
            player.moveSpeed = MoveSpeed.Walk
            player.tempMoveSpeed = MoveSpeed.Walk
            delay(1)
            elapsed++
        }
    } finally {
        player.forcedRoute = false
        player.routeDestination.clear()
        player.tempMoveSpeed = null
        player.resetPendingFaceSquare()
    }
    if (player.coords != dest) {
        telejump(dest, TeleportType.Exempt)
    }
}
