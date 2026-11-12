package org.rsmod.content.skills.agility.pack.shortcut

import dev.openrune.definition.dbtables.DBRowBuilder
import dev.openrune.definition.dbtables.DBTableBuilder
import dev.openrune.rscm.RSCM.asRSCM
import kotlin.math.abs
import kotlin.math.sign
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.ACTION_MESBOX
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.LEVEL
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.LEVEL_REFUSAL
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.LEVEL_REFUSAL_OBJ
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.LOCS
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.OP
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.QUEST
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.RECOVERY_STEP
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.REFUSAL
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.STEP
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.VARBIT_GATES
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.XP
import org.rsmod.content.skills.agility.pack.CLIMB
import org.rsmod.content.skills.agility.pack.CLIMBING
import org.rsmod.content.skills.agility.pack.CLIMBING_DOWN
import org.rsmod.content.skills.agility.pack.CLIMBING_SYNTH
import org.rsmod.content.skills.agility.pack.CLIMB_WALL_SYNTH
import org.rsmod.content.skills.agility.pack.CRUMBLED_WALL
import org.rsmod.content.skills.agility.pack.DOUBLE_PIPE_SQUEEZE
import org.rsmod.content.skills.agility.pack.FACE_EAST
import org.rsmod.content.skills.agility.pack.FACE_NORTH
import org.rsmod.content.skills.agility.pack.FACE_NORTH_WEST
import org.rsmod.content.skills.agility.pack.FACE_SOUTH
import org.rsmod.content.skills.agility.pack.FACE_WEST
import org.rsmod.content.skills.agility.pack.GRAPPLING_SYNTH
import org.rsmod.content.skills.agility.pack.HURDLE
import org.rsmod.content.skills.agility.pack.HURDLE_SYNTH
import org.rsmod.content.skills.agility.pack.INTO_SIDESTEP
import org.rsmod.content.skills.agility.pack.INTO_SIDESTEP_RIGHT
import org.rsmod.content.skills.agility.pack.JUMP_DOWN
import org.rsmod.content.skills.agility.pack.JUMP_DOWN_LAND
import org.rsmod.content.skills.agility.pack.JUMP_DOWN_SYNTH
import org.rsmod.content.skills.agility.pack.JUMP_SYNTH
import org.rsmod.content.skills.agility.pack.JUMP_UP_SYNTH
import org.rsmod.content.skills.agility.pack.LEDGE_SYNTH
import org.rsmod.content.skills.agility.pack.LOG_BALANCE
import org.rsmod.content.skills.agility.pack.LOG_SYNTH
import org.rsmod.content.skills.agility.pack.LaneAction
import org.rsmod.content.skills.agility.pack.LaneBuilder
import org.rsmod.content.skills.agility.pack.LaneMessage
import org.rsmod.content.skills.agility.pack.MONKEYBARS_OFF
import org.rsmod.content.skills.agility.pack.MONKEYBARS_OFF_SYNTH
import org.rsmod.content.skills.agility.pack.MONKEYBARS_ON
import org.rsmod.content.skills.agility.pack.MONKEYBARS_ON_SYNTH
import org.rsmod.content.skills.agility.pack.MONKEYBARS_SYNTH
import org.rsmod.content.skills.agility.pack.MONKEYBARS_WALK
import org.rsmod.content.skills.agility.pack.OUT_OF_SIDESTEP
import org.rsmod.content.skills.agility.pack.OUT_OF_SIDESTEP_RIGHT
import org.rsmod.content.skills.agility.pack.PICKUP_FLOOR
import org.rsmod.content.skills.agility.pack.PIPE_SQUEEZE
import org.rsmod.content.skills.agility.pack.PIPE_UNSQUEEZE
import org.rsmod.content.skills.agility.pack.RUN_UP
import org.rsmod.content.skills.agility.pack.SIDESTEP
import org.rsmod.content.skills.agility.pack.SIDESTEP_LONG_LEFT
import org.rsmod.content.skills.agility.pack.SIDESTEP_RIGHT
import org.rsmod.content.skills.agility.pack.SPOT_JUMP
import org.rsmod.content.skills.agility.pack.SQUEEZE_IN_SYNTH
import org.rsmod.content.skills.agility.pack.SQUEEZE_OUT_SYNTH
import org.rsmod.content.skills.agility.pack.STUMBLE_SYNTH
import org.rsmod.content.skills.agility.pack.WALK_BACK
import org.rsmod.content.skills.agility.pack.WALL_JUMP_LAND
import org.rsmod.content.skills.agility.pack.lane
import org.rsmod.content.skills.agility.pack.lanes
import org.rsmod.map.CoordGrid

