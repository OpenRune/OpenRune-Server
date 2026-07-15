package org.rsmod.content.other.ironman

import org.rsmod.api.death.PlayerDeathCleanupHook
import org.rsmod.api.player.hook.PlayerObjTakeValidateHook
import org.rsmod.plugin.module.PluginModule

public class IronmanModule : PluginModule() {
    override fun bind() {
        addSetBinding<PlayerObjTakeValidateHook>(IronmanObjTakeHook::class.java)
        addSetBinding<PlayerDeathCleanupHook>(HardcoreIronmanDeathHook::class.java)
    }
}
