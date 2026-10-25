package org.rsmod.content.areas.misc.deathsoffice

import org.rsmod.api.death.PlayerDeathCleanupHook
import org.rsmod.plugin.module.PluginModule

class DeathsOfficeModule : PluginModule() {
    override fun bind() {
        addSetBinding<PlayerDeathCleanupHook>(DeathTutorialArrival::class.java)
    }
}
