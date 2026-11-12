package org.rsmod.content.skills.agility.pack.rooftop

import dev.openrune.definition.dbtables.DBTableBuilder
import org.rsmod.content.skills.agility.pack.FACE_EAST
import org.rsmod.content.skills.agility.pack.FACE_NORTH
import org.rsmod.content.skills.agility.pack.FALL_LAND_SYNTH
import org.rsmod.content.skills.agility.pack.FallDamage
import org.rsmod.content.skills.agility.pack.HANDHOLDS_REVERSE
import org.rsmod.content.skills.agility.pack.HANDHOLDS_REVERSE_LAST
import org.rsmod.content.skills.agility.pack.HANDHOLDS_SYNTH
import org.rsmod.content.skills.agility.pack.JUMP_DOWN
import org.rsmod.content.skills.agility.pack.JUMP_DOWN_LAND
import org.rsmod.content.skills.agility.pack.JUMP_DOWN_SYNTH
import org.rsmod.content.skills.agility.pack.JUMP_UP_SYNTH
import org.rsmod.content.skills.agility.pack.LOG_BALANCE
import org.rsmod.content.skills.agility.pack.RUN_UP
import org.rsmod.content.skills.agility.pack.TIGHTROPE_SYNTH
import org.rsmod.content.skills.agility.pack.WALL_JUMP
import org.rsmod.content.skills.agility.pack.apRange
import org.rsmod.content.skills.agility.pack.fails
import org.rsmod.content.skills.agility.pack.lane
import org.rsmod.content.skills.agility.pack.lanes
import org.rsmod.content.skills.agility.pack.obstacle
import org.rsmod.content.skills.agility.pack.tile

private const val AL_KHARID = "dbrow.agility_course_al_kharid"

// UNVERIFIED: Al Kharid's falls, which stop at a guessed level 30 on guessed tiles.
private val AL_KHARID_FALL = FallDamage(min = 1, max = 5)

internal fun DBTableBuilder.alKharidObstacles() {
    row("dbrow.agility_kharid_rough_wall") {
        obstacle(AL_KHARID, step = 0, xp = 120, "loc.rooftops_kharid_wallclimb")
        lanes(lane(tile(3273, 3195, 0)) { climbWall(tile(3273, 3194, 0), tile(3273, 3192, 3)) })
    }

    row("dbrow.agility_kharid_tightrope_1") {
        obstacle(AL_KHARID, step = 1, xp = 360, "loc.rooftops_kharid_tightrope_1")
        lanes(
            lane(tile(3272, 3182, 3)) {
                tightrope(
                    on = tile(3272, 3181, 3),
                    last = tile(3272, 3173, 3),
                    off = tile(3272, 3172, 3),
                    loops = 6,
                    seq = LOG_BALANCE,
                )
            }
        )
        fails(0, 861, AL_KHARID_FALL, lane(tile(3272, 3182, 3)) { fall(tile(3272, 3177, 0)) })
    }

    row("dbrow.agility_kharid_cable") {
        obstacle(AL_KHARID, step = 2, xp = 480, "loc.rooftops_kharid_rope_swing")
        lanes(
            lane(tile(3266, 3166, 3)) {
                message("You begin an almighty run-up...")
                exactMove(tile(3269, 3166, 3), delay1 = 0, delay2 = 30, angle = FACE_EAST)
                anim(RUN_UP)
                next()
                message("You gained enough momentum to swing to the other side!")
                exactMove(tile(3284, 3166, 3), delay1 = 0, delay2 = 60, angle = FACE_EAST)
                anim("seq.human_ropeswing_long")
                land()
            }
        )
    }

    row("dbrow.agility_kharid_zip_line") {
        obstacle(AL_KHARID, step = 3, xp = 480, "loc.rooftops_kharid_slide_side")
        lanes(
            lane(tile(3301, 3163, 3)) {
                anim(JUMP_DOWN, delay = 10)
                next()
                telejump(tile(3303, 3163, 1))
                anim("seq.zipline_bite")
                sound("synth.bite")
                sound("synth.deathslide_first", delay = 15)
                next()
                anim("seq.zipline_slide")
                for (x in 3304..3313) {
                    exactMove(tile(x, 3163, 1), delay1 = 0, delay2 = 30, angle = FACE_EAST)
                    next()
                }
                exactMove(tile(3314, 3163, 1), delay1 = 0, delay2 = 15, angle = FACE_EAST)
                next()
                step(tile(3315, 3163, 1))
                resetAnim()
                land()
            }
        )
        fails(0, 861, AL_KHARID_FALL, lane(tile(3301, 3163, 3)) { fall(tile(3308, 3163, 0)) })
    }

    row("dbrow.agility_kharid_tropical_tree") {
        obstacle(AL_KHARID, step = 4, xp = 120, "loc.rooftops_kharid_bamboo_tree_top")
        apRange(2)
        lanes(
            lane(tile(3318, 3165, 1)) {
                exactMove(tile(3318, 3169, 1), delay1 = 35, delay2 = 49, angle = FACE_NORTH)
                anim(WALL_JUMP)
                sound(JUMP_UP_SYNTH, delay = 10)
                next(2)
                exactMove(tile(3318, 3170, 1), delay1 = 34, delay2 = 42, angle = FACE_EAST)
                anim(HANDHOLDS_REVERSE)
                sound(HANDHOLDS_SYNTH, delay = 35)
                next(2)
                exactMove(tile(3317, 3174, 1), delay1 = 34, delay2 = 52, angle = FACE_NORTH)
                anim(HANDHOLDS_REVERSE_LAST)
                sound(HANDHOLDS_SYNTH, delay = 35)
                next(2)
                telejump(tile(3317, 3174, 2))
                anim(JUMP_DOWN_LAND)
                sound(FALL_LAND_SYNTH)
                next()
                face(tile(3317, 3176, 2))
                resetAnim()
                land()
            }
        )
    }

    row("dbrow.agility_kharid_roof_top_beams") {
        obstacle(AL_KHARID, step = 5, xp = 60, "loc.rooftops_kharid_wallclimb_2")
        lanes(lane(tile(3316, 3179, 2)) { climbWall(tile(3316, 3180, 2), tile(3316, 3180, 3)) })
    }

    row("dbrow.agility_kharid_tightrope_2") {
        obstacle(AL_KHARID, step = 6, xp = 180, "loc.rooftops_kharid_tightrope_4")
        lanes(
            lane(tile(3313, 3185, 3)) {
                step(tile(3313, 3186, 3))
                next()
                balance(LOG_BALANCE)
                sound(TIGHTROPE_SYNTH, loops = 6)
                walkTo(tile(3303, 3186, 3))
                next(2)
                step(tile(3302, 3186, 3))
                balanceOff()
                land()
                next()
                step(tile(3302, 3187, 3))
            }
        )
    }

    row("dbrow.agility_kharid_gap") {
        obstacle(AL_KHARID, step = 7, xp = 360, "loc.rooftops_kharid_leapdown")
        lanes(
            lane(tile(3300, 3192, 3)) {
                face(tile(3298, 3194, 3))
                anim(JUMP_DOWN)
                sound(JUMP_DOWN_SYNTH, delay = 15)
                next()
                telejump(tile(3299, 3194, 0))
                resetAnim()
                land()
            }
        )
    }
}
