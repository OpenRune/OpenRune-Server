package org.rsmod.content.skills.agility.pack

import dev.openrune.definition.dbtables.DBRowBuilder
import dev.openrune.rscm.RSCM.asRSCM
import kotlin.math.sign
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.ACTIONS
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.ACTION_ANIM
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.ACTION_BALANCE
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.ACTION_BALANCE_OFF
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.ACTION_EXACT_MOVE
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.ACTION_EXACT_PATH
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.ACTION_FACE
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.ACTION_FACE_ANGLE
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.ACTION_HIT
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.ACTION_LAND
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.ACTION_LOC_ANIM
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.ACTION_NPC_SAY
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.ACTION_RESET_ANIM
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.ACTION_SOUND
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.ACTION_SPOTANIM
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.ACTION_STEP
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.ACTION_TELEJUMP
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.ACTION_VARBIT
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.ACTION_WAIT
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.ACTION_XP
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.AP_RANGE
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.BREAKS
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.COURSE
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.DIARY_XP
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.FAIL_ACTIONS
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.FAIL_DAMAGE
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.FAIL_MESSAGES
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.FAIL_SAYS
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.HAND_IN_MESSAGES
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.HAND_IN_OBJ
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.LAND_MESBOX
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.LAND_SPAWN_OBJ
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.LAND_SPAWN_TILES
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.LANES
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.LOCS
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.MESSAGES
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.NPCS
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.REQUIREMENT
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.REQUIREMENT_OBJS
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.SAYS
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.START_VARBIT
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.STATS
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.STEP
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.SUCCESS
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.WALK_ON
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.XP
import org.rsmod.map.CoordGrid

internal const val CLIMB = "seq.human_reachforladder"
internal const val LOG_BALANCE = "seq.human_walk_logbalance"
internal const val SIDESTEP = "seq.human_walk_sidestepl"
internal const val INTO_SIDESTEP = "seq.human_into_sidestepl"
internal const val OUT_OF_SIDESTEP = "seq.human_outof_sidestepl"
internal const val WALL_JUMP = "seq.agility_shortcut_wall_jump"
internal const val WALL_JUMP_LAND = "seq.agility_shortcut_wall_jump2"
internal const val JUMP_DOWN = "seq.agility_shortcut_wall_jumpdown"
internal const val JUMP_DOWN_LAND = "seq.agility_shortcut_wall_jumpdown2"
internal const val SPOT_JUMP = "seq.human_spot_jump"
internal const val HURDLE = "seq.human_jump_hurdle"
internal const val RUN_UP = "seq.ic_running"
internal const val HANDHOLDS = "seq.agilityarena_handholds_middle"
internal const val HANDHOLDS_LAST = "seq.agilityarena_handholds_last"
internal const val HANDHOLDS_REVERSE = "seq.agilityarena_handholds_middlereverse"
internal const val HANDHOLDS_REVERSE_LAST = "seq.agilityarena_handholds_lastreverse"
internal const val STUMBLE = "seq.human_walk_logbalance_stumble"
internal const val FALL_DROP = "seq.viking_drop_to_knee_and_backtoready"
internal const val HANDHOLDS_FALL_REVERSE = "seq.agilityarena_handholds_middlefallreverse"
internal const val ROPE_SWING = "seq.human_ropeswing_long"
internal const val CLIMBING = "seq.human_climbing"
internal const val CLIMBING_DOWN = "seq.human_climbing_down"
internal const val PIPE_SQUEEZE = "seq.human_pipesqueeze"
internal const val PIPE_UNSQUEEZE = "seq.human_pipeunsqueeze"
internal const val DOUBLE_PIPE_SQUEEZE = "seq.human_doublepipesqueeze"
internal const val SIDESTEP_RIGHT = "seq.human_walk_sidestep"
internal const val INTO_SIDESTEP_RIGHT = "seq.human_into_sidestep"
internal const val OUT_OF_SIDESTEP_RIGHT = "seq.human_outof_sidestep"
internal const val SIDESTEP_LONG_LEFT = "seq.human_speartrap_walk_sidestep_long_l"
internal const val MONKEYBARS_ON = "seq.human_monkeybars_on"
internal const val MONKEYBARS_WALK = "seq.human_monkeybars_walk"
internal const val MONKEYBARS_OFF = "seq.human_monkeybars_off"
internal const val CRUMBLED_WALL = "seq.human_walk_crumbledwall"
internal const val WALK_BACK = "seq.human_walk_b"
internal const val PICKUP_FLOOR = "seq.human_pickupfloor"
internal const val ZIPLINE = "seq.zipline_slide"
internal const val GRAPPLE_SWING = "seq.dorgesh_grapple_swing"