internal const val SCRAMBLE = "You scramble through the dark and dangerous tunnel..."

private const val SC_DEATH_CLIMBING_DOWN = "seq.death_human_climbing_down"
private const val SC_CLIMBING_LOOP = "seq.human_climbing_loop"
private const val SC_CLIMBING_FALL = "seq.human_climbing_fall"
private const val SC_WALK_STYLE = "seq.human_walk_style"
private const val SC_STUMBLE_BACK = "seq.human_stumble_back"
private const val SC_SIDESTEP_LONG = "seq.human_speartrap_walk_sidestep_long"
private const val SC_TIGHT_FIT = "seq.regicide_tightfit"
private const val SC_ENTER_HOLE = "seq.agilty_shortcut_enter_hole"
private const val SC_TUNNEL_WALK = "seq.agilty_shortcut_tunnel_walk"
private const val SC_EXIT_HOLE = "seq.agilty_shortcut_exit_hole"
private const val SC_LONG_CRAWL = "seq.human_longcrawl"
private const val SC_CRAWLING = "seq.human_crawling"
private const val SC_STEPPING_STONE_JUMP = "seq.human_steppingstonejump"
private const val SC_GRAPPLE_FIRE = "seq.xbows_human_fire_and_reload"
private const val SC_SWIM_NORTH = "seq.xbows_karamja_north_swimming_anim"
private const val SC_SWIM_SOUTH = "seq.xbows_karamja_south_swimming_anim"

private const val SC_CLIMB_UNDER_SYNTH = "synth.climb_under"
private const val SC_JUMP_BLADES_SYNTH = "synth.jump_blades"
private const val SC_GRAPPLE_SPLASH_SYNTH = "synth.crossbow_grapple_splash"

private const val ANGLE_SOUTH_WEST = 256
private const val ANGLE_NORTH_EAST = 1280
private const val ANGLE_SOUTH_EAST = 1792

private const val PIPE_GLIDE_TILES = 3
private const val END_MESBOX_TICK = -2
private const val FELL = "You fall and hurt yourself."
private const val OUCH = "Ouch"

internal class Way(val from: CoordGrid, val to: CoordGrid)

internal fun way(from: CoordGrid, to: CoordGrid): Way = Way(from, to)

internal fun both(a: CoordGrid, b: CoordGrid): List<Way> = listOf(Way(a, b), Way(b, a))

internal fun DBTableBuilder.shortcutObstacles() {
    shortcutClimbs()
    shortcutCrossings()
    shortcutSqueezes()
    shortcutAdditions()
}

/**
 * Makes the row an agility shortcut over [locs]: it needs [level] Agility (boosts count), pays
 * [xp] tenths on landing and answers op [op] of its locs.
 */
internal fun DBRowBuilder.shortcut(level: Int, xp: Int, vararg locs: String, op: Int = 1) {
    column(STEP, RECOVERY_STEP)
    columnRSCM(LOCS, *locs)
    column(XP, xp)
    column(LEVEL, level)
    if (op != 1) {
        column(OP, op)
    }
}

internal fun DBRowBuilder.quest(quest: String) {
    columnRSCM(QUEST, quest)
}

internal fun DBRowBuilder.varbitGates(vararg gates: Triple<String, Int, Int>) {
    column(VARBIT_GATES, gates.flatMap { (varbit, min, max) -> listOf(varbit.asRSCM(), min, max) })
}

internal fun DBRowBuilder.refusal(text: String) {
    column(REFUSAL, text)
}

internal fun DBRowBuilder.levelRefusal(text: String, obj: String? = null) {
    column(LEVEL_REFUSAL, text)
    if (obj != null) {
        columnRSCM(LEVEL_REFUSAL_OBJ, obj)
    }
}

internal fun DBRowBuilder.ways(ways: List<Way>, move: LaneBuilder.(Way) -> Unit) {
    lanes(*ways.map { way -> lane(way.from) { move(way) } }.toTypedArray())
}

internal fun angleTo(from: CoordGrid, to: CoordGrid): Int {
    val dx = (to.x - from.x).sign
    val dz = (to.z - from.z).sign
    return when {
        dx == 0 && dz < 0 -> FACE_SOUTH
        dx < 0 && dz < 0 -> ANGLE_SOUTH_WEST
        dx < 0 && dz == 0 -> FACE_WEST
        dx < 0 && dz > 0 -> FACE_NORTH_WEST
        dx == 0 && dz > 0 -> FACE_NORTH
        dx > 0 && dz > 0 -> ANGLE_NORTH_EAST
        dx > 0 && dz == 0 -> FACE_EAST
        else -> ANGLE_SOUTH_EAST
    }
}

