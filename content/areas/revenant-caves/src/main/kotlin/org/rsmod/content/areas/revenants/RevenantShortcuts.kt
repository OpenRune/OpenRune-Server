package org.rsmod.content.areas.revenants

import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import kotlin.math.atan2
import org.rsmod.annotations.InternalApi
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.agilityLvl
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.script.onApLoc1
import org.rsmod.game.hit.HitType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal class RevenantShortcuts @Inject constructor(private val deps: BossDeps) : PluginScript() {
    override fun ScriptContext.startup() {
        onApLoc1("loc.wild_cave_agility_jump") {
            apRange(3)
            if (!isWithinApRange(it.loc, 3)) return@onApLoc1
            val route = ROUTES.firstOrNull { route -> route.pillar.x == it.loc.coords.x && route.pillar.z == it.loc.coords.z } ?: return@onApLoc1
            cross(route)
        }
    }

    @OptIn(InternalApi::class)
    private suspend fun ProtectedAccess.cross(route: Shortcut) {
        if (coords.level != 0 || actionDelay > mapClock || player.pendingLogout || player.hitpoints == 0) return
        if (player.agilityLvl < route.level) { mes("You need Agility ${route.level} to jump across this pillar."); return }
        val freeze = player.timerMap["timer.combat_freeze".asRSCM().toShort()]
        if (player.isFrozen && freeze != null && player.timerMap.extractExpiry(freeze) - mapClock > 3) { mes("You are frozen and cannot cross yet."); return }
        val start = if (coords.chebyshevDistance(route.a) <= coords.chebyshevDistance(route.b)) route.a else route.b
        val end = if (start == route.a) route.b else route.a
        playerWalk(start)
        if (coords != start || player.pendingLogout || player.hitpoints == 0) return
        actionDelay = mapClock + 4
        faceSquare(end)
        val fail = deps.random.of(100) < FAILURE_PERCENT
        var completed = false
        try {
            anim("seq.human_jump_hurdle")
            val direction = ((atan2((end.x - start.x).toDouble(), (end.z - start.z).toDouble()) * 325.949323) + 2048).toInt() and 2047
            exactMove(start, end, 0, 90, direction, TeleportType.Exempt)
            delay(3)
            if (coords != end || player.pendingLogout || player.hitpoints == 0) return
            if (fail) queueHit(1, HitType.Typeless, deps.random.of(4, 11))
            statAdvance("stat.agility", if (fail) route.failXp else route.level / 10.0)
            completed = true
        } finally {
            resetAnim()
            if (!completed && !player.pendingLogout && player.hitpoints > 0 && coords.level == 0 && coords.chebyshevDistance(start) <= 4) telejump(end, TeleportType.Exempt)
        }
    }
    internal data class Shortcut(val pillar: CoordGrid, val a: CoordGrid, val b: CoordGrid, val level: Int, val failXp: Double)
    internal companion object {
        // Native stand tiles paired from the accepted map, including bridge-level pillars.
        val ROUTES = listOf(
            Shortcut(CoordGrid(3220, 10086), CoordGrid(3220, 10084), CoordGrid(3220, 10088), 65, 3.5),
            Shortcut(CoordGrid(3200, 10136), CoordGrid(3198, 10136), CoordGrid(3202, 10136), 75, 3.8),
            Shortcut(CoordGrid(3241, 10145), CoordGrid(3239, 10145), CoordGrid(3243, 10145), 89, 4.5),
            Shortcut(CoordGrid(3202, 10196), CoordGrid(3200, 10196), CoordGrid(3204, 10196), 75, 3.8),
            Shortcut(CoordGrid(3180, 10209), CoordGrid(3180, 10207), CoordGrid(3180, 10211), 75, 3.8),
        )
        const val FAILURE_PERCENT = 5 // Exact OSRS failure odds are unpublished.
    }
}
