package org.rsmod.content.raids.toa.pack

import dev.openrune.definition.dbtables.DBRowBuilder
import dev.openrune.definition.dbtables.DBTable
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import org.rsmod.map.CoordGrid

object HetPuzzleTables {

    const val INTERNAL_TILES = 0
    const val INTERNAL_ANGLES = 1
    const val END_TILES = 2
    const val END_ANGLES = 3
    const val BREAKABLE_INTERNAL_TILES = 4
    const val BREAKABLE_INTERNAL_ANGLES = 5
    const val BREAKABLE_END_TILES = 6
    const val BREAKABLE_END_ANGLES = 7
    const val FIXED_MIRROR_TILES = 8
    const val FIXED_MIRROR_ANGLES = 9

    const val SEAL_TEAM_SIZE = 0
    const val SEAL_HP = 1

    private val SEAL_HP_BY_TEAM = listOf(119, 225, 321, 417, 512, 608, 704, 800)

    private class Piece(val x: Int, val z: Int, val angle: Int)

    private fun p(x: Int, z: Int, angle: Int) = Piece(x, z, angle)

    private fun DBRowBuilder.pieces(tiles: Int, angles: Int, pieces: List<Piece>) {
        column(tiles, pieces.map { CoordGrid(it.x, it.z, 0).packed })
        column(angles, pieces.map { it.angle })
    }

