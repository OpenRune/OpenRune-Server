package org.rsmod.content.skills.construction

import dev.openrune.ServerCacheManager
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

    /**
     * Where a hotspot's part ends up once its room is turned.
     *
     * A loc keeps its south-west corner only while it is one tile square. Turn a room holding a 2x1
     * portal and the portal's anchor moves along its own width, so rotating the tile alone lands a
     * tile out - which is why this asks [RegionRotations.translateLoc] for the loc's own footprint
     * rather than [localCoords]. The footprint is swapped when the loc's own angle is a quarter turn.
     */
    fun partCoords(region: Region, slot: Int, rotation: Int, part: HotspotPart): CoordGrid {
        val base = roomBase(region, slot)
        if (rotation == 0) {
            return base.translate(part.localX, part.localZ)
        }
        val type = ServerCacheManager.getObject(part.locId)
        val turned = part.angleId and 1 == 1
        val sizeX = type?.width ?: 1
        val sizeZ = type?.length ?: 1
        val width = if (turned) sizeZ else sizeX
        val length = if (turned) sizeX else sizeZ
        val grid = ZoneGrid(part.localX, part.localZ, 0)
        return base.translate(RegionRotations.translateLoc(rotation, grid, width, length))
    }

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
