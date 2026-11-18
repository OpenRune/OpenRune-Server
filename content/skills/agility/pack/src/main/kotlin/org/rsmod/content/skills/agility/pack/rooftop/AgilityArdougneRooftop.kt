package org.rsmod.content.skills.agility.pack.rooftop

import dev.openrune.definition.dbtables.DBTableBuilder
import org.rsmod.content.skills.agility.pack.CLIMBING
import org.rsmod.content.skills.agility.pack.FACE_EAST
import org.rsmod.content.skills.agility.pack.FallDamage
import org.rsmod.content.skills.agility.pack.INTO_SIDESTEP
import org.rsmod.content.skills.agility.pack.JUMP_DOWN
import org.rsmod.content.skills.agility.pack.JUMP_DOWN_LAND
import org.rsmod.content.skills.agility.pack.JUMP_DOWN_SYNTH
import org.rsmod.content.skills.agility.pack.JUMP_SYNTH
import org.rsmod.content.skills.agility.pack.LEDGE_SYNTH
import org.rsmod.content.skills.agility.pack.LOG_BALANCE
import org.rsmod.content.skills.agility.pack.LaneBuilder
import org.rsmod.content.skills.agility.pack.OUT_OF_SIDESTEP
import org.rsmod.content.skills.agility.pack.SIDESTEP
import org.rsmod.content.skills.agility.pack.SPOT_JUMP
import org.rsmod.content.skills.agility.pack.TIGHTROPE_SYNTH
import org.rsmod.content.skills.agility.pack.WALL_JUMP
import org.rsmod.content.skills.agility.pack.apRange
import org.rsmod.content.skills.agility.pack.fails
import org.rsmod.content.skills.agility.pack.lane
import org.rsmod.content.skills.agility.pack.lanes
import org.rsmod.content.skills.agility.pack.obstacle
import org.rsmod.content.skills.agility.pack.tile
import org.rsmod.map.CoordGrid

private const val ARDOUGNE = "dbrow.agility_course_ardougne"

// UNVERIFIED: the falls. The success curves stop failing at the wiki's 95; the damage is a guess.
private val ARDOUGNE_FALL = FallDamage(min = 3, max = 8)

private const val JUMP_DOWN_FLAT = "seq.agility_shortcut_wall_jumpdown_noreachforward"

internal fun DBTableBuilder.ardougneObstacles() {
    row("dbrow.agility_ardougne_wooden_beams") {
        obstacle(ARDOUGNE, step = 0, xp = 430, "loc.rooftops_ardy_wallclimb")
        lanes(
            lane(tile(2673, 3298, 0)) {
                face(tile(2673, 3299, 0))
                anim(CLIMBING, delay = 15)
                next()
                telejump(tile(2673, 3298, 1))
                anim(CLIMBING)
                next()
                telejump(tile(2673, 3298, 2))
                anim(CLIMBING)
                next()
                telejump(tile(2671, 3299, 3))
                anim(JUMP_DOWN_LAND)
                next()
                resetAnim()
                land()
            }
        )
    }

    row("dbrow.agility_ardougne_gap_1") {
        obstacle(ARDOUGNE, step = 1, xp = 650, "loc.rooftops_ardy_jump")
        lanes(
            lane(tile(2671, 3309, 3)) {
                faceAngle(663)
                next()
                jumpAcross(tile(2667, 3311, 1))
                faceAngle(872)
                next()
                jumpAcross(tile(2665, 3315, 1))
                next()
                jumpAcross(tile(2665, 3318, 3), WALL_JUMP)
                next()
                land()
            }
        )
        fails(0, 266, ARDOUGNE_FALL, lane(tile(2671, 3309, 3)) { fall(tile(2668, 3314, 0)) })
    }

    row("dbrow.agility_ardougne_plank") {
        obstacle(ARDOUGNE, step = 2, xp = 500, "loc.rooftops_ardy_plank")
        lanes(
            lane(tile(2662, 3318, 3)) {
                next()
                balance(LOG_BALANCE)
                sound(TIGHTROPE_SYNTH, loops = 4)
                step(tile(2661, 3318, 3))
                next()
                walkTo(tile(2656, 3318, 3))
                balanceOff()
                next()
                land()
            }
        )
    }

    row("dbrow.agility_ardougne_gap_2") {
        obstacle(ARDOUGNE, step = 3, xp = 210, "loc.rooftops_ardy_jump_2")
        lanes(
            lane(tile(2654, 3318, 3)) {
                faceAngle(79)
                jumpAcross(tile(2653, 3314, 3))
                next()
                resetAnim()
                land()
            }
        )
        fails(0, 266, ARDOUGNE_FALL, lane(tile(2654, 3318, 3)) { fall(tile(2653, 3316, 0)) })
    }

    row("dbrow.agility_ardougne_gap_3") {
        obstacle(ARDOUGNE, step = 4, xp = 280, "loc.rooftops_ardy_jump_3")
        lanes(
            lane(tile(2653, 3310, 3)) {
                faceAngle(360)
                next()
                jumpAcross(tile(2651, 3309, 3), JUMP_DOWN_FLAT)
                next()
                resetAnim()
                land()
            }
        )
    }

    row("dbrow.agility_ardougne_steep_roof") {
        obstacle(ARDOUGNE, step = 5, xp = 570, "loc.rooftops_ardy_wallcrossing")
        lanes(
            lane(tile(2653, 3300, 3)) {
                anim(INTO_SIDESTEP)
                balance(SIDESTEP)
                step(tile(2654, 3299, 3))
                next()
                sound(LEDGE_SYNTH, loops = 3)
                step(tile(2655, 3298, 3))
                next()
                step(tile(2656, 3297, 3))
                next(2)
                balanceOff()
                anim(OUT_OF_SIDESTEP)
                land()
            }
        )
    }

    row("dbrow.agility_ardougne_gap_4") {
        obstacle(ARDOUGNE, step = 6, xp = 6250, "loc.rooftops_ardy_jump_4")
        apRange(2)
        lanes(
            lane(tile(2656, 3297, 3)) {
                faceAngle(FACE_EAST)
                jumpAcross(tile(2658, 3298, 1))
                next(2)
                walkTo(tile(2661, 3298, 1))
                next(3)
                exactMove(
                    tile(2663, 3297, 1),
                    delay1 = 15,
                    delay2 = 30,
                    angle = FACE_EAST,
                    faceAngle = false,
                )
                anim(SPOT_JUMP)
                sound(JUMP_SYNTH)
                next()
                walkTo(tile(2666, 3297, 1))
                next(3)
                exactMove(tile(2667, 3297, 1), delay1 = 15, delay2 = 30, angle = FACE_EAST)
                anim(SPOT_JUMP)
                sound(JUMP_SYNTH)
                next()
                anim(JUMP_DOWN)
                sound(JUMP_DOWN_SYNTH)
                next()
                telejump(tile(2668, 3297, 0))
                anim(JUMP_DOWN_LAND)
                next()
                resetAnim()
                land()
            }
        )
    }
}

private fun LaneBuilder.jumpAcross(dest: CoordGrid, seq: String = JUMP_DOWN) {
    anim(seq, delay = 15)
    sound(JUMP_DOWN_SYNTH, delay = 15)
    next()
    telejump(dest)
    anim(JUMP_DOWN_LAND)
}
