package org.rsmod.content.skills.agility.pack.arena

import dev.openrune.definition.dbtables.DBTableBuilder
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.RECOVERY_STEP
import org.rsmod.content.skills.agility.pack.CLIMB_WALL_SYNTH
import org.rsmod.content.skills.agility.pack.FACE_EAST
import org.rsmod.content.skills.agility.pack.FACE_NORTH
import org.rsmod.content.skills.agility.pack.FACE_SOUTH
import org.rsmod.content.skills.agility.pack.FACE_WEST
import org.rsmod.content.skills.agility.pack.FallDamage
import org.rsmod.content.skills.agility.pack.HANDHOLDS
import org.rsmod.content.skills.agility.pack.HANDHOLDS_GRAB_SYNTH
import org.rsmod.content.skills.agility.pack.HANDHOLDS_LAST
import org.rsmod.content.skills.agility.pack.HANDHOLDS_REVERSE
import org.rsmod.content.skills.agility.pack.HANDHOLDS_REVERSE_LAST
import org.rsmod.content.skills.agility.pack.HANDHOLDS_SYNTH
import org.rsmod.content.skills.agility.pack.INTO_SIDESTEP
import org.rsmod.content.skills.agility.pack.JUMP_DOWN_SYNTH
import org.rsmod.content.skills.agility.pack.LEDGE_SYNTH
import org.rsmod.content.skills.agility.pack.LOG_BALANCE
import org.rsmod.content.skills.agility.pack.LOG_SYNTH
import org.rsmod.content.skills.agility.pack.Lane
import org.rsmod.content.skills.agility.pack.LaneBuilder
import org.rsmod.content.skills.agility.pack.MONKEYBARS_OFF
import org.rsmod.content.skills.agility.pack.MONKEYBARS_OFF_SYNTH
import org.rsmod.content.skills.agility.pack.MONKEYBARS_ON
import org.rsmod.content.skills.agility.pack.MONKEYBARS_ON_SYNTH
import org.rsmod.content.skills.agility.pack.MONKEYBARS_SYNTH
import org.rsmod.content.skills.agility.pack.MONKEYBARS_WALK
import org.rsmod.content.skills.agility.pack.OUT_OF_SIDESTEP_RIGHT
import org.rsmod.content.skills.agility.pack.ROPE_SWING
import org.rsmod.content.skills.agility.pack.SIDESTEP
import org.rsmod.content.skills.agility.pack.SIDESTEP_RIGHT
import org.rsmod.content.skills.agility.pack.SPOT_JUMP
import org.rsmod.content.skills.agility.pack.SWING_SYNTH
import org.rsmod.content.skills.agility.pack.TIGHTROPE_SYNTH
import org.rsmod.content.skills.agility.pack.apRange
import org.rsmod.content.skills.agility.pack.falls
import org.rsmod.content.skills.agility.pack.lane
import org.rsmod.content.skills.agility.pack.lanes
import org.rsmod.content.skills.agility.pack.obstacle
import org.rsmod.content.skills.agility.pack.tile
import org.rsmod.map.CoordGrid

private const val ARENA = "dbrow.agility_course_brimhaven_arena"
private const val OFF_LAP = RECOVERY_STEP

private const val LOW_WALL = "seq.human_lowwall"
private const val HANDHOLDS_FIRST = "seq.agilityarena_handholds_first"
private const val HANDHOLDS_FIRST_REVERSE = "seq.agilityarena_handholds_firstreverse"

private const val HANDHOLDS_OFF_SYNTH = "synth.handholds_off"
private const val PLANK_SYNTH = "synth.plankwalk"

private val PLANK_FALL = FallDamage(min = 2, max = 2, hpShare = 5 to 100)

private class Heading(val dx: Int, val dz: Int, val angle: Int)

private val EAST = Heading(1, 0, FACE_EAST)
private val WEST = Heading(-1, 0, FACE_WEST)
private val NORTH = Heading(0, 1, FACE_NORTH)
private val SOUTH = Heading(0, -1, FACE_SOUTH)

private fun at(x: Int, z: Int): CoordGrid = tile(x, z, 3)

private fun CoordGrid.ahead(heading: Heading, tiles: Int): CoordGrid =
    translate(heading.dx * tiles, heading.dz * tiles)

private fun CoordGrid.path(heading: Heading, tiles: Int): List<CoordGrid> =
    (1..tiles).map { ahead(heading, it) }

/*
 * The Brimhaven Agility Arena's plain crossings between its pillars. None of them is part of a lap:
 * tagging a dispenser counts instead. Each lane follows the arena captures where a side was
 * captured; a side that was not mirrors the captured one, marked where its animation could differ.
 */