internal const val TIGHTROPE_SYNTH = "synth.tightrope"
internal const val LEDGE_SYNTH = "synth.balancing_ledge"
internal const val JUMP_SYNTH = "synth.jump"
internal const val JUMP_UP_SYNTH = "synth.jump_up"
internal const val JUMP_DOWN_SYNTH = "synth.jump2"
internal const val HURDLE_SYNTH = "synth.hurdle"
internal const val HANDHOLDS_SYNTH = "synth.handholds_loop"
internal const val FALL_LAND_SYNTH = "synth.fall_land"
internal const val STUMBLE_SYNTH = "synth.stumble_loop"
internal const val SWING_SYNTH = "synth.swing_across"
internal const val LOG_SYNTH = "synth.log_balance"
internal const val MONKEYBARS_ON_SYNTH = "synth.monkeybars_on"
internal const val MONKEYBARS_OFF_SYNTH = "synth.monkeybars_off"
internal const val CLIMBING_SYNTH = "synth.climbing_loop"
internal const val CLIMB_WALL_SYNTH = "synth.climb_wall"
internal const val SQUEEZE_IN_SYNTH = "synth.squeeze_in"
internal const val SQUEEZE_OUT_SYNTH = "synth.squeeze_out"
internal const val MONKEYBARS_SYNTH = "synth.monkeybars_loop"
internal const val HANDHOLDS_GRAB_SYNTH = "synth.handholds_grab_to_second"
internal const val GRAPPLING_SYNTH = "synth.crossbow_grappling"

internal const val OP_TICK = -1

internal const val FACE_SOUTH = 0
internal const val FACE_WEST = 512
internal const val FACE_NORTH_WEST = 768
internal const val FACE_NORTH = 1024
internal const val FACE_EAST = 1536

internal const val MITHRIL_GRAPPLE = "obj.xbows_grapple_tip_bolt_mithril_rope"

internal const val SWING_MESSAGE = "You skillfully swing across."
internal const val LOG_MESSAGE = "You walk carefully across the slippery log..."

internal class LaneAction(
    val tick: Int,
    val op: Int,
    val coord: CoordGrid = CoordGrid.ZERO,
    val ref: Int = -1,
    val arg1: Int = 0,
    val arg2: Int = 0,
)

internal class LaneMessage(val tick: Int, val text: String)

internal class Lane(
    val start: CoordGrid,
    val actions: List<LaneAction>,
    val messages: List<LaneMessage>,
    val says: List<LaneMessage>,
)

internal class FallDamage(val min: Int, val max: Int, val hpShare: Pair<Int, Int> = 0 to 1)

internal class LaneBuilder(start: CoordGrid) {
    val actions = mutableListOf<LaneAction>()
    val messages = mutableListOf<LaneMessage>()
    val says = mutableListOf<LaneMessage>()
    private var tick = 0
    private var position = start

    fun next(ticks: Int = 1) {
        tick += ticks
    }

    fun message(text: String) {
        messages += LaneMessage(tick, text)
    }

    fun say(text: String) {
        says += LaneMessage(tick, text)
    }

    fun hold() {
        next()
        actions += LaneAction(tick, ACTION_WAIT)
    }

    fun opMessage(text: String) {
        messages += LaneMessage(OP_TICK, text)
    }

