package org.alter.skills.cooking

import org.alter.game.util.DbHelper.Companion.table
import org.alter.game.util.column
import org.alter.game.util.columnOptional
import org.alter.game.util.vars.IntType
import org.alter.game.util.vars.ObjType
import org.alter.rscm.RSCM.asRSCM

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

    val recipes: List<CookingRecipe> = table("tables.cooking_recipes").map { recipeTable ->
        val rawItem = recipeTable.column("columns.cooking_recipes:raw_item", ObjType)
        val cookedItem = recipeTable.column("columns.cooking_recipes:cooked_item", ObjType)
        val burntItem = recipeTable.columnOptional("columns.cooking_recipes:burnt_item", ObjType)
        val level = recipeTable.column("columns.cooking_recipes:level", IntType)
        val xp = recipeTable.column("columns.cooking_recipes:xp", IntType).toDouble()
        val heal = recipeTable.column("columns.cooking_recipes:heal", IntType)
        val burnChance = recipeTable.columnOptional("columns.cooking_recipes:burn_chance", IntType) ?: 50

        CookingRecipe(
            rawItem = rawItem,
            cookedItem = cookedItem,
            burntItem = burntItem,
            level = level,
            xp = xp,
            heal = heal,
            burnChance = burnChance
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
