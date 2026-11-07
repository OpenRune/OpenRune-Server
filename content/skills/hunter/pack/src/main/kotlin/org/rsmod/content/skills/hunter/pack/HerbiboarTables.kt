package org.rsmod.content.skills.hunter.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import dev.openrune.pack.columnCoord
import org.rsmod.map.CoordGrid

object HerbiboarLegsTable {
    const val FROM_LOC = 0
    const val TRAIL = 1
    const val DIR = 2
    const val TO_GROUP = 3
    const val END_TUNNEL = 4

    const val NO_GROUP = -1

    private data class Leg(val from: String, val trail: String, val dir: Int, val to: Char?, val endTunnel: Int)

    private val GROUP_TUNNELS = mapOf('C' to 5, 'D' to 4, 'F' to 9, 'J' to 6, 'K' to 2)

    private fun leg(from: String, trail: String, dir: Int, to: Char) = Leg(from, trail, dir, to, GROUP_TUNNELS[to] ?: 0)

    private fun last(from: String, trail: String, dir: Int, tunnel: Int) = Leg(from, trail, dir, null, tunnel)

    private val LEGS =
        listOf(
            leg("a_mushroom", "9_5", 1, 'B'),
            leg("a_mushroom", "9_6", 1, 'E'),
            last("a_patch", "9_1", 2, 8),
            leg("b_seaweed", "9_4", 2, 'C'),
            leg("b_seaweed", "9_5", 2, 'A'),
            last("b_seaweed", "10_1", 1, 5),
            leg("b_seaweed", "10_2", 1, 'D'),
            last("c_mushroom", "9_0", 2, 8),
            leg("c_patch", "9_3", 1, 'D'),
            leg("c_patch", "9_4", 1, 'B'),
            leg("d_patch", "9_9", 1, 'H'),
            leg("d_patch", "10_0", 1, 'G'),
            leg("d_seaweed", "9_3", 2, 'C'),
            leg("d_seaweed", "10_2", 2, 'B'),
            leg("e_mushroom", "10_3", 1, 'F'),
            leg("e_mushroom", "10_4", 1, 'I'),
            leg("e_patch", "9_6", 2, 'A'),
            leg("f_mushroom", "9_7", 1, 'I'),
            leg("f_mushroom", "9_8", 1, 'G'),
            leg("f_mushroom", "10_3", 2, 'E'),
            last("f_patch", "9_2", 2, 8),
            leg("g_mushroom", "10_0", 2, 'D'),
            last("g_mushroom", "10_7", 1, 3),
            leg("g_patch", "9_8", 2, 'F'),
            leg("h_seaweed_east", "10_8", 1, 'J'),
            last("h_seaweed_east", "10_9", 1, 7),
            leg("h_seaweed_west", "9_9", 2, 'D'),
            leg("h_seaweed_west", "11_0", 1, 'G'),
            leg("i_mushroom", "10_5", 1, 'K'),
            last("i_mushroom", "10_6", 1, 1),
            leg("i_patch", "9_7", 2, 'F'),
            leg("i_patch", "10_4", 2, 'E'),
            leg("j_patch", "10_8", 2, 'H'),
            leg("j_patch", "11_3", 1, 'D'),
            leg("k_patch", "10_5", 2, 'I'),
            last("k_patch", "11_1", 1, 1),
            leg("k_patch", "11_2", 1, 'J'),
            leg("start_middle", "9_0", 1, 'C'),
            leg("start_middle", "9_1", 1, 'A'),
            leg("start_middle", "9_2", 1, 'F'),
            leg("start_ghost_mushroom", "11_1", 2, 'K'),
            leg("start_ghost_mushroom", "10_6", 2, 'I'),
            leg("start_camp_entrance", "10_5", 2, 'I'),
            leg("start_camp_entrance", "11_2", 1, 'J'),
            leg("start_leprechaun", "10_7", 2, 'G'),
            leg("start_driftwood", "10_9", 2, 'H'),
        )

    private val FROM_LOCS =
        HerbiboarSpotsTable.SPOTS.associate { it.key to it.loc } +
            mapOf(
                "start_middle" to "loc.hunting_trail_spawn_fossil1",
                "start_ghost_mushroom" to "loc.hunting_trail_spawn_fossil2",
                "start_camp_entrance" to "loc.hunting_trail_spawn_fossil3",
                "start_leprechaun" to "loc.hunting_trail_spawn_fossil4",
                "start_driftwood" to "loc.hunting_trail_spawn_fossil5",
            )

    fun herbiboarLegs() =
        dbTable("dbtable.herbiboar_legs", serverOnly = true) {
            column("from_loc", FROM_LOC, VarType.LOC)
            column("trail", TRAIL, VarType.INT)
            column("dir", DIR, VarType.INT)
            column("to_group", TO_GROUP, VarType.INT)
            column("end_tunnel", END_TUNNEL, VarType.INT)

            LEGS.forEachIndexed { index, leg ->
                row("dbrow.herbiboar_leg_$index") {
                    columnRSCM(FROM_LOC, checkNotNull(FROM_LOCS[leg.from]) { leg.from })
                    columnRSCM(TRAIL, "varbit.hunting_trail_state${leg.trail}")
                    column(DIR, leg.dir)
                    column(TO_GROUP, leg.to?.let { it - 'A' } ?: NO_GROUP)
                    column(END_TUNNEL, leg.endTunnel)
                }
            }
        }
}

object HerbiboarSpotsTable {
    const val LOC = 0
    const val GROUP = 1

    data class Spot(val key: String, val loc: String, val group: Char)

