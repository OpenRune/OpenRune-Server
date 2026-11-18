package org.rsmod.content.skills.agility.pack.shortcut

import dev.openrune.definition.dbtables.DBRowBuilder
import dev.openrune.definition.dbtables.DBTableBuilder
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.REQUIREMENT_WORN
import org.rsmod.content.skills.agility.pack.FallDamage
import org.rsmod.content.skills.agility.pack.fails
import org.rsmod.content.skills.agility.pack.failsOn
import org.rsmod.content.skills.agility.pack.lane
import org.rsmod.content.skills.agility.pack.lanes
import org.rsmod.content.skills.agility.pack.requirement
import org.rsmod.content.skills.agility.pack.tile

/*
 * Agility shortcuts climbed: rocks, slopes, walls, trellises, chains and railings. Their tiles
 * are the map's and each plays its type's template from the shortcut DSL. The Karamja, Troll
 * Stronghold, Death Plateau, Trollheim, Aldarin, Ice Mountain, Shilo, Waterbirth, Morytania and
 * Fossil Island rocks, the Auburn Valley, Falador and Burgh de Rott walls, the Slayer Tower and
 * Chasm of Fire chains and the Ectofuntus wall are captured; every other row is UNVERIFIED for
 * live timing, and so is which way a rock face is climbed up where the locs do not say.
 */
