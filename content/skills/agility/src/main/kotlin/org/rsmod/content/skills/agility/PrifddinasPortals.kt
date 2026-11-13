package org.rsmod.content.skills.agility

import jakarta.inject.Inject
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.table.agility.AgilityCoursesRow
import org.rsmod.api.table.agility.AgilityPortalsRow
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal class PrifddinasPortals @Inject constructor() : AgilityCourseHook {
    private val portals = AgilityPortalsRow.all()

    override fun landed(access: ProtectedAccess, obstacle: AgilityObstacle) {
        val player = access.player
        val course = obstacle.course.rowId
        if (obstacle.step == 0) {
            val candidates = portals.filter { it.course.rowId == course }
            if (candidates.isEmpty()) {
                return
            }
            // UNVERIFIED: the wiki's 0.94 shards a lap suggests a portal on nearly every lap.
            player.lapPortal = access.random.pick(candidates).shown
            player.portalShown = 0
        }
        val portal =
            portals.firstOrNull { it.course.rowId == course && it.shown == player.lapPortal }
                ?: return
        if (portal.afterStep == obstacle.step) {
            player.portalShown = portal.shown
        }
    }

    override fun lapDone(access: ProtectedAccess, course: AgilityCoursesRow) {
        if (portals.none { it.course.rowId == course.rowId }) {
            return
        }
        access.player.lapPortal = 0
        access.player.portalShown = 0
    }

    private companion object {
        var Player.lapPortal by intVarBit("varbit.agility_lap_portal")
        var Player.portalShown by intVarBit("varbit.prif_agility_shortcut_transmit")
    }
}

class PrifddinasPortalScript @Inject constructor(private val objRepo: ObjRepository) :
    PluginScript() {
    override fun ScriptContext.startup() {
        for (portal in AgilityPortalsRow.all()) {
            onOpLoc1(portal.loc) { travel(portal, it.loc) }
        }
    }

    private suspend fun ProtectedAccess.travel(portal: AgilityPortalsRow, loc: BoundLocInfo) {
        if (player.portalShown != portal.shown) {
            return
        }
        arriveDelay()
        faceSquare(loc.coords)
        delay(1)
        if (player.portalShown != portal.shown) {
            return
        }
        player.portalShown = 0
        player.busy = true
        try {
            teleport(loc.coords, TeleportType.Exempt)
            delay(1)
            mes("You find a crystal shard on your way through the portal.")
            telejump(portal.exit, TeleportType.Exempt)
            statAdvance(AGILITY, PORTAL_XP)
            invAddOrDrop(objRepo, CRYSTAL_SHARD)
            if (player.lapCourse == portal.course.rowId && player.lapStep == portal.afterStep + 1) {
                player.lapStep = portal.nextStep
            }
        } finally {
            player.busy = false
        }
    }

    private companion object {
        const val AGILITY = "stat.agility"
        const val CRYSTAL_SHARD = "obj.prif_crystal_shard"
        const val PORTAL_XP = 82.0

        var Player.lapCourse by intVarBit("varbit.agility_lap_course")
        var Player.lapStep by intVarBit("varbit.agility_lap_step")
        var Player.busy by boolVarBit("varbit.busy")
        var Player.portalShown by intVarBit("varbit.prif_agility_shortcut_transmit")
    }
}
