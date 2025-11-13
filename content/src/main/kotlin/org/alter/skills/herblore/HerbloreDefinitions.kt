package org.alter.skills.herblore

import org.alter.game.util.DbHelper
import org.alter.game.util.DbHelper.Companion.table
import org.alter.game.util.column
import org.alter.game.util.columnOptional
import org.alter.game.util.multiColumnOptional
import org.alter.game.util.vars.IntType
import org.alter.game.util.vars.ObjType

/**
 * Definitions for herblore potions.
 * Contains data structures for unfinished and finished potions loaded from cache tables.
 */
object HerbloreDefinitions {

    /**
     * Data for creating unfinished potions (herb + vial of water)
     */
    data class UnfinishedPotionData(
        val herbItem: Int,
        val level: Int,
        val xp: Int,
        val unfinishedPotion: Int
    )

    /**
     * Data for creating finished potions (unfinished potion + secondary ingredients)
     */
    data class FinishedPotionData(
        val unfinishedPotion: Int,
        val secondaries: List<Int>,
        val level: Int,
        val xp: Int,
        val finishedPotion: Int?
    ) {
        /**
         * Pre-computed set of all required items (unfinished potion + all secondaries)
         * Cached for performance to avoid creating new Set on every lookup
         */
        val requiredItems: Set<Int> = setOf(unfinishedPotion) + secondaries
    }

    /**
     * Data for cleaning grimy herbs (unidentified -> clean)
     */
    data class CleaningHerbData(
        val grimyHerb: Int,
        val level: Int,
        val xp: Int,
        val cleanHerb: Int
    )

    /**
     * Data for creating barbarian mixes (two-dose potion + roe/caviar)
     */
    data class BarbarianMixData(
        val twoDosePotion: Int,
        val mixIngredient: Int,
        val level: Int,
        val xp: Int,
        val barbarianMix: Int
    )

    /**
     * Loads unfinished potion data from cache table.
     */
    val unfinishedPotions: List<UnfinishedPotionData> = table("tables.herblore_unfinished").map { row ->
        val herbItem = row.column("columns.herblore_unfinished:herb_item", ObjType)
        val level = row.column("columns.herblore_unfinished:level", IntType)
        val xp = row.column("columns.herblore_unfinished:xp", IntType)
        val unfinishedPotion = row.column("columns.herblore_unfinished:unfinished_potion", ObjType)

        UnfinishedPotionData(herbItem, level, xp, unfinishedPotion)
    }

    /**
     * Loads finished potion data from cache table.
     */
    val finishedPotions: List<FinishedPotionData> = table("tables.herblore_finished").map { row ->
        val unfinishedPotion = row.column("columns.herblore_finished:pot_primary", ObjType)
        // Always use multiColumnOptional to get ALL secondaries (works for both single and multiple values)
        val secondaries = row.multiColumnOptional("columns.herblore_finished:secondaries", ObjType).filterNotNull()
        val level = row.column("columns.herblore_finished:level_required", IntType)
        val xp = row.column("columns.herblore_finished:xp", IntType)
        val finishedPotion = row.columnOptional("columns.herblore_finished:finished_potion", ObjType)

        FinishedPotionData(
            unfinishedPotion,
            secondaries,
            level,
            xp,
            finishedPotion
        )
    }

    /**
     * Set of all herb item IDs for fast lookup (O(1) instead of O(n))
     */
    val herbItemIds: Set<Int> = unfinishedPotions.mapTo(mutableSetOf()) { it.herbItem }

    /**
     * Reverse lookup map: item ID -> list of potions that use this item
     * This allows O(1) lookup instead of O(n) filtering
     */
    val itemToPotions: Map<Int, List<FinishedPotionData>> = run {
        val map = mutableMapOf<Int, MutableList<FinishedPotionData>>()
        finishedPotions.forEach { potion ->
            // Add unfinished potion -> potion mapping
            map.getOrPut(potion.unfinishedPotion) { mutableListOf() }.add(potion)
            // Add each secondary -> potion mapping
            potion.secondaries.forEach { secondary ->
                map.getOrPut(secondary) { mutableListOf() }.add(potion)
            }
        }
        map
    }

