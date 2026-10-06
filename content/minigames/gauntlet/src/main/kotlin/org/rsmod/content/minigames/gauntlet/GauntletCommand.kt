package org.rsmod.content.minigames.gauntlet

import dev.or2.central.account.Rights
import jakarta.inject.Inject
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.events.InstancePlayerLeaveEvent
import org.rsmod.api.instances.events.instanceEventId
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.events.EventBus
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class GauntletCommand
@Inject
constructor(
    private val protectedAccess: ProtectedAccessLauncher,
    private val manager: InstanceManager,
    private val runs: GauntletRuns,
    private val eventBus: EventBus,
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
        onPlayerSoftTimer(GauntletRuns.TIME_LIMIT_TIMER) {
            player.clearSoftTimer(GauntletRuns.TIME_LIMIT_TIMER)
            if (!GauntletRuns.ENFORCE_TIME_LIMIT) return@onPlayerSoftTimer
            protectedAccess.launch(player) {
                mes("You have run out of time.")
                with(runs) { leave() }
            }
        }
        onEvent<InstancePlayerLeaveEvent>(instanceEventId(GauntletRuns.KEY)) {
            player.clearSoftTimer(GauntletRuns.TIME_LIMIT_TIMER)
            GauntletHolding.restore(player)
            player.ifCloseOverlay(GauntletRuns.OVERLAY, eventBus)
            player.inGauntlet = false
            player.gauntletCorrupted = false
            player.gauntletStart = 0
        }
    }
}
