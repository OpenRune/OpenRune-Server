package org.alter.skills.cooking

import org.generated.tables.cooking.CookingRecipesRow

/**
 * Base data class for cooking recipes
 */
data class CookingRecipe(
    val rawItem: Int,
    val cookedItem: Int,
    val burntItem: Int?,
    val level: Int,
    val xp: Double,
    val heal: Int,
    val burnChance: Int = 0, // Base burn chance (0-100), decreases with level
)

/**
 * Cooking definitions loaded from database
 */
object CookingDefinitions {

    val recipes: List<CookingRecipe> = CookingRecipesRow.all().map { row ->
        CookingRecipe(
            rawItem = row.rawItem,
            cookedItem = row.cookedItem,
            burntItem = row.burntItem,
            level = row.level,
            xp = row.xp.toDouble(),
            heal = row.heal,
            burnChance = row.burnChance ?: 50
        )
    }

    /**
     * Get recipe by raw item ID
     */
    fun getRecipe(rawItemId: Int): CookingRecipe? {
        return recipes.firstOrNull { it.rawItem == rawItemId }
    }

    /**
     * Calculate burn chance based on cooking level and base burn chance
     * Formula: burnChance decreases by 1% per level above requirement, minimum 0%
     */
    fun calculateBurnChance(recipe: CookingRecipe, cookingLevel: Int, isRange: Boolean): Int {
        val baseChance = recipe.burnChance
        val levelDiff = cookingLevel - recipe.level

        // Ranges reduce burn chance by 10%
        val rangeBonus = if (isRange) 10 else 0

        // Burn chance decreases by 1% per level above requirement
        val levelReduction = levelDiff.coerceAtLeast(0)

        // Calculate final burn chance
        val finalChance = (baseChance - levelReduction - rangeBonus).coerceIn(0, 100)

        return finalChance
    }
}
