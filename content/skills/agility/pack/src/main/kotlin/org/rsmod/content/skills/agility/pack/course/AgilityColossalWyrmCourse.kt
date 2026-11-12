package org.rsmod.content.skills.agility.pack.course

import dev.openrune.definition.dbtables.DBTableBuilder
import org.rsmod.content.skills.agility.pack.CLIMB
import org.rsmod.content.skills.agility.pack.FACE_EAST
import org.rsmod.content.skills.agility.pack.FACE_NORTH
import org.rsmod.content.skills.agility.pack.FACE_NORTH_WEST
import org.rsmod.content.skills.agility.pack.FACE_SOUTH
import org.rsmod.content.skills.agility.pack.FACE_WEST
import org.rsmod.content.skills.agility.pack.HURDLE
import org.rsmod.content.skills.agility.pack.HURDLE_SYNTH
import org.rsmod.content.skills.agility.pack.LOG_BALANCE
import org.rsmod.content.skills.agility.pack.LOG_SYNTH
import org.rsmod.content.skills.agility.pack.Lane
import org.rsmod.content.skills.agility.pack.LaneBuilder
import org.rsmod.content.skills.agility.pack.MONKEYBARS_OFF_SYNTH
import org.rsmod.content.skills.agility.pack.MONKEYBARS_ON_SYNTH
import org.rsmod.content.skills.agility.pack.RUN_UP
import org.rsmod.content.skills.agility.pack.TIGHTROPE_SYNTH
import org.rsmod.content.skills.agility.pack.WALK_BACK
import org.rsmod.content.skills.agility.pack.lane
import org.rsmod.content.skills.agility.pack.lanes
import org.rsmod.content.skills.agility.pack.obstacle
import org.rsmod.content.skills.agility.pack.tile
import org.rsmod.map.CoordGrid

private const val WYRM_BASIC = "dbrow.agility_course_wyrm_basic"
private const val WYRM_ADVANCED = "dbrow.agility_course_wyrm_advanced"

private const val GET_UP = "seq.human_getup"
private const val WYRM_LONG_JUMP = "seq.wyrm_agility_longjump"
private const val WYRM_WOBBLE = "seq.wyrm_agility_tightrope_wobble"
private const val WYRM_LEDGE_WALK = "seq.wyrm_agility_ledge_walk"
private const val WYRM_LEDGE_ON = "seq.wyrm_agility_ledge_on"
private const val WYRM_LEDGE_OFF = "seq.wyrm_agility_ledge_off"
private const val WYRM_ZIP_ON = "seq.wyrm_agility_zipline_on"
private const val WYRM_ZIP_CAPE_DRAG = "seq.wyrm_agility_zipline_cape_drag"
private const val WYRM_ZIP_JUMP = "seq.wyrm_agility_zipline_jump"
private const val WYRM_KNOCKDOWN = "seq.wyrm_agility_knockdown_loop"

private const val LONG_JUMP_SYNTH = "synth.varl_wyrm_agility_longjump_01"
private const val CAPE_DRAG_SYNTH = "synth.varl_wyrm_agility_cape_drag_slide_land_01"

private const val FACE_NORTH_NORTH_WEST = 871

private fun wyrmLadder(): Lane =
    lane(tile(1652, 2931, 0)) {
        anim(CLIMB)
        next()
        telejump(tile(1653, 2931, 1))
        land()
    }

private fun wyrmFirstTightrope(): Lane =
    lane(tile(1655, 2926, 1)) {
        step(tile(1655, 2925, 1))
        next()
        balance(LOG_BALANCE)
        sound(TIGHTROPE_SYNTH, loops = 4)
        walkTo(tile(1655, 2918, 1))
        next()
        balanceOff()
        exactMove(tile(1655, 2916, 1), delay1 = 15, delay2 = 60, FACE_SOUTH)
        anim(RUN_UP)
        xp(372)
        next(2)
        val hops =
            listOf(
                tile(1655, 2914, 1) to FACE_SOUTH,
                tile(1653, 2914, 1) to FACE_WEST,
                tile(1653, 2912, 1) to FACE_SOUTH,
                tile(1651, 2912, 1) to FACE_WEST,
                tile(1651, 2910, 1) to FACE_SOUTH,
                tile(1649, 2910, 1) to FACE_WEST,
            )
        for ((index, hop) in hops.withIndex()) {
            val (dest, angle) = hop
            exactMove(dest, delay1 = 20, delay2 = 60, angle, faceAngle = false)
            anim(HURDLE, delay = 10)
            sound(HURDLE_SYNTH, delay = 10)
            next(2)
            if (index < hops.lastIndex) {
                faceAngle(hops[index + 1].second)
                xp(62)
                next()
            }
        }
        land()
    }

