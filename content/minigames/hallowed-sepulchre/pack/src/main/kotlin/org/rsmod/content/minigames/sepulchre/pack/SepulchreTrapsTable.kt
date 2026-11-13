package org.rsmod.content.minigames.sepulchre.pack

import dev.openrune.definition.dbtables.DBTableBuilder
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import org.rsmod.map.CoordGrid

/**
 * The statues of every floor, grouped by how they fire. Each trap runs on its floor's clock, which
 * starts three ticks after the first player arrives on an empty floor; a trap attacks on the ticks
 * where `clock % period` is one of its `phases`.
 *
 * Wizard statues breathe fire over their `fires` locs (south-west tile, angle) for `attack` ticks.
 * Knight statues throw their `npc` sword from `launch` to `target` at `speed` tiles a tick, hold it
 * `hold` ticks and bring it back, showing `thrown` meanwhile. Crossbowman statues pick `volley` of
 * their statues each attack, each firing a bolt from its own `launches` tile as far as the first
 * lane's `target` reaches. Priest statues strike lightning in an X around each of their `strikes`.
 */
object SepulchreTrapsTable {
    const val FLOOR = 0
    const val KIND = 1
    const val STATUES = 2
    const val FIRES = 3
    const val PERIOD = 4
    const val PHASES = 5
    const val LAUNCHES = 6
    const val TARGET = 7
    const val SPEED = 8
    const val HOLD = 9
    const val VOLLEY = 10
    const val NPC = 11
    const val THROWN = 12
    const val ATTACK = 13
    const val STRIKES = 14

    const val KIND_FLAMES = 0
    const val KIND_SWORD = 1
    const val KIND_BOLTS = 2
    const val KIND_LIGHTNING = 3

