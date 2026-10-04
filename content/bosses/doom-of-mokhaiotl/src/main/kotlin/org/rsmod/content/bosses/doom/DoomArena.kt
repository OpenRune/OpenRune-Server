package org.rsmod.content.bosses.doom

import org.rsmod.api.bosses.dsl.area
import org.rsmod.api.bosses.dsl.spawnTile
import org.rsmod.api.bosses.spec.Area
import org.rsmod.map.CoordGrid

internal object DoomArena {
    const val REGION = 5269

    val BOSS_SPAWN = CoordGrid(1309, 9571, 0)
    val LANDING = CoordGrid(1311, 9559, 0)
    val GAP = CoordGrid(1310, 9557, 0)
    val LOBBY = CoordGrid(1311, 9551, 0)

    const val FLOOR_MIN_X = -10
    const val FLOOR_MAX_X = 14
    const val FLOOR_MIN_Z = -12
    const val FLOOR_MAX_Z = 14

    val ATTACK_RANGE = maxOf(FLOOR_MAX_X - FLOOR_MIN_X, FLOOR_MAX_Z - FLOOR_MIN_Z)

    val AREA: Area = area(spawnTile(FLOOR_MIN_X, FLOOR_MIN_Z), spawnTile(FLOOR_MAX_X, FLOOR_MAX_Z))

    fun onFloor(bossSpawn: CoordGrid, tile: CoordGrid): Boolean =
        tile.level == bossSpawn.level &&
            tile.x - bossSpawn.x in FLOOR_MIN_X..FLOOR_MAX_X &&
            tile.z - bossSpawn.z in FLOOR_MIN_Z..FLOOR_MAX_Z
}
