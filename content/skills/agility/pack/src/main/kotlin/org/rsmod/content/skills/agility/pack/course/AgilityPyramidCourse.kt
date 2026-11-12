package org.rsmod.content.skills.agility.pack.course

import dev.openrune.definition.dbtables.DBRowBuilder
import dev.openrune.definition.dbtables.DBTableBuilder
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.COURSE
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.RECOVERY_STEP
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.STEP
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.XP
import org.rsmod.content.skills.agility.pack.CLIMBING_DOWN
import org.rsmod.content.skills.agility.pack.CLIMBING_SYNTH
import org.rsmod.content.skills.agility.pack.CLIMB_WALL_SYNTH
import org.rsmod.content.skills.agility.pack.CRUMBLED_WALL
import org.rsmod.content.skills.agility.pack.FACE_EAST
import org.rsmod.content.skills.agility.pack.FACE_NORTH
import org.rsmod.content.skills.agility.pack.FACE_SOUTH
import org.rsmod.content.skills.agility.pack.FACE_WEST
import org.rsmod.content.skills.agility.pack.FALL_LAND_SYNTH
import org.rsmod.content.skills.agility.pack.HANDHOLDS_GRAB_SYNTH
import org.rsmod.content.skills.agility.pack.INTO_SIDESTEP
import org.rsmod.content.skills.agility.pack.INTO_SIDESTEP_RIGHT
import org.rsmod.content.skills.agility.pack.LEDGE_SYNTH
import org.rsmod.content.skills.agility.pack.LOG_BALANCE
import org.rsmod.content.skills.agility.pack.LOG_SYNTH
import org.rsmod.content.skills.agility.pack.Lane
import org.rsmod.content.skills.agility.pack.LaneBuilder
import org.rsmod.content.skills.agility.pack.OUT_OF_SIDESTEP
import org.rsmod.content.skills.agility.pack.OUT_OF_SIDESTEP_RIGHT
import org.rsmod.content.skills.agility.pack.SIDESTEP
import org.rsmod.content.skills.agility.pack.SIDESTEP_RIGHT
import org.rsmod.content.skills.agility.pack.landMesbox
import org.rsmod.content.skills.agility.pack.lane
import org.rsmod.content.skills.agility.pack.lanes
import org.rsmod.content.skills.agility.pack.obstacle
import org.rsmod.content.skills.agility.pack.shortcut.shortcut
import org.rsmod.content.skills.agility.pack.tile
import org.rsmod.map.CoordGrid

private const val PYRAMID = "dbrow.agility_course_pyramid"

private const val FLOOR_STEP = 1
private const val DOORWAY_STEP = 2

private const val PYRAMID_LEVEL = "varbit.agility_pyramid_level"
private const val PYRAMID_TOP = "varbit.agility_pyramid_top"

private const val STAIRS_UP = "loc.agility_pyramid_steps1"
private const val STAIRS_DOWN = "loc.agility_pyramid_steps_top_hotspot"
private const val LOW_WALL = "loc.agility_pyramid_low_wall"
private const val GAP = "loc.agility_pyramid_jump_hotspot"
private const val LEDGE = "loc.agility_pyramid_ledge_hotspot"
private const val PLANK_START = "loc.agility_pyramid_plank_start"
private const val PLANK_END = "loc.agility_pyramid_plank_end"
private const val HANG_START = "loc.agility_pyramid_wallhang_start_hotspot"
private const val HANG_END = "loc.agility_pyramid_wallhang_end_hotspot"
private const val DOORWAY = "loc.agility_pyramid_door_hotspot"
private const val DOORWAY_MIRROR = "loc.agility_pyramid_door_hotspot_mirror"

private const val DIVE = "seq.agilityarena_dive_player"
private const val GAP_JUMP = "seq.agility_pyramid_gap_jump"
private const val HANG_ON = "seq.agility_pyramid_ledge_on_right"
private const val HANG_WALK = "seq.agility_pyramid_ledge_walk_right"
private const val HANG_OFF = "seq.agility_pyramid_ledge_off_right"
private const val HANG_ON_LEFT = "seq.agility_pyramid_ledge_on_left"
private const val HANG_WALK_LEFT = "seq.agility_pyramid_ledge_walk_left"
private const val HANG_OFF_LEFT = "seq.agility_pyramid_ledge_off_left"

