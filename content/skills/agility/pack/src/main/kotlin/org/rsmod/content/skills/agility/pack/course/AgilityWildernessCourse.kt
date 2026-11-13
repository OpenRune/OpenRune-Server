package org.rsmod.content.skills.agility.pack.course

import dev.openrune.definition.dbtables.DBTableBuilder
import org.rsmod.content.skills.agility.pack.CLIMBING
import org.rsmod.content.skills.agility.pack.CLIMBING_SYNTH
import org.rsmod.content.skills.agility.pack.DOUBLE_PIPE_SQUEEZE
import org.rsmod.content.skills.agility.pack.FACE_NORTH
import org.rsmod.content.skills.agility.pack.FACE_WEST
import org.rsmod.content.skills.agility.pack.FallDamage
import org.rsmod.content.skills.agility.pack.JUMP_SYNTH
import org.rsmod.content.skills.agility.pack.LOG_BALANCE
import org.rsmod.content.skills.agility.pack.LOG_MESSAGE
import org.rsmod.content.skills.agility.pack.LOG_SYNTH
import org.rsmod.content.skills.agility.pack.LaneBuilder
import org.rsmod.content.skills.agility.pack.ROPE_SWING
import org.rsmod.content.skills.agility.pack.SPOT_JUMP
import org.rsmod.content.skills.agility.pack.SQUEEZE_IN_SYNTH
import org.rsmod.content.skills.agility.pack.STUMBLE
import org.rsmod.content.skills.agility.pack.STUMBLE_SYNTH
import org.rsmod.content.skills.agility.pack.SWING_MESSAGE
import org.rsmod.content.skills.agility.pack.SWING_SYNTH
import org.rsmod.content.skills.agility.pack.fails
import org.rsmod.content.skills.agility.pack.lane
import org.rsmod.content.skills.agility.pack.lanes
import org.rsmod.content.skills.agility.pack.obstacle
import org.rsmod.content.skills.agility.pack.tile

private const val WILDERNESS = "dbrow.agility_course_wilderness"

private const val ROPE_SWING_MISS = "seq.human_ropeswing_long_miss"

private const val LAVA_SYNTH = "synth.sizzle"
private const val ROPE_SWING_LOC = "seq.ropeswing_long"

// UNVERIFIED: the success curve; 2 of 46 crossings failed at 99 (stones 1 of 12, ridge 1 of 11,
// rope 0 of 11, log 0 of 12). The rope and log falls, their pit tiles and damage are the wiki's.
private const val WILDERNESS_FAIL_LOW = 200
private const val WILDERNESS_FAIL_HIGH = 250
private val ROPE_AND_LOG_FALL = FallDamage(min = 1, max = 1, hpShare = 15 to 100)

// UNVERIFIED: the damage at other hitpoints; 20 at 99 is the only sample.
private val STONES_FALL = FallDamage(min = 1, max = 1, hpShare = 20 to 100)

