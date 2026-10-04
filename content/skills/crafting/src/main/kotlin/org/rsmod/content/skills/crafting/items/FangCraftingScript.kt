package org.rsmod.content.skills.crafting.items

import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.table.crafting.CraftingHandRow
import org.rsmod.content.skills.Material
import org.rsmod.content.skills.crafting.CraftingProduct
import org.rsmod.content.skills.crafting.CraftingRecipes
import org.rsmod.content.skills.crafting.CraftingSection
import org.rsmod.content.skills.crafting.craftInstantly
import org.rsmod.content.skills.crafting.registerHeldCrafting
import org.rsmod.content.skills.crafting.toCraftingProduct
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** The cache exposes Etch separately from the older unetched-fang assembly recipe. */
class FangCraftingScript : PluginScript() {
    override fun ScriptContext.startup() {
        registerEtching(FANG, ETCHED, 86, "araxyte fang")
        registerEtching("obj.elder_venator_fang", "obj.etched_elder_venator_fang", 84, "elder venator fang")
        val rancour = CraftingHandRow.all().map { it.toCraftingProduct() }
            .single { it.output == "obj.amulet_of_rancour" }
            .let { recipe ->
                recipe.copy(inputs = recipe.inputs.map {
                    if (it.internal == FANG) Material(ETCHED, it.count) else it
                })
            }
        CraftingRecipes.register(rancour)
        registerHeldCrafting(rancour)
    }

    private fun ScriptContext.registerEtching(fang: String, etched: String, level: Int, name: String) {
        val etching = CraftingProduct(
            section = CraftingSection.COMBINING,
            output = etched,
            inputs = listOf(Material(fang, 1)),
            level = level,
            xp = 0.0,
            ticks = listOf(1),
            tools = listOf("obj.chisel"),
            actionName = "etch an $name",
        )
        CraftingRecipes.register(etching)
        onOpHeld1(fang) { craftInstantly(etching) }
        registerHeldCrafting(etching)
    }

    private companion object {
        const val FANG = "obj.araxyte_fang"
        const val ETCHED = "obj.etched_araxyte_fang"
    }
}
