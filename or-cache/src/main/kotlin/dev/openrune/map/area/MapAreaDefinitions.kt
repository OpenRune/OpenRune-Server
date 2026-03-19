package dev.openrune.map.area

import org.rsmod.game.area.AreaIndex
import org.rsmod.map.square.MapSquareGrid
import org.rsmod.map.square.MapSquareKey
import org.rsmod.map.util.LocalMapSquareZone
import org.rsmod.map.zone.ZoneKey

object MapAreaDefinitions {

    public fun putAreas(index: AreaIndex, square: MapSquareKey, areaDef: MapAreaDefinition) {
        val squareBase = square.toCoords(level = 0)

        if (areaDef.mapSquareAreas.isNotEmpty()) {
            index.registerAll(square, areaDef.mapSquareAreas.iterator())
        }

        if (areaDef.zoneAreas.isNotEmpty()) {
            for ((packedZone, areas) in areaDef.zoneAreas.byte2ObjectEntrySet()) {
                val localZone = LocalMapSquareZone(packedZone.toInt())
                val zoneBase = localZone.toCoords(baseX = squareBase.x, baseZ = squareBase.z)
                val zoneKey = ZoneKey.from(zoneBase)
                index.registerAll(zoneKey, areas.iterator())
            }
        }

        if (areaDef.coordAreas.isNotEmpty()) {
            for ((packedGrid, areas) in areaDef.coordAreas.short2ObjectEntrySet()) {
                val grid = MapSquareGrid(packedGrid.toInt())
                val coord = squareBase.translate(grid.x, grid.z, grid.level)
                index.registerAll(coord, areas.iterator())
            }
        }
    }
}
