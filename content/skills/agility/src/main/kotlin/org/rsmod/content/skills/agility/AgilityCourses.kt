package org.rsmod.content.skills.agility

import org.rsmod.map.CoordGrid

internal const val STAT_AGILITY: String = "stat.agility"
internal const val MARK_OF_GRACE: String = "obj.grace"
internal const val VARP_COURSE: String = "varp.agility_course"
internal const val VARP_PROGRESS: String = "varp.agility_course_progress"
internal const val VARP_MARK_CLOCK: String = "varp.agility_mark_clock"

private const val CLIMB = "seq.human_reachforladder"
private const val CLIMB_UP = "seq.human_climbing"
private const val BALANCE = "seq.human_walk_logbalance_loop"
private const val SIDESTEP = "seq.human_into_sidestepl"
private const val SIDESTEP_2 = "seq.human_into_sidestep"
private const val JUMP_UP = "seq.agility_shortcut_wall_jump"
private const val JUMP_UP_2 = "seq.agility_shortcut_wall_jump2"
private const val JUMP_DOWN = "seq.agility_shortcut_wall_jumpdown"
private const val JUMP_DOWN_FLAT = "seq.agility_shortcut_wall_jumpdown_noreachforward"
private const val HURDLE = "seq.human_jump_hurdle"
private const val SPOT_JUMP = "seq.human_spot_jump"
private const val ROPE_SWING = "seq.human_ropeswing_long"
private const val ZIPLINE = "seq.zipline_slide"
private const val HANDHOLDS = "seq.agilityarena_handholds_middle"
private const val POLE_VAULT = "seq.rooftops_pole_vault"
private const val TREE_CLIMB = "seq.mdaughter_tree_climb_combi"
private const val PIPE = "seq.human_doublepipesqueeze"
private const val CLIMB_DOWN = "seq.human_climbing_down"
private const val CRUMBLE_WALL = "seq.human_walk_crumbledwall"
private const val STEPPING_STONE = "seq.human_steppingstonejump"

/**
 * Where an obstacle puts the player. [x], [z] and [level] are absolute and default to "unchanged";
 * the deltas are applied on top, which is how obstacles that only shift the player a few tiles from
 * wherever they are standing are expressed.
 */
data class Landing(
    val x: Int? = null,
    val z: Int? = null,
    val level: Int? = null,
    val dx: Int = 0,
    val dz: Int = 0,
    val dLevel: Int = 0,
) {
    fun resolve(from: CoordGrid): CoordGrid =
        CoordGrid(
            x = (x ?: from.x) + dx,
            z = (z ?: from.z) + dz,
            level = (level ?: from.level) + dLevel,
        )
}

data class Obstacle(
    val locs: List<String>,
    val landing: Landing,
    val xp: Double,
    val anim: String,
    val ticks: Int = 2,
    val slide: Boolean = false,
    val repeats: Int = 1,
)

/**
 * [markOdds] is the chance of a mark of grace on a completed lap once the shared three minute
 * cooldown has passed: two in six for most rooftops, two in five at Rellekka and two in three at
 * Canifis and Ardougne. [markPenalty] is the 80% cut that applies twenty levels above [level] -
 * Canifis is the one course where live never applies it.
 */
data class Course(
    val name: String,
    val level: Int,
    val lapXp: Double,
    val obstacles: List<Obstacle>,
    val markSpawns: List<CoordGrid> = emptyList(),
    val markOdds: Double = 0.0,
    val markPenalty: Boolean = true,
) {
    /**
     * One entry per obstacle crossing a lap needs, so an obstacle placed three times in a row -
     * the Barbarian Outpost walls all share one loc - is three steps against one obstacle index.
     */
    val steps: List<Int> = obstacles.flatMapIndexed { index, o -> List(o.repeats) { index } }
}

private fun at(x: Int, z: Int, level: Int) = Landing(x = x, z = z, level = level)

private fun obstacle(
    loc: String,
    landing: Landing,
    xp: Double,
    anim: String,
    ticks: Int = 2,
    slide: Boolean = false,
    repeats: Int = 1,
) = Obstacle(listOf(loc), landing, xp, anim, ticks, slide, repeats)

private fun marks(vararg coords: Triple<Int, Int, Int>) =
    coords.map { CoordGrid(it.first, it.second, it.third) }

