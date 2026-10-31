package org.rsmod.content.skills.crafting.util

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.skills.crafting.CraftingProduct
import org.rsmod.content.skills.crafting.CraftingSection

private val TOOL_EQUIVALENTS: Map<String, List<String>> = mapOf(
    CraftingConstants.HAMMER to listOf(
        CraftingConstants.HAMMER,
        CraftingConstants.IMCANDO_HAMMER,
        CraftingConstants.IMCANDO_HAMMER_OFFHAND,
    ),
    CraftingConstants.KNIFE to listOf(
        CraftingConstants.KNIFE,
        CraftingConstants.FLETCHING_KNIFE,
    ),
    CraftingConstants.NEEDLE to listOf(
        CraftingConstants.NEEDLE,
        CraftingConstants.COSTUME_NEEDLE,
    ),
)

private val WORN_TOOLS: Set<String> = setOf(
    CraftingConstants.IMCANDO_HAMMER,
    CraftingConstants.IMCANDO_HAMMER_OFFHAND,
)

fun toolEquivalents(tool: String): List<String> = TOOL_EQUIVALENTS[tool] ?: listOf(tool)

fun ProtectedAccess.hasCraftingTool(tool: String): Boolean = toolEquivalents(tool).any { holds(it) }

/** Knife-section recipes that award Fletching XP; only these use the special fletching knife's faster ticks. */
fun CraftingProduct.isKnifeFletching(): Boolean =
    section == CraftingSection.KNIFE && extraXp.any { it.stat == CraftingConstants.STAT_FLETCHING }

/** Ticks for [cycle]; knife-fletching uses the first tick entry unless the special knife is held. */
fun ProtectedAccess.ticksFor(product: CraftingProduct, cycle: Int): Int =
    if (product.isKnifeFletching() && !holdsFletchingKnife()) {
        product.ticks.first()
    } else {
        product.ticksAt(cycle)
    }

fun ProtectedAccess.holdsImcandoHammer(): Boolean = IMCANDO_HAMMERS.any { holds(it) }

private fun ProtectedAccess.holdsFletchingKnife(): Boolean =
    inv.contains(CraftingConstants.FLETCHING_KNIFE) || CraftingConstants.FLETCHING_KNIFE in player.worn

fun ProtectedAccess.holdsCostumeNeedle(): Boolean = inv.contains(CraftingConstants.COSTUME_NEEDLE)

private fun ProtectedAccess.holds(tool: String): Boolean {
    if (inv.contains(tool)) {
        return true
    }
    val wearable = tool in WORN_TOOLS
    return wearable && tool in player.worn
}

private val IMCANDO_HAMMERS = listOf(
    CraftingConstants.IMCANDO_HAMMER,
    CraftingConstants.IMCANDO_HAMMER_OFFHAND,
)
