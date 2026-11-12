package org.rsmod.content.minigames.sepulchre.pack

import dev.openrune.definition.dbtables.DBTableBuilder
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import dev.openrune.rscm.RSCM.asRSCM
import org.rsmod.map.CoordGrid

/**
 * One row per floor of the Hallowed Sepulchre
 * (https://oldschool.runescape.wiki/w/Hallowed_Sepulchre).
 * `centre` is where a descent lands first, the tick before the path start. A floor's treasure
 * encounters set their state varbits on arrival: every `encounters` varbit, plus one of
 * `encounter_choices` at random. Stepping into a `checkpoints` box (south-west, north-east,
 * respawn) makes its respawn the tile a trap hit sends the player back to; until then it is the
 * floor's start. `treasure_xp` is the Agility xp each treasure crossing on the floor pays: a mended
 * bridge, a grapple swing, a conjured portal or an opened holy barrier.
 */
object SepulchreFloorsTable {
    const val FLOOR = 0
    const val LEVEL = 1
    const val XP = 2
    const val PB = 3
    const val COMPLETIONS = 4
    const val CENTRE = 5
    const val STARTS = 6
    const val BOUNDS = 7
    const val DAMAGE = 8
    const val STAIRS = 9
    const val ENCOUNTERS = 10
    const val ENCOUNTER_CHOICES = 11
    const val CHECKPOINTS = 12
    const val COFFIN_MARKS = 13
    const val LOOT_TIERS = 14
    const val LOCKPICK_CHANCE = 15
    const val CLUE = 16
    const val PAGE = 17
    const val PAGE_FOUND = 18
    const val PET_CHANCE = 19
    const val TREASURE_XP = 20

