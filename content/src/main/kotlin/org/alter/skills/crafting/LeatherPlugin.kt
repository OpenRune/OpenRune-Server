package org.alter.skills.crafting

import dev.openrune.ServerCacheManager.getItem
import io.github.oshai.kotlinlogging.KotlinLogging
import org.alter.api.Skills
import org.alter.api.ext.message
import org.alter.api.ext.produceItemBox
import org.alter.game.model.entity.Player
import org.alter.game.model.item.Item
import org.alter.game.model.queue.QueueTask
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.ItemOnItemEvent
import org.alter.game.pluginnew.event.impl.SatisfyType
import org.alter.game.pluginnew.event.impl.onItemOnItem
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.asRSCM
import org.generated.tables.crafting.CraftingLeatherRow

/**
 * Leather crafting plugin.
 *
 * Handles crafting leather items using a needle on leather (or hard leather).
 * A needle and thread are required. Thread is consumed based on the recipe's
 * threadCost value.
 */
class LeatherPlugin : PluginEvent() {

    companion object {
        private val logger = KotlinLogging.logger {}

        /** All leather recipes. */
        lateinit var allRecipes: List<CraftingLeatherRow>
            private set

        /** Recipes grouped by leather type. */
        lateinit var recipesByLeather: Map<Int, List<CraftingLeatherRow>>
            private set

        private val NEEDLE_ID = "items.needle".asRSCM()
        private val THREAD_ID = "items.thread".asRSCM()

        /** Leather crafting animation. */
        private const val CRAFT_ANIMATION = "sequences.human_leather_crafting"
    }

    override fun init() {
        allRecipes = CraftingLeatherRow.all()
        recipesByLeather = allRecipes.groupBy { it.leatherType }

        logger.info { "Loaded ${allRecipes.size} leather crafting recipes." }

        val leatherTypes = recipesByLeather.keys.toList()

        leatherTypes.forEach { leatherId ->
            onItemOnItem(NEEDLE_ID, leatherId).type(SatisfyType.ANY) {
                val recipes = recipesByLeather[leatherId] ?: return@type
                val craftingLevel = player.getSkills().getCurrentLevel(Skills.CRAFTING)

                val available = recipes.filter { craftingLevel >= it.level }
                if (available.isEmpty()) {
                    player.message("You need a higher Crafting level to make anything from this leather.")
                    return@type
                }

                player.queue {
                    val items = available.map { it.outputItem }.toIntArray()
                    produceItemBox(
                        player,
                        *items,
                        title = "What would you like to make?",
                        maxProducable = player.inventory.getItemCount(leatherId)
                    ) { selectedItem: Int, amount: Int ->
                        val recipe = available.firstOrNull { it.outputItem == selectedItem }
                            ?: return@produceItemBox
                        player.queue { craftLoop(player, recipe, amount) }
                    }
                }
            }
        }
    }

    /**
     * Main leather crafting loop.
     */
    private suspend fun QueueTask.craftLoop(
        player: Player,
        recipe: CraftingLeatherRow,
        amount: Int,
    ) {
        val craftingLevel = player.getSkills().getCurrentLevel(Skills.CRAFTING)
        if (craftingLevel < recipe.level) {
            val outputName = itemName(recipe.outputItem)
            player.message("You need a Crafting level of at least ${recipe.level} to make $outputName.")
            return
        }

        if (!player.inventory.contains(recipe.leatherType)) {
            player.message("You don't have any leather to craft with.")
            return
        }

        if (!player.inventory.contains(NEEDLE_ID)) {
            player.message("You need a needle to craft leather.")
            return
        }

        if (!player.inventory.contains(THREAD_ID)) {
            player.message("You need thread to craft leather.")
            return
        }

        var remaining = amount

        player.message("You start crafting.")
        player.animate(CRAFT_ANIMATION)

        repeatWhile(
            delay = 4,
            immediate = false,
            canRepeat = {
                remaining > 0 &&
                    player.inventory.contains(recipe.leatherType) &&
                    player.inventory.contains(NEEDLE_ID) &&
                    player.inventory.contains(THREAD_ID)
            }
        ) {
            player.lock()

            val currentLevel = player.getSkills().getCurrentLevel(Skills.CRAFTING)
            if (currentLevel < recipe.level) {
                player.message("You need a Crafting level of at least ${recipe.level} to make this.")
                player.unlock()
                return@repeatWhile
            }

            // Consume leather
            val leatherCount = recipe.amountNeeded
            if (player.inventory.getItemCount(recipe.leatherType) < leatherCount) {
                player.message("You don't have enough leather.")
                player.unlock()
                return@repeatWhile
            }

            player.inventory.remove(recipe.leatherType, leatherCount, assureFullRemoval = true)

            // Consume thread
            player.inventory.remove(THREAD_ID, recipe.threadCost, assureFullRemoval = true)

            // Produce item
            player.inventory.add(recipe.outputItem, 1)

            // XP values in the table are x10
            val xp = recipe.xp / 10.0
            player.addXp(Skills.CRAFTING, xp)

            val outputName = itemName(recipe.outputItem)
            player.message("You make $outputName.")

            player.animate(CRAFT_ANIMATION)
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
