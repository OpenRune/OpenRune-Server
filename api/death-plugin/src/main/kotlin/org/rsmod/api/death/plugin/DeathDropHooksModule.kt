package org.rsmod.api.death.plugin

import org.rsmod.api.death.NpcDeathDropHook
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.death.PlayerDeathCleanupHook
import org.rsmod.api.death.PlayerDeathHook
import org.rsmod.api.death.PvPAttackValidateHook
import org.rsmod.api.death.PvPPlayerHitHook
import org.rsmod.api.death.PvPSkullHook
import org.rsmod.plugin.module.PluginModule

public class DeathDropHooksModule : PluginModule() {
    override fun bind() {
        newSetBinding<NpcDeathDropHook>()
        newSetBinding<NpcDeathKillHook>()
        newSetBinding<PlayerDeathCleanupHook>()
        newSetBinding<PlayerDeathHook>()
        newSetBinding<PvPAttackValidateHook>()
        newSetBinding<PvPSkullHook>()
        newSetBinding<PvPPlayerHitHook>()
        addSetBinding<PlayerDeathHook>(UimPlayerDeathHook::class.java)
        addSetBinding<PlayerDeathHook>(StandardPvmDeathHook::class.java)
    }
}
