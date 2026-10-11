package org.rsmod.content.skills.crafting.scripts

import jakarta.inject.Inject
import org.rsmod.api.table.crafting.CraftingHandRow
import org.rsmod.content.skills.crafting.CraftingMode
import org.rsmod.content.skills.crafting.CraftingRecipeProvider
import org.rsmod.content.skills.crafting.CraftingRecipes
import org.rsmod.content.skills.crafting.craftInstantly
import org.rsmod.content.skills.crafting.registerHeldCrafting
import org.rsmod.content.skills.crafting.toCraftingProduct
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class HeldCraftingScript @Inject constructor(private val providers: Set<@JvmSuppressWildcards CraftingRecipeProvider> = emptySet()) : PluginScript() {
    override fun ScriptContext.startup() {
        val products = CraftingHandRow.all()
            .map { it.toCraftingProduct() }
            .filter { it.section.mode != CraftingMode.SERVICE }
        val provided = providers.flatMap { provider -> provider.products.map { it to provider } }.toMap()
        provided.keys.forEach { CraftingRecipes.register(it) }
        registerHeldCrafting(products + provided.keys) { product ->
            val provider = provided[product]
            if (provider == null) craftInstantly(product) else provider.create(this, product)
        }
    }
}
