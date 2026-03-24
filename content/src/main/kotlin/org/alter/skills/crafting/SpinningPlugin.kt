package org.alter.skills.crafting

import io.github.oshai.kotlinlogging.KotlinLogging
import org.alter.api.Skills
import org.alter.api.ext.message
import org.alter.api.ext.produceItemBox
import org.alter.game.model.entity.Player
import org.alter.game.model.item.Item
import org.alter.game.model.queue.QueueTask
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.ItemOnObject
import org.generated.tables.crafting.CraftingSpinningRow

/**
 * Spinning skill plugin.
 *
 * Handles spinning items on a spinning wheel. Players use a spinnable item
 * on a spinning wheel object (any object with a "Spin" action) to open
 * a quantity selection interface, then repeatedly spin items.
 */
class SpinningPlugin : PluginEvent() {

    companion object {
        private val logger = KotlinLogging.logger {}

        /** All spinning recipes keyed by input item ID. */
        lateinit var recipes: Map<Int, CraftingSpinningRow>
            private set

        /** Spinning wheel animation. */
        private const val SPIN_ANIMATION = "sequences.human_spinningwheel_90"

        /**
         * Returns true if the given game object is a spinning wheel
         * (has a "Spin" action in its definition).
         */
        fun isSpinningWheel(gameObject: org.alter.game.model.entity.GameObject): Boolean {
            val def = gameObject.getDef()
            return def.actions.any { it?.equals("Spin", ignoreCase = true) == true }
        }
    }

    override fun init() {
        val allRecipes = CraftingSpinningRow.all()
        recipes = allRecipes.associateBy { it.inputItem }

        logger.info { "Loaded ${recipes.size} spinning recipes." }

        on<ItemOnObject> {
            where {
                recipes.containsKey(item.id) && isSpinningWheel(gameObject)
            }
            then {
                val recipe = recipes[item.id] ?: return@then

                player.queue {
                    produceItemBox(
                        player,
                        recipe.outputItem,
                        title = "How many would you like to spin?",
                        maxProducable = player.inventory.getItemCount(recipe.inputItem),
                    ) { _, amount ->
                        player.queue { spinLoop(player, recipe, amount) }
                    }
                }
            }
        }
    }

    /**
     * Main spinning loop. Repeats until the player runs out of input items
     * or the requested amount is reached.
     */
    private suspend fun QueueTask.spinLoop(
        player: Player,
        recipe: CraftingSpinningRow,
        amount: Int,
    ) {
        val craftingLevel = player.getSkills().getCurrentLevel(Skills.CRAFTING)
        if (craftingLevel < recipe.level) {
            val outputName = itemName(recipe.outputItem)
            player.message("You need a Crafting level of at least ${recipe.level} to spin $outputName.")
            return
        }

        if (!player.inventory.contains(recipe.inputItem)) {
            player.message("You don't have any ${itemName(recipe.inputItem)} to spin.")
            return
        }

        var remaining = amount

        player.message("You start spinning.")
        player.animate(SPIN_ANIMATION)

        repeatWhile(
            delay = 3,
            immediate = false,
            canRepeat = { remaining > 0 && player.inventory.contains(recipe.inputItem) }
        ) {
            player.lock()

            val currentLevel = player.getSkills().getCurrentLevel(Skills.CRAFTING)
            if (currentLevel < recipe.level) {
                player.message("You need a Crafting level of at least ${recipe.level} to spin this.")
                player.unlock()
                return@repeatWhile
            }

            player.inventory.remove(recipe.inputItem, 1)
            player.inventory.add(recipe.outputItem, 1)

            // XP values in the table are x10 (e.g. 25 = 2.5 XP)
            val xp = recipe.xp / 10.0
            player.addXp(Skills.CRAFTING, xp)

            val outputName = itemName(recipe.outputItem)
            player.message("You spin the ${itemName(recipe.inputItem)} into $outputName.")

            player.animate(SPIN_ANIMATION)
            remaining--
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
            "item"
        }
    }
}