    val SPOTS =
        listOf(
            Spot("a_mushroom", "loc.hunting_trail_clue9_1", 'A'),
            Spot("a_patch", "loc.hunting_trail_clue10_5", 'A'),
            Spot("b_seaweed", "loc.hunting_trail_clue9_4", 'B'),
            Spot("c_mushroom", "loc.hunting_trail_clue10_4", 'C'),
            Spot("c_patch", "loc.hunting_trail_clue9_0", 'C'),
            Spot("d_patch", "loc.hunting_trail_clue9_3", 'D'),
            Spot("d_seaweed", "loc.hunting_trail_clue11_0", 'D'),
            Spot("e_mushroom", "loc.hunting_trail_clue9_6", 'E'),
            Spot("e_patch", "loc.hunting_trail_clue10_8", 'E'),
            Spot("f_mushroom", "loc.hunting_trail_clue9_2", 'F'),
            Spot("f_patch", "loc.hunting_trail_clue10_1", 'F'),
            Spot("g_mushroom", "loc.hunting_trail_clue9_8", 'G'),
            Spot("g_patch", "loc.hunting_trail_clue10_0", 'G'),
            Spot("h_seaweed_east", "loc.hunting_trail_clue9_9", 'H'),
            Spot("h_seaweed_west", "loc.hunting_trail_clue10_9", 'H'),
            Spot("i_mushroom", "loc.hunting_trail_clue9_7", 'I'),
            Spot("i_patch", "loc.hunting_trail_clue10_6", 'I'),
            Spot("j_patch", "loc.hunting_trail_clue10_3", 'J'),
            Spot("k_patch", "loc.hunting_trail_clue10_2", 'K'),
        )

    fun herbiboarSpots() =
        dbTable("dbtable.herbiboar_spots", serverOnly = true) {
            column("loc", LOC, VarType.LOC)
            column("group", GROUP, VarType.INT)

            for (spot in SPOTS) {
                row("dbrow.herbiboar_spot_${spot.key}") {
                    columnRSCM(LOC, spot.loc)
                    column(GROUP, spot.group - 'A')
                }
            }
        }
}

object HerbiboarTunnelsTable {
    const val INDEX = 0
    const val COORDS = 1

    private val TUNNELS =
        listOf(
            CoordGrid(3693, 3798, 0),
            CoordGrid(3702, 3808, 0),
            CoordGrid(3703, 3826, 0),
            CoordGrid(3710, 3881, 0),
            CoordGrid(3700, 3877, 0),
            CoordGrid(3715, 3840, 0),
            CoordGrid(3751, 3849, 0),
            CoordGrid(3685, 3869, 0),
            CoordGrid(3681, 3863, 0),
        )

    fun herbiboarTunnels() =
        dbTable("dbtable.herbiboar_tunnels", serverOnly = true) {
            column("index", INDEX, VarType.INT)
            column("coords", COORDS, VarType.COORDGRID)

            TUNNELS.forEachIndexed { i, coords ->
                row("dbrow.herbiboar_tunnel_${i + 1}") {
                    column(INDEX, i + 1)
                    columnCoord(COORDS, coords)
                }
            }
        }
}

object HerbiboarHerbsTable {
    const val HERB = 0
    const val LOW = 1
    const val HIGH = 2

    private val HERBS =
        listOf(
            Triple("ranarr", -10, 20),
            Triple("torstol", -70, 20),
            Triple("snapdragon", -60, 20),
            Triple("dwarf_weed", -50, 30),
            Triple("lantadyme", -30, 40),
            Triple("cadantine", -10, 50),
            Triple("kwuarm", 10, 60),
            Triple("avantoe", 20, 60),
            Triple("irit", 30, 70),
            Triple("tarromin", 70, -20),
            Triple("harralander", 100, -30),
            Triple("marentill", 170, -40),
            Triple("guam", 256, 256),
        )

    fun herbiboarHerbs() =
        dbTable("dbtable.herbiboar_herbs", serverOnly = true) {
            column("herb", HERB, VarType.OBJ)
            column("low", LOW, VarType.INT)
            column("high", HIGH, VarType.INT)

            for ((herb, low, high) in HERBS) {
                row("dbrow.herbiboar_herb_$herb") {
                    columnRSCM(HERB, "obj.unidentified_$herb")
                    column(LOW, low)
                    column(HIGH, high)
                }
            }
        }
}

object HerbiboarFossilsTable {
    const val FOSSIL = 0
    const val WEIGHT = 1
    const val MIN = 2
    const val MAX = 3

    private data class Fossil(val name: String, val weight: Int, val min: Int, val max: Int)

    private val FOSSILS =
        listOf(
            Fossil("numulite", 1260, 5, 24),
            Fossil("small_unid", 70, 1, 1),
            Fossil("medium_unid", 35, 1, 1),
            Fossil("large_unid", 28, 1, 1),
            Fossil("rare_unid", 7, 1, 1),
        )

    fun herbiboarFossils() =
        dbTable("dbtable.herbiboar_fossils", serverOnly = true) {
            column("fossil", FOSSIL, VarType.OBJ)
            column("weight", WEIGHT, VarType.INT)
            column("min", MIN, VarType.INT)
            column("max", MAX, VarType.INT)

            for (fossil in FOSSILS) {
                row("dbrow.herbiboar_fossil_${fossil.name}") {
                    columnRSCM(FOSSIL, "obj.fossil_${fossil.name}")
                    column(WEIGHT, fossil.weight)
                    column(MIN, fossil.min)
                    column(MAX, fossil.max)
                }
            }
        }
}
