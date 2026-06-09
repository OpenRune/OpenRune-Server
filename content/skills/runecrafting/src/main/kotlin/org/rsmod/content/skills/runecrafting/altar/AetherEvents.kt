package org.rsmod.content.skills.runecrafting.altar

import jakarta.inject.Inject
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.skills.runecrafting.action.RunecraftAction.craftAether
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class AetherEvents @Inject constructor(
    private val xpMods: XpModifiers,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLocU("loc.archeus_altar_soul", "obj.cosmic_soul_catalyst") {
            craftAether(xpMods)
        }
    }
}
