package org.rsmod.content.skills.agility.pack.course

import dev.openrune.definition.dbtables.DBRowBuilder
import dev.openrune.definition.dbtables.DBTableBuilder
import kotlin.math.sign
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.REQUIREMENT_CROSSBOW_GRAPPLE
import org.rsmod.content.skills.agility.pack.FACE_EAST
import org.rsmod.content.skills.agility.pack.FACE_NORTH
import org.rsmod.content.skills.agility.pack.FACE_SOUTH
import org.rsmod.content.skills.agility.pack.FACE_WEST
import org.rsmod.content.skills.agility.pack.FALL_LAND_SYNTH
import org.rsmod.content.skills.agility.pack.GRAPPLE_SWING
import org.rsmod.content.skills.agility.pack.GRAPPLING_SYNTH
import org.rsmod.content.skills.agility.pack.LOG_BALANCE
import org.rsmod.content.skills.agility.pack.Lane
import org.rsmod.content.skills.agility.pack.MITHRIL_GRAPPLE
import org.rsmod.content.skills.agility.pack.MONKEYBARS_OFF
import org.rsmod.content.skills.agility.pack.MONKEYBARS_OFF_SYNTH
import org.rsmod.content.skills.agility.pack.MONKEYBARS_ON
import org.rsmod.content.skills.agility.pack.MONKEYBARS_ON_SYNTH
import org.rsmod.content.skills.agility.pack.MONKEYBARS_SYNTH
import org.rsmod.content.skills.agility.pack.MONKEYBARS_WALK
import org.rsmod.content.skills.agility.pack.ObstacleStat
import org.rsmod.content.skills.agility.pack.PIPE_SQUEEZE
import org.rsmod.content.skills.agility.pack.PIPE_UNSQUEEZE
import org.rsmod.content.skills.agility.pack.SIDESTEP_LONG_LEFT
import org.rsmod.content.skills.agility.pack.SQUEEZE_IN_SYNTH
import org.rsmod.content.skills.agility.pack.SQUEEZE_OUT_SYNTH
import org.rsmod.content.skills.agility.pack.SWING_SYNTH
import org.rsmod.content.skills.agility.pack.TIGHTROPE_SYNTH
import org.rsmod.content.skills.agility.pack.apRange
import org.rsmod.content.skills.agility.pack.breaks
import org.rsmod.content.skills.agility.pack.lane
import org.rsmod.content.skills.agility.pack.lanes
import org.rsmod.content.skills.agility.pack.obstacle
import org.rsmod.content.skills.agility.pack.requirement
import org.rsmod.content.skills.agility.pack.startVarbit
import org.rsmod.content.skills.agility.pack.stats
import org.rsmod.content.skills.agility.pack.tile
import org.rsmod.map.CoordGrid

private const val DORGESH_KAAN = "dbrow.agility_course_dorgesh_kaan"

private const val CABLE = "loc.dorgesh_cablebalance_start"
private const val ROPE = "loc.dorgesh_caves_rope_swing"
private const val LADDER = "loc.dorgesh_caves_monkeybars_end"
private const val PYLON = "loc.dorgesh_pylon_grapple_base"

private const val LAST_ROUTE = "varbit.dorgesh_last_route"
private const val AGILITY_ROUTE = 0
private const val GRAPPLE_ROUTE = 1
private const val HEAVY_BREAKS = "enum.dorgesh_agility_heavy_breaks"
private const val DELICATE_BREAKS = "enum.dorgesh_agility_delicate_breaks"

private const val ROPE_SWING = "seq.fever_player_rope_swing_land_success"
private const val ROPE_SWAY = "seq.fever_rope_swing"
private const val XBOW_SWING = "seq.dorgesh_xbow_swing"
private const val GRAPPLE_SWING_REVERSE = "seq.dorgesh_grapple_swing_reverse"
private const val GRAPPLE_SPOT = "spotanim.dorgesh_grapple_spot"

private const val SWING_SUCCESS_SYNTH = "synth.cf_swingacross_sucess"
private const val GRAPPLE_SYNTH = "synth.dorg_crossbow_grapple"

private const val CROSSING_TILES = 11
private const val PYLON_XP = 180
private const val PYLON_STAT_LEVEL = 70
private const val PYLON_AP_RANGE = 10

