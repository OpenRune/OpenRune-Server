package org.rsmod.content.skills.agility.pack.rooftop

import dev.openrune.definition.dbtables.DBTableBuilder
import org.rsmod.content.skills.agility.pack.FACE_EAST
import org.rsmod.content.skills.agility.pack.FACE_NORTH
import org.rsmod.content.skills.agility.pack.FACE_SOUTH
import org.rsmod.content.skills.agility.pack.FACE_WEST
import org.rsmod.content.skills.agility.pack.FallDamage
import org.rsmod.content.skills.agility.pack.HURDLE
import org.rsmod.content.skills.agility.pack.HURDLE_SYNTH
import org.rsmod.content.skills.agility.pack.INTO_SIDESTEP_RIGHT
import org.rsmod.content.skills.agility.pack.JUMP_DOWN
import org.rsmod.content.skills.agility.pack.JUMP_DOWN_LAND
import org.rsmod.content.skills.agility.pack.JUMP_DOWN_SYNTH
import org.rsmod.content.skills.agility.pack.JUMP_UP_SYNTH
import org.rsmod.content.skills.agility.pack.LEDGE_SYNTH
import org.rsmod.content.skills.agility.pack.LOG_BALANCE
import org.rsmod.content.skills.agility.pack.LaneBuilder
import org.rsmod.content.skills.agility.pack.RUN_UP
import org.rsmod.content.skills.agility.pack.SIDESTEP_RIGHT
import org.rsmod.content.skills.agility.pack.TIGHTROPE_SYNTH
import org.rsmod.content.skills.agility.pack.WALL_JUMP
import org.rsmod.content.skills.agility.pack.diaryXp
import org.rsmod.content.skills.agility.pack.fails
import org.rsmod.content.skills.agility.pack.lane
import org.rsmod.content.skills.agility.pack.lanes
import org.rsmod.content.skills.agility.pack.obstacle
import org.rsmod.content.skills.agility.pack.tile
import org.rsmod.map.CoordGrid

private const val RELLEKKA = "dbrow.agility_course_rellekka"

// UNVERIFIED: the fall. The success curve stops failing at the wiki's 85; the damage is the wiki's
// tenth of current hitpoints.
internal fun DBTableBuilder.rellekkaObstacles() {
    row("dbrow.agility_rellekka_rough_wall") {
        obstacle(RELLEKKA, step = 0, xp = 200, "loc.rooftops_rellekka_wallclimb")
        lanes(lane(tile(2625, 3677, 0)) { climbWall(tile(2625, 3676, 0), tile(2625, 3676, 3)) })
    }

    row("dbrow.agility_rellekka_gap_1") {
        obstacle(RELLEKKA, step = 1, xp = 300, "loc.rooftops_rellekka_gap_1")
        lanes(lane(tile(2622, 3672, 3)) { runUpHurdle(tile(2622, 3668, 3), FACE_SOUTH) })
    }

    row("dbrow.agility_rellekka_tightrope_1") {
        obstacle(RELLEKKA, step = 2, xp = 400, "loc.rooftops_rellekka_tightrope_1")
        lanes(
            lane(tile(2622, 3658, 3)) {
                next()
                balance(LOG_BALANCE)
                sound(TIGHTROPE_SYNTH, loops = 3)
                step(tile(2623, 3657, 3))
                next()
                walkTo(tile(2626, 3654, 3))
                next()
                balanceOff()
                step(tile(2627, 3654, 3))
                land()
            }
        )
        fails(
            0,
            300,
            FallDamage(min = 0, max = 0, hpShare = 1 to 10),
            lane(tile(2622, 3658, 3)) { fall(tile(2624, 3656, 0)) },
        )
    }

    row("dbrow.agility_rellekka_gap_2") {
        obstacle(RELLEKKA, step = 3, xp = 850, "loc.rooftops_rellekka_gap_2")
        lanes(
            lane(tile(2629, 3655, 3)) {
                exactMove(
                    tile(2629, 3658, 3),
                    delay1 = 25,
                    delay2 = 30,
                    angle = FACE_NORTH,
                    faceAngle = false,
                )
                anim(WALL_JUMP, delay = 20)
                sound(JUMP_UP_SYNTH, delay = 20)
                next()
                anim(INTO_SIDESTEP_RIGHT)
                balance(SIDESTEP_RIGHT)
                faceAngle(FACE_EAST)
                sound(LEDGE_SYNTH, loops = 4)
                next()
                walkTo(tile(2634, 3658, 3))
                sound(LEDGE_SYNTH, loops = 2, delay = 10)
                next()
                step(tile(2635, 3658, 3))
                next()
                balance(LOG_BALANCE)
                sound(TIGHTROPE_SYNTH, loops = 4)
                step(tile(2636, 3657, 3))
                next()
                walkTo(tile(2639, 3654, 3))
                next()
                step(tile(2639, 3653, 3))
                next()
                balanceOff()
                next()
                land()
            }
        )
    }

    row("dbrow.agility_rellekka_gap_3") {
        obstacle(RELLEKKA, step = 4, xp = 250, "loc.rooftops_rellekka_gap_3")
        lanes(lane(tile(2643, 3653, 3)) { runUpHurdle(tile(2643, 3657, 3), FACE_NORTH) })
    }

    row("dbrow.agility_rellekka_tightrope_2") {
        obstacle(RELLEKKA, step = 5, xp = 1050, "loc.rooftops_rellekka_tightrope_3")
        lanes(
            lane(tile(2647, 3662, 3)) {
                faceAngle(1280)
                next()
                balance(LOG_BALANCE)
                sound(TIGHTROPE_SYNTH, loops = 6)
                step(tile(2647, 3663, 3))
                next()
                walkTo(tile(2654, 3670, 3))
                next()
                step(tile(2655, 3670, 3))
                next()
                balanceOff()
                next()
                land()
            }
        )
    }

    row("dbrow.agility_rellekka_pile_of_fish") {
        obstacle(RELLEKKA, step = 6, xp = 4750, "loc.rooftops_rellekka_dropoff")
        lanes(
            lane(tile(2655, 3676, 3)) {
                faceAngle(FACE_WEST)
                anim(JUMP_DOWN, delay = 15)
                sound(JUMP_DOWN_SYNTH, delay = 15)
                next()
                telejump(tile(2653, 3676, 0))
                anim(JUMP_DOWN_LAND)
                land()
                next()
                step(tile(2652, 3676, 0))
            }
        )
        diaryXp("varbit.fremennik_diary_hard_complete", 6150)
    }
}

private fun LaneBuilder.runUpHurdle(dest: CoordGrid, angle: Int) {
    anim(RUN_UP, delay = 15)
    next()
    exactMove(dest, delay1 = 8, delay2 = 50, angle = angle, faceAngle = false)
    anim(HURDLE)
    sound(HURDLE_SYNTH)
    next(2)
    land()
}
