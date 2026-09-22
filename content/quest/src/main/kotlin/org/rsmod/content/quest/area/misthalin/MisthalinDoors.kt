package org.rsmod.content.quest.area.misthalin

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.forcedWalk
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

internal fun BoundLocInfo.misthalinCrossingOffset(): Pair<Int, Int> =
    when (angle.id) {
        0 -> -1 to 0
        1 -> 0 to 1
        2 -> 1 to 0
        else -> 0 to -1
    }

internal fun BoundLocInfo.misthalinAcrossTile(): CoordGrid {
    val (dx, dz) = misthalinCrossingOffset()
    return CoordGrid(coords.x + dx, coords.z + dz, coords.level)
}

internal fun ProtectedAccess.misthalinCrossingRoute(door: BoundLocInfo): List<CoordGrid> {
    val here = player.coords
    val onTile = door.coords
    val across = door.misthalinAcrossTile()
    val exit = if (here == across) onTile else across
    return if (here == onTile || exit == onTile) listOf(exit) else listOf(onTile, exit)
}

internal fun ProtectedAccess.misthalinCrossingTiles(route: List<CoordGrid>): Int {
    var tiles = 0
    var from = player.coords
    for (waypoint in route) {
        tiles += from.chebyshevDistance(waypoint)
        from = waypoint
    }
    return maxOf(1, tiles)
}

internal fun misthalinCrossingOpenTicks(tiles: Int): Int =
    maxOf(MinOpenTicks, tiles + OpenTailTicks)

private const val MinOpenTicks = 4

private const val OpenTailTicks = 3

// Drop occupied leading waypoints so polling them does not consume a crossing tick.
internal suspend fun ProtectedAccess.misthalinCrossDoorway(route: List<CoordGrid>) {
    val trimmed = route.dropWhile { it == player.coords }
    if (trimmed.isEmpty()) {
        return
    }
    forcedWalk(trimmed, crossTiles = misthalinCrossingTiles(trimmed))
}

internal class MisthalinDoorLeaf(
    val tile: CoordGrid,
    val closed: String,
    val openLeaf: String,
    val openAngle: LocAngle,
    val closedAngle: LocAngle,
)

internal fun openMisthalinLeaf(locRepo: LocRepository, leaf: MisthalinDoorLeaf, openTicks: Int) {
    locRepo.add(leaf.tile, InvisWall, openTicks, leaf.closedAngle, LocShape.WallStraight)

    val (dx, dz) =
        when (leaf.closedAngle.id) {
            0 -> -1 to 0
            1 -> 0 to 1
            2 -> 1 to 0
            else -> 0 to -1
        }
    locRepo.add(
        CoordGrid(leaf.tile.x + dx, leaf.tile.z + dz, leaf.tile.level),
        leaf.openLeaf,
        openTicks,
        leaf.openAngle,
        LocShape.WallStraight,
    )
}

internal fun ProtectedAccess.misthalinDoorCreak() {
    soundSynth(DoorOpenSynth)
}

private const val InvisWall = "loc.inviswall"

private const val DoorOpenSynth = "synth.creakydoor_open"
