package org.rsmod.content.skills.crafting.scripts

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpContentLoc1
import org.rsmod.api.script.onOpContentLoc2
import org.rsmod.api.script.onOpContentMixedLocU
import org.rsmod.api.script.onOpLocCategoryU
import org.rsmod.api.table.crafting.CraftingFacilitiesRow
import org.rsmod.content.skills.crafting.CraftingProduct
import org.rsmod.content.skills.crafting.CraftingSection
import org.rsmod.content.skills.crafting.beginCraft
import org.rsmod.content.skills.crafting.selectCraftingProduct
import org.rsmod.content.skills.crafting.toCraftingProduct
import org.rsmod.content.skills.crafting.util.CraftingConstants
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FacilityCraftingScript : PluginScript() {
    private val rowsBySection: Map<String, List<CraftingFacilitiesRow>> by lazy {
        CraftingFacilitiesRow.all().groupBy { it.section }
    }

    private class Facility(
        val section: CraftingSection,
        val content: String,
        val materialOnLoc: Boolean = false,
        val products: List<CraftingProduct>,
    )

    private fun facilities(): List<Facility> =
        listOf(
            Facility(
                CraftingSection.SPINNING,
                CraftingConstants.CONTENT_SPINNING_WHEEL,
                materialOnLoc = true,
                products = spinningProducts(CraftingConstants.ANIM_SPINNING),
            ),
            Facility(
                CraftingSection.WEAVING,
                CraftingConstants.CONTENT_LOOM,
                materialOnLoc = true,
                products = weavingProducts,
            ),
            Facility(
                CraftingSection.POTTERY_SHAPING,
                CraftingConstants.CONTENT_POTTERY_WHEEL,
                products = shapingProducts,
            ),
            Facility(
                CraftingSection.POTTERY_FIRING,
                CraftingConstants.CONTENT_POTTERY_OVEN,
                products = firingProducts,
            ),
        )

    private fun spinningProducts(anim: String): List<CraftingProduct> =
        products(CraftingSection.SPINNING) { row ->
                row.toCraftingProduct(anim = anim, requiresMaterialsToShow = true)
            }
            .sortedBy { product ->
                val index = SPINNING_MENU_ORDER.indexOf(product.output)
                if (index == -1) SPINNING_MENU_ORDER.size else index
            }

    private val weavingProducts: List<CraftingProduct> by lazy {
        products(CraftingSection.WEAVING) {
            it.toCraftingProduct(requiresMaterialsToShow = true)
        }
    }

    private val shapingProducts: List<CraftingProduct> by lazy {
        products(CraftingSection.POTTERY_SHAPING) {
            it.toCraftingProduct(requiresMaterialsToShow = true)
        }
    }

    private val firingProducts: List<CraftingProduct> by lazy {
        products(CraftingSection.POTTERY_FIRING) { row ->
                row.toCraftingProduct(
                    requiresMaterialsToShow = row.output.first().internalName !in OVEN_DEFAULT_RECIPES,
                )
            }
            .sortedBy { product ->
                val index = OVEN_DEFAULT_RECIPES.indexOf(product.output)
                if (index == -1) OVEN_DEFAULT_RECIPES.size else index
            }
    }

    private val smeltingProducts: List<CraftingProduct> by lazy {
        products(CraftingSection.GLASS_SMELTING) { it.toCraftingProduct() }
    }

    private fun products(
        section: CraftingSection,
        adapt: (CraftingFacilitiesRow) -> CraftingProduct,
    ): List<CraftingProduct> = rowsBySection[section.id].orEmpty().map(adapt)

    override fun ScriptContext.startup() {
        for (facility in facilities()) {
            val products = facility.products
            onOpContentLoc1(facility.content) {
                selectCraftingProduct(facility.section, products, facility = it.loc)
            }
            onOpContentLoc2(facility.content) {
                selectCraftingProduct(facility.section, products, facility = it.loc)
            }
            if (facility.materialOnLoc) {
                registerMaterialOnLoc(facility.section, facility.content, products)
            }
            registerProductOnLoc(facility.content, products)
        }

        for (product in firingProducts) {
            val input = product.inputs.firstOrNull()?.internal ?: continue
            onOpContentMixedLocU(CraftingConstants.CONTENT_POTTERY_OVEN, input) {
                openForInputOnOven(product, facility = it.loc)
            }
        }

        objDialogueOnLoc(
            CraftingConstants.CONTENT_POTTERY_WHEEL,
            CraftingConstants.CLAY,
            "This clay is too hard to craft.<br>You'll need to soften it with some water.",
        )
        onOpContentMixedLocU(CraftingConstants.CONTENT_POTTERY_WHEEL, CraftingConstants.SOFT_CLAY) {
            selectCraftingProduct(CraftingSection.POTTERY_SHAPING, shapingProducts, facility = it.loc)
        }

        for (item in listOf(CraftingConstants.BUCKET_OF_SAND, CraftingConstants.SODA_ASH)) {
            onOpLocCategoryU(CraftingConstants.CATEGORY_FURNACE, item) {
                selectCraftingProduct(CraftingSection.GLASS_SMELTING, smeltingProducts)
            }
        }
    }

    private fun ScriptContext.registerMaterialOnLoc(
        section: CraftingSection,
        content: String,
        products: List<CraftingProduct>,
    ) {
        val byInput = products.groupBy { product -> product.inputs.firstOrNull()?.internal }
        for ((input, matching) in byInput) {
            if (input == null) {
                continue
            }
            onOpContentMixedLocU(content, input) {
                selectCraftingProduct(section, matching, facility = it.loc)
            }
        }
    }

    private fun ScriptContext.registerProductOnLoc(content: String, products: List<CraftingProduct>) {
        val materials = products.flatMap { product -> product.inputs.map { it.internal } }.toSet()
        val registered = mutableSetOf<String>()
        for (product in products) {
            val output = product.output
            val message = product.alreadyProcessedMessage ?: continue
            if (output in materials || !registered.add(output)) {
                continue
            }
            objDialogueOnLoc(content, output, message)
        }
    }

    private fun ScriptContext.objDialogueOnLoc(content: String, obj: String, message: String) {
        onOpContentMixedLocU(content, obj) { objbox(obj, message) }
    }

    private suspend fun ProtectedAccess.openForInputOnOven(
        product: CraftingProduct,
        facility: BoundLocInfo,
    ) {
        val input = product.inputs.firstOrNull()?.internal ?: return
        if (inv.count(input) == 1) {
            beginCraft(product, amount = 1, facility = facility)
        } else {
            selectCraftingProduct(product.section, listOf(product), facility = facility)
        }
    }
}

private val SPINNING_MENU_ORDER: List<String> = listOf(
    "obj.ball_of_wool",
    "obj.bow_string",
    "obj.rope",
    "obj.xbows_crossbow_string",
    "obj.magic_string",
)

private val OVEN_DEFAULT_RECIPES: List<String> = listOf(
    "obj.pot_empty",
    "obj.piedish",
    "obj.bowl_empty",
)
