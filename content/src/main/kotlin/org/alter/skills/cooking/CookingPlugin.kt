package org.alter.skills.cooking

import dev.openrune.ServerCacheManager.getItem
import io.github.oshai.kotlinlogging.KotlinLogging
import org.alter.api.Skills
import org.alter.api.ext.message
import org.alter.game.model.entity.DynamicObject
import org.alter.game.model.entity.GameObject
import org.alter.game.model.entity.Player
import org.alter.game.model.item.Item
import org.alter.game.model.queue.QueueTask
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.ItemOnObject
import org.alter.rscm.RSCM.asRSCM
import org.generated.tables.cooking.CookingRecipesRow

/**
 * Core Cooking skill plugin.
 *
 * Handles cooking raw food on fires and ranges. Fires are DynamicObjects
 * spawned by firemaking (type 10). Ranges are permanent objects whose
 * definition contains a "Cook" action.
 *
 * Method values from the cooking_recipes table:
 *  0 = fire or range
 *  1 = range only
 */
class CookingPlugin : PluginEvent() {

    companion object {
        private val logger = KotlinLogging.logger {}

        /** All recipes keyed by raw item ID. */
        lateinit var recipes: Map<Int, CookingRecipesRow>
            private set

        /** RSCM IDs for fire objects produced by firemaking. */
        private val FIRE_OBJECTS: Set<String> = setOf(
            "objects.fire",
            "objects.forestry_fire",
        )

        /** Cooking animation for ranges. */
        private const val RANGE_ANIMATION = "sequences.human_cooking"
        /** Cooking animation for fires. */
        private const val FIRE_ANIMATION = "sequences.human_firecooking"

        /**
         * Returns true if the given game object is a fire (DynamicObject with type 10).
         */
        fun isFire(gameObject: GameObject): Boolean {
            if (gameObject !is DynamicObject) return false
            return gameObject.type == 10
        }

        /**
         * Returns true if the given game object is a range or cooking surface
         * (has a "Cook" action in its definition).
         */
        fun isRange(gameObject: GameObject): Boolean {
            val def = gameObject.getDef()
            return def.actions.any { it?.equals("Cook", ignoreCase = true) == true }
        }

        /**
         * Returns true if the game object is a valid cooking surface (fire or range).
         */
        fun isCookingSurface(gameObject: GameObject): Boolean {
            return isFire(gameObject) || isRange(gameObject)
        }
    }

    override fun init() {
        val allRecipes = CookingRecipesRow.all()
        recipes = allRecipes.associateBy { it.rawItem }

        logger.info { "Loaded ${recipes.size} cooking recipes." }

        on<ItemOnObject> {
            where {
                recipes.containsKey(item.id) && isCookingSurface(gameObject)
            }
            then {
                val recipe = recipes[item.id] ?: return@then
                val onFire = isFire(gameObject)

                // Method 1 = range only
                if (recipe.method == 1 && onFire) {
                    player.message("You can't cook that on a fire.")
                    return@then
                }

                player.queue { cookLoop(player, recipe, onFire) }
            }
        }
    }

    /**
     * Main cooking loop. Repeats until the player runs out of raw items
     * or the cooking surface disappears.
     */
    private suspend fun QueueTask.cookLoop(
        player: Player,
        recipe: CookingRecipesRow,
        isFire: Boolean,
    ) {
        val cookingLevel = player.getSkills().getCurrentLevel(Skills.COOKING)
        if (cookingLevel < recipe.level) {
            val rawName = itemName(recipe.rawItem)
            player.message("You need a Cooking level of at least ${recipe.level} to cook $rawName.")
            return
        }

        val anim = if (isFire) FIRE_ANIMATION else RANGE_ANIMATION
        player.message("You cook the ${itemName(recipe.rawItem)}.")
        player.animate(anim)

        repeatWhile(
            delay = 4,
            immediate = false,
            canRepeat = { player.inventory.contains(recipe.rawItem) }
        ) {
            player.lock()

            // Re-check level (boosts can expire)
            val currentLevel = player.getSkills().getCurrentLevel(Skills.COOKING)
            if (currentLevel < recipe.level) {
                player.message("You need a Cooking level of at least ${recipe.level} to cook this.")
                player.unlock()
                return@repeatWhile
            }

            val burned = CookingBurnRates.shouldBurn(
                player = player,
                rawItem = recipe.rawItem,
                burnStopFire = recipe.burnStopFire,
                burnStopRange = recipe.burnStopRange,
                isFire = isFire,
            )

            player.inventory.remove(recipe.rawItem, 1)

            if (burned) {
                player.inventory.add(recipe.burntItem, 1)
                player.message("You accidentally burn the ${itemName(recipe.rawItem)}.")
            } else {
                player.inventory.add(recipe.cookedItem, 1)
                player.addXp(Skills.COOKING, recipe.xp)
                player.message("You successfully cook the ${itemName(recipe.rawItem)}.")

                FoodCookedEvent(
                    player = player,
                    rawItem = recipe.rawItem,
                    cookedItem = recipe.cookedItem,
                    isFire = isFire,
                ).post()
            }

            player.animate(anim)
            player.unlock()
        }
    }

    /**
     * Returns a display-friendly item name, lowercased.
     */
    private fun itemName(itemId: Int): String {
        return try {
            Item(itemId).getName().lowercase()
        } catch (_: Exception) {
            "food"
        }
    }
}
