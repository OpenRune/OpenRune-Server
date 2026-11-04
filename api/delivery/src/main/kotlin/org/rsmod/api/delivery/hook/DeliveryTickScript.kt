package org.rsmod.api.delivery.hook

import jakarta.inject.Inject
import org.rsmod.api.delivery.DeliveryClock
import org.rsmod.api.delivery.DeliveryManager
import org.rsmod.api.game.process.GameLifecycle
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.game.entity.PlayerList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Advance at StartCycle, drain at LateCycle, and restamp persistence on Logout before the
 * account save is queued.
 */
internal class DeliveryTickScript
@Inject
constructor(
    private val manager: DeliveryManager,
    private val clock: DeliveryClock,
    private val playerList: PlayerList,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<GameLifecycle.StartCycle> { clock.tick() }
        onEvent<GameLifecycle.LateCycle> { drainAll() }
        onPlayerLogin { manager.rehydrate(player) }
        onPlayerLogout { manager.flushForSave(player) }
    }

    private fun drainAll() {
        for (player in playerList) {
            if (!player.canProcess) {
                continue
            }
            manager.drain(player)
        }
    }
}
