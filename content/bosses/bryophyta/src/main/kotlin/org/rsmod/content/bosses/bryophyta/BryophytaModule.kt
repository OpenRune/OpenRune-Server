package org.rsmod.content.bosses.bryophyta

import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.player.hook.PlayerObjTakeValidateHook
import org.rsmod.plugin.module.PluginModule

class BryophytaModule : PluginModule() {
    override fun bind() {
        addSetBinding<PlayerObjTakeValidateHook>(BryophytaGroundItemHook::class.java)
        addSetBinding<NpcAttackValidateHook>(BryophytaPruneHook::class.java)
    }
}