private fun axisAngle(from: CoordGrid, to: CoordGrid): Int {
    val dx = to.x - from.x
    val dz = to.z - from.z
    return if (abs(dx) >= abs(dz)) {
        if (dx < 0) FACE_WEST else FACE_EAST
    } else {
        if (dz < 0) FACE_SOUTH else FACE_NORTH
    }
}

private fun CoordGrid.alongAxis(to: CoordGrid, tiles: Int): CoordGrid {
    val dx = to.x - x
    val dz = to.z - z
    return if (abs(dx) >= abs(dz)) translate(dx.sign * tiles, 0) else translate(0, dz.sign * tiles)
}

private fun distance(from: CoordGrid, to: CoordGrid): Int =
    maxOf(abs(to.x - from.x), abs(to.z - from.z))

private fun CoordGrid.stepsToward(dest: CoordGrid, tiles: Int): CoordGrid {
    val dx = dest.x - x
    val dz = dest.z - z
    return translate(dx.sign * minOf(tiles, abs(dx)), dz.sign * minOf(tiles, abs(dz)))
}

/** Shows the messages of the current tick in a message box without a continue button. */
internal fun LaneBuilder.mesbox(text: String) {
    message(text)
    actions += LaneAction(messages.last().tick, ACTION_MESBOX)
}

/** Shows [text] in a message box the player continues once the lane has ended. */
internal fun LaneBuilder.endMesbox(text: String) {
    messages += LaneMessage(END_MESBOX_TICK, text)
}

/**
 * How a rock face is glided down (or, with [facesTravel], up): the seq, the glide's delays, the
 * climbing sound, the tick the seq resets and whether the xp is paid then rather than at once.
 */
internal class RockClimb(
    val seq: String = CLIMBING_DOWN,
    val seqDelay: Int = 15,
    val delay1: Int = 0,
    val delay2: Int = 90,
    val loops: Int = 0,
    val soundDelay: Int = 0,
    val resetTick: Int = 3,
    val paidAtReset: Boolean = false,
    val facesTravel: Boolean = false,
)

internal val SHORT_ROCK = RockClimb(delay2 = 50, resetTick = 2)
internal val LONG_ROCK = RockClimb(delay2 = 120)
internal val CLIFF_DOWN = RockClimb(seqDelay = 0, loops = 5, soundDelay = 5)
internal val CLIFF_UP =
    RockClimb(seq = SC_CLIMBING_LOOP, seqDelay = 0, loops = 5, soundDelay = 5, facesTravel = true)
internal val SHILO_ROCK = RockClimb(SC_DEATH_CLIMBING_DOWN, 10, loops = 4, soundDelay = 10)
internal val WATERBIRTH_ROCK =
    RockClimb(SC_DEATH_CLIMBING_DOWN, 10, delay2 = 120, loops = 4, soundDelay = 10, resetTick = 4)
internal val TROLLHEIM_ROCK =
    RockClimb(
        SC_DEATH_CLIMBING_DOWN,
        10,
        delay2 = 120,
        loops = 4,
        soundDelay = 10,
        resetTick = 4,
        paidAtReset = true,
    )
internal val TROLLHEIM_LOW_ROCK =
    RockClimb(
        SC_DEATH_CLIMBING_DOWN,
        10,
        delay2 = 120,
        loops = 3,
        soundDelay = 10,
        resetTick = 4,
        paidAtReset = true,
    )
internal val FOSSIL_ROPE =
    RockClimb(
        SC_DEATH_CLIMBING_DOWN,
        10,
        delay1 = 10,
        delay2 = 120,
        loops = 6,
        soundDelay = 13,
        resetTick = 4,
    )

/**
 * Captured on the Troll Stronghold and Karamja rocks, the Aldarin and Ice Mountain cliffs and the
 * Shilo, Waterbirth, Trollheim and Fossil Island faces: one glide facing the rock face, paid as it
 * starts unless [climb] pays when the seq resets.
 */
internal fun LaneBuilder.climbDown(way: Way, climb: RockClimb = defaultClimb(way)) {
    val angle = if (climb.facesTravel) angleTo(way.from, way.to) else angleTo(way.to, way.from)
    anim(climb.seq, delay = climb.seqDelay)
    exactMove(way.to, climb.delay1, climb.delay2, angle, faceAngle = false)
    if (climb.loops > 0) {
        sound(CLIMBING_SYNTH, loops = climb.loops, delay = climb.soundDelay)
    }
    if (!climb.paidAtReset) {
        land()
    }
    next(climb.resetTick)
    resetAnim()
    if (climb.paidAtReset) {
        land()
    }
}