private const val SCRAPE_SYNTH = "synth.pyramid_scrape"
private const val JUMP_FURTHER_SYNTH = "synth.jump_further"

private const val LEDGE_ON = "You put your foot on the ledge and try to edge across..."
private const val LEDGE_OFF = "You skillfully edge across the gap."

private val NORTH = 0 to 1
private val EAST = 1 to 0
private val SOUTH = 0 to -1
private val WEST = -1 to 0

private val BASE = tile(3364, 2830, 0)

private const val HOP_WALL_TILES = 2
private const val HOP_GAP_TILES = 3
private const val CROSSING_TILES = 5

private class Stairs(
    val row: String,
    val step: Int,
    val loc: String,
    val from: CoordGrid,
    val landing: CoordGrid,
    val floor: Int,
)

private fun up(
    row: String,
    from: CoordGrid,
    landing: CoordGrid,
    floor: Int,
    step: Int = FLOOR_STEP,
) = Stairs(row, step, STAIRS_UP, from, landing, floor)

private fun down(row: String, from: CoordGrid, landing: CoordGrid, floor: Int) =
    Stairs(row, RECOVERY_STEP, STAIRS_DOWN, from, landing, floor)

private class StoneBlock(
    val row: String,
    val tilt: String,
    val near: List<CoordGrid>,
    val dir: Pair<Int, Int>,
    val angle: Int,
)

private class Hop(
    val row: String,
    val from: List<CoordGrid>,
    val dir: Pair<Int, Int>,
    val angle: Int,
    val synthDelay: Int = 30,
) {
    fun reversed(tiles: Int): Hop =
        Hop(row, from.map { it.toward(dir, tiles) }, dir.reversed(), angle.opposite(), synthDelay)
}

private fun Pair<Int, Int>.reversed(): Pair<Int, Int> = -first to -second

private fun Int.opposite(): Int = (this + FACE_NORTH) % (FACE_NORTH * 2)

private val STAIRS =
    listOf(
        up("dbrow.agility_pyramid_stairs", tile(3354, 2830, 0), tile(3355, 2833, 1), 1, step = 0),
        up("dbrow.agility_pyramid_stairs_1", tile(3356, 2832, 1), tile(3356, 2835, 2), 2),
        up("dbrow.agility_pyramid_stairs_2", tile(3358, 2834, 2), tile(3358, 2837, 3), 3),
        up("dbrow.agility_pyramid_stairs_3", tile(3360, 2836, 3), tile(3041, 4695, 2), 4),
        up("dbrow.agility_pyramid_stairs_4", tile(3042, 4694, 2), tile(3043, 4697, 3), 5),
        // UNVERIFIED: every way down; each lands where the flight below is climbed from.
        down("dbrow.agility_pyramid_stairs_down_1", tile(3354, 2833, 1), tile(3355, 2830, 0), 0),
        down("dbrow.agility_pyramid_stairs_down_2", tile(3356, 2835, 2), tile(3357, 2832, 1), 1),
        down("dbrow.agility_pyramid_stairs_down_3", tile(3358, 2837, 3), tile(3359, 2834, 2), 2),
        down("dbrow.agility_pyramid_stairs_down_4", tile(3040, 4695, 2), tile(3361, 2836, 3), 3),
        down("dbrow.agility_pyramid_stairs_down_5", tile(3042, 4697, 3), tile(3043, 4694, 2), 4),
    )

private val STONE_BLOCKS =
    listOf(
        StoneBlock(
            "dbrow.agility_pyramid_stone_block_1",
            "varbit.agility_pyramid_tilt_1",
            listOf(tile(3354, 2841, 1), tile(3355, 2841, 1)),
            NORTH,
            FACE_NORTH,
        ),
        StoneBlock(
            "dbrow.agility_pyramid_stone_block_2",
            "varbit.agility_pyramid_tilt_2",
            listOf(tile(3368, 2849, 2), tile(3368, 2850, 2)),
            EAST,
            FACE_EAST,
        ),
        StoneBlock(
            "dbrow.agility_pyramid_stone_block_3",
            "varbit.agility_pyramid_tilt_3",
            listOf(tile(3374, 2836, 1), tile(3375, 2836, 1)),
            SOUTH,
            FACE_SOUTH,
        ),
        StoneBlock(
            "dbrow.agility_pyramid_stone_block_4",
            "varbit.agility_pyramid_tilt_4",
            listOf(tile(3048, 4700, 2), tile(3049, 4700, 2)),
            SOUTH,
            FACE_SOUTH,
        ),
        StoneBlock(
            "dbrow.agility_pyramid_stone_block_5",
            "varbit.agility_pyramid_tilt_5",
            listOf(tile(3044, 4699, 3), tile(3044, 4700, 3)),
            EAST,
            FACE_EAST,
        ),
    )

