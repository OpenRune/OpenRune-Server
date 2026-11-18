package org.rsmod.content.skills.agility.pack.rooftop

import dev.openrune.definition.dbtables.DBTableBuilder
import org.rsmod.content.skills.agility.pack.FACE_EAST
import org.rsmod.content.skills.agility.pack.FACE_NORTH
import org.rsmod.content.skills.agility.pack.FACE_SOUTH
import org.rsmod.content.skills.agility.pack.FACE_WEST
import org.rsmod.content.skills.agility.pack.FALL_LAND_SYNTH
import org.rsmod.content.skills.agility.pack.FallDamage
import org.rsmod.content.skills.agility.pack.HANDHOLDS
import org.rsmod.content.skills.agility.pack.HANDHOLDS_LAST
import org.rsmod.content.skills.agility.pack.HANDHOLDS_SYNTH
import org.rsmod.content.skills.agility.pack.HURDLE
import org.rsmod.content.skills.agility.pack.JUMP_SYNTH
import org.rsmod.content.skills.agility.pack.JUMP_UP_SYNTH
import org.rsmod.content.skills.agility.pack.LOG_BALANCE
import org.rsmod.content.skills.agility.pack.SPOT_JUMP
import org.rsmod.content.skills.agility.pack.TIGHTROPE_SYNTH
import org.rsmod.content.skills.agility.pack.WALL_JUMP
import org.rsmod.content.skills.agility.pack.fails
import org.rsmod.content.skills.agility.pack.lane
import org.rsmod.content.skills.agility.pack.lanes
import org.rsmod.content.skills.agility.pack.obstacle
import org.rsmod.content.skills.agility.pack.tile

private const val FALADOR = "dbrow.agility_course_falador"

// UNVERIFIED: the level this stops failing at (66), as the curve is fitted to captured falls at
// 54-59. The damage share assumes the captured players had 99 hitpoints.
private val FALADOR_FALL = FallDamage(min = 2, max = 3, hpShare = 1 to 17)