private fun defaultClimb(way: Way): RockClimb {
    // UNVERIFIED: an uncaptured rock glides as the captured rock of its length nearest to it.
    return if (distance(way.from, way.to) <= 2) SHORT_ROCK else RockClimb()
}

/** Captured on the Morytania rocks: a short glide down each tile of [path], a tick apart. */
internal fun LaneBuilder.climbDownSteps(path: List<CoordGrid>) {
    for ((index, tile) in path.withIndex()) {
        if (index > 0) {
            next()
        }
        anim(SC_DEATH_CLIMBING_DOWN, delay = 5)
        exactMove(tile, delay1 = 0, delay2 = 40, angle = FACE_WEST, faceAngle = false)
        sound(CLIMBING_SYNTH, delay = 5)
    }
    next()
    resetAnim()
    land()
}

/**
 * Captured on the Karamja and Trollheim rocks: walked up a tile a tick in the climbing stance,
 * with [loops] of the climbing sound as it starts, and paid the tick after the top.
 */
internal fun LaneBuilder.climbUp(way: Way, loops: Int = 0, soundDelay: Int = 0) {
    // UNVERIFIED: the stance seq; live only shows the appearance changing.
    balance(CLIMBING)
    if (loops > 0) {
        sound(CLIMBING_SYNTH, loops = loops, delay = soundDelay)
    }
    walkTo(way.to)
    next()
    balanceOff()
    land()
}

/**
 * Captured on the Karamja and Trollheim rocks: [steps] tiles up the face the player slides back
 * to the foot over [glide] cycles, and [fallTicks] later is hurt and paid [failXp].
 */
internal fun LaneBuilder.climbFall(
    way: Way,
    steps: Int,
    glide: Int,
    fallTicks: Int,
    failXp: Int,
    loops: Int = 0,
) {
    balance(CLIMBING)
    if (loops > 0) {
        sound(CLIMBING_SYNTH, loops = loops)
    }
    for (walked in 1..steps) {
        if (walked > 1) {
            next()
        }
        step(way.from.stepsToward(way.to, walked))
    }
    next()
    exactMove(way.from, delay1 = 0, delay2 = glide, angleTo(way.from, way.to))
    anim(SC_CLIMBING_FALL)
    if (loops > 0) {
        sound(STUMBLE_SYNTH, delay = 10)
    } else {
        sound(STUMBLE_SYNTH, loops = 10)
    }
    next(fallTicks)
    message(FELL)
    say(OUCH)
    balanceOff()
    resetAnim()
    hit()
    xp(failXp)
}

/**
 * Captured on the Death Plateau rocks: the player steps onto the rock and down its far side in
 * one glide.
 */
internal fun LaneBuilder.walkOverRock(way: Way) {
    message("You climb onto the rock...")
    message("...and step down the other side.")
    exactMove(way.to, delay1 = 30, delay2 = 94, angleTo(way.from, way.to))
    anim(SC_WALK_STYLE, delay = 30)
    next(2)
    land()
}

/** Captured on the Death Plateau rocks: the player slips off the rock and stays where they were. */
internal fun LaneBuilder.slipOffRock(way: Way) {
    message("You climb onto the rock...")
    face(way.from.stepsToward(way.to, 1))
    next()
    message("...but you slip back down.")
    anim(SC_STUMBLE_BACK)
    hit()
}

/** How a low wall is climbed over: the glide's delays, the seq and sound delays, the landing. */
internal class WallClimb(
    val delay1: Int = 0,
    val delay2: Int = 70,
    val animDelay: Int = 0,
    val soundDelay: Int = 10,
    val landTick: Int = 2,
)

internal val AUBURN_WALL = WallClimb()
internal val FALADOR_WALL =
    WallClimb(delay1 = 30, delay2 = 100, animDelay = 30, soundDelay = 40, landTick = 3)
internal val BURGH_INN_WALL = WallClimb(delay2 = 60, soundDelay = 0)

/** Captured on the Auburn Valley, Falador castle and Burgh de Rott inn walls. */
internal fun LaneBuilder.climbOver(way: Way, wall: WallClimb = AUBURN_WALL) {
    exactMove(way.to, wall.delay1, wall.delay2, angleTo(way.from, way.to))
    anim(CRUMBLED_WALL, delay = wall.animDelay)
    sound(CLIMB_WALL_SYNTH, delay = wall.soundDelay)
    next(wall.landTick)
    land()
}

