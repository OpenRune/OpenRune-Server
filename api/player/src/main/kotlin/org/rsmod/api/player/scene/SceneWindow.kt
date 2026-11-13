package org.rsmod.api.player.scene

import org.rsmod.api.repo.region.RegionStaticTemplate
import org.rsmod.game.region.Region
import org.rsmod.game.region.zone.RegionZoneCopy
import org.rsmod.game.region.zone.RegionZoneFlag
import org.rsmod.game.scene.SceneBuildArea
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

/**
 * One OSRS rebuild-region window. [table] uses the decoded packet layout: destination grid X,
 * destination grid Z, source zone X, source zone Z, rotation. Destination cells are region-zone
 * coordinates (0..39) in the packed scene region.
 *
 * [sourceBase] and [rotation] describe the scene-local source frame used by content coordinates.
 * The client table is independent of that frame; it is what makes the scene match the packet
 * rebuild window even when the content coordinates are not normal-world coordinates.
 */
public class SceneWindow(
    public val name: String,
    public val flag: RegionZoneFlag,
    public val buildCenterX: Int,
    public val buildCenterZ: Int,
    public val transformOffsetX: Int,
    public val transformOffsetZ: Int,
    public val sourceBase: CoordGrid,
    public val rotation: Int,
    public val table: IntArray,
) {
    init {
        require(table.size % ENTRY_SIZE == 0) {
            "Scene window table must be a multiple of $ENTRY_SIZE entries. (name=$name)"
        }
        var index = 0
        while (index < table.size) {
            val destX = table[index]
            val destZ = table[index + 1]
            require(destX in 0..MAX_REGION_ZONE && destZ in 0..MAX_REGION_ZONE) {
                "Scene window destination is outside the packed region. (name=$name, x=$destX, z=$destZ)"
            }
            index += ENTRY_SIZE
        }
    }

    public fun coords(region: Region): SceneWindowCoords = SceneWindowCoords(region, this)

    public fun buildArea(
        region: Region,
        centerX: Int = buildCenterX,
        centerZ: Int = buildCenterZ,
        reload: Boolean = false,
    ): SceneBuildArea {
        val regionBase = ZoneKey.from(region.southWest)
        val base = region.southWest.translate((centerX - 6) * 8, (centerZ - 6) * 8)
        val center = regionBase.translate(centerX, centerZ)
        val zones = LinkedHashMap<ZoneKey, RegionZoneCopy>(table.size / ENTRY_SIZE)
        var index = 0
        while (index < table.size) {
            val destX = table[index]
            val destZ = table[index + 1]
            val sourceX = table[index + 2]
            val sourceZ = table[index + 3]
            val rotation = table[index + 4]
            zones[ZoneKey(regionBase.x + destX, regionBase.z + destZ, 0)] =
                RegionZoneCopy(ZoneKey(sourceX, sourceZ, 0), rotation, flag)
            index += ENTRY_SIZE
        }
        return SceneBuildArea(base, center, reload, zones)
    }

    internal fun toWorld(region: Region, source: CoordGrid): CoordGrid {
        val dx = source.x - sourceBase.x
        val dz = source.z - sourceBase.z
        val (buildX, buildZ) = rotate(dx, dz)
        return region.southWest.translate(
            transformOffsetX * 8 + buildX,
            transformOffsetZ * 8 + buildZ,
        )
    }

    internal fun copyInto(template: RegionStaticTemplate) {
        var index = 0
        while (index < table.size) {
            template[table[index], table[index + 1], 0] =
                RegionZoneCopy(
                    ZoneKey(table[index + 2], table[index + 3], 0),
                    table[index + 4],
                    flag,
                )
            index += ENTRY_SIZE
        }
    }

    private fun rotate(x: Int, z: Int): Pair<Int, Int> =
        when (rotation) {
            1 -> BUILD_AREA_SIZE - 1 - z to x
            2 -> BUILD_AREA_SIZE - 1 - x to BUILD_AREA_SIZE - 1 - z
            3 -> z to BUILD_AREA_SIZE - 1 - x
            else -> x to z
        }

    private companion object {
        const val ENTRY_SIZE = 5
        const val BUILD_AREA_SIZE = 104
        const val MAX_REGION_ZONE = 39
    }
}

public class SceneWindowCoords internal constructor(
    private val region: Region,
    private val window: SceneWindow,
) {
    public operator fun get(source: CoordGrid): CoordGrid = window.toWorld(region, source)
}
