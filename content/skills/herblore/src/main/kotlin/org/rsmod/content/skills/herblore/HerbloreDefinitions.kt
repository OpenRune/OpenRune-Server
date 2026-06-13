package org.rsmod.content.skills.herblore

import org.rsmod.api.table.herblore.HerbloreBarbarianMixesRow
import org.rsmod.api.table.herblore.HerbloreCleaningRow
import org.rsmod.api.table.herblore.HerbloreCrushingRow
import org.rsmod.api.table.herblore.HerbloreFinishedRow
import org.rsmod.api.table.herblore.HerbloreSwampTarRow
import org.rsmod.api.table.herblore.HerbloreUnfinishedRow
import org.rsmod.content.skills.Material
import org.rsmod.game.inv.Inventory

private fun amountsFor(count: Int, amounts: List<Int>?): List<Int> =
    amounts?.take(count)?.let { list ->
        if (list.size >= count) {
            list
        } else {
            List(count) { index -> list.getOrNull(index) ?: 1 }
        }
    } ?: List(count) { 1 }

private fun HerbloreFinishedRow.resolvedInputAmounts(): List<Int> = amountsFor(input.size, inputAmount)

val HerbloreUnfinishedRow.herbItem get() = input
val HerbloreUnfinishedRow.unfinishedPotion get() = output
val HerbloreUnfinishedRow.level get() = statReq.primaryLevel()

val HerbloreFinishedRow.unfPot get() = input.first()
val HerbloreFinishedRow.secondaries get() = input.drop(1)
val HerbloreFinishedRow.secondariesAmount: Int?
    get() = if (secondaries.size == 1) resolvedInputAmounts().drop(1).firstOrNull() ?: 1 else null
val HerbloreFinishedRow.outputPotion get() = output
val HerbloreFinishedRow.levelRequired get() = statReq.primaryLevel()

val HerbloreBarbarianMixesRow.twoDosePotion get() = input.first()
val HerbloreBarbarianMixesRow.mixIngredient get() = input[1]
val HerbloreBarbarianMixesRow.barbarianMix get() = output
val HerbloreBarbarianMixesRow.level get() = statReq.primaryLevel()

val HerbloreSwampTarRow.herb get() = input
val HerbloreSwampTarRow.finishedTar get() = output
val HerbloreSwampTarRow.level get() = statReq.primaryLevel()

val HerbloreCrushingRow.item get() = input
val HerbloreCrushingRow.crushedItem get() = output
val HerbloreCrushingRow.level get() = statReq.primaryLevel()

val HerbloreFinishedRow.skillMultiMaterials: List<Material>
    get() = buildList {
        val amounts = resolvedInputAmounts()
        add(Material(unfPot.internalName, amounts.firstOrNull() ?: 1))
        secondaries.forEachIndexed { index, secondary ->
            add(Material(secondary.internalName, amounts.getOrNull(index + 1) ?: 1))
        }
    }

fun HerbloreFinishedRow.hasRequiredMaterials(inventory: Inventory): Boolean {
    val amounts = resolvedInputAmounts()
    input.forEachIndexed { index, item ->
        val needed = amounts.getOrNull(index) ?: 1
        if (inventory.count(item.internalName) < needed) {
            return false
        }
    }
    return true
}

fun HerbloreFinishedRow.maxProducible(inventory: Inventory): Int {
    val amounts = resolvedInputAmounts()
    return input.indices.minOf { index ->
        val needed = amounts.getOrNull(index) ?: 1
        inventory.count(input[index].internalName) / needed
    }
}

object HerbloreDefinitions {

    val unfinishedPotions: List<HerbloreUnfinishedRow> = HerbloreUnfinishedRow.all()

    val finishedPotions: List<HerbloreFinishedRow> = HerbloreFinishedRow.all()

    val herbItemNames: Set<String> = unfinishedPotions.mapTo(mutableSetOf()) { it.herbItem.internalName }

    val itemToPotions: Map<String, List<HerbloreFinishedRow>> = run {
        val map = mutableMapOf<String, MutableList<HerbloreFinishedRow>>()
        finishedPotions.forEach { potion ->
            map.getOrPut(potion.unfPot.internalName) { mutableListOf() }.add(potion)
            potion.secondaries.forEach { secondary ->
                map.getOrPut(secondary.internalName) { mutableListOf() }.add(potion)
            }
        }
        map
    }

    val crushingRecipes: List<HerbloreCrushingRow> = HerbloreCrushingRow.all()

    val cleaningHerbs: List<HerbloreCleaningRow> = HerbloreCleaningRow.all()

    val barbarianMixes: List<HerbloreBarbarianMixesRow> = HerbloreBarbarianMixesRow.all()

    val swampTars: List<HerbloreSwampTarRow> = HerbloreSwampTarRow.all()

    fun findPotionCandidates(item1: String, item2: String): List<HerbloreFinishedRow> {
        val potions1 = itemToPotions[item1] ?: emptyList()
        val potions2 = itemToPotions[item2] ?: emptyList()
        return (potions1 + potions2).distinct()
    }
}
