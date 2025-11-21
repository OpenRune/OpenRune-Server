package org.alter.skills.cooking

import org.alter.api.Skills
import org.alter.api.ext.filterableMessage
import org.alter.api.ext.produceItemBox
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.ItemOnObject
import org.alter.game.util.DbHelper
import org.alter.game.util.multiColumn
import org.alter.game.util.vars.ObjType
import org.alter.skills.cooking.CookingDefinitions.calculateBurnChance
import org.alter.skills.cooking.CookingDefinitions.recipes
import org.alter.skills.firemaking.ColoredLogs

/**
 * Cooking plugin for all cookable items (meat, fish, etc.)
 * Handles cooking raw items on fires and ranges
 */
class CookablesPlugin : PluginEvent() {

    companion object {
        // Reuse fire definitions from firemaking
        private val VALID_FIRES = ColoredLogs.COLOURED_LOGS.values.map { it.second } + "objects.fire"

        private val VALID_RANGES = listOf(
            "objects.range",
            "objects.cooking_range",
            "objects.stove",
            "objects.fireplace",
        )

        private val ALL_COOKING_OBJECTS = VALID_FIRES + VALID_RANGES
        private val VALID_RANGES_NAMES = VALID_RANGES.toSet()
    }

    override fun init() {
        // Register cooking for all recipes from database
        recipes.forEach { recipe ->
            on<ItemOnObject> {
                where {
                    item.id == recipe.rawItem &&
                    ALL_COOKING_OBJECTS.contains(gameObject.id)
                }
                then {
                    player.queue {
                        cookItem(player, recipe, gameObject.id in VALID_RANGES_NAMES)
                    }
                }
            }
        }
    }

    private suspend fun QueueTask.cookItem(player: Player, recipe: CookingRecipe, isRange: Boolean) {
        val cookingLevel = player.getSkills().getCurrentLevel(Skills.COOKING)

        // Check level requirement
        if (cookingLevel < recipe.level) {
            player.filterableMessage("You need a Cooking level of ${recipe.level} to cook this.")
            return
        }

        val rawItemId = recipe.rawItem

        // Check if player has the raw item
        if (!player.inventory.contains(rawItemId)) {
            return
        }

        // Get count of raw items
        val rawItemCount = player.inventory.getItemCount(rawItemId)

        if (rawItemCount == 1) {
            // Single item - cook immediately
            cookMultipleItems(player, recipe, isRange, cookingLevel, 1)
        } else {
            // Multiple items - show produce box
            produceItemBox(
                player,
                rawItemId,
                maxProducable = rawItemCount
            ) { _, amount ->
                player.queue {
                    cookMultipleItems(player, recipe, isRange, cookingLevel, amount)
                }
            }
        }
    }

    private suspend fun QueueTask.cookMultipleItems(
        player: Player,
        recipe: CookingRecipe,
        isRange: Boolean,
        cookingLevel: Int,
        amount: Int
    ) {
        var cooked = 0
        var burnt = 0

        repeatWhile(
            delay = 2,
            immediate = true,
            canRepeat = {
                player.inventory.contains(recipe.rawItem) && cooked + burnt < amount
            }
        ) {
            val rawItemId = recipe.rawItem

            // Check if we still have the item
            if (!player.inventory.contains(rawItemId)) {
                stop()
                return@repeatWhile
            }

            // Animate
            player.animate("sequences.cooking")

            // Calculate burn chance
            val burnChance = calculateBurnChance(recipe, cookingLevel, isRange)
            val didBurn = player.world.random(100) < burnChance

            // Remove raw item (use -1 to find any slot)
            val removeResult = player.inventory.remove(rawItemId, beginSlot = -1)
            if (!removeResult.hasSucceeded()) {
                stop()
                return@repeatWhile
            }

            // Add result item (use -1 to find any free slot)
            if (didBurn && recipe.burntItem != null) {
                val burntItemId = recipe.burntItem
                if (player.inventory.add(burntItemId, beginSlot = -1).hasSucceeded()) {
                    burnt++
                    player.filterableMessage("You accidentally burn the ${getItemName(rawItemId)}.")
                }
            } else {
                val cookedItemId = recipe.cookedItem
                if (player.inventory.add(cookedItemId, beginSlot = -1).hasSucceeded()) {
                    cooked++
                    player.addXp(Skills.COOKING, recipe.xp)

                    // Ensure consumable definition exists
                    ensureConsumableDefinition(recipe)
                }
            }
        }

        // Summary message
        if (cooked > 0 && burnt > 0) {
            player.filterableMessage("You cook $cooked ${if (cooked == 1) "item" else "items"} successfully. However, you burn $burnt ${if (burnt == 1) "item" else "items"}.")
        } else if (cooked > 0) {
            player.filterableMessage("You successfully cook $cooked ${if (cooked == 1) "item" else "items"}.")
        } else if (burnt > 0) {
            player.filterableMessage("You accidentally burn all ${if (burnt == 1) "item" else "items"}.")
        }
    }

    private fun getItemName(itemId: Int): String {
        return dev.openrune.ServerCacheManager.getItem(itemId)?.name ?: "item"
    }

    /**
     * Ensures a consumable food definition exists for the cooked item.
     * This checks if the item is already in the consumable_food table.
     * If not, we log a warning - the item should be added to FoodTable.kt
     */
    private fun ensureConsumableDefinition(recipe: CookingRecipe) {
        val cookedItemId = recipe.cookedItem

        // Check if consumable definition exists
        val hasDefinition = DbHelper.table("tables.consumable_food").any { food ->
            val itemIds = food.multiColumn("columns.consumable_food:items", ObjType)
            itemIds.contains(cookedItemId)
        }

        if (!hasDefinition) {
            // Log that we need to add this to FoodTable.kt
            // The item will still be cookable, but won't be eatable until added to FoodTable.kt
            io.github.oshai.kotlinlogging.KotlinLogging.logger("Cooking").warn {
                "Cooked item (ID: $cookedItemId) does not have a consumable food definition. " +
                "Please add it to FoodTable.kt with heal=${recipe.heal}"
            }
        }
    }
}

