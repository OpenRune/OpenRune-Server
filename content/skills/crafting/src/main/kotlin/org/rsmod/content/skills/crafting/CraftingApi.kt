package org.rsmod.content.skills.crafting

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeldU
import org.rsmod.content.skills.crafting.util.CraftingConstants
import org.rsmod.content.skills.crafting.util.meetsUnlocks
import org.rsmod.content.skills.crafting.util.toolEquivalents
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.ScriptContext

object CraftingRecipes {
    private val byOutput = LinkedHashMap<String, LinkedHashSet<CraftingProduct>>()

    internal fun register(product: CraftingProduct) {
        byOutput.getOrPut(product.output) { LinkedHashSet() } += product
    }

    fun forOutput(output: String): List<CraftingProduct> = byOutput[output]?.toList() ?: emptyList()

    fun outputs(): Set<String> = byOutput.keys

    fun materialsFor(
        output: String,
        crafts: Int,
        player: Player? = null,
    ): List<CraftingMaterial>? {
        val recipes = forOutput(output)
        val usable = player?.let { p -> recipes.firstOrNull { p.meetsUnlocks(it) } }
        val product = usable ?: recipes.firstOrNull() ?: return null

        val inputs = product.inputs.map { CraftingMaterial(it.internal, it.count * crafts) }
        val tools = product.tools.map { CraftingMaterial(it, count = 1, tool = true) }
        return inputs + threadFor(product, crafts) + tools
    }

    private fun threadFor(product: CraftingProduct, crafts: Int): List<CraftingMaterial> {
        if (!product.consumesThread) {
            return emptyList()
        }
        val perSpool = CraftingConstants.THREAD_USES_PER_SPOOL
        val spools = (crafts + perSpool - 1) / perSpool
        return listOf(CraftingMaterial(CraftingConstants.THREAD, spools))
    }
}

data class CraftingMaterial(val obj: String, val count: Int, val tool: Boolean = false)

fun ScriptContext.registerHeldCrafting(
    products: List<CraftingProduct>,
    combine: suspend ProtectedAccess.(CraftingProduct) -> Unit = { craftInstantly(it) },
) {
    val registrations = LinkedHashMap<Set<String>, PairRegistration>()
    for (product in products) {
        for (pair in product.clickPairs()) {
            val (first, second) = pair
            val reg = registrations.getOrPut(setOf(first, second)) { PairRegistration(first, second) }
            if (product.ownsDefaultHandler(first) && reg.first != first) {
                reg.first = first
                reg.second = second
            }
            if (product !in reg.products) {
                reg.products += product
            }
        }
    }
    for (reg in registrations.values) {
        onOpHeldU(reg.first, reg.second) { craftFromClick(reg.products, combine) }
    }
}

fun ScriptContext.registerHeldCrafting(
    product: CraftingProduct,
    combine: suspend ProtectedAccess.(CraftingProduct) -> Unit = { craftInstantly(it) },
): Unit = registerHeldCrafting(listOf(product), combine)

private class PairRegistration(var first: String, var second: String) {
    val products = mutableListOf<CraftingProduct>()
}

private fun CraftingProduct.clickPairs(): List<Pair<String, String>> {
    val ingredients = triggers.ifEmpty { inputs.map { it.internal } }.distinct()
    val pairs = mutableListOf<Pair<String, String>>()
    for (i in ingredients.indices) {
        for (j in i + 1 until ingredients.size) {
            pairs += orderPair(ingredients[i], ingredients[j])
        }
    }
    for (tool in tools) {
        for (trigger in toolEquivalents(tool)) {
            for (ingredient in ingredients) {
                pairs += orderPair(trigger, ingredient)
            }
        }
    }
    return pairs
}

private fun CraftingProduct.orderPair(a: String, b: String): Pair<String, String> = if (ownsDefaultHandler(b) && !ownsDefaultHandler(a)) b to a else a to b

private fun CraftingProduct.ownsDefaultHandler(obj: String): Boolean = section.ownsDefaultHandler(obj) && inputs.any { it.internal == obj }

private suspend fun ProtectedAccess.craftFromClick(
    candidates: List<CraftingProduct>,
    combine: suspend ProtectedAccess.(CraftingProduct) -> Unit,
) {
    val unlocked = candidates.filter { player.meetsUnlocks(it) }
    if (unlocked.isEmpty()) {
        candidates.firstNotNullOfOrNull { it.lockedMessage }?.let { mesbox(it) }
        return
    }

    val product = unlocked.singleOrNull()
        ?: unlocked.firstOrNull { hasCraftingMaterials(it) }
        ?: unlocked.first()

    val mode = product.section.mode
    val sameSection = unlocked.filter { it.section === product.section }
    when {
        mode == CraftingMode.COMBINE -> combine(product)
        mode == CraftingMode.INSTANT -> craftInstantly(product)
        product.ticksAt(0) <= 0 -> craftInstantly(product)
        else -> selectCraftingProduct(product.section, sameSection)
    }
}
