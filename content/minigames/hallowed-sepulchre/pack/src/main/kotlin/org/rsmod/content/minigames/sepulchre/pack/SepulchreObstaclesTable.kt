package org.rsmod.content.minigames.sepulchre.pack

import dev.openrune.definition.dbtables.DBTableBuilder
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import org.rsmod.map.CoordGrid

/**
 * The obstacles at the end of each path: the platforms, stepping stones and gate, and the drops
 * and stairs that lead down to a floor's lower level; and the crossings the treasure encounters
 * open, lane by lane in both directions. Each lane is a (start, end) pair, one per placement of
 * the loc; a platform's middle hop and a grapple swing's pillars sit halfway. Stepping stones are
 * a chain of hops, each lane starting where the last one ended. Crossing a `finish` obstacle ends
 * the floor. A treasure crossing has no `xp` of its own and pays its floor's treasure xp.
 */
object SepulchreObstaclesTable {
    const val KIND = 0
    const val LOC = 1
    const val XP = 2
    const val LANES = 3
    const val FINISH = 4

    const val KIND_PLATFORM = 0
    const val KIND_DROP = 1
    const val KIND_STAIRS = 2
    const val KIND_GATE = 3
    const val KIND_BRIDGE = 4
    const val KIND_GRAPPLE = 5
    const val KIND_PORTAL = 6
    const val KIND_BARRIER = 7
    const val KIND_STONES = 8