    fun floors() =
        dbTable("dbtable.hallowed_floors", serverOnly = true) {
            column("floor", FLOOR, VarType.INT)
            column("level", LEVEL, VarType.INT)
            column("xp", XP, VarType.INT)
            column("pb", PB, VarType.INT)
            column("completions", COMPLETIONS, VarType.VARP)
            column("centre", CENTRE, VarType.COORDGRID)
            column("starts", STARTS, VarType.COORDGRID)
            column("bounds", BOUNDS, VarType.COORDGRID, VarType.COORDGRID)
            column("damage", DAMAGE, VarType.INT, VarType.INT)
            column("stairs", STAIRS, VarType.LOC)
            column("encounters", ENCOUNTERS, VarType.INT)
            column("encounter_choices", ENCOUNTER_CHOICES, VarType.INT)
            column(
                "checkpoints",
                CHECKPOINTS,
                VarType.COORDGRID,
                VarType.COORDGRID,
                VarType.COORDGRID,
            )
            column("coffin_marks", COFFIN_MARKS, VarType.INT, VarType.INT)
            column("loot_tiers", LOOT_TIERS, VarType.INT, VarType.INT, VarType.INT)
            column("lockpick_chance", LOCKPICK_CHANCE, VarType.INT)
            column("clue", CLUE, VarType.OBJ, VarType.INT)
            column("page", PAGE, VarType.OBJ)
            column("page_found", PAGE_FOUND, VarType.INT)
            column("pet_chance", PET_CHANCE, VarType.INT)
            column("treasure_xp", TREASURE_XP, VarType.INT)

            floor(
                "dbrow.hallowed_floor_1",
                floor = 1,
                level = 52,
                xp = 5000,
                pb = "varbit.hallowed_time_pb_f1",
                completions = "varp.hallowed_floor1_completions",
                centre = tile(2272, 5984, 2),
                // UNVERIFIED: the south start; the other three are captured.
                starts =
                    listOf(
                        tile(2253, 6018, 2),
                        tile(2309, 6011, 2),
                        tile(2293, 5949, 2),
                        tile(2234, 5960, 2),
                    ),
                bounds = tile(2224, 5940, 0) to tile(2315, 6025, 3),
                damage = 1 to 3,
                stairs = "loc.hallowed_stairs_floor1",
                encounterChoices =
                    listOf("varbit.hallowed_ranged_state", "varbit.hallowed_construction_state"),
                // UNVERIFIED: the drop landings; the other boxes are where captured hits respawned.
                checkpoints =
                    listOf(
                        Checkpoint(tile(2281, 6000, 2), tile(2283, 6001, 2), tile(2282, 6000, 2)),
                        Checkpoint(tile(2244, 6004, 2), tile(2246, 6006, 2), tile(2245, 6005, 2)),
                        Checkpoint(tile(2243, 5975, 2), tile(2245, 5977, 2), tile(2244, 5976, 2)),
                        Checkpoint(tile(2282, 5985, 1), tile(2282, 5985, 1), tile(2282, 5985, 1)),
                        Checkpoint(tile(2262, 5985, 1), tile(2262, 5985, 1), tile(2262, 5985, 1)),
                    ),
                coffinMarks = 1 to 1,
                lootTiers = Triple(60, 35, 5),
                lockpickChance = 200,
                clue = "obj.trail_clue_easy_simple001" to 50,
                page = "obj.hallowed_floor1_page",
                pageFound = "varbit.hallowed_tome_page1",
                petChance = 35000,
                treasureXp = 500,
            )
            floor(
                "dbrow.hallowed_floor_2",
                floor = 2,
                level = 62,
                xp = 8500,
                pb = "varbit.hallowed_time_pb_f2",
                completions = "varp.hallowed_floor2_completions",
                centre = tile(2528, 5984, 2),
                // UNVERIFIED: the north and south starts; east and west are captured.
                starts =
                    listOf(
                        tile(2528, 5988, 2),
                        tile(2532, 5984, 2),
                        tile(2528, 5980, 2),
                        tile(2524, 5984, 2),
                    ),
                bounds = tile(2484, 5940, 0) to tile(2565, 6025, 3),
                damage = 1 to 3,
                stairs = "loc.hallowed_stairs_floor2",
                encounterChoices =
                    listOf("varbit.hallowed_prayer_state", "varbit.hallowed_ranged_state"),
                // UNVERIFIED: the east stairs and west drop landings; the rest are captured
                // respawns.
                checkpoints =
                    listOf(
                        Checkpoint(tile(2534, 5983, 2), tile(2536, 5985, 2), tile(2535, 5984, 2)),
                        Checkpoint(tile(2548, 5983, 1), tile(2550, 5985, 1), tile(2549, 5984, 1)),
                        Checkpoint(tile(2555, 5958, 1), tile(2555, 5958, 1), tile(2555, 5958, 1)),
                        Checkpoint(tile(2493, 5977, 1), tile(2493, 5977, 1), tile(2493, 5977, 1)),
                        Checkpoint(tile(2510, 5968, 1), tile(2512, 5970, 1), tile(2511, 5969, 1)),
                        Checkpoint(tile(2527, 6010, 1), tile(2529, 6012, 1), tile(2528, 6011, 1)),
                        Checkpoint(tile(2554, 5968, 1), tile(2556, 5970, 1), tile(2555, 5969, 1)),
                    ),
                coffinMarks = 2 to 3,
                lootTiers = Triple(30, 60, 10),
                lockpickChance = 120,
                clue = "obj.trail_medium_emote_exp1" to 50,
                page = "obj.hallowed_floor2_page",
                pageFound = "varbit.hallowed_tome_page2",
                petChance = 16000,
                treasureXp = 500,
            )
            floor(
                "dbrow.hallowed_floor_3",
                floor = 3,
                level = 72,
                xp = 15250,
                pb = "varbit.hallowed_time_pb_f3",
                completions = "varp.hallowed_floor3_completions",
                centre = tile(2400, 5856, 2),
                starts = listOf(tile(2404, 5856, 2), tile(2396, 5856, 2)),
                bounds = tile(2360, 5815, 0) to tile(2440, 5895, 3),
                damage = 2 to 4,
                stairs = "loc.hallowed_stairs_floor3",
                encounters = listOf("varbit.hallowed_ranged_state"),
                encounterChoices =
                    listOf("varbit.hallowed_prayer_state", "varbit.hallowed_magic_state"),
                checkpoints =
                    listOf(
                        Checkpoint(tile(2414, 5858, 2), tile(2416, 5860, 2), tile(2415, 5859, 2)),
                        Checkpoint(tile(2427, 5867, 2), tile(2429, 5869, 2), tile(2428, 5868, 2)),
                        Checkpoint(tile(2421, 5838, 2), tile(2423, 5840, 2), tile(2422, 5839, 2)),
                        Checkpoint(tile(2410, 5845, 2), tile(2412, 5847, 2), tile(2411, 5846, 2)),
                        Checkpoint(tile(2402, 5828, 1), tile(2403, 5829, 1), tile(2402, 5828, 1)),
                        Checkpoint(tile(2379, 5869, 2), tile(2381, 5871, 2), tile(2380, 5870, 2)),
                        Checkpoint(tile(2372, 5839, 2), tile(2374, 5841, 2), tile(2373, 5840, 2)),
                        Checkpoint(tile(2397, 5828, 1), tile(2398, 5829, 1), tile(2398, 5828, 1)),
                        Checkpoint(tile(2372, 5883, 2), tile(2374, 5885, 2), tile(2373, 5884, 2)),
                    ),
                coffinMarks = 3 to 5,
                lootTiers = Triple(15, 65, 20),
                lockpickChance = 90,
                clue = "obj.trail_medium_emote_exp1" to 25,
                page = "obj.hallowed_floor3_page",
                pageFound = "varbit.hallowed_tome_page3",
                petChance = 8000,
                treasureXp = 750,
            )
            floor(
                "dbrow.hallowed_floor_4",
                floor = 4,
                level = 77,
                xp = 28000,
                pb = "varbit.hallowed_time_pb_f4",
                completions = "varp.hallowed_floor4_completions",
                centre = tile(2528, 5848, 2),
                // UNVERIFIED: the north start; the south is captured.
                starts = listOf(tile(2528, 5852, 2), tile(2528, 5860, 2)),
                bounds = tile(2488, 5808, 0) to tile(2565, 5890, 3),
                damage = 2 to 5,
                stairs = "loc.hallowed_stairs_floor4",
                encounters =
                    listOf("varbit.hallowed_prayer_state", "varbit.hallowed_construction_state"),
                checkpoints =
                    listOf(
                        Checkpoint(tile(2504, 5834, 2), tile(2506, 5836, 2), tile(2505, 5835, 2)),
                        Checkpoint(tile(2501, 5856, 1), tile(2503, 5858, 1), tile(2502, 5857, 1)),
                        Checkpoint(tile(2523, 5842, 1), tile(2523, 5842, 1), tile(2523, 5842, 1)),
                        Checkpoint(tile(2540, 5860, 1), tile(2540, 5860, 1), tile(2540, 5860, 1)),
                        Checkpoint(tile(2505, 5864, 2), tile(2507, 5866, 2), tile(2506, 5865, 2)),
                        Checkpoint(tile(2510, 5872, 2), tile(2512, 5874, 2), tile(2511, 5873, 2)),
                        Checkpoint(tile(2518, 5821, 2), tile(2520, 5823, 2), tile(2519, 5822, 2)),
                        Checkpoint(tile(2538, 5877, 2), tile(2540, 5879, 2), tile(2539, 5878, 2)),
                        Checkpoint(tile(2549, 5861, 1), tile(2551, 5863, 1), tile(2550, 5862, 1)),
                    ),
                coffinMarks = 3 to 6,
                lootTiers = Triple(0, 60, 40),
                lockpickChance = 60,
                clue = "obj.trail_clue_hard_map001" to 50,
                page = "obj.hallowed_floor4_page",
                pageFound = "varbit.hallowed_tome_page4",
                petChance = 4000,
                treasureXp = 750,
            )
            // UNVERIFIED: the bounds come from the map; the rest is captured.
            floor(
                "dbrow.hallowed_floor_5",
                floor = 5,
                level = 87,
                xp = 55750,
                pb = "varbit.hallowed_time_pb_f5",
                completions = "varp.hallowed_floor5_completions",
                centre = tile(2272, 5856, 2),
                starts = listOf(tile(2272, 5884, 2)),
                bounds = tile(2232, 5815, 0) to tile(2312, 5895, 3),
                damage = 3 to 5,
                encounters =
                    listOf(
                        "varbit.hallowed_ranged_state",
                        "varbit.hallowed_magic_state",
                        "varbit.hallowed_construction_state",
                    ),
                checkpoints =
                    listOf(
                        Checkpoint(tile(2271, 5882, 2), tile(2273, 5884, 2), tile(2272, 5883, 2)),
                        Checkpoint(tile(2246, 5879, 2), tile(2248, 5881, 2), tile(2247, 5880, 2)),
                        Checkpoint(tile(2249, 5827, 1), tile(2249, 5827, 1), tile(2249, 5827, 1)),
                        Checkpoint(tile(2250, 5880, 1), tile(2252, 5882, 1), tile(2251, 5881, 1)),
                        Checkpoint(tile(2300, 5828, 0), tile(2300, 5828, 0), tile(2300, 5828, 0)),
                        Checkpoint(tile(2299, 5872, 0), tile(2301, 5874, 0), tile(2300, 5873, 0)),
                    ),
                coffinMarks = 4 to 6,
                lootTiers = Triple(0, 20, 80),
                lockpickChance = 40,
                clue = "obj.trail_elite_emote_exp1" to 60,
                page = "obj.hallowed_floor5_page",
                pageFound = "varbit.hallowed_tome_page5",
                petChance = 2000,
                treasureXp = 800,
            )
        }