    fun traps() =
        dbTable("dbtable.hallowed_traps", serverOnly = true) {
            column("floor", FLOOR, VarType.INT)
            column("kind", KIND, VarType.INT)
            column("statues", STATUES, VarType.COORDGRID)
            column("fires", FIRES, VarType.COORDGRID, VarType.INT)
            column("period", PERIOD, VarType.INT)
            column("phases", PHASES, VarType.INT)
            column("launches", LAUNCHES, VarType.COORDGRID)
            column("target", TARGET, VarType.COORDGRID)
            column("speed", SPEED, VarType.INT)
            column("hold", HOLD, VarType.INT)
            column("volley", VOLLEY, VarType.INT)
            column("npc", NPC, VarType.NPC)
            column("thrown", THROWN, VarType.LOC)
            column("attack", ATTACK, VarType.INT)
            column("strikes", STRIKES, VarType.COORDGRID)

            flames(
                "dbrow.hallowed_trap_floor1_flames",
                floor = 1,
                period = 8,
                phases = listOf(0),
                attack = 3,
                statues =
                    listOf(
                        tile(2241, 5978, 2),
                        tile(2241, 5982, 2),
                        tile(2241, 5985, 2),
                        tile(2241, 5989, 2),
                        tile(2246, 5978, 2),
                        tile(2246, 5982, 2),
                        tile(2246, 5985, 2),
                        tile(2246, 5989, 2),
                        tile(2269, 5963, 2),
                        tile(2269, 5967, 2),
                        tile(2269, 5971, 2),
                        tile(2274, 5963, 2),
                        tile(2274, 5967, 2),
                        tile(2274, 5971, 2),
                        tile(2279, 5992, 2),
                        tile(2279, 5998, 2),
                        tile(2279, 6001, 2),
                        tile(2279, 6006, 2),
                        tile(2279, 6010, 2),
                        tile(2284, 5992, 2),
                        tile(2284, 5996, 2),
                        tile(2284, 5998, 2),
                        tile(2284, 6001, 2),
                        tile(2284, 6006, 2),
                        tile(2284, 6010, 2),
                        tile(2287, 5966, 2),
                        tile(2289, 5966, 2),
                        tile(2289, 5971, 2),
                        tile(2293, 5966, 2),
                        tile(2293, 5971, 2),
                        tile(2296, 5999, 2),
                        tile(2296, 6001, 2),
                        tile(2296, 6003, 2),
                        tile(2301, 5995, 2),
                        tile(2301, 5997, 2),
                        tile(2301, 5999, 2),
                    ),
                fires =
                    listOf(
                        fire(2243, 5978, 2, 3),
                        fire(2243, 5979, 2, 1),
                        fire(2243, 5982, 2, 3),
                        fire(2243, 5983, 2, 1),
                        fire(2243, 5985, 2, 3),
                        fire(2243, 5986, 2, 1),
                        fire(2243, 5989, 2, 3),
                        fire(2243, 5990, 2, 1),
                        fire(2271, 5963, 2, 3),
                        fire(2271, 5964, 2, 1),
                        fire(2271, 5967, 2, 3),
                        fire(2271, 5968, 2, 1),
                        fire(2271, 5971, 2, 3),
                        fire(2271, 5972, 2, 1),
                        fire(2281, 5992, 2, 3),
                        fire(2281, 5993, 2, 1),
                        fire(2281, 5997, 2, 1),
                        fire(2281, 5998, 2, 3),
                        fire(2281, 5999, 2, 1),
                        fire(2281, 6001, 2, 3),
                        fire(2281, 6002, 2, 1),
                        fire(2281, 6006, 2, 3),
                        fire(2281, 6007, 2, 1),
                        fire(2281, 6010, 2, 3),
                        fire(2281, 6011, 2, 1),
                        fire(2288, 5968, 2, 2),
                        fire(2289, 5968, 2, 0),
                        fire(2290, 5968, 2, 2),
                        fire(2293, 5968, 2, 0),
                        fire(2294, 5968, 2, 2),
                        fire(2298, 5996, 2, 1),
                        fire(2298, 5998, 2, 1),
                        fire(2298, 5999, 2, 3),
                        fire(2298, 6000, 2, 1),
                        fire(2298, 6001, 2, 3),
                        fire(2298, 6003, 2, 3),
                    ),
            )
            flames(
                "dbrow.hallowed_trap_floor2_flames",
                floor = 2,
                period = 8,
                phases = listOf(0),
                attack = 3,
                statues =
                    listOf(
                        tile(2485, 5980, 2),
                        tile(2485, 5982, 2),
                        tile(2485, 5987, 2),
                        tile(2490, 5980, 2),
                        tile(2490, 5982, 2),
                        tile(2490, 5985, 2),
                        tile(2525, 6000, 1),
                        tile(2525, 6004, 1),
                        tile(2525, 6008, 1),
                        tile(2530, 6002, 1),
                        tile(2530, 6008, 1),
                        tile(2537, 5981, 2),
                        tile(2537, 5986, 2),
                        tile(2539, 5981, 1),
                        tile(2541, 5986, 1),
                        tile(2541, 5986, 2),
                        tile(2543, 5981, 1),
                        tile(2543, 5981, 2),
                        tile(2545, 5986, 1),
                        tile(2547, 5981, 2),
                        tile(2547, 5986, 2),
                        tile(2552, 5971, 1),
                        tile(2552, 5973, 1),
                        tile(2552, 5979, 1),
                        tile(2557, 5971, 1),
                        tile(2557, 5977, 1),
                        tile(2557, 5979, 1),
                    ),
                fires =
                    listOf(
                        fire(2487, 5980, 2, 3),
                        fire(2487, 5981, 2, 1),
                        fire(2487, 5982, 2, 3),
                        fire(2487, 5983, 2, 1),
                        fire(2487, 5986, 2, 1),
                        fire(2487, 5987, 2, 3),
                        fire(2527, 6000, 1, 3),
                        fire(2527, 6003, 1, 1),
                        fire(2527, 6004, 1, 3),
                        fire(2527, 6008, 1, 3),
                        fire(2527, 6009, 1, 1),
                        fire(2537, 5983, 2, 0),
                        fire(2538, 5983, 2, 2),
                        fire(2540, 5983, 1, 2),
                        fire(2541, 5983, 1, 0),
                        fire(2541, 5983, 2, 0),
                        fire(2544, 5983, 1, 2),
                        fire(2544, 5983, 2, 2),
                        fire(2545, 5983, 1, 0),
                        fire(2547, 5983, 2, 0),
                        fire(2548, 5983, 2, 2),
                        fire(2554, 5971, 1, 3),
                        fire(2554, 5972, 1, 1),
                        fire(2554, 5973, 1, 3),
                        fire(2554, 5978, 1, 1),
                        fire(2554, 5979, 1, 3),
                        fire(2554, 5980, 1, 1),
                    ),
            )
            flames(
                "dbrow.hallowed_trap_floor3_flames_a",
                floor = 3,
                period = 16,
                phases = listOf(0),
                attack = 3,
                statues =
                    listOf(
                        tile(2377, 5876, 2),
                        tile(2382, 5874, 2),
                        tile(2397, 5841, 1),
                        tile(2397, 5843, 1),
                        tile(2397, 5845, 1),
                        tile(2397, 5847, 1),
                        tile(2402, 5831, 1),
                        tile(2402, 5833, 1),
                        tile(2402, 5835, 1),
                        tile(2402, 5837, 1),
                        tile(2408, 5835, 2),
                        tile(2408, 5837, 2),
                        tile(2412, 5861, 2),
                        tile(2412, 5865, 2),
                        tile(2412, 5872, 2),
                        tile(2413, 5835, 2),
                        tile(2413, 5837, 2),
                        tile(2417, 5861, 2),
                        tile(2417, 5865, 2),
                        tile(2417, 5872, 2),
                    ),
                fires =
                    listOf(
                        fire(2379, 5875, 2, 1),
                        fire(2379, 5876, 2, 3),
                        fire(2399, 5832, 1, 1),
                        fire(2399, 5834, 1, 1),
                        fire(2399, 5836, 1, 1),
                        fire(2399, 5838, 1, 1),
                        fire(2399, 5841, 1, 3),
                        fire(2399, 5843, 1, 3),
                        fire(2399, 5845, 1, 3),
                        fire(2399, 5847, 1, 3),
                        fire(2410, 5835, 2, 3),
                        fire(2410, 5836, 2, 1),
                        fire(2410, 5837, 2, 3),
                        fire(2410, 5838, 2, 1),
                        fire(2414, 5861, 2, 3),
                        fire(2414, 5862, 2, 1),
                        fire(2414, 5865, 2, 3),
                        fire(2414, 5866, 2, 1),
                        fire(2414, 5872, 2, 3),
                        fire(2414, 5873, 2, 1),
                    ),
            )
            flames(
                "dbrow.hallowed_trap_floor3_flames_ab",
                floor = 3,
                period = 16,
                phases = listOf(0, 8),
                attack = 3,
                statues =
                    listOf(
                        tile(2377, 5874, 2),
                        tile(2377, 5878, 2),
                        tile(2382, 5872, 2),
                        tile(2382, 5876, 2),
                        tile(2408, 5839, 2),
                        tile(2413, 5839, 2),
                    ),
                fires =
                    listOf(
                        fire(2379, 5873, 2, 1),
                        fire(2379, 5874, 2, 3),
                        fire(2379, 5877, 2, 1),
                        fire(2379, 5878, 2, 3),
                        fire(2410, 5839, 2, 3),
                        fire(2410, 5840, 2, 1),
                    ),
            )
            flames(
                "dbrow.hallowed_trap_floor3_flames_b",
                floor = 3,
                period = 16,
                phases = listOf(8),
                attack = 3,
                statues =
                    listOf(
                        tile(2377, 5872, 2),
                        tile(2382, 5878, 2),
                        tile(2397, 5831, 1),
                        tile(2397, 5833, 1),
                        tile(2397, 5835, 1),
                        tile(2397, 5837, 1),
                        tile(2402, 5841, 1),
                        tile(2402, 5843, 1),
                        tile(2402, 5845, 1),
                        tile(2402, 5847, 1),
                        tile(2408, 5841, 2),
                        tile(2408, 5843, 2),
                        tile(2412, 5863, 2),
                        tile(2412, 5870, 2),
                        tile(2412, 5874, 2),
                        tile(2413, 5841, 2),
                        tile(2413, 5843, 2),
                        tile(2417, 5863, 2),
                        tile(2417, 5870, 2),
                        tile(2417, 5874, 2),
                    ),
                fires =
                    listOf(
                        fire(2379, 5872, 2, 3),
                        fire(2379, 5879, 2, 1),
                        fire(2399, 5831, 1, 3),
                        fire(2399, 5833, 1, 3),
                        fire(2399, 5835, 1, 3),
                        fire(2399, 5837, 1, 3),
                        fire(2399, 5842, 1, 1),
                        fire(2399, 5844, 1, 1),
                        fire(2399, 5846, 1, 1),
                        fire(2399, 5848, 1, 1),
                        fire(2410, 5841, 2, 3),
                        fire(2410, 5842, 2, 1),
                        fire(2410, 5843, 2, 3),
                        fire(2410, 5844, 2, 1),
                        fire(2414, 5863, 2, 3),
                        fire(2414, 5864, 2, 1),
                        fire(2414, 5870, 2, 3),
                        fire(2414, 5871, 2, 1),
                        fire(2414, 5874, 2, 3),
                        fire(2414, 5875, 2, 1),
                    ),
            )
            flames(
                "dbrow.hallowed_trap_floor4_flames_a",
                floor = 4,
                period = 16,
                phases = listOf(0),
                attack = 3,
                statues =
                    listOf(
                        tile(2505, 5822, 2),
                        tile(2505, 5824, 2),
                        tile(2505, 5828, 2),
                        tile(2510, 5822, 2),
                    ),
                fires =
                    listOf(
                        fire(2507, 5822, 2, 3),
                        fire(2507, 5823, 2, 1),
                        fire(2507, 5824, 2, 3),
                        fire(2507, 5828, 2, 3),
                    ),
            )
            flames(
                "dbrow.hallowed_trap_floor4_flames_ab",
                floor = 4,
                period = 16,
                phases = listOf(0, 8),
                attack = 3,
                statues =
                    listOf(
                        tile(2505, 5826, 2),
                        tile(2510, 5824, 2),
                        tile(2510, 5826, 2),
                        tile(2510, 5839, 2),
                        tile(2510, 5844, 2),
                        tile(2512, 5839, 2),
                        tile(2512, 5844, 2),
                        tile(2518, 5839, 2),
                        tile(2518, 5844, 2),
                        tile(2518, 5875, 2),
                        tile(2518, 5880, 2),
                        tile(2520, 5839, 2),
                        tile(2520, 5844, 2),
                        tile(2520, 5875, 2),
                        tile(2520, 5880, 2),
                        tile(2522, 5875, 2),
                        tile(2522, 5880, 2),
                        tile(2524, 5875, 2),
                        tile(2524, 5880, 2),
                        tile(2526, 5875, 2),
                        tile(2526, 5880, 2),
                        tile(2528, 5875, 2),
                        tile(2528, 5880, 2),
                        tile(2530, 5875, 2),
                        tile(2530, 5880, 2),
                        tile(2532, 5875, 2),
                        tile(2532, 5880, 2),
                        tile(2534, 5875, 2),
                        tile(2534, 5880, 2),
                        tile(2536, 5875, 2),
                        tile(2536, 5880, 2),
                    ),
                fires =
                    listOf(
                        fire(2507, 5825, 2, 1),
                        fire(2507, 5826, 2, 3),
                        fire(2507, 5827, 2, 1),
                        fire(2510, 5841, 2, 0),
                        fire(2511, 5841, 2, 2),
                        fire(2512, 5841, 2, 0),
                        fire(2513, 5841, 2, 2),
                        fire(2518, 5841, 2, 0),
                        fire(2518, 5877, 2, 0),
                        fire(2519, 5841, 2, 2),
                        fire(2519, 5877, 2, 2),
                        fire(2520, 5841, 2, 0),
                        fire(2520, 5877, 2, 0),
                        fire(2521, 5841, 2, 2),
                        fire(2521, 5877, 2, 2),
                        fire(2522, 5877, 2, 0),
                        fire(2523, 5877, 2, 2),
                        fire(2524, 5877, 2, 0),
                        fire(2525, 5877, 2, 2),
                        fire(2526, 5877, 2, 0),
                        fire(2527, 5877, 2, 2),
                        fire(2528, 5877, 2, 0),
                        fire(2529, 5877, 2, 2),
                        fire(2530, 5877, 2, 0),
                        fire(2531, 5877, 2, 2),
                        fire(2532, 5877, 2, 0),
                        fire(2533, 5877, 2, 2),
                        fire(2534, 5877, 2, 0),
                        fire(2535, 5877, 2, 2),
                        fire(2536, 5877, 2, 0),
                        fire(2537, 5877, 2, 2),
                    ),
            )
            flames(
                "dbrow.hallowed_trap_floor4_flames_b",
                floor = 4,
                period = 16,
                phases = listOf(8),
                attack = 3,
                statues =
                    listOf(
                        tile(2505, 5830, 2),
                        tile(2510, 5830, 2),
                        tile(2514, 5844, 2),
                        tile(2516, 5839, 2),
                    ),
                fires =
                    listOf(
                        fire(2507, 5830, 2, 3),
                        fire(2507, 5831, 2, 1),
                        fire(2514, 5841, 2, 0),
                        fire(2517, 5841, 2, 2),
                    ),
            )
            flames(
                "dbrow.hallowed_trap_floor5_flames_a",
                floor = 5,
                period = 14,
                phases = listOf(0),
                attack = 2,
                statues =
                    listOf(
                        tile(2252, 5873, 0),
                        tile(2254, 5873, 0),
                        tile(2256, 5873, 0),
                        tile(2258, 5873, 0),
                        tile(2269, 5878, 0),
                        tile(2274, 5873, 0),
                        tile(2274, 5878, 0),
                        tile(2285, 5878, 0),
                        tile(2287, 5878, 0),
                        tile(2289, 5878, 0),
                        tile(2291, 5878, 0),
                        tile(2293, 5878, 0),
                        tile(2257, 5878, 1),
                        tile(2257, 5883, 1),
                        tile(2261, 5878, 1),
                        tile(2263, 5883, 1),
                        tile(2282, 5878, 1),
                        tile(2288, 5883, 1),
                        tile(2255, 5878, 2),
                        tile(2255, 5883, 2),
                        tile(2263, 5878, 2),
                        tile(2263, 5883, 2),
                    ),
                fires =
                    listOf(
                        fire(2253, 5875, 0, 2),
                        fire(2255, 5875, 0, 2),
                        fire(2257, 5875, 0, 2),
                        fire(2259, 5875, 0, 2),
                        fire(2269, 5875, 0, 0),
                        fire(2274, 5875, 0, 0),
                        fire(2275, 5875, 0, 2),
                        fire(2285, 5875, 0, 0),
                        fire(2287, 5875, 0, 0),
                        fire(2289, 5875, 0, 0),
                        fire(2291, 5875, 0, 0),
                        fire(2293, 5875, 0, 0),
                        fire(2257, 5880, 1, 0),
                        fire(2258, 5880, 1, 2),
                        fire(2262, 5880, 1, 2),
                        fire(2263, 5880, 1, 0),
                        fire(2283, 5880, 1, 2),
                        fire(2288, 5880, 1, 0),
                        fire(2255, 5880, 2, 0),
                        fire(2256, 5880, 2, 2),
                        fire(2263, 5880, 2, 0),
                        fire(2264, 5880, 2, 2),
                    ),
            )
            flames(
                "dbrow.hallowed_trap_floor5_flames_b",
                floor = 5,
                period = 14,
                phases = listOf(7),
                attack = 2,
                statues =
                    listOf(
                        tile(2252, 5878, 0),
                        tile(2254, 5878, 0),
                        tile(2256, 5878, 0),
                        tile(2258, 5878, 0),
                        tile(2285, 5873, 0),
                        tile(2287, 5873, 0),
                        tile(2289, 5873, 0),
                        tile(2291, 5873, 0),
                        tile(2293, 5873, 0),
                        tile(2253, 5878, 1),
                        tile(2253, 5883, 1),
                        tile(2255, 5883, 1),
                        tile(2259, 5878, 1),
                        tile(2261, 5883, 1),
                        tile(2278, 5878, 1),
                        tile(2280, 5883, 1),
                        tile(2286, 5883, 1),
                        tile(2257, 5883, 2),
                        tile(2259, 5878, 2),
                        tile(2259, 5883, 2),
                        tile(2267, 5878, 2),
                        tile(2267, 5883, 2),
                    ),
                fires =
                    listOf(
                        fire(2252, 5875, 0, 0),
                        fire(2254, 5875, 0, 0),
                        fire(2256, 5875, 0, 0),
                        fire(2258, 5875, 0, 0),
                        fire(2286, 5875, 0, 2),
                        fire(2288, 5875, 0, 2),
                        fire(2290, 5875, 0, 2),
                        fire(2292, 5875, 0, 2),
                        fire(2294, 5875, 0, 2),
                        fire(2253, 5880, 1, 0),
                        fire(2254, 5880, 1, 2),
                        fire(2255, 5880, 1, 0),
                        fire(2260, 5880, 1, 2),
                        fire(2261, 5880, 1, 0),
                        fire(2279, 5880, 1, 2),
                        fire(2280, 5880, 1, 0),
                        fire(2286, 5880, 1, 0),
                        fire(2257, 5880, 2, 0),
                        fire(2259, 5880, 2, 0),
                        fire(2260, 5880, 2, 2),
                        fire(2267, 5880, 2, 0),
                        fire(2268, 5880, 2, 2),
                    ),
            )
            flames(
                "dbrow.hallowed_trap_floor5_flames_ab",
                floor = 5,
                period = 14,
                phases = listOf(0, 7),
                attack = 2,
                statues =
                    listOf(
                        tile(2250, 5873, 0),
                        tile(2250, 5878, 0),
                        tile(2269, 5873, 0),
                        tile(2255, 5878, 1),
                        tile(2259, 5883, 1),
                        tile(2263, 5878, 1),
                        tile(2265, 5878, 1),
                        tile(2265, 5883, 1),
                        tile(2278, 5883, 1),
                        tile(2280, 5878, 1),
                        tile(2282, 5883, 1),
                        tile(2284, 5878, 1),
                        tile(2284, 5883, 1),
                        tile(2286, 5878, 1),
                        tile(2288, 5878, 1),
                        tile(2290, 5878, 1),
                        tile(2290, 5883, 1),
                        tile(2257, 5878, 2),
                        tile(2261, 5878, 2),
                        tile(2261, 5883, 2),
                        tile(2265, 5878, 2),
                        tile(2265, 5883, 2),
                    ),
                fires =
                    listOf(
                        fire(2250, 5875, 0, 0),
                        fire(2251, 5875, 0, 2),
                        fire(2270, 5875, 0, 2),
                        fire(2256, 5880, 1, 2),
                        fire(2259, 5880, 1, 0),
                        fire(2264, 5880, 1, 2),
                        fire(2265, 5880, 1, 0),
                        fire(2266, 5880, 1, 2),
                        fire(2278, 5880, 1, 0),
                        fire(2281, 5880, 1, 2),
                        fire(2282, 5880, 1, 0),
                        fire(2284, 5880, 1, 0),
                        fire(2285, 5880, 1, 2),
                        fire(2287, 5880, 1, 2),
                        fire(2289, 5880, 1, 2),
                        fire(2290, 5880, 1, 0),
                        fire(2291, 5880, 1, 2),
                        fire(2258, 5880, 2, 2),
                        fire(2261, 5880, 2, 0),
                        fire(2262, 5880, 2, 2),
                        fire(2265, 5880, 2, 0),
                        fire(2266, 5880, 2, 2),
                    ),
            )
            sword(
                "dbrow.hallowed_trap_floor1_sword_north",
                floor = 1,
                statue = tile(2237, 5992, 2),
                thrown = "loc.hallowed_sword_trap_owl_thrown",
                npc = "npc.hallowed_sword_npc",
                launch = tile(2237, 5997, 2),
                target = tile(2237, 6007, 2),
                speed = 2,
                hold = 3,
                period = 20,
            )
            sword(
                "dbrow.hallowed_trap_floor1_sword_south",
                floor = 1,
                statue = tile(2283, 5952, 2),
                thrown = "loc.hallowed_sword_trap_lion_thrown",
                npc = "npc.hallowed_sword_npc",
                launch = tile(2278, 5952, 2),
                target = tile(2259, 5952, 2),
                speed = 2,
                hold = 3,
                period = 30,
            )
            sword(
                "dbrow.hallowed_trap_floor1_sword_east",
                floor = 1,
                statue = tile(2304, 5989, 2),
                thrown = "loc.hallowed_sword_trap_unicorn_thrown",
                npc = "npc.hallowed_sword_npc",
                launch = tile(2304, 5984, 2),
                target = tile(2304, 5974, 2),
                speed = 2,
                hold = 3,
                period = 20,
            )
            bolts(
                "dbrow.hallowed_trap_floor1_bolts_north",
                floor = 1,
                statues = column(2264, 6001, 2),
                launches = column(2260, 6001, 2),
                target = tile(2246, 6001, 2),
                npc = "npc.hallowed_projectile_npc",
                speed = 3,
                volley = 1,
                period = 6,
            )
            bolts(
                "dbrow.hallowed_trap_floor1_bolts_south",
                floor = 1,
                statues = column(2260, 5975, 2),
                launches = column(2264, 5975, 2),
                target = tile(2275, 5975, 2),
                npc = "npc.hallowed_projectile_npc",
                speed = 3,
                volley = 1,
                period = 6,
            )
            bolts(
                "dbrow.hallowed_trap_floor1_bolts_east",
                floor = 1,
                statues = column(2284, 6016, 2),
                launches = column(2280, 6016, 2),
                target = tile(2266, 6016, 2),
                npc = "npc.hallowed_projectile_npc",
                speed = 3,
                volley = 1,
                period = 6,
            )

            sword(
                "dbrow.hallowed_trap_floor2_sword_lower",
                floor = 2,
                statue = tile(2492, 5983, 1),
                thrown = "loc.hallowed_sword_trap_thrown",
                npc = "npc.hallowed_sword_npc",
                launch = tile(2497, 5983, 1),
                target = tile(2510, 5983, 1),
                speed = 2,
                hold = 3,
                period = 24,
            )
            sword(
                "dbrow.hallowed_trap_floor2_sword_south",
                floor = 2,
                statue = tile(2505, 5962, 2),
                thrown = "loc.hallowed_sword_trap_thrown",
                npc = "npc.hallowed_sword_npc",
                launch = tile(2500, 5962, 2),
                target = tile(2487, 5962, 2),
                speed = 2,
                hold = 3,
                period = 24,
            )
            sword(
                "dbrow.hallowed_trap_floor2_sword_west",
                floor = 2,
                statue = tile(2510, 5983, 2),
                thrown = "loc.hallowed_sword_trap_thrown",
                npc = "npc.hallowed_sword_npc",
                launch = tile(2510, 5988, 2),
                target = tile(2510, 5998, 2),
                speed = 2,
                hold = 4,
                period = 22,
            )
            sword(
                "dbrow.hallowed_trap_floor2_sword_north",
                floor = 2,
                statue = tile(2531, 6012, 2),
                thrown = "loc.hallowed_sword_trap_thrown",
                npc = "npc.hallowed_sword_npc",
                launch = tile(2536, 6012, 2),
                target = tile(2549, 6012, 2),
                speed = 2,
                hold = 3,
                period = 24,
            )
            bolts(
                "dbrow.hallowed_trap_floor2_bolts_south",
                floor = 2,
                statues = column(2512, 5971, 2),
                launches = column(2516, 5971, 2),
                target = tile(2530, 5971, 2),
                npc = "npc.hallowed_projectile_npc",
                speed = 3,
                volley = 1,
                period = 6,
            )
            bolts(
                "dbrow.hallowed_trap_floor2_bolts_north",
                floor = 2,
                statues = column(2528, 6012, 2),
                launches = column(2524, 6012, 2),
                target = tile(2516, 6012, 2),
                npc = "npc.hallowed_projectile_npc",
                speed = 3,
                volley = 1,
                period = 6,
            )
            bolts(
                "dbrow.hallowed_trap_floor2_bolts_lower",
                floor = 2,
                statues = column(2531, 5971, 1),
                launches = column(2527, 5971, 1),
                target = tile(2504, 5971, 1),
                npc = "npc.hallowed_projectile_npc",
                speed = 3,
                volley = 1,
                period = 6,
            )
            bolts(
                "dbrow.hallowed_trap_floor2_bolts_east",
                floor = 2,
                statues = row(2554, 5954, 2),
                launches = row(2554, 5958, 2),
                target = tile(2554, 5975, 2),
                npc = "npc.hallowed_projectile_npc",
                speed = 3,
                volley = 1,
                period = 6,
            )

            sword(
                "dbrow.hallowed_trap_floor3_sword_northwest",
                floor = 3,
                statue = tile(2369, 5884, 2),
                thrown = "loc.hallowed_sword_trap_t2_thrown",
                npc = "npc.hallowed_sword_npc_t2",
                launch = tile(2369, 5878, 2),
                target = tile(2369, 5866, 2),
                speed = 3,
                hold = 5,
                period = 22,
            )
            sword(
                "dbrow.hallowed_trap_floor3_sword_southwest",
                floor = 3,
                statue = tile(2369, 5836, 2),
                thrown = "loc.hallowed_sword_trap_t2_thrown",
                npc = "npc.hallowed_sword_npc_t2",
                launch = tile(2375, 5836, 2),
                target = tile(2390, 5836, 2),
                speed = 3,
                hold = 4,
                period = 22,
            )
            sword(
                "dbrow.hallowed_trap_floor3_sword_northeast",
                floor = 3,
                statue = tile(2411, 5879, 2),
                thrown = "loc.hallowed_sword_trap_t2_thrown",
                npc = "npc.hallowed_sword_npc_t2",
                launch = tile(2417, 5879, 2),
                target = tile(2432, 5879, 2),
                speed = 3,
                hold = 5,
                period = 24,
            )
            sword(
                "dbrow.hallowed_trap_floor3_sword_southeast",
                floor = 3,
                statue = tile(2418, 5835, 2),
                thrown = "loc.hallowed_sword_trap_t2_thrown",
                npc = "npc.hallowed_sword_npc_t2",
                launch = tile(2418, 5841, 2),
                target = tile(2418, 5850, 2),
                speed = 3,
                hold = 5,
                period = 20,
            )
            bolts(
                "dbrow.hallowed_trap_floor3_bolts_west",
                floor = 3,
                statues = row(2369, 5844, 2),
                launches = row(2369, 5848, 2),
                target = tile(2369, 5862, 2),
                npc = "npc.hallowed_projectile_npc_t2",
                speed = 3,
                volley = 2,
                period = 5,
            )
            bolts(
                "dbrow.hallowed_trap_floor3_bolts_centre",
                floor = 3,
                statues = column(2378, 5859, 2),
                launches = column(2382, 5859, 2),
                target = tile(2392, 5859, 2),
                npc = "npc.hallowed_projectile_npc_t2",
                speed = 3,
                volley = 2,
                period = 5,
            )
            bolts(
                "dbrow.hallowed_trap_floor3_bolts_lower",
                floor = 3,
                statues = row(2399, 5822, 1),
                launches = row(2399, 5826, 1),
                target = tile(2399, 5852, 1),
                npc = "npc.hallowed_projectile_npc_t2",
                speed = 3,
                volley = 2,
                period = 6,
            )
            bolts(
                "dbrow.hallowed_trap_floor3_bolts_east",
                floor = 3,
                statues = row(2424, 5837, 2),
                launches = row(2424, 5841, 2),
                target = tile(2424, 5870, 2),
                npc = "npc.hallowed_projectile_npc_t2",
                speed = 3,
                volley = 2,
                period = 5,
            )

            sword(
                "dbrow.hallowed_trap_floor4_sword_north",
                floor = 4,
                statue = tile(2503, 5861, 2),
                thrown = "loc.hallowed_sword_trap_t2_lion_thrown",
                npc = "npc.hallowed_sword_npc_t2",
                launch = tile(2503, 5867, 2),
                target = tile(2503, 5885, 2),
                speed = 3,
                hold = 4,
                period = 24,
            )
            sword(
                "dbrow.hallowed_trap_floor4_sword_south",
                floor = 4,
                statue = tile(2507, 5838, 2),
                thrown = "loc.hallowed_sword_trap_t2_lion_thrown",
                npc = "npc.hallowed_sword_npc_t2",
                launch = tile(2507, 5832, 2),
                target = tile(2507, 5814, 2),
                speed = 3,
                hold = 8,
                period = 32,
            )
            sword(
                "dbrow.hallowed_trap_floor4_sword_lower_west",
                floor = 4,
                statue = tile(2513, 5838, 1),
                thrown = "loc.hallowed_sword_trap_t2_lion_thrown",
                npc = "npc.hallowed_sword_npc_t2",
                launch = tile(2513, 5844, 1),
                target = tile(2513, 5871, 1),
                speed = 3,
                hold = 8,
                period = 38,
            )
            sword(
                "dbrow.hallowed_trap_floor4_sword_lower_east",
                floor = 4,
                statue = tile(2552, 5865, 1),
                thrown = "loc.hallowed_sword_trap_t3_lion_thrown",
                npc = "npc.hallowed_sword_npc_t3",
                launch = tile(2552, 5858, 1),
                target = tile(2552, 5841, 1),
                speed = 4,
                hold = 10,
                period = 34,
            )
            bolts(
                "dbrow.hallowed_trap_floor4_bolts_lower_west",
                floor = 4,
                statues = row(2504, 5828, 1),
                launches = row(2504, 5833, 1),
                target = tile(2504, 5858, 1),
                npc = "npc.hallowed_projectile_npc_t3",
                speed = 4,
                volley = 2,
                period = 5,
            )
            bolts(
                "dbrow.hallowed_trap_floor4_bolts_north",
                floor = 4,
                statues = column(2546, 5885, 2),
                launches = column(2542, 5885, 2),
                target = tile(2501, 5885, 2),
                npc = "npc.hallowed_projectile_npc_t2",
                speed = 3,
                volley = 2,
                period = 5,
            )
            bolts(
                "dbrow.hallowed_trap_floor4_bolts_lower_east",
                floor = 4,
                statues = row(2552, 5825, 1),
                launches = row(2552, 5830, 1),
                target = tile(2552, 5864, 1),
                npc = "npc.hallowed_projectile_npc_t3",
                speed = 4,
                volley = 2,
                period = 5,
            )
            bolts(
                "dbrow.hallowed_trap_floor4_bolts_east",
                floor = 4,
                statues = column(2553, 5869, 2),
                launches = column(2549, 5869, 2),
                target = tile(2510, 5869, 2),
                npc = "npc.hallowed_projectile_npc_t2",
                speed = 3,
                volley = 2,
                period = 5,
            )
            bolts(
                "dbrow.hallowed_trap_floor4_bolts_south",
                floor = 4,
                statues = column(2555, 5824, 2),
                launches = column(2551, 5824, 2),
                target = tile(2516, 5824, 2),
                npc = "npc.hallowed_projectile_npc_t2",
                speed = 3,
                volley = 2,
                period = 5,
            )
            lightning(
                "dbrow.hallowed_trap_floor4_lightning",
                floor = 4,
                statues =
                    listOf(
                        tile(2496, 5837, 2),
                        tile(2496, 5844, 2),
                        tile(2509, 5859, 2),
                        tile(2517, 5859, 2),
                        tile(2534, 5864, 2),
                        tile(2542, 5864, 2),
                        tile(2509, 5843, 1),
                        tile(2509, 5851, 1),
                        tile(2509, 5859, 1),
                    ),
                strikes =
                    listOf(
                        tile(2502, 5838, 2),
                        tile(2502, 5845, 2),
                        tile(2510, 5865, 2),
                        tile(2518, 5865, 2),
                        tile(2535, 5870, 2),
                        tile(2543, 5870, 2),
                        tile(2514, 5844, 1),
                        tile(2514, 5852, 1),
                        tile(2514, 5860, 1),
                    ),
                period = 6,
            )

            sword(
                "dbrow.hallowed_trap_floor5_sword_upper",
                floor = 5,
                statue = tile(2243, 5880, 2),
                thrown = "loc.hallowed_sword_trap_t3_thrown",
                npc = "npc.hallowed_sword_npc_t3",
                launch = tile(2243, 5873, 2),
                target = tile(2243, 5845, 2),
                speed = 4,
                hold = 5,
                period = 28,
            )
            sword(
                "dbrow.hallowed_trap_floor5_sword_lower",
                floor = 5,
                statue = tile(2299, 5881, 1),
                thrown = "loc.hallowed_sword_trap_t3_thrown",
                npc = "npc.hallowed_sword_npc_t3",
                launch = tile(2299, 5874, 1),
                target = tile(2299, 5845, 1),
                speed = 4,
                hold = 4,
                period = 28,
            )
            bolts(
                "dbrow.hallowed_trap_floor5_bolts_west",
                floor = 5,
                statues = column(2242, 5875, 0),
                launches = column(2247, 5875, 0),
                target = tile(2302, 5875, 0),
                npc = "npc.hallowed_projectile_npc_t3",
                speed = 4,
                volley = 2,
                period = 5,
            )
            bolts(
                "dbrow.hallowed_trap_floor5_bolts_middle",
                floor = 5,
                statues = row(2243, 5861, 1),
                launches = row(2243, 5856, 1),
                target = tile(2243, 5829, 1),
                npc = "npc.hallowed_projectile_npc_t3",
                speed = 4,
                volley = 2,
                period = 5,
            )
            bolts(
                "dbrow.hallowed_trap_floor5_bolts_east",
                floor = 5,
                statues = row(2296, 5870, 0),
                launches = row(2296, 5865, 0),
                target = tile(2296, 5826, 0),
                npc = "npc.hallowed_projectile_npc_t3",
                speed = 4,
                volley = 2,
                period = 5,
            )
            lightning(
                "dbrow.hallowed_trap_floor5_lightning",
                floor = 5,
                statues =
                    listOf(
                        tile(2262, 5880, 0),
                        tile(2280, 5880, 0),
                        tile(2300, 5837, 0),
                        tile(2300, 5860, 0),
                        tile(2269, 5883, 1),
                        tile(2273, 5883, 1),
                    ),
                strikes =
                    listOf(
                        tile(2262, 5876, 0),
                        tile(2264, 5876, 0),
                        tile(2280, 5876, 0),
                        tile(2282, 5876, 0),
                        tile(2297, 5837, 0),
                        tile(2297, 5839, 0),
                        tile(2297, 5860, 0),
                        tile(2297, 5862, 0),
                        tile(2269, 5881, 1),
                        tile(2271, 5881, 1),
                        tile(2273, 5881, 1),
                        tile(2275, 5881, 1),
                    ),
                period = 5,
            )
        }

