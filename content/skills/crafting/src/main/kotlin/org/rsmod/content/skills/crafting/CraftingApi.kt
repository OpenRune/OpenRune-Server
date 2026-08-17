package org.rsmod.content.skills.crafting

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeldU
import org.rsmod.content.skills.crafting.util.CraftingConstants
import org.rsmod.content.skills.crafting.util.meetsUnlocks
import org.rsmod.content.skills.crafting.util.toolEquivalents
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Every recipe the module knows about, keyed by the obj it produces. [craftingProduct] populates
 * this itself, so recipes registered by other modules through the same builder show up too.
 */
object CraftingRecipes {
    private val byOutput = LinkedHashMap<String, LinkedHashSet<CraftingProduct>>()

    /** Called by [craftingProduct] for every recipe built. */
    internal fun register(product: CraftingProduct) {
        byOutput.getOrPut(product.output) { LinkedHashSet() } += product
    }

    /** Recipes producing [output]. Some objs have more than one, such as bow string. */
    fun forOutput(output: String): List<CraftingProduct> = byOutput[output]?.toList() ?: emptyList()

    /** Every obj the module can craft. */
    fun outputs(): Set<String> = byOutput.keys

    /**
     * Everything needed to make [crafts] of [output]. This will be the consumed inputs, any thread, and
     * the recipe's tools. Returns null when nothing crafts [output]. Passing a [player] picks the
     * variant they could actually craft, which is important if the desired output has a quest requirement.
     */
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

    /** Counts the thread spools required for the provided inputs. */
    private fun threadFor(product: CraftingProduct, crafts: Int): List<CraftingMaterial> {
        if (!product.consumesThread) {
            return emptyList()
        }
        val perSpool = CraftingConstants.THREAD_USES_PER_SPOOL
        val spools = (crafts + perSpool - 1) / perSpool
        return listOf(CraftingMaterial(CraftingConstants.THREAD, spools))
    }
}

/** One line of a recipe's shopping list. A [tool] is required but never consumed. */
data class CraftingMaterial(val obj: String, val count: Int, val tool: Boolean = false)

/**
 * Registers [products] as held (inventory) crafting recipes, one `onOpHeldU` per click pair.
 *
 * A recipe's click targets are its [CraftingProduct.triggers], or its inputs when it names none.
 * It starts from any target used on any other, or from any of its tools used on any target, in
 * either order. Tools match through [toolEquivalents], so a stand-in works as well as the named
 * one. A birdhouse therefore starts from log on clockwork, hammer on log, or chisel on clockwork.
 *
 * Recipes sharing a pair register once, and the click resolves to whichever of them the player can
 * actually make. Other modules can register their own recipes here after building them with
 * [craftingProduct].
 */
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

/** Every click pair that should start this recipe. */
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

/** Keys a pair under whichever obj owns a competing default handler. */
private fun CraftingProduct.orderPair(a: String, b: String): Pair<String, String> = if (ownsDefaultHandler(b) && !ownsDefaultHandler(a)) b to a else a to b

private fun CraftingProduct.ownsDefaultHandler(obj: String): Boolean = section.ownsDefaultHandler(obj) && inputs.any { it.internal == obj }

/** Resolves which recipe a click pair meant, then starts it the way its section says to. */
private suspend fun ProtectedAccess.craftFromClick(
    candidates: List<CraftingProduct>,
    combine: suspend ProtectedAccess.(CraftingProduct) -> Unit,
) {
    val unlocked = candidates.filter { player.meetsUnlocks(it) }
    if (unlocked.isEmpty()) {
        candidates.firstNotNullOfOrNull { it.lockedMessage }?.let { mesbox(it) }
        return
    }
    // Falling back to the first lets the craft fail with its own "you need" message rather than
    // the click doing nothing at all.
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