internal fun DBTableBuilder.brimhavenArenaObstacles() {
    row("dbrow.agility_arena_rope_balance") {
        obstacle(ARENA, step = OFF_LAP, xp = 100, "loc.agilityarena_ropebalance")
        lanes(
            ropeBalance(at(2772, 9558), NORTH),
            ropeBalance(at(2772, 9567), SOUTH),
            ropeBalance(at(2783, 9580), NORTH),
            ropeBalance(at(2783, 9589), SOUTH),
            ropeBalance(at(2794, 9547), NORTH),
            ropeBalance(at(2794, 9556), SOUTH),
        )
    }

    row("dbrow.agility_arena_log_balance") {
        obstacle(
            ARENA,
            step = OFF_LAP,
            xp = 120,
            "loc.agilityarena_logbalance1",
            "loc.agilityarena_logbalance3",
        )
        lanes(
            logBalance(at(2770, 9579), WEST),
            logBalance(at(2763, 9579), EAST),
            logBalance(at(2794, 9581), NORTH),
            logBalance(at(2794, 9588), SOUTH),
            logBalance(at(2805, 9555), SOUTH),
            logBalance(at(2805, 9548), NORTH),
        )
    }

    row("dbrow.agility_arena_ledge") {
        obstacle(
            ARENA,
            step = OFF_LAP,
            xp = 160,
            "loc.agilityarena_ledgebalance",
            "loc.agilityarena_ledgebalance2",
        )
        lanes(
            ledgeWest(at(2770, 9546)),
            ledgeEast(at(2763, 9546)),
            ledgeWest(at(2770, 9590)),
            ledgeEast(at(2763, 9590)),
            ledgeWest(at(2803, 9546)),
            ledgeEast(at(2796, 9546)),
        )
    }

    // UNVERIFIED: the south, north and west crossings mirror the captured ones.
    row("dbrow.agility_arena_monkey_bars") {
        obstacle(ARENA, step = OFF_LAP, xp = 140, "loc.agilityarena_monkeybars_end")
        lanes(
            monkeyBars(at(2772, 9570), NORTH),
            monkeyBars(at(2772, 9577), SOUTH),
            monkeyBars(at(2774, 9546), EAST),
            monkeyBars(at(2781, 9546), WEST),
            monkeyBars(at(2794, 9566), SOUTH),
            monkeyBars(at(2794, 9559), NORTH),
        )
    }

    // UNVERIFIED: the northbound climbs take the eastbound one's seq delay.
    row("dbrow.agility_arena_low_wall") {
        obstacle(ARENA, step = OFF_LAP, xp = 80, "loc.agilityarena_lowwall")
        lanes(
            lowWall(at(2779, 9590), WEST, seqDelay = 0),
            lowWall(at(2776, 9590), EAST, seqDelay = 30),
            lowWall(at(2783, 9564), SOUTH, seqDelay = 0),
            lowWall(at(2783, 9561), NORTH, seqDelay = 30),
            lowWall(at(2805, 9564), SOUTH, seqDelay = 0),
            lowWall(at(2805, 9561), NORTH, seqDelay = 30),
        )
    }

    // UNVERIFIED: the east side's ropes are not captured; they mirror the west side's.
    row("dbrow.agility_arena_rope_swing") {
        obstacle(ARENA, step = OFF_LAP, xp = 200, "loc.agilityarena_ropeswing")
        apRange(2)
        lanes(
            ropeSwing(rope = at(2767, 9567), WEST),
            ropeSwing(rope = at(2766, 9569), EAST),
            ropeSwing(rope = at(2804, 9584), NORTH),
            ropeSwing(rope = at(2806, 9585), SOUTH),
        )
    }

    row("dbrow.agility_arena_pillars") {
        obstacle(ARENA, step = OFF_LAP, xp = 180, "loc.agilityarena_pillar_top")
        lanes(
            pillars(at(2785, 9568), EAST),
            pillars(at(2792, 9568), WEST),
            pillars(at(2805, 9570), NORTH),
            pillars(at(2805, 9577), SOUTH),
            pillars(at(2761, 9548), NORTH),
            pillars(at(2761, 9555), SOUTH),
        )
    }

    // UNVERIFIED: the west hand holds' facing and which way their seqs run.
    row("dbrow.agility_arena_hand_holds") {
        obstacle(ARENA, step = OFF_LAP, xp = 220, "loc.agilityarena_handholds")
        lanes(
            handHolds(at(2792, 9544), WEST, FACE_SOUTH, reverse = false),
            handHolds(at(2785, 9544), EAST, FACE_SOUTH, reverse = true),
            handHolds(at(2792, 9592), WEST, FACE_NORTH, reverse = true),
            handHolds(at(2785, 9592), EAST, FACE_NORTH, reverse = false),
            handHolds(at(2759, 9559), NORTH, FACE_WEST, reverse = false),
            handHolds(at(2759, 9566), SOUTH, FACE_WEST, reverse = true),
        )
    }

    row("dbrow.agility_arena_plank") {
        obstacle(
            ARENA,
            step = OFF_LAP,
            xp = 60,
            "loc.agilityarena_plank",
            "loc.agilityarena_plank2",
            "loc.agilityarena_plank3",
        )
        val planks =
            PLANK_ROWS.flatMap { (west, z) -> listOf(at(west + 7, z) to WEST, at(west, z) to EAST) }
        lanes(*planks.map { (start, heading) -> plankWalk(start, heading) }.toTypedArray())
        val fallLanes = planks.map { (start, heading) -> plankFall(start, heading) }
        falls(PLANK_FALL, *fallLanes.toTypedArray())
    }
}

