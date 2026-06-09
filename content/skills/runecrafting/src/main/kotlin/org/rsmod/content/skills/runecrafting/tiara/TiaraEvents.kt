package org.rsmod.content.skills.runecrafting.tiara

import jakarta.inject.Inject
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.api.table.runecrafting.RunecraftingAltarsRow
import org.rsmod.content.skills.runecrafting.tiara.TiaraAction.createSpecialTiara
import org.rsmod.content.skills.runecrafting.tiara.TiaraAction.createTiara
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class TiaraEvents @Inject constructor(
    private val xpMods: XpModifiers,
) : PluginScript() {
    override fun ScriptContext.startup() {
        RunecraftingAltarsRow.all().filter { it.tiara != null && it.talisman != null }.forEach { altar ->
            val talisman = altar.talisman!!
            val tiaraDef = altar.tiara!!
            val altarLoc = altar.altarObject.internalName

            onOpHeldU(talisman.internalName, "obj.tiara") {
                createTiara(talisman.internalName, tiaraDef, xpMods)
            }

            onOpLocU(altarLoc, "obj.tiara") {
                createTiara(talisman.internalName, tiaraDef, xpMods)
            }
        }

        onOpHeldU("obj.elemental_talisman", "obj.tiara_gold") {
            createSpecialTiara(
                "obj.elemental_talisman",
                "obj.tiara_elemental",
                xp = 40,
                xpMods = xpMods,
            )
        }

        onOpHeldU("obj.catalytic_talisman", "obj.tiara_gold") {
            createSpecialTiara(
                "obj.catalytic_talisman",
                "obj.tiara_catalytic",
                xp = 52,
                xpMods = xpMods,
            )
        }
    }
}