    fun obstacles() =
        dbTable("dbtable.hallowed_obstacles", serverOnly = true) {
            column("kind", KIND, VarType.INT)
            column("loc", LOC, VarType.LOC)
            column("xp", XP, VarType.INT)
            column("lanes", LANES, VarType.COORDGRID, VarType.COORDGRID)
            column("finish", FINISH, VarType.BOOLEAN)

            obstacle(
                "dbrow.hallowed_platform_west",
                KIND_PLATFORM,
                "loc.hallowed_path_end_jumpover_01_west",
                xp = 500,
                tile(2281, 5984, 1) to tile(2277, 5984, 1),
                tile(2537, 5984, 1) to tile(2533, 5984, 1),
                finish = true,
            )
            obstacle(
                "dbrow.hallowed_platform_east",
                KIND_PLATFORM,
                "loc.hallowed_path_end_jumpover_01_east",
                xp = 500,
                tile(2263, 5984, 1) to tile(2267, 5984, 1),
                tile(2519, 5984, 1) to tile(2523, 5984, 1),
                finish = true,
            )
            // UNVERIFIED: floor 2's north and south paths are not captured.
            obstacle(
                "dbrow.hallowed_platform_north",
                KIND_PLATFORM,
                "loc.hallowed_path_end_jumpover_01_north",
                xp = 500,
                tile(2528, 5975, 1) to tile(2528, 5979, 1),
                finish = true,
            )
            obstacle(
                "dbrow.hallowed_platform_south",
                KIND_PLATFORM,
                "loc.hallowed_path_end_jumpover_01_south",
                xp = 500,
                tile(2528, 5993, 1) to tile(2528, 5989, 1),
                finish = true,
            )
            // UNVERIFIED: the east platform; the west one is captured.
            obstacle(
                "dbrow.hallowed_platform_floor3",
                KIND_PLATFORM,
                "loc.hallowed_path_end_jumpover_02",
                xp = 500,
                tile(2395, 5854, 1) to tile(2395, 5858, 1),
                tile(2405, 5854, 1) to tile(2405, 5858, 1),
                finish = true,
            )
            obstacle(
                "dbrow.hallowed_platform_floor5",
                KIND_STONES,
                "loc.hallowed_floor_5_steppingstone",
                xp = 1000,
                tile(2272, 5865, 0) to tile(2272, 5863, 0),
                tile(2272, 5863, 0) to tile(2272, 5862, 0),
                tile(2272, 5862, 0) to tile(2272, 5860, 0),
                finish = true,
            )
            obstacle(
                "dbrow.hallowed_stones_floor2",
                KIND_STONES,
                "loc.hallowed_floor_2_steppingstone",
                xp = 100,
                tile(2518, 5999, 2) to tile(2518, 6001, 2),
                tile(2518, 6001, 2) to tile(2518, 6003, 2),
                tile(2518, 6003, 2) to tile(2518, 6005, 2),
                tile(2518, 6005, 2) to tile(2518, 6007, 2),
                tile(2518, 6007, 2) to tile(2518, 6009, 2),
            )
            obstacle(
                "dbrow.hallowed_gate_floor4",
                KIND_GATE,
                "loc.hallowed_path_end_gate",
                xp = 500,
                tile(2528, 5844, 1) to tile(2528, 5846, 1),
                finish = true,
            )
            obstacle(
                "dbrow.hallowed_drop_floor1_north",
                KIND_DROP,
                "loc.hallowed_floor_1_northpath_drop",
                xp = 250,
                tile(2282, 5989, 2) to tile(2282, 5985, 1),
            )
            obstacle(
                "dbrow.hallowed_drop_floor1_west",
                KIND_DROP,
                "loc.hallowed_floor_1_westpath_drop",
                xp = 250,
                tile(2262, 5989, 2) to tile(2262, 5985, 1),
            )
            obstacle(
                "dbrow.hallowed_drop_floor2_west",
                KIND_DROP,
                "loc.hallowed_floor_2_westpath_drop",
                xp = 250,
                tile(2493, 5973, 2) to tile(2493, 5977, 1),
            )
            obstacle(
                "dbrow.hallowed_drop_floor3_east",
                KIND_DROP,
                "loc.hallowed_floor_3_eastpath_drop",
                xp = 250,
                tile(2406, 5829, 2) to tile(2402, 5828, 1),
            )
            obstacle(
                "dbrow.hallowed_drop_floor3_west",
                KIND_DROP,
                "loc.hallowed_floor_3_westpath_drop",
                xp = 250,
                tile(2394, 5829, 2) to tile(2398, 5828, 1),
            )
            obstacle(
                "dbrow.hallowed_drop_floor4_north",
                KIND_DROP,
                "loc.hallowed_floor_4_northpath_drop",
                xp = 250,
                tile(2544, 5861, 2) to tile(2540, 5860, 1),
            )
            obstacle(
                "dbrow.hallowed_drop_floor5_first",
                KIND_DROP,
                "loc.hallowed_floor_5_drop_1",
                xp = 250,
                tile(2245, 5828, 2) to tile(2249, 5827, 1),
            )
            obstacle(
                "dbrow.hallowed_drop_floor5_second",
                KIND_DROP,
                "loc.hallowed_floor_5_drop_2",
                xp = 250,
                tile(2299, 5832, 1) to tile(2300, 5828, 0),
            )
            obstacle(
                "dbrow.hallowed_stairs_floor2_east",
                KIND_STAIRS,
                "loc.hallowed_floor_2_eastpath_stairs",
                xp = 250,
                tile(2536, 5959, 2) to tile(2555, 5958, 1),
            )
            obstacle(
                "dbrow.hallowed_stairs_floor2_north",
                KIND_STAIRS,
                "loc.hallowed_floor_2_northpath_stairs",
                xp = 250,
                tile(2548, 6008, 2) to tile(2548, 6008, 1),
            )
            obstacle(
                "dbrow.hallowed_stairs_floor2_south",
                KIND_STAIRS,
                "loc.hallowed_floor_2_southpath_stairs",
                xp = 250,
                tile(2511, 5965, 2) to tile(2511, 5969, 1),
            )
            obstacle(
                "dbrow.hallowed_stairs_floor4_south",
                KIND_STAIRS,
                "loc.hallowed_floor_4_southpath_stairs",
                xp = 250,
                tile(2533, 5842, 2) to tile(2523, 5842, 1),
            )
            // UNVERIFIED: no capture takes these stairs, so each lands by its path's platform.
            obstacle(
                "dbrow.hallowed_stairs_floor1_east",
                KIND_STAIRS,
                "loc.hallowed_floor_1_eastpath_stairs",
                xp = 250,
                tile(2285, 5982, 2) to tile(2281, 5984, 1),
            )
            obstacle(
                "dbrow.hallowed_stairs_floor1_south",
                KIND_STAIRS,
                "loc.hallowed_floor_1_southpath_stairs",
                xp = 250,
                tile(2259, 5982, 2) to tile(2263, 5984, 1),
            )

            // UNVERIFIED: floor 4's bridge; floor 1's is captured.
            obstacle(
                "dbrow.hallowed_bridge",
                KIND_BRIDGE,
                "loc.hallowed_treasure_construction_final",
                xp = null,
                *both(tile(2262, 5981, 1), tile(2262, 5975, 1)),
                *both(tile(2553, 5849, 2), tile(2553, 5843, 2)),
            )
            // UNVERIFIED: no capture crosses the pool bridge in time.
            obstacle(
                "dbrow.hallowed_bridge_pool",
                KIND_BRIDGE,
                "loc.hallowed_treasure_construction_pool_final",
                xp = null,
                *both(tile(2244, 5864, 0), tile(2244, 5858, 0)),
            )
            // UNVERIFIED: every swing but floor 3's east one and floor 5's.
            obstacle(
                "dbrow.hallowed_grapple",
                KIND_GRAPPLE,
                "loc.hallowed_treasure_ranged_pillar_ready",
                xp = null,
                *both(tile(2413, 5868, 2), tile(2403, 5868, 2)),
                *both(tile(2397, 5867, 2), tile(2387, 5867, 2)),
                *both(tile(2282, 5983, 1), tile(2282, 5973, 1)),
                *both(tile(2512, 6004, 2), tile(2522, 6004, 2)),
                *both(tile(2544, 6004, 2), tile(2534, 6004, 2)),
                *both(tile(2293, 5835, 1), tile(2283, 5835, 1)),
            )
            // UNVERIFIED: floor 4's portal; floor 3's and floor 5's are captured.
            obstacle(
                "dbrow.hallowed_portal",
                KIND_PORTAL,
                "loc.hallowed_treasure_magic_final",
                xp = null,
                *both(tile(2386, 5839, 2), tile(2386, 5843, 2)),
                *both(tile(2551, 5855, 2), tile(2551, 5859, 2)),
                *both(tile(2255, 5835, 1), tile(2261, 5835, 1)),
            )
            // UNVERIFIED: no capture passes a holy barrier.
            obstacle(
                "dbrow.hallowed_barrier",
                KIND_BARRIER,
                "loc.hallowed_treasure_prayer_barrier_final",
                xp = null,
                *both(tile(2519, 5960, 2), tile(2521, 5960, 2)),
                *both(tile(2529, 5960, 2), tile(2527, 5960, 2)),
                *both(tile(2413, 5828, 2), tile(2415, 5828, 2)),
                *both(tile(2537, 5842, 1), tile(2539, 5842, 1)),
            )
            obstacle(
                "dbrow.hallowed_grand_barrier",
                KIND_BARRIER,
                "loc.hallowed_final_chest_barrier_inactive",
                xp = 0,
                *both(tile(2272, 5849, 0), tile(2272, 5851, 0)),
                *both(tile(2272, 5858, 0), tile(2272, 5856, 0)),
            )
        }

    private fun both(from: CoordGrid, to: CoordGrid): Array<Pair<CoordGrid, CoordGrid>> =
        arrayOf(from to to, to to from)

    private fun DBTableBuilder.obstacle(
        row: String,
        kind: Int,
        loc: String,
        xp: Int?,
        vararg lanes: Pair<CoordGrid, CoordGrid>,
        finish: Boolean = false,
    ) {
        row(row) {
            column(KIND, kind)
            columnRSCM(LOC, loc)
            if (xp != null) {
                column(XP, xp)
            }
            column(LANES, lanes.flatMap { listOf(it.first.packed, it.second.packed) })
            if (finish) {
                column(FINISH, true)
            }
        }
    }
}