private fun ropeBalance(start: CoordGrid, heading: Heading): Lane =
    lane(start) {
        val tiles = start.path(heading, 8)
        step(tiles[0])
        next()
        balance(LOG_BALANCE)
        sound(TIGHTROPE_SYNTH, loops = 2)
        step(tiles[1])
        for (index in 2 until tiles.size) {
            next()
            step(tiles[index])
            when (index) {
                4 -> sound(TIGHTROPE_SYNTH, loops = 2)
                6 -> sound(TIGHTROPE_SYNTH)
            }
        }
        balanceOff()
        next()
        land()
    }

private fun logBalance(start: CoordGrid, heading: Heading): Lane =
    lane(start) {
        val tiles = start.path(heading, 7)
        balance(LOG_BALANCE)
        sound(LOG_SYNTH, loops = 2)
        step(tiles[0])
        for (index in 1 until tiles.size) {
            next()
            step(tiles[index])
            when (index) {
                3 -> sound(LOG_SYNTH, loops = 2)
                6 -> sound(LOG_SYNTH)
            }
        }
        balanceOff()
        next()
        land()
    }

private fun LaneBuilder.ledgeWalk(tiles: List<CoordGrid>) {
    sound(LEDGE_SYNTH, loops = 3, delay = 10)
    step(tiles[0])
    for (index in 1 until tiles.size) {
        next()
        step(tiles[index])
        if (index == 3 || index == 5) {
            sound(LEDGE_SYNTH, loops = 2)
        }
    }
}

private fun ledgeWest(start: CoordGrid): Lane =
    lane(start) {
        anim(INTO_SIDESTEP)
        balance(SIDESTEP)
        ledgeWalk(start.path(WEST, 7))
        next()
        balanceOff()
        land()
    }

// UNVERIFIED: the eastbound walk style; the capture shows only that one is set.
private fun ledgeEast(start: CoordGrid): Lane =
    lane(start) {
        balance(SIDESTEP_RIGHT)
        ledgeWalk(start.path(EAST, 7))
        next()
        anim(OUT_OF_SIDESTEP_RIGHT, delay = 20)
        next()
        balanceOff()
        land()
    }

private fun monkeyBars(start: CoordGrid, heading: Heading): Lane =
    lane(start) {
        val tiles = start.path(heading, 8)
        anim(MONKEYBARS_ON)
        balance(MONKEYBARS_WALK)
        sound(MONKEYBARS_ON_SYNTH)
        next(2)
        for ((index, tile) in tiles.withIndex()) {
            if (index > 0) {
                next()
            }
            step(tile)
            when (index) {
                0 -> sound(MONKEYBARS_SYNTH, loops = 6)
                3 -> sound(MONKEYBARS_SYNTH, loops = 4, delay = 10)
                5 -> sound(MONKEYBARS_SYNTH, loops = 3, delay = 5)
            }
        }
        anim(MONKEYBARS_OFF)
        balanceOff()
        sound(MONKEYBARS_OFF_SYNTH)
        next()
        land()
    }

private fun lowWall(start: CoordGrid, heading: Heading, seqDelay: Int): Lane =
    lane(start) {
        exactMove(start.ahead(heading, 3), 0, 144, heading.angle, faceAngle = false)
        anim(LOW_WALL, delay = seqDelay)
        sound(CLIMB_WALL_SYNTH, delay = 40)
        next(4)
        land()
    }