    fun layouts(): DBTable =
        dbTable("dbtable.toa_het_layouts", serverOnly = true) {
            column("internal_tiles", INTERNAL_TILES, VarType.COORDGRID)
            column("internal_angles", INTERNAL_ANGLES, VarType.INT)
            column("end_tiles", END_TILES, VarType.COORDGRID)
            column("end_angles", END_ANGLES, VarType.INT)
            column("breakable_internal_tiles", BREAKABLE_INTERNAL_TILES, VarType.COORDGRID)
            column("breakable_internal_angles", BREAKABLE_INTERNAL_ANGLES, VarType.INT)
            column("breakable_end_tiles", BREAKABLE_END_TILES, VarType.COORDGRID)
            column("breakable_end_angles", BREAKABLE_END_ANGLES, VarType.INT)
            column("fixed_mirror_tiles", FIXED_MIRROR_TILES, VarType.COORDGRID)
            column("fixed_mirror_angles", FIXED_MIRROR_ANGLES, VarType.INT)

            row("dbrow.toa_het_layout_0") {
                pieces(
                    INTERNAL_TILES,
                    INTERNAL_ANGLES,
                    listOf(
                        p(3687, 5280, 3), p(3687, 5281, 1), p(3687, 5282, 3), p(3681, 5285, 3),
                        p(3681, 5286, 3), p(3683, 5285, 1), p(3683, 5286, 3), p(3674, 5282, 3),
                        p(3674, 5283, 1), p(3674, 5284, 1), p(3677, 5285, 0), p(3678, 5285, 0),
                        p(3679, 5285, 2), p(3679, 5278, 2), p(3680, 5278, 2), p(3681, 5278, 2),
                        p(3682, 5278, 0), p(3683, 5278, 0), p(3684, 5278, 2), p(3685, 5278, 0),
                        p(3686, 5278, 0), p(3682, 5273, 1), p(3682, 5274, 1),
                    ),
                )
                pieces(
                    END_TILES,
                    END_ANGLES,
                    listOf(
                        p(3680, 5285, 0), p(3687, 5283, 3), p(3681, 5284, 1), p(3681, 5287, 3),
                        p(3683, 5284, 1), p(3683, 5287, 3), p(3674, 5281, 1), p(3674, 5285, 3),
                        p(3676, 5285, 2), p(3678, 5278, 2), p(3687, 5278, 0), p(3687, 5279, 1),
                        p(3682, 5272, 1), p(3682, 5275, 3),
                    ),
                )
                pieces(
                    BREAKABLE_INTERNAL_TILES,
                    BREAKABLE_INTERNAL_ANGLES,
                    listOf(
                        p(3681, 5282, 0), p(3682, 5282, 0), p(3683, 5282, 2), p(3684, 5282, 2),
                        p(3686, 5286, 0), p(3687, 5286, 2),
                    ),
                )
                pieces(
                    BREAKABLE_END_TILES,
                    BREAKABLE_END_ANGLES,
                    listOf(
                        p(3680, 5282, 2), p(3685, 5282, 0), p(3685, 5286, 2), p(3688, 5286, 0),
                    ),
                )
                pieces(
                    FIXED_MIRROR_TILES,
                    FIXED_MIRROR_ANGLES,
                    listOf(
                        p(3675, 5288, 1), p(3674, 5277, 0), p(3677, 5276, 0),
                    ),
                )
            }

            row("dbrow.toa_het_layout_1") {
                pieces(
                    INTERNAL_TILES,
                    INTERNAL_ANGLES,
                    listOf(
                        p(3682, 5283, 1), p(3682, 5284, 1), p(3682, 5285, 3), p(3675, 5282, 0),
                        p(3676, 5282, 0), p(3677, 5282, 0), p(3677, 5285, 0), p(3678, 5285, 2),
                        p(3679, 5285, 2), p(3678, 5275, 1), p(3678, 5276, 1), p(3678, 5277, 3),
                        p(3681, 5275, 0), p(3682, 5275, 2), p(3683, 5275, 2), p(3683, 5278, 0),
                        p(3684, 5278, 2), p(3685, 5278, 0),
                    ),
                )
                pieces(
                    END_TILES,
                    END_ANGLES,
                    listOf(
                        p(3680, 5285, 0), p(3682, 5282, 1), p(3682, 5286, 3), p(3674, 5282, 2),
                        p(3678, 5282, 0), p(3676, 5285, 2), p(3678, 5274, 1), p(3678, 5278, 3),
                        p(3680, 5275, 2), p(3684, 5275, 0), p(3682, 5278, 2), p(3686, 5278, 0),
                    ),
                )
                pieces(
                    BREAKABLE_INTERNAL_TILES,
                    BREAKABLE_INTERNAL_ANGLES,
                    listOf(
                        p(3686, 5284, 2), p(3687, 5284, 2), p(3688, 5284, 2), p(3672, 5277, 0),
                        p(3673, 5277, 2), p(3674, 5277, 2),
                    ),
                )
                pieces(
                    BREAKABLE_END_TILES,
                    BREAKABLE_END_ANGLES,
                    listOf(
                        p(3685, 5284, 2), p(3689, 5284, 0), p(3675, 5277, 0), p(3671, 5277, 2),
                    ),
                )
                pieces(
                    FIXED_MIRROR_TILES,
                    FIXED_MIRROR_ANGLES,
                    listOf(
                        p(3685, 5285, 2), p(3673, 5286, 1), p(3674, 5273, 0), p(3684, 5273, 3),
                    ),
                )
            }

            row("dbrow.toa_het_layout_2") {
                pieces(
                    INTERNAL_TILES,
                    INTERNAL_ANGLES,
                    listOf(
                        p(3687, 5280, 1), p(3687, 5281, 3), p(3687, 5282, 1), p(3681, 5285, 1),
                        p(3681, 5286, 1), p(3683, 5285, 1), p(3683, 5286, 1), p(3674, 5282, 3),
                        p(3674, 5283, 3), p(3674, 5284, 1), p(3679, 5278, 2), p(3680, 5278, 0),
                        p(3681, 5278, 0), p(3682, 5278, 2), p(3683, 5278, 2), p(3684, 5278, 2),
                        p(3685, 5278, 0), p(3686, 5278, 2),
                    ),
                )
                pieces(
                    END_TILES,
                    END_ANGLES,
                    listOf(
                        p(3687, 5283, 3), p(3681, 5284, 1), p(3681, 5287, 3), p(3683, 5284, 1),
                        p(3683, 5287, 3), p(3674, 5281, 1), p(3674, 5285, 3), p(3678, 5278, 2),
                        p(3687, 5278, 0), p(3687, 5279, 1),
                    ),
                )
                pieces(
                    BREAKABLE_INTERNAL_TILES,
                    BREAKABLE_INTERNAL_ANGLES,
                    listOf(
                        p(3681, 5282, 0), p(3682, 5282, 2), p(3683, 5282, 0), p(3684, 5282, 0),
                        p(3686, 5286, 0), p(3687, 5286, 0), p(3677, 5285, 0), p(3678, 5285, 2),
                        p(3679, 5285, 2), p(3682, 5273, 1), p(3682, 5274, 3),
                    ),
                )
                pieces(
                    BREAKABLE_END_TILES,
                    BREAKABLE_END_ANGLES,
                    listOf(
                        p(3680, 5285, 0), p(3680, 5282, 2), p(3685, 5282, 0), p(3685, 5286, 2),
                        p(3688, 5286, 0), p(3676, 5285, 2), p(3682, 5272, 1), p(3682, 5275, 3),
                    ),
                )
                pieces(
                    FIXED_MIRROR_TILES,
                    FIXED_MIRROR_ANGLES,
                    listOf(
                        p(3672, 5280, 1), p(3675, 5283, 1), p(3675, 5276, 3), p(3689, 5277, 3),
                    ),
                )
            }

            row("dbrow.toa_het_layout_3") {
                pieces(
                    INTERNAL_TILES,
                    INTERNAL_ANGLES,
                    listOf(
                        p(3682, 5283, 3), p(3682, 5284, 1), p(3682, 5285, 1), p(3675, 5282, 0),
                        p(3676, 5282, 0), p(3677, 5282, 0), p(3677, 5285, 0), p(3678, 5285, 2),
                        p(3679, 5285, 0), p(3678, 5275, 1), p(3678, 5276, 1), p(3678, 5277, 1),
                        p(3681, 5275, 2), p(3682, 5275, 0), p(3683, 5275, 0), p(3683, 5278, 2),
                        p(3684, 5278, 0), p(3685, 5278, 0),
                    ),
                )
                pieces(
                    END_TILES,
                    END_ANGLES,
                    listOf(
                        p(3680, 5285, 0), p(3682, 5282, 1), p(3682, 5286, 3), p(3674, 5282, 2),
                        p(3678, 5282, 0), p(3676, 5285, 2), p(3678, 5274, 1), p(3678, 5278, 3),
                        p(3680, 5275, 2), p(3684, 5275, 0), p(3682, 5278, 2), p(3686, 5278, 0),
                    ),
                )
                pieces(
                    BREAKABLE_INTERNAL_TILES,
                    BREAKABLE_INTERNAL_ANGLES,
                    listOf(
                        p(3686, 5284, 0), p(3687, 5284, 0), p(3688, 5284, 2), p(3672, 5277, 0),
                        p(3673, 5277, 2), p(3674, 5277, 0),
                    ),
                )
                pieces(
                    BREAKABLE_END_TILES,
                    BREAKABLE_END_ANGLES,
                    listOf(
                        p(3685, 5284, 2), p(3689, 5284, 0), p(3675, 5277, 0), p(3671, 5277, 2),
                    ),
                )
                pieces(
                    FIXED_MIRROR_TILES,
                    FIXED_MIRROR_ANGLES,
                    listOf(
                        p(3687, 5283, 0), p(3685, 5286, 2), p(3687, 5281, 2), p(3674, 5276, 0),
                    ),
                )
            }
        }

    fun seal(): DBTable =
        dbTable("dbtable.toa_het_seal", serverOnly = true) {
            column("team_size", SEAL_TEAM_SIZE, VarType.INT)
            column("hp", SEAL_HP, VarType.INT)

            SEAL_HP_BY_TEAM.forEachIndexed { index, hp ->
                row("dbrow.toa_het_seal_${index + 1}") {
                    column(SEAL_TEAM_SIZE, index + 1)
                    column(SEAL_HP, hp)
                }
            }
        }
}
