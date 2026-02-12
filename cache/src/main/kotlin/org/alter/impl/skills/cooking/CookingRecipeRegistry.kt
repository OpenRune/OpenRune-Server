package org.alter.impl.skills.cooking

import org.alter.impl.skills.cooking.recipes.BakedGoodsRecipes
import org.alter.impl.skills.cooking.recipes.BreadRecipes
import org.alter.impl.skills.cooking.recipes.FishRecipes
import org.alter.impl.skills.cooking.recipes.MeatRecipes
import org.alter.impl.skills.cooking.recipes.MiscFoodRecipes
import org.alter.impl.skills.cooking.recipes.PieRecipes
import org.alter.impl.skills.cooking.recipes.PizzaRecipes
import org.alter.impl.skills.cooking.recipes.PotatoRecipes
import org.alter.impl.skills.cooking.recipes.StewRecipes
import org.alter.impl.skills.cooking.recipes.WineRecipes

/**
 * Central registry of all cooking recipes.
 *
 * Add new recipe categories here as they are implemented.
 */
object CookingRecipeRegistry {

    /**
     * All cooking action definitions from all recipe categories.
     */
    val allRecipes: List<ActionDef> by lazy {
        FishRecipes.recipes +
            MeatRecipes.recipes +
            BakedGoodsRecipes.recipes +
            BreadRecipes.recipes +
            PieRecipes.recipes +
            PizzaRecipes.recipes +
            StewRecipes.recipes +
            PotatoRecipes.recipes +
            MiscFoodRecipes.recipes +
            WineRecipes.recipes
    }
}
