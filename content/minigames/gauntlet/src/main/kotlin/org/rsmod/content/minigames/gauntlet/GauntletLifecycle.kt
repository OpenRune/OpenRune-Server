package org.rsmod.content.minigames.gauntlet

import jakarta.inject.Inject
import org.rsmod.api.bossbar.plugin.BossHpBarScript
import org.rsmod.api.instances.events.InstanceEndedEvent
import org.rsmod.api.instances.events.InstancePlayerLeaveEvent
import org.rsmod.api.instances.events.instanceEventId
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.content.minigames.gauntlet.hunllef.HunllefFloor
import org.rsmod.events.EventBus
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class GauntletLifecycle
@Inject
constructor(
    private val protectedAccess: ProtectedAccessLauncher,
    private val eventBus: EventBus,
    private val bossEntry: GauntletBossEntry,
    private val bossHpBar: BossHpBarScript,
    private val hunllefFloor: HunllefFloor,
    private val runs: GauntletRuns,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<SessionStateEvent.PrepareLogin> {
            player.gauntletRewardVisible = player.gauntletRewardAvailable
            if (runs.runFor(player) == null && (player.holdingInventory || player.inGauntlet)) {
                player.coords = GauntletLobby.LOBBY
            }
        }
        onPlayerSoftTimer(GauntletRuns.TIME_LIMIT_TIMER) {
            player.clearSoftTimer(GauntletRuns.TIME_LIMIT_TIMER)
            protectedAccess.launch(player) { with(bossEntry) { begin() } }
        }
        onEvent<InstancePlayerLeaveEvent>(instanceEventId(GauntletRuns.KEY)) {
            player.clearSoftTimer(GauntletRuns.TIME_LIMIT_TIMER)
            player.clearWeakQueue(GauntletGathering.GATHER_QUEUE)
            hunllefFloor.stopFor(player)
            GauntletHolding.restore(player)
            GauntletHolding.resetVitals(player)
            player.ifCloseOverlay(GauntletRuns.OVERLAY, eventBus)
            if (player.gauntletBossStarted) bossHpBar.onDeathClose(player)
            player.inGauntlet = false
            player.gauntletCorrupted = false
            player.gauntletStart = 0
            player.gauntletBossStarted = false
            player.gauntletPoints = 0
        }
        onEvent<InstanceEndedEvent>(instanceEventId(GauntletRuns.KEY)) {
            runs.finish(instanceId)
        }
        onPlayerLogin {
            player.gauntletRewardVisible = player.gauntletRewardAvailable
            if (runs.runFor(player) == null && (player.holdingInventory || player.inGauntlet)) {
                GauntletHolding.restore(player)
                GauntletHolding.resetVitals(player)
                player.inGauntlet = false
                player.gauntletCorrupted = false
                player.gauntletBossStarted = false
                player.gauntletPoints = 0
                player.gauntletStart = 0
                player.clearSoftTimer(GauntletRuns.TIME_LIMIT_TIMER)
                player.clearWeakQueue(GauntletGathering.GATHER_QUEUE)
                protectedAccess.launch(player) {
                    telejump(GauntletLobby.LOBBY, TeleportType.Exempt)
                    mes("Your Gauntlet run has ended. Your equipment has been returned.")
                }
            }
        }
    }
}