    fun step(dest: CoordGrid) {
        actions += LaneAction(tick, ACTION_STEP, dest)
        position = dest
    }

    /** Steps one tile a tick towards [dest]; the last step is on the current tick on return. */
    fun walkTo(dest: CoordGrid) {
        var first = true
        while (position != dest) {
            if (!first) {
                next()
            }
            step(position.translate((dest.x - position.x).sign, (dest.z - position.z).sign))
            first = false
        }
    }

    fun face(coord: CoordGrid) {
        actions += LaneAction(tick, ACTION_FACE, coord)
    }

    fun faceAngle(angle: Int) {
        actions += LaneAction(tick, ACTION_FACE_ANGLE, ref = angle)
    }

    fun anim(seq: String, delay: Int = 0) {
        actions += LaneAction(tick, ACTION_ANIM, ref = seq.asRSCM(), arg1 = delay)
    }

    fun resetAnim() {
        actions += LaneAction(tick, ACTION_RESET_ANIM)
    }

    fun sound(synth: String, loops: Int = 1, delay: Int = 0) {
        actions += LaneAction(tick, ACTION_SOUND, ref = synth.asRSCM(), arg1 = loops, arg2 = delay)
    }

    fun spotanim(spot: String, delay: Int = 0, height: Int = 0) {
        actions +=
            LaneAction(tick, ACTION_SPOTANIM, ref = spot.asRSCM(), arg1 = delay, arg2 = height)
    }

    fun locAnim(loc: CoordGrid, seq: String) {
        actions += LaneAction(tick, ACTION_LOC_ANIM, loc, ref = seq.asRSCM())
    }

    /** The nearest [npc] within [within] tiles of the player says a random line of [lines]. */
    fun npcSay(npc: String, lines: String, within: Int) {
        actions +=
            LaneAction(
                tick,
                ACTION_NPC_SAY,
                ref = npc.asRSCM(),
                arg1 = lines.asRSCM(),
                arg2 = within,
            )
    }

    fun balance(seq: String) {
        actions += LaneAction(tick, ACTION_BALANCE, ref = seq.asRSCM())
    }

    fun balanceOff() {
        actions += LaneAction(tick, ACTION_BALANCE_OFF)
    }

    /**
     * Moves the player to [dest] while the client draws them gliding between [delay1] and
     * [delay2]. The glide runs from the player's tile to [dest] unless [from]/[to] give the path
     * live draws, which on some obstacles is a tile or two off the tile the player ends on. Live
     * sends [angle] as the face angle too, except on the few moves [faceAngle] turns off.
     */
    fun exactMove(
        dest: CoordGrid,
        delay1: Int,
        delay2: Int,
        angle: Int,
        from: CoordGrid? = null,
        to: CoordGrid? = null,
        faceAngle: Boolean = true,
    ) {
        actions += LaneAction(tick, ACTION_EXACT_MOVE, dest, angle, delay1, delay2)
        if (faceAngle) {
            actions += LaneAction(tick, ACTION_FACE_ANGLE, ref = angle)
        }
        if (from != null || to != null) {
            val path = (to ?: dest).packed
            actions += LaneAction(tick, ACTION_EXACT_PATH, from ?: position, ref = path)
        }
        position = dest
    }

    fun telejump(dest: CoordGrid) {
        actions += LaneAction(tick, ACTION_TELEJUMP, dest)
        position = dest
    }

    fun land() {
        actions += LaneAction(tick, ACTION_LAND)
    }

    fun xp(tenths: Int) {
        actions += LaneAction(tick, ACTION_XP, ref = tenths)
    }

    fun hit() {
        actions += LaneAction(tick, ACTION_HIT)
    }

    fun varbit(varbit: String, value: Int) {
        actions += LaneAction(tick, ACTION_VARBIT, ref = varbit.asRSCM(), arg1 = value)
    }

    fun climbWall(face: CoordGrid, dest: CoordGrid) {
        face(face)
        anim(CLIMB, delay = 15)
        next(2)
        telejump(dest)
        resetAnim()
        land()
    }

