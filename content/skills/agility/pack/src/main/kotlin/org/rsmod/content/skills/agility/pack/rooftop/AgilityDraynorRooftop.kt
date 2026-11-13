package org.rsmod.content.skills.agility.pack.rooftop

import dev.openrune.definition.dbtables.DBTableBuilder
import org.rsmod.content.skills.agility.pack.FACE_SOUTH
import org.rsmod.content.skills.agility.pack.INTO_SIDESTEP
import org.rsmod.content.skills.agility.pack.JUMP_UP_SYNTH
import org.rsmod.content.skills.agility.pack.LEDGE_SYNTH
import org.rsmod.content.skills.agility.pack.LOG_BALANCE
import org.rsmod.content.skills.agility.pack.OUT_OF_SIDESTEP
import org.rsmod.content.skills.agility.pack.SIDESTEP
import org.rsmod.content.skills.agility.pack.TIGHTROPE_SYNTH
import org.rsmod.content.skills.agility.pack.WALL_JUMP
import org.rsmod.content.skills.agility.pack.WALL_JUMP_LAND
import org.rsmod.content.skills.agility.pack.lane
import org.rsmod.content.skills.agility.pack.lanes
import org.rsmod.content.skills.agility.pack.obstacle
import org.rsmod.content.skills.agility.pack.tile

private const val DRAYNOR = "dbrow.agility_course_draynor"

internal fun DBTableBuilder.draynorObstacles() {
    row("dbrow.agility_draynor_rough_wall") {
        obstacle(DRAYNOR, step = 0, xp = 50, "loc.rooftops_draynor_wallclimb")
        lanes(lane(tile(3103, 3279, 0)) { climbWall(tile(3102, 3279, 0), tile(3102, 3279, 3)) })
    }

    row("dbrow.agility_draynor_tightrope_1") {
        obstacle(DRAYNOR, step = 1, xp = 80, "loc.rooftops_draynor_tightrope_1")
        lanes(
            lane(tile(3099, 3277, 3)) {
                step(tile(3098, 3277, 3))
                next()
                balance(LOG_BALANCE)
                sound(TIGHTROPE_SYNTH, loops = 3)
                walkTo(tile(3094, 3277, 3))
                next()
                sound(TIGHTROPE_SYNTH, loops = 3)
                walkTo(tile(3090, 3277, 3))
                next()
                balanceOff()
                step(tile(3090, 3276, 3))
                land()
            }
        )
    }

    row("dbrow.agility_draynor_tightrope_2") {
        obstacle(DRAYNOR, step = 2, xp = 70, "loc.rooftops_draynor_tightrope_2")
        lanes(
            lane(tile(3091, 3276, 3)) {
                tightrope(
                    on = tile(3092, 3276, 3),
                    last = tile(3092, 3267, 3),
                    off = tile(3092, 3266, 3),
                    loops = 7,
                    seq = LOG_BALANCE,
                )
            }
        )
    }

    row("dbrow.agility_draynor_narrow_wall") {
        obstacle(DRAYNOR, step = 3, xp = 70, "loc.rooftops_draynor_wallcrossing")
        lanes(
            lane(tile(3089, 3265, 3)) {
                anim(INTO_SIDESTEP)
                balance(SIDESTEP)
                step(tile(3089, 3264, 3))
                next()
                sound(LEDGE_SYNTH, loops = 3)
                walkTo(tile(3089, 3262, 3))
                next()
                step(tile(3088, 3261, 3))
                next()
                balanceOff()
                anim(OUT_OF_SIDESTEP)
                land()
            }
        )
    }

    row("dbrow.agility_draynor_wall") {
        obstacle(DRAYNOR, step = 4, xp = 100, "loc.rooftops_draynor_wallscramble")
        lanes(
            lane(tile(3088, 3257, 3)) {
                exactMove(tile(3088, 3256, 3), delay1 = 25, delay2 = 30, angle = FACE_SOUTH)
                anim(WALL_JUMP, delay = 20)
                sound(JUMP_UP_SYNTH, delay = 20)
                next()
                anim(WALL_JUMP_LAND)
                next()
                exactMove(tile(3088, 3255, 3), delay1 = 17, delay2 = 26, angle = FACE_SOUTH)
                next()
                resetAnim()
                land()
            }
        )
    }

    row("dbrow.agility_draynor_gap") {
        obstacle(DRAYNOR, step = 5, xp = 40, "loc.rooftops_draynor_leapdown")
        lanes(lane(tile(3094, 3255, 3)) { gap(tile(3097, 3257, 3), tile(3096, 3256, 3)) })
    }

    row("dbrow.agility_draynor_crate") {
        obstacle(DRAYNOR, step = 6, xp = 790, "loc.rooftops_draynor_crate")
        lanes(
            lane(tile(3101, 3261, 3)) {
                jumpDown(tile(3102, 3261, 1))
                next()
                resetAnim()
                next()
                jumpDown(tile(3103, 3261, 0))
                land()
                next()
                resetAnim()
            }
        )
    }
}
