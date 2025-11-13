package org.alter.skills.herblore

import dev.openrune.ServerCacheManager.getItem
import org.alter.api.Skills
import org.alter.api.ext.filterableMessage
import org.alter.api.ext.message
import org.alter.game.model.entity.Entity
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onItemOnItem
import org.alter.rscm.RSCM.asRSCM

/**
 * Plugin for creating unfinished potions (herb + vial of water)
 */
class UnfinishedPotionsPlugin : PluginEvent() {

    companion object {
        private const val VIAL_OF_WATER = "items.vial_water"
        private val VIAL_OF_WATER_ID = VIAL_OF_WATER.asRSCM()
    }

    override fun init() {
        // Register herb + vial of water interactions
        HerbloreDefinitions.unfinishedPotions.forEach { potionData ->
            onItemOnItem(potionData.herbItem, VIAL_OF_WATER_ID) {
                createUnfinishedPotion(player, potionData)
            }
        }
    }

    /**
     * Creates an unfinished potion from a herb and vial of water
     */
    private fun createUnfinishedPotion(
        player: Player,
        potionData: HerbloreDefinitions.UnfinishedPotionData
    ) {
        val herbloreLevel = player.getSkills().getCurrentLevel(Skills.HERBLORE)

        // Check level requirement
        if (herbloreLevel < potionData.level) {
            player.filterableMessage("You need a Herblore level of ${potionData.level} to clean this herb.")
            return
        }

        // Check if player has both items
        val hasHerb = player.inventory.contains(potionData.herbItem)
        val hasVial = player.inventory.contains(VIAL_OF_WATER_ID)

        if (!hasHerb || !hasVial) {
            player.filterableMessage("You don't have all the ingredients needed to make this potion.")
            return
        }

        // Check inventory space (removing 2 items, adding 1)
        if (player.inventory.freeSlotCount < 1 && !player.inventory.contains(potionData.unfinishedPotion)) {
            player.filterableMessage("You don't have enough inventory space to make this potion.")
            return
        }

        player.queue {
            // Play herblore animation
            player.animate("sequences.human_herbing_vial", interruptable = true)

            // Remove ingredients
            val herbRemoved = player.inventory.remove(potionData.herbItem, 1)
            val vialRemoved = player.inventory.remove(VIAL_OF_WATER_ID, 1)

            if (!herbRemoved.hasSucceeded() || !vialRemoved.hasSucceeded()) {
                // Restore items if removal failed (unexpected state - we already validated)
                if (herbRemoved.hasSucceeded()) player.inventory.add(potionData.herbItem, 1)
                if (vialRemoved.hasSucceeded()) player.inventory.add(VIAL_OF_WATER_ID, 1)
                player.message(Entity.NOTHING_INTERESTING_HAPPENS)
                return@queue
            }

            // Add unfinished potion
            val addResult = player.inventory.add(potionData.unfinishedPotion, 1)
            if (!addResult.hasSucceeded()) {
                // Restore items if adding failed
                player.inventory.add(potionData.herbItem, 1)
                player.inventory.add(VIAL_OF_WATER_ID, 1)
                player.filterableMessage("You don't have enough inventory space to make this potion.")
                return@queue
            }

            // Award XP and message
            player.addXp(Skills.HERBLORE, potionData.xp.toDouble())
            val herbName = getItem(potionData.herbItem)?.name?.lowercase() ?: "herb"
            player.filterableMessage("You put the $herbName into the vial of water.")
        }
    }
}

