package org.rsmod.api.death.plugin

import org.rsmod.api.death.NpcDeathDropHook
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.death.NpcDropReceiveHook
import org.rsmod.api.death.PlayerDeathCleanupHook
import org.rsmod.api.death.PlayerDeathDropHook
import org.rsmod.api.death.PlayerDeathHook
import org.rsmod.api.death.PlayerDeathPreDropHook
import org.rsmod.api.death.PlayerDeathStorageHook
import org.rsmod.api.death.PlayerGraveHook
import org.rsmod.api.death.PlayerRespawnHook
import org.rsmod.api.death.PvPAttackValidateHook
import org.rsmod.api.death.PvPPlayerHitHook
import org.rsmod.api.death.PvPSkullHook
import org.rsmod.api.death.PvPSpecialAttackHook
import org.rsmod.plugin.module.PluginModule

public class DeathDropHooksModule : PluginModule() {
    override fun bind() {
        newSetBinding<NpcDeathDropHook>()
        newSetBinding<NpcDeathKillHook>()
        newSetBinding<NpcDropReceiveHook>()
        newSetBinding<PlayerDeathCleanupHook>()
        newSetBinding<PlayerDeathDropHook>()
        newSetBinding<PlayerDeathHook>()
        newSetBinding<PlayerDeathPreDropHook>()
        newSetBinding<PlayerDeathStorageHook>()
        newSetBinding<PlayerGraveHook>()
        newSetBinding<PlayerRespawnHook>()
        newSetBinding<PvPAttackValidateHook>()
        newSetBinding<PvPSkullHook>()
        newSetBinding<PvPPlayerHitHook>()
        newSetBinding<PvPSpecialAttackHook>()
        addSetBinding<PlayerDeathHook>(UimPlayerDeathHook::class.java)
        addSetBinding<PlayerDeathHook>(StandardPvmDeathHook::class.java)
    }
}
