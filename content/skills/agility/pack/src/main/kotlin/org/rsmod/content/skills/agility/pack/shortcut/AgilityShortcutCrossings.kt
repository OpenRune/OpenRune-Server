package org.rsmod.content.skills.agility.pack.shortcut

import dev.openrune.definition.dbtables.DBTableBuilder
import org.rsmod.content.skills.agility.pack.FallDamage
import org.rsmod.content.skills.agility.pack.LaneBuilder
import org.rsmod.content.skills.agility.pack.apRange
import org.rsmod.content.skills.agility.pack.fails
import org.rsmod.content.skills.agility.pack.failsOn
import org.rsmod.content.skills.agility.pack.lane
import org.rsmod.content.skills.agility.pack.lanes
import org.rsmod.content.skills.agility.pack.tile
import org.rsmod.map.CoordGrid

/*
 * Agility shortcuts crossed: stepping stones, logs, hops and monkey bars. The Wilderness lava
 * maze, Lumbridge Swamp Caves, Shilo river, Necropolis, Rellekka and Nature Grotto stones, the
 * Ardougne log, the Rellekka and Barrows hurdles and the Yanille dungeon bars are captured; every
 * other row is UNVERIFIED for live timing. Only the stones and logs whose wiki page charts the
 * success chance and the captured Mort Myre bridge can fail.
 */
