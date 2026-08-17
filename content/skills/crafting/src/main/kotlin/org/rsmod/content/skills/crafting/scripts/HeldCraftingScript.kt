package org.rsmod.content.skills.crafting.scripts

import org.rsmod.api.table.crafting.CraftingHandRow
import org.rsmod.content.skills.crafting.CraftingMode
import org.rsmod.content.skills.crafting.registerHeldCrafting
import org.rsmod.content.skills.crafting.toCraftingProduct
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Held (inventory) crafting */
class HeldCraftingScript : PluginScript() {

    override fun ScriptContext.startup() {
        val products = CraftingHandRow.all()
            .map { it.toCraftingProduct() }
            .filter { it.section.mode != CraftingMode.SERVICE }
        registerHeldCrafting(products)
    }
}
