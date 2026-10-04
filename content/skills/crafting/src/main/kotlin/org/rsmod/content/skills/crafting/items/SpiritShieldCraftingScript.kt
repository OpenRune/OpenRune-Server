package org.rsmod.content.skills.crafting.items

import org.rsmod.content.skills.Material
import org.rsmod.content.skills.crafting.CraftingProduct
import org.rsmod.content.skills.crafting.CraftingRecipes
import org.rsmod.content.skills.crafting.CraftingSection
import org.rsmod.content.skills.crafting.registerHeldCrafting
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SpiritShieldCraftingScript : PluginScript() {
    override fun ScriptContext.startup() {
        val products = listOf(
            recipe("spirit_shield", "holy_elixir", "blessed_spirit_shield"),
            recipe("blessed_spirit_shield", "spectral_sigil", "spectral"),
            recipe("blessed_spirit_shield", "arcane_sigil", "arcane"),
            recipe("blessed_spirit_shield", "elysian_sigil", "elysian"),
        )
        products.forEach(CraftingRecipes::register)
        registerHeldCrafting(products)
    }

    private fun recipe(shield: String, ingredient: String, output: String) = CraftingProduct(
        section = CraftingSection.COMBINING,
        output = "obj.$output",
        inputs = listOf(Material("obj.$shield", 1), Material("obj.$ingredient", 1)),
        level = 1,
        xp = 0.0,
        ticks = listOf(1),
        successMessage = "You create a ${output.replace('_', ' ')}.",
        actionName = "create a ${output.replace('_', ' ')}",
    )
}
