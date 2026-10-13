package org.rsmod.content.raids.toa.raid.encounter.apmeken.puzzle

import org.rsmod.map.CoordGrid

internal object ApmekenCoords {
    val BABOON_SPAWNS =
        listOf(
            CoordGrid(3800, 5288, 0),
            CoordGrid(3808, 5288, 0),
            CoordGrid(3816, 5288, 0),
            CoordGrid(3800, 5272, 0),
            CoordGrid(3808, 5272, 0),
            CoordGrid(3816, 5272, 0),
        )

    val VENTS =
        listOf(
            CoordGrid(3800, 5284, 0),
            CoordGrid(3816, 5284, 0),
            CoordGrid(3816, 5276, 0),
            CoordGrid(3800, 5276, 0),
        )

    val NORTH_VENTS = VENTS.filter { it.z > CENTRE_Z }
    val SOUTH_VENTS = VENTS.filter { it.z < CENTRE_Z }

    val SUPPORTS =
        listOf(
            CoordGrid(3803, 5289, 0),
            CoordGrid(3811, 5289, 0),
            CoordGrid(3803, 5269, 0),
            CoordGrid(3811, 5269, 0),
        )

    val SUPPORT_FRONTS =
        listOf(
            CoordGrid(3804, 5288, 0),
            CoordGrid(3812, 5288, 0),
            CoordGrid(3804, 5272, 0),
            CoordGrid(3812, 5272, 0),
        )

    val EXIT_BARRIER =
        listOf(CoordGrid(3820, 5279, 0), CoordGrid(3820, 5280, 0), CoordGrid(3820, 5281, 0))

    val CRATE_HAMMERS = CoordGrid(3793, 5284, 0)
    val CRATE_POTIONS = CoordGrid(3795, 5285, 0)

    val AVOID_TILES = VENTS + SUPPORT_FRONTS

    private const val CENTRE_Z = 5280
}