private fun ropeSwing(rope: CoordGrid, heading: Heading): Lane =
    lane(rope.ahead(heading, -2)) {
        exactMove(rope.ahead(heading, 3), 30, 55, heading.angle, faceAngle = false)
        anim(ROPE_SWING, delay = 5)
        sound(SWING_SYNTH, delay = 5)
        next()
        land()
    }

private fun pillars(start: CoordGrid, heading: Heading): Lane =
    lane(start) {
        for ((index, tile) in start.path(heading, 7).withIndex()) {
            if (index > 0) {
                next()
            }
            exactMove(tile, 15, 30, heading.angle, faceAngle = false)
            anim(SPOT_JUMP)
            sound(JUMP_DOWN_SYNTH)
        }
        next()
        land()
    }

private fun handHolds(start: CoordGrid, heading: Heading, angle: Int, reverse: Boolean): Lane =
    lane(start) {
        val tiles = start.path(heading, 7)
        exactMove(tiles.first(), 46, 60, angle, faceAngle = false)
        anim(if (reverse) HANDHOLDS_FIRST_REVERSE else HANDHOLDS_FIRST)
        sound(HANDHOLDS_GRAB_SYNTH)
        next()
        for (tile in tiles.subList(1, tiles.size - 1)) {
            next()
            exactMove(tile, 4, 22, angle, faceAngle = false)
            anim(if (reverse) HANDHOLDS_REVERSE else HANDHOLDS)
            sound(HANDHOLDS_SYNTH, delay = 5)
        }
        next()
        exactMove(tiles.last(), 4, 22, angle, faceAngle = false)
        anim(if (reverse) HANDHOLDS_REVERSE_LAST else HANDHOLDS_LAST)
        sound(HANDHOLDS_OFF_SYNTH)
        next()
        land()
    }

private val PLANK_ROWS = (9556..9558).map { 2763 to it } + (9589..9591).map { 2796 to it }

private fun plankWalk(start: CoordGrid, heading: Heading): Lane =
    lane(start) {
        for ((index, tile) in start.path(heading, 7).withIndex()) {
            if (index > 0) {
                next()
            }
            step(tile)
            when (index) {
                0 -> sound(PLANK_SYNTH, loops = 2, delay = 10)
                2 -> sound(PLANK_SYNTH, loops = 4, delay = 10)
            }
        }
        next()
        land()
    }

// UNVERIFIED: the whole fall. The wiki only says a broken plank drops the player, who climbs a
// rope back up; the ropes hang in the pillars' bottom level, so this lands below the start.
private fun plankFall(start: CoordGrid, heading: Heading): Lane =
    lane(start) {
        val tiles = start.path(heading, 2)
        step(tiles[0])
        sound(PLANK_SYNTH, loops = 2, delay = 10)
        next()
        step(tiles[1])
        next()
        fall(tile(start.x, start.z, 0))
    }

/**
 * The arena's traps, which go off as the player steps onto their span rather than on a click, and
 * the dispensers the arrow moves between. Xp is in tenths; a success curve is the
 * `statRandom(agility, low, high)` of the trap, and a trap without one never fails.
 */
object AgilityArenaTables {
    const val TRAP_KIND = 0
    const val TRAP_SPAN = 1
    const val TRAP_XP = 2
    const val TRAP_SUCCESS = 3
    const val TRAP_DISPENSERS = 4
    const val TRAP_LOC_ANGLE = 5
    const val TRAP_LOC = 6

    const val KIND_SPINNING_BLADES = 0
    const val KIND_FLOOR_SPIKES = 1
    const val KIND_PRESSURE_PAD = 2
    const val KIND_DARTS = 3
    const val KIND_SAW_BLADE = 4

    const val ARENA_DISPENSERS = 0
    const val ARENA_PLANKS = 1

