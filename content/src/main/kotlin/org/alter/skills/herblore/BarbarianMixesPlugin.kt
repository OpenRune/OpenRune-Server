package org.alter.skills.herblore

import dev.openrune.ServerCacheManager.getItem
import org.alter.api.Skills
import org.alter.api.ext.filterableMessage
import org.alter.api.ext.message
import org.alter.game.model.entity.Entity
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.ItemOnItemEvent

/**
 * Plugin for creating barbarian mixes (two-dose potion + roe/caviar)
 */
class BarbarianMixesPlugin : PluginEvent() {

    override fun init() {
        // Register item-on-item interactions for barbarian mixes
        onEvent<ItemOnItemEvent> {
            val item1 = fromItem.id
            val item2 = toItem.id

            // Check for barbarian mix (two-dose potion + roe/caviar)
            val mixData = HerbloreDefinitions.findBarbarianMix(item1, item2)
            if (mixData != null) {
                createBarbarianMix(player, mixData)
                return@onEvent
            }
        }
    }

    /**
     * Creates a barbarian mix from a two-dose potion and roe/caviar.
     */
    private fun createBarbarianMix(
        player: Player,
        mixData: HerbloreDefinitions.BarbarianMixData
    ) {
        val herbloreLevel = player.getSkills().getCurrentLevel(Skills.HERBLORE)

        // Check level requirement
        if (herbloreLevel < mixData.level) {
            player.filterableMessage("You need a Herblore level of ${mixData.level} to make this mix.")
            return
        }

        // Check if player has both items
        val hasPotion = player.inventory.contains(mixData.twoDosePotion)
        val hasIngredient = player.inventory.contains(mixData.mixIngredient)

        if (!hasPotion || !hasIngredient) {
            player.message(Entity.NOTHING_INTERESTING_HAPPENS)
            return
        }

        // Check inventory space (removing 2 items, adding 1)
        if (player.inventory.freeSlotCount < 1 && !player.inventory.contains(mixData.barbarianMix)) {
            player.filterableMessage("You don't have enough inventory space to make this mix.")
            return
        }

        // Remove ingredients
        val potionRemoved = player.inventory.remove(mixData.twoDosePotion, 1)
        val ingredientRemoved = player.inventory.remove(mixData.mixIngredient, 1)

        if (!potionRemoved.hasSucceeded() || !ingredientRemoved.hasSucceeded()) {
            // Restore items if removal failed
            if (potionRemoved.hasSucceeded()) player.inventory.add(mixData.twoDosePotion, 1)
            if (ingredientRemoved.hasSucceeded()) player.inventory.add(mixData.mixIngredient, 1)
            player.message(Entity.NOTHING_INTERESTING_HAPPENS)
            return
        }

        // Add barbarian mix
        val addResult = player.inventory.add(mixData.barbarianMix, 1)
        if (!addResult.hasSucceeded()) {
            // Restore items if adding failed
            player.inventory.add(mixData.twoDosePotion, 1)
            player.inventory.add(mixData.mixIngredient, 1)
            player.filterableMessage("You don't have enough inventory space to make this mix.")
            return
        }

        // Award XP and message (barbarian mixes typically give 0 XP, but we check anyway)
        if (mixData.xp > 0) {
            player.addXp(Skills.HERBLORE, mixData.xp.toDouble())
        }

        val mixName = getItem(mixData.barbarianMix)?.name ?: "mix"
        player.filterableMessage("You add the ingredient to the potion.")
        player.filterableMessage("You make a $mixName.")
    }
}

