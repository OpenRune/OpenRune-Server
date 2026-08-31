package org.rsmod.content.skills.crafting.scripts

import org.rsmod.api.script.onOpLocCategoryU
import org.rsmod.content.skills.crafting.interfaces.openGoldCrafting
import org.rsmod.content.skills.crafting.interfaces.openSilverCrafting
import org.rsmod.content.skills.crafting.util.CraftingConstants
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class JewelleryScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLocCategoryU(CraftingConstants.CATEGORY_FURNACE, CraftingConstants.GOLD_BAR) {
            openGoldCrafting()
        }
        onOpLocCategoryU(CraftingConstants.CATEGORY_FURNACE, CraftingConstants.SILVER_BAR) {
            openSilverCrafting()
        }
    }
}