internal fun DBTableBuilder.dorgeshKaanObstacles() {
    row("dbrow.agility_dorgesh_kaan_cable_1") {
        obstacle(DORGESH_KAAN, step = 0, xp = 250, CABLE)
        agilityRoute()
        lanes(
            cableLane(tile(2721, 5241, 3), tile(2721, 5230, 3)),
            // UNVERIFIED: mirrored.
            cableLane(tile(2721, 5230, 3), tile(2721, 5241, 3)),
        )
    }
    row("dbrow.agility_dorgesh_kaan_swing_1") {
        obstacle(DORGESH_KAAN, step = 1, xp = 220, ROPE)
        agilityRoute()
        lanes(
            ropeLane(tile(2728, 5228, 3), tile(2733, 5228, 3), tile(2729, 5228, 3), delay1 = 25),
            // UNVERIFIED: mirrored onto the second rope, with the captured westward swing.
            ropeLane(tile(2733, 5229, 3), tile(2728, 5229, 3), tile(2729, 5229, 3), delay1 = 24),
        )
    }
    row("dbrow.agility_dorgesh_kaan_ladder_1") {
        obstacle(DORGESH_KAAN, step = 2, xp = 250, LADDER)
        agilityRoute()
        lanes(
            ladderLane(tile(2740, 5225, 3), tile(2740, 5214, 3)),
            // UNVERIFIED: mirrored.
            ladderLane(tile(2740, 5214, 3), tile(2740, 5225, 3)),
        )
    }
    row("dbrow.agility_dorgesh_kaan_swing_2") {
        obstacle(DORGESH_KAAN, step = 3, xp = 220, ROPE)
        agilityRoute()
        lanes(
            ropeLane(tile(2732, 5216, 3), tile(2727, 5216, 3), tile(2728, 5216, 3), delay1 = 24),
            // UNVERIFIED: mirrored onto the second rope, with the captured eastward swing.
            ropeLane(tile(2727, 5215, 3), tile(2732, 5215, 3), tile(2728, 5215, 3), delay1 = 25),
        )
    }
    row("dbrow.agility_dorgesh_kaan_jutting_wall") {
        obstacle(DORGESH_KAAN, step = 4, xp = 75, "loc.dorgesh_cave_juttingwall")
        agilityRoute()
        lanes(
            juttingWallLane(tile(2721, 5215, 3), tile(2721, 5213, 3), FACE_SOUTH),
            // UNVERIFIED: mirrored.
            juttingWallLane(tile(2721, 5213, 3), tile(2721, 5215, 3), FACE_NORTH),
        )
    }
    row("dbrow.agility_dorgesh_kaan_tunnel") {
        obstacle(DORGESH_KAAN, step = 5, xp = 75, "loc.dorgesh_caves_pipe_agility2")
        agilityRoute()
        lanes(
            tunnelLane(tile(2721, 5209, 3), tile(2721, 5205, 3)),
            // UNVERIFIED: mirrored.
            tunnelLane(tile(2721, 5205, 3), tile(2721, 5209, 3)),
        )
    }
    row("dbrow.agility_dorgesh_kaan_cable_2") {
        obstacle(DORGESH_KAAN, step = 6, xp = 250, CABLE)
        agilityRoute()
        lanes(
            cableLane(tile(2720, 5205, 3), tile(2720, 5194, 3)),
            // UNVERIFIED: mirrored.
            cableLane(tile(2720, 5194, 3), tile(2720, 5205, 3)),
        )
    }
    row("dbrow.agility_dorgesh_kaan_ladder_2") {
        obstacle(DORGESH_KAAN, step = 7, xp = 250, LADDER)
        agilityRoute()
        lanes(
            ladderLane(tile(2718, 5192, 3), tile(2707, 5192, 3)),
            // UNVERIFIED: mirrored.
            ladderLane(tile(2707, 5192, 3), tile(2718, 5192, 3)),
        )
    }

    row("dbrow.agility_dorgesh_kaan_pylon_1") {
        obstacle(DORGESH_KAAN, step = 0, xp = PYLON_XP, PYLON)
        grappleRoute()
        lanes(
            pylonLane(tile(2719, 5241, 3), tile(2709, 5241, 3), tile(2710, 5241, 3), FACE_WEST),
            pylonLane(tile(2709, 5241, 3), tile(2719, 5241, 3), tile(2710, 5241, 3), FACE_EAST),
        )
    }
    row("dbrow.agility_dorgesh_kaan_pylon_2") {
        obstacle(DORGESH_KAAN, step = 1, xp = PYLON_XP, PYLON)
        grappleRoute()
        lanes(
            // UNVERIFIED: mirrored.
            pylonLane(tile(2705, 5233, 3), tile(2705, 5223, 3), tile(2705, 5224, 3), FACE_SOUTH),
            pylonLane(tile(2705, 5223, 3), tile(2705, 5233, 3), tile(2705, 5224, 3), FACE_NORTH),
        )
    }
    row("dbrow.agility_dorgesh_kaan_pylon_3") {
        obstacle(DORGESH_KAAN, step = 2, xp = PYLON_XP, PYLON)
        grappleRoute()
        lanes(
            // UNVERIFIED: mirrored.
            pylonLane(tile(2704, 5221, 3), tile(2704, 5211, 3), tile(2704, 5212, 3), FACE_SOUTH),
            pylonLane(tile(2704, 5211, 3), tile(2704, 5221, 3), tile(2704, 5212, 3), FACE_NORTH),
        )
    }
}

// UNVERIFIED: live drops a player carrying a part meant for the other route off the obstacle,
// breaking it; with no capture of that fall, the part only breaks here.
private fun DBRowBuilder.agilityRoute() {
    startVarbit(LAST_ROUTE, AGILITY_ROUTE)
    breaks(DELICATE_BREAKS)
}

