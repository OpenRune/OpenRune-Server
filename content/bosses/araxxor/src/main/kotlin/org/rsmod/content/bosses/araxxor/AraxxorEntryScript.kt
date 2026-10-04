package org.rsmod.content.bosses.araxxor

import dev.or2.central.account.Rights
import jakarta.inject.Inject
import org.rsmod.api.instances.InstanceAccess
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onOpLoc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal class AraxxorEntryScript @Inject constructor(
    private val instances: InstanceManager,
    private val access: ProtectedAccessLauncher,
    private val controller: AraxxorController,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("araxxor") {
            requiredRights = Rights.ADMINISTRATOR
            desc = "Enter Araxxor's private lair; ::araxxor leave to exit"
            cheat {
                val leave = args.singleOrNull()?.equals("leave", ignoreCase = true) == true
                access.launch(player) {
                    if (leave) leaveArena() else enterArena()
                }
            }
        }
        onOpLoc1("loc.araxxor_cave_outer_tunnel_op_4") { leaveArena() }
        onOpLoc1("loc.araxxor_cave_outer_tunnel_multi_4") { leaveArena() }
        for (symbol in listOf("loc.araxxor_boss_tunnel_multi", "loc.araxxor_boss_tunnel_op",
            "loc.araxxor_cave_outer_tunnel_op")) {
            onOpLoc1(symbol) {
                if (it.loc.coords.x == 3655 && it.loc.coords.z == 9814) enterArena()
            }
        }
    }

    private suspend fun ProtectedAccess.enterArena() {
        if (player.hitpoints <= 0 || player.loggingOut || player.pendingLogout) return
        val owner = player.uuid ?: return
        if (instances.sessionForPlayer(player) != null || instances.sessionForOwner(owner) != null) {
            mes("Leave your current instance before entering Araxxor's lair.")
            return
        }
        val returnTo = player.coords
        val result = instances.create(
            player, AraxxorArena.KEY, AraxxorArena.spec(returnTo), InstanceAccess.Private, mapClock,
        )
        if (result is InstanceManager.Result.Failed) {
            mes(result.reason)
            return
        }
        if (result !is InstanceManager.Result.Created) return
        var entered = false
        try {
            telejump(result.enter, TeleportType.Exempt)
            instances.finalizeEntry(player, result.session, mapClock)
            controller.spawn(player, result.session)
            entered = true
            mes("You enter Araxxor's lair.")
        } finally {
            if (!entered && instances.sessionForId(result.session.id) === result.session) {
                controller.end(result.session.id)
                if (instances.sessionForPlayer(player) === result.session) {
                    instances.leave(player, result.session, mapClock)
                } else {
                    instances.cancelPendingEntry(player, mapClock)
                }
                if (player.coords == result.enter) telejump(returnTo, TeleportType.Exempt)
            }
        }
    }

    private suspend fun ProtectedAccess.leaveArena() {
        val session = instances.sessionForPlayer(player) ?: return
        if (session.key != AraxxorArena.KEY || session.owner != player.uuid) return
        telejump(instances.leave(player, session, mapClock), TeleportType.Exempt)
    }
}