    private class Fire(val coords: CoordGrid, val angle: Int)

    private fun fire(x: Int, z: Int, level: Int, angle: Int): Fire = Fire(tile(x, z, level), angle)

    private fun row(x: Int, z: Int, level: Int): List<CoordGrid> =
        List(BOLT_STATUES) { tile(x + it, z, level) }

    private fun column(x: Int, z: Int, level: Int): List<CoordGrid> =
        List(BOLT_STATUES) { tile(x, z + it, level) }

    private fun DBTableBuilder.flames(
        row: String,
        floor: Int,
        period: Int,
        phases: List<Int>,
        attack: Int,
        statues: List<CoordGrid>,
        fires: List<Fire>,
    ) {
        row(row) {
            column(FLOOR, floor)
            column(KIND, KIND_FLAMES)
            column(STATUES, statues.map { it.packed })
            column(FIRES, fires.flatMap { listOf(it.coords.packed, it.angle) })
            column(PERIOD, period)
            column(PHASES, phases)
            column(ATTACK, attack)
        }
    }

    private fun DBTableBuilder.sword(
        row: String,
        floor: Int,
        statue: CoordGrid,
        thrown: String,
        npc: String,
        launch: CoordGrid,
        target: CoordGrid,
        speed: Int,
        hold: Int,
        period: Int,
    ) {
        row(row) {
            column(FLOOR, floor)
            column(KIND, KIND_SWORD)
            column(STATUES, statue.packed)
            column(PERIOD, period)
            column(PHASES, 0)
            column(LAUNCHES, launch.packed)
            column(TARGET, target.packed)
            column(SPEED, speed)
            column(HOLD, hold)
            columnRSCM(NPC, npc)
            columnRSCM(THROWN, thrown)
        }
    }

    private fun DBTableBuilder.bolts(
        row: String,
        floor: Int,
        statues: List<CoordGrid>,
        launches: List<CoordGrid>,
        target: CoordGrid,
        npc: String,
        speed: Int,
        volley: Int,
        period: Int,
    ) {
        row(row) {
            column(FLOOR, floor)
            column(KIND, KIND_BOLTS)
            column(STATUES, statues.map { it.packed })
            column(PERIOD, period)
            column(PHASES, 0)
            column(LAUNCHES, launches.map { it.packed })
            column(TARGET, target.packed)
            column(SPEED, speed)
            column(VOLLEY, volley)
            columnRSCM(NPC, npc)
        }
    }

    private fun DBTableBuilder.lightning(
        row: String,
        floor: Int,
        statues: List<CoordGrid>,
        strikes: List<CoordGrid>,
        period: Int,
    ) {
        row(row) {
            column(FLOOR, floor)
            column(KIND, KIND_LIGHTNING)
            column(STATUES, statues.map { it.packed })
            column(PERIOD, period)
            column(PHASES, 0)
            column(STRIKES, strikes.map { it.packed })
        }
    }

    private const val BOLT_STATUES = 3
}
