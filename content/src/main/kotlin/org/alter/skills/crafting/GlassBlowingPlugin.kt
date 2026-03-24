package org.alter.skills.crafting

import io.github.oshai.kotlinlogging.KotlinLogging
import org.alter.api.Skills
import org.alter.api.ext.message
import org.alter.api.ext.produceItemBox
import org.alter.game.model.entity.Player
import org.alter.game.model.item.Item
import org.alter.game.model.queue.QueueTask
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.SatisfyType
import org.alter.game.pluginnew.event.impl.onItemOnItem
import org.alter.rscm.RSCM.asRSCM
import org.generated.tables.crafting.CraftingGlassRow

/**
 * Glass blowing plugin.
 *
 * Handles blowing molten glass into various glass items using a glassblowing
 * pipe. Players use a glassblowing pipe on molten glass to open the selection
 * interface, then repeatedly blow glass items.
 */
class GlassBlowingPlugin : PluginEvent() {

    companion object {
        private val logger = KotlinLogging.logger {}

        /** All glass blowing recipes. */
        lateinit var allRecipes: List<CraftingGlassRow>
            private set

        /** Recipes keyed by output item. */
        lateinit var recipesByOutput: Map<Int, CraftingGlassRow>
            private set

        private val GLASSBLOWING_PIPE_ID = "items.glassblowingpipe".asRSCM()
        private val MOLTEN_GLASS_ID = "items.molten_glass".asRSCM()

        /** Glass blowing animation. */
        private const val BLOW_ANIMATION = "sequences.human_glassblowing"
    }

    override fun init() {
        allRecipes = CraftingGlassRow.all()
        recipesByOutput = allRecipes.associateBy { it.outputItem }

        logger.info { "Loaded ${allRecipes.size} glass blowing recipes." }

        onItemOnItem(GLASSBLOWING_PIPE_ID, MOLTEN_GLASS_ID).type(SatisfyType.ANY) {
            val craftingLevel = player.getSkills().getCurrentLevel(Skills.CRAFTING)

            val available = allRecipes.filter { craftingLevel >= it.level }
            if (available.isEmpty()) {
                player.message("You need a higher Crafting level to make anything from molten glass.")
                return@type
            }

            player.queue {
                val items = available.map { it.outputItem }.toIntArray()
                produceItemBox(
                    player,
                    *items,
                    title = "What would you like to make?",
                    maxProducable = player.inventory.getItemCount(MOLTEN_GLASS_ID)
                ) { selectedItem: Int, amount: Int ->
                    val recipe = recipesByOutput[selectedItem] ?: return@produceItemBox
                    player.queue { blowLoop(player, recipe, amount) }
                }
            }
        }
    }

    /**
     * Main glass blowing loop.
     */
    private suspend fun QueueTask.blowLoop(
        player: Player,
        recipe: CraftingGlassRow,
        amount: Int,
    ) {
        val craftingLevel = player.getSkills().getCurrentLevel(Skills.CRAFTING)
        if (craftingLevel < recipe.level) {
            val outputName = itemName(recipe.outputItem)
            player.message("You need a Crafting level of at least ${recipe.level} to make $outputName.")
            return
        }

        if (!player.inventory.contains(MOLTEN_GLASS_ID)) {
            player.message("You don't have any molten glass.")
            return
        }

        var remaining = amount

        player.message("You start blowing glass.")
        player.animate(BLOW_ANIMATION)

        repeatWhile(
            delay = 3,
            immediate = false,
            canRepeat = {
                remaining > 0 &&
                    player.inventory.contains(MOLTEN_GLASS_ID) &&
                    player.inventory.contains(GLASSBLOWING_PIPE_ID)
            }
        ) {
            player.lock()

            val currentLevel = player.getSkills().getCurrentLevel(Skills.CRAFTING)
            if (currentLevel < recipe.level) {
                player.message("You need a Crafting level of at least ${recipe.level} to make this.")
                player.unlock()
                return@repeatWhile
            }

            // Consume molten glass
            player.inventory.remove(MOLTEN_GLASS_ID, 1, assureFullRemoval = true)

            // Produce glass item
            player.inventory.add(recipe.outputItem, 1)

            // XP values in the table are x10
            val xp = recipe.xp / 10.0
            player.addXp(Skills.CRAFTING, xp)

            val outputName = itemName(recipe.outputItem)
            player.message("You make $outputName.")

            player.animate(BLOW_ANIMATION)
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
            "glass item"
        }
    }
}
