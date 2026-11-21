package org.rsmod.content.skills.agility.pack.shortcut

import dev.openrune.definition.dbtables.DBTableBuilder
import org.rsmod.content.skills.agility.pack.FACE_EAST
import org.rsmod.content.skills.agility.pack.FACE_NORTH
import org.rsmod.content.skills.agility.pack.FACE_SOUTH
import org.rsmod.content.skills.agility.pack.FACE_WEST
import org.rsmod.content.skills.agility.pack.FallDamage
import org.rsmod.content.skills.agility.pack.fails
import org.rsmod.content.skills.agility.pack.lane
import org.rsmod.content.skills.agility.pack.lanes
import org.rsmod.content.skills.agility.pack.tile

private const val CRACK_SQUEEZE = "You squeeze through the narrow tunnel."
private const val YANILLE_PIPE_XP = 75

/*
 * Agility shortcuts squeezed or crawled through: cracks, crevices, pipes, railings and tunnels.
 * The Catacombs of Kourend crack, the Barbarian Outpost, Yanille, Edgeville and Taverley pipes,
 * the Varrock and Yanille wall tunnels, the Royal Titans tunnel, the Zanaris jutting wall, the
 * Canifis and Mudskipper Point railings, the Yanille dungeon rubble and the Fossil Island hole are
 * captured; every other row is UNVERIFIED for live timing. Only the Zanaris jutting walls and the
 * Taverley pipe can fail.
 */
