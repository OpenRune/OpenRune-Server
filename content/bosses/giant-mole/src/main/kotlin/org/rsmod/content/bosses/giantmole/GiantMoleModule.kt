package org.rsmod.content.bosses.giantmole

import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.player.hook.PlayerPostTickHook
import org.rsmod.api.player.hook.SpadeDigHook
import org.rsmod.plugin.module.PluginModule

class GiantMoleModule : PluginModule() {
    override fun bind() {
        addSetBinding<SpadeDigHook>(MoleHillDigHook::class.java)
        addSetBinding<NpcAttackValidateHook>(MoleHoleAttackHook::class.java)
        addSetBinding<NpcDeathKillHook>(GiantMoleDeathHook::class.java)
        addSetBinding<PlayerPostTickHook>(MoleHolePostTickHook::class.java)
    }
}
