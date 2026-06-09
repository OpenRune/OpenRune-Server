package org.rsmod.content.skills.runecrafting.altar

import jakarta.inject.Inject
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.api.table.runecrafting.RunecraftingAltarsRow
import org.rsmod.content.skills.runecrafting.action.RunecraftAction.craftCombination
import org.rsmod.content.skills.runecrafting.tiara.TiaraAction.createTiara
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ComboEvents @Inject constructor(
    private val xpMods: XpModifiers,
) : PluginScript() {
    override fun ScriptContext.startup() {
        RunecraftingAltarsRow.all().forEach { altar ->
            if (altar.combo.isEmpty()) {
                return@forEach
            }

            val altarLoc = altar.altarObject.internalName
            val registeredTalismans = hashSetOf<String>()

            altar.combo.forEach { combo ->
                val talisman = combo.talisman?.internalName ?: return@forEach
                if (!registeredTalismans.add(talisman)) {
                    return@forEach
                }

                onOpLocU(altarLoc, talisman) {
                    val matchingCombos =
                        altar.combo.filter { it.talisman?.internalName == talisman }
                    val selectedCombo =
                        matchingCombos.firstOrNull { candidate ->
                            val input = candidate.runeInput?.internalName
                            input != null &&
                                inv.contains(input) &&
                                (inv.contains("obj.blankrune_high") || inv.contains("obj.gotr_guardian_essence"))
                        }

                    if (selectedCombo != null) {
                        craftCombination(selectedCombo, xpMods)
                        return@onOpLocU
                    }

                    val tiaraDef = altar.tiara
                    if (inv.contains("obj.tiara") && tiaraDef != null) {
                        createTiara(talisman, tiaraDef, xpMods)
                        return@onOpLocU
                    }

                    craftCombination(matchingCombos.first(), xpMods)
                }
            }
        }
    }
}
