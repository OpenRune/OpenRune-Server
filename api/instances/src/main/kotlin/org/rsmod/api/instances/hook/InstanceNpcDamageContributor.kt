package org.rsmod.api.instances.hook

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.currentInstanceId
import org.rsmod.api.npc.hit.NpcDamageContributor
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

@Singleton
internal class InstanceNpcDamageContributor
@Inject
constructor(private val instances: InstanceManager) : NpcDamageContributor {
    override fun onPlayerDamageNpc(npc: Npc, source: Player, damage: Int) {
        val instanceId = source.currentInstanceId() ?: instances.instanceForNpc(npc) ?: return
        instances.contributionsFor(instanceId)?.record(source, damage)
    }
}