/** Captured on the Catacombs of Kourend crack. */
internal fun LaneBuilder.squeezeThrough(way: Way, text: String? = null) {
    text?.let { message(it) }
    anim(PIPE_SQUEEZE)
    next()
    telejump(way.to)
    anim(PIPE_UNSQUEEZE)
    land()
}

/**
 * Captured on the Zanaris jutting walls: one long sidestep glide past the wall, the left-foot seq
 * going south.
 */
internal fun LaneBuilder.sidestepSqueeze(way: Way) {
    sidestep(way)
    land()
}

private fun LaneBuilder.sidestep(way: Way) {
    message("You try to squeeze past.")
    exactMove(way.to, delay1 = 28, delay2 = 124, angleTo(way.from, way.to))
    anim(if (way.to.z < way.from.z) SIDESTEP_LONG_LEFT else SC_SIDESTEP_LONG)
    sound(SQUEEZE_IN_SYNTH)
    next(5)
    sound(SQUEEZE_OUT_SYNTH)
}

/** Captured on the Canifis railings: a single quick squeeze. */
internal fun LaneBuilder.railingSqueeze(way: Way) {
    exactMove(way.to, delay1 = 15, delay2 = 30, angleTo(way.from, way.to))
    anim(PIPE_SQUEEZE)
    sound(SQUEEZE_OUT_SYNTH)
    next()
    land()
}

/** Captured on the Mudskipper Point loose railing. */
internal fun LaneBuilder.tightFit(way: Way) {
    message("You squeeze through the fence.")
    exactMove(way.to, delay1 = 0, delay2 = 76, angleTo(way.from, way.to))
    anim(SC_TIGHT_FIT)
    next(2)
    land()
}

/**
 * Captured on the Yanille dungeon, Edgeville dungeon and Taverley dungeon pipes: three tiles in
 * one glide, and a longer pipe a second glide straight after.
 */
internal fun LaneBuilder.pipe(way: Way) {
    val tiles = distance(way.from, way.to)
    val angle = angleTo(way.from, way.to)
    val first = way.from.stepsToward(way.to, PIPE_GLIDE_TILES)
    exactMove(first, delay1 = 30, delay2 = 126, angle, faceAngle = false)
    anim(DOUBLE_PIPE_SQUEEZE, delay = 30)
    next(4)
    sound(SQUEEZE_IN_SYNTH)
    if (tiles > PIPE_GLIDE_TILES) {
        // UNVERIFIED: a pipe longer than six tiles glides the rest in the second move.
        next()
        exactMove(way.to, delay1 = 30, delay2 = 126, angle, faceAngle = false)
        anim(DOUBLE_PIPE_SQUEEZE, delay = 30)
        next(4)
        sound(SQUEEZE_IN_SYNTH)
    }
    land()
}

/** The jumps of a stepping stone crossing: its glides, seq delays and sound delays. */
internal class StoneHops(
    val firstDelay1: Int,
    val firstDelay2: Int,
    val firstSoundDelay: Int,
    val laterDelay1: Int,
    val laterDelay2: Int,
    val laterAnimDelay: Int,
    val laterSoundDelay: Int,
)

/** Captured on the Shilo river, Necropolis and Rellekka stones: every jump alike. */
internal val EVEN_HOPS = StoneHops(30, 45, 15, 30, 45, 15, 15)

/** Captured on the lava maze, Nature Grotto and Saltpetre stones: later jumps are quicker. */
internal val QUICK_HOPS = StoneHops(30, 45, 20, 15, 28, 0, 5)

/**
 * A spot jump onto each stone, two ticks apart, landing the tick after the last, with [endText]
 * as it lands.
 */
internal fun LaneBuilder.stoneHops(
    way: Way,
    stones: List<CoordGrid> = emptyList(),
    hops: StoneHops = QUICK_HOPS,
    endText: String? = null,
) {
    var at = way.from
    for ((index, stone) in (stones + way.to).withIndex()) {
        if (index == 0) {
            exactMove(stone, hops.firstDelay1, hops.firstDelay2, angleTo(at, stone))
            anim(SPOT_JUMP, delay = 15)
            sound(JUMP_SYNTH, delay = hops.firstSoundDelay)
        } else {
            next(2)
            exactMove(
                stone,
                hops.laterDelay1,
                hops.laterDelay2,
                angleTo(at, stone),
                faceAngle = false,
            )
            anim(SPOT_JUMP, delay = hops.laterAnimDelay)
            sound(JUMP_SYNTH, delay = hops.laterSoundDelay)
        }
        at = stone
    }
    next()
    endText?.let { message(it) }
    land()
}

