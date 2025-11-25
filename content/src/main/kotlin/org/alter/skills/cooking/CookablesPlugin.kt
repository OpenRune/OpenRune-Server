package org.alter.skills.cooking

import org.alter.api.Skills
import org.alter.api.ext.*
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.ItemOnObject
import org.alter.game.pluginnew.event.impl.onObjectOption
import org.generated.tables.ConsumableFoodRow
import org.alter.skills.cooking.CookingDefinitions.calculateBurnChance
import org.alter.skills.cooking.CookingDefinitions.recipes
import org.alter.skills.firemaking.ColoredLogs

/**
 * Cooking plugin for all cookable items (meat, fish, etc.)
 * Handles cooking raw items on fires and ranges/stoves
 */
class CookablesPlugin : PluginEvent() {

    companion object {
        // Reuse fire definitions from firemaking
        private val VALID_FIRES = ColoredLogs.COLOURED_LOGS.values.map { it.second } + "objects.fire"

        private val VALID_STOVES = listOf(
            "objects.range",
            "objects.cooking_range",
            "objects.stove",
            "objects.fireplace",
        )

        private val ALL_COOKING_OBJECTS = VALID_FIRES + VALID_STOVES
        private val VALID_STOVES_NAMES = VALID_STOVES.toSet()
    }

    override fun init() {
        // Register cooking for all recipes from database (item on object)
        recipes.forEach { recipe ->
            on<ItemOnObject> {
                where {
                    item.id == recipe.rawItem &&
                    ALL_COOKING_OBJECTS.contains(gameObject.id)
                }
                then {
                    player.queue {
                        startCooking(player, recipe, gameObject.id in VALID_STOVES_NAMES)
                    }
                }
            }
        }

        // Register "Cook" option on stoves
        VALID_STOVES.forEach { stoveId ->
            onObjectOption(stoveId, "Cook", "cook") {
                player.queue {
                    handleStoveCook(player)
                }
            }
        }
    }

    /**
     * Handles clicking "Cook" option on a stove
     * Shows a menu of all cookable items in inventory
     */
    private suspend fun QueueTask.handleStoveCook(player: Player) {
        val cookableItems = recipes.mapNotNull { recipe ->
            if (player.inventory.contains(recipe.rawItem)) {
                recipe to player.inventory.getItemCount(recipe.rawItem)
            } else {
                null
            }
        }

        if (cookableItems.isEmpty()) {
            player.filterableMessage("You don't have anything to cook.")
            return
        }

        // If only one type of cookable item, start cooking it directly
        if (cookableItems.size == 1) {
            val (recipe, _) = cookableItems.first()
            startCooking(player, recipe, isStove = true)
            return
        }

        // TODO: Show menu to select which item to cook
        // For now, just cook the first item
        val (recipe, _) = cookableItems.first()
        startCooking(player, recipe, isStove = true)
    }

    /**
     * Starts the cooking process - always shows production menu
     */
    private suspend fun QueueTask.startCooking(player: Player, recipe: CookingRecipe, isStove: Boolean) {
        val cookingLevel = player.getSkills().getBaseLevel(Skills.COOKING)

        // Check level requirement (allow exact level match)
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

        // Always show production menu
        produceItemBox(
            player,
            rawItemId,
            maxProducable = rawItemCount
        ) { _, amount ->
            player.queue {
                cookMultipleItems(player, recipe, isStove, cookingLevel, amount)
            }
        }
    }

    /**
     * Cooks multiple items in a loop with animation
     */
    private suspend fun QueueTask.cookMultipleItems(
        player: Player,
        recipe: CookingRecipe,
        isStove: Boolean,
        cookingLevel: Int,
        amount: Int
    ) {
        var cooked = 0
        var burnt = 0

        // Start looping animation based on cooking type (interruptable)
        val animation = if (isStove) {
            "sequences.human_cooking"
        } else {
            "sequences.human_firecooking"
        }
        player.loopAnim(animation, interruptable = true)

        repeatWhile(
            delay = 2,
            immediate = false,
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

            // Calculate burn chance
            val burnChance = calculateBurnChance(recipe, cookingLevel, isStove)
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

        // Stop looping but let current animation finish (don't cancel it)
        player.stopLoopAnim(allowFinish = true)

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

        // Check if consumable definition exists using generated table classes
        // Note: This requires the cache to be built to generate the table classes
        val hasDefinition = try {
            ConsumableFoodRow.all().any { food ->
                food.items.any { it != null && it == cookedItemId }
            }
        } catch (e: Exception) {
            // Generated classes might not exist yet, skip check
            true
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