    private class Checkpoint(
        val southWest: CoordGrid,
        val northEast: CoordGrid,
        val respawn: CoordGrid,
    )

    private fun DBTableBuilder.floor(
        row: String,
        floor: Int,
        level: Int,
        xp: Int,
        pb: String,
        completions: String,
        centre: CoordGrid,
        starts: List<CoordGrid>,
        bounds: Pair<CoordGrid, CoordGrid>,
        damage: Pair<Int, Int>,
        stairs: String? = null,
        encounters: List<String> = emptyList(),
        encounterChoices: List<String> = emptyList(),
        checkpoints: List<Checkpoint>,
        coffinMarks: Pair<Int, Int>,
        lootTiers: Triple<Int, Int, Int>,
        lockpickChance: Int,
        clue: Pair<String, Int>,
        page: String,
        pageFound: String,
        petChance: Int,
        treasureXp: Int,
    ) {
        row(row) {
            column(FLOOR, floor)
            column(LEVEL, level)
            column(XP, xp)
            column(PB, pb.asRSCM())
            columnRSCM(COMPLETIONS, completions)
            column(CENTRE, centre.packed)
            column(STARTS, starts.map { it.packed })
            column(BOUNDS, bounds.first.packed, bounds.second.packed)
            column(DAMAGE, damage.first, damage.second)
            if (stairs != null) {
                columnRSCM(STAIRS, stairs)
            }
            if (encounters.isNotEmpty()) {
                column(ENCOUNTERS, encounters.map { it.asRSCM() })
            }
            if (encounterChoices.isNotEmpty()) {
                column(ENCOUNTER_CHOICES, encounterChoices.map { it.asRSCM() })
            }
            column(
                CHECKPOINTS,
                checkpoints.flatMap {
                    listOf(it.southWest.packed, it.northEast.packed, it.respawn.packed)
                },
            )
            column(COFFIN_MARKS, coffinMarks.first, coffinMarks.second)
            column(LOOT_TIERS, lootTiers.first, lootTiers.second, lootTiers.third)
            column(LOCKPICK_CHANCE, lockpickChance)
            column(CLUE, clue.first.asRSCM(), clue.second)
            columnRSCM(PAGE, page)
            column(PAGE_FOUND, pageFound.asRSCM())
            column(PET_CHANCE, petChance)
            column(TREASURE_XP, treasureXp)
        }
    }
}

internal fun tile(x: Int, z: Int, level: Int): CoordGrid = CoordGrid(x, z, level)
