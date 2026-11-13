package org.rsmod.content.quest.area.asgarnia.icemountain

import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.game.region.Region
import org.rsmod.game.region.zone.RegionZoneCopy
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

internal object BimRegions {
    /** The Varrock square block used by the opening and cardio scenes. */
    val VarrockSquare: RegionTemplate = cutscene(BimCutsceneMaps.VARROCK_SQUARE)

    /** The Varrock palace block used by the final sprint. */
    val VarrockPalace: RegionTemplate = cutscene(BimCutsceneMaps.VARROCK_PALACE)

    /**
     * The Barbarian Village pushup, west-run and second-pushup blocks. All three share one region
     * and only change the build-area centre, matching the OSRS rebuild sequence.
     */
    val BarbarianUnion: RegionTemplate =
        cutscene(
            BimCutsceneMaps.BARBARIAN_WEST +
                shiftDestinationX(BimCutsceneMaps.BARBARIAN_PUSHUP, 5),
        )

    private fun shiftDestinationX(entries: IntArray, shift: Int): IntArray {
        val copy = entries.copyOf()
        var index = 0
        while (index < copy.size) {
            copy[index] += shift
            index += 5
        }
        return copy
    }

    private fun cutscene(entries: IntArray): RegionTemplate {
        return RegionTemplate.createLarge {
            terrainOnly = true
            val seen = BooleanArray(40 * 40)
            var index = 0
            while (index < entries.size) {
                val destX = entries[index]
                val destZ = entries[index + 1]
                val sourceX = entries[index + 2]
                val sourceZ = entries[index + 3]
                val rotation = entries[index + 4]
                val key = destX * 40 + destZ
                if (!seen[key]) {
                    seen[key] = true
                    this[destX, destZ, 0] =
                        RegionZoneCopy(ZoneKey(sourceX, sourceZ, 0), rotation, flag = null)
                }
                index += 5
            }
        }
    }
}

/**
 * Maps a normal-map coordinate into the cutscene region, applying the area rotation that the client
 * applies when copying the source zones into the build area.
 */
internal class CutsceneScene(
    private val region: Region,
    private val rotation: Int,
    private val sourceBase: CoordGrid,
    private val centerCellX: Int = 12,
    private val centerCellZ: Int = 12,
) {
    operator fun get(coords: CoordGrid): CoordGrid {
        val dx = coords.x - sourceBase.x
        val dz = coords.z - sourceBase.z
        val (buildX, buildZ) = rotate(dx, dz)
        return region.southWest.translate(
            (centerCellX - 6) * 8 + buildX,
            (centerCellZ - 6) * 8 + buildZ,
        )
    }

    fun buildAreaBase(): CoordGrid =
        region.southWest.translate((centerCellX - 6) * 8, (centerCellZ - 6) * 8)

    fun buildAreaBase(centerCellX: Int, centerCellZ: Int): CoordGrid =
        region.southWest.translate((centerCellX - 6) * 8, (centerCellZ - 6) * 8)

    private fun rotate(x: Int, z: Int): Pair<Int, Int> =
        when (rotation) {
            1 -> BUILD_AREA_SIZE - 1 - z to x
            2 -> BUILD_AREA_SIZE - 1 - x to BUILD_AREA_SIZE - 1 - z
            3 -> z to BUILD_AREA_SIZE - 1 - x
            else -> x to z
        }

    private companion object {
        const val BUILD_AREA_SIZE = 104
    }
}
