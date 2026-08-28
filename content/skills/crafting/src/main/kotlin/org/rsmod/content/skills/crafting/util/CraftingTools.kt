package org.rsmod.content.skills.crafting.util

import org.rsmod.api.player.protect.ProtectedAccess

private val TOOL_EQUIVALENTS: Map<String, List<String>> = mapOf(
    CraftingConstants.HAMMER to listOf(
        CraftingConstants.HAMMER,
        CraftingConstants.IMCANDO_HAMMER,
        CraftingConstants.IMCANDO_HAMMER_OFFHAND,
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

fun ProtectedAccess.holdsImcandoHammer(): Boolean = IMCANDO_HAMMERS.any { holds(it) }

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