/**
 * Captured on the Lumbridge Swamp Caves stones: a long leap onto each stone three ticks apart,
 * paid on the last.
 */
internal fun LaneBuilder.steppingStoneLeaps(way: Way, stones: List<CoordGrid>) {
    message("You leap across with a mighty leap!")
    var at = way.from
    for ((index, stone) in (stones + way.to).withIndex()) {
        if (index > 0) {
            next(3)
        }
        exactMove(stone, delay1 = 47, delay2 = 59, angleTo(at, stone))
        anim(SC_STEPPING_STONE_JUMP, delay = 19)
        sound(JUMP_SYNTH, delay = 35)
        at = stone
    }
    land()
}

/** Captured on the Mort Myre bridge: one long leap over the gap, paid four ticks later. */
internal fun LaneBuilder.bridgeJump(way: Way) {
    exactMove(way.to, delay1 = 58, delay2 = 70, angleTo(way.from, way.to))
    anim(SC_STEPPING_STONE_JUMP, delay = 30)
    next(2)
    sound(JUMP_SYNTH)
    next(2)
    land()
}

/**
 * Captured on the Ardougne and Karamja logs: two tiles a tick on the log, with the balance
 * messages, landing three ticks after the last step.
 */
internal fun LaneBuilder.logBalance(way: Way) {
    message("You attempt to walk across the slippery log.")
    balance(LOG_BALANCE)
    sound(LOG_SYNTH, loops = 2)
    val tiles = distance(way.from, way.to)
    var walked = minOf(2, tiles)
    step(way.from.stepsToward(way.to, walked))
    while (walked < tiles) {
        next()
        if (walked == 2) {
            message("You make it across the log without any problems.")
        }
        walked = minOf(walked + 2, tiles)
        step(way.from.stepsToward(way.to, walked))
        sound(LOG_SYNTH, loops = 2)
    }
    next(3)
    balanceOff()
    land()
}

/**
 * Captured on the Yanille dungeon ledge: edged across a tile a tick, the first step two tiles on
 * the right-foot side, with the ledge sound as it starts and on the last step and three before.
 */
internal fun LaneBuilder.ledge(way: Way, leftFoot: Boolean) {
    val tiles = distance(way.from, way.to)
    val first = if (leftFoot) 1 else 2
    val rest = tiles - first
    message("You put your foot on the ledge and try to edge across...")
    anim(if (leftFoot) INTO_SIDESTEP else INTO_SIDESTEP_RIGHT)
    balance(if (leftFoot) SIDESTEP else SIDESTEP_RIGHT)
    sound(LEDGE_SYNTH, loops = 4)
    step(way.from.stepsToward(way.to, first))
    for (walked in 1..rest) {
        next()
        step(way.from.stepsToward(way.to, first + walked))
        if (walked == rest - 3 || walked == rest) {
            sound(LEDGE_SYNTH, loops = 2)
        }
    }
    next(2)
    message("You skillfully edge across the gap.")
    anim(if (leftFoot) OUT_OF_SIDESTEP else OUT_OF_SIDESTEP_RIGHT)
    balanceOff()
    land()
}

internal fun LaneBuilder.hopOver(way: Way) {
    // UNVERIFIED: the hurdle hop of the courses, not captured on a shortcut.
    hop(way.to, angleTo(way.from, way.to))
}

/** Captured on the Rellekka broken fence: a run-up on the spot, then the hurdle over. */
internal fun LaneBuilder.runUpHurdle(way: Way) {
    val angle = angleTo(way.from, way.to)
    faceAngle(angle)
    anim(RUN_UP, delay = 15)
    next()
    exactMove(way.to, delay1 = 8, delay2 = 50, angle)
    anim(HURDLE)
    sound(HURDLE_SYNTH)
    next()
    land()
}

/**
 * Captured on the Burgh de Rott fence: the player walks back from the fence to [runUp], turns,
 * runs two tiles at it and hurdles two tiles over it.
 */
internal fun LaneBuilder.fenceHurdle(way: Way, runUp: CoordGrid) {
    val angle = angleTo(runUp, way.to)
    walkTo(runUp)
    next(2)
    faceAngle(angle)
    next()
    exactMove(runUp.stepsToward(way.to, 2), delay1 = 8, delay2 = 50, angle)
    anim(RUN_UP)
    sound(SC_JUMP_BLADES_SYNTH)
    next()
    exactMove(way.to, delay1 = 8, delay2 = 50, angle)
    anim(HURDLE)
    next()
    land()
}

