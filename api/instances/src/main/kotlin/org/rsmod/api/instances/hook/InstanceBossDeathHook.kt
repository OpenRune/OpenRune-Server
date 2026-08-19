package org.rsmod.api.instances.hook

import jakarta.inject.Inject
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.instances.InstanceManager
import org.rsmod.game.MapClock

internal class InstanceBossDeathHook
@Inject
constructor(private val manager: InstanceManager, private val worldClock: MapClock) :
    NpcDeathKillHook {
    override fun onKill(context: NpcDeathKillContext) {
        manager.handleBossKill(context.npc, worldClock.cycle)
    }
}
