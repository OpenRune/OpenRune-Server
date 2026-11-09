package dev.openrune.tables

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object GauntletRoomSlotsTable {

    const val COL_KIND = 0
    const val COL_VARIANT = 1
    const val COL_ROCK = 2
    const val COL_TREE = 3
    const val COL_POND = 4
    const val COL_HERB = 5
    const val COL_FIBRE = 6

    private fun pack(x: Int, z: Int) = (x shl 8) or z

    fun table() =
        dbTable("dbtable.gauntlet_room_slots", serverOnly = true) {
            column("kind", COL_KIND, VarType.INT)
            column("variant", COL_VARIANT, VarType.INT)
            column("rock", COL_ROCK, VarType.INT)
            column("tree", COL_TREE, VarType.INT)
            column("pond", COL_POND, VarType.INT)
            column("herb", COL_HERB, VarType.INT)
            column("fibre", COL_FIBRE, VarType.INT)

            row("dbrow.gauntlet_slots_middle_0") {
                column(COL_KIND, 0)
                column(COL_VARIANT, 0)
                column(COL_ROCK, pack(13, 3), pack(3, 3), pack(8, 6), pack(10, 11), pack(10, 5), pack(2, 12), pack(2, 2), pack(3, 13), pack(4, 11))
                column(COL_TREE, pack(13, 3), pack(2, 12), pack(3, 13), pack(3, 3), pack(8, 6), pack(0, 1), pack(10, 5), pack(11, 10), pack(12, 12), pack(12, 2), pack(13, 13), pack(15, 2), pack(2, 2), pack(5, 4))
                column(COL_POND, pack(11, 10), pack(12, 12), pack(10, 5), pack(12, 2), pack(15, 2), pack(2, 12), pack(3, 13), pack(3, 3), pack(4, 11), pack(4, 5), pack(7, 5), pack(8, 6))
                column(COL_HERB, pack(12, 2), pack(2, 2), pack(3, 13), pack(5, 10), pack(10, 5), pack(12, 12), pack(15, 2), pack(2, 12), pack(4, 5), pack(7, 5), pack(8, 6))
                column(COL_FIBRE, pack(0, 1), pack(11, 10), pack(11, 4), pack(13, 13), pack(13, 3), pack(5, 4), pack(7, 5), pack(8, 6), pack(10, 11), pack(10, 5), pack(4, 11))
            }

            row("dbrow.gauntlet_slots_middle_1") {
                column(COL_KIND, 0)
                column(COL_VARIANT, 1)
                column(COL_ROCK, pack(11, 10), pack(8, 11), pack(12, 12), pack(13, 13), pack(15, 11), pack(2, 12), pack(3, 13), pack(3, 3))
                column(COL_TREE, pack(11, 4), pack(12, 12), pack(12, 2), pack(2, 12), pack(3, 13), pack(8, 11), pack(15, 11))
                column(COL_POND, pack(12, 12), pack(2, 2), pack(8, 11), pack(11, 10), pack(11, 4), pack(13, 13), pack(13, 3), pack(3, 13), pack(5, 4))
                column(COL_HERB, pack(11, 4), pack(12, 2), pack(15, 11), pack(2, 12), pack(8, 11), pack(10, 11), pack(10, 5), pack(13, 3), pack(14, 12), pack(3, 13), pack(3, 3), pack(4, 11), pack(5, 10))
                column(COL_FIBRE, pack(2, 2), pack(5, 10), pack(5, 4), pack(10, 5), pack(11, 10), pack(13, 13), pack(13, 3), pack(4, 5), pack(8, 11), pack(9, 12))
            }

            row("dbrow.gauntlet_slots_middle_2") {
                column(COL_KIND, 0)
                column(COL_VARIANT, 2)
                column(COL_ROCK, pack(11, 10), pack(11, 4), pack(11, 7), pack(4, 5), pack(5, 10), pack(5, 4), pack(12, 1), pack(13, 0), pack(13, 13), pack(13, 3), pack(3, 13), pack(3, 3), pack(4, 11))
                column(COL_TREE, pack(11, 10), pack(4, 11), pack(11, 7), pack(12, 12), pack(12, 2), pack(2, 2), pack(3, 13))
                column(COL_POND, pack(10, 5), pack(5, 10), pack(10, 6), pack(11, 10), pack(11, 4), pack(12, 12), pack(2, 12), pack(2, 2), pack(5, 4))
                column(COL_HERB, pack(13, 0), pack(10, 11), pack(11, 4), pack(12, 1), pack(12, 2), pack(3, 13), pack(3, 3))
                column(COL_FIBRE, pack(10, 11), pack(12, 1), pack(3, 13), pack(10, 6), pack(11, 10), pack(12, 12), pack(12, 2), pack(13, 0), pack(2, 12), pack(2, 2), pack(5, 4))
            }

            row("dbrow.gauntlet_slots_middle_3") {
                column(COL_KIND, 0)
                column(COL_VARIANT, 3)
                column(COL_ROCK, pack(2, 12), pack(4, 12), pack(10, 11), pack(10, 6), pack(13, 11), pack(13, 2), pack(2, 11), pack(4, 11), pack(5, 13))
                column(COL_TREE, pack(10, 13), pack(12, 10), pack(12, 12), pack(3, 13), pack(4, 12), pack(9, 5), pack(10, 11), pack(11, 10), pack(13, 2), pack(14, 1), pack(2, 12), pack(3, 10), pack(5, 10))
                column(COL_POND, pack(11, 10), pack(11, 12), pack(12, 12), pack(14, 1), pack(2, 11), pack(2, 12), pack(3, 10), pack(4, 12))
                column(COL_HERB, pack(10, 6), pack(12, 12), pack(13, 11), pack(14, 1), pack(4, 11), pack(9, 5), pack(10, 13), pack(11, 10), pack(11, 12), pack(2, 11), pack(3, 10), pack(5, 10))
                column(COL_FIBRE, pack(10, 11), pack(10, 13), pack(13, 13), pack(13, 2), pack(2, 11), pack(5, 13), pack(11, 10), pack(11, 12), pack(12, 10), pack(14, 1), pack(4, 12), pack(5, 10), pack(9, 5))
            }

            row("dbrow.gauntlet_slots_edge_0") {
                column(COL_KIND, 1)
                column(COL_VARIANT, 0)
                column(COL_ROCK, pack(10, 5), pack(11, 13), pack(11, 2), pack(12, 11), pack(11, 10), pack(11, 5), pack(12, 2), pack(12, 6), pack(3, 3))
                column(COL_TREE, pack(12, 1), pack(4, 12), pack(4, 5), pack(5, 4), pack(3, 10))
                column(COL_POND, pack(12, 11), pack(12, 12), pack(4, 12), pack(5, 4))
                column(COL_HERB, pack(11, 13), pack(11, 4), pack(3, 3), pack(5, 4))
                column(COL_FIBRE, pack(12, 11), pack(3, 3), pack(11, 4), pack(12, 1), pack(13, 3), pack(4, 12))
            }

            row("dbrow.gauntlet_slots_edge_1") {
                column(COL_KIND, 1)
                column(COL_VARIANT, 1)
                column(COL_ROCK, pack(11, 1), pack(11, 4), pack(12, 7), pack(3, 13), pack(13, 12), pack(3, 15), pack(4, 11))
                column(COL_TREE, pack(11, 1), pack(3, 13), pack(4, 11), pack(10, 3), pack(11, 6), pack(12, 0), pack(13, 3), pack(2, 12), pack(4, 14), pack(5, 10))
                column(COL_POND, pack(12, 4), pack(12, 7), pack(13, 12), pack(11, 1))
                column(COL_HERB, pack(3, 15), pack(10, 12), pack(11, 1), pack(11, 4), pack(13, 12), pack(3, 8))
                column(COL_FIBRE, pack(10, 12), pack(10, 3), pack(11, 4), pack(11, 6), pack(12, 4), pack(12, 7), pack(3, 15), pack(4, 14), pack(9, 11))
            }

            row("dbrow.gauntlet_slots_edge_2") {
                column(COL_KIND, 1)
                column(COL_VARIANT, 2)
                column(COL_ROCK, pack(10, 11), pack(11, 12), pack(11, 4), pack(6, 10), pack(6, 8))
                column(COL_TREE, pack(10, 3), pack(11, 12), pack(11, 4), pack(5, 7), pack(6, 10))
                column(COL_POND, pack(13, 11), pack(2, 1), pack(2, 13), pack(5, 5), pack(5, 7), pack(5, 9), pack(6, 10), pack(6, 6), pack(6, 8))
                column(COL_HERB, pack(13, 3), pack(10, 11), pack(11, 4), pack(6, 10), pack(6, 6), pack(6, 8))
                column(COL_FIBRE, pack(2, 15), pack(13, 11), pack(13, 3), pack(2, 1), pack(2, 13), pack(5, 5), pack(5, 7), pack(5, 9), pack(6, 6))
            }

            row("dbrow.gauntlet_slots_edge_3") {
                column(COL_KIND, 1)
                column(COL_VARIANT, 3)
                column(COL_ROCK, pack(11, 9), pack(13, 13), pack(3, 13), pack(5, 9), pack(8, 4))
                column(COL_TREE, pack(13, 13), pack(5, 9), pack(8, 4))
                column(COL_POND, pack(11, 9), pack(13, 13), pack(3, 13), pack(8, 4))
                column(COL_HERB, pack(11, 10), pack(11, 9), pack(13, 14), pack(3, 13), pack(5, 9))
                column(COL_FIBRE, pack(0, 3), pack(10, 11), pack(11, 10), pack(11, 9), pack(12, 15), pack(13, 13), pack(13, 14), pack(3, 14), pack(4, 11), pack(5, 10), pack(8, 4))
            }

            row("dbrow.gauntlet_slots_corner_0") {
                column(COL_KIND, 2)
                column(COL_VARIANT, 0)
                column(COL_ROCK, pack(13, 3), pack(3, 13), pack(4, 0), pack(4, 7))
                column(COL_TREE, pack(4, 4))
                column(COL_FIBRE, pack(13, 5), pack(3, 1), pack(4, 11), pack(10, 5), pack(11, 4), pack(4, 0), pack(5, 10))
            }

            row("dbrow.gauntlet_slots_corner_1") {
                column(COL_KIND, 2)
                column(COL_VARIANT, 1)
                column(COL_FIBRE, pack(1, 3), pack(11, 4), pack(12, 13), pack(13, 3), pack(5, 1), pack(7, 4))
            }

            row("dbrow.gauntlet_slots_corner_2") {
                column(COL_KIND, 2)
                column(COL_VARIANT, 2)
                column(COL_ROCK, pack(4, 11))
                column(COL_HERB, pack(2, 5))
            }

            row("dbrow.gauntlet_slots_corner_3") {
                column(COL_KIND, 2)
                column(COL_VARIANT, 3)
                column(COL_POND, pack(0, 4), pack(11, 14), pack(4, 5))
            }
        }
}
