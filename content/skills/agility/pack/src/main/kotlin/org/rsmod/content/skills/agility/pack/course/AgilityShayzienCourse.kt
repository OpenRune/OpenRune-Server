package org.rsmod.content.skills.agility.pack.course

import dev.openrune.definition.dbtables.DBTableBuilder
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.REQUIREMENT_CROSSBOW_GRAPPLE
import org.rsmod.content.skills.agility.pack.CLIMB
import org.rsmod.content.skills.agility.pack.FACE_EAST
import org.rsmod.content.skills.agility.pack.FACE_SOUTH
import org.rsmod.content.skills.agility.pack.FACE_WEST
import org.rsmod.content.skills.agility.pack.GRAPPLE_SWING
import org.rsmod.content.skills.agility.pack.JUMP_SYNTH
import org.rsmod.content.skills.agility.pack.LOG_BALANCE
import org.rsmod.content.skills.agility.pack.LOG_SYNTH
import org.rsmod.content.skills.agility.pack.Lane
import org.rsmod.content.skills.agility.pack.LaneBuilder
import org.rsmod.content.skills.agility.pack.MITHRIL_GRAPPLE
import org.rsmod.content.skills.agility.pack.MONKEYBARS_OFF
import org.rsmod.content.skills.agility.pack.MONKEYBARS_OFF_SYNTH
import org.rsmod.content.skills.agility.pack.MONKEYBARS_ON
import org.rsmod.content.skills.agility.pack.MONKEYBARS_ON_SYNTH
import org.rsmod.content.skills.agility.pack.MONKEYBARS_WALK
import org.rsmod.content.skills.agility.pack.SPOT_JUMP
import org.rsmod.content.skills.agility.pack.TIGHTROPE_SYNTH
import org.rsmod.content.skills.agility.pack.ZIPLINE
import org.rsmod.content.skills.agility.pack.lane
import org.rsmod.content.skills.agility.pack.lanes
import org.rsmod.content.skills.agility.pack.obstacle
import org.rsmod.content.skills.agility.pack.requirement
import org.rsmod.content.skills.agility.pack.tile
import org.rsmod.map.CoordGrid

private const val SHAYZIEN_BASIC = "dbrow.agility_course_shayzien_basic"
private const val SHAYZIEN_ADVANCED = "dbrow.agility_course_shayzien_advanced"

