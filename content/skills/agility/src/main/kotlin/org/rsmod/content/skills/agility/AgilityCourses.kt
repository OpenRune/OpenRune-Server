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
private const val ZIPLINE_BITE = "seq.zipline_bite"
private const val HANDHOLDS = "seq.agilityarena_handholds_middle"
private const val POLE_VAULT = "seq.rooftops_pole_vault"
private const val TREE_CLIMB = "seq.mdaughter_tree_climb_combi"
private const val PIPE = "seq.human_doublepipesqueeze"
private const val MONKEY_BARS = "seq.human_monkeybars_walk"
private const val CLIMB_DOWN = "seq.human_climbing_down"
private const val CRUMBLE_WALL = "seq.human_walk_crumbledwall"
private const val STEPPING_STONE = "seq.human_steppingstonejump"
private const val SQUEEZE = "seq.human_squeeze"
private const val GRAPPLE_SWING = "seq.dorgesh_grapple_swing"
private const val WALK = "seq.human_walk_f"
private const val WYRM_LONGJUMP = "seq.wyrm_agility_longjump"
private const val WYRM_SHORTJUMP = "seq.wyrm_agility_shortjump"

// Obstacles are built from three linked animations, and the cache names them that way.
private const val BARS_ON = "seq.human_monkeybars_on"
private const val BARS_WALK = "seq.human_monkeybars_walk"
private const val BARS_OFF = "seq.human_monkeybars_off"
private const val BALANCE_ON = "seq.human_walk_logbalance"
private const val BALANCE_WALK = "seq.human_walk_logbalance_loop"

private const val WYRM_LEDGE_ON = "seq.wyrm_agility_ledge_on"
private const val WYRM_LEDGE_WALK = "seq.wyrm_agility_ledge_walk"
private const val WYRM_LEDGE_OFF = "seq.wyrm_agility_ledge_off"
private const val WYRM_ZIP_READY = "seq.wyrm_agility_zipline_ready"
private const val WYRM_ZIP_ON = "seq.wyrm_agility_zipline_on"
private const val WYRM_ZIP_WALK = "seq.wyrm_agility_zipline_walk"
private const val WYRM_ZIP_OFF = "seq.wyrm_agility_zipline_off"

private const val APE_STONE_JUMP = "seq.100_ilm_stepping_stone_jump"
private const val APE_TREE_CLIMB = "seq.100_ilm_climb_tree"
private const val APE_BARS_ON = "seq.100_ilm_monkeybar_jump_up"
private const val APE_BARS_WALK = "seq.100_ilm_monkeybar_move"
private const val APE_BARS_OFF = "seq.100_ilm_monkeybar_jump_down"
private const val APE_SLOPE = "seq.100_ilm_climb_slope"
private const val APE_VINE_SWING = "seq.100_ilm_vine_swing"
private const val APE_DOWN_VINE = "seq.100_ilm_monkey_down_vine"

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

/**
 * One linked movement of an obstacle. Live builds a crossing out of three of these - climb on,
 * travel, drop off - and the cache names them that way too: `monkeybars_on`, `monkeybars_walk`,
 * `monkeybars_off`.
 *
 * A null [landing] keeps the player still, which is what a mount and a dismount do. [ticks] of 0
 * means "however long the distance takes" at one tile per tick, so a traverse keeps pace with its
 * own looping animation instead of gliding across ahead of the player's feet.
 *
 * [moveTicks] separates the travelling from the animation. A jump is airborne for a moment and then
 * spends the rest of its animation landing, so the movement has to finish early or the player keeps
 * drifting after they have already come down.
 *
 * [perTile] is how many ticks one tile of a traverse takes, and it has to be the length of the
 * animation's own loop or the feet and the ground disagree. Monkeybars loop in one tick and a log
 * balance takes two, which is why a tightrope walked at a monkeybar's pace looks like skating.
 */
data class Stage(
    val anim: String,
    val landing: Landing? = null,
    val ticks: Int = 0,
    val slide: Boolean = true,
    val moveTicks: Int = 0,
    val perTile: Int = 1,
)