internal fun DBTableBuilder.shortcutClimbs() {
    row("dbrow.agility_shortcut_yanille_climbing_rocks") {
        shortcut(level = 5, xp = 250, "loc.watchshortcut")
        ways(
            listOf(
                way(tile(2561, 3108, 0), tile(2561, 3111, 0)),
            )
        ) {
            climbUp(it)
        }
    }

    row("dbrow.agility_shortcut_corsair_cove_rocks") {
        shortcut(level = 10, xp = 10, "loc.ds2_corsair_cove_shortcut")
        ways(
            listOf(
                way(tile(2546, 2873, 0), tile(2546, 2871, 0)),
                way(tile(2546, 2871, 0), tile(2546, 2873, 0)),
            )
        ) {
            climbDown(it)
        }
    }

    row("dbrow.agility_shortcut_death_plateau_rocks") {
        shortcut(level = 15, xp = 0, "loc.troll_climbingrocks")
        requirement(REQUIREMENT_WORN, "obj.death_climbingboots", "obj.climbing_boots_g")
        ways(DEATH_PLATEAU_WAYS) { walkOverRock(it) }
        // UNVERIFIED: a curve through the 3 slips in 21 captured climbs at levels 61 and 66.
        fails(
            180,
            240,
            FallDamage(3, 3),
            *DEATH_PLATEAU_WAYS.map { way -> lane(way.from) { slipOffRock(way) } }.toTypedArray(),
        )
    }

    row("dbrow.agility_shortcut_karamja_rocks") {
        shortcut(level = 15, xp = 10, "loc.zqclimbingrocks")
        val up = (2978..2980).map { z -> way(tile(2795, z, 0), tile(2791, z, 0)) }
        val down = (2978..2980).map { z -> way(tile(2791, z, 0), tile(2795, z, 0)) }
        lanes(
            *up.map { way -> lane(way.from) { climbUp(way) } }.toTypedArray(),
            *down.map { way -> lane(way.from) { climbDown(way, LONG_ROCK) } }.toTypedArray(),
        )
        // UNVERIFIED: a curve through the 3 falls in 11 captured climbs at levels 39 and 40.
        failsOn(
            140,
            255,
            FallDamage(2, 4),
            up.withIndex().associate { (index, way) ->
                index to lane(way.from) { climbFall(way, 2, 50, 2, 4) }
            },
        )
    }

    row("dbrow.agility_shortcut_hosidius_snake_rock") {
        shortcut(level = 1, xp = 0, "loc.hosidiusquest_rock_snake")
        ways(
            listOf(
                way(tile(1686, 9755, 0), tile(1688, 9755, 0)),
            )
        ) {
            climbDown(it)
        }
    }

    row("dbrow.agility_shortcut_eagles_peak_rocks") {
        shortcut(level = 25, xp = 10, "loc.ep_climbing_rocks01")
        ways(
            listOf(
                way(tile(2322, 3502, 0), tile(2324, 3497, 0)),
                way(tile(2324, 3497, 0), tile(2322, 3502, 0)),
            )
        ) {
            climbDown(it)
        }
    }

    row("dbrow.agility_shortcut_mount_karuulm_lower_rocks") {
        shortcut(level = 29, xp = 0, "loc.mount_karuulm_shortcut_rocks_low")
        ways(
            listOf(
                way(tile(1324, 3777, 0), tile(1324, 3785, 0)),
                way(tile(1324, 3785, 0), tile(1324, 3777, 0)),
            )
        ) {
            climbDown(it)
        }
    }

    row("dbrow.agility_shortcut_mount_karuulm_upper_rocks") {
        shortcut(level = 62, xp = 0, "loc.mount_karuulm_shortcut_rocks")
        ways(
            listOf(
                way(tile(1324, 3787, 0), tile(1324, 3795, 0)),
                way(tile(1324, 3795, 0), tile(1324, 3787, 0)),
            )
        ) {
            climbDown(it)
        }
    }

    row("dbrow.agility_shortcut_corsair_resource_rocks") {
        shortcut(
            level = 30,
            xp = 20,
            "loc.ds2_corsair_shortcut_top",
            "loc.ds2_corsair_shortcut_bottom",
        )
        quest("dbrow.quest_dragonslayer1")
        lanes(
            lane(tile(2485, 2898, 0)) { climbDown(way(tile(2485, 2898, 0), tile(2489, 2898, 0))) },
            lane(tile(2489, 2898, 0)) { climbUp(way(tile(2489, 2898, 0), tile(2485, 2898, 0))) },
        )
    }

    row("dbrow.agility_shortcut_gnome_stronghold_rocks") {
        shortcut(
            level = 37,
            xp = 0,
            "loc.gnome_stronghold_sc_rock_top",
            "loc.gnome_stronghold_sc_rock_bottom",
        )
        quest("dbrow.quest_grandtree")
        lanes(
            lane(tile(2486, 3515, 0)) { climbDown(way(tile(2486, 3515, 0), tile(2489, 3521, 0))) },
            lane(tile(2489, 3521, 0)) { climbUp(way(tile(2489, 3521, 0), tile(2486, 3515, 0))) },
        )
    }

    row("dbrow.agility_shortcut_trollheim_rocks_1") {
        shortcut(level = 41, xp = 80, "loc.asc_troll_mountain_climbrock_1")
        trollheimRocks(
            down = listOf(way(tile(2872, 3671, 0), tile(2869, 3671, 0))),
            up = listOf(way(tile(2869, 3671, 0), tile(2872, 3671, 0))),
            climb = TROLLHEIM_ROCK,
            loops = 3,
        )
    }

    row("dbrow.agility_shortcut_trollheim_rocks_2") {
        shortcut(level = 43, xp = 80, "loc.asc_troll_mountain_climbrock_2")
        trollheimRocks(
            down =
                listOf(
                    way(tile(2878, 3668, 0), tile(2878, 3665, 0)),
                    way(tile(2879, 3667, 0), tile(2878, 3665, 0)),
                ),
            up =
                listOf(
                    way(tile(2878, 3665, 0), tile(2878, 3668, 0)),
                    way(tile(2877, 3666, 0), tile(2878, 3668, 0)),
                ),
            climb = TROLLHEIM_LOW_ROCK,
            loops = 3,
        )
    }

    row("dbrow.agility_shortcut_trollheim_rocks_3") {
        // UNVERIFIED: which way is climbed up; both ways glide.
        shortcut(level = 44, xp = 80, "loc.asc_troll_mountain_climbrock_3")
        ways(
            listOf(
                way(tile(2907, 3682, 0), tile(2909, 3684, 0)),
                way(tile(2909, 3684, 0), tile(2907, 3682, 0)),
            )
        ) {
            climbDown(it)
        }
    }

    row("dbrow.agility_shortcut_trollheim_rocks_4") {
        shortcut(level = 47, xp = 80, "loc.asc_troll_mountain_climbrock_4")
        trollheimRocks(
            down =
                listOf(
                    way(tile(2900, 3680, 0), tile(2903, 3680, 0)),
                    way(tile(2901, 3679, 0), tile(2903, 3680, 0)),
                ),
            up =
                listOf(
                    way(tile(2903, 3680, 0), tile(2900, 3680, 0)),
                    way(tile(2902, 3681, 0), tile(2900, 3680, 0)),
                ),
            climb = TROLLHEIM_ROCK,
            loops = 4,
        )
    }

    row("dbrow.agility_shortcut_troll_stronghold_rocks") {
        shortcut(
            level = 43,
            xp = 10,
            "loc.troll_mountain_shortcut_climbingrocks1",
            "loc.troll_mountain_shortcut_climbingrocks2",
        )
        ways(
            listOf(
                way(tile(2886, 3683, 0), tile(2884, 3683, 0)),
                way(tile(2884, 3683, 0), tile(2886, 3683, 0)),
                way(tile(2886, 3684, 0), tile(2884, 3684, 0)),
                way(tile(2884, 3684, 0), tile(2886, 3684, 0)),
            )
        ) {
            climbDown(it, SHORT_ROCK)
        }
    }

    row("dbrow.agility_shortcut_auburn_valley_rocks") {
        shortcut(level = 41, xp = 95, "loc.av_scramble_1")
        ways(
            listOf(
                way(tile(1391, 3323, 0), tile(1396, 3323, 0)),
                way(tile(1396, 3323, 0), tile(1391, 3323, 0)),
            )
        ) {
            climbDown(it)
        }
    }

    row("dbrow.agility_shortcut_tal_teklan_rocks") {
        shortcut(level = 43, xp = 0, "loc.tlati_tree_area_shortcut_top")
        ways(
            listOf(
                way(tile(1270, 3002, 0), tile(1274, 3002, 0)),
                way(tile(1274, 3002, 0), tile(1270, 3002, 0)),
            )
        ) {
            climbDown(it)
        }
    }

    row("dbrow.agility_shortcut_proudspire_dark_rocks") {
        shortcut(level = 45, xp = 0, "loc.proudspire_climbing_rocks_dark01_op")
        ways(
            listOf(
                way(tile(1591, 3258, 0), tile(1586, 3262, 0)),
                way(tile(1590, 3257, 0), tile(1586, 3262, 0)),
                way(tile(1587, 3262, 0), tile(1591, 3257, 0)),
                way(tile(1586, 3261, 0), tile(1591, 3257, 0)),
            )
        ) {
            climbDown(it)
        }
    }

    row("dbrow.agility_shortcut_ralos_rise_rocks") {
        shortcut(level = 47, xp = 0, "loc.ralos_rise_sc")
        ways(
            listOf(
                way(tile(1465, 3128, 0), tile(1455, 3128, 0)),
                way(tile(1455, 3128, 0), tile(1465, 3128, 0)),
            )
        ) {
            climbDown(it)
        }
    }

    row("dbrow.agility_shortcut_great_conch_town_rocks") {
        shortcut(
            level = 50,
            xp = 0,
            "loc.great_conch_cliff_shortcut_town_bottom",
            "loc.great_conch_cliff_shortcut_town_top",
        )
        lanes(
            lane(tile(3180, 2431, 0)) { climbUp(way(tile(3180, 2431, 0), tile(3180, 2435, 0))) },
            lane(tile(3180, 2435, 0)) { climbDown(way(tile(3180, 2435, 0), tile(3180, 2431, 0))) },
        )
    }

    row("dbrow.agility_shortcut_arceuus_mid_grey_rocks") {
        shortcut(level = 52, xp = 0, "loc.archeuus_runestone_shortcut_midgrey_bottom")
        ways(
            listOf(
                way(tile(1769, 3849, 0), tile(1774, 3849, 0)),
                way(tile(1774, 3849, 0), tile(1769, 3849, 0)),
            )
        ) {
            climbDown(it)
        }
    }

    row("dbrow.agility_shortcut_great_conch_east_rocks") {
        shortcut(
            level = 52,
            xp = 0,
            "loc.great_conch_cliff_shortcut_east_1_top",
            "loc.great_conch_cliff_shortcut_east_1_bottom",
        )
        lanes(
            lane(tile(3235, 2390, 0)) { climbDown(way(tile(3235, 2390, 0), tile(3235, 2386, 0))) },
            lane(tile(3235, 2386, 0)) { climbUp(way(tile(3235, 2386, 0), tile(3235, 2390, 0))) },
        )
    }

    row("dbrow.agility_shortcut_aldarin_rocks") {
        shortcut(
            level = 54,
            xp = 0,
            "loc.aldarin_cliff_shortcut_bottom",
            "loc.aldarin_cliff_shortcut_top",
        )
        lanes(
            lane(tile(1343, 2917, 0)) {
                climbDown(way(tile(1343, 2917, 0), tile(1338, 2917, 0)), CLIFF_DOWN)
            },
            lane(tile(1338, 2917, 0)) {
                climbDown(way(tile(1338, 2917, 0), tile(1343, 2917, 0)), CLIFF_UP)
            },
        )
    }

    row("dbrow.agility_shortcut_giants_plateau_rocks") {
        shortcut(level = 54, xp = 75, "loc.gh_climbing_rocks_top", "loc.gh_climbing_rocks_bottom")
        lanes(
            lane(tile(3391, 3155, 0)) { climbDown(way(tile(3391, 3155, 0), tile(3396, 3155, 0))) },
            lane(tile(3396, 3155, 0)) { climbUp(way(tile(3396, 3155, 0), tile(3391, 3155, 0))) },
        )
    }

    row("dbrow.agility_shortcut_great_conch_south_east_rocks") {
        shortcut(
            level = 55,
            xp = 0,
            "loc.great_conch_cliff_shortcut_southeast_top",
            "loc.great_conch_cliff_shortcut_southeast_bottom",
        )
        lanes(
            lane(tile(3272, 2333, 0)) { climbDown(way(tile(3272, 2333, 0), tile(3272, 2327, 0))) },
            lane(tile(3272, 2327, 0)) { climbUp(way(tile(3272, 2327, 0), tile(3272, 2333, 0))) },
        )
    }

    row("dbrow.agility_shortcut_great_conch_east_upper_rocks") {
        shortcut(
            level = 57,
            xp = 0,
            "loc.great_conch_cliff_shortcut_east_2_top",
            "loc.great_conch_cliff_shortcut_east_2_bottom",
        )
        lanes(
            lane(tile(3254, 2398, 0)) { climbDown(way(tile(3254, 2398, 0), tile(3260, 2398, 0))) },
            lane(tile(3260, 2398, 0)) { climbUp(way(tile(3260, 2398, 0), tile(3254, 2398, 0))) },
        )
    }

    row("dbrow.agility_shortcut_elven_overpass_rocks_1") {
        shortcut(
            level = 59,
            xp = 0,
            "loc.elves_overpass_sc_rocks_bottom",
            "loc.elves_overpass_sc_rocks_top",
        )
        ways(
            listOf(
                way(tile(2346, 3300, 0), tile(2344, 3294, 0)),
                way(tile(2344, 3294, 0), tile(2346, 3300, 0)),
            )
        ) {
            climbDown(it)
        }
    }

    row("dbrow.agility_shortcut_elven_overpass_rocks_2") {
        shortcut(
            level = 68,
            xp = 0,
            "loc.elves_overpass_sc_rocks_bottom",
            "loc.elves_overpass_sc_rocks_top",
        )
        ways(
            listOf(
                way(tile(2338, 3286, 0), tile(2338, 3281, 0)),
                way(tile(2338, 3281, 0), tile(2338, 3286, 0)),
            )
        ) {
            climbDown(it)
        }
    }

    row("dbrow.agility_shortcut_elven_overpass_rocks_3") {
        shortcut(
            level = 85,
            xp = 0,
            "loc.elves_overpass_sc_rocks_bottom",
            "loc.elves_overpass_sc_rocks_top",
        )
        ways(
            listOf(
                way(tile(2338, 3253, 0), tile(2332, 3252, 0)),
                way(tile(2332, 3252, 0), tile(2338, 3253, 0)),
            )
        ) {
            climbDown(it)
        }
    }

    row("dbrow.agility_shortcut_god_wars_handholds") {
        shortcut(
            level = 60,
            xp = 0,
            "loc.godwars_climbing_rocks_up",
            "loc.godwars_climbing_rocks_down",
        )
        lanes(
            lane(tile(2927, 3758, 0)) { climbUp(way(tile(2927, 3758, 0), tile(2927, 3761, 0))) },
            lane(tile(2928, 3757, 0)) { climbUp(way(tile(2928, 3757, 0), tile(2927, 3761, 0))) },
            lane(tile(2929, 3758, 0)) { climbUp(way(tile(2929, 3758, 0), tile(2927, 3761, 0))) },
            lane(tile(2927, 3761, 0)) { climbDown(way(tile(2927, 3761, 0), tile(2928, 3757, 0))) },
        )
    }

    row("dbrow.agility_shortcut_trollheim_wilderness_rocks") {
        shortcut(level = 64, xp = 0, "loc.trollheim_wildy_climb_rocks")
        ways(
            listOf(
                way(tile(2915, 3672, 0), tile(2918, 3672, 0)),
                way(tile(2918, 3672, 0), tile(2915, 3672, 0)),
                way(tile(2921, 3672, 0), tile(2924, 3673, 0)),
                way(tile(2924, 3673, 0), tile(2921, 3672, 0)),
                way(tile(2946, 3678, 0), tile(2950, 3681, 0)),
            )
        ) {
            climbDown(it)
        }
    }

    row("dbrow.agility_shortcut_morytania_rocks") {
        shortcut(
            level = 65,
            xp = 0,
            "loc.morytania_climbingrocks_sc_top",
            "loc.morytania_climbingrocks_sc_bottom",
        )
        lanes(
            lane(tile(3424, 3476, 0)) {
                climbDownSteps(
                    listOf(tile(3425, 3476, 0), tile(3426, 3476, 0), tile(3427, 3477, 0))
                )
            },
            lane(tile(3427, 3477, 0)) {
                climbUp(way(tile(3427, 3477, 0), tile(3424, 3476, 0)), loops = 5)
            },
        )
    }

    row("dbrow.agility_shortcut_taverley_rocks") {
        shortcut(level = 66, xp = 250, "loc.tavelryshortcut")
        ways(
            listOf(
                way(tile(2946, 3439, 0), tile(2943, 3439, 0)),
            )
        ) {
            climbUp(it)
        }
    }

    row("dbrow.agility_shortcut_ice_mountain_rocks") {
        shortcut(
            level = 68,
            xp = 0,
            "loc.ice_mountain_shortcut_bottom",
            "loc.ice_mountain_shortcut_top",
        )
        lanes(
            lane(tile(2997, 3483, 0)) {
                climbDown(way(tile(2997, 3483, 0), tile(3002, 3483, 0)), CLIFF_UP)
            },
            lane(tile(3002, 3483, 0)) {
                climbDown(way(tile(3002, 3483, 0), tile(2997, 3483, 0)), CLIFF_DOWN)
            },
        )
    }

    row("dbrow.agility_shortcut_white_wolf_mountain_rocks") {
        shortcut(level = 68, xp = 0, "loc.hilltop_shortcut_bottom", "loc.hilltop_shortcut_top")
        lanes(
            lane(tile(2866, 3428, 0)) { climbUp(way(tile(2866, 3428, 0), tile(2869, 3428, 0))) },
            lane(tile(2869, 3428, 0)) { climbDown(way(tile(2869, 3428, 0), tile(2866, 3428, 0))) },
        )
    }

    row("dbrow.agility_shortcut_arceuus_grey_north_rocks") {
        shortcut(level = 69, xp = 0, "loc.archeuus_runestone_shortcut_grey_shortcut_north")
        ways(
            listOf(
                way(tile(1761, 3874, 0), tile(1761, 3872, 0)),
                way(tile(1761, 3872, 0), tile(1761, 3874, 0)),
            )
        ) {
            climbDown(it)
        }
    }

    row("dbrow.agility_shortcut_proudspire_grey_rocks") {
        shortcut(level = 71, xp = 0, "loc.proudspire_climbing_rocks_grey03_op")
        ways(
            listOf(
                way(tile(1581, 3249, 0), tile(1576, 3254, 0)),
                way(tile(1576, 3254, 0), tile(1581, 3249, 0)),
            )
        ) {
            climbDown(it)
        }
    }

    row("dbrow.agility_shortcut_gu_tanoth_wall") {
        shortcut(level = 71, xp = 0, "loc.gutanoth_wall_shortcut", "loc.gutanoth_rock_shortcut")
        quest("dbrow.quest_watchtower")
        lanes(
            lane(tile(2545, 3031, 0)) { climbUp(way(tile(2545, 3031, 0), tile(2549, 3031, 0))) },
            lane(tile(2549, 3031, 0)) { climbDown(way(tile(2549, 3031, 0), tile(2545, 3031, 0))) },
        )
    }

    row("dbrow.agility_shortcut_arceuus_grey_rocks") {
        shortcut(level = 73, xp = 0, "loc.archeuus_runestone_shortcut_grey_top")
        ways(
            listOf(
                way(tile(1742, 3854, 0), tile(1752, 3854, 0)),
            )
        ) {
            climbDown(it)
        }
    }

    row("dbrow.agility_shortcut_trollheim_diary_rocks") {
        shortcut(level = 73, xp = 0, "loc.diary_troll_climbingrocks")
        ways(
            listOf(
                way(tile(2838, 3693, 0), tile(2844, 3693, 0)),
                way(tile(2844, 3693, 0), tile(2838, 3693, 0)),
                way(tile(2843, 3692, 0), tile(2838, 3693, 0)),
            )
        ) {
            climbDown(it)
        }
    }

    row("dbrow.agility_shortcut_shilo_village_rocks") {
        shortcut(
            level = 79,
            xp = 0,
            "loc.shortcut_shilo_rocks_bottom",
            "loc.shortcut_shilo_rocks_top",
        )
        lanes(
            lane(tile(2871, 3008, 0)) {
                climbUp(way(tile(2871, 3008, 0), tile(2871, 3003, 0)), loops = 4)
            },
            lane(tile(2871, 3003, 0)) {
                // UNVERIFIED: live glides 2871,3004 to 3007 and steps the last tile.
                climbDown(way(tile(2871, 3003, 0), tile(2871, 3008, 0)), SHILO_ROCK)
            },
        )
    }

    row("dbrow.agility_shortcut_kharazi_vine") {
        shortcut(
            level = 79,
            xp = 0,
            "loc.kharazi_shortcut_vine_diag1_live",
            "loc.kharazi_shortcut_vine_end_live",
        )
        quest("dbrow.quest_legends")
        ways(
            listOf(
                way(tile(2899, 2937, 0), tile(2899, 2942, 0)),
                way(tile(2899, 2942, 0), tile(2899, 2937, 0)),
            )
        ) {
            climbDown(it)
        }
    }

    row("dbrow.agility_shortcut_pollnivneach_slope") {
        shortcut(
            level = 83,
            xp = 40,
            "loc.pollnivneach_scramble_up",
            "loc.pollnivneach_scramble_down",
        )
        lanes(
            lane(tile(3348, 2981, 0)) { climbUp(way(tile(3348, 2981, 0), tile(3339, 2981, 0))) },
            lane(tile(3339, 2981, 0)) { climbDown(way(tile(3339, 2981, 0), tile(3348, 2981, 0))) },
        )
    }

    row("dbrow.agility_shortcut_crandor_rocks") {
        shortcut(level = 84, xp = 0, "loc.crandor_shortcut_bottom", "loc.crandor_shortcut_top")
        lanes(
            lane(tile(2832, 3247, 0)) { climbUp(way(tile(2832, 3247, 0), tile(2832, 3252, 0))) },
            lane(tile(2832, 3252, 0)) { climbDown(way(tile(2832, 3252, 0), tile(2832, 3247, 0))) },
        )
    }

    row("dbrow.agility_shortcut_waterbirth_rocks") {
        shortcut(
            level = 85,
            xp = 0,
            "loc.dagannoth_waterbirth_rock_climb_agility_shortcut_bottom",
            "loc.dagannoth_waterbirth_rock_climb_agility_shortcut_top",
        )
        lanes(
            lane(tile(2547, 3749, 0)) {
                // UNVERIFIED: live is seen two tiles a tick on the way up.
                climbUp(way(tile(2547, 3749, 0), tile(2547, 3744, 0)), loops = 4)
            },
            lane(tile(2547, 3744, 0)) {
                climbDown(way(tile(2547, 3744, 0), tile(2547, 3749, 0)), WATERBIRTH_ROCK)
            },
        )
    }

    row("dbrow.agility_shortcut_falador_crumbling_wall") {
        // UNVERIFIED: the Burgh de Rott inn wall's level and gates; it pays as Falador's does.
        shortcut(
            level = 5,
            xp = 5,
            "loc.fai_falador_castle_crumble_mid",
            "loc.burgh_inn_climb_over",
        )
        lanes(
            *both(tile(2936, 3355, 0), tile(2934, 3355, 0))
                .map { way -> lane(way.from) { climbOver(way, FALADOR_WALL) } }
                .toTypedArray(),
            *both(tile(3491, 3230, 0), tile(3491, 3231, 0))
                .map { way -> lane(way.from) { climbOver(way, BURGH_INN_WALL) } }
                .toTypedArray(),
        )
    }

    row("dbrow.agility_shortcut_mourning_temple_wall") {
        shortcut(level = 1, xp = 0, "loc.mourning_temple_wall_jump")
        quest("dbrow.quest_mourningsendpart2")
        ways(
            listOf(
                way(tile(1882, 4620, 1), tile(1884, 4620, 1)),
                way(tile(1884, 4620, 1), tile(1882, 4620, 1)),
                way(tile(1882, 4658, 1), tile(1884, 4658, 1)),
                way(tile(1884, 4658, 1), tile(1882, 4658, 1)),
            )
        ) {
            climbOver(it)
        }
    }

    row("dbrow.agility_shortcut_slayer_tower_window") {
        shortcut(level = 18, xp = 30, "loc.slayertower_window_shortcut_through")
        ways(
            listOf(
                way(tile(3442, 3531, 0), tile(3444, 3533, 0)),
                way(tile(3444, 3533, 0), tile(3442, 3531, 0)),
            )
        ) {
            climbOver(it)
        }
    }

    row("dbrow.agility_shortcut_auburn_valley_walls") {
        shortcut(level = 24, xp = 60, "loc.av_lowwall_climb_1", "loc.av_lowwall_climb_2")
        ways(
            listOf(
                way(tile(1369, 3294, 0), tile(1369, 3296, 0)),
                way(tile(1369, 3296, 0), tile(1369, 3294, 0)),
                way(tile(1387, 3303, 0), tile(1387, 3301, 0)),
                way(tile(1387, 3301, 0), tile(1387, 3303, 0)),
                way(tile(1389, 3309, 0), tile(1391, 3309, 0)),
                way(tile(1391, 3309, 0), tile(1389, 3309, 0)),
            )
        ) {
            climbOver(it)
        }
    }

    row("dbrow.agility_shortcut_varrock_trellis") {
        shortcut(level = 35, xp = 0, "loc.garden_trellis_concave_shortcut")
        quest("dbrow.quest_gardenoftranquillity")
        ways(
            listOf(
                way(tile(3228, 3470, 0), tile(3228, 3471, 0)),
            )
        ) {
            climbOver(it)
        }
    }

    row("dbrow.agility_shortcut_al_kharid_palace_wall") {
        shortcut(level = 70, xp = 0, "loc.kharid_poshwall_topless", "loc.kharid_bigwindow")
        quest("dbrow.quest_princealirescue")
        ways(
            listOf(
                way(tile(3295, 3157, 0), tile(3295, 3159, 0)),
                way(tile(3295, 3159, 0), tile(3295, 3157, 0)),
            )
        ) {
            climbOver(it)
        }
    }

    row("dbrow.agility_shortcut_yanille_watchtower_trellis") {
        shortcut(level = 18, xp = 0, "loc.qip_watchtower_trellis_base")
        ways(
            listOf(
                way(tile(2548, 3119, 0), tile(2548, 3117, 1)),
            )
        ) {
            climbToFloor(it)
        }
    }

    row("dbrow.agility_shortcut_slayer_tower_lower_chain") {
        shortcut(
            level = 61,
            xp = 30,
            "loc.slayertower_sc_chainbottom",
            "loc.slayertower_sc_chaintop",
        )
        ways(
            listOf(
                way(tile(3421, 3550, 0), tile(3421, 3550, 1)),
                way(tile(3421, 3550, 1), tile(3421, 3550, 0)),
                way(tile(3422, 3551, 0), tile(3422, 3551, 1)),
                way(tile(3422, 3551, 1), tile(3422, 3551, 0)),
                way(tile(3423, 3550, 0), tile(3423, 3550, 1)),
                way(tile(3423, 3550, 1), tile(3423, 3550, 0)),
                way(tile(3422, 3549, 0), tile(3422, 3549, 1)),
                way(tile(3422, 3549, 1), tile(3422, 3549, 0)),
            )
        ) {
            chainClimb(it)
        }
    }

    row("dbrow.agility_shortcut_slayer_tower_upper_chain") {
        shortcut(
            level = 71,
            xp = 30,
            "loc.slayertower_sc_chainbottom",
            "loc.slayertower_sc_chaintop",
        )
        ways(
            listOf(
                way(tile(3447, 3575, 1), tile(3447, 3575, 2)),
                way(tile(3447, 3575, 2), tile(3447, 3575, 1)),
                way(tile(3446, 3576, 1), tile(3446, 3576, 2)),
                way(tile(3446, 3576, 2), tile(3446, 3576, 1)),
                way(tile(3447, 3577, 1), tile(3447, 3577, 2)),
                way(tile(3447, 3577, 2), tile(3447, 3577, 1)),
                way(tile(3448, 3576, 1), tile(3448, 3576, 2)),
                way(tile(3448, 3576, 2), tile(3448, 3576, 1)),
            )
        ) {
            chainClimb(it)
        }
    }

    row("dbrow.agility_shortcut_chasm_of_fire_chain") {
        shortcut(level = 83, xp = 50, "loc.cof_shortcut_top")
        lanes(
            lane(tile(1435, 10075, 2)) {
                chainSlide(cage = tile(1436, 10075, 2), dest = tile(1437, 10074, 0))
            }
        )
    }

    row("dbrow.agility_shortcut_falador_rough_walls") {
        shortcut(level = 52, xp = 0, "loc.falador_wallclimb", "loc.falador_wallclimb2")
        ways(
            listOf(
                way(tile(3033, 3390, 0), tile(3033, 3389, 1)),
                way(tile(3032, 3388, 0), tile(3032, 3389, 1)),
            )
        ) {
            climbToFloor(it)
        }
    }

    row("dbrow.agility_shortcut_falador_wall_drops") {
        shortcut(
            level = 4,
            xp = 0,
            "loc.xbows_fai_falador_castle_walls_battlement",
            "loc.xbows_fai_falador_castle_walls_crenalation",
        )
        ways(
            listOf(
                way(tile(3033, 3389, 1), tile(3033, 3390, 0)),
                way(tile(3032, 3389, 1), tile(3032, 3388, 0)),
            )
        ) {
            dropDown(it)
        }
    }

    row("dbrow.agility_shortcut_yanille_rough_wall") {
        shortcut(level = 69, xp = 0, "loc.yanille_wallclimb")
        ways(
            listOf(
                way(tile(2556, 3072, 0), tile(2556, 3073, 1)),
                way(tile(2556, 3075, 0), tile(2556, 3074, 1)),
            )
        ) {
            climbToFloor(it)
        }
    }

    row("dbrow.agility_shortcut_yanille_wall_drop") {
        shortcut(level = 4, xp = 0, "loc.xbows_yanille_castlewall_battlement")
        ways(
            listOf(
                way(tile(2556, 3073, 1), tile(2556, 3072, 0)),
                way(tile(2556, 3074, 1), tile(2556, 3075, 0)),
            )
        ) {
            dropDown(it)
        }
    }

    row("dbrow.agility_shortcut_ectofuntus_weathered_wall") {
        shortcut(level = 58, xp = 0, "loc.ectopool_sc_raildown", "loc.ectopool_sc_wallclimb")
        lanes(
            lane(tile(3670, 9888, 3)) { dropDown(way(tile(3670, 9888, 3), tile(3671, 9888, 2))) },
            lane(tile(3671, 9888, 2)) {
                wallJumpUp(way(tile(3671, 9888, 2), tile(3670, 9888, 3)))
            },
        )
    }

    row("dbrow.agility_shortcut_taverley_dragon_rocks") {
        shortcut(level = 70, xp = 0, "loc.taverley_dragon_jumpup", "loc.taverley_dragon_jumpdown")
        lanes(
            lane(tile(2886, 9823, 0)) {
                climbToFloor(way(tile(2886, 9823, 0), tile(2888, 9823, 1)))
            },
            lane(tile(2888, 9823, 1)) { dropDown(way(tile(2888, 9823, 1), tile(2886, 9823, 0))) },
        )
    }

    row("dbrow.agility_shortcut_arceuus_library_railings_1") {
        shortcut(
            level = 52,
            xp = 30,
            "loc.archeuus_library_railing_05",
            "loc.archeuus_library_railing_06",
            op = 2,
        )
        ways(
            listOf(
                way(tile(1627, 3804, 1), tile(1629, 3804, 0)),
                way(tile(1627, 3808, 1), tile(1629, 3808, 0)),
                way(tile(1638, 3806, 1), tile(1636, 3806, 0)),
                way(tile(1638, 3810, 1), tile(1636, 3810, 0)),
                way(tile(1631, 3802, 1), tile(1631, 3804, 0)),
                way(tile(1635, 3802, 1), tile(1635, 3804, 0)),
                way(tile(1629, 3813, 1), tile(1629, 3811, 0)),
                way(tile(1633, 3813, 1), tile(1633, 3811, 0)),
                way(tile(1627, 3806, 1), tile(1629, 3806, 0)),
                way(tile(1627, 3810, 1), tile(1629, 3810, 0)),
                way(tile(1638, 3804, 1), tile(1636, 3804, 0)),
                way(tile(1629, 3802, 1), tile(1629, 3804, 0)),
                way(tile(1633, 3802, 1), tile(1633, 3804, 0)),
                way(tile(1631, 3813, 1), tile(1631, 3811, 0)),
                way(tile(1635, 3813, 1), tile(1635, 3811, 0)),
            )
        ) {
            dropDown(it)
        }
    }

    row("dbrow.agility_shortcut_arceuus_library_railings_2") {
        shortcut(
            level = 62,
            xp = 40,
            "loc.archeuus_library_railing_05",
            "loc.archeuus_library_railing_06",
            op = 2,
        )
        ways(
            listOf(
                way(tile(1627, 3804, 2), tile(1629, 3804, 0)),
                way(tile(1627, 3808, 2), tile(1629, 3808, 0)),
                way(tile(1638, 3810, 2), tile(1636, 3810, 0)),
                way(tile(1631, 3802, 2), tile(1631, 3804, 0)),
                way(tile(1635, 3802, 2), tile(1635, 3804, 0)),
                way(tile(1629, 3813, 2), tile(1629, 3811, 0)),
                way(tile(1633, 3813, 2), tile(1633, 3811, 0)),
                way(tile(1627, 3806, 2), tile(1629, 3806, 0)),
                way(tile(1627, 3810, 2), tile(1629, 3810, 0)),
                way(tile(1638, 3808, 2), tile(1636, 3808, 0)),
                way(tile(1629, 3802, 2), tile(1629, 3804, 0)),
                way(tile(1633, 3802, 2), tile(1633, 3804, 0)),
                way(tile(1631, 3813, 2), tile(1631, 3811, 0)),
                way(tile(1635, 3813, 2), tile(1635, 3811, 0)),
            )
        ) {
            dropDown(it)
        }
    }

    row("dbrow.agility_shortcut_fossil_island_volcano_rope") {
        shortcut(
            level = 64,
            xp = 0,
            "loc.fossil_volcano_agility_rope_bottom",
            "loc.fossil_volcano_agility_rope_top",
        )
        lanes(
            lane(tile(3778, 3821, 0)) {
                climbUp(way(tile(3778, 3821, 0), tile(3784, 3821, 0)), loops = 6, soundDelay = 10)
            },
            lane(tile(3784, 3821, 0)) {
                climbDown(way(tile(3784, 3821, 0), tile(3778, 3821, 0)), FOSSIL_ROPE)
            },
        )
    }
}