// UNVERIFIED: Shayzien past the rope walk. Its tiles were checked in a client and its seqs are
// the cache's, but its timings are guesses; nothing fails since the wiki gives no fall tiles.
internal fun DBTableBuilder.shayzienObstacles() {
    row("dbrow.agility_shayzien_basic_ladder") {
        obstacle(SHAYZIEN_BASIC, step = 0, xp = 55, "loc.shayzien_agility_both_start_ladder")
        lanes(shayzienLadder())
    }
    row("dbrow.agility_shayzien_basic_monkeybars") {
        obstacle(SHAYZIEN_BASIC, step = 1, xp = 80, "loc.shayzien_agility_both_rope_climb")
        lanes(shayzienMonkeybars())
    }
    row("dbrow.agility_shayzien_basic_tightrope") {
        obstacle(SHAYZIEN_BASIC, step = 2, xp = 90, "loc.shayzien_agility_both_rope_walk")
        lanes(shayzienTightrope())
    }
    row("dbrow.agility_shayzien_basic_bar") {
        obstacle(SHAYZIEN_BASIC, step = 3, xp = 70, "loc.shayzien_agility_low_bar_climb")
        lanes(
            lane(tile(1523, 3639, 2)) {
                face(tile(1523, 3640, 2))
                anim(CLIMB, delay = 15)
                next(2)
                telejump(tile(1523, 3643, 3))
                resetAnim()
                land()
            }
        )
    }
    row("dbrow.agility_shayzien_basic_tightrope_2") {
        obstacle(SHAYZIEN_BASIC, step = 4, xp = 90, "loc.shayzien_agility_low_rope_walk_1")
        lanes(
            lane(tile(1524, 3644, 3)) {
                tightrope(
                    on = tile(1525, 3644, 3),
                    last = tile(1539, 3644, 3),
                    off = tile(1540, 3644, 2),
                    loops = 8,
                    seq = LOG_BALANCE,
                )
            }
        )
    }
    row("dbrow.agility_shayzien_basic_tightrope_3") {
        obstacle(SHAYZIEN_BASIC, step = 5, xp = 90, "loc.shayzien_agility_low_rope_walk_2")
        lanes(
            lane(tile(1540, 3644, 2)) {
                tightrope(
                    on = tile(1541, 3644, 2),
                    last = tile(1552, 3644, 2),
                    off = tile(1553, 3644, 2),
                    loops = 6,
                    seq = LOG_BALANCE,
                )
            }
        )
    }
    row("dbrow.agility_shayzien_basic_gap") {
        obstacle(SHAYZIEN_BASIC, step = 6, xp = 1060, "loc.shayzien_agility_low_end_jump")
        lanes(lane(tile(1554, 3642, 2)) { gap(tile(1554, 3640, 2), tile(1554, 3637, 0)) })
    }

    row("dbrow.agility_shayzien_advanced_ladder") {
        obstacle(SHAYZIEN_ADVANCED, step = 0, xp = 60, "loc.shayzien_agility_both_start_ladder")
        lanes(shayzienLadder())
    }
    row("dbrow.agility_shayzien_advanced_monkeybars") {
        obstacle(SHAYZIEN_ADVANCED, step = 1, xp = 80, "loc.shayzien_agility_both_rope_climb")
        lanes(shayzienMonkeybars())
    }
    row("dbrow.agility_shayzien_advanced_tightrope") {
        obstacle(SHAYZIEN_ADVANCED, step = 2, xp = 90, "loc.shayzien_agility_both_rope_walk")
        lanes(shayzienTightrope())
    }
    row("dbrow.agility_shayzien_advanced_beam") {
        obstacle(SHAYZIEN_ADVANCED, step = 3, xp = 230, "loc.shayzien_agility_up_swing_jump_1")
        requirement(REQUIREMENT_CROSSBOW_GRAPPLE, MITHRIL_GRAPPLE)
        lanes(lane(tile(1521, 3637, 2)) { grappleSwing(tile(1510, 3635, 2), FACE_WEST) })
    }
    row("dbrow.agility_shayzien_advanced_edge") {
        obstacle(SHAYZIEN_ADVANCED, step = 4, xp = 180, "loc.shayzien_agility_up_jump_platform_1")
        lanes(lane(tile(1510, 3635, 2)) { leap(tile(1510, 3630, 2), FACE_SOUTH) })
    }
    row("dbrow.agility_shayzien_advanced_edge_2") {
        obstacle(SHAYZIEN_ADVANCED, step = 5, xp = 210, "loc.shayzien_agility_up_jump_platform_2")
        lanes(lane(tile(1510, 3630, 2)) { leap(tile(1511, 3622, 2), FACE_SOUTH) })
    }
    row("dbrow.agility_shayzien_advanced_beam_2") {
        obstacle(SHAYZIEN_ADVANCED, step = 6, xp = 230, "loc.shayzien_agility_up_swing_jump_2")
        requirement(REQUIREMENT_CROSSBOW_GRAPPLE, MITHRIL_GRAPPLE)
        lanes(lane(tile(1511, 3622, 2)) { grappleSwing(tile(1519, 3621, 2), FACE_EAST) })
    }
    row("dbrow.agility_shayzien_advanced_zipline") {
        obstacle(SHAYZIEN_ADVANCED, step = 7, xp = 4000, "loc.shayzien_agility_up_end_jump")
        lanes(
            lane(tile(1519, 3621, 2)) {
                face(tile(1522, 3621, 2))
                anim(ZIPLINE)
                next()
                balance(ZIPLINE)
                exactMove(tile(1551, 3630, 2), delay1 = 0, delay2 = 240, angle = FACE_EAST)
                next(8)
                balanceOff()
                telejump(tile(1551, 3630, 0))
                land()
            }
        )
    }
}

private fun shayzienLadder(): Lane =
    lane(tile(1554, 3630, 0)) {
        anim(CLIMB)
        next()
        telejump(tile(1554, 3632, 3))
        land()
    }

private fun shayzienMonkeybars(): Lane =
    lane(tile(1553, 3633, 3)) {
        next()
        anim(MONKEYBARS_ON)
        balance(MONKEYBARS_WALK)
        sound(MONKEYBARS_ON_SYNTH)
        sound(LOG_SYNTH, loops = 9)
        walkTo(tile(1541, 3633, 3))
        next()
        faceAngle(FACE_WEST)
        balanceOff()
        anim(MONKEYBARS_OFF)
        sound(MONKEYBARS_OFF_SYNTH)
        next()
        telejump(tile(1541, 3633, 2))
        land()
    }

private fun shayzienTightrope(): Lane =
    lane(tile(1537, 3633, 2)) {
        step(tile(1536, 3633, 2))
        next()
        balance(LOG_BALANCE)
        sound(TIGHTROPE_SYNTH, loops = 3)
        walkTo(tile(1532, 3633, 2))
        sound(TIGHTROPE_SYNTH, loops = 4)
        next()
        walkTo(tile(1528, 3633, 2))
        next()
        balanceOff()
        land()
    }

private fun LaneBuilder.grappleSwing(dest: CoordGrid, angle: Int) {
    anim(GRAPPLE_SWING)
    next(2)
    exactMove(dest, delay1 = 0, delay2 = 90, angle = angle)
    next(4)
    land()
}

private fun LaneBuilder.leap(dest: CoordGrid, angle: Int) {
    exactMove(dest, delay1 = 15, delay2 = 45, angle = angle)
    anim(SPOT_JUMP)
    sound(JUMP_SYNTH)
    next(2)
    land()
}
