package org.rsmod.content.skills.agility.pack.rooftop

import dev.openrune.definition.dbtables.DBTableBuilder
import org.rsmod.content.skills.agility.pack.FACE_SOUTH
import org.rsmod.content.skills.agility.pack.FallDamage
import org.rsmod.content.skills.agility.pack.HANDHOLDS
import org.rsmod.content.skills.agility.pack.JUMP_DOWN
import org.rsmod.content.skills.agility.pack.JUMP_DOWN_SYNTH
import org.rsmod.content.skills.agility.pack.JUMP_UP_SYNTH
import org.rsmod.content.skills.agility.pack.LOG_BALANCE
import org.rsmod.content.skills.agility.pack.LaneBuilder
import org.rsmod.content.skills.agility.pack.TIGHTROPE_SYNTH
import org.rsmod.content.skills.agility.pack.WALL_JUMP
import org.rsmod.content.skills.agility.pack.WALL_JUMP_LAND
import org.rsmod.content.skills.agility.pack.fails
import org.rsmod.content.skills.agility.pack.lane
import org.rsmod.content.skills.agility.pack.lanes
import org.rsmod.content.skills.agility.pack.obstacle
import org.rsmod.content.skills.agility.pack.tile

private const val SEERS = "dbrow.agility_course_seers"

// UNVERIFIED: the levels these stop failing at (gap 71, tightrope 79), as the curves are fitted to
// captured falls at 60-70. The damage shares assume the captured players had 99 hitpoints.
private val SEERS_GAP_FALL = FallDamage(min = 3, max = 4, hpShare = 1 to 17)
private val SEERS_TIGHTROPE_FALL = FallDamage(min = 2, max = 3, hpShare = 1 to 18)
private val SEERS_GAP_STARTS = listOf(tile(2721, 3494, 3), tile(2721, 3495, 3))

internal fun DBTableBuilder.seersObstacles() {
    row("dbrow.agility_seers_wall") {
        obstacle(SEERS, step = 0, xp = 450, "loc.rooftops_seers_wallclimb")
        lanes(
            lane(tile(2729, 3489, 0)) {
                message("You climb up the wall...")
                face(tile(2729, 3492, 0))
                anim("seq.human_climbing", delay = 15)
                next(2)
                message("...jump, and grab hold of the sign!")
                telejump(tile(2729, 3488, 1))
                anim(HANDHOLDS)
                next(2)
                telejump(tile(2729, 3491, 3))
                resetAnim()
                land()
            }
        )
    }

    row("dbrow.agility_seers_gap_1") {
        obstacle(SEERS, step = 1, xp = 200, "loc.rooftops_seers_jump")
        val leaps =
            SEERS_GAP_STARTS.map { start ->
                lane(start) {
                    seersGapFirstLeap()
                    jumpDown(tile(2713, 3494, 2))
                    next()
                    resetAnim()
                    land()
                }
            }
        lanes(*leaps.toTypedArray())
        // UNVERIFIED: a fall from the second start tile, which no capture shows.
        val falls =
            SEERS_GAP_STARTS.map { start ->
                lane(start) {
                    seersGapFirstLeap()
                    anim(JUMP_DOWN, delay = 15)
                    sound(JUMP_DOWN_SYNTH, delay = 15)
                    next()
                    drop(tile(2715, 3494, 0))
                }
            }
        fails(93, 321, SEERS_GAP_FALL, *falls.toTypedArray())
    }

    row("dbrow.agility_seers_tightrope") {
        obstacle(SEERS, step = 2, xp = 200, "loc.rooftops_seers_tightrope")
        lanes(
            lane(tile(2710, 3490, 2)) {
                seersTightropeFirstHalf()
                next()
                sound(TIGHTROPE_SYNTH, loops = 2)
                walkTo(tile(2710, 3480, 2))
                balanceOff()
                land()
            }
        )
        fails(
            112,
            292,
            SEERS_TIGHTROPE_FALL,
            lane(tile(2710, 3490, 2)) {
                seersTightropeFirstHalf()
                next()
                stumble(tile(2710, 3484, 0))
            },
        )
    }

    row("dbrow.agility_seers_gap_2") {
        obstacle(SEERS, step = 3, xp = 350, "loc.rooftops_seers_jump_1")
        val jumps =
            listOf(2710, 2712).map { x ->
                lane(tile(x, 3477, 2)) {
                    exactMove(tile(x, 3474, 2), delay1 = 25, delay2 = 30, angle = FACE_SOUTH)
                    anim(WALL_JUMP, delay = 20)
                    sound(JUMP_UP_SYNTH, delay = 20)
                    next()
                    telejump(tile(x, 3474, 3))
                    anim(WALL_JUMP_LAND)
                    next()
                    exactMove(tile(x, 3472, 3), delay1 = 17, delay2 = 26, angle = FACE_SOUTH)
                    next()
                    resetAnim()
                    land()
                }
            }
        lanes(*jumps.toTypedArray())
    }

    row("dbrow.agility_seers_gap_3") {
        obstacle(SEERS, step = 4, xp = 150, "loc.rooftops_seers_jump_2")
        lanes(lane(tile(2702, 3470, 3)) { gap(tile(2702, 3468, 3), tile(2702, 3465, 2)) })
    }

    row("dbrow.agility_seers_edge") {
        obstacle(SEERS, step = 5, xp = 4350, "loc.rooftops_seers_leapdown")
        lanes(lane(tile(2702, 3464, 2)) { gap(tile(2706, 3464, 2), tile(2704, 3464, 0)) })
    }
}

private fun LaneBuilder.seersGapFirstLeap() {
    face(tile(2718, 3495, 3))
    next()
    jumpDown(tile(2719, 3495, 2))
    next()
}

private fun LaneBuilder.seersTightropeFirstHalf() {
    step(tile(2710, 3489, 2))
    next()
    balance(LOG_BALANCE)
    sound(TIGHTROPE_SYNTH, loops = 3)
    step(tile(2710, 3487, 2))
    next()
    walkTo(tile(2710, 3484, 2))
}