private val LOW_WALLS =
    listOf(
        Hop(
            "dbrow.agility_pyramid_low_wall_1",
            listOf(tile(3354, 2848, 1), tile(3355, 2848, 1)),
            NORTH,
            FACE_NORTH,
        ),
        Hop(
            "dbrow.agility_pyramid_low_wall_2",
            listOf(tile(3371, 2833, 2), tile(3371, 2834, 2)),
            WEST,
            FACE_WEST,
        ),
        Hop(
            "dbrow.agility_pyramid_low_wall_3",
            listOf(tile(3358, 2838, 3), tile(3359, 2838, 3)),
            NORTH,
            FACE_NORTH,
        ),
        Hop(
            "dbrow.agility_pyramid_low_wall_4",
            listOf(tile(3041, 4701, 2), tile(3041, 4702, 2)),
            EAST,
            FACE_EAST,
            synthDelay = 40,
        ),
        Hop(
            "dbrow.agility_pyramid_low_wall_5",
            listOf(tile(3048, 4693, 2), tile(3048, 4694, 2)),
            WEST,
            FACE_WEST,
        ),
    )

private val GAP_JUMPS =
    listOf(
        Hop(
            "dbrow.agility_pyramid_gap_jump_1",
            listOf(tile(3356, 2846, 2), tile(3357, 2846, 2)),
            NORTH,
            FACE_NORTH,
        ),
        Hop(
            "dbrow.agility_pyramid_gap_jump_2",
            listOf(tile(3366, 2833, 2), tile(3366, 2834, 2)),
            WEST,
            FACE_WEST,
        ),
        Hop(
            "dbrow.agility_pyramid_gap_jump_3",
            listOf(tile(3370, 2843, 3), tile(3371, 2843, 3)),
            SOUTH,
            FACE_SOUTH,
        ),
        Hop(
            "dbrow.agility_pyramid_gap_jump_4",
            listOf(tile(3040, 4696, 2), tile(3041, 4696, 2)),
            NORTH,
            FACE_NORTH,
        ),
        Hop(
            "dbrow.agility_pyramid_gap_jump_5",
            listOf(tile(3048, 4697, 2), tile(3049, 4697, 2)),
            SOUTH,
            FACE_SOUTH,
        ),
        Hop(
            "dbrow.agility_pyramid_gap_jump_6",
            listOf(tile(3046, 4699, 3), tile(3047, 4699, 3)),
            SOUTH,
            FACE_SOUTH,
        ),
    )

/*
 * The Agility Pyramid lap is the stairs up from the ground, the floors' obstacles in any order and
 * the doorway at the top. The climbing rocks at the top are the pyramid script's, as they pay no xp
 * and take no part in the lap.
 */
