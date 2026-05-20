package org.rsmod.content.skills.herblore

import org.rsmod.api.table.herblore.HerbloreBarbarianMixesRow
import org.rsmod.api.table.herblore.HerbloreCleaningRow
import org.rsmod.api.table.herblore.HerbloreCrushingRow
import org.rsmod.api.table.herblore.HerbloreFinishedRow
import org.rsmod.api.table.herblore.HerbloreSwampTarRow
import org.rsmod.api.table.herblore.HerbloreUnfinishedRow
import org.rsmod.content.skills.Material
import org.rsmod.game.inv.Inventory

object HerbloreDefinitions {

    val HerbloreFinishedRow.skillMultiMaterials: List<Material> get() = buildList {
        add(Material(unfPot.internalName, 1))
        if (secondaries.size == 1) {
            add(Material(secondaries.first().internalName, secondariesAmount?: 1))
        } else {
            secondaries.forEach { add(Material(it.internalName, 1)) }
        }
    }

    fun HerbloreFinishedRow.hasRequiredMaterials(inventory: Inventory): Boolean {
        if (inventory.count(unfPot.internalName) < 1) {
            return false
        }
        if (secondaries.size == 1) {
            val needed = secondariesAmount?: 1
            return inventory.count(secondaries.first().internalName) >= needed
        }
        return secondaries.all { inventory.count(it.internalName) >= 1 }
    }

    fun HerbloreFinishedRow.maxProducible(inventory: Inventory): Int {
        val counts = buildList {
            add(inventory.count(unfPot.internalName))
            if (secondaries.size == 1) {
                val needed = secondariesAmount?: 1
                add(inventory.count(secondaries.first().internalName) / needed)
            } else {
                secondaries.forEach { add(inventory.count(it.internalName)) }
            }
        }
        return counts.minOrNull() ?: 0
    }

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
