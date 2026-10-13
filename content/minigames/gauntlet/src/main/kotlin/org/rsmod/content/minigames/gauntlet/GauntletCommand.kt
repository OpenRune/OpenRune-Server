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
        onCommand("testgauntlet") {
            requiredRights = Rights.ADMINISTRATOR
            desc = "Gauntlet entrance: ::testgauntlet. Tier 3 runs: ::gauntletkit or ::gauntletkithm"
            cheat {
                protectedAccess.launch(player) {
                    if (manager.sessionForPlayer(player)?.key == GauntletRuns.KEY) {
                        with(runs) { leave() }
                    } else if (manager.sessionForPlayer(player) == null) {
                        telejump(GauntletLobby.PRIF_ENTRANCE, TeleportType.Exempt)
                    } else {
                        mes("Leave your current instance first.")
                    }
                }
            }
        }
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
        for ((name, mode) in listOf("gauntletkit" to GauntletMode.NORMAL, "gauntletkithm" to GauntletMode.CORRUPTED)) {
            onCommand(name) {
                requiredRights = Rights.ADMINISTRATOR
                desc = "Enter the ${if (mode.corrupted) "Corrupted " else ""}Gauntlet with full tier 3 gear and paddlefish"
                cheat {
                    protectedAccess.launch(player) {
                        if (manager.sessionForPlayer(player) != null) {
                            mes("You are already inside an instance.")
                            return@launch
                        }
                        player.gauntletSpokenToBryn = true
                        with(runs) { enter(mode) }
                        if (manager.sessionForPlayer(player) != null) {
                            GauntletHolding.giveFullKit(player, mode.corrupted)
                        }
                    }
                }
            }
        }
    }
}
