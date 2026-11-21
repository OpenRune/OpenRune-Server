package org.rsmod.content.skills.agility.pack.rooftop

import dev.openrune.definition.dbtables.DBTableBuilder
import org.rsmod.content.skills.agility.pack.FACE_EAST
import org.rsmod.content.skills.agility.pack.FACE_NORTH
import org.rsmod.content.skills.agility.pack.FACE_SOUTH
import org.rsmod.content.skills.agility.pack.FALL_LAND_SYNTH
import org.rsmod.content.skills.agility.pack.FallDamage
import org.rsmod.content.skills.agility.pack.HANDHOLDS
import org.rsmod.content.skills.agility.pack.HANDHOLDS_REVERSE
import org.rsmod.content.skills.agility.pack.HANDHOLDS_REVERSE_LAST
import org.rsmod.content.skills.agility.pack.HANDHOLDS_SYNTH
import org.rsmod.content.skills.agility.pack.HURDLE
import org.rsmod.content.skills.agility.pack.HURDLE_SYNTH
import org.rsmod.content.skills.agility.pack.JUMP_DOWN_LAND
import org.rsmod.content.skills.agility.pack.JUMP_SYNTH
import org.rsmod.content.skills.agility.pack.JUMP_UP_SYNTH
import org.rsmod.content.skills.agility.pack.RUN_UP
import org.rsmod.content.skills.agility.pack.SPOT_JUMP
import org.rsmod.content.skills.agility.pack.WALL_JUMP
import org.rsmod.content.skills.agility.pack.WALL_JUMP_LAND
import org.rsmod.content.skills.agility.pack.apRange
import org.rsmod.content.skills.agility.pack.diaryXp
import org.rsmod.content.skills.agility.pack.fails
import org.rsmod.content.skills.agility.pack.lane
import org.rsmod.content.skills.agility.pack.lanes
import org.rsmod.content.skills.agility.pack.obstacle
import org.rsmod.content.skills.agility.pack.tile

private const val POLLNIVNEACH = "dbrow.agility_course_pollnivneach"

