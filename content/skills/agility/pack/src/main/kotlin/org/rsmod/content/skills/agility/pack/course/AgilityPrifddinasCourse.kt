package org.rsmod.content.skills.agility.pack.course

import dev.openrune.definition.dbtables.DBTableBuilder
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.RECOVERY_STEP
import org.rsmod.content.skills.agility.pack.CLIMB
import org.rsmod.content.skills.agility.pack.FACE_NORTH
import org.rsmod.content.skills.agility.pack.FallDamage
import org.rsmod.content.skills.agility.pack.HURDLE
import org.rsmod.content.skills.agility.pack.HURDLE_SYNTH
import org.rsmod.content.skills.agility.pack.JUMP_UP_SYNTH
import org.rsmod.content.skills.agility.pack.LOG_BALANCE
import org.rsmod.content.skills.agility.pack.LOG_SYNTH
import org.rsmod.content.skills.agility.pack.LaneBuilder
import org.rsmod.content.skills.agility.pack.PICKUP_FLOOR
import org.rsmod.content.skills.agility.pack.TIGHTROPE_SYNTH
import org.rsmod.content.skills.agility.pack.WALL_JUMP
import org.rsmod.content.skills.agility.pack.WALL_JUMP_LAND
import org.rsmod.content.skills.agility.pack.apRange
import org.rsmod.content.skills.agility.pack.fails
import org.rsmod.content.skills.agility.pack.lane
import org.rsmod.content.skills.agility.pack.lanes
import org.rsmod.content.skills.agility.pack.obstacle
import org.rsmod.content.skills.agility.pack.tile
import org.rsmod.map.CoordGrid

private const val PRIFDDINAS = "dbrow.agility_course_prifddinas"

// UNVERIFIED: the first tightrope's and the chimney's falls. The success curves stop failing
// at the wiki's 91.
private const val FAIL_LOW = 41
private const val FAIL_HIGH = 274
private val FALL = FallDamage(min = 1, max = 3)

/**
 * Live runs the treetop half of the course on the 2240-2270 copy of the map, and a fall off its
 * first tightrope lands in the city's copy below it.
 */
