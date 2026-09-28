package org.rsmod.content.bosses.barrows

import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.player.hook.PlayerPostTickHook
import org.rsmod.plugin.module.PluginModule

class BarrowsModule : PluginModule() {
    override fun bind() {
        addSetBinding<NpcDeathKillHook>(BarrowsKillHook::class.java)
        addSetBinding<NpcAttackValidateHook>(BarrowsAttackHook::class.java)
        addSetBinding<PlayerPostTickHook>(BarrowsPresenceHook::class.java)
    }
}
