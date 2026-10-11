package org.rsmod.content.other.pouches

import org.rsmod.api.death.PlayerDeathDropHook
import org.rsmod.api.player.hook.PlayerObjTakeStorageHook
import org.rsmod.content.other.pouches.lootingbag.LootingBagTakeHook
import org.rsmod.plugin.module.PluginModule

class PouchesModule : PluginModule() {
    override fun bind() {
        addSetBinding<PlayerDeathDropHook>(PouchDeathHook::class.java)
        addSetBinding<PlayerObjTakeStorageHook>(LootingBagTakeHook::class.java)
    }
}
