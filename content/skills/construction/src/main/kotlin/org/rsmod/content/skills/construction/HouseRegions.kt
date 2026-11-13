package org.rsmod.content.skills.construction

import dev.openrune.ServerCacheManager
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.game.region.Region
import org.rsmod.game.region.util.RegionRotations
import org.rsmod.game.region.zone.RegionZoneCopy
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneGrid
import org.rsmod.map.zone.ZoneKey

/**
 * Allocates the dynamic region a house lives in. Every placed room is one 8x8 zone copied out of
 * its source chunk at the room's grid slot, rotated to match how the player placed it.
 */
@Singleton
class HouseRegions
@Inject
constructor(private val regionRepo: RegionRepository, private val catalogue: ConstructionCatalogue) {
    /**
     * Every slot without a room takes its floor's filler, as live sends it: lawn across the ground
     * floor turned a quarter more each step, the bare basement below, and upstairs the blank chunk,
     * whose tiles all block, reaching one slot past the grid so a doorway into nothing upstairs
     * cannot be walked through.
     */
    fun allocate(layout: HouseLayout): Region? {
        if (layout.rooms.none { catalogue.room(it.value.room) != null }) {
            return null
        }
        val template =
            RegionTemplate.create {
                for (level in 0 until HOUSE_LEVELS) {
                    for (x in -1..HOUSE_GRID) {
                        for (z in -1..HOUSE_GRID) {
                            val slot = RoomSlot.of(level, x, z)
                            val placed = slot?.let(layout::placed)
                            val def = placed?.let { catalogue.room(it.room) }
                            val zone =
                                if (def != null) {
                                    RegionZoneCopy(def.sourceZone, placed.rotation, flag = null)
                                } else {
                                    filler(level, x, z, inGrid = slot != null) ?: continue
                                }
                            this[x + HOUSE_GRID_OFFSET, z + HOUSE_GRID_OFFSET, level] = zone
                        }
                    }
                }
            }
        return regionRepo.add(template)
    }

    private fun filler(level: Int, x: Int, z: Int, inGrid: Boolean): RegionZoneCopy? {
        val (offset, rotation) =
            when {
                level == LEVEL_UPSTAIRS -> BLANK to 0
                !inGrid -> return null
                level == LEVEL_GROUND -> LAWN to quarterTurns(x + z, 0)
                else -> BASEMENT to 0
            }
        return RegionZoneCopy(ZoneKey.from(offset), rotation, flag = null)
    }

    fun roomBase(region: Region, slot: RoomSlot): CoordGrid =
        CoordGrid(
            region.southWest.x + (slot.x + HOUSE_GRID_OFFSET) * ZoneGrid.LENGTH,
            region.southWest.z + (slot.z + HOUSE_GRID_OFFSET) * ZoneGrid.LENGTH,
            slot.level,
        )

    /**
     * The hotspot a click on [locId] at [coords] landed on, whether the loc is the empty hotspot or
     * a piece built on it.
     *
     * A part is matched on the tile its loc actually occupies once the room is turned, which for a
     * loc wider than one tile is not where its template anchor rotates to: a 2x1 portal or a 4x2
     * table in a room placed at 180 degrees sits a tile off. Where a room repeats a hotspot the
     * tile tells them apart; the loc alone settles a click whose tile matches nothing.
     */
    fun hotspotAt(session: HouseSession, coords: CoordGrid, locId: Int? = null): HotspotTarget? {
        val slot = session.slotAt(coords) ?: return null
        val placed = session.layout.placed(slot) ?: return null
        val def = catalogue.room(placed.room) ?: return null
        val candidates =
            if (locId == null) def.hotspots else def.hotspots.filter { shows(session, slot, it, locId) }
        val hotspot =
            candidates.firstOrNull { spot ->
                spot.parts.any { partCoords(session.region, slot, placed.rotation, it) == coords }
            } ?: candidates.singleOrNull() ?: return null
        return HotspotTarget(slot, placed.rotation, hotspot)
    }

    /** Whether [locId] is [hotspot] itself or what is built on it in [slot]. */
    private fun shows(session: HouseSession, slot: RoomSlot, hotspot: HotspotDef, locId: Int): Boolean {
        if (hotspot.parts.any { it.locId == locId }) {
            return true
        }
        if (session.layout.variant(slot, hotspot.index) == locId) {
            return true
        }
        val row = session.layout.built(slot, hotspot.index) ?: return false
        val furniture = hotspot.builds.firstOrNull { it.rowId == row } ?: return false
        return locId in catalogue.builtLocIds(furniture) ||
            hotspot.parts.any { catalogue.builtLocFor(furniture, it) == locId }
    }

    fun doorAt(session: HouseSession, coords: CoordGrid): DoorTarget? {
        val slot = session.slotAt(coords) ?: return null
        val placed = session.layout.placed(slot) ?: return null
        val def = catalogue.room(placed.room) ?: return null
        val door =
            def.doors.firstOrNull {
                localCoords(session.region, slot, placed.rotation, it.localX, it.localZ) == coords
            } ?: return null
        return DoorTarget(slot, door.direction.turned(placed.rotation))
    }

    /**
     * Where a hotspot's part ends up once its room is turned.
     *
     * A loc keeps its south-west corner only while it is one tile square. Turn a room holding a 2x1
     * portal and the portal's anchor moves along its own width, so rotating the tile alone lands a
     * tile out - which is why this asks [RegionRotations.translateLoc] for the loc's own footprint
     * rather than [localCoords]. The footprint is swapped when the loc's own angle is a quarter turn.
     */
    fun partCoords(region: Region, slot: RoomSlot, rotation: Int, part: HotspotPart): CoordGrid {
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
        slot: RoomSlot,
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

    private companion object {
        /** Filler template chunks, by their south-west tile in the house template grid. */
        val LAWN = CoordGrid(1864, 7040, 0)
        val BASEMENT = CoordGrid(1880, 7040, 0)
        val BLANK = CoordGrid(1888, 7048, 0)
    }
}
