package org.rsmod.content.skills.hunter.pack

import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import org.rsmod.map.CoordGrid

object ImplingSpawnTable {
    const val IMPLINGS = 0
    const val SPOTS = 0

    private fun weights(vararg entries: Pair<Int, Int>) =
        entries.flatMap { (type, weight) -> listOf(ConstantProvider.getMapping("npc.ii_impling_type_$type"), weight) }

    private val GIELINOR =
        listOf(
            CoordGrid(1254, 3559, 0),
            CoordGrid(1275, 3749, 0),
            CoordGrid(1705, 3479, 0),
            CoordGrid(2203, 3236, 0),
            CoordGrid(2331, 3639, 0),
            CoordGrid(2394, 3512, 0),
            CoordGrid(2459, 3418, 0),
            CoordGrid(2584, 2972, 0),
            CoordGrid(2592, 3252, 0),
            CoordGrid(2739, 3342, 0),
            CoordGrid(2841, 2928, 0),
            CoordGrid(2849, 3033, 0),
            CoordGrid(3018, 3523, 0),
            CoordGrid(3020, 3424, 0),
            CoordGrid(3143, 3231, 0),
            CoordGrid(3239, 3283, 0),
            CoordGrid(3292, 3265, 0),
            CoordGrid(3356, 3013, 0),
            CoordGrid(3408, 3125, 0),
            CoordGrid(3441, 3351, 0),
            CoordGrid(3454, 3486, 0),
            CoordGrid(3548, 3528, 0),
            CoordGrid(3677, 3321, 0),
            CoordGrid(2982, 3276, 0),
            CoordGrid(3169, 3001, 0),
            CoordGrid(2904, 3489, 0),
            CoordGrid(2646, 3421, 0),
            CoordGrid(2654, 3609, 0),
            CoordGrid(1616, 3701, 0),
            CoordGrid(3135, 3378, 0),
            CoordGrid(2727, 3768, 0),
            CoordGrid(2814, 3512, 0),
            CoordGrid(2470, 3217, 0),
            CoordGrid(2740, 3535, 0),
            CoordGrid(1600, 3837, 0),
            CoordGrid(2843, 3161, 0),
            CoordGrid(1525, 3520, 0),
            CoordGrid(3730, 3016, 0),
            CoordGrid(3765, 3798, 0),
            CoordGrid(2198, 2963, 0),
            CoordGrid(2526, 3101, 0),
            CoordGrid(2500, 2875, 0),
            CoordGrid(1328, 2971, 0),
            CoordGrid(1453, 3130, 0),
            CoordGrid(1721, 3001, 0),
            CoordGrid(1617, 3182, 0),
            CoordGrid(1288, 3168, 0),
            CoordGrid(1455, 3319, 0),
            CoordGrid(3160, 2488, 0),
        )

    private val GIELINOR_LOW =
        listOf(
            CoordGrid(1705, 3483, 0),
            CoordGrid(2354, 3608, 0),
            CoordGrid(2279, 3188, 0),
            CoordGrid(2455, 3084, 0),
            CoordGrid(2567, 3384, 0),
            CoordGrid(2784, 3463, 0),
            CoordGrid(2967, 3411, 0),
            CoordGrid(3093, 3238, 0),
            CoordGrid(3283, 3428, 0),
            CoordGrid(3278, 3155, 0),
            CoordGrid(2481, 4442, 0),
        )

    private val PURO_PURO_HIGH =
        listOf(
            CoordGrid(2576, 4336, 0),
            CoordGrid(2610, 4301, 0),
        )

    private val PURO_PURO_MID =
        listOf(
            CoordGrid(2569, 4342, 0),
            CoordGrid(2591, 4339, 0),
            CoordGrid(2616, 4343, 0),
            CoordGrid(2578, 4334, 0),
            CoordGrid(2605, 4334, 0),
            CoordGrid(2572, 4321, 0),
            CoordGrid(2610, 4320, 0),
            CoordGrid(2575, 4306, 0),
            CoordGrid(2591, 4301, 0),
            CoordGrid(2605, 4306, 0),
            CoordGrid(2570, 4297, 0),
            CoordGrid(2617, 4295, 0),
        )

    private val PURO_PURO_LOW =
        listOf(
            CoordGrid(2570, 4332, 0),
            CoordGrid(2585, 4340, 0),
            CoordGrid(2603, 4340, 0),
            CoordGrid(2615, 4333, 0),
            CoordGrid(2574, 4322, 0),
            CoordGrid(2609, 4322, 0),
            CoordGrid(2578, 4299, 0),
            CoordGrid(2589, 4299, 0),
            CoordGrid(2598, 4302, 0),
            CoordGrid(2614, 4308, 0),
        )

    fun spawnTables() =
        dbTable("dbtable.impling_spawn_tables", serverOnly = true) {
            column("implings", IMPLINGS, VarType.NPC, VarType.INT)

            row("dbrow.impling_spawn_low") {
                column(IMPLINGS, weights(1 to 20, 2 to 20, 3 to 20, 4 to 20, 5 to 10, 6 to 10))
            }
            row("dbrow.impling_spawn_mid") {
                column(IMPLINGS, weights(3 to 10, 4 to 10, 5 to 20, 6 to 37, 7 to 20, 8 to 2, 9 to 1))
            }
            row("dbrow.impling_spawn_high") {
                column(IMPLINGS, weights(7 to 10, 8 to 50, 9 to 30, 10 to 10, 11 to 1))
            }
            row("dbrow.impling_spawn_high_puro_puro") {
                column(IMPLINGS, weights(7 to 150, 8 to 114, 9 to 27, 10 to 9, 11 to 1))
            }
        }

    fun spawnPoints() =
        dbTable("dbtable.impling_spawn_points", serverOnly = true) {
            column("spots", SPOTS, VarType.COORDGRID)

            row("dbrow.impling_points_gielinor") { column(SPOTS, GIELINOR.map { it.packed }) }
            row("dbrow.impling_points_gielinor_low") { column(SPOTS, GIELINOR_LOW.map { it.packed }) }
            row("dbrow.impling_points_puro_puro_high") { column(SPOTS, PURO_PURO_HIGH.map { it.packed }) }
            row("dbrow.impling_points_puro_puro_mid") { column(SPOTS, PURO_PURO_MID.map { it.packed }) }
            row("dbrow.impling_points_puro_puro_low") { column(SPOTS, PURO_PURO_LOW.map { it.packed }) }
        }
}