/**
 * [stages] is the real shape of a crossing; when it is empty the obstacle is the older single
 * animate-wait-land, which is all the rooftop courses need.
 *
 * [via] is a corner to travel through on the way to [landing]. A slide is a straight line, so an
 * obstacle that turns - a rope south then a ledge west - needs the turn named or the player cuts
 * the corner through open air.
 */
data class Obstacle(
    val locs: List<String>,
    val landing: Landing,
    val xp: Double,
    val anim: String,
    val ticks: Int = 2,
    val slide: Boolean = false,
    val repeats: Int = 1,
    val fail: ObstacleFail? = null,
    val via: Landing? = null,
    val stages: List<Stage> = emptyList(),
)

/**
 * An obstacle that can be failed. [low] and [high] are the wiki's level-1 and level-99 odds out of
 * 256; the damage is the live formula, a share of the hitpoints the player has left rather than a
 * flat hit, which is why waiting until low health is the way players save food.
 */
data class ObstacleFail(val low: Int, val high: Int, val damage: (Int) -> Int)

/**
 * What a course asks for beyond the Agility level. The greegree and the ring stand in for the
 * quests behind them where live gates on the item rather than on quest progress.
 */
data class CourseReqs(
    val quest: String? = null,
    val worn: List<String> = emptyList(),
    val wornMessage: String = "",
    val grapple: Boolean = false,
) {
    companion object {
        val NONE: CourseReqs = CourseReqs()
    }
}

/**
 * [markOdds] is the chance of a mark of grace on a completed lap once the shared three minute
 * cooldown has passed: two in six for most rooftops, two in five at Rellekka and two in three at
 * Canifis and Ardougne. [markPenalty] is the 80% cut that applies twenty levels above [level] -
 * Canifis is the one course where live never applies it.
 *
 * [petBase] is the course's base for the giant squirrel roll, 1 in `petBase - level * 25`.
 */
data class Course(
    val name: String,
    val level: Int,
    val lapXp: Double,
    val obstacles: List<Obstacle>,
    val markSpawns: List<CoordGrid> = emptyList(),
    val markOdds: Double = 0.0,
    val markPenalty: Boolean = true,
    val petBase: Int = 0,
    val reqs: CourseReqs = CourseReqs.NONE,
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
    fail: ObstacleFail? = null,
) = Obstacle(listOf(loc), landing, xp, anim, ticks, slide, repeats, fail)

/**
 * A ladder behaves the way every other ladder in the game does - see `LadderScript`: play the climb
 * and go up. Nothing draws a player rising, because `EntityExactMove` carries x and z deltas and no
 * height, so there is no version of this that climbs visibly.
 */
private fun ladder(anim: String, to: Landing): List<Stage> =
    listOf(Stage(anim, landing = to, ticks = 1, slide = false))

/**
 * Step onto the obstacle's own line before travelling it. A crossing otherwise starts from wherever
 * the engine parked the player, which is beside the rope as often as on it, and the whole traverse
 * then runs parallel to the real rope instead of along it.
 */
private fun mount(anim: String, onto: Landing): Stage = Stage(anim, landing = onto, ticks = 1)

/** Climb on, travel to [to] at walking pace, drop off: the shape of most real obstacles. */
/** A crossing with no dismount animation, for obstacles the cache ships none for. */
private fun crossing(on: String, walk: String, to: Landing): List<Stage> =
    listOf(Stage(on, ticks = 1), Stage(walk, landing = to))

private fun crossing(
    on: String,
    walk: String,
    off: String,
    to: Landing,
    perTile: Int = 1,
): List<Stage> =
    listOf(
        Stage(on, ticks = 1),
        Stage(walk, landing = to, perTile = perTile),
        Stage(off, ticks = 1),
    )

/**
 * The same, but over a route rather than a straight line. The scaffolding an obstacle crosses is
 * rarely straight - the wyrm's ledges zigzag one plank at a time - and a slide only ever travels
 * straight, so every corner has to be a waypoint or the player cuts it through open air.
 */
