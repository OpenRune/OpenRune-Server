package org.rsmod.content.skills.crafting.scripts

import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.table.crafting.CraftingHandRow
import org.rsmod.content.skills.crafting.CraftingMode
import org.rsmod.content.skills.crafting.registerHeldCrafting
import org.rsmod.content.skills.crafting.selectCraftingProduct
import org.rsmod.content.skills.crafting.toCraftingProduct
import org.rsmod.content.skills.crafting.util.isKnifeFletching
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class HeldCraftingScript : PluginScript() {
    override fun ScriptContext.startup() {
        val products = CraftingHandRow.all()
            .map { it.toCraftingProduct() }
            .filter { it.section.mode != CraftingMode.SERVICE }
        registerHeldCrafting(products)

        // Native "Fletch" op on the bark itself shares the knife-on-bark selection/worker.
        val fletching = products.filter { it.isKnifeFletching() }
        for (bark in fletching.flatMap { p -> p.inputs.map { it.internal } }.distinct()) {
            val recipes = fletching.filter { p -> p.inputs.any { it.internal == bark } }
            onOpHeld1(bark) {
                if (inv[it.slot]?.id != it.obj.id) {
                    return@onOpHeld1
                }
                selectCraftingProduct(recipes.first().section, recipes)
            }
        }
    }
}
