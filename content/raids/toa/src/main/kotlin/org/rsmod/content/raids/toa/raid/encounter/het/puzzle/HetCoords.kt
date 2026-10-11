package org.rsmod.content.raids.toa.raid.encounter.het.puzzle

import org.rsmod.map.CoordGrid

internal class HetMirrorSpawn(val tile: CoordGrid, val rotation: Int)

internal object HetCoords {
    val SEAL = CoordGrid(3679, 5279, 0)
    val BEAM_START = CoordGrid(3675, 5280, 0)

    val MIN = CoordGrid(3670, 5267, 0)
    val MAX = CoordGrid(3690, 5293, 0)

    val CASTER_MIN = CoordGrid(3676, 5279, 0)
    val CASTER_MAX = CoordGrid(3678, 5281, 0)
    val TARGET_MIN = CoordGrid(3682, 5279, 0)
    val TARGET_MAX = CoordGrid(3684, 5281, 0)
    val SEAL_MIN = SEAL
    val SEAL_MAX = CoordGrid(3681, 5281, 0)

    val EXIT_BARRIER =
        listOf(CoordGrid(3669, 5279, 0), CoordGrid(3669, 5280, 0), CoordGrid(3669, 5281, 0))

    val MIRROR_SPAWNS =
        listOf(
            HetMirrorSpawn(CoordGrid(3689, 5285, 0), rotation = 2),
            HetMirrorSpawn(CoordGrid(3675, 5289, 0), rotation = 3),
            HetMirrorSpawn(CoordGrid(3675, 5271, 0), rotation = 1),
        )

    const val ORB_LINE_MIN_X = 3671
    const val ORB_LINE_MAX_X = 3689
    const val ORB_LINE_MIN_Z = 5271
    const val ORB_LINE_MAX_Z = 5289
}

internal fun CoordGrid.within(min: CoordGrid, max: CoordGrid): Boolean =
    x in min.x..max.x && z in min.z..max.z
