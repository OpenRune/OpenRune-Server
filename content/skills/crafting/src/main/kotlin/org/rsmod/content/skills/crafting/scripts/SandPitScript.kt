package org.rsmod.content.skills.crafting.scripts

import org.rsmod.api.script.onOpContentMixedLocU
import org.rsmod.api.table.crafting.CraftingFacilitiesRow
import org.rsmod.content.skills.crafting.CraftingProduct
import org.rsmod.content.skills.crafting.CraftingSection
import org.rsmod.content.skills.crafting.selectCraftingProduct
import org.rsmod.content.skills.crafting.toCraftingProduct
import org.rsmod.content.skills.crafting.util.CraftingConstants
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SandPitScript : PluginScript() {
    private val products: List<CraftingProduct> by lazy {
        CraftingFacilitiesRow.all()
            .filter { it.section == CraftingSection.SAND_PIT.id }
            .map { it.toCraftingProduct() }
    }

    override fun ScriptContext.startup() {
        if (products.isEmpty()) {
            return
        }
        onOpContentMixedLocU(CraftingConstants.CONTENT_SAND_PIT, CraftingConstants.BUCKET_EMPTY) {
            selectCraftingProduct(CraftingSection.SAND_PIT, products, facility = it.loc)
        }
    }
}