private val DEATH_PLATEAU_WAYS =
    both(tile(2820, 3635, 0), tile(2822, 3635, 0)) +
        both(tile(2857, 3611, 0), tile(2857, 3613, 0)) +
        both(tile(2856, 3611, 0), tile(2856, 3613, 0)) +
        both(tile(2833, 3627, 0), tile(2833, 3629, 0)) +
        both(tile(2834, 3627, 0), tile(2834, 3629, 0)) +
        both(tile(2909, 3686, 0), tile(2911, 3686, 0)) +
        both(tile(2909, 3687, 0), tile(2911, 3687, 0))

/**
 * Captured on the four Trollheim rocks: glided [down] and walked [up] with [loops] of the climbing
 * sound; the way up can fail, sliding the player back down a tick into it.
 */
private fun DBRowBuilder.trollheimRocks(
    down: List<Way>,
    up: List<Way>,
    climb: RockClimb,
    loops: Int,
) {
    lanes(
        *down.map { way -> lane(way.from) { climbDown(way, climb) } }.toTypedArray(),
        *up.map { way -> lane(way.from) { climbUp(way, loops) } }.toTypedArray(),
    )
    // UNVERIFIED: a curve through the 8 falls in 27 captured climbs up at level 61.
    failsOn(
        60,
        255,
        FallDamage(4, 5),
        up.withIndex().associate { (index, way) ->
            down.size + index to lane(way.from) { climbFall(way, 1, 120, 3, 40, loops) }
        },
    )
}