    /**
     * Loads cleaning herb data from cache table.
     */
    val cleaningHerbs: List<CleaningHerbData> = table("tables.herblore_cleaning").map { row ->
        val grimyHerb = row.column("columns.herblore_cleaning:grimy_herb", ObjType)
        val level = row.column("columns.herblore_cleaning:level", IntType)
        val xp = row.column("columns.herblore_cleaning:xp", IntType)
        val cleanHerb = row.column("columns.herblore_cleaning:clean_herb", ObjType)

        CleaningHerbData(grimyHerb, level, xp, cleanHerb)
    }

    /**
     * Loads barbarian mix data from cache table.
     */
    val barbarianMixes: List<BarbarianMixData> = table("tables.herblore_barbarian_mixes").map { row ->
        val twoDosePotion = row.column("columns.herblore_barbarian_mixes:two_dose_potion", ObjType)
        val mixIngredient = row.column("columns.herblore_barbarian_mixes:mix_ingredient", ObjType)
        val level = row.column("columns.herblore_barbarian_mixes:level", IntType)
        val xp = row.column("columns.herblore_barbarian_mixes:xp", IntType)
        val barbarianMix = row.column("columns.herblore_barbarian_mixes:barbarian_mix", ObjType)

        BarbarianMixData(twoDosePotion, mixIngredient, level, xp, barbarianMix)
    }

    /**
     * Map for quick lookup of barbarian mixes by two-dose potion and ingredient
     */
    val mixLookup: Map<Pair<Int, Int>, BarbarianMixData> = barbarianMixes.associateBy { mix ->
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
    fun findPotionCandidates(item1: Int, item2: Int): List<FinishedPotionData> {
        val potions1 = itemToPotions[item1] ?: emptyList()
        val potions2 = itemToPotions[item2] ?: emptyList()
        return (potions1 + potions2).distinct()
    }

    /**
     * Finds a barbarian mix recipe for the given two items.
     */
    fun findBarbarianMix(potion: Int, ingredient: Int): BarbarianMixData? {
        return mixLookup[Pair(potion, ingredient)] ?: mixLookup[Pair(ingredient, potion)]
    }

    /**
     * Data for creating swamp tar (herb + 15x swamp tar = 15x finished tar)
     */
    data class SwampTarData(
        val herb: Int,
        val level: Int,
        val xp: Int,
        val finishedTar: Int
    )

    /**
     * Loads swamp tar data from cache table.
     */
    val swampTars: List<SwampTarData> = table("tables.herblore_swamp_tar").map { row ->
        val herb = row.column("columns.herblore_swamp_tar:herb", ObjType)
        val level = row.column("columns.herblore_swamp_tar:level", IntType)
        val xp = row.column("columns.herblore_swamp_tar:xp", IntType)
        val finishedTar = row.column("columns.herblore_swamp_tar:finished_tar", ObjType)

        SwampTarData(herb, level, xp, finishedTar)
    }

    /**
     * Map for quick lookup of swamp tar recipes by herb
     */
    val swampTarLookup: Map<Int, SwampTarData> = swampTars.associateBy { it.herb }

    /**
     * Finds a swamp tar recipe for the given herb.
     */
    fun findSwampTar(herb: Int): SwampTarData? {
        return swampTarLookup[herb]
    }

    /**
     * Data for crushing items with pestle and mortar
     */
    data class CrushingData(
        val item: Int,
        val level: Int,
        val xp: Int,
        val crushedItem: Int
    )

    /**
     * Loads crushing data from cache table.
     */
    val crushingRecipes: List<CrushingData> = table("tables.herblore_crushing").map { row ->
        val item = row.column("columns.herblore_crushing:item", ObjType)
        val level = row.column("columns.herblore_crushing:level", IntType)
        val xp = row.column("columns.herblore_crushing:xp", IntType)
        val crushedItem = row.column("columns.herblore_crushing:crushed_item", ObjType)

        CrushingData(item, level, xp, crushedItem)
    }

    /**
     * Map for quick lookup of crushing recipes by item
     */
    val crushingLookup: Map<Int, CrushingData> = crushingRecipes.associateBy { it.item }
}