internal fun DBTableBuilder.shortcutSqueezes() {
    row("dbrow.agility_shortcut_isafdar_sticks") {
        shortcut(level = 1, xp = 0, "loc.regicide_trap_woodspring")
        ways(
            listOf(
                way(tile(2199, 3169, 0), tile(2202, 3169, 0)),
                way(tile(2202, 3169, 0), tile(2199, 3169, 0)),
                way(tile(2234, 3181, 0), tile(2238, 3181, 0)),
                way(tile(2238, 3181, 0), tile(2234, 3181, 0)),
                way(tile(2181, 3212, 0), tile(2181, 3208, 0)),
                way(tile(2181, 3208, 0), tile(2181, 3212, 0)),
                way(tile(2256, 3227, 0), tile(2260, 3227, 0)),
                way(tile(2260, 3227, 0), tile(2256, 3227, 0)),
                way(tile(2274, 3163, 0), tile(2277, 3163, 0)),
                way(tile(2277, 3163, 0), tile(2274, 3163, 0)),
                way(tile(2295, 3217, 0), tile(2295, 3213, 0)),
                way(tile(2295, 3213, 0), tile(2295, 3217, 0)),
            )
        ) {
            squeezeThrough(it)
        }
    }

    row("dbrow.agility_shortcut_isafdar_dense_forest_camp_south") {
        shortcut(level = 56, xp = 0, "loc.regicide_cross_over1_tyras_camp")
        ways(
            listOf(
                way(tile(2188, 3162, 0), tile(2188, 3165, 0)),
                way(tile(2188, 3165, 0), tile(2188, 3162, 0)),
            )
        ) {
            squeezeThrough(it)
        }
    }

    row("dbrow.agility_shortcut_isafdar_dense_forest_3") {
        shortcut(level = 56, xp = 0, "loc.regicide_cross_over3")
        ways(
            listOf(
                way(tile(2188, 3165, 0), tile(2188, 3168, 0)),
                way(tile(2188, 3168, 0), tile(2188, 3165, 0)),
                way(tile(2217, 3169, 0), tile(2217, 3166, 0)),
                way(tile(2217, 3166, 0), tile(2217, 3169, 0)),
                way(tile(2217, 3163, 0), tile(2217, 3160, 0)),
                way(tile(2217, 3160, 0), tile(2217, 3163, 0)),
                way(tile(2237, 3149, 0), tile(2240, 3149, 0)),
                way(tile(2240, 3149, 0), tile(2237, 3149, 0)),
                way(tile(2229, 3219, 0), tile(2232, 3219, 0)),
                way(tile(2232, 3219, 0), tile(2229, 3219, 0)),
                way(tile(2235, 3219, 0), tile(2238, 3219, 0)),
                way(tile(2238, 3219, 0), tile(2235, 3219, 0)),
                way(tile(2233, 3249, 0), tile(2236, 3249, 0)),
                way(tile(2236, 3249, 0), tile(2233, 3249, 0)),
                way(tile(2279, 3231, 0), tile(2279, 3228, 0)),
                way(tile(2279, 3228, 0), tile(2279, 3231, 0)),
                way(tile(2268, 3192, 0), tile(2271, 3192, 0)),
                way(tile(2271, 3192, 0), tile(2268, 3192, 0)),
                way(tile(2303, 3219, 0), tile(2303, 3222, 0)),
                way(tile(2303, 3222, 0), tile(2303, 3219, 0)),
            )
        ) {
            squeezeThrough(it)
        }
    }

    row("dbrow.agility_shortcut_isafdar_dense_forest_camp_north") {
        shortcut(level = 56, xp = 0, "loc.regicide_cross_over2_tyras_camp")
        ways(
            listOf(
                way(tile(2188, 3168, 0), tile(2188, 3171, 0)),
                way(tile(2188, 3171, 0), tile(2188, 3168, 0)),
            )
        ) {
            squeezeThrough(it)
        }
    }

    row("dbrow.agility_shortcut_isafdar_dense_forest_2") {
        shortcut(level = 56, xp = 0, "loc.regicide_cross_over2")
        ways(
            listOf(
                way(tile(2217, 3166, 0), tile(2217, 3163, 0)),
                way(tile(2217, 3163, 0), tile(2217, 3166, 0)),
                way(tile(2234, 3149, 0), tile(2237, 3149, 0)),
                way(tile(2237, 3149, 0), tile(2234, 3149, 0)),
                way(tile(2226, 3219, 0), tile(2229, 3219, 0)),
                way(tile(2229, 3219, 0), tile(2226, 3219, 0)),
                way(tile(2230, 3249, 0), tile(2233, 3249, 0)),
                way(tile(2233, 3249, 0), tile(2230, 3249, 0)),
                way(tile(2279, 3225, 0), tile(2279, 3222, 0)),
                way(tile(2279, 3222, 0), tile(2279, 3225, 0)),
                way(tile(2265, 3192, 0), tile(2268, 3192, 0)),
                way(tile(2268, 3192, 0), tile(2265, 3192, 0)),
                way(tile(2303, 3216, 0), tile(2303, 3219, 0)),
                way(tile(2303, 3219, 0), tile(2303, 3216, 0)),
                way(tile(2303, 3222, 0), tile(2303, 3225, 0)),
                way(tile(2303, 3225, 0), tile(2303, 3222, 0)),
            )
        ) {
            squeezeThrough(it)
        }
    }

    row("dbrow.agility_shortcut_isafdar_dense_forest_1") {
        shortcut(level = 56, xp = 0, "loc.regicide_cross_over1")
        ways(
            listOf(
                way(tile(2231, 3149, 0), tile(2234, 3149, 0)),
                way(tile(2234, 3149, 0), tile(2231, 3149, 0)),
                way(tile(2232, 3219, 0), tile(2235, 3219, 0)),
                way(tile(2235, 3219, 0), tile(2232, 3219, 0)),
                way(tile(2236, 3249, 0), tile(2239, 3249, 0)),
                way(tile(2239, 3249, 0), tile(2236, 3249, 0)),
                way(tile(2279, 3228, 0), tile(2279, 3225, 0)),
                way(tile(2279, 3225, 0), tile(2279, 3228, 0)),
                way(tile(2271, 3192, 0), tile(2274, 3192, 0)),
                way(tile(2274, 3192, 0), tile(2271, 3192, 0)),
                way(tile(2303, 3213, 0), tile(2303, 3216, 0)),
                way(tile(2303, 3216, 0), tile(2303, 3213, 0)),
            )
        ) {
            squeezeThrough(it)
        }
    }

    row("dbrow.agility_shortcut_brimhaven_dungeon_lower_pipe") {
        shortcut(level = 34, xp = 100, "loc.karam_dungeon_pipe")
        ways(
            listOf(
                way(tile(2698, 9492, 0), tile(2698, 9500, 0)),
            )
        ) {
            pipe(it)
        }
    }

    row("dbrow.agility_shortcut_brimhaven_dungeon_lower_pipe_back") {
        shortcut(level = 1, xp = 100, "loc.karam_dungeon_pipe")
        ways(
            listOf(
                way(tile(2698, 9500, 0), tile(2698, 9492, 0)),
            )
        ) {
            pipe(it)
        }
    }

    row("dbrow.agility_shortcut_brimhaven_dungeon_entrance_pipe") {
        shortcut(level = 22, xp = 85, "loc.karam_dungeon_pipe2")
        ways(
            listOf(
                way(tile(2655, 9566, 0), tile(2655, 9573, 0)),
            )
        ) {
            pipe(it)
        }
    }

    row("dbrow.agility_shortcut_brimhaven_dungeon_entrance_pipe_back") {
        shortcut(level = 1, xp = 85, "loc.karam_dungeon_pipe2")
        ways(
            listOf(
                way(tile(2655, 9573, 0), tile(2655, 9566, 0)),
            )
        ) {
            pipe(it)
        }
    }

    row("dbrow.agility_shortcut_brimhaven_dragon_tunnel") {
        shortcut(level = 1, xp = 0, "loc.brimhaven_slayer_dragon_tunnel")
        ways(
            listOf(
                way(tile(2697, 9436, 0), tile(2684, 9436, 0)),
                way(tile(2684, 9436, 0), tile(2697, 9436, 0)),
            )
        ) {
            // UNVERIFIED: crawled as the captured cave tunnels, without their messages.
            longCrawl(it, angleTo(it.from, it.to))
        }
    }

    row("dbrow.agility_shortcut_lost_tribe_hole") {
        shortcut(level = 13, xp = 0, "loc.lost_tribe_cavewall_hole_walldecor")
        varbitGates(Triple("varbit.lost_tribe_quest", 4, Int.MAX_VALUE))
        ways(
            listOf(
                way(tile(3219, 9618, 0), tile(3221, 9618, 0)),
                way(tile(3221, 9618, 0), tile(3219, 9618, 0)),
            )
        ) {
            squeezeThrough(it)
        }
    }

    row("dbrow.agility_shortcut_yanille_wall_tunnel") {
        shortcut(level = 16, xp = 0, "loc.yanille_castlewall_sc", "loc.yanille_castlehole_sc")
        ways(
            listOf(
                way(tile(2575, 3107, 0), tile(2575, 3112, 0)),
                way(tile(2575, 3112, 0), tile(2575, 3107, 0)),
            )
        ) {
            tunnelWall(it)
        }
    }

    row("dbrow.agility_shortcut_catacombs_cracks") {
        shortcut(level = 17, xp = 0, "loc.zeah_cata_crack")
        lanes(
            lane(tile(1706, 10078, 0)) {
                squeezeThrough(way(tile(1706, 10078, 0), tile(1716, 10056, 0)), CRACK_SQUEEZE)
            },
            lane(tile(1716, 10056, 0)) {
                squeezeThrough(way(tile(1716, 10056, 0), tile(1706, 10078, 0)), CRACK_SQUEEZE)
            },
            lane(tile(1648, 10009, 0)) {
                squeezeThrough(way(tile(1648, 10009, 0), tile(1646, 10000, 0)), CRACK_SQUEEZE)
            },
            lane(tile(1646, 10000, 0)) {
                squeezeThrough(way(tile(1646, 10000, 0), tile(1648, 10009, 0)), CRACK_SQUEEZE)
            },
        )
    }

    row("dbrow.agility_shortcut_hosidius_quest_crevices") {
        shortcut(level = 18, xp = 0, "loc.hosidiusquest_crackin", "loc.hosidiusquest_crackout")
        ways(
            listOf(
                way(tile(1711, 9824, 0), tile(1711, 9820, 0)),
                way(tile(1711, 9820, 0), tile(1711, 9824, 0)),
            )
        ) {
            squeezeThrough(it)
        }
    }

    row("dbrow.agility_shortcut_varrock_underwall_tunnel") {
        shortcut(level = 21, xp = 0, "loc.varrock_sc_tunnel_east", "loc.varrock_sc_tunnel_west")
        ways(
            listOf(
                way(tile(3142, 3513, 0), tile(3137, 3516, 0)),
                way(tile(3137, 3516, 0), tile(3142, 3513, 0)),
            )
        ) {
            tunnelWall(it)
        }
    }

    row("dbrow.agility_shortcut_auburn_valley_tunnel") {
        shortcut(level = 33, xp = 75, "loc.av_tunnel_1")
        ways(
            listOf(
                way(tile(1368, 3327, 0), tile(1368, 3323, 0)),
                way(tile(1368, 3323, 0), tile(1368, 3327, 0)),
            )
        ) {
            // UNVERIFIED: crawled as the captured wall tunnels.
            tunnelWall(it)
        }
    }

    row("dbrow.agility_shortcut_barbarian_outpost_pipe") {
        shortcut(level = 35, xp = 100, "loc.agility_obstical_pipe_barbarian")
        ways(
            listOf(
                way(tile(2552, 3561, 0), tile(2552, 3558, 0)),
                way(tile(2552, 3558, 0), tile(2552, 3561, 0)),
            )
        ) {
            pipe(it)
        }
    }

    row("dbrow.agility_shortcut_yanille_dungeon_pipe") {
        // The Yanille dungeon ledge shares the pipe's level; each pays its own xp.
        shortcut(level = 40, xp = 0, "loc.obstical_pipe4", "loc.balancing_ledge3")
        lanes(
            *both(tile(2578, 9506, 0), tile(2572, 9506, 0))
                .map { way ->
                    lane(way.from) {
                        pipe(way)
                        xp(YANILLE_PIPE_XP)
                    }
                }
                .toTypedArray(),
            *YANILLE_LEDGE_WAYS.map { way ->
                    lane(way.from) {
                        ledge(way, leftFoot = way.to.z > way.from.z)
                        xp(YANILLE_LEDGE_XP)
                    }
                }
                .toTypedArray(),
        )
    }

    row("dbrow.agility_shortcut_zanaris_jutting_wall_1") {
        shortcut(level = 46, xp = 99, "loc.fairy_sc_juttingwall")
        ways(
            listOf(
                way(tile(2400, 4402, 0), tile(2400, 4404, 0)),
                way(tile(2400, 4404, 0), tile(2400, 4402, 0)),
            )
        ) {
            sidestepSqueeze(it)
        }
        fails(
            50,
            254,
            FallDamage(1, 3),
            lane(tile(2400, 4402, 0)) { scrape(way(tile(2400, 4402, 0), tile(2400, 4404, 0)), 60) },
            lane(tile(2400, 4404, 0)) { scrape(way(tile(2400, 4404, 0), tile(2400, 4402, 0)), 60) },
        )
    }

    row("dbrow.agility_shortcut_zanaris_jutting_wall_2") {
        shortcut(level = 66, xp = 99, "loc.fairy_sc_juttingwall")
        ways(
            listOf(
                way(tile(2409, 4400, 0), tile(2409, 4402, 0)),
                way(tile(2409, 4402, 0), tile(2409, 4400, 0)),
            )
        ) {
            sidestepSqueeze(it)
        }
        fails(
            50,
            254,
            FallDamage(1, 3),
            lane(tile(2409, 4400, 0)) { scrape(way(tile(2409, 4400, 0), tile(2409, 4402, 0)), 60) },
            lane(tile(2409, 4402, 0)) { scrape(way(tile(2409, 4402, 0), tile(2409, 4400, 0)), 60) },
        )
    }

    row("dbrow.agility_shortcut_draynor_manor_fence") {
        shortcut(level = 49, xp = 15, "loc.draynor_manor_fence_broken")
        ways(
            listOf(
                way(tile(3085, 3352, 0), tile(3086, 3352, 0)),
                way(tile(3086, 3352, 0), tile(3085, 3352, 0)),
            )
        ) {
            squeezeThrough(it)
        }
    }

    row("dbrow.agility_shortcut_edgeville_dungeon_pipe") {
        shortcut(level = 51, xp = 100, "loc.varrock_dungeon_pipe_sc")
        ways(
            listOf(
                way(tile(3149, 9906, 0), tile(3155, 9906, 0)),
                way(tile(3155, 9906, 0), tile(3149, 9906, 0)),
                way(tile(3150, 9907, 0), tile(3155, 9906, 0)),
                way(tile(3150, 9905, 0), tile(3155, 9906, 0)),
                way(tile(3154, 9907, 0), tile(3149, 9906, 0)),
                way(tile(3154, 9905, 0), tile(3149, 9906, 0)),
            )
        ) {
            pipe(it)
        }
    }

    row("dbrow.agility_shortcut_stalker_den_cave") {
        shortcut(
            level = 59,
            xp = 0,
            "loc.stalker_den_external_shortcut_inside",
            "loc.stalker_den_external_shortcut_outside",
        )
        quest("dbrow.quest_shadowsofcustodia")
        ways(
            listOf(
                way(tile(1324, 9768, 0), tile(1324, 3363, 0)),
                way(tile(1324, 3363, 0), tile(1324, 9768, 0)),
            )
        ) {
            squeezeThrough(it)
        }
    }

    row("dbrow.agility_shortcut_royal_titans_tunnel") {
        shortcut(
            level = 60,
            xp = 0,
            "loc.cavewall_shortcut_royal_titans_east",
            "loc.cavewall_shortcut_royal_titans_west",
        )
        val west = (9546..9548).map { z -> way(tile(2991, z, 0), tile(2969, 9551, 0)) }
        val east = (2968..2970).map { x -> way(tile(x, 9550, 0), tile(2992, 9547, 0)) }
        lanes(
            *west.map { way ->
                    lane(way.from) {
                        longCrawl(way, FACE_WEST, SCRAMBLE, exitAngle = FACE_NORTH)
                    }
                }
                .toTypedArray(),
            *east.map { way ->
                    lane(way.from) {
                        longCrawl(way, FACE_SOUTH, SCRAMBLE, exitAngle = FACE_EAST)
                    }
                }
                .toTypedArray(),
        )
    }

    row("dbrow.agility_shortcut_obor_pipe") {
        shortcut(level = 60, xp = 100, "loc.varrock_dungeon_pipe_2_sc")
        ways(
            listOf(
                way(tile(3122, 9883, 0), tile(3122, 9880, 0)),
                way(tile(3122, 9880, 0), tile(3122, 9883, 0)),
            )
        ) {
            pipe(it)
        }
    }

    row("dbrow.agility_shortcut_fremennik_slayer_crevice") {
        shortcut(level = 61, xp = 0, "loc.slayer_dungeon_2_sc_wall_crack")
        ways(
            listOf(
                way(tile(2735, 10008, 0), tile(2730, 10008, 0)),
                way(tile(2730, 10008, 0), tile(2735, 10008, 0)),
            )
        ) {
            squeezeThrough(it)
        }
    }

    row("dbrow.agility_shortcut_mudskipper_loose_railing") {
        shortcut(level = 63, xp = 0, "loc.deepdungeonlooserailing")
        ways(
            listOf(
                way(tile(2936, 9810, 0), tile(2935, 9810, 0)),
                way(tile(2935, 9810, 0), tile(2936, 9810, 0)),
            )
        ) {
            tightFit(it)
        }
    }

    row("dbrow.agility_shortcut_canifis_railings") {
        shortcut(
            level = 65,
            xp = 0,
            "loc.morytania_railing_sc_fence_1",
            "loc.morytania_railing_sc_fence_2",
        )
        ways(
            listOf(
                way(tile(3423, 3476, 0), tile(3424, 3476, 0)),
                way(tile(3424, 3476, 0), tile(3423, 3476, 0)),
                way(tile(3425, 3483, 0), tile(3425, 3484, 0)),
                way(tile(3425, 3484, 0), tile(3425, 3483, 0)),
            )
        ) {
            railingSqueeze(it)
        }
    }

    row("dbrow.agility_shortcut_yanille_dungeon_rubble") {
        shortcut(level = 67, xp = 0, "loc.climbingcaverocks2", "loc.climbingcaverocks1")
        val down =
            listOf(
                tile(2615, 9503, 0),
                tile(2616, 9503, 0),
                tile(2614, 9504, 0),
                tile(2614, 9505, 0),
                tile(2615, 9506, 0),
                tile(2616, 9506, 0),
            )
        val up = listOf(tile(2616, 9571, 0), tile(2617, 9571, 0))
        lanes(
            *down.map { start ->
                    lane(start) { rubbleDown(way(start, tile(2616, 9571, 0))) }
                }
                .toTypedArray(),
            *up.map { start -> lane(start) { rubbleUp(way(start, tile(2615, 9506, 0)), 60) } }
                .toTypedArray(),
        )
    }

    row("dbrow.agility_shortcut_heroes_guild_crevice") {
        shortcut(
            level = 67,
            xp = 0,
            "loc.heroes_guild_shortcut_to_fountain",
            "loc.heroes_guild_shortcut_from_fountain",
        )
        varbitGates(Triple("varbit.falador_diary_hard_complete", 1, 1))
        ways(
            listOf(
                way(tile(2899, 9902, 0), tile(2915, 9894, 0)),
                way(tile(2898, 9901, 0), tile(2915, 9894, 0)),
                way(tile(2915, 9895, 0), tile(2898, 9902, 0)),
                way(tile(2914, 9894, 0), tile(2898, 9902, 0)),
            )
        ) {
            squeezeThrough(it)
        }
    }

    row("dbrow.agility_shortcut_taverley_pipe") {
        shortcut(level = 70, xp = 100, "loc.taverly_dungeon_pipe_sc")
        ways(
            listOf(
                way(tile(2886, 9799, 0), tile(2892, 9799, 0)),
                way(tile(2892, 9799, 0), tile(2886, 9799, 0)),
            )
        ) {
            pipe(it)
        }
        fails(
            0,
            254,
            FallDamage(1, 3),
            lane(tile(2886, 9799, 0)) { slip(tile(2886, 9799, 0), 0) },
            lane(tile(2892, 9799, 0)) { slip(tile(2892, 9799, 0), 0) },
        )
    }

    row("dbrow.agility_shortcut_kourend_slayer_tunnel") {
        shortcut(level = 72, xp = 0, "loc.slayer_cave_tunnel_right")
        ways(
            listOf(
                way(tile(2429, 9807, 0), tile(2435, 9807, 0)),
                way(tile(2429, 9806, 0), tile(2435, 9806, 0)),
            )
        ) {
            // UNVERIFIED: crawled as the captured cave tunnels, without their messages.
            longCrawl(it, FACE_EAST)
        }
    }

    row("dbrow.agility_shortcut_wyvern_cave_north_tunnel") {
        shortcut(level = 72, xp = 0, "loc.cavewall_shortcut_wyvern_north")
        ways(
            listOf(
                way(tile(3025, 9571, 0), tile(3035, 9557, 0)),
                way(tile(3026, 9571, 0), tile(3035, 9557, 0)),
                way(tile(3027, 9571, 0), tile(3035, 9557, 0)),
                way(tile(3034, 9558, 0), tile(3026, 9571, 0)),
                way(tile(3035, 9558, 0), tile(3026, 9571, 0)),
                way(tile(3036, 9558, 0), tile(3026, 9571, 0)),
            )
        ) {
            // UNVERIFIED: crawled as the Royal Titans tunnel in the same cave.
            longCrawl(it, if (it.to.z < it.from.z) FACE_SOUTH else FACE_NORTH, SCRAMBLE)
        }
    }

    row("dbrow.agility_shortcut_wilderness_slayer_crevice") {
        shortcut(level = 77, xp = 100, "loc.wilderness_slayer_cave_crevice")
        ways(
            listOf(
                way(tile(3334, 10119, 0), tile(3334, 10121, 0)),
                way(tile(3334, 10121, 0), tile(3334, 10119, 0)),
                way(tile(3342, 10149, 0), tile(3342, 10153, 0)),
                way(tile(3342, 10153, 0), tile(3342, 10149, 0)),
                way(tile(3434, 10091, 0), tile(3434, 10093, 0)),
                way(tile(3434, 10093, 0), tile(3434, 10091, 0)),
                way(tile(3435, 10112, 0), tile(3435, 10115, 0)),
                way(tile(3435, 10115, 0), tile(3435, 10112, 0)),
            )
        ) {
            squeezeThrough(it)
        }
    }

    row("dbrow.agility_shortcut_iorwerth_dungeon_north_gap") {
        shortcut(
            level = 78,
            xp = 10,
            "loc.prif_slayer_dungeon_shortcut_1b",
            "loc.prif_slayer_dungeon_shortcut_1a",
        )
        ways(
            listOf(
                way(tile(3216, 12441, 0), tile(3222, 12441, 0)),
                way(tile(3222, 12441, 0), tile(3216, 12441, 0)),
            )
        ) {
            squeezeThrough(it)
        }
    }

    row("dbrow.agility_shortcut_deepfin_crevice") {
        shortcut(level = 84, xp = 0, "loc.deepfin_cave_shortcut")
        ways(
            listOf(
                way(tile(2076, 9203, 0), tile(2082, 9203, 0)),
                way(tile(2082, 9203, 0), tile(2076, 9203, 0)),
            )
        ) {
            squeezeThrough(it)
        }
    }

    row("dbrow.agility_shortcut_iorwerth_dungeon_south_gap") {
        shortcut(
            level = 84,
            xp = 15,
            "loc.prif_slayer_dungeon_shortcut_2b",
            "loc.prif_slayer_dungeon_shortcut_2a",
        )
        ways(
            listOf(
                way(tile(3232, 12420, 0), tile(3242, 12420, 0)),
                way(tile(3242, 12420, 0), tile(3232, 12420, 0)),
            )
        ) {
            squeezeThrough(it)
        }
    }

    row("dbrow.agility_shortcut_kalphite_lair_crevice") {
        shortcut(level = 86, xp = 0, "loc.kalphite_wall_shortcut")
        ways(
            listOf(
                way(tile(3500, 9510, 2), tile(3506, 9505, 2)),
                way(tile(3506, 9505, 2), tile(3500, 9510, 2)),
            )
        ) {
            squeezeThrough(it)
        }
    }

    row("dbrow.agility_shortcut_legends_cave_crevice") {
        shortcut(level = 96, xp = 75, "loc.legends_quest_cave_shortcut")
        quest("dbrow.quest_legends")
        ways(
            listOf(
                way(tile(2770, 9340, 0), tile(2764, 9339, 0)),
                way(tile(2764, 9339, 0), tile(2770, 9340, 0)),
            )
        ) {
            squeezeThrough(it)
        }
    }

    row("dbrow.agility_shortcut_fossil_island_hole") {
        shortcut(
            level = 70,
            xp = 0,
            "loc.fossil_shortcut_basecamp_a",
            "loc.fossil_shortcut_basecamp_b",
        )
        ways(
            listOf(
                way(tile(3713, 3830, 0), tile(3715, 3815, 0)),
                way(tile(3714, 3830, 0), tile(3715, 3815, 0)),
                way(tile(3715, 3830, 0), tile(3715, 3815, 0)),
                way(tile(3713, 3816, 0), tile(3713, 3830, 0)),
                way(tile(3714, 3815, 0), tile(3713, 3830, 0)),
                way(tile(3716, 3815, 0), tile(3713, 3830, 0)),
                way(tile(3717, 3815, 0), tile(3713, 3830, 0)),
                way(tile(3718, 3816, 0), tile(3713, 3830, 0)),
            )
        ) {
            holeClimb(it)
        }
    }
}
