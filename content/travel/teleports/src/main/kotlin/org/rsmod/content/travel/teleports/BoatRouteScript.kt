package org.rsmod.content.travel.teleports

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpLoc4
import org.rsmod.api.script.onOpLoc5
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc2
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.script.onOpNpc5
import org.rsmod.api.table.TeleportRouteRow
import org.rsmod.content.travel.teleports.RouteTravel.ride
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Boats, ferries and rowboats from `dbtable.teleport_route`: each row is one op of an npc or loc,
 * where it is used from and where it lands. The rows of the op that start near the player are the
 * choices; one choice sails straight away, several open a list.
 */
class BoatRouteScript : PluginScript() {
    override fun ScriptContext.startup() {
        val byTarget = TeleportRouteRow.all().groupBy { it.target() to it.op }
        for ((key, routes) in byTarget) {
            val (target, op) = key
            if (target.startsWith("npc.")) {
                onNpcOp(target, op) { sail(routes) }
            } else {
                onLocOp(target, op) { sail(routes) }
            }
        }
    }

    private suspend fun ProtectedAccess.sail(routes: List<TeleportRouteRow>) {
        val here = routes.filter { it.origin.chebyshevDistance(coords) <= NEARBY }
        if (here.isEmpty()) {
            mes("Nothing sails from here.")
            return
        }
        val route = if (here.size == 1) here.single() else choose(here) ?: return
        ride(TeleportDestination(route.name, route.destination, false), DEPART_MESSAGE)
    }

    private suspend fun ProtectedAccess.choose(routes: List<TeleportRouteRow>): TeleportRouteRow? {
        val labels =
            routes.map { route ->
                val clash = routes.count { it.name == route.name } > 1
                if (clash) "${route.name} (${route.destination.x}, ${route.destination.z})"
                else route.name
            }
        val picked = menu(MENU_TITLE, hotkeys = true, choices = labels + CANCEL)
        return routes.getOrNull(picked)
    }

    private fun TeleportRouteRow.target(): String =
        npc?.let { RSCM.getReverseMapping(RSCMType.NPC, it.id) }
            ?: RSCM.getReverseMapping(RSCMType.LOC, checkNotNull(loc) { "Route $rowId" }.id)

    private fun ScriptContext.onNpcOp(
        npc: String,
        op: Int,
        action: suspend ProtectedAccess.() -> Unit,
    ) {
        when (op) {
            1 -> onOpNpc1(npc) { action() }
            2 -> onOpNpc2(npc) { action() }
            3 -> onOpNpc3(npc) { action() }
            4 -> onOpNpc4(npc) { action() }
            5 -> onOpNpc5(npc) { action() }
        }
    }

    private fun ScriptContext.onLocOp(
        loc: String,
        op: Int,
        action: suspend ProtectedAccess.() -> Unit,
    ) {
        when (op) {
            1 -> onOpLoc1(loc) { action() }
            2 -> onOpLoc2(loc) { action() }
            3 -> onOpLoc3(loc) { action() }
            4 -> onOpLoc4(loc) { action() }
            5 -> onOpLoc5(loc) { action() }
        }
    }

    private companion object {
        private const val NEARBY = 20
        private const val MENU_TITLE = "Where would you like to go?"
        private const val CANCEL = "Cancel"
        private const val DEPART_MESSAGE = "You set off across the water."
    }
}
