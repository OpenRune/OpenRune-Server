package org.rsmod.content.skills.agility.pack.rooftop

import dev.openrune.definition.dbtables.DBTableBuilder
import org.rsmod.content.skills.agility.pack.CLIMB
import org.rsmod.content.skills.agility.pack.FACE_EAST
import org.rsmod.content.skills.agility.pack.FACE_NORTH
import org.rsmod.content.skills.agility.pack.FACE_SOUTH
import org.rsmod.content.skills.agility.pack.FACE_WEST
import org.rsmod.content.skills.agility.pack.FALL_LAND_SYNTH
import org.rsmod.content.skills.agility.pack.FallDamage
import org.rsmod.content.skills.agility.pack.HANDHOLDS_FALL_REVERSE
import org.rsmod.content.skills.agility.pack.HANDHOLDS_REVERSE
import org.rsmod.content.skills.agility.pack.HANDHOLDS_REVERSE_LAST
import org.rsmod.content.skills.agility.pack.HANDHOLDS_SYNTH
import org.rsmod.content.skills.agility.pack.HURDLE
import org.rsmod.content.skills.agility.pack.HURDLE_SYNTH
import org.rsmod.content.skills.agility.pack.INTO_SIDESTEP
import org.rsmod.content.skills.agility.pack.JUMP_SYNTH
import org.rsmod.content.skills.agility.pack.JUMP_UP_SYNTH
import org.rsmod.content.skills.agility.pack.LEDGE_SYNTH
import org.rsmod.content.skills.agility.pack.LaneBuilder
import org.rsmod.content.skills.agility.pack.RUN_UP
import org.rsmod.content.skills.agility.pack.SIDESTEP
import org.rsmod.content.skills.agility.pack.SPOT_JUMP
import org.rsmod.content.skills.agility.pack.STUMBLE
import org.rsmod.content.skills.agility.pack.STUMBLE_SYNTH
import org.rsmod.content.skills.agility.pack.WALL_JUMP
import org.rsmod.content.skills.agility.pack.WALL_JUMP_LAND
import org.rsmod.content.skills.agility.pack.apRange
import org.rsmod.content.skills.agility.pack.fails
import org.rsmod.content.skills.agility.pack.lane
import org.rsmod.content.skills.agility.pack.lanes
import org.rsmod.content.skills.agility.pack.obstacle
import org.rsmod.content.skills.agility.pack.tile

private const val VARROCK = "dbrow.agility_course_varrock"

// UNVERIFIED: Varrock's curves are fitted to captured falls at levels 30-36 and stop at a guessed
// 40. The damage shares assume the captured players had 99 hitpoints.
private val CLOTHESLINE_FALL = FallDamage(min = 1, max = 2, hpShare = 1 to 17)
private val WALL_SWING_FALL = FallDamage(min = 0, max = 1, hpShare = 1 to 17)