internal fun DBTableBuilder.pyramidObstacles() {
    pyramidRocks()
    for (stairs in STAIRS) {
        row(stairs.row) {
            obstacle(PYRAMID, stairs.step, xp = 0, stairs.loc)
            lanes(
                *listOf(stairs.from, stairs.from.translate(1, 0))
                    .map { from ->
                        lane(from) {
                            varbit(PYRAMID_LEVEL, stairs.floor)
                            telejump(stairs.landing)
                            land()
                        }
                    }
                    .toTypedArray()
            )
        }
    }

    for (block in STONE_BLOCKS) {
        row(block.row) {
            stepOnObstacle(PYRAMID, FLOOR_STEP, xp = 120)
            lanes(*block.near.map { lane(it) { stoneBlock(block, it) } }.toTypedArray())
        }
    }

    for (wall in LOW_WALLS) {
        row(wall.row) {
            obstacle(PYRAMID, FLOOR_STEP, xp = 80, LOW_WALL)
            val back = wall.reversed(HOP_WALL_TILES)
            val ways = wall.from.map { lane(it) { lowWall(wall, it) } }
            // UNVERIFIED: no capture climbs a low wall this way.
            val waysBack = back.from.map { lane(it) { lowWall(back, it) } }
            lanes(*(ways + waysBack).toTypedArray())
        }
    }

    for (gap in GAP_JUMPS) {
        row(gap.row) {
            obstacle(PYRAMID, FLOOR_STEP, xp = 220, GAP)
            val back = gap.reversed(HOP_GAP_TILES)
            val ways = gap.from.map { lane(it) { gapJump(gap, it) } }
            // UNVERIFIED: no capture jumps a gap this way.
            val waysBack = back.from.map { lane(it) { gapJump(back, it) } }
            lanes(*(ways + waysBack).toTypedArray())
        }
    }

    row("dbrow.agility_pyramid_ledge_1") {
        obstacle(PYRAMID, FLOOR_STEP, xp = 520, LEDGE)
        lanes(*ledgeLanes(tile(3363, 2851, 1), EAST, OUT_OF_SIDESTEP))
    }
    row("dbrow.agility_pyramid_ledge_2") {
        obstacle(
            PYRAMID,
            FLOOR_STEP,
            xp = 520,
            "loc.agility_pyramid_ledgebalance_start_south_hotspot",
            "loc.agility_pyramid_ledgebalance_end_south_hotspot",
        )
        lanes(*ledgeLanes(tile(3364, 2832, 1), WEST, OUT_OF_SIDESTEP_RIGHT))
    }
    row("dbrow.agility_pyramid_ledge_3") {
        obstacle(PYRAMID, FLOOR_STEP, xp = 520, LEDGE)
        lanes(*ledgeLanes(tile(3372, 2841, 2), SOUTH, OUT_OF_SIDESTEP))
    }
    row("dbrow.agility_pyramid_ledge_4") {
        obstacle(
            PYRAMID,
            FLOOR_STEP,
            xp = 520,
            "loc.agility_pyramid_ledgebalance_start_west_hotspot",
            "loc.agility_pyramid_ledgebalance_end_west_hotspot",
        )
        val start = tile(3359, 2842, 3)
        lanes(
            *ledgeLanes(start, NORTH, OUT_OF_SIDESTEP),
            lane(tile(3358, 2842, 3)) {
                step(start)
                next(2)
                ledge(start, NORTH, OUT_OF_SIDESTEP)
            },
        )
    }

    row("dbrow.agility_pyramid_plank_1") {
        obstacle(PYRAMID, FLOOR_STEP, xp = 564, PLANK_START, PLANK_END)
        lanes(*planks(tile(3375, 2845, 1), approach = tile(3375, 2846, 1), SOUTH))
    }
    row("dbrow.agility_pyramid_plank_2") {
        obstacle(PYRAMID, FLOOR_STEP, xp = 564, PLANK_START, PLANK_END)
        lanes(*planks(tile(3370, 2835, 3), approach = tile(3370, 2836, 3), WEST))
    }

    row("dbrow.agility_pyramid_gap_1") {
        obstacle(
            PYRAMID,
            FLOOR_STEP,
            xp = 564,
            "loc.agility_pyramid_wallhang_start_south_hotspot",
            "loc.agility_pyramid_wallhang_end_south_hotspot",
            HANG_START,
            HANG_END,
        )
        lanes(*wallHangLanes(tile(3372, 2832, 1), WEST, FACE_WEST))
    }
    row("dbrow.agility_pyramid_gap_2") {
        obstacle(
            PYRAMID,
            FLOOR_STEP,
            xp = 564,
            "loc.agility_pyramid_wallhang_start_west_hotspot",
            "loc.agility_pyramid_wallhang_end_west_hotspot",
            HANG_START,
            HANG_END,
        )
        val start = tile(3357, 2836, 2)
        lanes(
            *wallHangLanes(start, NORTH, FACE_NORTH),
            lane(tile(3356, 2836, 2)) {
                step(start)
                next(2)
                wallHang(start, NORTH, FACE_NORTH)
            },
        )
    }
    row("dbrow.agility_pyramid_gap_3") {
        obstacle(
            PYRAMID,
            FLOOR_STEP,
            xp = 564,
            "loc.agility_pyramid_wallhang_start_hotspot_ground",
            "loc.agility_pyramid_wallhang_end_hotspot_ground",
            HANG_START,
            HANG_END,
        )
        lanes(*wallHangLanes(tile(3359, 2849, 2), EAST, FACE_EAST))
    }

    row("dbrow.agility_pyramid_doorway") {
        obstacle(PYRAMID, DOORWAY_STEP, xp = 0, DOORWAY, DOORWAY_MIRROR)
        lanes(
            *listOf(tile(3044, 4696, 3), tile(3045, 4696, 3))
                .map { from ->
                    lane(from) {
                        varbit(PYRAMID_TOP, 0)
                        varbit(PYRAMID_LEVEL, 0)
                        telejump(BASE)
                        land()
                    }
                }
                .toTypedArray()
        )
        landMesbox("You climb down the steep passage. It leads to the base of the pyramid.")
    }
}

