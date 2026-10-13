package org.rsmod.content.minigames.gauntlet

import org.rsmod.api.death.PlayerRespawnHook
import org.rsmod.api.player.hook.PlayerTeleportValidateHook
import org.rsmod.plugin.module.PluginModule

class GauntletModule : PluginModule() {
    override fun bind() {
        addSetBinding<PlayerRespawnHook>(GauntletRespawnHook::class.java)
        addSetBinding<PlayerTeleportValidateHook>(GauntletTeleportHook::class.java)
    }
}