internal fun DBTableBuilder.varrockObstacles() {
    row("dbrow.agility_varrock_rough_wall") {
        obstacle(VARROCK, step = 0, xp = 135, "loc.rooftops_varrock_wallclimb")
        lanes(
            lane(tile(3221, 3414, 0)) {
                face(tile(3218, 3414, 0))
                anim(CLIMB, delay = 15)
                next(2)
                telejump(tile(3220, 3414, 3))
                anim(WALL_JUMP_LAND)
                sound(JUMP_UP_SYNTH)
                next()
                exactMove(tile(3219, 3414, 3), delay1 = 17, delay2 = 26, angle = FACE_WEST)
                next()
                resetAnim()
                land()
            }
        )
    }

    row("dbrow.agility_varrock_clothes_line") {
        obstacle(VARROCK, step = 1, xp = 230, "loc.rooftops_varrock_clothesline")
        lanes(
            lane(tile(3214, 3414, 3)) {
                exactMove(tile(3212, 3414, 3), delay1 = 15, delay2 = 30, angle = FACE_WEST)
                anim(SPOT_JUMP)
                sound(JUMP_SYNTH)
                next()
                exactMove(tile(3210, 3414, 3), delay1 = 30, delay2 = 45, angle = FACE_WEST)
                anim(SPOT_JUMP, delay = 15)
                sound(JUMP_SYNTH, delay = 15)
                next(2)
                exactMove(tile(3208, 3414, 3), delay1 = 15, delay2 = 30, angle = FACE_WEST)
                anim(SPOT_JUMP)
                sound(JUMP_SYNTH)
                next(2)
                land()
            }
        )
        fails(
            159,
            399,
            CLOTHESLINE_FALL,
            lane(tile(3214, 3414, 3)) {
                exactMove(tile(3212, 3414, 3), delay1 = 15, delay2 = 30, angle = FACE_WEST)
                anim(SPOT_JUMP)
                sound(JUMP_SYNTH)
                next()
                anim(STUMBLE)
                sound(STUMBLE_SYNTH, loops = 10)
                next()
                drop(tile(3212, 3414, 0))
            },
        )
    }

    row("dbrow.agility_varrock_gap_1") {
        obstacle(VARROCK, step = 2, xp = 190, "loc.rooftops_varrock_leaptoruins")
        lanes(lane(tile(3201, 3416, 3)) { gap(tile(3196, 3416, 3), tile(3197, 3416, 1)) })
    }

    row("dbrow.agility_varrock_wall") {
        obstacle(VARROCK, step = 3, xp = 280, "loc.rooftops_varrock_wallswing")
        apRange(3)
        lanes(
            lane(tile(3194, 3416, 1)) {
                wallSwingRunUp()
                for (z in 3413 downTo 3409) {
                    val seq = if (z == 3409) HANDHOLDS_REVERSE_LAST else HANDHOLDS_REVERSE
                    wallSwingHandhold(z, seq)
                }
                next()
                balance(SIDESTEP)
                anim(INTO_SIDESTEP)
                next()
                step(tile(3190, 3408, 1))
                sound(LEDGE_SYNTH, loops = 2)
                next()
                step(tile(3190, 3407, 1))
                sound(LEDGE_SYNTH, loops = 2)
                next()
                exactMove(tile(3192, 3406, 1), delay1 = 5, delay2 = 30, angle = FACE_EAST)
                balanceOff()
                anim(SPOT_JUMP)
                sound(JUMP_UP_SYNTH)
                next()
                telejump(tile(3192, 3406, 3))
                resetAnim()
                next()
                land()
            }
        )
        fails(
            141,
            427,
            WALL_SWING_FALL,
            lane(tile(3194, 3416, 1)) {
                wallSwingRunUp()
                for (z in 3413 downTo 3411) {
                    wallSwingHandhold(z, HANDHOLDS_REVERSE)
                }
                next()
                exactMove(tile(3190, 3410, 1), delay1 = 34, delay2 = 52, angle = FACE_WEST)
                anim(HANDHOLDS_FALL_REVERSE)
                next(2)
                telejump(tile(3190, 3410, 0))
                resetAnim()
                sound(FALL_LAND_SYNTH)
                hit()
            },
        )
    }

    row("dbrow.agility_varrock_gap_2") {
        obstacle(VARROCK, step = 4, xp = 100, "loc.rooftops_varrock_wallscramble")
        lanes(
            lane(tile(3193, 3402, 3)) {
                exactMove(tile(3193, 3399, 3), delay1 = 25, delay2 = 30, angle = FACE_SOUTH)
                anim(WALL_JUMP, delay = 20)
                sound(JUMP_UP_SYNTH, delay = 20)
                next()
                anim(WALL_JUMP_LAND)
                next()
                exactMove(tile(3193, 3398, 3), delay1 = 17, delay2 = 26, angle = FACE_SOUTH)
                next()
                resetAnim()
                land()
            }
        )
    }

    row("dbrow.agility_varrock_gap_3") {
        obstacle(VARROCK, step = 5, xp = 245, "loc.rooftops_varrock_leaptobalcony")
        lanes(
            lane(tile(3208, 3397, 3)) {
                face(tile(3215, 3399, 3))
                anim(RUN_UP)
                next()
                exactMove(tile(3215, 3399, 3), delay1 = 20, delay2 = 50, angle = FACE_EAST)
                anim("seq.seaslug_ken_leap", delay = 15)
                sound(JUMP_SYNTH, delay = 15)
                next(2)
                exactMove(tile(3217, 3399, 3), delay1 = 5, delay2 = 10, angle = FACE_EAST)
                anim(WALL_JUMP)
                sound(JUMP_UP_SYNTH)
                next()
                exactMove(tile(3218, 3399, 3), delay1 = 18, delay2 = 27, angle = FACE_EAST)
                anim(WALL_JUMP_LAND)
                next()
                resetAnim()
                land()
            }
        )
    }

    row("dbrow.agility_varrock_gap_4") {
        obstacle(VARROCK, step = 6, xp = 45, "loc.rooftops_varrock_leapdown")
        lanes(lane(tile(3232, 3402, 3)) { gap(tile(3237, 3404, 3), tile(3236, 3403, 3)) })
    }

    row("dbrow.agility_varrock_ledge") {
        obstacle(VARROCK, step = 7, xp = 35, "loc.rooftops_varrock_stepuproof")
        val hurdles =
            listOf(3236, 3238).map { x ->
                lane(tile(x, 3408, 3)) {
                    exactMove(tile(x, 3410, 3), delay1 = 8, delay2 = 50, angle = FACE_NORTH)
                    anim(HURDLE)
                    sound(HURDLE_SYNTH)
                    next(2)
                    land()
                }
            }
        lanes(*hurdles.toTypedArray())
    }

    row("dbrow.agility_varrock_edge") {
        obstacle(VARROCK, step = 8, xp = 1437, "loc.rooftops_varrock_finish")
        lanes(
            lane(tile(3236, 3415, 3)) {
                exactMove(tile(3236, 3416, 3), delay1 = 15, delay2 = 30, angle = FACE_NORTH)
                anim(SPOT_JUMP)
                sound(JUMP_SYNTH)
                next()
                jumpDown(tile(3236, 3417, 0))
                land()
                next()
                resetAnim()
            }
        )
    }
}

private fun LaneBuilder.wallSwingRunUp() {
    exactMove(tile(3193, 3416, 1), delay1 = 15, delay2 = 45, angle = FACE_WEST)
    anim(RUN_UP, delay = 15)
    next()
    exactMove(tile(3190, 3414, 1), delay1 = 25, delay2 = 30, angle = FACE_WEST)
    anim(WALL_JUMP, delay = 20)
    sound(JUMP_UP_SYNTH, delay = 20)
}

private fun LaneBuilder.wallSwingHandhold(z: Int, seq: String) {
    next()
    exactMove(tile(3190, z, 1), delay1 = 34, delay2 = 52, angle = FACE_WEST)
    anim(seq)
    sound(HANDHOLDS_SYNTH, delay = 35)
    next()
}