internal fun DBTableBuilder.shortcutCrossings() {
    row("dbrow.agility_shortcut_lumbridge_swamp_stones") {
        shortcut(
            level = 1,
            xp = 30,
            "loc.swamp_cave_steppingstone_b",
            "loc.swamp_cave_steppingstone_a",
        )
        apRange(2)
        lanes(
            lane(tile(3221, 9556, 0)) {
                steppingStoneLeaps(
                    way(tile(3221, 9556, 0), tile(3221, 9552, 0)),
                    listOf(tile(3221, 9554, 0)),
                )
            },
            lane(tile(3221, 9552, 0)) {
                steppingStoneLeaps(
                    way(tile(3221, 9552, 0), tile(3221, 9556, 0)),
                    listOf(tile(3221, 9554, 0)),
                )
            },
            lane(tile(3204, 9572, 0)) {
                steppingStoneLeaps(
                    way(tile(3204, 9572, 0), tile(3208, 9572, 0)),
                    listOf(tile(3206, 9572, 0)),
                )
            },
            lane(tile(3208, 9572, 0)) {
                steppingStoneLeaps(
                    way(tile(3208, 9572, 0), tile(3204, 9572, 0)),
                    listOf(tile(3206, 9572, 0)),
                )
            },
        )
        fails(
            51,
            252,
            FallDamage(1, 3),
            lane(tile(3221, 9556, 0)) { slip(tile(3221, 9556, 0), 10) },
            lane(tile(3221, 9552, 0)) { slip(tile(3221, 9552, 0), 10) },
            lane(tile(3204, 9572, 0)) { slip(tile(3204, 9572, 0), 10) },
            lane(tile(3208, 9572, 0)) { slip(tile(3208, 9572, 0), 10) },
        )
    }

    row("dbrow.agility_shortcut_brimhaven_dungeon_stones") {
        shortcut(level = 12, xp = 75, "loc.karam_dungeon_stone1", "loc.karam_dungeon_stone2")
        lanes(
            lane(tile(2649, 9562, 0)) {
                stoneHops(
                    way(tile(2649, 9562, 0), tile(2647, 9557, 0)),
                    listOf(tile(2649, 9561, 0), tile(2647, 9558, 0)),
                )
            },
            lane(tile(2647, 9557, 0)) {
                stoneHops(
                    way(tile(2647, 9557, 0), tile(2649, 9562, 0)),
                    listOf(tile(2647, 9558, 0), tile(2649, 9561, 0)),
                )
            },
        )
    }

    row("dbrow.agility_shortcut_hosidius_quest_stones") {
        shortcut(level = 18, xp = 0, "loc.hosidiusquest_stone")
        lanes(
            lane(tile(1708, 9800, 0)) {
                stoneHops(
                    way(tile(1708, 9800, 0), tile(1702, 9800, 0)),
                    listOf(tile(1706, 9800, 0), tile(1704, 9800, 0)),
                )
            },
            lane(tile(1702, 9800, 0)) {
                stoneHops(
                    way(tile(1702, 9800, 0), tile(1708, 9800, 0)),
                    listOf(tile(1704, 9800, 0), tile(1706, 9800, 0)),
                )
            },
        )
    }

    row("dbrow.agility_shortcut_catacombs_stones") {
        shortcut(level = 28, xp = 20, "loc.zeah_cata_stepstone")
        lanes(
            lane(tile(1613, 10070, 0)) {
                stoneHops(
                    way(tile(1613, 10070, 0), tile(1609, 10061, 0)),
                    listOf(tile(1613, 10067, 0), tile(1610, 10064, 0), tile(1610, 10063, 0)),
                )
            },
            lane(tile(1609, 10061, 0)) {
                stoneHops(
                    way(tile(1609, 10061, 0), tile(1613, 10070, 0)),
                    listOf(tile(1610, 10064, 0), tile(1613, 10067, 0), tile(1613, 10068, 0)),
                )
            },
        )
    }

    row("dbrow.agility_shortcut_karamja_stones") {
        shortcut(level = 30, xp = 30, "loc.zqrockjump1", "loc.zqrockjump3")
        ways(
            listOf(
                way(tile(2925, 2951, 0), tile(2925, 2950, 0)),
                way(tile(2925, 2949, 0), tile(2925, 2948, 0)),
                way(tile(2925, 2947, 0), tile(2925, 2948, 0)),
            )
        ) {
            stoneHops(it)
        }
        fails(
            50,
            253,
            FallDamage(1, 3),
            lane(tile(2925, 2951, 0)) { slip(tile(2925, 2951, 0), 10) },
            lane(tile(2925, 2949, 0)) { slip(tile(2925, 2949, 0), 10) },
            lane(tile(2925, 2947, 0)) { slip(tile(2925, 2947, 0), 10) },
        )
    }

    row("dbrow.agility_shortcut_shilo_river_stones") {
        shortcut(level = 32, xp = 0, "loc.shilo_river_steppingstone")
        lanes(
            lane(tile(2863, 2971, 0)) {
                stoneHops(
                    way(tile(2863, 2971, 0), tile(2863, 2976, 0)),
                    listOf(tile(2863, 2974, 0)),
                    EVEN_HOPS,
                )
            },
            lane(tile(2863, 2976, 0)) {
                stoneHops(
                    way(tile(2863, 2976, 0), tile(2863, 2971, 0)),
                    listOf(tile(2863, 2974, 0)),
                    EVEN_HOPS,
                )
            },
        )
    }

    row("dbrow.agility_shortcut_auburn_valley_stones") {
        shortcut(level = 36, xp = 140, "loc.av_stepstone_1")
        lanes(
            lane(tile(1393, 3309, 0)) {
                stoneHops(
                    way(tile(1393, 3309, 0), tile(1399, 3309, 0)),
                    listOf(tile(1395, 3309, 0), tile(1397, 3309, 0)),
                )
            },
            lane(tile(1399, 3309, 0)) {
                stoneHops(
                    way(tile(1399, 3309, 0), tile(1393, 3309, 0)),
                    listOf(tile(1397, 3309, 0), tile(1395, 3309, 0)),
                )
            },
        )
    }

    row("dbrow.agility_shortcut_kourend_lake_stones") {
        shortcut(
            level = 40,
            xp = 0,
            "loc.zeah_lake_shortcut_hosidius",
            "loc.zeah_lake_shortcut_shayzien",
        )
        lanes(
            lane(tile(1614, 3570, 0)) {
                stoneHops(
                    way(tile(1614, 3570, 0), tile(1610, 3570, 0)),
                    listOf(tile(1612, 3570, 0)),
                )
            },
            lane(tile(1610, 3570, 0)) {
                stoneHops(
                    way(tile(1610, 3570, 0), tile(1614, 3570, 0)),
                    listOf(tile(1612, 3570, 0)),
                )
            },
            lane(tile(1603, 3571, 0)) {
                stoneHops(
                    way(tile(1603, 3571, 0), tile(1607, 3571, 0)),
                    listOf(tile(1605, 3571, 0)),
                )
            },
            lane(tile(1607, 3571, 0)) {
                stoneHops(
                    way(tile(1607, 3571, 0), tile(1603, 3571, 0)),
                    listOf(tile(1605, 3571, 0)),
                )
            },
        )
    }

    row("dbrow.agility_shortcut_saltpetre_stones") {
        shortcut(level = 45, xp = 0, "loc.zeah_saltpetre_shortcut")
        lanes(
            lane(tile(1720, 3551, 0)) {
                stoneHops(
                    way(tile(1720, 3551, 0), tile(1724, 3551, 0)),
                    listOf(tile(1722, 3551, 0)),
                )
            },
            lane(tile(1724, 3551, 0)) {
                stoneHops(
                    way(tile(1724, 3551, 0), tile(1720, 3551, 0)),
                    listOf(tile(1722, 3551, 0)),
                )
            },
        )
    }

    row("dbrow.agility_shortcut_arceuus_boulder") {
        shortcut(level = 49, xp = 0, "loc.archeuus_runestone_shortcut_boulder")
        lanes(
            lane(tile(1776, 3884, 0)) {
                stoneHops(
                    way(tile(1776, 3884, 0), tile(1776, 3880, 0)),
                    listOf(tile(1776, 3883, 0)),
                )
            },
        )
    }

    row("dbrow.agility_shortcut_fairy_ring_grotto_stones") {
        shortcut(level = 50, xp = 0, "loc.fairy_island_nature_grotto_shortcut")
        lanes(
            lane(tile(3422, 3325, 0)) {
                stoneHops(
                    way(tile(3422, 3325, 0), tile(3417, 3325, 0)),
                    listOf(tile(3419, 3325, 0)),
                    endText = GROTTO_SHORE,
                )
            },
            lane(tile(3417, 3325, 0)) {
                stoneHops(
                    way(tile(3417, 3325, 0), tile(3422, 3325, 0)),
                    listOf(tile(3419, 3325, 0)),
                    endText = GROTTO_SHORE,
                )
            },
        )
    }

    row("dbrow.agility_shortcut_rellekka_diary_stones") {
        shortcut(level = 55, xp = 0, "loc.misc_diary_steppingstone")
        apRange(2)
        ways(
            listOf(
                way(tile(2573, 3859, 0), tile(2575, 3861, 0)),
                way(tile(2575, 3861, 0), tile(2573, 3859, 0)),
            )
        ) {
            stoneHops(it, listOf(tile(2573, 3861, 0)), EVEN_HOPS)
        }
    }

    row("dbrow.agility_shortcut_karamja_dungeon_stones") {
        shortcut(level = 56, xp = 0, "loc.karamja_dungeon_stepping_stone_end")
        lanes(
            lane(tile(2682, 9548, 0)) {
                stoneHops(
                    way(tile(2682, 9548, 0), tile(2690, 9547, 0)),
                    listOf(tile(2684, 9548, 0), tile(2688, 9547, 0)),
                )
            },
            lane(tile(2697, 9525, 0)) {
                stoneHops(
                    way(tile(2697, 9525, 0), tile(2695, 9533, 0)),
                    listOf(tile(2696, 9527, 0), tile(2695, 9531, 0)),
                )
            },
        )
    }

    row("dbrow.agility_shortcut_great_conch_stones") {
        shortcut(level = 61, xp = 0, "loc.great_conch_stepping_stone")
        lanes(
            lane(tile(3206, 2395, 0)) {
                stoneHops(
                    way(tile(3206, 2395, 0), tile(3212, 2395, 0)),
                    listOf(tile(3209, 2395, 0)),
                )
            },
            lane(tile(3212, 2395, 0)) {
                stoneHops(
                    way(tile(3212, 2395, 0), tile(3206, 2395, 0)),
                    listOf(tile(3209, 2395, 0)),
                )
            },
        )
    }

    row("dbrow.agility_shortcut_necropolis_stones") {
        shortcut(
            level = 62,
            xp = 0,
            "loc.necropolis_stepping_stone_1",
            "loc.necropolis_stepping_stone_2",
        )
        lanes(
            lane(tile(3289, 2700, 0)) {
                necropolisHops(
                    way(tile(3289, 2700, 0), tile(3295, 2700, 0)),
                    listOf(tile(3291, 2700, 0), tile(3293, 2700, 0)),
                )
            },
            lane(tile(3295, 2700, 0)) {
                necropolisHops(
                    way(tile(3295, 2700, 0), tile(3289, 2700, 0)),
                    listOf(tile(3293, 2700, 0), tile(3291, 2700, 0)),
                )
            },
            lane(tile(3295, 2704, 0)) {
                necropolisHops(
                    way(tile(3295, 2704, 0), tile(3295, 2708, 0)),
                    listOf(tile(3295, 2706, 0)),
                )
            },
            lane(tile(3295, 2708, 0)) {
                necropolisHops(
                    way(tile(3295, 2708, 0), tile(3295, 2704, 0)),
                    listOf(tile(3295, 2706, 0)),
                )
            },
        )
    }

    row("dbrow.agility_shortcut_stalker_den_stones") {
        shortcut(level = 64, xp = 0, "loc.stalker_den_stepping_stone")
        quest("dbrow.quest_shadowsofcustodia")
        lanes(
            lane(tile(1308, 9782, 0)) {
                stoneHops(
                    way(tile(1308, 9782, 0), tile(1309, 9786, 0)),
                    listOf(tile(1308, 9784, 0)),
                )
            },
            lane(tile(1309, 9786, 0)) {
                stoneHops(
                    way(tile(1309, 9786, 0), tile(1308, 9782, 0)),
                    listOf(tile(1308, 9784, 0)),
                )
            },
        )
    }

    row("dbrow.agility_shortcut_pollnivneach_stone") {
        shortcut(level = 71, xp = 250, "loc.polli_stepping_stone")
        lanes(
            lane(tile(3372, 2959, 0)) {
                stoneHops(
                    way(tile(3372, 2959, 0), tile(3373, 2955, 0)),
                    listOf(tile(3373, 2957, 0)),
                )
            },
            lane(tile(3373, 2955, 0)) {
                stoneHops(
                    way(tile(3373, 2955, 0), tile(3372, 2959, 0)),
                    listOf(tile(3373, 2957, 0)),
                )
            },
        )
    }

    row("dbrow.agility_shortcut_chaos_temple_stones") {
        shortcut(level = 72, xp = 0, "loc.wilderness_chaos_temple_shortcut")
        lanes(
            lane(tile(3268, 3629, 0)) {
                stoneHops(
                    way(tile(3268, 3629, 0), tile(3268, 3625, 0)),
                    listOf(tile(3268, 3627, 0)),
                )
            },
            lane(tile(3268, 3625, 0)) {
                stoneHops(
                    way(tile(3268, 3625, 0), tile(3268, 3629, 0)),
                    listOf(tile(3268, 3627, 0)),
                )
            },
        )
    }

    row("dbrow.agility_shortcut_lava_dragon_isle_stones") {
        shortcut(level = 74, xp = 0, "loc.wilderness_lava_dragons_shortcut")
        lanes(
            lane(tile(3201, 3807, 0)) {
                stoneHops(
                    way(tile(3201, 3807, 0), tile(3201, 3810, 0)),
                    listOf(tile(3201, 3808, 0)),
                )
            },
            lane(tile(3201, 3810, 0)) {
                stoneHops(
                    way(tile(3201, 3810, 0), tile(3201, 3807, 0)),
                    listOf(tile(3201, 3808, 0)),
                )
            },
        )
    }

    row("dbrow.agility_shortcut_zul_andra_stones") {
        shortcut(level = 76, xp = 0, "loc.snakeboss_steppingstone")
        quest("dbrow.quest_regicide")
        lanes(
            lane(tile(2154, 3072, 0)) {
                stoneHops(
                    way(tile(2154, 3072, 0), tile(2160, 3072, 0)),
                    listOf(tile(2157, 3072, 0)),
                )
            },
            lane(tile(2160, 3072, 0)) {
                stoneHops(
                    way(tile(2160, 3072, 0), tile(2154, 3072, 0)),
                    listOf(tile(2157, 3072, 0)),
                )
            },
        )
    }

    row("dbrow.agility_shortcut_sophanem_stones") {
        shortcut(level = 79, xp = 50, "loc.sophanem_agility_stone_river")
        lanes(
            lane(tile(3263, 2817, 0)) {
                stoneHops(
                    way(tile(3263, 2817, 0), tile(3271, 2817, 0)),
                    listOf(tile(3265, 2817, 0), tile(3269, 2817, 0)),
                )
            },
            lane(tile(3271, 2817, 0)) {
                stoneHops(
                    way(tile(3271, 2817, 0), tile(3263, 2817, 0)),
                    listOf(tile(3269, 2817, 0), tile(3265, 2817, 0)),
                )
            },
        )
    }

    row("dbrow.agility_shortcut_lava_maze_stones") {
        shortcut(level = 82, xp = 0, "loc.wilderness_lava_maze_northern_shortcut")
        lanes(
            lane(tile(3091, 3882, 0)) {
                stoneHops(
                    way(tile(3091, 3882, 0), tile(3093, 3879, 0)),
                    listOf(tile(3092, 3880, 0)),
                )
            },
            lane(tile(3093, 3879, 0)) {
                stoneHops(
                    way(tile(3093, 3879, 0), tile(3091, 3882, 0)),
                    listOf(tile(3092, 3880, 0)),
                )
            },
        )
    }

    row("dbrow.agility_shortcut_mos_le_harmless_east_stones") {
        shortcut(level = 82, xp = 0, "loc.mos_le_harmless_step_1", "loc.mos_le_harmless_step_2")
        quest("dbrow.quest_cabinfever")
        lanes(
            lane(tile(3810, 3048, 0)) {
                stoneHops(
                    way(tile(3810, 3048, 0), tile(3811, 3056, 0)),
                    listOf(tile(3810, 3050, 0), tile(3810, 3055, 0)),
                )
            },
            lane(tile(3811, 3056, 0)) {
                stoneHops(
                    way(tile(3811, 3056, 0), tile(3810, 3048, 0)),
                    listOf(tile(3810, 3055, 0), tile(3810, 3050, 0)),
                )
            },
        )
    }

    row("dbrow.agility_shortcut_brimhaven_dungeon_logs") {
        // UNVERIFIED: the Karamja log's and the Mort Myre bridge's levels and gates; they share
        // this row and pay their own xp. The Karamja log fails live, which is not built.
        shortcut(
            level = 1,
            xp = 0,
            "loc.karam_dungeon_bamboo_logbalance3",
            "loc.karam_dungeon_bamboo_logbalance1",
            "loc.zq_logbalance",
            "loc.druidjump_loc",
        )
        val logs = both(tile(2687, 9506, 0), tile(2682, 9506, 0))
        lanes(
            *logs.map { way -> lane(way.from) { logBalance(way) } }.toTypedArray(),
            *KARAMJA_LOG_WAYS.map { way ->
                    lane(way.from) {
                        logBalance(way)
                        xp(KARAMJA_LOG_XP)
                    }
                }
                .toTypedArray(),
            *MORT_MYRE_BRIDGE_WAYS.map { way ->
                    lane(way.from) {
                        bridgeJump(way)
                        xp(MORT_MYRE_BRIDGE_XP)
                    }
                }
                .toTypedArray(),
        )
        // UNVERIFIED: a curve through the 4 falls in 9 captured bridge jumps at levels 10 to 17.
        val bridgeStart = logs.size + KARAMJA_LOG_WAYS.size
        failsOn(
            110,
            255,
            FallDamage(3, 3),
            MORT_MYRE_BRIDGE_WAYS.withIndex().associate { (index, way) ->
                bridgeStart + index to lane(way.from) { bridgeFall(way) }
            },
        )
    }

    row("dbrow.agility_shortcut_coal_trucks_log") {
        shortcut(level = 20, xp = 85, "loc.mine_log_balance1")
        ways(
            listOf(
                way(tile(2603, 3477, 0), tile(2598, 3477, 0)),
                way(tile(2598, 3477, 0), tile(2603, 3477, 0)),
                way(tile(2599, 3478, 0), tile(2603, 3477, 0)),
                way(tile(2602, 3478, 0), tile(2598, 3477, 0)),
                way(tile(2602, 3476, 0), tile(2598, 3477, 0)),
                way(tile(2599, 3476, 0), tile(2603, 3477, 0)),
            )
        ) {
            logBalance(it)
        }
    }

    row("dbrow.agility_shortcut_ardougne_log") {
        shortcut(
            level = 33,
            xp = 40,
            "loc.ardougne_log_balance_right_sc",
            "loc.ardougne_log_balance_left_sc",
        )
        ways(
            listOf(
                way(tile(2602, 3336, 0), tile(2598, 3336, 0)),
                way(tile(2598, 3336, 0), tile(2602, 3336, 0)),
                way(tile(2599, 3337, 0), tile(2602, 3336, 0)),
            )
        ) {
            logBalance(it)
        }
        fails(
            90,
            250,
            FallDamage(2, 6),
            lane(tile(2602, 3336, 0)) { slip(tile(2602, 3336, 0), 20) },
            lane(tile(2598, 3336, 0)) { slip(tile(2598, 3336, 0), 20) },
            lane(tile(2599, 3337, 0)) { slip(tile(2599, 3337, 0), 20) },
        )
    }

    row("dbrow.agility_shortcut_tal_teklan_log") {
        shortcut(level = 40, xp = 0, "loc.tlati_north_river_log_balance_1")
        ways(
            listOf(
                way(tile(1283, 3147, 0), tile(1283, 3138, 0)),
                way(tile(1283, 3138, 0), tile(1283, 3147, 0)),
            )
        ) {
            logBalance(it)
        }
    }

    row("dbrow.agility_shortcut_auburn_valley_logs") {
        shortcut(level = 45, xp = 195, "loc.av_balance_2", "loc.av_balance_1")
        ways(
            listOf(
                way(tile(1453, 3336, 0), tile(1453, 3329, 0)),
                way(tile(1453, 3329, 0), tile(1453, 3336, 0)),
                way(tile(1401, 3291, 0), tile(1401, 3283, 0)),
                way(tile(1401, 3283, 0), tile(1401, 3291, 0)),
            )
        ) {
            logBalance(it)
        }
    }

    row("dbrow.agility_shortcut_isafdar_logs") {
        shortcut(
            level = 45,
            xp = 0,
            "loc.regicide_logbalance1_start",
            "loc.regicide_logbalance2_start",
            "loc.regicide_logbalance3_start",
        )
        ways(
            listOf(
                way(tile(2202, 3237, 0), tile(2196, 3237, 0)),
                way(tile(2196, 3237, 0), tile(2202, 3237, 0)),
                way(tile(2258, 3250, 0), tile(2264, 3250, 0)),
                way(tile(2264, 3250, 0), tile(2258, 3250, 0)),
                way(tile(2290, 3232, 0), tile(2290, 3239, 0)),
                way(tile(2290, 3239, 0), tile(2290, 3232, 0)),
            )
        ) {
            logBalance(it)
        }
    }

    row("dbrow.agility_shortcut_sinclair_mansion_log") {
        shortcut(
            level = 48,
            xp = 40,
            "loc.slayer_river_sc_logbalance_3",
            "loc.slayer_river_sc_logbalance_1",
        )
        ways(
            listOf(
                way(tile(2722, 3592, 0), tile(2722, 3596, 0)),
                way(tile(2722, 3596, 0), tile(2722, 3592, 0)),
            )
        ) {
            logBalance(it)
        }
        fails(
            140,
            255,
            FallDamage(1, 3),
            lane(tile(2722, 3592, 0)) { slip(tile(2722, 3592, 0), 20) },
            lane(tile(2722, 3596, 0)) { slip(tile(2722, 3596, 0), 20) },
        )
    }

    row("dbrow.agility_shortcut_isafdar_tripwire") {
        shortcut(level = 1, xp = 0, "loc.regicide_trap_tripwire")
        ways(
            listOf(
                way(tile(2220, 3155, 0), tile(2220, 3152, 0)),
                way(tile(2220, 3152, 0), tile(2220, 3155, 0)),
                way(tile(2215, 3156, 0), tile(2215, 3153, 0)),
                way(tile(2215, 3153, 0), tile(2215, 3156, 0)),
                way(tile(2250, 3168, 0), tile(2253, 3168, 0)),
                way(tile(2253, 3168, 0), tile(2250, 3168, 0)),
                way(tile(2284, 3188, 0), tile(2287, 3188, 0)),
                way(tile(2287, 3188, 0), tile(2284, 3188, 0)),
                way(tile(2294, 3242, 0), tile(2294, 3245, 0)),
                way(tile(2294, 3245, 0), tile(2294, 3242, 0)),
            )
        ) {
            hopOver(it)
        }
    }

    row("dbrow.agility_shortcut_isafdar_leaves") {
        shortcut(level = 1, xp = 0, "loc.regicide_pitfall_side")
        ways(
            listOf(
                way(tile(2209, 3201, 0), tile(2209, 3205, 0)),
                way(tile(2209, 3205, 0), tile(2209, 3201, 0)),
                way(tile(2275, 3262, 0), tile(2279, 3262, 0)),
                way(tile(2279, 3262, 0), tile(2275, 3262, 0)),
                way(tile(2267, 3205, 0), tile(2267, 3201, 0)),
                way(tile(2267, 3201, 0), tile(2267, 3205, 0)),
                way(tile(2274, 3176, 0), tile(2274, 3172, 0)),
                way(tile(2274, 3172, 0), tile(2274, 3176, 0)),
            )
        ) {
            hopOver(it)
        }
    }

    row("dbrow.agility_shortcut_kharazi_jagged_wall") {
        shortcut(level = 50, xp = 0, "loc.crumbled_wall")
        quest("dbrow.quest_legends")
        ways(
            listOf(
                way(tile(2790, 9295, 0), tile(2789, 9296, 0)),
                way(tile(2789, 9296, 0), tile(2790, 9295, 0)),
            )
        ) {
            hopOver(it)
        }
    }

    row("dbrow.agility_shortcut_rellekka_broken_fence") {
        shortcut(level = 57, xp = 0, "loc.viking_pike_defence_broken")
        ways(
            listOf(
                way(tile(2688, 3697, 0), tile(2691, 3697, 0)),
                way(tile(2691, 3697, 0), tile(2688, 3697, 0)),
            )
        ) {
            runUpHurdle(it)
        }
    }

    row("dbrow.agility_shortcut_forthos_strange_floor") {
        shortcut(level = 63, xp = 100, "loc.hosdun_agility_shortcut")
        ways(
            listOf(
                way(tile(1820, 9947, 0), tile(1820, 9945, 0)),
                way(tile(1820, 9945, 0), tile(1820, 9947, 0)),
            )
        ) {
            hopOver(it)
        }
    }

    row("dbrow.agility_shortcut_barrows_wall") {
        shortcut(level = 72, xp = 250, "loc.barrows_drystonewall_shortcut")
        ways(
            listOf(
                way(tile(3545, 3282, 0), tile(3547, 3282, 0)),
                way(tile(3546, 3282, 0), tile(3544, 3282, 0)),
            )
        ) {
            wallHurdle(it)
        }
    }

    row("dbrow.agility_shortcut_chasm_of_fire_gap") {
        // UNVERIFIED: the platform edge's xp; it pays as the gaps do.
        shortcut(level = 73, xp = 25, "loc.cof_agility_jump", "loc.cof_agility_lip")
        val gaps =
            listOf(
                way(tile(1448, 10097, 1), tile(1453, 10097, 1)),
                way(tile(1453, 10097, 1), tile(1448, 10097, 1)),
                way(tile(1453, 10097, 1), tile(1456, 10097, 1)),
                way(tile(1456, 10097, 1), tile(1453, 10097, 1)),
            )
        lanes(
            *gaps.map { way -> lane(way.from) { hopOver(way) } }.toTypedArray(),
            lane(tile(1438, 10097, 2)) {
                lipDrop(edge = tile(1438, 10098, 2), dest = tile(1438, 10098, 1))
            },
        )
    }

    row("dbrow.agility_shortcut_taverley_strange_floor") {
        shortcut(level = 80, xp = 125, "loc.taverly_dungeon_floor_spikes_sc")
        ways(
            listOf(
                way(tile(2880, 9813, 0), tile(2877, 9813, 0)),
                way(tile(2878, 9813, 0), tile(2881, 9813, 0)),
            )
        ) {
            hopOver(it)
        }
        fails(
            0,
            220,
            FallDamage(1, 3),
            lane(tile(2880, 9813, 0)) { slip(tile(2880, 9813, 0), 0) },
            lane(tile(2878, 9813, 0)) { slip(tile(2878, 9813, 0), 0) },
        )
    }

    row("dbrow.agility_shortcut_darkmeyer_rocks") {
        shortcut(level = 86, xp = 0, "loc.darkm_wall_rock_shortcut")
        varbitGates(Triple("varbit.myq5", 138, Int.MAX_VALUE))
        ways(
            listOf(
                way(tile(3565, 3379, 0), tile(3562, 3379, 0)),
            )
        ) {
            hopOver(it)
        }
    }

    row("dbrow.agility_shortcut_darkmeyer_inner_rocks") {
        shortcut(level = 86, xp = 0, "loc.hh_master005", op = 3)
        varbitGates(Triple("varbit.myq5", 138, Int.MAX_VALUE))
        ways(
            listOf(
                way(tile(3562, 3379, 0), tile(3565, 3379, 0)),
            )
        ) {
            hopOver(it)
        }
    }

    row("dbrow.agility_shortcut_edgeville_dungeon_monkeybars") {
        shortcut(level = 15, xp = 0, "loc.monkeybars_end1")
        ways(
            listOf(
                way(tile(3120, 9963, 0), tile(3120, 9970, 0)),
                way(tile(3120, 9970, 0), tile(3120, 9963, 0)),
            )
        ) {
            // UNVERIFIED: longer bars than the captured Yanille ones, with their sounds.
            monkeybars(it)
        }
    }

    row("dbrow.agility_shortcut_yanille_dungeon_monkeybars") {
        shortcut(level = 57, xp = 200, "loc.monkeybars_end2")
        ways(
            listOf(
                way(tile(2598, 9489, 0), tile(2598, 9494, 0)),
                way(tile(2598, 9494, 0), tile(2598, 9489, 0)),
                way(tile(2599, 9489, 0), tile(2599, 9494, 0)),
                way(tile(2599, 9494, 0), tile(2599, 9489, 0)),
            )
        ) {
            monkeybars(it)
        }
    }
}

private const val GROTTO_SHORE = "You leap to the shore."

/**
 * Captured on the Necropolis stones: told on setting off, which reads "stone" where only one stone
 * lies between the banks, and told again the tick after landing.
 */
private fun LaneBuilder.necropolisHops(way: Way, stones: List<CoordGrid>) {
    val noun = if (stones.size == 1) "stone" else "stones"
    message("You attempt to cross using the stepping $noun...")
    stoneHops(way, stones, EVEN_HOPS)
    next()
    message("You successfully make it to the other side.")
}
