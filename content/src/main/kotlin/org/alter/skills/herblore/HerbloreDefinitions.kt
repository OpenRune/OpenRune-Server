package org.alter.skills.herblore

import org.generated.tables.herblore.HerbloreBarbarianMixesRow
import org.generated.tables.herblore.HerbloreCleaningRow
import org.generated.tables.herblore.HerbloreCrushingRow
import org.generated.tables.herblore.HerbloreFinishedRow
import org.generated.tables.herblore.HerbloreSwampTarRow
import org.generated.tables.herblore.HerbloreUnfinishedRow


/**
 * Definitions for herblore potions.
 * Contains data structures for unfinished and finished potions loaded from cache tables.
 */
object HerbloreDefinitions {

    /**
     * Pre-computed set of all required items (unfinished potion + all secondaries)
     * Cached for performance to avoid creating new Set on every lookup
     */
    val HerbloreFinishedRow.requiredItems: Set<Int>
        get() = setOf(unfPot) + secondaries

    /**
     * Loads unfinished potion data from cache table.
     */
    val unfinishedPotions: List<HerbloreUnfinishedRow> = HerbloreUnfinishedRow.all()

    /**
     * Loads finished potion data from cache table.
     */
    val finishedPotions: List<HerbloreFinishedRow> = HerbloreFinishedRow.all()

    /**
     * Set of all herb item IDs for fast lookup (O(1) instead of O(n))
     */
    val herbItemIds: Set<Int> = unfinishedPotions.mapTo(mutableSetOf()) { it.herbItem }

    /**
     * Reverse lookup map: item ID -> list of potions that use this item
     * This allows O(1) lookup instead of O(n) filtering
     */
    val itemToPotions: Map<Int, List<HerbloreFinishedRow>> = run {
        val map = mutableMapOf<Int, MutableList<HerbloreFinishedRow>>()
        finishedPotions.forEach { potion ->
            // Add unfinished potion -> potion mapping
            map.getOrPut(potion.unfPot) { mutableListOf() }.add(potion)
            // Add each secondary -> potion mapping
            potion.secondaries.forEach { secondary ->
                map.getOrPut(secondary) { mutableListOf() }.add(potion)
            }
        }
        map
    }


    val crushingRecipes: List<HerbloreCrushingRow> = HerbloreCrushingRow.all()

    /**
     * Loads cleaning herb data from cache table.
     */
    val cleaningHerbs: List<HerbloreCleaningRow> = HerbloreCleaningRow.all()

    /**
     * Loads barbarian mix data from cache table.
     */
    val barbarianMixes: List<HerbloreBarbarianMixesRow> = HerbloreBarbarianMixesRow.all()

    /**
     * Map for quick lookup of barbarian mixes by two-dose potion and ingredient
     */
    val mixLookup: Map<Pair<Int, Int>, HerbloreBarbarianMixesRow> = barbarianMixes.associateBy { mix ->
        Pair(mix.twoDosePotion, mix.mixIngredient)
    }

    /**
     * Finds all potion recipes that could match the given two items.
     * Returns candidates that need to be checked for all required ingredients.
     * Uses reverse lookup map for O(1) lookup instead of O(n) filtering.
     *
     * Note: ANY of the required items (unfinished potion or any secondary)
     * can be used to trigger the interaction. For example, with Super Combat Potion, you can
     * use super_strength_3 (secondary) on torstol_potion_unf (unfinished) and it will still work
     * as long as you have all required ingredients.
     */
    fun findPotionCandidates(item1: Int, item2: Int): List<HerbloreFinishedRow> {
        val potions1 = itemToPotions[item1] ?: emptyList()
        val potions2 = itemToPotions[item2] ?: emptyList()
        return (potions1 + potions2).distinct()
    }

    /**
     * Finds a barbarian mix recipe for the given two items.
     */
    fun findBarbarianMix(potion: Int, ingredient: Int): HerbloreBarbarianMixesRow? {
        return mixLookup[Pair(potion, ingredient)] ?: mixLookup[Pair(ingredient, potion)]
    }

    /**
     * Loads swamp tar data from cache table.
     */
    val swampTars: List<HerbloreSwampTarRow> = HerbloreSwampTarRow.all()

    /**
     * Map for quick lookup of swamp tar recipes by herb
     */
    val swampTarLookup: Map<Int, HerbloreSwampTarRow> = swampTars.associateBy { it.herb }

    /**
     * Finds a swamp tar recipe for the given herb.
     */
    fun findSwampTar(herb: Int): HerbloreSwampTarRow? {
        return swampTarLookup[herb]
    }

}