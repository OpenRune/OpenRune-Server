package org.rsmod.content.skills.agility

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import kotlin.math.abs
import org.rsmod.api.config.Constants
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.agilityLvl
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpLoc4
import org.rsmod.api.script.onOpLoc5
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class AgilityObstacles
@Inject
constructor(private val objRepo: ObjRepository, private val xpMods: XpModifiers) : PluginScript() {
    override fun ScriptContext.startup() {
        for ((courseIndex, course) in AgilityCourses.courses.withIndex()) {
            for ((index, obstacle) in course.obstacles.withIndex()) {
                for (loc in obstacle.locs) {
                    registerObstacle(loc) { traverse(course, courseIndex, index, obstacle, it) }
                }
            }
        }
    }

    private fun ScriptContext.registerObstacle(
        loc: String,
        action: suspend ProtectedAccess.(BoundLocInfo) -> Unit,
    ) {
        val type = ServerCacheManager.getObject(loc.asRSCM(RSCMType.LOC)) ?: return
        val slot = (1..5).firstOrNull { !type.actions.getOpOrNull(it - 1).isNullOrBlank() } ?: return
        when (slot) {
            1 -> onOpLoc1(loc) { action(it.loc) }
            2 -> onOpLoc2(loc) { action(it.loc) }
            3 -> onOpLoc3(loc) { action(it.loc) }
            4 -> onOpLoc4(loc) { action(it.loc) }
            else -> onOpLoc5(loc) { action(it.loc) }
        }
    }

    private suspend fun ProtectedAccess.traverse(
        course: Course,
        courseIndex: Int,
        index: Int,
        obstacle: Obstacle,
        loc: BoundLocInfo,
    ) {
        if (player.agilityLvl < course.level) {
            mes("You need an Agility level of ${course.level} to use this obstacle.")
            return
        }

        faceSquare(loc.coords)
        val dest = obstacle.landing.resolve(coords)
        anim(obstacle.anim)

        if (obstacle.slide) {
            val cycles = obstacle.ticks * CLIENT_CYCLES_PER_TICK
            exactMove(coords, dest, 0, cycles, facing(coords, dest), TeleportType.Exempt)
            delay(obstacle.ticks)
        } else {
            delay(obstacle.ticks)
            teleport(dest, TeleportType.Exempt)
        }

        resetAnim()
        if (obstacle.xp > 0) {
            statAdvance(STAT_AGILITY, obstacle.xp * xpMods.get(player, STAT_AGILITY))
        }
        advance(course, courseIndex, index)
    }

    /**
     * Laps only count when the obstacles are cleared in order, so leaving a course part-way through
     * and re-entering at a later obstacle earns the obstacle xp but never the lap bonus.
     */
    private fun ProtectedAccess.advance(course: Course, courseIndex: Int, index: Int) {
        if (index == 0) {
            player.agilityCourse = courseIndex + 1
            player.agilityProgress = 1
            return
        }

        val onCourse = player.agilityCourse == courseIndex + 1
        if (!onCourse || player.agilityProgress != index) {
            player.agilityCourse = 0
            player.agilityProgress = 0
            return
        }

        val completed = index + 1
        if (completed < course.obstacles.size) {
            player.agilityProgress = completed
            return
        }

        player.agilityCourse = 0
        player.agilityProgress = 0
        statAdvance(STAT_AGILITY, course.lapXp * xpMods.get(player, STAT_AGILITY))
        rollMark(course)
    }

    private fun ProtectedAccess.rollMark(course: Course) {
        if (course.markChance <= 0 || course.markSpawns.isEmpty()) {
            return
        }
        if (random.of(1, course.markChance) != 1) {
            return
        }
        val spawn = course.markSpawns[random.of(0, course.markSpawns.size - 1)]
        objRepo.add(MARK_OF_GRACE, spawn, MARK_DURATION, receiver = player)
    }

    private companion object {
        const val CLIENT_CYCLES_PER_TICK = 30
        const val MARK_DURATION = 200

        var Player.agilityCourse: Int by intVarp(VARP_COURSE)
        var Player.agilityProgress: Int by intVarp(VARP_PROGRESS)

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
