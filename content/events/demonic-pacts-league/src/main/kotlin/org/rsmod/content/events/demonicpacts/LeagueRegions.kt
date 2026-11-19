package org.rsmod.content.events.demonicpacts

import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

object LeagueRegions {
    private const val UNDERGROUND_OFFSET = 6400

    fun regionAt(coords: CoordGrid): LeagueRegion? {
        val regions = DemonicPactsData.regions
        for (region in regions) {
            if (region.rawBoxes.any { it.contains(coords.x, coords.z) }) {
                return region
            }
        }
        val surfaceZ = if (coords.z >= UNDERGROUND_OFFSET) coords.z - UNDERGROUND_OFFSET else coords.z
        for (region in regions) {
            if (region.boxes.any { it.contains(coords.x, surfaceZ) }) {
                return region
            }
        }
        return null
    }

    fun isAllowed(player: Player, coords: CoordGrid): Boolean {
        if (!DemonicPactsData.settings.regionLock) {
            return true
        }
        val region = regionAt(coords) ?: return true
        return player.hasRegion(region)
    }

    private fun List<Int>.contains(x: Int, z: Int): Boolean =
        x in this[0]..this[2] && z in this[1]..this[3]
}
