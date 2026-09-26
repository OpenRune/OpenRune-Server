package org.rsmod.content.raids.toa.raid

import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.player.hook.PlayerTeleportValidateHook
import org.rsmod.content.raids.toa.raid.encounter.crondis.zebak.ZebakSwimAttackHook
import org.rsmod.plugin.module.PluginModule

/**
 * Set bindings for the raid, the same way WildernessModule binds its teleport hook. Content
 * PluginModules are auto-loaded; reflection-instantiated, so a class with a no-arg constructor.
 */
class ToaRaidModule : PluginModule() {
    override fun bind() {
        addSetBinding<PlayerTeleportValidateHook>(ToaTeleportHook::class.java)
        // Zebak's Tidal Waves: no starting combat while swimming (PvNCombat asks every hook).
        addSetBinding<NpcAttackValidateHook>(ZebakSwimAttackHook::class.java)
    }
}
