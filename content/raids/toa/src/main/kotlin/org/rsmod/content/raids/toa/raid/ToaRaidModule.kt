package org.rsmod.content.raids.toa.raid

import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.PlayerDeathCleanupHook
import org.rsmod.api.npc.hit.NpcDamageContributor
import org.rsmod.api.player.hook.PlayerTeleportValidateHook
import org.rsmod.content.other.consumables.ConsumableActivityGate
import org.rsmod.content.raids.toa.raid.supplies.ToaConsumableGate
import org.rsmod.plugin.module.PluginModule

class ToaRaidModule : PluginModule() {
    override fun bind() {
        addSetBinding<PlayerTeleportValidateHook>(ToaTeleportHook::class.java)
        addSetBinding<NpcAttackValidateHook>(ToaAttackHook::class.java)
        addSetBinding<NpcDamageContributor>(ToaDamageContributor::class.java)
        addSetBinding<ConsumableActivityGate>(ToaConsumableGate::class.java)
        addSetBinding<PlayerDeathCleanupHook>(ToaRetrievalDeathHook::class.java)
    }
}
