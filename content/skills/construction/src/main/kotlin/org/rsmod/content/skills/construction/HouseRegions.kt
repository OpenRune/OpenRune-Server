package org.rsmod.content.skills.construction

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.game.region.Region
import org.rsmod.game.region.util.RegionRotations
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneGrid

/**
 * Allocates the dynamic region a house lives in. Every placed room is one 8x8 zone copied out of
 * its source chunk at the room's grid slot, rotated to match how the player placed it.
 */
@Singleton
class HouseRegions
@Inject
constructor(private val regionRepo: RegionRepository, private val catalogue: ConstructionCatalogue) {
    fun allocate(layout: HouseLayout): Region? {
        val placements =
            layout.rooms.mapNotNull { (slot, placed) ->
                val def = catalogue.room(placed.room) ?: return@mapNotNull null
                Triple(slot, placed, def)
            }
        if (placements.isEmpty()) {
            return null
        }
        val template =
            RegionTemplate.create {
                for ((slot, placed, def) in placements) {
                    val zone = def.sourceZone
                    val x = slotX(slot)
                    val z = slotZ(slot)
                    val level = slotLevel(slot)
                    when (placed.rotation) {
                        1 -> this[x, z, level] = zone.rotate90()
                        2 -> this[x, z, level] = zone.rotate180()
                        3 -> this[x, z, level] = zone.rotate270()
                        else -> this[x, z, level] = zone
                    }
                }
            }
        return regionRepo.add(template)
    }

    fun roomBase(region: Region, slot: Int): CoordGrid =
        CoordGrid(
            region.southWest.x + slotX(slot) * ZoneGrid.LENGTH,
            region.southWest.z + slotZ(slot) * ZoneGrid.LENGTH,
            slotLevel(slot),
        )

    fun partCoords(region: Region, slot: Int, rotation: Int, part: HotspotPart): CoordGrid =
        localCoords(region, slot, rotation, part.localX, part.localZ)

    fun localCoords(
        region: Region,
        slot: Int,
        rotation: Int,
        localX: Int,
        localZ: Int,
    ): CoordGrid {
        val base = roomBase(region, slot)
        if (rotation == 0) {
            return base.translate(localX, localZ)
        }
        val grid = ZoneGrid(localX, localZ, 0)
        return base.translate(RegionRotations.translateCoords(rotation, grid))
    }
}
