package org.rsmod.content.minigames.gauntlet

import dev.or2.central.account.Rights
import jakarta.inject.Inject
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onCommand
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class GauntletCommand
@Inject
constructor(
    private val protectedAccess: ProtectedAccessLauncher,
    private val manager: InstanceManager,
    private val runs: GauntletRuns,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("gauntlet") {
            requiredRights = Rights.ADMINISTRATOR
            desc = "Teleport to the Gauntlet entrance, or leave a run if inside one"
            cheat {
                protectedAccess.launch(player) {
                    if (manager.sessionForPlayer(player) != null) {
                        with(runs) { leave() }
                    } else {
                        telejump(GauntletLobby.PRIF_ENTRANCE, TeleportType.Exempt)
                    }
                }
            }
        }
    }
}