private fun wyrmZipline(): Lane =
    lane(tile(1625, 2933, 2)) {
        step(tile(1626, 2933, 2))
        anim(WYRM_ZIP_ON)
        sound(MONKEYBARS_ON_SYNTH)
        next()
        anim(WYRM_ZIP_CAPE_DRAG)
        sound(CAPE_DRAG_SYNTH)
        for (x in 1628..1644 step 2) {
            if (x != 1628) {
                next()
            }
            exactMove(tile(x, 2933, 2), delay1 = 0, delay2 = 30, FACE_EAST, faceAngle = false)
        }
        anim(WYRM_ZIP_JUMP)
        sound(MONKEYBARS_OFF_SYNTH)
        next()
        telejump(tile(1644, 2933, 0))
        anim(WYRM_KNOCKDOWN)
        next(2)
        telejump(tile(1645, 2933, 0))
        anim(GET_UP)
        next()
        land()
    }

private fun LaneBuilder.ledge(last: CoordGrid, angle: Int, loops: Int) {
    balance(WYRM_LEDGE_WALK)
    anim(WYRM_LEDGE_ON)
    faceAngle(angle)
    sound(MONKEYBARS_ON_SYNTH)
    next()
    sound(LOG_SYNTH, loops = loops)
    walkTo(last)
    next()
    balanceOff()
    anim(WYRM_LEDGE_OFF)
    sound(MONKEYBARS_OFF_SYNTH)
    next()
}