    fun jumpDown(dest: CoordGrid) {
        anim(JUMP_DOWN, delay = 15)
        sound(JUMP_DOWN_SYNTH, delay = 15)
        next()
        telejump(dest)
        anim(JUMP_DOWN_LAND)
    }

    fun gap(face: CoordGrid, dest: CoordGrid) {
        face(face)
        next()
        jumpDown(dest)
        next()
        resetAnim()
        land()
    }

    fun hop(dest: CoordGrid, angle: Int, seq: String = HURDLE) {
        exactMove(dest, delay1 = 15, delay2 = 30, angle = angle)
        anim(seq)
        sound(JUMP_SYNTH)
        next()
        resetAnim()
        land()
    }

    fun tightrope(on: CoordGrid, last: CoordGrid, off: CoordGrid, loops: Int, seq: String) {
        step(on)
        next()
        balance(seq)
        sound(TIGHTROPE_SYNTH, loops = loops)
        walkTo(last)
        next()
        balanceOff()
        step(off)
        land()
    }

    fun stumble(tile: CoordGrid) {
        anim(STUMBLE)
        sound(STUMBLE_SYNTH, loops = 10)
        next()
        telejump(tile)
        balanceOff()
        resetAnim()
        sound(FALL_LAND_SYNTH)
        hit()
    }

    fun drop(tile: CoordGrid) {
        telejump(tile)
        anim(FALL_DROP)
        sound(FALL_LAND_SYNTH)
        hit()
    }

    // UNVERIFIED: a guessed stumble, drop and hit, for the falls no capture shows.
    fun fall(tile: CoordGrid) {
        anim(STUMBLE)
        next()
        telejump(tile)
        anim(JUMP_DOWN_LAND)
        sound(FALL_LAND_SYNTH)
        hit()
        next()
        resetAnim()
    }
}

internal fun tile(x: Int, z: Int, level: Int): CoordGrid = CoordGrid(x, z, level)

internal fun lane(start: CoordGrid, block: LaneBuilder.() -> Unit): Lane {
    val builder = LaneBuilder(start).apply(block)
    return Lane(start, builder.actions, builder.messages, builder.says)
}

internal fun DBRowBuilder.obstacle(course: String, step: Int, xp: Int, vararg locs: String) {
    columnRSCM(COURSE, course)
    column(STEP, step)
    columnRSCM(LOCS, *locs)
    column(XP, xp)
}

internal fun DBRowBuilder.lanes(vararg lanes: Lane) {
    for ((index, lane) in lanes.withIndex()) {
        require(lane.actions.count { it.op == ACTION_LAND } == 1) {
            "Lane $index starting ${lane.start} must land exactly once."
        }
    }
    column(LANES, lanes.flatMapIndexed { index, lane -> listOf(index, lane.start.packed) })
    column(ACTIONS, actionValues(lanes.withIndex().associate { (index, lane) -> index to lane }))
    val messages = lanes.flatMapIndexed { index, lane ->
        lane.messages.flatMap { listOf(index, it.tick, it.text) }
    }
    if (messages.isNotEmpty()) {
        column(MESSAGES, messages)
    }
    val says = lanes.flatMapIndexed { index, lane ->
        lane.says.flatMap { listOf(index, it.tick, it.text) }
    }
    if (says.isNotEmpty()) {
        column(SAYS, says)
    }
}

internal fun DBRowBuilder.npcObstacle(course: String, step: Int, xp: Int, vararg npcs: String) {
    columnRSCM(COURSE, course)
    column(STEP, step)
    columnRSCM(NPCS, *npcs)
    column(XP, xp)
}

internal fun DBRowBuilder.handIn(obj: String, given: String, refused: String) {
    columnRSCM(HAND_IN_OBJ, obj)
    column(HAND_IN_MESSAGES, given, refused)
}

/**
 * Makes the obstacle failable: success is `statRandom(agility, low, high)` and each fail lane, in
 * the order of the lanes, replaces the matching lane on a failed roll.
 */