    fun traps() =
        dbTable("dbtable.agility_arena_traps", serverOnly = true) {
            column("kind", TRAP_KIND, VarType.INT)
            column("span", TRAP_SPAN, VarType.COORDGRID)
            column("xp", TRAP_XP, VarType.INT)
            column("success", TRAP_SUCCESS, VarType.INT, VarType.INT)
            column("dispensers", TRAP_DISPENSERS, VarType.COORDGRID)
            column("loc_angle", TRAP_LOC_ANGLE, VarType.INT)
            column("loc", TRAP_LOC, VarType.COORDGRID)

            spinningBlades(
                "dbrow.agility_arena_spinning_blades_1",
                at(2777, 9557),
                EAST,
                loc = at(2777, 9556),
            )
            spinningBlades(
                "dbrow.agility_arena_spinning_blades_2",
                at(2777, 9579),
                EAST,
                loc = at(2777, 9580),
            )
            spinningBlades(
                "dbrow.agility_arena_spinning_blades_3",
                at(2783, 9573),
                NORTH,
                loc = at(2782, 9573),
            )

            floorSpikes("dbrow.agility_arena_floor_spikes_1", span(2772, 9551, NORTH))
            floorSpikes("dbrow.agility_arena_floor_spikes_2", span(2761, 9573, NORTH))
            floorSpikes("dbrow.agility_arena_floor_spikes_3", span(2799, 9568, EAST))

            pressurePad("dbrow.agility_arena_pressure_pad_1", span(2772, 9584, NORTH))
            pressurePad("dbrow.agility_arena_pressure_pad_2", span(2799, 9557, EAST))
            pressurePad("dbrow.agility_arena_pressure_pad_3", span(2799, 9579, EAST))

            darts("dbrow.agility_arena_darts_1", at(2772, 9568), at(2783, 9568), EAST)
            darts("dbrow.agility_arena_darts_2", at(2783, 9557), at(2794, 9557), EAST)
            darts("dbrow.agility_arena_darts_3", at(2794, 9568), at(2794, 9579), NORTH)

            sawBlade("dbrow.agility_arena_saw_blade_1", span(2761, 9584, NORTH), angle = 2)
            sawBlade("dbrow.agility_arena_saw_blade_2", span(2783, 9551, NORTH), angle = 2)
            sawBlade("dbrow.agility_arena_saw_blade_3", span(2788, 9579, EAST), angle = 3)
        }

    fun arena() =
        dbTable("dbtable.agility_arena", serverOnly = true) {
            column("dispensers", ARENA_DISPENSERS, VarType.COORDGRID)
            column("planks", ARENA_PLANKS, VarType.COORDGRID)

            row("dbrow.agility_arena") {
                val dispensers =
                    listOf(2761, 2772, 2783, 2794, 2805).flatMap { x ->
                        listOf(9546, 9557, 9568, 9579, 9590).map { z -> at(x, z) }
                    } - at(2805, 9590)
                column(ARENA_DISPENSERS, dispensers.map { it.packed })
                val middles =
                    PLANK_ROWS.flatMap { (west, z) -> (west + 2..west + 5).map { at(it, z) } }
                column(ARENA_PLANKS, middles.map { it.packed })
            }
        }

    private fun span(x: Int, z: Int, heading: Heading, tiles: Int = 2): List<CoordGrid> =
        (0 until tiles).map { at(x, z).ahead(heading, it) }

    private fun DBTableBuilder.floorSpikes(row: String, span: List<CoordGrid>) {
        row(row) {
            column(TRAP_KIND, KIND_FLOOR_SPIKES)
            column(TRAP_SPAN, span.map { it.packed })
            column(TRAP_XP, 240)
        }
    }

    private fun DBTableBuilder.pressurePad(row: String, span: List<CoordGrid>) {
        row(row) {
            column(TRAP_KIND, KIND_PRESSURE_PAD)
            column(TRAP_SPAN, span.map { it.packed })
            column(TRAP_XP, 260)
        }
    }

    private fun DBTableBuilder.spinningBlades(
        row: String,
        first: CoordGrid,
        heading: Heading,
        loc: CoordGrid,
    ) {
        row(row) {
            column(TRAP_KIND, KIND_SPINNING_BLADES)
            column(TRAP_SPAN, (0 until 3).map { first.ahead(heading, it).packed })
            column(TRAP_XP, 280)
            column(TRAP_SUCCESS, 153, 255)
            column(TRAP_LOC, loc.packed)
        }
    }

    private fun DBTableBuilder.darts(
        row: String,
        low: CoordGrid,
        high: CoordGrid,
        heading: Heading,
    ) {
        val middle = (low.x + high.x) / 2 * heading.dx + (low.z + high.z) / 2 * heading.dz
        val first = if (heading.dx != 0) at(middle, low.z) else at(low.x, middle)
        row(row) {
            column(TRAP_KIND, KIND_DARTS)
            column(TRAP_SPAN, listOf(first.packed, first.ahead(heading, 1).packed))
            column(TRAP_XP, 300)
            column(TRAP_SUCCESS, 131, 255)
            column(TRAP_DISPENSERS, listOf(low.packed, high.packed))
        }
    }

    private fun DBTableBuilder.sawBlade(row: String, span: List<CoordGrid>, angle: Int) {
        row(row) {
            column(TRAP_KIND, KIND_SAW_BLADE)
            column(TRAP_SPAN, span.map { it.packed })
            column(TRAP_XP, 0)
            column(TRAP_LOC_ANGLE, angle)
        }
    }
}