private fun route(on: String, walk: String, off: String, vararg tiles: Landing): List<Stage> =
    listOf(Stage(on, ticks = 1)) +
        tiles.map { Stage(walk, landing = it) } +
        Stage(off, ticks = 1)

/**
 * Grab the line, ride it, drop off. The ride is a fixed few ticks rather than one per tile: a
 * zipline crosses its whole span quickly, so pacing it like a walk leaves the player crawling
 * through the air for a quarter of a minute.
 */
/**
 * Grab the line, ride it, drop off.
 *
 * [rideTo] has to be on the plane the line starts from, not the ground it ends on. An exact move
 * teleports the player to its destination first and then draws the horizontal travel, and the
 * protocol carries no height - so a ride that ends on the ground is drawn along the ground for its
 * whole length, and the player skims the scenery instead of hanging from the line. Riding at height
 * and dropping at the end is what puts them in the air.
 */
private fun zipline(
    ready: String,
    on: String,
    ride: String,
    off: String,
    rideTo: Landing,
    landing: Landing,
    rideTicks: Int,
): List<Stage> =
    listOf(
        Stage(ready, ticks = 2),
        Stage(on, ticks = 1),
        Stage(ride, landing = rideTo, ticks = rideTicks),
        Stage(off, landing = landing, ticks = 1),
    )

/**
 * A run of jumps from plank to plank. The Colossal Wyrm's scaffolding leaves real gaps between
 * boards - the locs are named `multijump` and `jump` for a reason - so each hop is one jump that
 * takes as long as the animation rather than as long as the gap is wide.
 */