internal fun DBRowBuilder.fails(low: Int, high: Int, damage: FallDamage, vararg lanes: Lane) {
    failsOn(low, high, damage, lanes.withIndex().associate { (index, lane) -> index to lane })
}

/** As [fails], for the lanes at the keys of [lanes] only; the row's other lanes never fail. */
internal fun DBRowBuilder.failsOn(
    low: Int,
    high: Int,
    damage: FallDamage,
    lanes: Map<Int, Lane>,
) {
    column(SUCCESS, low, high)
    fallsOn(damage, lanes)
}

/** Fail lanes the runtime picks instead of a success roll, as the arena's broken planks. */
internal fun DBRowBuilder.falls(damage: FallDamage, vararg lanes: Lane) {
    fallsOn(damage, lanes.withIndex().associate { (index, lane) -> index to lane })
}

private fun DBRowBuilder.fallsOn(damage: FallDamage, lanes: Map<Int, Lane>) {
    for ((index, lane) in lanes) {
        require(lane.actions.count { it.op == ACTION_HIT } == 1) {
            "Fail lane $index starting ${lane.start} must hit exactly once."
        }
        require(lane.actions.none { it.op == ACTION_LAND }) {
            "Fail lane $index starting ${lane.start} must not land."
        }
    }
    column(FAIL_ACTIONS, actionValues(lanes))
    column(FAIL_DAMAGE, damage.min, damage.max, damage.hpShare.first, damage.hpShare.second)
    val messages = lanes.flatMap { (index, lane) ->
        lane.messages.flatMap { listOf(index, it.tick, it.text) }
    }
    if (messages.isNotEmpty()) {
        column(FAIL_MESSAGES, messages)
    }
    val says = lanes.flatMap { (index, lane) ->
        lane.says.flatMap { listOf(index, it.tick, it.text) }
    }
    if (says.isNotEmpty()) {
        column(FAIL_SAYS, says)
    }
}

internal fun DBRowBuilder.requirement(requirement: Int, vararg objs: String) {
    column(REQUIREMENT, requirement)
    columnRSCM(REQUIREMENT_OBJS, *objs)
}

internal fun DBRowBuilder.landSpawn(obj: String, tiles: List<CoordGrid>) {
    columnRSCM(LAND_SPAWN_OBJ, obj)
    column(LAND_SPAWN_TILES, tiles.map { it.packed })
}

/**
 * Starts the obstacle once the player is within [distance] tiles with a clear line to it, for
 * obstacles whose start tile is behind a roof edge or across a gap from the loc.
 */
internal fun DBRowBuilder.apRange(distance: Int) {
    column(AP_RANGE, distance)
}

internal fun DBRowBuilder.landMesbox(text: String) {
    column(LAND_MESBOX, text)
}

internal fun DBRowBuilder.diaryXp(varbit: String, xp: Int) {
    column(DIARY_XP, varbit.asRSCM(), xp)
}

internal class ObstacleStat(val stat: String, val level: Int, val xpTenths: Int)

internal fun DBRowBuilder.stats(vararg stats: ObstacleStat) {
    column(STATS, stats.flatMap { listOf(it.stat.asRSCM(), it.level, it.xpTenths) })
}

internal fun DBRowBuilder.startVarbit(varbit: String, value: Int) {
    column(START_VARBIT, varbit.asRSCM(), value)
}

internal fun DBRowBuilder.breaks(enum: String) {
    columnRSCM(BREAKS, enum)
}

internal fun DBRowBuilder.walkOn() {
    column(WALK_ON, true)
}

private fun actionValues(lanes: Map<Int, Lane>): List<Any> {
    val values = mutableListOf<Any>()
    for ((index, lane) in lanes) {
        for (action in lane.actions) {
            values.addAll(
                listOf(
                    index,
                    action.tick,
                    action.op,
                    action.coord.packed,
                    action.ref,
                    action.arg1,
                    action.arg2,
                )
            )
        }
    }
    return values
}