internal fun DBTableBuilder.prifddinasObstacles() {
    row("dbrow.agility_prifddinas_ladder") {
        obstacle(PRIFDDINAS, step = 0, xp = 115, "loc.prif_agility_start_ladder")
        lanes(
            lane(tile(3253, 6109, 0)) {
                anim(CLIMB)
                next()
                telejump(tile(3255, 6109, 2))
                land()
            }
        )
    }

    row("dbrow.agility_prifddinas_tightrope_1") {
        obstacle(PRIFDDINAS, step = 1, xp = 307, "loc.prif_agility_tightrope_start1")
        lanes(
            lane(tile(3257, 6106, 2)) {
                step(tile(3257, 6105, 2))
                next()
                step(tile(3258, 6105, 2))
                balance(LOG_BALANCE)
                sound(TIGHTROPE_SYNTH, loops = 6)
                next()
                walkTo(tile(3264, 6105, 2))
                sound(TIGHTROPE_SYNTH, loops = 9)
                next()
                walkTo(tile(3272, 6105, 2))
                next()
                balanceOff()
                land()
            }
        )
        fails(
            FAIL_LOW,
            FAIL_HIGH,
            FALL,
            lane(tile(3257, 6106, 2)) {
                step(tile(3257, 6105, 2))
                next()
                step(tile(3258, 6105, 2))
                balance(LOG_BALANCE)
                sound(TIGHTROPE_SYNTH, loops = 6)
                next()
                walkTo(tile(3264, 6105, 2))
                next()
                stumble(tile(3264, 6104, 0))
            },
        )
    }

    row("dbrow.agility_prifddinas_chimney") {
        obstacle(PRIFDDINAS, step = 2, xp = 281, "loc.prif_agility_chimney_jump")
        apRange(2)
        lanes(
            lane(tile(3272, 6105, 2)) {
                chimneyMove(tile(3273, 6106, 2), delay1 = 25, delay2 = 30, angle = FACE_NORTH)
                anim(WALL_JUMP, delay = 20)
                sound(JUMP_UP_SYNTH, delay = 20)
                next()
                anim(WALL_JUMP_LAND)
                next()
                chimneyMove(
                    tile(3273, 6107, 2),
                    delay1 = 17,
                    delay2 = 26,
                    angle = FACE_NORTH,
                    to = tile(3273, 6106, 2),
                )
                next()
                face(tile(3269, 6112, 2))
                resetAnim()
                next()
                chimneyMove(tile(3269, 6112, 2), delay1 = 8, delay2 = 50, angle = 803)
                anim(HURDLE)
                sound(HURDLE_SYNTH)
                next(2)
                land()
            }
        )
        fails(
            FAIL_LOW,
            FAIL_HIGH,
            FALL,
            lane(tile(3272, 6105, 2)) {
                chimneyMove(tile(3273, 6106, 2), delay1 = 25, delay2 = 30, angle = FACE_NORTH)
                anim(WALL_JUMP, delay = 20)
                sound(JUMP_UP_SYNTH, delay = 20)
                next()
                anim(WALL_JUMP_LAND)
                next()
                stumble(tile(3273, 6108, 0))
            },
        )
    }

    row("dbrow.agility_prifddinas_roof_edge") {
        obstacle(PRIFDDINAS, step = 3, xp = 230, "loc.prif_agility_roof_jump")
        lanes(
            lane(tile(3269, 6115, 2)) {
                jumpDown(tile(3269, 6117, 0))
                next()
                resetAnim()
                land()
            }
        )
    }

    row("dbrow.agility_prifddinas_dark_hole") {
        obstacle(PRIFDDINAS, step = 4, xp = 115, "loc.prif_agility_dark_hole_active")
        lanes(lane(tile(3269, 6117, 0)) { darkHole(tile(2269, 3389, 0), tile(2269, 3390, 0)) })
    }

    row("dbrow.agility_prifddinas_ladder_2") {
        obstacle(PRIFDDINAS, step = 5, xp = 0, "loc.prif_agility_tree_ladder_long")
        lanes(
            lane(tile(2271, 3393, 0)) {
                anim(CLIMB)
                next()
                telejump(tile(2269, 3393, 2))
                land()
            }
        )
    }

    row("dbrow.agility_prifddinas_rope_bridge_1") {
        obstacle(PRIFDDINAS, step = 6, xp = 256, "loc.prif_agility_rope_bridge1")
        lanes(lane(tile(2265, 3390, 2)) { ropeBridge(tile(2264, 3390, 2), tile(2257, 3390, 2)) })
    }

    row("dbrow.agility_prifddinas_tightrope_2") {
        obstacle(PRIFDDINAS, step = 7, xp = 307, "loc.prif_agility_tightrope1")
        lanes(
            lane(tile(2255, 3390, 2)) {
                step(tile(2254, 3390, 2))
                next()
                step(tile(2253, 3391, 2))
                balance(LOG_BALANCE)
                sound(TIGHTROPE_SYNTH, loops = 3)
                next()
                walkTo(tile(2250, 3394, 2))
                sound(TIGHTROPE_SYNTH, loops = 4)
                next()
                walkTo(tile(2247, 3397, 2))
                next()
                balanceOff()
                land()
            }
        )
        fails(
            FAIL_LOW,
            FAIL_HIGH,
            FALL,
            lane(tile(2255, 3390, 2)) {
                step(tile(2254, 3390, 2))
                next()
                step(tile(2253, 3391, 2))
                balance(LOG_BALANCE)
                sound(TIGHTROPE_SYNTH, loops = 3)
                next()
                walkTo(tile(2251, 3393, 2))
                next()
                stumble(tile(3275, 6146, 0))
            },
        )
    }

    row("dbrow.agility_prifddinas_rope_bridge_2") {
        obstacle(PRIFDDINAS, step = 8, xp = 256, "loc.prif_agility_rope_bridge2")
        lanes(lane(tile(2246, 3398, 2)) { ropeBridge(tile(2246, 3399, 2), tile(2246, 3406, 2)) })
    }

    row("dbrow.agility_prifddinas_tightrope_3") {
        obstacle(PRIFDDINAS, step = 9, xp = 307, "loc.prif_agility_tightrope2")
        lanes(
            lane(tile(2244, 3408, 2)) {
                step(tile(2244, 3409, 2))
                next()
                step(tile(2244, 3410, 2))
                balance(LOG_BALANCE)
                sound(TIGHTROPE_SYNTH, loops = 7)
                next()
                walkTo(tile(2250, 3416, 2))
                next()
                balanceOff()
                land()
            }
        )
    }

    row("dbrow.agility_prifddinas_tightrope_4") {
        obstacle(PRIFDDINAS, step = 10, xp = 307, "loc.prif_agility_tightrope3")
        lanes(
            lane(tile(2252, 3418, 2)) {
                step(tile(2253, 3418, 2))
                next()
                step(tile(2254, 3419, 2))
                balance(LOG_BALANCE)
                sound(TIGHTROPE_SYNTH, loops = 7)
                next()
                walkTo(tile(2260, 3425, 2))
                next()
                telejump(tile(2260, 3425, 0))
                balanceOff()
                land()
            }
        )
    }

    row("dbrow.agility_prifddinas_dark_hole_2") {
        obstacle(PRIFDDINAS, step = 11, xp = 10371, "loc.prif_agility_dark_hole_end")
        lanes(lane(tile(2258, 3431, 0)) { darkHole(tile(3240, 6109, 0), tile(3241, 6109, 0)) })
    }

    row("dbrow.agility_prifddinas_ladder_back") {
        obstacle(PRIFDDINAS, step = RECOVERY_STEP, xp = 0, "loc.prif_agility_tree_ladder_short")
        lanes(
            lane(tile(3267, 6144, 0)) {
                anim(CLIMB)
                next()
                telejump(tile(2243, 3394, 2))
                land()
            }
        )
    }
}

private fun LaneBuilder.darkHole(dest: CoordGrid, face: CoordGrid) {
    anim(PICKUP_FLOOR)
    next(2)
    telejump(dest)
    next(4)
    face(face)
    land()
}

private fun LaneBuilder.ropeBridge(on: CoordGrid, last: CoordGrid) {
    step(on)
    balance(LOG_BALANCE)
    sound(LOG_SYNTH, loops = 8)
    next()
    walkTo(last)
    next()
    balanceOff()
    land()
}

/** Live sends the chimney's jumps without a face angle. */
private fun LaneBuilder.chimneyMove(
    dest: CoordGrid,
    delay1: Int,
    delay2: Int,
    angle: Int,
    to: CoordGrid? = null,
) {
    exactMove(dest, delay1, delay2, angle, to = to, faceAngle = false)
}
