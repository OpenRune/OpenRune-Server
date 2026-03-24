package org.alter.skills.crafting

import io.github.oshai.kotlinlogging.KotlinLogging
import org.alter.api.Skills
import org.alter.api.ext.message
import org.alter.game.model.entity.Player
import org.alter.game.model.item.Item
import org.alter.game.model.queue.QueueTask
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onItemOnItem
import org.generated.tables.crafting.CraftingGemsRow
import kotlin.random.Random

/**
 * Gem cutting skill plugin.
 *
 * Handles cutting uncut gems with a chisel. Players use a chisel on an
 * uncut gem to begin cutting. Each gem takes a 3-tick cycle. Semi-precious
 * gems (those with a non-null crushItem) have a chance to be crushed based
 * on the player's Crafting level.
 */
class GemCuttingPlugin : PluginEvent() {

    companion object {
        private val logger = KotlinLogging.logger {}

        /** All gem recipes keyed by uncut item ID. */
        lateinit var recipes: Map<Int, CraftingGemsRow>
            private set

        /** Chisel cutting animation. */
        private const val CHISEL_ANIMATION = "sequences.enakh_player_chisel"
    }

    override fun init() {
        val allRecipes = CraftingGemsRow.all()
        recipes = allRecipes.associateBy { it.uncut }

        logger.info { "Loaded ${recipes.size} gem cutting recipes." }

        allRecipes.forEach { gem ->
            onItemOnItem("items.chisel", gem.uncut) {
                val recipe = recipes[gem.uncut] ?: return@onItemOnItem
                player.queue { cutLoop(player, recipe) }
            }
        }
    }

    /**
     * Main gem cutting loop. Cuts gems one at a time with a 3-tick cycle
     * until the player runs out of uncut gems.
     */
    private suspend fun QueueTask.cutLoop(
        player: Player,
        recipe: CraftingGemsRow,
    ) {
        val craftingLevel = player.getSkills().getCurrentLevel(Skills.CRAFTING)
        if (craftingLevel < recipe.level) {
            val gemName = itemName(recipe.uncut)
            player.message("You need a Crafting level of at least ${recipe.level} to cut $gemName.")
            return
        }

        if (!player.inventory.contains(recipe.uncut)) {
            player.message("You don't have any ${itemName(recipe.uncut)} to cut.")
            return
        }

        player.animate(CHISEL_ANIMATION)

        repeatWhile(
            delay = 3,
            immediate = false,
            canRepeat = { player.inventory.contains(recipe.uncut) }
        ) {
            player.lock()

            val currentLevel = player.getSkills().getCurrentLevel(Skills.CRAFTING)
            if (currentLevel < recipe.level) {
                player.message("You need a Crafting level of at least ${recipe.level} to cut this.")
                player.unlock()
                return@repeatWhile
            }

            player.inventory.remove(recipe.uncut, 1)

            // Semi-precious gems can be crushed
            val crushItem = recipe.crushItem
            if (crushItem != null && shouldCrush(currentLevel, recipe.level)) {
                player.inventory.add(crushItem, 1)
                val crushXp = recipe.crushXp / 10.0
                player.addXp(Skills.CRAFTING, crushXp)
                player.message("You accidentally crush the ${itemName(recipe.uncut)}.")
            } else {
                player.inventory.add(recipe.cut, 1)
                val xp = recipe.xp / 10.0
                player.addXp(Skills.CRAFTING, xp)
                player.message("You cut the ${itemName(recipe.uncut)}.")
            }

            player.animate(CHISEL_ANIMATION)
            player.unlock()
        }
    }

    /**
     * Determines if a semi-precious gem should be crushed.
     * Higher crafting level reduces the chance of crushing.
     * The crush chance scales from ~50% at the minimum level down to ~0% at level 99.
     */
    private fun shouldCrush(craftingLevel: Int, requiredLevel: Int): Boolean {
        val levelAbove = (craftingLevel - requiredLevel).coerceAtLeast(0)
        // Crush chance decreases as the player's level increases above the requirement.
        // At required level: ~50% chance. Each level above reduces by ~1.5%.
        val crushChance = (50 - (levelAbove * 1.5)).coerceAtLeast(0.0)
        return Random.nextDouble(100.0) < crushChance
    }

    /**
     * Returns a display-friendly item name, lowercased.
     */
    private fun itemName(itemId: Int): String {
        return try {
            Item(itemId).getName().lowercase()
        } catch (_: Exception) {
            "gem"
        }
    }
}
