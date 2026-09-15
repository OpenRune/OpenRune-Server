package org.rsmod.content.skills.construction

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.table.FurnitureRow
import org.rsmod.game.region.Region
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneGrid

val entryScript: Int by lazy { CLIENTSCRIPT_FURNITURE_ENTRY.asRSCM(RSCMType.CLIENTSCRIPT) }

class HotspotTarget(val slot: Int, val rotation: Int, val hotspot: HotspotDef)

class DoorTarget(val slot: Int, val direction: Int)

class HouseSession(val region: Region, val layout: HouseLayout) {
    fun slotAt(coords: CoordGrid): Int? {
        val x = (coords.x - region.southWest.x) / ZoneGrid.LENGTH
        val z = (coords.z - region.southWest.z) / ZoneGrid.LENGTH
        if (!inGrid(coords.level, x, z)) {
            return null
        }
        return slotKey(coords.level, x, z)
    }

    /**
     * The hotspot a click landed on.
     *
     * Matched on the loc that was clicked rather than on where the room's template says its parts
     * sit. Rotating a room rotates the anchor tile of each loc, and a loc wider than one tile does
     * not keep that anchor once turned - a portal is 2x1, so in a room placed at 180 degrees its
     * computed tile is one off the tile it is actually on, and comparing coords found nothing.
     */
    fun resolve(
        catalogue: ConstructionCatalogue,
        houses: HouseRegions,
        coords: CoordGrid,
        locId: Int? = null,
    ): HotspotTarget? {
        val slot = slotAt(coords) ?: return null
        val placed = layout.placed(slot) ?: return null
        val def = catalogue.room(placed.room) ?: return null
        val byLoc = locId?.let { id -> def.hotspots.firstOrNull { it.parts.any { p -> p.locId == id } } }
        val hotspot =
            byLoc
                ?: def.hotspots.firstOrNull { spot ->
                    spot.parts.any {
                        houses.localCoords(region, slot, placed.rotation, it.localX, it.localZ) == coords
                    }
                }
                ?: return null
        return HotspotTarget(slot, placed.rotation, hotspot)
    }

    fun resolveDoor(
        catalogue: ConstructionCatalogue,
        houses: HouseRegions,
        coords: CoordGrid,
    ): DoorTarget? {
        val slot = slotAt(coords) ?: return null
        val placed = layout.placed(slot) ?: return null
        val def = catalogue.room(placed.room) ?: return null
        val door =
            def.doors.firstOrNull {
                houses.localCoords(region, slot, placed.rotation, it.localX, it.localZ) == coords
            } ?: return null
        return DoorTarget(slot, (door.direction + placed.rotation) and 3)
    }
}

fun FurnitureRow.buildLevel(): Int = levelRequirement.firstOrNull()?.t1 ?: 1

fun FurnitureRow.materials(): List<Pair<String, Int>> =
    materialCost.map { it.t0.internalName to it.t1 }

fun FurnitureRow.materialText(): String =
    materialCost.joinToString(", ") { "${it.t1} x ${it.t0.name}" }

/**
 * The reference table's value where it has one, because `dbtable.furniture` has no xp column and the
 * material total only reproduces the standard plank builds: anything made of something outside
 * [MATERIAL_XP] works out to zero.
 */
fun FurnitureRow.xp(): Double =
    furnitureXp(modelObj.id)
        ?: materialCost.sumOf { (MATERIAL_XP[it.t0.internalName] ?: 0.0) * it.t1 }
