package org.rsmod.content.skills.runecrafting.altar

import jakarta.inject.Inject
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.api.table.runecrafting.RunecraftingAltarsRow
import org.rsmod.content.skills.runecrafting.action.RunecraftAction.craftCore
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CoreEvents @Inject constructor(
    private val xpMods: XpModifiers,
) : PluginScript() {

    private val coresByAltar = mapOf(
        "loc.mind_altar" to "obj.camdozaal_golem_core_mind",
        "loc.body_altar" to "obj.camdozaal_golem_core_body",
        "loc.chaos_altar" to "obj.camdozaal_golem_core_chaos"
    )

    override fun ScriptContext.startup() {
        RunecraftingAltarsRow.all().forEach { altar ->
            val coreItem = coresByAltar[altar.altarObject.internalName] ?: return@forEach
            onOpLocU(altar.altarObject.internalName, coreItem) {
                craftCore(altar.rune, coreItem, xpMods)
            }
        }
    }
}