private fun DBRowBuilder.stepOnObstacle(course: String, step: Int, xp: Int) {
    columnRSCM(COURSE, course)
    column(STEP, step)
    column(XP, xp)
}

private fun CoordGrid.toward(dir: Pair<Int, Int>, tiles: Int): CoordGrid =
    translate(dir.first * tiles, dir.second * tiles)

private fun LaneBuilder.stoneBlock(block: StoneBlock, from: CoordGrid) {
    varbit(block.tilt, 1)
    exactMove(from.toward(block.dir, 2), delay1 = 10, delay2 = 30, block.angle, faceAngle = false)
    anim(DIVE)
    sound(SCRAPE_SYNTH)
    sound(FALL_LAND_SYNTH, delay = 25)
    next()
    varbit(block.tilt, 0)
    land()
}

private fun LaneBuilder.lowWall(wall: Hop, from: CoordGrid) {
    message("You climb the low wall...")
    exactMove(from.toward(wall.dir, 2), delay1 = 30, delay2 = 100, wall.angle, faceAngle = false)
    anim(CRUMBLED_WALL, delay = 30)
    sound(CLIMB_WALL_SYNTH, delay = wall.synthDelay)
    next(3)
    message("... and make it over.")
    land()
}

private fun LaneBuilder.gapJump(gap: Hop, from: CoordGrid) {
    message("You jump the gap...")
    exactMove(from.toward(gap.dir, 3), delay1 = 53, delay2 = 85, gap.angle, faceAngle = false)
    anim(GAP_JUMP, delay = 20)
    sound(JUMP_FURTHER_SYNTH, delay = 20)
    next(3)
    land()
}

private fun ledgeLanes(from: CoordGrid, dir: Pair<Int, Int>, off: String): Array<Lane> {
    val back = from.toward(dir, CROSSING_TILES)
    val backOff = if (off == OUT_OF_SIDESTEP) OUT_OF_SIDESTEP_RIGHT else OUT_OF_SIDESTEP
    return arrayOf(
        lane(from) { ledge(from, dir, off) },
        // UNVERIFIED: no capture crosses a ledge this way.
        lane(back) { ledge(back, dir.reversed(), backOff, INTO_SIDESTEP_RIGHT, SIDESTEP_RIGHT) },
    )
}

private fun LaneBuilder.ledge(
    from: CoordGrid,
    dir: Pair<Int, Int>,
    off: String,
    into: String = INTO_SIDESTEP,
    walk: String = SIDESTEP,
) {
    message(LEDGE_ON)
    anim(into)
    balance(walk)
    sound(LEDGE_SYNTH, loops = 5)
    step(from.toward(dir, 1))
    for (tiles in 2..CROSSING_TILES) {
        next()
        step(from.toward(dir, tiles))
    }
    next(2)
    message(LEDGE_OFF)
    anim(off)
    balanceOff()
    land()
}

private class HangSeqs(val on: String, val walk: String, val off: String)

private val HANG_RIGHT = HangSeqs(HANG_ON, HANG_WALK, HANG_OFF)
private val HANG_LEFT = HangSeqs(HANG_ON_LEFT, HANG_WALK_LEFT, HANG_OFF_LEFT)