internal fun DBTableBuilder.faladorObstacles() {
    row("dbrow.agility_falador_rough_wall") {
        obstacle(FALADOR, step = 0, xp = 110, "loc.rooftops_falador_wallclimb")
        lanes(lane(tile(3036, 3341, 0)) { climbWall(tile(3036, 3342, 0), tile(3036, 3342, 3)) })
    }

    row("dbrow.agility_falador_tightrope_1") {
        obstacle(FALADOR, step = 1, xp = 220, "loc.rooftops_falador_tightrope_1")
        lanes(
            lane(tile(3039, 3343, 3)) {
                tightrope(
                    on = tile(3040, 3343, 3),
                    last = tile(3047, 3343, 3),
                    off = tile(3047, 3344, 3),
                    loops = 5,
                    seq = LOG_BALANCE,
                )
            }
        )
    }

    row("dbrow.agility_falador_hand_holds") {
        obstacle(FALADOR, step = 2, xp = 610, "loc.rooftops_falador_handholds_start")
        lanes(
            lane(tile(3050, 3349, 3)) {
                exactMove(tile(3050, 3351, 3), delay1 = 25, delay2 = 30, angle = FACE_NORTH)
                anim(WALL_JUMP, delay = 20)
                sound(JUMP_UP_SYNTH, delay = 20)
                next()
                telejump(tile(3050, 3351, 2))
                anim(HANDHOLDS)
                sound(HANDHOLDS_SYNTH, delay = 35)
                next()
                exactMove(
                    tile(3051, 3352, 2),
                    delay1 = 13,
                    delay2 = 22,
                    angle = FACE_WEST,
                    to = tile(3050, 3352, 2),
                )
                for (z in 3353..3355) {
                    next()
                    exactMove(tile(3051, z, 2), delay1 = 34, delay2 = 52, angle = FACE_WEST)
                    anim(HANDHOLDS)
                    sound(HANDHOLDS_SYNTH, delay = 35)
                    next()
                }
                next()
                exactMove(tile(3050, 3357, 2), delay1 = 34, delay2 = 60, angle = FACE_WEST)
                anim(HANDHOLDS_LAST)
                sound(HANDHOLDS_SYNTH, delay = 35)
                next(2)
                telejump(tile(3050, 3357, 3))
                land()
            }
        )
        fails(
            203,
            281,
            FALADOR_FALL,
            lane(tile(3050, 3349, 3)) {
                exactMove(tile(3050, 3351, 3), delay1 = 25, delay2 = 30, angle = FACE_NORTH)
                anim(WALL_JUMP, delay = 20)
                sound(JUMP_UP_SYNTH, delay = 20)
                next()
                drop(tile(3050, 3351, 0))
            },
        )
    }

    row("dbrow.agility_falador_gap_1") {
        obstacle(FALADOR, step = 3, xp = 270, "loc.rooftops_falador_gap_1")
        lanes(lane(tile(3048, 3358, 3)) { hop(tile(3048, 3361, 3), FACE_NORTH, SPOT_JUMP) })
    }

    row("dbrow.agility_falador_gap_2") {
        obstacle(FALADOR, step = 4, xp = 260, "loc.rooftops_falador_gap_2")
        lanes(lane(tile(3045, 3361, 3)) { hop(tile(3041, 3361, 3), FACE_WEST, SPOT_JUMP) })
    }

    row("dbrow.agility_falador_tightrope_2") {
        obstacle(FALADOR, step = 5, xp = 610, "loc.rooftops_falador_tightrope_2")
        lanes(
            lane(tile(3035, 3361, 3)) {
                step(tile(3034, 3361, 3))
                next()
                balance(LOG_BALANCE)
                sound(TIGHTROPE_SYNTH, loops = 4)
                step(tile(3033, 3361, 3))
                next()
                walkTo(tile(3028, 3356, 3))
                next()
                walkTo(tile(3028, 3354, 3))
                balanceOff()
                land()
            }
        )
    }

    row("dbrow.agility_falador_tightrope_3") {
        obstacle(FALADOR, step = 6, xp = 530, "loc.rooftops_falador_tightrope_3")
        lanes(
            lane(tile(3027, 3353, 3)) {
                step(tile(3026, 3353, 3))
                next()
                exactMove(tile(3021, 3353, 3), delay1 = 0, delay2 = 89, angle = FACE_WEST)
                balance(LOG_BALANCE)
                anim("seq.human_walk_logbalance_loop")
                sound("synth.deathslide_first", delay = 5)
                next(3)
                step(tile(3020, 3353, 3))
                balanceOff()
                resetAnim()
                sound(FALL_LAND_SYNTH)
                land()
            }
        )
    }

    row("dbrow.agility_falador_gap_3") {
        obstacle(FALADOR, step = 7, xp = 300, "loc.rooftops_falador_gap_3")
        lanes(lane(tile(3018, 3353, 3)) { hop(tile(3018, 3349, 3), FACE_SOUTH) })
    }

    row("dbrow.agility_falador_ledge_1") {
        obstacle(FALADOR, step = 8, xp = 140, "loc.rooftops_falador_ledge_1")
        lanes(
            lane(tile(3016, 3346, 3)) { hop(tile(3014, 3346, 3), FACE_WEST) },
            lane(tile(3016, 3345, 3)) { hop(tile(3014, 3345, 3), FACE_WEST) },
        )
    }

    row("dbrow.agility_falador_ledge_2") {
        obstacle(FALADOR, step = 9, xp = 130, "loc.rooftops_falador_ledge_2")
        lanes(lane(tile(3013, 3344, 3)) { hop(tile(3013, 3342, 3), FACE_SOUTH) })
    }

    row("dbrow.agility_falador_ledge_3") {
        obstacle(
            FALADOR,
            step = 10,
            xp = 130,
            "loc.rooftops_falador_ledge_3a",
            "loc.rooftops_falador_ledge_3b",
        )
        lanes(lane(tile(3013, 3335, 3)) { hop(tile(3015, 3334, 3), FACE_EAST) })
    }

    row("dbrow.agility_falador_ledge_4") {
        obstacle(FALADOR, step = 11, xp = 140, "loc.rooftops_falador_ledge_4")
        lanes(
            lane(tile(3017, 3334, 3)) { hop(tile(3019, 3334, 3), FACE_EAST) },
            lane(tile(3017, 3333, 3)) { hop(tile(3019, 3333, 3), FACE_EAST) },
        )
    }

    row("dbrow.agility_falador_edge") {
        obstacle(FALADOR, step = 12, xp = 2410, "loc.rooftops_falador_edge")
        val edges =
            listOf(3334, 3333).map { z ->
                lane(tile(3024, z, 3)) {
                    exactMove(tile(3027, z, 3), delay1 = 15, delay2 = 30, angle = FACE_EAST)
                    anim(HURDLE)
                    sound(JUMP_SYNTH)
                    next()
                    jumpDown(tile(3029, z, 0))
                    land()
                }
            }
        lanes(*edges.toTypedArray())
    }
}