private fun DBRowBuilder.grappleRoute() {
    startVarbit(LAST_ROUTE, GRAPPLE_ROUTE)
    breaks(HEAVY_BREAKS)
    requirement(REQUIREMENT_CROSSBOW_GRAPPLE, MITHRIL_GRAPPLE)
    stats(
        ObstacleStat("stat.ranged", PYLON_STAT_LEVEL, PYLON_XP),
        ObstacleStat("stat.strength", PYLON_STAT_LEVEL, PYLON_XP),
    )
    apRange(PYLON_AP_RANGE)
}

// UNVERIFIED: the walk style on the cable; the capture only shows that one is set.
private fun cableLane(start: CoordGrid, end: CoordGrid): Lane =
    lane(start) {
        message("You attempt to balance on the cable...")
        for (index in 1..CROSSING_TILES) {
            if (index > 1) {
                next()
            }
            step(start.towards(end, index))
            if (index == 2) {
                balance(LOG_BALANCE)
                sound(TIGHTROPE_SYNTH, loops = 5)
            } else if (index in 3 until CROSSING_TILES) {
                sound(TIGHTROPE_SYNTH)
            }
        }
        next()
        message("...and safely reach the other side.")
        balanceOff()
        land()
    }

private fun ropeLane(start: CoordGrid, dest: CoordGrid, rope: CoordGrid, delay1: Int): Lane =
    lane(start) {
        val angle = if (dest.x > start.x) FACE_EAST else FACE_WEST
        message("You swing on the cable...")
        exactMove(dest, delay1, delay2 = 45, angle, to = start.towards(dest, 4), faceAngle = false)
        anim(ROPE_SWING)
        locAnim(rope, ROPE_SWAY)
        sound(SWING_SUCCESS_SYNTH)
        sound(SWING_SYNTH)
        next(3)
        message("...and safely reach the other side.")
        land()
    }

private fun ladderLane(start: CoordGrid, end: CoordGrid): Lane =
    lane(start) {
        for (index in 1..CROSSING_TILES) {
            if (index > 1) {
                next()
            }
            step(start.towards(end, index))
            when (index) {
                2 -> {
                    message("You swing hand-over-hand across the fallen ladder...")
                    anim(MONKEYBARS_ON)
                    balance(MONKEYBARS_WALK)
                    sound(MONKEYBARS_ON_SYNTH)
                }
                3 -> {
                    sound(MONKEYBARS_SYNTH, loops = 4, delay = 40)
                    sound(MONKEYBARS_SYNTH, loops = 2, delay = 15)
                }
                CROSSING_TILES -> sound(MONKEYBARS_SYNTH, loops = 2)
                1 -> Unit
                else -> sound(MONKEYBARS_SYNTH, loops = 2, delay = 15)
            }
        }
        next()
        message("...and safely reach the other side.")
        anim(MONKEYBARS_OFF)
        balanceOff()
        sound(MONKEYBARS_OFF_SYNTH)
        land()
    }

private fun juttingWallLane(start: CoordGrid, dest: CoordGrid, angle: Int): Lane =
    lane(start) {
        exactMove(dest, delay1 = 28, delay2 = 124, angle, faceAngle = false)
        anim(SIDESTEP_LONG_LEFT)
        sound(SQUEEZE_IN_SYNTH)
        next(5)
        message("You squeeze past the jutting wall.")
        sound(SQUEEZE_OUT_SYNTH)
        land()
    }

private fun tunnelLane(start: CoordGrid, dest: CoordGrid): Lane =
    lane(start) {
        message("You squeeze through the narrow tunnel.")
        anim(PIPE_SQUEEZE)
        next()
        telejump(dest)
        anim(PIPE_UNSQUEEZE)
        next()
        land()
    }

private fun pylonLane(start: CoordGrid, end: CoordGrid, grapple: CoordGrid, angle: Int): Lane =
    lane(start) {
        val forward = start.chebyshevDistance(grapple) < end.chebyshevDistance(grapple)
        message("You fire your grapple at the pylon...")
        faceAngle(angle)
        next()
        anim(XBOW_SWING, delay = 6)
        spotanim(GRAPPLE_SPOT, delay = 6, height = 82)
        locAnim(grapple, if (forward) GRAPPLE_SWING else GRAPPLE_SWING_REVERSE)
        sound(GRAPPLING_SYNTH)
        sound(GRAPPLE_SYNTH, delay = 6)
        next(3)
        exactMove(start.towards(end, 5), delay1 = 15, delay2 = 29, angle, faceAngle = false)
        next()
        exactMove(end, delay1 = 0, delay2 = 15, angle, faceAngle = false)
        next()
        message("...and swing safely to the other side.")
        sound(FALL_LAND_SYNTH)
        land()
    }

private fun CoordGrid.towards(other: CoordGrid, tiles: Int): CoordGrid =
    translate((other.x - x).sign * tiles, (other.z - z).sign * tiles)