/**
 * Captured on the Barrows wall: the player backs off two tiles, runs one tile at the wall and
 * hurdles three tiles over it, so a crossing ends two tiles past its start.
 */
internal fun LaneBuilder.wallHurdle(way: Way) {
    val angle = angleTo(way.from, way.to)
    val dx = (way.to.x - way.from.x).sign
    val dz = (way.to.z - way.from.z).sign
    val back = way.from.translate(-2 * dx, -2 * dz)
    exactMove(back, delay1 = 0, delay2 = 60, angle)
    anim(WALK_BACK)
    next(2)
    exactMove(back.stepsToward(way.from, 1), delay1 = 0, delay2 = 30, angle, faceAngle = false)
    anim(RUN_UP)
    next()
    exactMove(way.to, delay1 = 8, delay2 = 50, angle, faceAngle = false)
    anim(HURDLE)
    sound(HURDLE_SYNTH)
    next()
    message("You jump over the wall.")
    land()
}

/**
 * Captured on the Varrock and Yanille wall tunnels: a glide into the hole, one through the tunnel
 * and one out, a tick each.
 */
internal fun LaneBuilder.tunnelWall(way: Way) {
    val angle = axisAngle(way.from, way.to)
    val entry = way.from.alongAxis(way.to, 1)
    val exit = way.to.alongAxis(way.from, 1)
    faceAngle(angle)
    exactMove(entry, delay1 = 0, delay2 = 30, angle, faceAngle = false)
    anim(SC_ENTER_HOLE)
    sound(SC_CLIMB_UNDER_SYNTH)
    next()
    exactMove(exit, delay1 = 0, delay2 = 30, angle, faceAngle = false)
    anim(SC_TUNNEL_WALK)
    next()
    exactMove(way.to, delay1 = 0, delay2 = 30, angle, faceAngle = false)
    anim(SC_EXIT_HOLE)
    next()
    land()
}

/**
 * Captured on the Royal Titans and Wyvern tunnels: the player crawls in facing [angle] and comes
 * out two ticks later, or by way of [via] and its [texts] two ticks apart where the tunnel is
 * long, each line in a message box, standing up [standTicks] after coming out.
 */
internal fun LaneBuilder.longCrawl(
    way: Way,
    angle: Int,
    text: String? = null,
    via: CoordGrid? = null,
    texts: List<String> = emptyList(),
    exitAngle: Int? = null,
    standTicks: Int = 4,
) {
    text?.let { mesbox(it) }
    anim(SC_LONG_CRAWL)
    faceAngle(angle)
    sound(CLIMBING_SYNTH, loops = 2, delay = 4)
    next(2)
    text?.let { mesbox(it) }
    if (via == null) {
        telejump(way.to)
        next(standTicks)
    } else {
        telejump(via)
        for (line in texts) {
            next(2)
            mesbox(line)
        }
        next(2)
        telejump(way.to)
    }
    resetAnim()
    exitAngle?.let { faceAngle(it) }
    land()
}

/** Captured on the Fossil Island hole: the player reaches down and climbs through. */
internal fun LaneBuilder.holeClimb(way: Way) {
    anim(PICKUP_FLOOR)
    next()
    message("You climb through the tunnel.")
    telejump(way.to)
    land()
}

/** Captured on the Dagannoth crevice: the player crawls in and comes out below, paid [xp]. */
internal fun LaneBuilder.creviceCrawl(way: Way, xp: Int) {
    anim(SC_CRAWLING)
    faceAngle(angleTo(way.from, way.to))
    next()
    message("You crawl through the narrow crevice.")
    telejump(way.to)
    xp(xp)
    land()
}

/** A shortcut that only goes one way, refused with [text] from its far end. */
internal fun LaneBuilder.oneWayOnly(angle: Int, text: String) {
    message(text)
    faceAngle(angle)
    endMesbox(text)
    land()
}

/**
 * Captured on the Karamja jungle tree: the player fires the grapple, and three ticks later is
 * pulled through the water, onto the island or off it to the far shore.
 */
internal fun LaneBuilder.grappleSwim(way: Way, northChannel: Boolean, toIsland: Boolean) {
    val angle = angleTo(way.from, way.to)
    faceAngle(angle)
    anim(SC_GRAPPLE_FIRE)
    sound(GRAPPLING_SYNTH)
    next(3)
    if (toIsland) {
        message("You successfully grapple the tree on the island.")
    } else {
        message("You successfully grapple the tree on the opposite shore.")
    }
    exactMove(way.to, delay1 = 0, delay2 = 200, angle, faceAngle = false)
    anim(if (northChannel) SC_SWIM_NORTH else SC_SWIM_SOUTH)
    sound(SC_GRAPPLE_SPLASH_SYNTH, delay = 3)
    land()
}