// UNVERIFIED: the tightropes' walk style and anything the August 2026 course update changed.
internal fun DBTableBuilder.wyrmObstacles() {
    row("dbrow.agility_wyrm_basic_ladder") {
        obstacle(WYRM_BASIC, step = 0, xp = 372, "loc.varlamore_wyrm_agility_start_ladder_trigger")
        lanes(wyrmLadder())
    }
    row("dbrow.agility_wyrm_basic_tightrope") {
        obstacle(WYRM_BASIC, step = 1, xp = 62, "loc.varlamore_wyrm_agility_balance_1_trigger")
        lanes(wyrmFirstTightrope())
    }
    row("dbrow.agility_wyrm_basic_tightrope_2") {
        obstacle(
            WYRM_BASIC,
            step = 2,
            xp = 372,
            "loc.varlamore_wyrm_agility_basic_balance_1_trigger",
        )
        lanes(
            lane(tile(1647, 2910, 1)) {
                step(tile(1646, 2910, 1))
                next()
                balance(LOG_BALANCE)
                sound(TIGHTROPE_SYNTH, loops = 4)
                walkTo(tile(1640, 2910, 1))
                next()
                anim(WYRM_WOBBLE)
                next(4)
                sound(TIGHTROPE_SYNTH, loops = 3)
                walkTo(tile(1635, 2910, 1))
                next()
                balanceOff()
                faceAngle(FACE_WEST)
                land()
            }
        )
    }
    row("dbrow.agility_wyrm_basic_rope") {
        obstacle(
            WYRM_BASIC,
            step = 3,
            xp = 372,
            "loc.varlamore_wyrm_agility_basic_monkeybars_1_trigger",
        )
        lanes(
            lane(tile(1632, 2910, 1)) {
                step(tile(1631, 2910, 1))
                next()
                ledge(tile(1627, 2914, 1), FACE_NORTH_WEST, loops = 3)
                exactMove(tile(1627, 2918, 1), delay1 = 10, delay2 = 60, FACE_NORTH)
                anim(RUN_UP)
                xp(372)
                next(2)
                exactMove(tile(1627, 2923, 1), 0, 60, FACE_NORTH, faceAngle = false)
                anim(WYRM_LONG_JUMP)
                sound(LONG_JUMP_SYNTH)
                next(2)
                exactMove(tile(1627, 2926, 1), 0, 30, FACE_NORTH, faceAngle = false)
                anim(RUN_UP)
                next()
                exactMove(tile(1627, 2931, 1), 0, 60, FACE_NORTH, faceAngle = false)
                anim(WYRM_LONG_JUMP)
                sound(LONG_JUMP_SYNTH)
                next(2)
                land()
            }
        )
    }
    row("dbrow.agility_wyrm_basic_ladder_2") {
        obstacle(
            WYRM_BASIC,
            step = 4,
            xp = 372,
            "loc.varlamore_wyrm_agility_basic_ladder_1_trigger",
        )
        lanes(
            lane(tile(1626, 2932, 1)) {
                anim(CLIMB)
                next()
                telejump(tile(1625, 2932, 2))
                land()
            }
        )
    }
    row("dbrow.agility_wyrm_basic_zipline") {
        obstacle(WYRM_BASIC, step = 5, xp = 3412, "loc.varlamore_wyrm_agility_end_zipline_trigger")
        lanes(wyrmZipline())
    }

    row("dbrow.agility_wyrm_advanced_ladder") {
        obstacle(
            WYRM_ADVANCED,
            step = 0,
            xp = 372,
            "loc.varlamore_wyrm_agility_start_ladder_trigger",
        )
        lanes(wyrmLadder())
    }
    row("dbrow.agility_wyrm_advanced_tightrope") {
        obstacle(WYRM_ADVANCED, step = 1, xp = 62, "loc.varlamore_wyrm_agility_balance_1_trigger")
        lanes(wyrmFirstTightrope())
    }
    row("dbrow.agility_wyrm_advanced_ladder_2") {
        obstacle(
            WYRM_ADVANCED,
            step = 2,
            xp = 700,
            "loc.varlamore_wyrm_agility_advanced_ladder_1_trigger",
        )
        lanes(
            lane(tile(1648, 2909, 1)) {
                anim(CLIMB)
                next()
                telejump(tile(1648, 2908, 2))
                land()
            }
        )
    }
    row("dbrow.agility_wyrm_advanced_edge") {
        obstacle(
            WYRM_ADVANCED,
            step = 3,
            xp = 700,
            "loc.varlamore_wyrm_agility_advanced_jump_1_trigger",
        )
        lanes(
            lane(tile(1647, 2907, 2)) {
                exactMove(tile(1648, 2907, 2), 0, 30, FACE_WEST, faceAngle = false)
                anim(WALK_BACK)
                next()
                exactMove(tile(1647, 2907, 2), 0, 30, FACE_WEST, faceAngle = false)
                anim(RUN_UP)
                next()
                for ((index, x) in listOf(1643, 1639, 1635).withIndex()) {
                    val delay1 = if (index == 0) 8 else 0
                    exactMove(tile(x, 2907, 2), delay1, 50, FACE_WEST, faceAngle = false)
                    anim(HURDLE)
                    sound(HURDLE_SYNTH)
                    next(2)
                }
                land()
            }
        )
    }
    row("dbrow.agility_wyrm_advanced_tightrope_2") {
        obstacle(
            WYRM_ADVANCED,
            step = 4,
            xp = 700,
            "loc.varlamore_wyrm_agility_advanced_balance_1_trigger",
        )
        lanes(
            lane(tile(1634, 2908, 2)) {
                step(tile(1633, 2908, 2))
                next()
                balance(LOG_BALANCE)
                sound(TIGHTROPE_SYNTH, loops = 2)
                walkTo(tile(1630, 2911, 2))
                next()
                anim(WYRM_WOBBLE)
                faceAngle(FACE_NORTH_WEST)
                next(4)
                sound(TIGHTROPE_SYNTH, loops = 3)
                walkTo(tile(1625, 2916, 2))
                next()
                balanceOff()
                exactMove(tile(1624, 2918, 2), 15, 60, FACE_NORTH_NORTH_WEST, faceAngle = false)
                anim(RUN_UP, delay = 15)
                xp(700)
                next(2)
                ledge(tile(1624, 2931, 2), FACE_NORTH, loops = 9)
                faceAngle(FACE_NORTH)
                land()
            }
        )
    }
    row("dbrow.agility_wyrm_advanced_zipline") {
        obstacle(
            WYRM_ADVANCED,
            step = 5,
            xp = 6620,
            "loc.varlamore_wyrm_agility_end_zipline_trigger",
        )
        lanes(wyrmZipline())
    }
}
