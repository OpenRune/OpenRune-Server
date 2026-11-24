package org.rsmod.content.quest.mudfoot

import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.game.region.Region
import org.rsmod.map.CoordGrid

/**
 * A block of whole zones copied into an instance for one of the quest's scenes. [anchor] is any
 * static tile inside the block, used to translate instanced coords back to the static map.
 */
internal class MudfootArea(
    private val zoneX: Int,
    private val zoneZ: Int,
    private val zoneWidth: Int,
    private val zoneLength: Int,
    val anchor: CoordGrid,
) {
    val template: RegionTemplate =
        RegionTemplate.create {
            copy(zoneX, zoneZ, 0) {
                regionZoneX = REGION_OFFSET_ZONES
                regionZoneZ = REGION_OFFSET_ZONES
                zoneWidth = this@MudfootArea.zoneWidth
                zoneLength = this@MudfootArea.zoneLength
            }
        }

    fun contains(x: Int, z: Int): Boolean =
        x in zoneX * ZONE_SIZE until (zoneX + zoneWidth) * ZONE_SIZE &&
            z in zoneZ * ZONE_SIZE until (zoneZ + zoneLength) * ZONE_SIZE

    private companion object {
        const val ZONE_SIZE = 8
        const val REGION_OFFSET_ZONES = 4
    }
}

/** Maps the static coords of a [MudfootArea] into its instanced copy, and back again. */
internal class MudfootScene(val area: MudfootArea, val region: Region) {
    private val mappedAnchor = region.normal[area.anchor]

    operator fun get(coords: CoordGrid): CoordGrid = region.normal[coords]

    fun toStatic(coords: CoordGrid): CoordGrid =
        coords.translate(area.anchor.x - mappedAnchor.x, area.anchor.z - mappedAnchor.z)

    fun contains(coords: CoordGrid): Boolean =
        coords.level == mappedAnchor.level &&
            area.contains(
                coords.x - mappedAnchor.x + area.anchor.x,
                coords.z - mappedAnchor.z + area.anchor.z,
            )
}

internal object MudfootAreas {
    val GENERAL_STORE =
        MudfootArea(zoneX = 401, zoneZ = 404, zoneWidth = 3, zoneLength = 4, CoordGrid(3219, 3245, 0))
    val WINDMILL =
        MudfootArea(zoneX = 394, zoneZ = 409, zoneWidth = 4, zoneLength = 4, CoordGrid(3167, 3288, 0))
    val WOODS =
        MudfootArea(zoneX = 395, zoneZ = 403, zoneWidth = 3, zoneLength = 4, CoordGrid(3171, 3241, 0))
    val GRAVEYARD =
        MudfootArea(zoneX = 404, zoneZ = 398, zoneWidth = 3, zoneLength = 3, CoordGrid(3244, 3197, 0))
}