internal fun LaneBuilder.climbToFloor(way: Way) {
    // UNVERIFIED: the courses' rough wall climb, not captured on a shortcut.
    climbWall(way.to, way.to)
}

/** Captured on the Slayer Tower chains: reached up, and on the next floor a tick later. */
internal fun LaneBuilder.chainClimb(way: Way) {
    anim(CLIMB)
    next()
    telejump(way.to)
    resetAnim()
    land()
}

/** Captured on the Yanille dungeon rubble: climbed up a tick after reaching for it, paid [xp]. */
internal fun LaneBuilder.rubbleUp(way: Way, xp: Int) {
    anim(CLIMB)
    next()
    message("You climb up the pile of rubble...")
    telejump(way.to)
    resetAnim()
    xp(xp)
    land()
}

/** Captured on the Yanille dungeon rubble: climbed down at once. */
internal fun LaneBuilder.rubbleDown(way: Way) {
    message("You climb down the pile of rubble...")
    telejump(way.to)
    land()
}

/**
 * Captured on the Chasm of Fire chain: the player slides onto the cage on the floor above and
 * lands below a tick later.
 */
internal fun LaneBuilder.chainSlide(cage: CoordGrid, dest: CoordGrid) {
    message("You deftly slide down the chain, leaping off before landing on the cage.")
    exactMove(cage, delay1 = 10, delay2 = 29, angle = 1424)
    anim(CLIMB)
    sound(JUMP_SYNTH, delay = 13)
    next()
    telejump(dest)
    faceAngle(907)
    land()
}

/** Captured on the Chasm of Fire platform edge: the player hangs off it and drops a floor. */
internal fun LaneBuilder.lipDrop(edge: CoordGrid, dest: CoordGrid) {
    exactMove(edge, delay1 = 22, delay2 = 35, angle = FACE_SOUTH)
    anim(PICKUP_FLOOR, delay = 4)
    sound(CLIMBING_SYNTH, loops = 2, delay = 12)
    sound(JUMP_SYNTH, loops = 2, delay = 26)
    next()
    message("You slide off the lip of the platform, landing on the one below.")
    telejump(dest)
    land()
}

/** Captured on the Ectofuntus weathered wall: one glide up onto the ledge above. */
internal fun LaneBuilder.wallJumpUp(way: Way) {
    exactMove(way.to, delay1 = 49, delay2 = 68, angleTo(way.from, way.to))
    anim(WALL_JUMP_LAND)
    sound(JUMP_UP_SYNTH)
    next()
    land()
}

/** Captured on the Ectofuntus weathered wall: the courses' jump down, without their delays. */
internal fun LaneBuilder.dropDown(way: Way) {
    faceAngle(angleTo(way.from, way.to))
    anim(JUMP_DOWN)
    sound(JUMP_DOWN_SYNTH)
    next()
    telejump(way.to)
    anim(JUMP_DOWN_LAND)
    next()
    resetAnim()
    land()
}

/** A failed attempt that leaves the player where they stood, paying [failXp] tenths. */
internal fun LaneBuilder.slip(start: CoordGrid, failXp: Int) {
    // UNVERIFIED: the courses' stumble on the near side.
    stumble(start)
    if (failXp > 0) {
        xp(failXp)
    }
}

/** A failed squeeze that scrapes the player but still gets them through, paying [failXp]. */
internal fun LaneBuilder.scrape(way: Way, failXp: Int) {
    // UNVERIFIED: the wiki says a failed squeeze still passes; when the hit lands is a guess.
    sidestep(way)
    hit()
    xp(failXp)
}

/**
 * Captured on the Yanille dungeon bars: on and the first tile at once, a tile a tick, a turn back
 * the tick after the last and off the tick after that.
 */
internal fun LaneBuilder.monkeybars(way: Way) {
    anim(MONKEYBARS_ON)
    balance(MONKEYBARS_WALK)
    sound(MONKEYBARS_ON_SYNTH)
    step(way.from.stepsToward(way.to, 1))
    next()
    sound(MONKEYBARS_SYNTH, loops = 4, delay = 40)
    walkTo(way.to)
    sound(MONKEYBARS_SYNTH, loops = 3)
    next()
    faceAngle(angleTo(way.to, way.from))
    next()
    balanceOff()
    anim(MONKEYBARS_OFF)
    sound(MONKEYBARS_OFF_SYNTH)
    land()
}
