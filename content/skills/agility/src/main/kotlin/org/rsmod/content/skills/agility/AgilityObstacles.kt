package org.rsmod.content.skills.agility

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import kotlin.math.abs
import org.rsmod.api.config.Constants
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.agilityLvl
import org.rsmod.api.player.stat.baseAgilityLvl
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpLoc4
import org.rsmod.api.script.onOpLoc5
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.isType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import skillSuccess

/** One course's claim on a loc: which lap it belongs to and which step of it this obstacle is. */
private data class Binding(
    val course: Course,
    val courseIndex: Int,
    val index: Int,
    val obstacle: Obstacle,
)

class AgilityObstacles
@Inject
constructor(private val objRepo: ObjRepository, private val xpMods: XpModifiers) : PluginScript() {
    override fun ScriptContext.startup() {
        val bindings = LinkedHashMap<String, MutableList<Binding>>()
        for ((courseIndex, course) in AgilityCourses.courses.withIndex()) {
            for ((index, obstacle) in course.obstacles.withIndex()) {
                for (loc in obstacle.locs) {
                    bindings.getOrPut(loc) { mutableListOf() } +=
                        Binding(course, courseIndex, index, obstacle)
                }
            }
        }
        for ((loc, shared) in bindings) {
            registerObstacle(loc) { traverse(shared, it) }
        }
    }

    private fun ScriptContext.registerObstacle(
        loc: String,
        action: suspend ProtectedAccess.(BoundLocInfo) -> Unit,
    ) {
        val type = ServerCacheManager.getObject(locId(loc) ?: return) ?: return
        val slot = (1..5).firstOrNull { !type.actions.getOpOrNull(it - 1).isNullOrBlank() } ?: return
        when (slot) {
            1 -> onOpLoc1(loc) { action(it.loc) }
            2 -> onOpLoc2(loc) { action(it.loc) }
            3 -> onOpLoc3(loc) { action(it.loc) }
            4 -> onOpLoc4(loc) { action(it.loc) }
            else -> onOpLoc5(loc) { action(it.loc) }
        }
    }

    /**
     * The Shayzien and Colossal Wyrm pairs share their opening obstacles and split later, so one loc
     * can belong to two courses. The lap the player is already running wins; otherwise the hardest
     * course they qualify for does, which is what lets stepping onto a branch move them onto it.
     */
    private fun ProtectedAccess.select(bindings: List<Binding>): Binding {
        if (bindings.size == 1) {
            return bindings[0]
        }
        val running =
            bindings.firstOrNull {
                player.agilityCourse == it.courseIndex + 1 &&
                    it.course.steps.getOrNull(player.agilityProgress) == it.index
            }
        if (running != null) {
            return running
        }
        val open =
            bindings.filter {
                player.agilityLvl >= it.course.level && satisfies(it.course.reqs)
            }
        return open.maxByOrNull { it.course.level } ?: bindings.minByOrNull { it.course.level }!!
    }

    private suspend fun ProtectedAccess.traverse(bindings: List<Binding>, loc: BoundLocInfo) {
        val (course, courseIndex, index, obstacle) = select(bindings)
        if (player.agilityLvl < course.level) {
            mes("You need an Agility level of ${course.level} to use this obstacle.")
            return
        }

        if (!meets(course.reqs)) {
            return
        }

        faceObstacle(loc)
        val dest = obstacle.landing.resolve(coords)

        val fail = obstacle.fail
        if (fail != null && !skillSuccess(fail.low, fail.high, player.agilityLvl)) {
            slip(fail)
            return
        }

        // A crossing that follows the scaffolding passes over isolated planks with nothing walkable
        // around them, so it must never end part-way: whatever interrupts it, the player finishes on
        // the far side rather than stranded in mid-air.
        try {
            for (stage in obstacle.stages.ifEmpty { singleStage(obstacle) }) {
                runStage(stage)
            }
        } finally {
            if (coords != dest) {
                teleport(dest, TeleportType.Exempt)
            }
        }

        resetAnim()
        if (obstacle.xp > 0) {
            statAdvance(STAT_AGILITY, obstacle.xp * xpMods.get(player, STAT_AGILITY))
        }
        advance(course, courseIndex, index)
    }

    /**
     * The older animate-wait-land obstacle expressed as stages, so the rooftop courses keep the
     * behaviour they were verified with.
     */
    private fun singleStage(obstacle: Obstacle): List<Stage> =
        listOf(
            Stage(
                obstacle.anim,
                obstacle.landing,
                obstacle.ticks.coerceAtLeast(1),
                obstacle.slide,
            )
        )

    /** One linked movement: play its animation and carry the player for as long as it runs. */
    private suspend fun ProtectedAccess.runStage(stage: Stage) {
        val dest = stage.landing?.resolve(coords)
        if (dest == null || dest == coords) {
            anim(stage.anim)
            hold(stage.anim, stage.ticks.coerceAtLeast(1))
            return
        }
        val ticks =
            if (stage.ticks > 0) stage.ticks
            else tilesBetween(coords, dest) * stage.perTile
        if (!stage.slide) {
            anim(stage.anim)
            hold(stage.anim, ticks)
            teleport(dest, TeleportType.Exempt)
            return
        }
        val travel = if (stage.moveTicks in 1 until ticks) stage.moveTicks else ticks
        anim(stage.anim)
        exactMove(
            coords,
            dest,
            0,
            travel * CLIENT_CYCLES_PER_TICK,
            facing(coords, dest),
            TeleportType.Exempt,
        )
        hold(stage.anim, ticks)
    }

    /**
     * Waits out a stage, restarting its animation whenever it would run out.
     *
     * `anim` plays a sequence once. A crossing lasts as long as its distance, not as long as one
     * pass of its animation, so without this the player hangs from a zipline for two ticks and then
     * slides the remaining six with no animation at all - which reads as floating.
     */
    private suspend fun ProtectedAccess.hold(seq: String, ticks: Int) {
        val length = animTicks(seq)
        var elapsed = 0
        while (elapsed < ticks) {
            val chunk = minOf(length, ticks - elapsed)
            delay(chunk)
            elapsed += chunk
            if (elapsed < ticks) {
                anim(seq)
            }
        }
    }

    /**
     * Turns to the obstacle before using it.
     *
     * A wall loc - most ladders - sits on the edge of the tile the player is standing on, so facing
     * its coordinate is facing their own feet and turns them nowhere. The wall's angle is the way
     * they should be looking.
     */
    private fun ProtectedAccess.faceObstacle(loc: BoundLocInfo) {
        if (loc.coords != coords) {
            faceSquare(loc.coords)
            return
        }
        val (dx, dz) =
            when (loc.angle) {
                LocAngle.West -> -1 to 0
                LocAngle.North -> 0 to 1
                LocAngle.East -> 1 to 0
                LocAngle.South -> 0 to -1
            }
        faceSquare(coords.translate(dx, dz))
    }

    /**
     * Messages and returns false on the first thing the course asks for that the player is short of.
     * Quest gates go through [QuestRequirements] so they follow the realm's quest requirement mode,
     * the same as the shortcuts do.
     */
    private fun ProtectedAccess.meets(reqs: CourseReqs): Boolean {
        val quest = reqs.quest
        if (quest != null && !QuestRequirements.hasCompleted(player, quest)) {
            mes("You need to have completed a quest to use this course.")
            return false
        }
        if (!wieldsFor(reqs)) {
            mes(reqs.wornMessage)
            return false
        }
        if (reqs.grapple && !wearingGrapple()) {
            mes("You need a crossbow and a mith grapple to use this course.")
            return false
        }
        return true
    }

    /** The same check without the message, for choosing between two courses sharing an obstacle. */
    private fun ProtectedAccess.satisfies(reqs: CourseReqs): Boolean {
        val quest = reqs.quest
        if (quest != null && !QuestRequirements.hasCompleted(player, quest)) {
            return false
        }
        return wieldsFor(reqs) && (!reqs.grapple || wearingGrapple())
    }

    private fun ProtectedAccess.wieldsFor(reqs: CourseReqs): Boolean {
        if (reqs.worn.isEmpty()) {
            return true
        }
        val wielded = player.worn[Wearpos.RightHand.slot] ?: return false
        return reqs.worn.any { wielded.isType(it) }
    }

    /**
     * A failed obstacle costs the lap and a share of the player's remaining hitpoints. Live drops
     * them off the roof; here they keep their footing and only the lap is lost, because no source
     * records the tile each obstacle drops you onto.
     */
    private fun ProtectedAccess.slip(fail: ObstacleFail) {
        mes("You slip and fail to complete the obstacle.")
        queueHit(delay = 0, type = HitType.Typeless, damage = fail.damage(player.hitpoints))
        player.agilityCourse = 0
        player.agilityProgress = 0
    }

    /**
     * A basic and an advanced course run the same obstacles until they split, so the branch the
     * player takes is what decides which lap they are running. The switch is only allowed where the
     * two courses really are the same lap so far: every step before this one has to be the same loc,
     * which stops a part-finished lap of one course being cashed in on another.
     */
    private fun ProtectedAccess.switchesTo(course: Course, index: Int): Boolean {
        val current = AgilityCourses.courses.getOrNull(player.agilityCourse - 1) ?: return false
        val progress = player.agilityProgress
        if (course.steps.getOrNull(progress) != index) {
            return false
        }
        return (0 until progress).all { step ->
            current.obstacles.getOrNull(current.steps.getOrNull(step) ?: -1)?.locs ==
                course.obstacles.getOrNull(course.steps.getOrNull(step) ?: -1)?.locs
        }
    }

    /**
     * Laps only count when the obstacles are cleared in order, so leaving a course part-way through
     * and re-entering at a later obstacle earns the obstacle xp but never the lap bonus. Progress
     * counts steps rather than obstacles, because an obstacle placed more than once in a lap - the
     * Barbarian Outpost walls are one loc standing in three places - is one step per crossing.
     */
    private fun ProtectedAccess.advance(course: Course, courseIndex: Int, index: Int) {
        val onCourse = player.agilityCourse == courseIndex + 1 || switchesTo(course, index)
        val progress = if (onCourse) player.agilityProgress else 0

        if (course.steps.getOrNull(progress) != index) {
            val restarting = course.steps.first() == index
            player.agilityCourse = if (restarting) courseIndex + 1 else 0
            player.agilityProgress = if (restarting) 1 else 0
            return
        }

        val completed = progress + 1
        if (completed < course.steps.size) {
            player.agilityCourse = courseIndex + 1
            player.agilityProgress = completed
            return
        }

        player.agilityCourse = 0
        player.agilityProgress = 0
        statAdvance(STAT_AGILITY, course.lapXp * xpMods.get(player, STAT_AGILITY))
        rollMark(course)
        with(SquirrelPet) { rollSquirrel(course) }
    }

    /**
     * A lap rolls for a mark only once three minutes have passed since the last one spawned, and
     * the roll is 80% weaker twenty levels above the course requirement. The clock is kept in
     * minutes so it survives a logout the way the live timer does.
     */
    private fun ProtectedAccess.rollMark(course: Course) {
        if (course.markOdds <= 0.0 || course.markSpawns.isEmpty()) {
            return
        }

        val now = (System.currentTimeMillis() / MILLIS_PER_MINUTE).toInt()
        val last = player.agilityMarkClock
        if (last != 0 && now - last < MARK_COOLDOWN_MINUTES) {
            return
        }

        val outlevelled = player.baseAgilityLvl >= course.level + MARK_PENALTY_LEVELS
        val odds =
            if (course.markPenalty && outlevelled) course.markOdds * MARK_PENALTY else course.markOdds
        if (random.randomDouble() >= odds) {
            return
        }

        player.agilityMarkClock = now
        val spawn = course.markSpawns[random.of(0, course.markSpawns.size - 1)]
        objRepo.add(MARK_OF_GRACE, spawn, MARK_DURATION, receiver = player)
    }

    private companion object {
        const val CLIENT_CYCLES_PER_TICK = 30

        /** How many ticks one pass of an animation runs for, so it can be restarted in step. */
        fun animTicks(seq: String): Int {
            val id = runCatching { seq.asRSCM(RSCMType.SEQ) }.getOrNull() ?: return 1
            return ServerCacheManager.getAnim(id)?.tickDuration?.coerceAtLeast(1) ?: 1
        }

        /** Tiles apart, diagonals included, which is one tick of walking each. */
        fun tilesBetween(from: CoordGrid, to: CoordGrid): Int =
            maxOf(abs(to.x - from.x), abs(to.z - from.z)).coerceAtLeast(1)

        const val MARK_DURATION = 1000
        const val MARK_COOLDOWN_MINUTES = 3
        const val MARK_PENALTY_LEVELS = 20
        const val MARK_PENALTY = 0.2
        const val MILLIS_PER_MINUTE = 60_000L

        var Player.agilityCourse: Int by intVarp(VARP_COURSE)
        var Player.agilityProgress: Int by intVarp(VARP_PROGRESS)
        var Player.agilityMarkClock: Int by intVarp(VARP_MARK_CLOCK)

        fun facing(from: CoordGrid, to: CoordGrid): Int {
            val dx = to.x - from.x
            val dz = to.z - from.z
            return when {
                dx == 0 && dz == 0 -> Constants.em_face_south
                abs(dx) >= abs(dz) * 2 -> if (dx > 0) Constants.em_face_east else Constants.em_face_west
                abs(dz) >= abs(dx) * 2 ->
                    if (dz > 0) Constants.em_face_north else Constants.em_face_south
                dx > 0 -> if (dz > 0) Constants.em_face_northeast else Constants.em_face_southeast
                else -> if (dz > 0) Constants.em_face_northwest else Constants.em_face_southwest
            }
        }
    }
}