object AgilityCourses {
    // ponytail: one stage per obstacle - animate, wait, land. Live obstacles play several linked
    // hops; those are cosmetic and the upgrade path is extra stages, not a different model.
    // Every xp value below is the wiki's, and each course's obstacles plus its lap bonus add up to
    // the wiki's total for a lap.
    val courses: List<Course> =
        listOf(
            Course(
                name = "Gnome Stronghold Agility Course",
                level = 1,
                lapXp = 50.0,
                obstacles =
                    listOf(
                        obstacle(
                            "loc.gnome_log_balance1",
                            at(2474, 3429, 0),
                            10.0,
                            BALANCE,
                            ticks = 4,
                            slide = true,
                        ),
                        obstacle("loc.obstical_net2", Landing(z = 3424, level = 1), 10.0, CLIMB),
                        obstacle("loc.climbing_branch", at(2473, 3420, 2), 6.5, CLIMB),
                        obstacle(
                            "loc.balancing_rope",
                            at(2483, 3420, 2),
                            10.0,
                            BALANCE,
                            ticks = 4,
                            slide = true,
                        ),
                        obstacle("loc.climbing_tree", at(2485, 3419, 0), 6.5, CLIMB),
                        obstacle("loc.obstical_net3", Landing(z = 3428, level = 0), 10.0, CLIMB),
                        Obstacle(
                            locs = listOf("loc.obstical_pipe3_1", "loc.obstical_pipe3_2"),
                            landing = Landing(dz = 7),
                            xp = 7.5,
                            anim = PIPE,
                            ticks = 6,
                            slide = true,
                        ),
                    ),
            ),
            Course(
                name = "Draynor Village Rooftop Course",
                level = 1,
                lapXp = 79.0,
                markOdds = 2.0 / 6,
                markSpawns =
                    marks(
                        Triple(3099, 3280, 3),
                        Triple(3089, 3274, 3),
                        Triple(3094, 3266, 3),
                        Triple(3088, 3259, 3),
                        Triple(3092, 3255, 3),
                        Triple(3099, 3257, 3),
                        Triple(3098, 3259, 3),
                    ),
                obstacles =
                    listOf(
                        obstacle(
                            "loc.rooftops_draynor_wallclimb",
                            at(3102, 3279, 3),
                            5.0,
                            CLIMB,
                            ticks = 3,
                        ),
                        obstacle(
                            "loc.rooftops_draynor_tightrope_1",
                            at(3090, 3276, 3),
                            8.0,
                            BALANCE,
                            ticks = 4,
                            slide = true,
                        ),
                        obstacle(
                            "loc.rooftops_draynor_tightrope_2",
                            at(3092, 3266, 3),
                            7.0,
                            BALANCE,
                            ticks = 6,
                            slide = true,
                        ),
                        obstacle(
                            "loc.rooftops_draynor_wallcrossing",
                            at(3088, 3261, 3),
                            7.0,
                            SIDESTEP,
                            ticks = 4,
                            slide = true,
                        ),
                        obstacle(
                            "loc.rooftops_draynor_wallscramble",
                            at(3088, 3255, 3),
                            10.0,
                            JUMP_UP,
                            ticks = 3,
                        ),
                        obstacle(
                            "loc.rooftops_draynor_leapdown",
                            at(3096, 3256, 3),
                            4.0,
                            JUMP_DOWN,
                        ),
                        obstacle(
                            "loc.rooftops_draynor_crate",
                            at(3103, 3261, 0),
                            0.0,
                            JUMP_DOWN,
                            ticks = 3,
                        ),
                    ),
            ),
            Course(
                name = "Al Kharid Rooftop Course",
                level = 20,
                lapXp = 36.0,
                markOdds = 2.0 / 6,
                markSpawns =
                    marks(
                        Triple(3276, 3188, 3),
                        Triple(3271, 3191, 3),
                        Triple(3273, 3182, 3),
                        Triple(3267, 3171, 3),
                        Triple(3271, 3170, 3),
                        Triple(3268, 3163, 3),
                        Triple(3266, 3166, 3),
                        Triple(3291, 3163, 3),
                        Triple(3297, 3168, 3),
                        Triple(3301, 3164, 3),
                        Triple(3316, 3161, 1),
                        Triple(3318, 3163, 1),
                        Triple(3315, 3176, 2),
                        Triple(3317, 3178, 2),
                        Triple(3315, 3183, 3),
                        Triple(3313, 3181, 3),
                        Triple(3302, 3189, 3),
                        Triple(3300, 3190, 3),
                    ),
                obstacles =
                    listOf(
                        obstacle(
                            "loc.rooftops_kharid_wallclimb",
                            at(3273, 3192, 3),
                            12.0,
                            CLIMB,
                            ticks = 3,
                        ),
                        obstacle(
                            "loc.rooftops_kharid_tightrope_1",
                            at(3272, 3172, 3),
                            36.0,
                            BALANCE,
                            ticks = 10,
                            slide = true,
                        ),
                        obstacle(
                            "loc.rooftops_kharid_rope_swing",
                            at(3284, 3166, 3),
                            48.0,
                            ROPE_SWING,
                            ticks = 3,
                            slide = true,
                        ),
                        obstacle(
                            "loc.rooftops_kharid_slide_side",
                            at(3315, 3163, 1),
                            48.0,
                            ZIPLINE,
                            ticks = 5,
                            slide = true,
                        ),
                        obstacle(
                            "loc.rooftops_kharid_bamboo_tree_top",
                            at(3317, 3174, 2),
                            12.0,
                            JUMP_UP,
                            ticks = 4,
                        ),
                        obstacle(
                            "loc.rooftops_kharid_wallclimb_2",
                            at(3316, 3180, 3),
                            6.0,
                            CLIMB,
                            ticks = 3,
                        ),
                        obstacle(
                            "loc.rooftops_kharid_tightrope_4",
                            at(3302, 3187, 3),
                            18.0,
                            BALANCE,
                            ticks = 8,
                            slide = true,
                        ),
                        obstacle("loc.rooftops_kharid_leapdown", at(3299, 3194, 0), 0.0, JUMP_DOWN),
                    ),
            ),
            Course(
                name = "Varrock Rooftop Course",
                level = 30,
                lapXp = 143.7,
                markOdds = 2.0 / 6,
                markSpawns =
                    marks(
                        Triple(3214, 3417, 3),
                        Triple(3202, 3417, 3),
                        Triple(3194, 3416, 1),
                        Triple(3194, 3404, 3),
                        Triple(3196, 3394, 3),
                        Triple(3205, 3395, 3),
                        Triple(3226, 3402, 3),
                        Triple(3236, 3407, 3),
                    ),
                obstacles =
                    listOf(
                        obstacle(
                            "loc.rooftops_varrock_wallclimb",
                            at(3219, 3414, 3),
                            13.5,
                            CLIMB,
                            ticks = 4,
                        ),
                        obstacle(
                            "loc.rooftops_varrock_clothesline",
                            at(3208, 3414, 3),
                            23.0,
                            SPOT_JUMP,
                            ticks = 5,
                            slide = true,
                        ),
                        obstacle(
                            "loc.rooftops_varrock_leaptoruins",
                            at(3197, 3416, 1),
                            19.0,
                            JUMP_DOWN,
                        ),
                        obstacle(
                            "loc.rooftops_varrock_wallswing",
                            at(3192, 3406, 3),
                            28.0,
                            SIDESTEP,
                            ticks = 6,
                            slide = true,
                        ),
                        obstacle(
                            "loc.rooftops_varrock_wallscramble",
                            Landing(z = 3398, level = 3),
                            10.0,
                            JUMP_UP,
                            ticks = 3,
                        ),
                        obstacle(
                            "loc.rooftops_varrock_leaptobalcony",
                            at(3218, 3399, 3),
                            24.5,
                            JUMP_UP,
                            ticks = 4,
                        ),
                        obstacle(
                            "loc.rooftops_varrock_leapdown",
                            at(3236, 3403, 3),
                            4.5,
                            JUMP_DOWN,
                        ),
                        obstacle(
                            "loc.rooftops_varrock_stepuproof",
                            at(3236, 3410, 3),
                            3.5,
                            HURDLE,
                        ),
                        obstacle(
                            "loc.rooftops_varrock_finish",
                            at(3236, 3417, 0),
                            0.0,
                            JUMP_DOWN,
                            ticks = 3,
                        ),
                    ),
            ),
            Course(
                name = "Canifis Rooftop Course",
                level = 40,
                lapXp = 175.0,
                markOdds = 2.0 / 3,
                markPenalty = false,
                markSpawns =
                    marks(
                        Triple(3508, 3494, 2),
                        Triple(3502, 3506, 2),
                        Triple(3499, 3505, 2),
                        Triple(3489, 3500, 2),
                        Triple(3492, 3499, 2),
                        Triple(3476, 3496, 3),
                        Triple(3475, 3493, 3),
                        Triple(3482, 3486, 2),
                        Triple(3478, 3484, 2),
                        Triple(3493, 3476, 3),
                        Triple(3495, 3472, 3),
                        Triple(3491, 3472, 3),
                        Triple(3513, 3479, 2),
                        Triple(3512, 3481, 2),
                        Triple(3510, 3476, 2),
                    ),
                obstacles =
                    listOf(
                        obstacle(
                            "loc.rooftops_canifis_start_tree",
                            at(3506, 3492, 2),
                            10.0,
                            TREE_CLIMB,
                            ticks = 4,
                        ),
                        obstacle(
                            "loc.rooftops_canifis_jump",
                            at(3502, 3504, 2),
                            8.0,
                            JUMP_DOWN,
                            ticks = 3,
                        ),
                        obstacle(
                            "loc.rooftops_canifis_jump_2",
                            at(3493, 3504, 2),
                            8.0,
                            JUMP_DOWN,
                            ticks = 3,
                        ),
                        obstacle(
                            "loc.rooftops_canifis_jump_5",
                            Landing(x = 3487, z = 3499, dx = -8, dLevel = 1),
                            10.0,
                            JUMP_UP,
                            ticks = 4,
                        ),
                        obstacle(
                            "loc.rooftops_canifis_jump_3",
                            at(3478, 3486, 2),
                            8.0,
                            JUMP_DOWN,
                            ticks = 3,
                        ),
                        obstacle(
                            "loc.rooftops_canifis_polevault",
                            at(3489, 3476, 3),
                            10.0,
                            POLE_VAULT,
                            ticks = 4,
                        ),
                        obstacle(
                            "loc.rooftops_canifis_jump_4",
                            at(3510, 3476, 2),
                            11.0,
                            JUMP_DOWN,
                            ticks = 3,
                        ),
                        obstacle(
                            "loc.rooftops_canifis_leapdown",
                            at(3510, 3485, 0),
                            0.0,
                            JUMP_DOWN,
                            ticks = 3,
                        ),
                    ),
            ),
            Course(
                name = "Falador Rooftop Course",
                level = 50,
                lapXp = 241.0,
                markOdds = 2.0 / 6,
                markSpawns =
                    marks(
                        Triple(3038, 3343, 3),
                        Triple(3049, 3348, 3),
                        Triple(3049, 3357, 3),
                        Triple(3045, 3365, 3),
                        Triple(3035, 3362, 3),
                        Triple(3028, 3353, 3),
                        Triple(3017, 3345, 3),
                        Triple(3011, 3339, 3),
                        Triple(3016, 3333, 3),
                    ),
                obstacles =
                    listOf(
                        obstacle(
                            "loc.rooftops_falador_wallclimb",
                            at(3036, 3342, 3),
                            11.0,
                            CLIMB,
                            ticks = 3,
                        ),
                        obstacle(
                            "loc.rooftops_falador_tightrope_1",
                            at(3047, 3343, 3),
                            22.0,
                            BALANCE,
                            ticks = 8,
                            slide = true,
                        ),
                        obstacle(
                            "loc.rooftops_falador_handholds_start",
                            at(3050, 3357, 3),
                            61.0,
                            HANDHOLDS,
                            ticks = 6,
                            slide = true,
                        ),
                        obstacle(
                            "loc.rooftops_falador_gap_1",
                            at(3048, 3361, 3),
                            27.0,
                            SPOT_JUMP,
                        ),
                        obstacle(
                            "loc.rooftops_falador_gap_2",
                            at(3041, 3361, 3),
                            26.0,
                            SPOT_JUMP,
                        ),
                        obstacle(
                            "loc.rooftops_falador_tightrope_2",
                            at(3028, 3354, 3),
                            61.0,
                            BALANCE,
                            ticks = 8,
                            slide = true,
                        ),
                        obstacle(
                            "loc.rooftops_falador_tightrope_3",
                            at(3020, 3353, 3),
                            53.0,
                            BALANCE,
                            ticks = 6,
                            slide = true,
                        ),
                        obstacle("loc.rooftops_falador_gap_3", Landing(dz = -4), 30.0, HURDLE),
                        obstacle("loc.rooftops_falador_ledge_1", Landing(dx = -2), 14.0, HURDLE),
                        obstacle("loc.rooftops_falador_ledge_2", Landing(dz = -2), 13.0, HURDLE),
                        obstacle("loc.rooftops_falador_ledge_3a", Landing(dz = -2), 13.0, HURDLE),
                        obstacle("loc.rooftops_falador_ledge_4", Landing(dx = 2), 14.0, HURDLE),
                        obstacle(
                            "loc.rooftops_falador_edge",
                            at(3029, 3333, 0),
                            0.0,
                            JUMP_DOWN,
                            ticks = 3,
                        ),
                    ),
            ),
            Course(
                name = "Seers' Village Rooftop Course",
                level = 60,
                lapXp = 435.0,
                markOdds = 2.0 / 6,
                markSpawns =
                    marks(
                        Triple(2726, 3492, 3),
                        Triple(2728, 3495, 3),
                        Triple(2707, 3493, 2),
                        Triple(2708, 3489, 2),
                        Triple(2712, 3481, 2),
                        Triple(2710, 3478, 2),
                        Triple(2710, 3472, 3),
                        Triple(2702, 3474, 3),
                        Triple(2698, 3462, 2),
                    ),
                obstacles =
                    listOf(
                        obstacle(
                            "loc.rooftops_seers_wallclimb",
                            at(2729, 3491, 3),
                            45.0,
                            CLIMB_UP,
                            ticks = 4,
                        ),
                        obstacle(
                            "loc.rooftops_seers_jump",
                            at(2713, 3494, 2),
                            20.0,
                            JUMP_DOWN,
                            ticks = 4,
                            slide = true,
                        ),
                        obstacle(
                            "loc.rooftops_seers_tightrope",
                            at(2710, 3481, 2),
                            20.0,
                            BALANCE,
                            ticks = 8,
                            slide = true,
                        ),
                        obstacle(
                            "loc.rooftops_seers_jump_1",
                            Landing(z = 3472, level = 3),
                            35.0,
                            JUMP_UP_2,
                            ticks = 3,
                        ),
                        obstacle(
                            "loc.rooftops_seers_jump_2",
                            at(2702, 3465, 2),
                            15.0,
                            JUMP_DOWN,
                            ticks = 3,
                        ),
                        obstacle(
                            "loc.rooftops_seers_leapdown",
                            at(2704, 3464, 0),
                            0.0,
                            JUMP_DOWN,
                            ticks = 3,
                        ),
                    ),
            ),
            Course(
                name = "Rellekka Rooftop Course",
                level = 80,
                lapXp = 615.0,
                markOdds = 2.0 / 5,
                markSpawns =
                    marks(
                        Triple(2622, 3676, 3),
                        Triple(2617, 3664, 3),
                        Triple(2618, 3660, 3),
                        Triple(2628, 3652, 3),
                        Triple(2628, 3655, 3),
                        Triple(2641, 3649, 3),
                        Triple(2643, 3651, 3),
                        Triple(2649, 3659, 3),
                        Triple(2644, 3662, 3),
                        Triple(2658, 3674, 3),
                        Triple(2656, 3681, 3),
                    ),
                obstacles =
                    listOf(
                        obstacle(
                            "loc.rooftops_rellekka_wallclimb",
                            at(2626, 3676, 3),
                            20.0,
                            CLIMB,
                            ticks = 3,
                        ),
                        obstacle(
                            "loc.rooftops_rellekka_gap_1",
                            Landing(dx = -1, dz = -4),
                            30.0,
                            HURDLE,
                            ticks = 3,
                        ),
                        obstacle(
                            "loc.rooftops_rellekka_tightrope_1",
                            at(2627, 3654, 3),
                            40.0,
                            BALANCE,
                            ticks = 8,
                            slide = true,
                        ),
                        obstacle(
                            "loc.rooftops_rellekka_gap_2",
                            at(2639, 3653, 3),
                            85.0,
                            SIDESTEP_2,
                            ticks = 8,
                            slide = true,
                        ),
                        obstacle(
                            "loc.rooftops_rellekka_gap_3",
                            Landing(dz = 4),
                            25.0,
                            HURDLE,
                            ticks = 3,
                        ),
                        obstacle(
                            "loc.rooftops_rellekka_tightrope_3",
                            at(2655, 3670, 3),
                            105.0,
                            BALANCE,
                            ticks = 8,
                            slide = true,
                        ),
                        obstacle(
                            "loc.rooftops_rellekka_dropoff",
                            at(2653, 3676, 0),
                            0.0,
                            JUMP_DOWN,
                            ticks = 3,
                        ),
                    ),
            ),
            Course(
                name = "Ardougne Rooftop Course",
                level = 90,
                lapXp = 625.0,
                markOdds = 2.0 / 3,
                markSpawns =
                    marks(
                        Triple(2671, 3304, 3),
                        Triple(2663, 3318, 3),
                        Triple(2654, 3318, 3),
                        Triple(2653, 3313, 3),
                        Triple(2653, 3302, 3),
                    ),
                obstacles =
                    listOf(
                        obstacle(
                            "loc.rooftops_ardy_wallclimb",
                            at(2671, 3299, 3),
                            43.0,
                            CLIMB_UP,
                            ticks = 4,
                        ),
                        obstacle(
                            "loc.rooftops_ardy_jump",
                            at(2665, 3318, 3),
                            65.0,
                            JUMP_DOWN,
                            ticks = 6,
                        ),
                        obstacle(
                            "loc.rooftops_ardy_plank",
                            at(2656, 3318, 3),
                            50.0,
                            BALANCE,
                            ticks = 6,
                            slide = true,
                        ),
                        obstacle(
                            "loc.rooftops_ardy_jump_2",
                            at(2653, 3314, 3),
                            21.0,
                            JUMP_DOWN,
                            ticks = 3,
                        ),
                        obstacle(
                            "loc.rooftops_ardy_jump_3",
                            at(2651, 3309, 3),
                            28.0,
                            JUMP_DOWN_FLAT,
                            ticks = 3,
                        ),
                        obstacle(
                            "loc.rooftops_ardy_wallcrossing",
                            at(2656, 3297, 3),
                            57.0,
                            SIDESTEP,
                            ticks = 6,
                            slide = true,
                        ),
                        obstacle(
                            "loc.rooftops_ardy_jump_4",
                            at(2668, 3297, 0),
                            0.0,
                            JUMP_DOWN,
                            ticks = 6,
                        ),
                    ),
            ),
            Course(
                name = "Barbarian Outpost Agility Course",
                level = 35,
                lapXp = 46.3,
                obstacles =
                    listOf(
                        obstacle(
                            "loc.obstical_ropeswing1",
                            at(2551, 3549, 0),
                            22.0,
                            ROPE_SWING,
                            ticks = 3,
                            slide = true,
                        ),
                        obstacle(
                            "loc.barbarian_log_balance1",
                            at(2541, 3546, 0),
                            13.7,
                            BALANCE,
                            ticks = 8,
                            slide = true,
                        ),
                        obstacle(
                            "loc.agility_obstical_net_barbarian",
                            Landing(dx = -2, level = 1),
                            8.2,
                            CLIMB,
                        ),
                        obstacle(
                            "loc.balancing_ledge1",
                            at(2532, 3546, 1),
                            22.0,
                            SIDESTEP,
                            ticks = 5,
                            slide = true,
                        ),
                        obstacle("loc.laddertop_norim", Landing(level = 0), 0.0, CLIMB_DOWN),
                        obstacle(
                            "loc.castlecrumbly1",
                            Landing(dx = 2),
                            13.7,
                            CRUMBLE_WALL,
                            repeats = 3,
                        ),
                    ),
            ),
            Course(
                name = "Wilderness Agility Course",
                level = 52,
                lapXp = 498.9,
                obstacles =
                    listOf(
                        obstacle(
                            "loc.obstical_pipe2",
                            at(3004, 3948, 0),
                            12.5,
                            PIPE,
                            ticks = 6,
                            slide = true,
                        ),
                        obstacle(
                            "loc.obstical_ropeswing2",
                            Landing(dz = 5),
                            20.0,
                            ROPE_SWING,
                            ticks = 3,
                            slide = true,
                        ),
                        obstacle(
                            "loc.steppingstone1",
                            Landing(dx = -6),
                            20.0,
                            STEPPING_STONE,
                            ticks = 6,
                            slide = true,
                        ),
                        obstacle(
                            "loc.wilderness_log_balance1",
                            at(2994, 3945, 0),
                            20.0,
                            BALANCE,
                            ticks = 8,
                            slide = true,
                        ),
                        obstacle("loc.wildclimbingrock", Landing(dz = -4), 0.0, CLIMB_DOWN, ticks = 3),
                    ),
            ),
        )
}
