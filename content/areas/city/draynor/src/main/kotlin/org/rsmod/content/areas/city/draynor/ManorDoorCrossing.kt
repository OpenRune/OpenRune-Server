package org.rsmod.content.areas.city.draynor

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.forcedWalk
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

internal fun BoundLocInfo.crossingOffset(): Pair<Int, Int> =
    when (angle.id) {
        0 -> -1 to 0
        1 -> 0 to 1
        2 -> 1 to 0
        else -> 0 to -1
    }

internal fun BoundLocInfo.acrossTile(): CoordGrid {
    val (dx, dz) = crossingOffset()
    return CoordGrid(coords.x + dx, coords.z + dz, coords.level)
}

internal fun BoundLocInfo.behindTile(): CoordGrid {
    val (dx, dz) = crossingOffset()
    return CoordGrid(coords.x - dx, coords.z - dz, coords.level)
}

private val BoundLocInfo.fillsOwnTile: Boolean
    get() = shape == LocShape.CentrepieceStraight || shape == LocShape.CentrepieceDiagonal

internal fun ProtectedAccess.crossingRoute(door: BoundLocInfo): List<CoordGrid> =
    crossingRoute(player.coords, door)

internal fun crossingRoute(here: CoordGrid, door: BoundLocInfo): List<CoordGrid> {
    val onTile = door.coords
    val across = door.acrossTile()

    val exit =
        when {
            !door.fillsOwnTile -> if (here == across) onTile else across
            here == across -> door.behindTile()
            else -> across
        }

    return if (here == onTile || exit == onTile) listOf(exit) else listOf(onTile, exit)
}

internal fun ProtectedAccess.crossingTiles(route: List<CoordGrid>): Int =
    crossingTiles(player.coords, route)

internal fun crossingTiles(start: CoordGrid, route: List<CoordGrid>): Int {
    var tiles = 0
    var from = start
    for (waypoint in route) {
        tiles += from.chebyshevDistance(waypoint)
        from = waypoint
    }
    return maxOf(1, tiles)
}

// The op tick cannot move; keep the door open for route polling and the final waypoint.
internal fun crossingOpenTicks(tiles: Int): Int = maxOf(MinOpenTicks, tiles + OpenTailTicks)

private const val MinOpenTicks = 4

private const val OpenTailTicks = 2

// Drop already-occupied leading waypoints so polling them does not consume the open window.
internal suspend fun ProtectedAccess.crossDoorway(route: List<CoordGrid>) {
    val trimmed = route.dropWhile { it == player.coords }
    if (trimmed.isEmpty()) {
        return
    }
    forcedWalk(trimmed, crossTiles = crossingTiles(trimmed))
}

internal suspend fun ProtectedAccess.crossDoorway(dest: CoordGrid): Unit = crossDoorway(listOf(dest))
