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
import org.alter.game.pluginnew.event.impl.ItemOnObject
import org.alter.rscm.RSCM.asRSCM
import org.alter.skills.smithing.SmithingData
import org.generated.tables.crafting.CraftingJewelryGoldRow

/**
 * Jewelry crafting plugin.
 *
 * Handles crafting gold jewelry at a furnace. Players use a gold bar on a
 * furnace to open the jewelry crafting interface. Each piece of jewelry
 * requires a gold bar, possibly a gem, and the appropriate mould.
 */
class JewelryPlugin : PluginEvent() {

    companion object {
        private val logger = KotlinLogging.logger {}

        /** All jewelry recipes. */
        lateinit var allRecipes: List<CraftingJewelryGoldRow>
            private set

        /** Recipes keyed by output item. */
        lateinit var recipesByOutput: Map<Int, CraftingJewelryGoldRow>
            private set

        private val GOLD_BAR_ID = "items.gold_bar".asRSCM()
        private val RING_MOULD_ID = "items.ring_mould".asRSCM()
        private val NECKLACE_MOULD_ID = "items.necklace_mould".asRSCM()
        private val AMULET_MOULD_ID = "items.amulet_mould".asRSCM()

        /** Furnace animation (same as smelting). */
        private const val FURNACE_ANIMATION = "sequences.human_furnace"

        /**
         * Determines the required mould based on the output item name.
         */
        fun requiredMould(outputItemId: Int): Int? {
            val name = try {
                Item(outputItemId).getName().lowercase()
            } catch (_: Exception) {
                return null
            }
            return when {
                name.contains("ring") -> RING_MOULD_ID
                name.contains("necklace") -> NECKLACE_MOULD_ID
                name.contains("amulet") || name.contains("ammy") -> AMULET_MOULD_ID
                else -> null
            }
        }
    }

    override fun init() {
        allRecipes = CraftingJewelryGoldRow.all()
        recipesByOutput = allRecipes.associateBy { it.output }

        logger.info { "Loaded ${allRecipes.size} gold jewelry recipes." }

        on<ItemOnObject> {
            where {
                item.id == GOLD_BAR_ID &&
                    gameObject.getDef().category == SmithingData.FURNACE_CATEGORY
            }
            then {
                val craftingLevel = player.getSkills().getCurrentLevel(Skills.CRAFTING)

                val available = allRecipes.filter { recipe ->
                    craftingLevel >= recipe.level && canMakeJewelry(player, recipe)
                }

                if (available.isEmpty()) {
                    player.message("You can't make any jewelry with what you have.")
                    return@then
                }

                player.queue {
                    val items = available.map { it.output }.toIntArray()
                    produceItemBox(
                        player,
                        *items,
                        title = "What would you like to make?",
                        maxProducable = player.inventory.getItemCount(GOLD_BAR_ID)
                    ) { selectedItem: Int, amount: Int ->
                        val recipe = recipesByOutput[selectedItem] ?: return@produceItemBox
                        player.queue { craftLoop(player, recipe, amount) }
                    }
                }
            }
        }
    }

    /**
     * Checks if a player has the materials to craft a piece of jewelry.
     */
    private fun canMakeJewelry(player: Player, recipe: CraftingJewelryGoldRow): Boolean {
        // Must have gold bar
        if (!player.inventory.contains(GOLD_BAR_ID)) return false

        // Must have gem if required
        val gem = recipe.gem
        if (gem != null && !player.inventory.contains(gem)) return false

        // Must have the appropriate mould
        val mould = requiredMould(recipe.output) ?: return false
        return player.inventory.contains(mould)
    }

    /**
     * Main jewelry crafting loop.
     */
    private suspend fun QueueTask.craftLoop(
        player: Player,
        recipe: CraftingJewelryGoldRow,
        amount: Int,
    ) {
        val craftingLevel = player.getSkills().getCurrentLevel(Skills.CRAFTING)
        if (craftingLevel < recipe.level) {
            val outputName = itemName(recipe.output)
            player.message("You need a Crafting level of at least ${recipe.level} to make $outputName.")
            return
        }

        if (!canMakeJewelry(player, recipe)) {
            player.message("You don't have the materials to make this.")
            return
        }

        var remaining = amount

        player.animate(FURNACE_ANIMATION)

        repeatWhile(
            delay = 3,
            immediate = false,
            canRepeat = {
                remaining > 0 && canMakeJewelry(player, recipe)
            }
        ) {
            player.lock()

            val currentLevel = player.getSkills().getCurrentLevel(Skills.CRAFTING)
            if (currentLevel < recipe.level) {
                player.message("You need a Crafting level of at least ${recipe.level} to make this.")
                player.unlock()
                return@repeatWhile
            }

            // Consume gold bar
            player.inventory.remove(GOLD_BAR_ID, 1, assureFullRemoval = true)

            // Consume gem if required
            val gem = recipe.gem
            if (gem != null) {
                player.inventory.remove(gem, 1, assureFullRemoval = true)
            }

            // Produce jewelry
            player.inventory.add(recipe.output, 1)

            // XP values in the table are x10
            val xp = recipe.xp / 10.0
            player.addXp(Skills.CRAFTING, xp)

            val outputName = itemName(recipe.output)
            player.message("You make $outputName.")

            player.animate(FURNACE_ANIMATION)
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
            "jewelry"
        }
    }
}