private fun wallHangLanes(from: CoordGrid, dir: Pair<Int, Int>, angle: Int): Array<Lane> {
    val back = from.toward(dir, CROSSING_TILES)
    return arrayOf(
        lane(from) { wallHang(from, dir, angle) },
        // UNVERIFIED: no capture crosses a gap this way.
        lane(back) { wallHang(back, dir.reversed(), angle.opposite(), HANG_LEFT) },
    )
}

private fun LaneBuilder.wallHang(
    from: CoordGrid,
    dir: Pair<Int, Int>,
    angle: Int,
    seqs: HangSeqs = HANG_RIGHT,
) {
    anim(seqs.on)
    faceAngle(angle)
    sound(HANDHOLDS_GRAB_SYNTH)
    // UNVERIFIED: the walk seq; live only shows the appearance changing.
    balance(seqs.walk)
    for (tiles in listOf(1, 2, 3, CROSSING_TILES)) {
        next()
        step(from.toward(dir, tiles))
    }
    next(2)
    anim(seqs.off)
    balanceOff()
    sound(FALL_LAND_SYNTH)
    land()
}

private fun planks(start: CoordGrid, approach: CoordGrid, dir: Pair<Int, Int>): Array<Lane> {
    val back = start.toward(dir, CROSSING_TILES)
    return arrayOf(
        lane(start) { plank(start, dir) },
        lane(approach) {
            step(start)
            next(2)
            plank(start, dir)
        },
        // UNVERIFIED: no capture crosses a plank this way.
        lane(back) { plank(back, dir.reversed()) },
    )
}

private fun LaneBuilder.plank(start: CoordGrid, dir: Pair<Int, Int>) {
    message("You walk carefully across the slippery plank...")
    // UNVERIFIED: the walk seq; live only shows the appearance changing.
    balance(LOG_BALANCE)
    sound(LOG_SYNTH, loops = 3)
    step(start.toward(dir, 1))
    for (tiles in 2..CROSSING_TILES) {
        next()
        step(start.toward(dir, tiles))
    }
    next()
    balanceOff()
    land()
}

private const val ROCKS_1 = "loc.ntk_agility_climbing_rocks_1"
private const val ROCKS_2 = "loc.ntk_agility_climbing_rocks_2"

private const val ROCKS_2_LEVEL = 30
private const val ROCKS_2_XP = 50
private const val GLIDE_CYCLES = 150
private const val CLIMB_TICKS = 5

/*
 * The two sets of climbing rocks down the cliffs to Simon Templeton and on to the pyramid. They
 * take no part in the Agility Pyramid's lap.
 */
private fun DBTableBuilder.pyramidRocks() {
    row("dbrow.agility_pyramid_rocks_1") {
        shortcut(level = 1, xp = 0, ROCKS_1)
        lanes(
            *(2826..2829)
                .flatMap { z ->
                    listOf(
                        lane(tile(3334, z, 0)) {
                            step(tile(3335, z, 0))
                            next()
                            descend(tile(3338, z, 0), FACE_WEST)
                        },
                        // UNVERIFIED: the climb back up mirrors the captured climb down.
                        lane(tile(3338, z, 0)) {
                            step(tile(3337, z, 0))
                            next()
                            descend(tile(3334, z, 0), FACE_EAST)
                        },
                    )
                }
                .toTypedArray()
        )
    }
    row("dbrow.agility_pyramid_rocks_2") {
        shortcut(level = ROCKS_2_LEVEL, xp = ROCKS_2_XP, ROCKS_2)
        lanes(
            *(2827..2829)
                .flatMap { z ->
                    listOf(
                        lane(tile(3348, z, 0)) { descend(tile(3352, z, 0), FACE_WEST) },
                        // UNVERIFIED: the climb back up mirrors the captured climb down.
                        lane(tile(3352, z, 0)) { descend(tile(3348, z, 0), FACE_EAST) },
                    )
                }
                .toTypedArray()
        )
    }
}

private fun LaneBuilder.descend(dest: CoordGrid, angle: Int) {
    faceAngle(angle)
    next()
    exactMove(dest, delay1 = 0, delay2 = GLIDE_CYCLES, angle)
    anim(CLIMBING_DOWN, delay = 15)
    sound(CLIMBING_SYNTH, loops = 8, delay = 15)
    next(CLIMB_TICKS)
    resetAnim()
    land()
}