internal fun DBTableBuilder.pollnivneachObstacles() {
    row("dbrow.agility_pollnivneach_basket") {
        obstacle(POLLNIVNEACH, step = 0, xp = 100, "loc.rooftops_pollnivneach_basket")
        lanes(
            lane(tile(3351, 2961, 0)) {
                exactMove(tile(3351, 2962, 1), delay1 = 25, delay2 = 30, angle = FACE_NORTH)
                anim(WALL_JUMP, delay = 20)
                sound(JUMP_UP_SYNTH, delay = 20)
                next()
                anim(WALL_JUMP, delay = 20)
                sound(JUMP_UP_SYNTH, delay = 20)
                next()
                telejump(tile(3351, 2964, 1))
                anim(JUMP_DOWN_LAND)
                next()
                resetAnim()
                land()
            }
        )
    }

    row("dbrow.agility_pollnivneach_market_stall") {
        obstacle(POLLNIVNEACH, step = 1, xp = 450, "loc.rooftops_pollnivneach_marketstall")
        apRange(2)
        lanes(
            lane(tile(3349, 2968, 1)) {
                exactMove(
                    tile(3350, 2967, 1),
                    delay1 = 15,
                    delay2 = 29,
                    angle = FACE_NORTH,
                    from = tile(3349, 2967, 1),
                    faceAngle = false,
                )
                anim(RUN_UP, delay = 15)
                next()
                exactMove(
                    tile(3350, 2971, 2),
                    delay1 = 8,
                    delay2 = 50,
                    angle = FACE_NORTH,
                    to = tile(3350, 2970, 2),
                )
                anim(HURDLE)
                sound(HURDLE_SYNTH)
                next(2)
                exactMove(tile(3352, 2973, 2), delay1 = 25, delay2 = 59, angle = FACE_NORTH)
                anim(WALL_JUMP)
                sound(JUMP_UP_SYNTH)
                next(2)
                telejump(tile(3352, 2973, 1))
                anim(JUMP_DOWN_LAND)
                sound(FALL_LAND_SYNTH)
                next()
                resetAnim()
                land()
            }
        )
        // UNVERIFIED: the fall, its tile and its curve, which stops failing at the wiki's 81.
        fails(
            60,
            300,
            FallDamage(min = 2, max = 2, hpShare = 1 to 17),
            lane(tile(3349, 2968, 1)) { fall(tile(3349, 2970, 0)) },
        )
    }

    row("dbrow.agility_pollnivneach_banner") {
        obstacle(POLLNIVNEACH, step = 2, xp = 650, "loc.rooftops_pollnivneach_hangingbanner")
        apRange(3)
        lanes(
            lane(tile(3354, 2976, 1)) {
                face(tile(3359, 2981, 1))
                anim(RUN_UP, delay = 15)
                next()
                exactMove(tile(3357, 2977, 2), delay1 = 35, delay2 = 59, angle = FACE_NORTH)
                anim(WALL_JUMP, delay = 10)
                sound(JUMP_UP_SYNTH, delay = 10)
                next(2)
                exactMove(tile(3360, 2977, 2), delay1 = 34, delay2 = 59, angle = FACE_NORTH)
                anim(HANDHOLDS)
                sound(HANDHOLDS_SYNTH, delay = 35)
                next(2)
                telejump(tile(3360, 2977, 1))
                anim(JUMP_DOWN_LAND)
                sound(FALL_LAND_SYNTH)
                next()
                face(tile(3361, 2978, 1))
                resetAnim()
                land()
            }
        )
    }

    row("dbrow.agility_pollnivneach_gap") {
        obstacle(POLLNIVNEACH, step = 3, xp = 350, "loc.rooftops_pollnivneach_gap")
        apRange(2)
        lanes(
            lane(tile(3362, 2977, 1)) {
                exactMove(tile(3365, 2976, 2), delay1 = 40, delay2 = 49, angle = FACE_EAST)
                anim(WALL_JUMP, delay = 15)
                sound(JUMP_UP_SYNTH, delay = 15)
                next(2)
                exactMove(tile(3366, 2976, 1), delay1 = 25, delay2 = 30, angle = FACE_EAST)
                anim(WALL_JUMP_LAND)
                next()
                resetAnim()
                land()
            }
        )
    }

    row("dbrow.agility_pollnivneach_tree") {
        obstacle(POLLNIVNEACH, step = 4, xp = 750, "loc.rooftops_pollnivneach_tree")
        apRange(2)
        lanes(
            lane(tile(3368, 2976, 1)) {
                exactMove(tile(3368, 2978, 2), delay1 = 35, delay2 = 49, angle = FACE_NORTH)
                anim(WALL_JUMP, delay = 10)
                sound(JUMP_UP_SYNTH, delay = 10)
                next(2)
                exactMove(tile(3368, 2979, 2), delay1 = 34, delay2 = 52, angle = FACE_EAST)
                anim(HANDHOLDS_REVERSE)
                sound(HANDHOLDS_SYNTH, delay = 35)
                next(2)
                exactMove(tile(3368, 2982, 2), delay1 = 34, delay2 = 52, angle = FACE_EAST)
                anim(HANDHOLDS_REVERSE_LAST)
                sound(HANDHOLDS_SYNTH, delay = 35)
                next(2)
                telejump(tile(3368, 2982, 1))
                anim(JUMP_DOWN_LAND)
                sound(FALL_LAND_SYNTH)
                next()
                face(tile(3368, 2983, 1))
                resetAnim()
                land()
            }
        )
    }

    row("dbrow.agility_pollnivneach_rough_wall") {
        obstacle(POLLNIVNEACH, step = 5, xp = 50, "loc.rooftops_pollnivneach_wallclimb")
        lanes(lane(tile(3365, 2982, 1)) { climbWall(tile(3365, 2983, 1), tile(3365, 2983, 2)) })
    }

    row("dbrow.agility_pollnivneach_monkeybars") {
        obstacle(POLLNIVNEACH, step = 6, xp = 550, "loc.rooftops_pollnivneach_monkeybars_start")
        lanes(
            lane(tile(3358, 2984, 2)) {
                balance("seq.human_monkeybars_walk")
                anim("seq.human_monkeybars_on")
                sound("synth.monkeybars_on")
                sound("synth.monkeybars_loop", loops = 13, delay = 40)
                step(tile(3358, 2985, 2))
                next()
                walkTo(tile(3358, 2991, 2))
                next(2)
                balanceOff()
                face(tile(3358, 2994, 2))
                anim("seq.human_monkeybars_off")
                sound("synth.monkeybars_off")
                next()
                land()
            }
        )
    }

    row("dbrow.agility_pollnivneach_treetop") {
        obstacle(POLLNIVNEACH, step = 7, xp = 600, "loc.rooftops_pollnivneach_treetop")
        apRange(2)
        lanes(
            lane(tile(3360, 2995, 2)) {
                exactMove(tile(3360, 2997, 3), delay1 = 8, delay2 = 50, angle = FACE_NORTH)
                anim(HURDLE)
                sound(HURDLE_SYNTH)
                next(2)
                exactMove(tile(3359, 3000, 3), delay1 = 18, delay2 = 60, angle = FACE_NORTH)
                anim(HURDLE, delay = 10)
                sound(HURDLE_SYNTH, delay = 10)
                next(2)
                telejump(tile(3359, 3000, 2))
                next()
                land()
            }
        )
    }

    row("dbrow.agility_pollnivneach_drying_line") {
        obstacle(POLLNIVNEACH, step = 8, xp = 5400, "loc.rooftops_pollnivneach_line")
        apRange(2)
        lanes(
            lane(tile(3362, 3002, 2)) {
                face(tile(3363, 2997, 2))
                next()
                exactMove(tile(3363, 3000, 3), delay1 = 15, delay2 = 45, angle = FACE_SOUTH)
                anim(SPOT_JUMP)
                sound(JUMP_SYNTH)
                next(2)
                jumpDown(tile(3363, 2998, 0))
                land()
                next()
                resetAnim()
            }
        )
        diaryXp("varbit.desert_diary_hard_complete", 6660)
    }
}
