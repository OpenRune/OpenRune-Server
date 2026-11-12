package org.rsmod.content.skills.agility.pack.rooftop

import dev.openrune.definition.dbtables.DBTableBuilder
import org.rsmod.content.skills.agility.pack.FACE_EAST
import org.rsmod.content.skills.agility.pack.FACE_NORTH
import org.rsmod.content.skills.agility.pack.FACE_WEST
import org.rsmod.content.skills.agility.pack.FallDamage
import org.rsmod.content.skills.agility.pack.JUMP_DOWN_LAND
import org.rsmod.content.skills.agility.pack.JUMP_UP_SYNTH
import org.rsmod.content.skills.agility.pack.RUN_UP
import org.rsmod.content.skills.agility.pack.WALL_JUMP
import org.rsmod.content.skills.agility.pack.WALL_JUMP_LAND
import org.rsmod.content.skills.agility.pack.fails
import org.rsmod.content.skills.agility.pack.lane
import org.rsmod.content.skills.agility.pack.lanes
import org.rsmod.content.skills.agility.pack.obstacle
import org.rsmod.content.skills.agility.pack.tile

private const val CANIFIS = "dbrow.agility_course_canifis"

// UNVERIFIED: the roll, from the one captured fall. Canifis' gap follows the wiki's curve, and the
// damage share assumes the captured player had 99 hitpoints.
private val CANIFIS_FALL = FallDamage(min = 1, max = 2, hpShare = 1 to 17)

internal fun DBTableBuilder.canifisObstacles() {
    row("dbrow.agility_canifis_tall_tree") {
        obstacle(CANIFIS, step = 0, xp = 100, "loc.rooftops_canifis_start_tree")
        val climbs =
            listOf(3508, 3505).map { x ->
                lane(tile(x, 3488, 0)) {
                    exactMove(
                        tile(3507, 3489, 0),
                        delay1 = 30,
                        delay2 = 60,
                        angle = FACE_NORTH,
                        to = tile(x, 3489, 0),
                    )
                    anim("seq.mdaughter_tree_climb_combi", delay = 60)
                    sound("synth.tree_climbing", delay = 60)
                    next(2)
                    exactMove(tile(3505, 3489, 0), delay1 = 60, delay2 = 115, angle = FACE_EAST)
                    next(4)
                    telejump(tile(3506, 3492, 2))
                    resetAnim()
                    land()
                }
            }
        lanes(*climbs.toTypedArray())
    }

    row("dbrow.agility_canifis_gap_1") {
        obstacle(CANIFIS, step = 1, xp = 80, "loc.rooftops_canifis_jump")
        lanes(
            lane(tile(3505, 3497, 2)) {
                exactMove(tile(3505, 3498, 2), delay1 = 20, delay2 = 40, angle = FACE_NORTH)
                anim(RUN_UP, delay = 20)
                next()
                jumpDown(tile(3502, 3504, 2))
                next()
                resetAnim()
                land()
            }
        )
    }

    row("dbrow.agility_canifis_gap_2") {
        obstacle(CANIFIS, step = 2, xp = 80, "loc.rooftops_canifis_jump_2")
        lanes(
            lane(tile(3498, 3504, 2)) {
                face(tile(3497, 3504, 2))
                next()
                jumpDown(tile(3493, 3504, 2))
                next()
                step(tile(3492, 3504, 2))
                resetAnim()
                next()
                land()
            }
        )
    }

    row("dbrow.agility_canifis_gap_3") {
        obstacle(CANIFIS, step = 3, xp = 100, "loc.rooftops_canifis_jump_5")
        lanes(
            lane(tile(3487, 3499, 2)) {
                exactMove(tile(3481, 3499, 2), delay1 = 25, delay2 = 30, angle = FACE_WEST)
                anim(WALL_JUMP, delay = 20)
                sound(JUMP_UP_SYNTH, delay = 20)
                next()
                anim(WALL_JUMP_LAND)
                next()
                exactMove(tile(3479, 3499, 2), delay1 = 17, delay2 = 26, angle = FACE_WEST)
                next()
                telejump(tile(3479, 3499, 3))
                resetAnim()
                land()
            }
        )
        fails(
            175,
            300,
            CANIFIS_FALL,
            lane(tile(3487, 3499, 2)) {
                exactMove(tile(3481, 3499, 2), delay1 = 25, delay2 = 30, angle = FACE_WEST)
                anim(WALL_JUMP, delay = 20)
                sound(JUMP_UP_SYNTH, delay = 20)
                next()
                drop(tile(3482, 3499, 0))
            },
        )
    }

    row("dbrow.agility_canifis_gap_4") {
        obstacle(CANIFIS, step = 4, xp = 80, "loc.rooftops_canifis_jump_3")
        lanes(
            lane(tile(3478, 3493, 3)) {
                jumpDown(tile(3478, 3486, 2))
                next()
                resetAnim()
                land()
            }
        )
    }

    row("dbrow.agility_canifis_pole_vault") {
        obstacle(CANIFIS, step = 5, xp = 100, "loc.rooftops_canifis_polevault")
        lanes(
            lane(tile(3480, 3484, 2)) {
                exactMove(
                    tile(3478, 3485, 2),
                    delay1 = 0,
                    delay2 = 30,
                    angle = FACE_EAST,
                    from = tile(3478, 3486, 2),
                    to = tile(3480, 3484, 2),
                    faceAngle = false,
                )
                anim(RUN_UP)
                next()
                exactMove(
                    tile(3482, 3481, 2),
                    delay1 = 0,
                    delay2 = 30,
                    angle = FACE_EAST,
                    to = tile(3480, 3483, 2),
                )
                anim("seq.rooftops_pole_vault")
                for (n in 1..5) {
                    next()
                    val delay2 = if (n == 5) 28 else 30
                    exactMove(tile(3482 + n, 3481 - n, 2), 0, delay2, angle = FACE_EAST)
                }
                next()
                telejump(tile(3489, 3476, 3))
                anim(JUMP_DOWN_LAND)
                next()
                resetAnim()
                land()
            }
        )
    }

    row("dbrow.agility_canifis_gap_5") {
        obstacle(CANIFIS, step = 6, xp = 110, "loc.rooftops_canifis_jump_4")
        lanes(lane(tile(3502, 3476, 3)) { gap(tile(3504, 3476, 3), tile(3510, 3476, 2)) })
    }

    row("dbrow.agility_canifis_gap_6") {
        obstacle(CANIFIS, step = 7, xp = 1750, "loc.rooftops_canifis_leapdown")
        lanes(lane(tile(3510, 3482, 2)) { gap(tile(3510, 3484, 2), tile(3510, 3485, 0)) })
    }
}
