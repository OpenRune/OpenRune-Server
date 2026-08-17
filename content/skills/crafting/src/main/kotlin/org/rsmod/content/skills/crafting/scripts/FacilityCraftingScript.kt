package org.rsmod.content.skills.crafting.scripts

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLocCategoryU
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.table.crafting.CraftingFacilitiesRow
import org.rsmod.content.skills.crafting.CraftingProduct
import org.rsmod.content.skills.crafting.CraftingSection
import org.rsmod.content.skills.crafting.beginCraft
import org.rsmod.content.skills.crafting.selectCraftingProduct
import org.rsmod.content.skills.crafting.toCraftingProduct
import org.rsmod.content.skills.crafting.util.CraftingConstants
import org.rsmod.content.skills.crafting.util.CraftingGamevals
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FacilityCraftingScript : PluginScript() {
    private val rowsBySection: Map<String, List<CraftingFacilitiesRow>> by lazy {
        CraftingFacilitiesRow.all().groupBy { it.section }
    }

    private class Facility(
        val section: CraftingSection,
        val locs: List<String>,
        val materialOnLoc: Boolean = false,
        val products: (loc: String) -> List<CraftingProduct>,
    )

    /** The facility list, one entry per way of crafting at a loc. */
    private fun facilities(): List<Facility> {
        val spinning60 = spinningProducts(CraftingConstants.ANIM_SPINNING_60)
        val spinning90 = spinningProducts(CraftingConstants.ANIM_SPINNING_90)

        return listOf(
            Facility(CraftingSection.SPINNING, CraftingConstants.SPINNING_WHEELS, materialOnLoc = true) { loc ->
                if (loc in CraftingConstants.SPINNING_WHEELS_60) spinning60 else spinning90
            },
            Facility(CraftingSection.WEAVING, CraftingConstants.LOOMS, materialOnLoc = true) { weavingProducts },
            Facility(CraftingSection.POTTERY_SHAPING, CraftingConstants.POTTERY_WHEELS) { shapingProducts },
            Facility(CraftingSection.POTTERY_FIRING, CraftingConstants.POTTERY_OVENS) { firingProducts },
        )
    }

    /** The spinning menu, listing its standard recipes first and material gated ones after. */
    private fun spinningProducts(anim: String): List<CraftingProduct> =
        products(CraftingSection.SPINNING) { row ->
                row.toCraftingProduct(
                    anim = anim,
                    requiresMaterialsToShow =
                        row.output.first().internalName !in SPINNING_DEFAULT_RECIPES,
                )
            }
            .sortedBy { product ->
                val index = SPINNING_DEFAULT_RECIPES.indexOf(product.output)
                if (index == -1) SPINNING_DEFAULT_RECIPES.size else index
            }

    /** The loom lists only what the player can currently weave, since every recipe is gated. */
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

    /** Shows three pottery defaults, with the other firing recipes requiring its unfired material before showing. */
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

    /** Builds a section's products from the facilities table. */
    private fun products(
        section: CraftingSection,
        adapt: (CraftingFacilitiesRow) -> CraftingProduct,
    ): List<CraftingProduct> = rowsBySection[section.id].orEmpty().map(adapt)

    override fun ScriptContext.startup() {
        for (facility in facilities()) {
            for (loc in CraftingGamevals.filterResolvable(facility.locs)) {
                val products = facility.products(loc)
                onOpLoc1(loc) { selectCraftingProduct(facility.section, products, facility = it.loc) }
                onOpLoc2(loc) { selectCraftingProduct(facility.section, products, facility = it.loc) }
                if (facility.materialOnLoc) {
                    registerMaterialOnLoc(facility.section, loc, products)
                }
                registerProductOnLoc(loc, products)
            }
        }

        for (loc in CraftingGamevals.filterResolvable(CraftingConstants.POTTERY_OVENS)) {
            for (product in firingProducts) {
                val input = product.inputs.firstOrNull()?.internal ?: continue
                if (!CraftingGamevals.exists(input)) continue
                onOpLocU(loc, input) { openForInputOnOven(product, facility = it.loc) }
            }
        }

        for (loc in CraftingGamevals.filterResolvable(CraftingConstants.POTTERY_WHEELS)) {
            objDialogueOnLoc(loc, CraftingConstants.CLAY, "This clay is too hard to craft.<br>You'll need to soften it with some water.")
            onOpLocU(loc, CraftingConstants.SOFT_CLAY) {
                selectCraftingProduct(CraftingSection.POTTERY_SHAPING, shapingProducts, facility = it.loc)
            }
        }

        for (item in listOf(CraftingConstants.BUCKET_OF_SAND, CraftingConstants.SODA_ASH)) {
            onOpLocCategoryU(CraftingConstants.CATEGORY_FURNACE, item) {
                selectCraftingProduct(CraftingSection.GLASS_SMELTING, smeltingProducts)
            }
        }
    }

    /** Opens just the recipe a dragged material feeds, rather than the facility's whole list. */
    private fun ScriptContext.registerMaterialOnLoc(
        section: CraftingSection,
        loc: String,
        products: List<CraftingProduct>,
    ) {
        val byInput = products.groupBy { product -> product.inputs.firstOrNull()?.internal }
        for ((input, matching) in byInput) {
            if (input == null || !CraftingGamevals.exists(input)) {
                continue
            }
            onOpLocU(loc, input) { selectCraftingProduct(section, matching, facility = it.loc) }
        }
    }

    /** Registers the dialogue a facility shows when one of its own products is used on it. */
    private fun ScriptContext.registerProductOnLoc(loc: String, products: List<CraftingProduct>) {
        val materials = products.flatMap { product -> product.inputs.map { it.internal } }.toSet()
        val registered = mutableSetOf<String>()
        for (product in products) {
            val output = product.output
            val message = product.alreadyProcessedMessage ?: continue
            if (output in materials || !CraftingGamevals.exists(output) || !registered.add(output)) {
                continue
            }
            objDialogueOnLoc(loc, output, message)
        }
    }

    /**
     * Registers the dialogue [loc] shows when [obj] is used on it.
     * Covers both a facility rejecting its own product and a one-off such as dry clay.
     */
    private fun ScriptContext.objDialogueOnLoc(loc: String, obj: String, message: String) {
        onOpLocU(loc, obj) { objbox(obj, message) }
    }

    /** Handles an unfired item dragged onto the oven, firing it outright when only one is held. */
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

private val SPINNING_DEFAULT_RECIPES: List<String> = listOf(
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