internal fun DBTableBuilder.wildernessObstacles() {
    row("dbrow.agility_wilderness_pipe") {
        obstacle(WILDERNESS, step = 0, xp = 125, "loc.obstical_pipe2")
        lanes(
            lane(tile(3004, 3937, 0)) {
                exactMove(tile(3004, 3940, 0), delay1 = 30, delay2 = 126, angle = FACE_NORTH)
                anim(DOUBLE_PIPE_SQUEEZE, delay = 30)
                next(4)
                sound(SQUEEZE_IN_SYNTH)
                next()
                telejump(tile(3004, 3947, 0))
                next()
                exactMove(tile(3004, 3950, 0), delay1 = 30, delay2 = 126, angle = FACE_NORTH)
                anim(DOUBLE_PIPE_SQUEEZE, delay = 30)
                next(4)
                sound(SQUEEZE_IN_SYNTH)
                land()
            }
        )
    }

    row("dbrow.agility_wilderness_ropeswing") {
        obstacle(WILDERNESS, step = 1, xp = 200, "loc.obstical_ropeswing2")
        lanes(
            lane(tile(3005, 3951, 0)) {
                walkOntoRope()
                exactMove(tile(3005, 3958, 0), delay1 = 45, delay2 = 70, angle = FACE_NORTH)
                anim(ROPE_SWING, delay = 20)
                locAnim(tile(3005, 3952, 0), ROPE_SWING_LOC)
                next()
                sound(SWING_SYNTH)
                message(SWING_MESSAGE)
                land()
            }
        )
        fails(
            WILDERNESS_FAIL_LOW,
            WILDERNESS_FAIL_HIGH,
            ROPE_AND_LOG_FALL,
            lane(tile(3005, 3951, 0)) {
                walkOntoRope()
                anim(ROPE_SWING_MISS, delay = 20)
                next()
                message("You slip and fall to the pit below.")
                fall(tile(3005, 10357, 0))
            },
        )
    }

    row("dbrow.agility_wilderness_stepping_stones") {
        obstacle(WILDERNESS, step = 2, xp = 200, "loc.steppingstone1")
        lanes(
            lane(tile(3002, 3960, 0)) {
                message("You carefully start crossing the stepping stones...")
                for (x in 3001 downTo 2996) {
                    stone(x)
                    next(if (x == 2996) 1 else 2)
                }
                message("...You safely cross to the other side.")
                land()
            }
        )
        fails(
            WILDERNESS_FAIL_LOW,
            WILDERNESS_FAIL_HIGH,
            STONES_FALL,
            lane(tile(3002, 3960, 0)) {
                message("You carefully start crossing the stepping stones...")
                for (x in 3001 downTo 2999) {
                    stone(x)
                    next(2)
                }
                message("...You lose your footing and fall into the lava.")
                anim(STUMBLE)
                faceAngle(FACE_NORTH)
                sound(STUMBLE_SYNTH, loops = 10)
                next(2)
                telejump(tile(3002, 3963, 0))
                sound(LAVA_SYNTH)
                hit()
            },
        )
    }

    row("dbrow.agility_wilderness_log_balance") {
        obstacle(WILDERNESS, step = 3, xp = 200, "loc.wilderness_log_balance1")
        lanes(
            lane(tile(3002, 3945, 0)) {
                message(LOG_MESSAGE)
                step(tile(3001, 3945, 0))
                balance(LOG_BALANCE)
                sound(LOG_SYNTH, loops = 3)
                next()
                walkTo(tile(2997, 3945, 0))
                sound(LOG_SYNTH, loops = 2)
                next()
                walkTo(tile(2994, 3945, 0))
                sound(LOG_SYNTH, loops = 2)
                message("You skillfully edge across the gap.")
                next()
                balanceOff()
                land()
            }
        )
        fails(
            WILDERNESS_FAIL_LOW,
            WILDERNESS_FAIL_HIGH,
            ROPE_AND_LOG_FALL,
            lane(tile(3002, 3945, 0)) {
                message(LOG_MESSAGE)
                step(tile(3001, 3945, 0))
                balance(LOG_BALANCE)
                next()
                balanceOff()
                fall(tile(3001, 10346, 0))
            },
        )
    }

    row("dbrow.agility_wilderness_rocks") {
        obstacle(WILDERNESS, step = 4, xp = 0, "loc.wildclimbingrock")
        lanes(
            *(2993..2995)
                .map { x ->
                    lane(tile(x, 3937, 0)) {
                        step(tile(x, 3936, 0))
                        balance(CLIMBING)
                        sound(CLIMBING_SYNTH, loops = 7)
                        next()
                        walkTo(tile(x, 3933, 0))
                        next()
                        balanceOff()
                        message("You reach the top.")
                        land()
                    }
                }
                .toTypedArray()
        )
    }
}

private fun LaneBuilder.walkOntoRope() {
    step(tile(3005, 3952, 0))
    next()
    step(tile(3005, 3953, 0))
    next()
    face(tile(3005, 3955, 0))
    next()
}

private fun LaneBuilder.stone(x: Int) {
    exactMove(tile(x, 3960, 0), delay1 = 15, delay2 = 30, angle = FACE_WEST)
    anim(SPOT_JUMP)
    sound(JUMP_SYNTH)
}
