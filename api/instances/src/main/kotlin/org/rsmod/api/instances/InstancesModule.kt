package org.rsmod.api.instances

import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.death.PlayerDeathCleanupHook
import org.rsmod.api.instances.hook.InstanceBossDeathHook
import org.rsmod.api.instances.hook.InstanceDeathCleanupHook
import org.rsmod.api.instances.hook.InstanceNpcDamageContributor
import org.rsmod.api.npc.hit.NpcDamageContributor
import org.rsmod.plugin.module.PluginModule

public class InstancesModule : PluginModule() {
    override fun bind() {
        addSetBinding<NpcDamageContributor>(InstanceNpcDamageContributor::class.java)
        addSetBinding<PlayerDeathCleanupHook>(InstanceDeathCleanupHook::class.java)
        addSetBinding<NpcDeathKillHook>(InstanceBossDeathHook::class.java)
    }
}