private fun jumps(anim: String, ticks: Int, vararg tiles: Landing): List<Stage> =
    tiles.map { Stage(anim, landing = it, ticks = ticks, moveTicks = 1) }

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
                petBase = 35609,
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
                petBase = 33005,
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
                petBase = 26648,
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
                petBase = 24410,
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
                petBase = 36842,
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
                petBase = 26806,
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
                petBase = 35205,
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
                name = "Pollnivneach Rooftop Course",
                level = 70,
                lapXp = 666.0,
                petBase = 33422,
                markOdds = 2.0 / 6,
                markSpawns =
                    marks(
                        Triple(3349, 2967, 1),
                        Triple(3354, 2974, 1),
                        Triple(3362, 2979, 1),
                        Triple(3369, 2974, 1),
                        Triple(3365, 2985, 1),
                        Triple(3361, 2981, 2),
                        Triple(3362, 2994, 2),
                        Triple(3356, 3004, 2),
                    ),
                obstacles =
                    listOf(
                        obstacle(
                            "loc.rooftops_pollnivneach_basket",
                            at(3351, 2964, 1),
                            10.0,
                            CLIMB,
                            ticks = 3,
                        ),
                        obstacle(
                            "loc.rooftops_pollnivneach_marketstall",
                            at(3352, 2973, 1),
                            45.0,
                            SPOT_JUMP,
                            ticks = 4,
                            fail = ObstacleFail(60, 300) { hitpoints -> hitpoints / 17 + 2 },
                        ),
                        obstacle(
                            "loc.rooftops_pollnivneach_hangingbanner",
                            at(3360, 2978, 1),
                            65.0,
                            ROPE_SWING,
                            ticks = 4,
                            slide = true,
                        ),
                        obstacle(
                            "loc.rooftops_pollnivneach_gap",
                            Landing(dx = 4, dz = -1),
                            35.0,
                            JUMP_DOWN_FLAT,
                            ticks = 3,
                            slide = true,
                        ),
                        obstacle(
                            "loc.rooftops_pollnivneach_tree",
                            at(3366, 2982, 1),
                            75.0,
                            SPOT_JUMP,
                            ticks = 5,
                            slide = true,
                        ),
                        obstacle(
                            "loc.rooftops_pollnivneach_wallclimb",
                            at(3365, 2983, 2),
                            5.0,
                            JUMP_UP,
                            ticks = 3,
                        ),
                        obstacle(
                            "loc.rooftops_pollnivneach_monkeybars_start",
                            Landing(dz = 7),
                            55.0,
                            MONKEY_BARS,
                            ticks = 8,
                            slide = true,
                        ),
                        obstacle(
                            "loc.rooftops_pollnivneach_treetop",
                            at(3359, 3000, 2),
                            60.0,
                            HURDLE,
                            ticks = 5,
                            slide = true,
                        ),
                        obstacle(
                            "loc.rooftops_pollnivneach_line",
                            at(3364, 2998, 0),
                            0.0,
                            JUMP_DOWN,
                            ticks = 5,
                        ),
                    ),
            ),
            Course(
                name = "Rellekka Rooftop Course",
                level = 80,
                lapXp = 615.0,
                petBase = 31063,
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
                petBase = 34440,
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
                name = "Prifddinas Agility Course",
                level = 75,
                lapXp = 1037.1,
                petBase = 25146,
                obstacles =
                    listOf(
                        obstacle(
                            "loc.prif_agility_start_ladder",
                            at(3255, 6109, 2),
                            11.5,
                            CLIMB_UP,
                            ticks = 3,
                        ),
                        obstacle(
                            "loc.prif_agility_tightrope_start1",
                            at(3272, 6105, 2),
                            30.7,
                            BALANCE,
                            ticks = 8,
                            slide = true,
                        ),
                        obstacle(
                            "loc.prif_agility_chimney_jump",
                            at(3269, 6113, 2),
                            28.1,
                            SPOT_JUMP,
                            ticks = 5,
                            slide = true,
                        ),
                        obstacle(
                            "loc.prif_agility_roof_jump",
                            at(3269, 6117, 0),
                            23.0,
                            JUMP_DOWN_FLAT,
                            ticks = 3,
                        ),
                        obstacle(
                            "loc.prif_agility_dark_hole_active",
                            at(3293, 6141, 0),
                            11.5,
                            CLIMB_DOWN,
                            ticks = 4,
                        ),
                        obstacle(
                            "loc.prif_agility_tree_ladder_long",
                            at(3293, 6145, 2),
                            0.0,
                            CLIMB_UP,
                            ticks = 3,
                        ),
                        obstacle(
                            "loc.prif_agility_rope_bridge1",
                            at(3281, 6142, 2),
                            25.6,
                            BALANCE,
                            ticks = 6,
                            slide = true,
                        ),
                        obstacle(
                            "loc.prif_agility_tightrope1",
                            at(3271, 6149, 2),
                            30.7,
                            BALANCE,
                            ticks = 6,
                            slide = true,
                        ),
                        obstacle(
                            "loc.prif_agility_rope_bridge2",
                            at(3270, 6158, 2),
                            25.6,
                            BALANCE,
                            ticks = 6,
                            slide = true,
                        ),
                        obstacle(
                            "loc.prif_agility_tightrope2",
                            at(3274, 6168, 2),
                            30.7,
                            BALANCE,
                            ticks = 6,
                            slide = true,
                        ),
                        obstacle(
                            "loc.prif_agility_tightrope3",
                            at(3284, 6177, 0),
                            30.7,
                            BALANCE,
                            ticks = 6,
                            slide = true,
                        ),
                        obstacle(
                            "loc.prif_agility_dark_hole_end",
                            at(3240, 6109, 0),
                            0.0,
                            CLIMB_DOWN,
                            ticks = 4,
                        ),
                    ),
            ),
            Course(
                name = "Barbarian Outpost Agility Course",
                level = 35,
                lapXp = 46.3,
                petBase = 44376,
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
                petBase = 34666,
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
            Course(
                name = "Shayzien Basic Agility Course",
                level = 1,
                lapXp = 0.0,
                petBase = 31804,
                obstacles =
                    listOf(
                        Obstacle(
                            locs = listOf("loc.shayzien_agility_both_start_ladder"),
                            landing = at(1554, 3632, 3),
                            xp = 5.5,
                            anim = CLIMB,
                            stages = ladder(CLIMB, at(1554, 3632, 3)),
                        ),
                        Obstacle(
                            locs = listOf("loc.shayzien_agility_both_rope_climb"),
                            landing = at(1537, 3633, 2),
                            xp = 8.0,
                            anim = BARS_WALK,
                            stages = crossing(BARS_ON, BARS_WALK, BARS_OFF, at(1537, 3633, 2)),
                        ),
                        Obstacle(
                            locs = listOf("loc.shayzien_agility_both_rope_walk"),
                            landing = at(1526, 3633, 2),
                            xp = 9.0,
                            anim = BALANCE_WALK,
                            stages =
                                crossing(BALANCE_ON, BALANCE_WALK, at(1526, 3633, 2)),
                        ),
                        obstacle(
                            "loc.shayzien_agility_low_bar_climb",
                            at(1523, 3643, 3),
                            7.0,
                            CLIMB_UP,
                            ticks = 3,
                        ),
                        Obstacle(
                            locs = listOf("loc.shayzien_agility_low_rope_walk_1"),
                            landing = at(1540, 3644, 2),
                            xp = 9.0,
                            anim = BALANCE_WALK,
                            stages =
                                crossing(BALANCE_ON, BALANCE_WALK, at(1540, 3644, 2)),
                        ),
                        Obstacle(
                            locs = listOf("loc.shayzien_agility_low_rope_walk_2"),
                            landing = at(1553, 3644, 2),
                            xp = 9.0,
                            anim = BALANCE_WALK,
                            stages =
                                crossing(BALANCE_ON, BALANCE_WALK, at(1553, 3644, 2)),
                        ),
                        obstacle(
                            "loc.shayzien_agility_low_end_jump",
                            at(1554, 3637, 0),
                            106.0,
                            JUMP_DOWN,
                            ticks = 3,
                            slide = true,
                        ),
                    ),
            ),
            Course(
                name = "Shayzien Advanced Agility Course",
                level = 45,
                lapXp = 0.0,
                petBase = 29738,
                reqs = CourseReqs(grapple = true),
                obstacles =
                    listOf(
                        Obstacle(
                            locs = listOf("loc.shayzien_agility_both_start_ladder"),
                            landing = at(1554, 3632, 3),
                            xp = 6.0,
                            anim = CLIMB,
                            stages = ladder(CLIMB, at(1554, 3632, 3)),
                        ),
                        Obstacle(
                            locs = listOf("loc.shayzien_agility_both_rope_climb"),
                            landing = at(1537, 3633, 2),
                            xp = 8.0,
                            anim = BARS_WALK,
                            stages = crossing(BARS_ON, BARS_WALK, BARS_OFF, at(1537, 3633, 2)),
                        ),
                        Obstacle(
                            locs = listOf("loc.shayzien_agility_both_rope_walk"),
                            landing = at(1526, 3633, 2),
                            xp = 9.0,
                            anim = BALANCE_WALK,
                            stages =
                                crossing(BALANCE_ON, BALANCE_WALK, at(1526, 3633, 2)),
                        ),
                        obstacle(
                            "loc.shayzien_agility_up_swing_jump_1",
                            at(1511, 3635, 2),
                            23.0,
                            GRAPPLE_SWING,
                            ticks = 4,
                            slide = true,
                        ),
                        obstacle(
                            "loc.shayzien_agility_up_jump_platform_1",
                            at(1510, 3630, 2),
                            18.0,
                            SPOT_JUMP,
                            ticks = 3,
                            slide = true,
                        ),
                        obstacle(
                            "loc.shayzien_agility_up_jump_platform_2",
                            at(1511, 3622, 2),
                            21.0,
                            SPOT_JUMP,
                            ticks = 3,
                            slide = true,
                        ),
                        obstacle(
                            "loc.shayzien_agility_up_swing_jump_2",
                            at(1519, 3621, 2),
                            23.0,
                            GRAPPLE_SWING,
                            ticks = 4,
                            slide = true,
                        ),
                        Obstacle(
                            locs = listOf("loc.shayzien_agility_up_end_jump"),
                            landing = at(1551, 3630, 0),
                            xp = 400.0,
                            anim = ZIPLINE,
                            stages =
                                listOf(
                                    Stage(ZIPLINE, landing = at(1551, 3630, 2), ticks = 10),
                                    Stage(ZIPLINE, landing = at(1551, 3630, 0), ticks = 1, slide = false),
                                ),
                        ),
                    ),
            ),
            // The two Colossal Wyrm courses share their ladder, first tightrope and final zipline,
            // and their ledges have no op at all: live walks the player along them, so each is
            // folded into the obstacle before it, xp included.
            Course(
                name = "Colossal Wyrm Basic Agility Course",
                level = 50,
                lapXp = 0.0,
                reqs = CourseReqs(quest = "quest_childrenofthesun"),
                obstacles =
                    listOf(
                        Obstacle(
                            locs = listOf("loc.varlamore_wyrm_agility_start_ladder_trigger"),
                            landing = at(1654, 2931, 1),
                            xp = 37.2,
                            anim = CLIMB,
                            stages = ladder(CLIMB, at(1654, 2931, 1)),
                        ),
                        Obstacle(
                            locs = listOf("loc.varlamore_wyrm_agility_balance_1_trigger"),
                            landing = at(1649, 2910, 1),
                            xp = 74.4,
                            anim = BALANCE_WALK,
                            stages =
                                listOf(
                                    mount(BALANCE_ON, at(1655, 2924, 1)),
                                    Stage(BALANCE_WALK, landing = at(1655, 2919, 1)),
                                    Stage(WALK, landing = at(1655, 2916, 1)),
                                ) +
                                    jumps(
                                        WYRM_SHORTJUMP,
                                        2,
                                        at(1655, 2914, 1),
                                        at(1653, 2914, 1),
                                        at(1653, 2912, 1),
                                        at(1651, 2912, 1),
                                        at(1651, 2910, 1),
                                        at(1649, 2910, 1),
                                    ),
                        ),
                        Obstacle(
                            locs = listOf("loc.varlamore_wyrm_agility_basic_balance_1_trigger"),
                            landing = at(1635, 2910, 1),
                            xp = 37.2,
                            anim = BALANCE_WALK,
                            stages =
                                listOf(
                                    mount(BALANCE_ON, at(1645, 2910, 1)),
                                    Stage(BALANCE_WALK, landing = at(1635, 2910, 1)),
                                ),
                        ),
                        Obstacle(
                            locs = listOf("loc.varlamore_wyrm_agility_basic_monkeybars_1_trigger"),
                            landing = at(1627, 2931, 1),
                            xp = 74.4,
                            anim = BARS_WALK,
                            stages =
                                listOf(mount(BARS_ON, at(1631, 2910, 1))) +
                                    crossing(BARS_ON, BARS_WALK, BARS_OFF, at(1627, 2914, 1)) +
                                    Stage(WALK, landing = at(1627, 2918, 1)) +
                                    jumps(WYRM_LONGJUMP, 3, at(1627, 2923, 1)) +
                                    Stage(WALK, landing = at(1627, 2926, 1)) +
                                    jumps(WYRM_LONGJUMP, 3, at(1627, 2931, 1)),
                        ),
                        Obstacle(
                            locs = listOf("loc.varlamore_wyrm_agility_basic_ladder_1_trigger"),
                            landing = at(1625, 2932, 2),
                            xp = 37.2,
                            anim = CLIMB,
                            stages = ladder(CLIMB, at(1625, 2932, 2)),
                        ),
                        Obstacle(
                            locs = listOf("loc.varlamore_wyrm_agility_end_zipline_trigger"),
                            landing = at(1645, 2934, 0),
                            xp = 341.2,
                            anim = WYRM_ZIP_WALK,
                            stages =
                                // 1626,2933 is the scaffold wall the line hangs from, not floor: the last
                                // decking is 1625, and standing past it leaves the player in the air.
                                listOf(mount(WYRM_ZIP_READY, at(1625, 2933, 2))) +
                                    zipline(
                                        WYRM_ZIP_READY,
                                        WYRM_ZIP_ON,
                                        WYRM_ZIP_WALK,
                                        WYRM_ZIP_OFF,
                                        // The line runs dead east along z=2933 and ends at 1642;
                                        // riding to 1650,2931 crossed open air beside it.
                                        rideTo = at(1642, 2933, 2),
                                        landing = at(1645, 2934, 0),
                                        rideTicks = 4,
                                    ),
                        ),
                    ),
            ),
            Course(
                name = "Colossal Wyrm Advanced Agility Course",
                level = 62,
                lapXp = 0.0,
                reqs = CourseReqs(quest = "quest_childrenofthesun"),
                obstacles =
                    listOf(
                        obstacle(
                            "loc.varlamore_wyrm_agility_start_ladder_trigger",
                            at(1654, 2931, 1),
                            37.2,
                            CLIMB,
                            ticks = 3,
                        ),
                        Obstacle(
                            locs = listOf("loc.varlamore_wyrm_agility_balance_1_trigger"),
                            landing = at(1649, 2910, 1),
                            xp = 74.4,
                            anim = BALANCE_WALK,
                            stages =
                                listOf(
                                    mount(BALANCE_ON, at(1655, 2924, 1)),
                                    Stage(BALANCE_WALK, landing = at(1655, 2919, 1)),
                                    Stage(WALK, landing = at(1655, 2916, 1)),
                                ) +
                                    jumps(
                                        WYRM_SHORTJUMP,
                                        2,
                                        at(1655, 2914, 1),
                                        at(1653, 2914, 1),
                                        at(1653, 2912, 1),
                                        at(1651, 2912, 1),
                                        at(1651, 2910, 1),
                                        at(1649, 2910, 1),
                                    ),
                        ),
                        Obstacle(
                            locs = listOf("loc.varlamore_wyrm_agility_advanced_ladder_1_trigger"),
                            landing = at(1648, 2908, 2),
                            xp = 70.0,
                            anim = CLIMB,
                            stages = ladder(CLIMB, at(1648, 2908, 2)),
                        ),
                        Obstacle(
                            locs = listOf("loc.varlamore_wyrm_agility_advanced_jump_1_trigger"),
                            landing = at(1635, 2907, 2),
                            xp = 70.0,
                            anim = WYRM_LONGJUMP,
                            stages =
                                jumps(
                                    WYRM_LONGJUMP,
                                    3,
                                    at(1643, 2907, 2),
                                    at(1639, 2907, 2),
                                    at(1635, 2907, 2),
                                ),
                        ),
                        Obstacle(
                            locs = listOf("loc.varlamore_wyrm_agility_advanced_balance_1_trigger"),
                            landing = at(1624, 2931, 2),
                            xp = 140.0,
                            anim = BALANCE_WALK,
                            stages =
                                listOf(
                                    Stage(BALANCE_ON, ticks = 1),
                                    // Nothing is built between the two platforms, so the rope really
                                    // does run straight across the gap here.
                                    Stage(BALANCE_WALK, landing = at(1625, 2916, 2)),
                                    Stage(BALANCE_ON, ticks = 1),
                                    Stage(WALK, landing = at(1624, 2918, 2)),
                                ) +
                                    crossing(BARS_ON, BARS_WALK, BARS_OFF, at(1624, 2931, 2)),
                        ),
                        Obstacle(
                            locs = listOf("loc.varlamore_wyrm_agility_end_zipline_trigger"),
                            landing = at(1645, 2934, 0),
                            xp = 662.0,
                            anim = WYRM_ZIP_WALK,
                            stages =
                                // 1626,2933 is the scaffold wall the line hangs from, not floor: the last
                                // decking is 1625, and standing past it leaves the player in the air.
                                listOf(mount(WYRM_ZIP_READY, at(1625, 2933, 2))) +
                                    zipline(
                                        WYRM_ZIP_READY,
                                        WYRM_ZIP_ON,
                                        WYRM_ZIP_WALK,
                                        WYRM_ZIP_OFF,
                                        // The line runs dead east along z=2933 and ends at 1642;
                                        // riding to 1650,2931 crossed open air beside it.
                                        rideTo = at(1642, 2933, 2),
                                        landing = at(1645, 2934, 0),
                                        rideTicks = 4,
                                    ),
                        ),
                    ),
            ),
            Course(
                name = "Ape Atoll Agility Course",
                level = 48,
                lapXp = 300.0,
                petBase = 37720,
                reqs =
                    CourseReqs(
                        worn =
                            listOf(
                                "obj.mm_monkey_greegree_for_small_ninja_monkey",
                                "obj.mm_monkey_greegree_for_medium_ninja_monkey",
                                "obj.mm2_kruk_greegree",
                            ),
                        wornMessage = "Only the stealthiest and most agile monkey can use this!",
                    ),
                obstacles =
                    listOf(
                        obstacle(
                            "loc.100_ilm_stepping_stone",
                            Landing(dx = -2),
                            40.0,
                            APE_STONE_JUMP,
                            ticks = 2,
                            slide = true,
                        ),
                        obstacle(
                            "loc.100_ilm_climbable_tree",
                            Landing(level = 2),
                            40.0,
                            APE_TREE_CLIMB,
                            ticks = 4,
                        ),
                        Obstacle(
                            locs =
                                listOf("loc.100_ilm_monkeybars_start", "loc.100_ilm_monkeybars_end"),
                            landing = Landing(dx = -6, level = 0),
                            xp = 40.0,
                            anim = APE_BARS_WALK,
                            stages =
                                crossing(
                                    APE_BARS_ON,
                                    APE_BARS_WALK,
                                    APE_BARS_OFF,
                                    Landing(dx = -6, level = 0),
                                ),
                        ),
                        obstacle(
                            "loc.100_ilm_cliff_climb_1",
                            Landing(dx = -2),
                            60.0,
                            APE_SLOPE,
                            ticks = 2,
                            slide = true,
                        ),
                        obstacle(
                            "loc.100_ilm_rope_swing",
                            Landing(dx = 4),
                            100.0,
                            APE_VINE_SWING,
                            ticks = 3,
                            slide = true,
                        ),
                        obstacle(
                            "loc.100_ilm_agility_tree_base",
                            Landing(dz = 5),
                            0.0,
                            APE_DOWN_VINE,
                            ticks = 2,
                            slide = true,
                        ),
                    ),
            ),
            // No stick: the 380 bonus for handing one to the Agility Trainer needs the spawn and
            // the dialogue, so a lap here pays the obstacles only.
            Course(
                name = "Werewolf Agility Course",
                level = 60,
                lapXp = 0.0,
                petBase = 32597,
                reqs = CourseReqs(quest = "quest_creatureoffenkenstrain"),
                obstacles =
                    listOf(
                        obstacle(
                            "loc.werewolf_steping_stone",
                            at(3540, 9882, 0),
                            50.0,
                            STEPPING_STONE,
                            ticks = 6,
                            slide = true,
                        ),
                        Obstacle(
                            locs =
                                listOf(
                                    "loc.werewolf_hurdle_mid",
                                    "loc.werewolf_hurdle_end",
                                    "loc.werewolf_hurdle_end_mirror",
                                ),
                            landing = Landing(dz = 3),
                            xp = 20.0,
                            anim = HURDLE,
                            repeats = 3,
                            slide = true,
                        ),
                        obstacle("loc.waa_pipe", Landing(dz = 5), 15.0, SQUEEZE, ticks = 4, slide = true),
                        Obstacle(
                            locs =
                                listOf("loc.werewolf_skull_climb_1", "loc.werewolf_skull_climb_2"),
                            landing = Landing(dx = -3),
                            xp = 25.0,
                            anim = CLIMB_UP,
                            ticks = 3,
                            slide = true,
                        ),
                        Obstacle(
                            locs =
                                listOf(
                                    "loc.werewolf_slide_center",
                                    "loc.werewolf_slide_side",
                                    "loc.werewolf_slide_side_mirror",
                                ),
                            landing = at(3543, 9880, 0),
                            xp = 200.0,
                            anim = ZIPLINE,
                            stages =
                                listOf(
                                    Stage(ZIPLINE_BITE, ticks = 2),
                                    Stage(ZIPLINE, landing = at(3543, 9880, 0), ticks = 10),
                                ),
                        ),
                    ),
            ),
        )
}
