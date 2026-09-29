package org.rsmod.api.instances.timer

import jakarta.inject.Inject
import org.rsmod.api.game.process.GameLifecycle
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.events.InstancePlayerLeaveUnboundEvent
import org.rsmod.api.instances.ui.BossCountdown
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal class InstanceBossRespawnScript
@Inject
constructor(
    private val manager: InstanceManager,
    private val players: PlayerList,
    private val clock: MapClock,
    private val countdown: BossCountdown,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<GameLifecycle.LateCycle> { players.forEach(::update) }
        onEvent<InstancePlayerLeaveUnboundEvent> { countdown.clear(player, OWNER) }
        onPlayerLogout { countdown.clear(player, OWNER) }
    }

    private fun update(player: Player) {
        val session = manager.sessionForPlayer(player)
        val next = session?.takeIf { player.uuid in it.occupants }?.let {
            InstanceBossRespawn.next(it, manager.npcsForInstance(it.id), clock.cycle)
        }
        if (next == null) {
            countdown.clear(player, OWNER)
        } else {
            countdown.show(player, OWNER, next.bossName, next.deadlineTick - clock.cycle)
        }
    }

    private companion object {
        private const val OWNER = "native-instance-respawn"
    }
}
