package org.rsmod.content.skills.hunter.pack

import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import org.rsmod.map.CoordGrid

object HunterTrackingTable {
    const val NAME = 0
    const val LEVEL = 1
    const val XP_TENTHS = 2
    const val LOOT = 3
    const val CATCH_SEQ = 4
    const val FLEE_SPOTANIM = 5
    const val SPOT_NOUN = 6
    const val SPOT_WHERE = 7
    const val USED_VARBIT = 8
    const val ENDS_VARBIT = 9
    const val NMIN = 10
    const val DENS = 11
    const val SPOTS = 12
    const val TUNNELS = 13
    const val SEGMENTS = 14
    const val SPOT_LOC = 15
    const val NOOSE_FAIL_PERMILLE = 16

    private fun node(id: Int): Int = id

    private fun loc(name: String): Int = ConstantProvider.getMapping(name)

    private fun rows(vararg rows: List<Any>): List<Any> = rows.toList().flatten()

    fun tracking() =
        dbTable("dbtable.hunter_tracking", serverOnly = true) {
            column("name", NAME, VarType.STRING)
            column("level", LEVEL, VarType.INT)
            column("xp_tenths", XP_TENTHS, VarType.INT)
            column("loot", LOOT, VarType.OBJ)
            column("catch_seq", CATCH_SEQ, VarType.SEQ)
            column("flee_spotanim", FLEE_SPOTANIM, VarType.STRING)
            column("spot_noun", SPOT_NOUN, VarType.STRING)
            column("spot_where", SPOT_WHERE, VarType.STRING)
            column("used_varbit", USED_VARBIT, VarType.STRING)
            column("ends_varbit", ENDS_VARBIT, VarType.STRING)
            column("nmin", NMIN, VarType.INT)
            column("dens", DENS, VarType.INT, VarType.LOC)
            column("spots", SPOTS, VarType.INT, VarType.COORDGRID, VarType.INT)
            column("tunnels", TUNNELS, VarType.INT, VarType.INT)
            column("segments", SEGMENTS, VarType.STRING, VarType.INT, VarType.INT, VarType.BOOLEAN, VarType.LOC)
            column("spot_loc", SPOT_LOC, VarType.LOC)
            column("noose_fail_permille", NOOSE_FAIL_PERMILLE, VarType.INT)

            row("dbrow.hunter_tracking_polar") {
                column(NAME, "polar kebbit")
                column(LEVEL, 1)
                column(XP_TENTHS, 300)
                column(LOOT, listOf("obj.bones", "obj.spit_raw_beast_meat", "obj.huntingbeast_polar_fur").map { loc(it) })
                columnRSCM(CATCH_SEQ, "seq.hunting_noose_polar")
                column(FLEE_SPOTANIM, "spotanim.hunting_beastproj_polar")
                column(SPOT_NOUN, "snowdrift")
                column(SPOT_WHERE, "under this snowdrift")
                column(USED_VARBIT, "varbit.hunting_trails_used_polar")
                column(ENDS_VARBIT, "varbit.hunting_trail_ends_polar")
                column(NMIN, 3)
                column(NOOSE_FAIL_PERMILLE, 70)
                columnRSCM(SPOT_LOC, "loc.hunting_trail_end_polar")
                column(DENS, rows(
                        listOf(node(1), loc("loc.hunting_trail_spawn_polar1")),
                        listOf(node(3), loc("loc.hunting_trail_spawn_polar2")),
                ))
                column(SPOTS, rows(
                        listOf(node(0), CoordGrid(2708, 3819, 1).packed, 1),
                        listOf(node(1), CoordGrid(2712, 3831, 1).packed, 2),
                        listOf(node(2), CoordGrid(2716, 3827, 1).packed, 3),
                        listOf(node(3), CoordGrid(2718, 3820, 1).packed, 4),
                ))
                column(TUNNELS, rows(
                        listOf(node(4), node(5)),
                        listOf(node(5), node(4)),
                        listOf(node(6), node(7)),
                        listOf(node(7), node(6)),
                        listOf(node(8), node(9)),
                        listOf(node(9), node(8)),
                ))
                column(SEGMENTS, rows(
                        listOf("varbit.hunting_trail_state8_0", 0, 3, false, loc("loc.hunting_trail_clue8_0")),
                        listOf("varbit.hunting_trail_state8_1", 0, 4, false, loc("loc.hunting_trail_clue8_1")),
                        listOf("varbit.hunting_trail_state8_2", 5, 3, true, loc("loc.hunting_trail_clue8_2")),
                        listOf("varbit.hunting_trail_state8_3", 3, 7, false, loc("loc.hunting_trail_clue8_3")),
                        listOf("varbit.hunting_trail_state8_4", 0, 1, true, loc("loc.hunting_trail_clue8_4")),
                        listOf("varbit.hunting_trail_state8_5", 8, 2, true, loc("loc.hunting_trail_clue8_5")),
                        listOf("varbit.hunting_trail_state8_6", 2, 6, true, loc("loc.hunting_trail_clue8_6")),
                        listOf("varbit.hunting_trail_state8_7", 1, 2, true, loc("loc.hunting_trail_clue8_7")),
                        listOf("varbit.hunting_trail_state8_8", 1, 9, false, loc("loc.hunting_trail_clue8_8")),
                ))
            }

            row("dbrow.hunter_tracking_common") {
                column(NAME, "common kebbit")
                column(LEVEL, 3)
                column(XP_TENTHS, 360)
                column(LOOT, listOf("obj.bones", "obj.spit_raw_beast_meat", "obj.huntingbeast_woodland_fur").map { loc(it) })
                columnRSCM(CATCH_SEQ, "seq.hunting_noose_wood")
                column(FLEE_SPOTANIM, "spotanim.hunting_beastproj_wood")
                column(SPOT_NOUN, "bush")
                column(SPOT_WHERE, "in this bush")
                column(USED_VARBIT, "varbit.hunting_trails_used35_55")
                column(ENDS_VARBIT, "varbit.hunting_trail_ends_wood2")
                column(NMIN, 5)
                column(NOOSE_FAIL_PERMILLE, 0)
                columnRSCM(SPOT_LOC, "loc.hunting_trail_end_bush35_55")
                column(DENS, rows(
                        listOf(node(8), loc("loc.hunting_trail_spawn4")),
                        listOf(node(9), loc("loc.hunting_trail_spawn5")),
                        listOf(node(10), loc("loc.hunting_trail_spawn6")),
                ))
                column(SPOTS, rows(
                        listOf(node(0), CoordGrid(2322, 3570, 0).packed, 1),
                        listOf(node(1), CoordGrid(2323, 3563, 0).packed, 2),
                        listOf(node(2), CoordGrid(2327, 3573, 0).packed, 3),
                        listOf(node(3), CoordGrid(2332, 3568, 0).packed, 4),
                        listOf(node(4), CoordGrid(2332, 3578, 0).packed, 5),
                        listOf(node(5), CoordGrid(2336, 3571, 0).packed, 6),
                        listOf(node(6), CoordGrid(2337, 3565, 0).packed, 7),
                        listOf(node(7), CoordGrid(2343, 3568, 0).packed, 8),
                ))
                column(TUNNELS, rows(
                        listOf(node(-1), node(-1)),
                ))
                column(SEGMENTS, rows(
                        listOf("varbit.hunting_trail_state3_0", 1, 8, false, loc("loc.hunting_trail_clue3_0")),
                        listOf("varbit.hunting_trail_state3_1", 8, 3, true, loc("loc.hunting_trail_clue3_1")),
                        listOf("varbit.hunting_trail_state3_2", 8, 6, true, loc("loc.hunting_trail_clue3_2")),
                        listOf("varbit.hunting_trail_state3_3", 6, 7, false, loc("loc.hunting_trail_clue3_3")),
                        listOf("varbit.hunting_trail_state3_4", 0, 1, false, loc("loc.hunting_trail_clue3_4")),
                        listOf("varbit.hunting_trail_state3_5", 1, 3, false, loc("loc.hunting_trail_clue3_5")),
                        listOf("varbit.hunting_trail_state3_6", 6, 5, true, loc("loc.hunting_trail_clue3_6")),
                        listOf("varbit.hunting_trail_state3_7", 5, 7, false, loc("loc.hunting_trail_clue3_7")),
                        listOf("varbit.hunting_trail_state3_8", 10, 7, true, loc("loc.hunting_trail_clue3_8")),
                        listOf("varbit.hunting_trail_state3_9", 0, 9, false, loc("loc.hunting_trail_clue3_9")),
                        listOf("varbit.hunting_trail_state4_0", 0, 2, true, loc("loc.hunting_trail_clue4_0")),
                        listOf("varbit.hunting_trail_state4_1", 2, 3, true, loc("loc.hunting_trail_clue4_1")),
                        listOf("varbit.hunting_trail_state4_2", 9, 2, true, loc("loc.hunting_trail_clue4_2")),
                        listOf("varbit.hunting_trail_state4_3", 9, 4, true, loc("loc.hunting_trail_clue4_3")),
                        listOf("varbit.hunting_trail_state4_4", 4, 5, true, loc("loc.hunting_trail_clue4_4")),
                        listOf("varbit.hunting_trail_state4_5", 5, 10, false, loc("loc.hunting_trail_clue4_5")),
                        listOf("varbit.hunting_trail_state4_6", 4, 10, true, loc("loc.hunting_trail_clue4_6")),
                ))
            }

            row("dbrow.hunter_tracking_razorback") {
                column(NAME, "razor-backed kebbit")
                column(LEVEL, 49)
                column(XP_TENTHS, 3485)
                column(LOOT, listOf("obj.bones", "obj.spit_raw_beast_meat", "obj.huntingbeast_bigspike").map { loc(it) })
                columnRSCM(CATCH_SEQ, "seq.hunting_noose_razorback")
                column(FLEE_SPOTANIM, "spotanim.hunting_beastproj_razor2")
                column(SPOT_NOUN, "bush")
                column(SPOT_WHERE, "in this bush")
                column(USED_VARBIT, "varbit.hunting_trails_used")
                column(ENDS_VARBIT, "varbit.hunting_trail_state2_9")
                column(NMIN, 5)
                column(NOOSE_FAIL_PERMILLE, 0)
                columnRSCM(SPOT_LOC, "loc.hunting_trail_end_bush29")
                column(DENS, rows(
                        listOf(node(8), loc("loc.hunting_trail_spawn1")),
                        listOf(node(9), loc("loc.hunting_trail_spawn2")),
                        listOf(node(10), loc("loc.hunting_trail_spawn3")),
                ))
                column(SPOTS, rows(
                        listOf(node(0), CoordGrid(2349, 3604, 0).packed, 1),
                        listOf(node(1), CoordGrid(2351, 3619, 0).packed, 2),
                        listOf(node(2), CoordGrid(2354, 3609, 0).packed, 3),
                        listOf(node(3), CoordGrid(2355, 3601, 0).packed, 4),
                        listOf(node(4), CoordGrid(2357, 3607, 0).packed, 5),
                        listOf(node(5), CoordGrid(2358, 3620, 0).packed, 6),
                        listOf(node(6), CoordGrid(2360, 3602, 0).packed, 7),
                        listOf(node(7), CoordGrid(2362, 3615, 0).packed, 8),
                ))
                column(TUNNELS, rows(
                        listOf(node(-1), node(-1)),
                ))
                column(SEGMENTS, rows(
                        listOf("varbit.hunting_trail_state1_0", 9, 6, false, loc("loc.hunting_trail_clue1_0")),
                        listOf("varbit.hunting_trail_state1_1", 9, 3, false, loc("loc.hunting_trail_clue1_1")),
                        listOf("varbit.hunting_trail_state1_2", 0, 9, true, loc("loc.hunting_trail_clue1_2")),
                        listOf("varbit.hunting_trail_state1_3", 3, 6, true, loc("loc.hunting_trail_clue1_3")),
                        listOf("varbit.hunting_trail_state1_4", 0, 3, true, loc("loc.hunting_trail_clue1_4")),
                        listOf("varbit.hunting_trail_state1_5", 4, 6, true, loc("loc.hunting_trail_clue1_5")),
                        listOf("varbit.hunting_trail_state1_6", 6, 10, false, loc("loc.hunting_trail_clue1_6")),
                        listOf("varbit.hunting_trail_state1_7", 4, 10, false, loc("loc.hunting_trail_clue1_7")),
                        listOf("varbit.hunting_trail_state1_8", 2, 4, true, loc("loc.hunting_trail_clue1_8")),
                        listOf("varbit.hunting_trail_state1_9", 0, 2, true, loc("loc.hunting_trail_clue1_9")),
                        listOf("varbit.hunting_trail_state2_0", 8, 7, false, loc("loc.hunting_trail_clue2_0")),
                        listOf("varbit.hunting_trail_state2_1", 1, 8, false, loc("loc.hunting_trail_clue2_1")),
                        listOf("varbit.hunting_trail_state2_2", 8, 5, true, loc("loc.hunting_trail_clue2_2")),
                        listOf("varbit.hunting_trail_state2_3", 11, 0, false, loc("loc.hunting_trail_clue2_3")),
                        listOf("varbit.hunting_trail_state2_4", 11, 2, true, loc("loc.hunting_trail_clue2_4")),
                        listOf("varbit.hunting_trail_state2_5", 11, 1, true, loc("loc.hunting_trail_clue2_5")),
                        listOf("varbit.hunting_trail_state2_6", 1, 5, true, loc("loc.hunting_trail_clue2_6")),
                        listOf("varbit.hunting_trail_state2_7", 10, 7, true, loc("loc.hunting_trail_clue2_7")),
                        listOf("varbit.hunting_trail_state2_8", 5, 7, false, loc("loc.hunting_trail_clue2_8")),
                ))
            }

            row("dbrow.hunter_tracking_feldip") {
                column(NAME, "feldip weasel")
                column(LEVEL, 7)
                column(XP_TENTHS, 480)
                column(LOOT, listOf("obj.bones", "obj.spit_raw_beast_meat", "obj.huntingbeast_jungle_fur").map { loc(it) })
                columnRSCM(CATCH_SEQ, "seq.hunting_noose_jungle")
                column(FLEE_SPOTANIM, "spotanim.hunting_beastproj_jungle")
                column(SPOT_NOUN, "bush")
                column(SPOT_WHERE, "in this bush")
                column(USED_VARBIT, "varbit.hunting_trails_used_jungle")
                column(ENDS_VARBIT, "varbit.hunting_trail_ends_jungle")
                column(NMIN, 5)
                column(NOOSE_FAIL_PERMILLE, 0)
                columnRSCM(SPOT_LOC, "loc.hunting_trail_end_jungle")
                column(DENS, rows(
                        listOf(node(6), loc("loc.hunting_trail_spawn_jungle2")),
                        listOf(node(7), loc("loc.hunting_trail_spawn_jungle1")),
                ))
                column(SPOTS, rows(
                        listOf(node(0), CoordGrid(2525, 2882, 0).packed, 1),
                        listOf(node(1), CoordGrid(2531, 2890, 0).packed, 2),
                        listOf(node(2), CoordGrid(2533, 2885, 0).packed, 3),
                        listOf(node(3), CoordGrid(2540, 2886, 0).packed, 4),
                        listOf(node(4), CoordGrid(2542, 2881, 0).packed, 5),
                        listOf(node(5), CoordGrid(2553, 2888, 0).packed, 6),
                ))
                column(TUNNELS, rows(
                        listOf(node(-1), node(-1)),
                ))
                column(SEGMENTS, rows(
                        listOf("varbit.hunting_trail_state6_3", 0, 6, true, loc("loc.hunting_trail_clue6_3")),
                        listOf("varbit.hunting_trail_state6_4", 0, 6, false, loc("loc.hunting_trail_clue6_4")),
                        listOf("varbit.hunting_trail_state6_5", 6, 1, true, loc("loc.hunting_trail_clue6_5")),
                        listOf("varbit.hunting_trail_state6_6", 1, 2, true, loc("loc.hunting_trail_clue6_6")),
                        listOf("varbit.hunting_trail_state6_7", 0, 8, false, loc("loc.hunting_trail_clue6_7")),
                        listOf("varbit.hunting_trail_state6_8", 2, 8, false, loc("loc.hunting_trail_clue6_8")),
                        listOf("varbit.hunting_trail_state7_0", 9, 1, false, loc("loc.hunting_trail_clue7_0")),
                        listOf("varbit.hunting_trail_state7_1", 2, 3, true, loc("loc.hunting_trail_clue7_1")),
                        listOf("varbit.hunting_trail_state7_2", 8, 4, false, loc("loc.hunting_trail_clue7_2")),
                        listOf("varbit.hunting_trail_state7_3", 3, 4, false, loc("loc.hunting_trail_clue7_3")),
                        listOf("varbit.hunting_trail_state7_4", 3, 9, true, loc("loc.hunting_trail_clue7_4")),
                        listOf("varbit.hunting_trail_state7_5", 9, 5, true, loc("loc.hunting_trail_clue7_5")),
                        listOf("varbit.hunting_trail_state7_6", 4, 7, false, loc("loc.hunting_trail_clue7_6")),
                        listOf("varbit.hunting_trail_state7_7", 5, 7, true, loc("loc.hunting_trail_clue7_7")),
                        listOf("varbit.hunting_trail_state7_8", 7, 5, false, loc("loc.hunting_trail_clue7_8")),
                ))
            }

            row("dbrow.hunter_tracking_desert") {
                column(NAME, "desert devil")
                column(LEVEL, 13)
                column(XP_TENTHS, 660)
                column(LOOT, listOf("obj.bones", "obj.spit_raw_beast_meat", "obj.huntingbeast_desert_fur").map { loc(it) })
                columnRSCM(CATCH_SEQ, "seq.hunting_noose_desert")
                column(FLEE_SPOTANIM, "spotanim.hunting_beastproj_desert")
                column(SPOT_NOUN, "disturbed sand")
                column(SPOT_WHERE, "under this sand")
                column(USED_VARBIT, "varbit.hunting_trails_used_desert")
                column(ENDS_VARBIT, "varbit.hunting_trail_ends_desert")
                column(NMIN, 5)
                column(NOOSE_FAIL_PERMILLE, 0)
                columnRSCM(SPOT_LOC, "loc.hunting_trail_end_desert")
                column(DENS, rows(
                        listOf(node(6), loc("loc.hunting_trail_spawn_desert2")),
                        listOf(node(7), loc("loc.hunting_trail_spawn_desert1")),
                ))
                column(SPOTS, rows(
                        listOf(node(0), CoordGrid(3393, 3122, 0).packed, 1),
                        listOf(node(1), CoordGrid(3400, 3114, 0).packed, 2),
                        listOf(node(2), CoordGrid(3405, 3124, 0).packed, 3),
                        listOf(node(3), CoordGrid(3407, 3121, 0).packed, 4),
                        listOf(node(4), CoordGrid(3411, 3108, 0).packed, 5),
                        listOf(node(5), CoordGrid(3414, 3121, 0).packed, 6),
                ))
                column(TUNNELS, rows(
                        listOf(node(-1), node(-1)),
                ))
                column(SEGMENTS, rows(
                        listOf("varbit.hunting_trail_state4_7", 0, 6, true, loc("loc.hunting_trail_clue4_7")),
                        listOf("varbit.hunting_trail_state4_8", 6, 2, false, loc("loc.hunting_trail_clue4_8")),
                        listOf("varbit.hunting_trail_state5_0", 6, 5, true, loc("loc.hunting_trail_clue5_0")),
                        listOf("varbit.hunting_trail_state5_1", 0, 8, false, loc("loc.hunting_trail_clue5_1")),
                        listOf("varbit.hunting_trail_state5_2", 8, 2, false, loc("loc.hunting_trail_clue5_2")),
                        listOf("varbit.hunting_trail_state5_3", 2, 3, false, loc("loc.hunting_trail_clue5_3")),
                        listOf("varbit.hunting_trail_state5_4", 3, 5, true, loc("loc.hunting_trail_clue5_4")),
                        listOf("varbit.hunting_trail_state5_5", 0, 7, false, loc("loc.hunting_trail_clue5_5")),
                        listOf("varbit.hunting_trail_state5_6", 8, 1, true, loc("loc.hunting_trail_clue5_6")),
                        listOf("varbit.hunting_trail_state5_7", 9, 3, true, loc("loc.hunting_trail_clue5_7")),
                        listOf("varbit.hunting_trail_state5_8", 4, 5, false, loc("loc.hunting_trail_clue5_8")),
                        listOf("varbit.hunting_trail_state5_9", 7, 1, false, loc("loc.hunting_trail_clue5_9")),
                        listOf("varbit.hunting_trail_state6_0", 1, 9, true, loc("loc.hunting_trail_clue6_0")),
                        listOf("varbit.hunting_trail_state6_1", 9, 4, false, loc("loc.hunting_trail_clue6_1")),
                        listOf("varbit.hunting_trail_state6_2", 7, 4, false, loc("loc.hunting_trail_clue6_2")),
                ))
            }
        }
}
