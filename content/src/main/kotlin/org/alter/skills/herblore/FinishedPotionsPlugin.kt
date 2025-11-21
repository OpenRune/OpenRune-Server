package org.alter.skills.herblore

import dev.openrune.ServerCacheManager.getItem
import org.alter.api.Skills
import org.alter.api.ext.filterableMessage
import org.alter.api.ext.message
import org.alter.api.ext.produceItemBox
import org.alter.game.model.entity.Entity
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.ItemOnItemEvent
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.asRSCM
import org.alter.skills.herblore.HerbloreDefinitions.requiredItems
import org.generated.tables.herblore.HerbloreFinishedRow

/**
 * Plugin for creating finished potions (unfinished potion + secondary ingredients)
 */
class FinishedPotionsPlugin : PluginEvent() {

    companion object {
        private const val VIAL_OF_WATER = "items.vial_water"
        private val VIAL_OF_WATER_ID = VIAL_OF_WATER.asRSCM()
        private const val PESTLE_AND_MORTAR = "items.pestle_and_mortar"
        private val PESTLE_AND_MORTAR_ID = PESTLE_AND_MORTAR.asRSCM()
        private const val SWAMP_TAR = "items.swamp_tar"
        private val SWAMP_TAR_ID = SWAMP_TAR.asRSCM()
    }

    override fun init() {
        // Register item-on-item interactions for finished potions
        // Exclude herb+vial interactions (handled by UnfinishedPotionsPlugin)
        // Exclude pestle and mortar interactions (handled by CrushingPlugin)
        // Exclude swamp tar interactions (handled by SwampTarPlugin)
        onEvent<ItemOnItemEvent> {
            val item1 = fromItem.id
            val item2 = toItem.id

            // Skip if this is a herb+vial interaction (handled by UnfinishedPotionsPlugin)
            val isHerbVial = (HerbloreDefinitions.herbItemIds.contains(item1) && VIAL_OF_WATER_ID == item2) ||
                             (HerbloreDefinitions.herbItemIds.contains(item2) && VIAL_OF_WATER_ID == item1)
            if (isHerbVial) {
                return@onEvent
            }

            // Skip if this is a pestle and mortar interaction (handled by CrushingPlugin)
            val isPestleMortar = (PESTLE_AND_MORTAR_ID == item1 || PESTLE_AND_MORTAR_ID == item2)
            if (isPestleMortar) {
                return@onEvent
            }

            // Skip if this is a swamp tar interaction (handled by SwampTarPlugin)
            val isSwampTar = (SWAMP_TAR_ID == item1 || SWAMP_TAR_ID == item2)
            if (isSwampTar) {
                return@onEvent
            }

            // Find all potion candidates that could match these items
            val candidates = HerbloreDefinitions.findPotionCandidates(item1, item2)

            if (candidates.isEmpty()) {
                return@onEvent
            }

            val herbloreLevel = player.getSkills().getCurrentLevel(Skills.HERBLORE)

            // Filter candidates to only those where player has ALL required ingredients AND meets level requirement
            val validCandidates = candidates.filter { potion ->
                herbloreLevel >= potion.levelRequired &&
                potion.requiredItems.all { itemId -> player.inventory.contains(itemId) }
            }

            if (validCandidates.isEmpty()) {
                return@onEvent
            }

            // Calculate max producible for each candidate and create a map
            val candidatesWithMax = validCandidates.map { potion ->
                // Calculate max producible: minimum of all ingredient counts
                val unfinishedCount = player.inventory.getItemCount(potion.unfPot)
                val secondaryCounts = potion.secondaries.map { secondaryId ->
                    player.inventory.getItemCount(secondaryId)
                }
                val allCounts = listOf(unfinishedCount) + secondaryCounts
                val maxProducible = allCounts.minOrNull() ?: 0
                potion to maxProducible
            }.filter { (_, max) -> max > 0 }

            if (candidatesWithMax.isEmpty()) {
                return@onEvent
            }

            // Show interface to select potion and quantity
            player.queue {
                val potionItems = candidatesWithMax.map { (potion, _) -> potion.outputPotion }.toIntArray()
                val maxProducible = candidatesWithMax.maxOf { (_, max) -> max }

                produceItemBox(
                    player,
                    *potionItems,
                    title = "What would you like to make?",
                    maxProducable = maxProducible
                ) { selectedItemId: Int, quantity: Int ->
                    // Find the selected potion
                    val selectedPotion = candidatesWithMax.firstOrNull { (potion, _) ->
                        potion.outputPotion == selectedItemId
                    }?.first ?: return@produceItemBox

                    // Create the specified quantity (up to what they can make)
                    createFinishedPotion(player, selectedPotion, quantity)
                }
            }
        }
    }

    /**
     * Creates a finished potion from an unfinished potion and all required secondary ingredients.
     * Creates the specified quantity (up to what the player can make).
     */
    private fun createFinishedPotion(
        player: Player,
        potionData: HerbloreFinishedRow,
        quantity: Int
    ) {
        val herbloreLevel = player.getSkills().getCurrentLevel(Skills.HERBLORE)

        // Check level requirement
        if (herbloreLevel < potionData.levelRequired) {
            player.filterableMessage("You need a Herblore level of ${potionData.levelRequired} to make this potion.")
            return
        }

        // Check if player has all required items
        // Check unfinished potion
        if (!player.inventory.contains(potionData.unfPot)) {
            player.message(Entity.NOTHING_INTERESTING_HAPPENS)
            return
        }
        // Check all secondaries (preserving duplicates if same item appears multiple times)
        val hasAllSecondaries = potionData.secondaries.all { itemId -> player.inventory.contains(itemId) }
        if (!hasAllSecondaries) {
            player.message(Entity.NOTHING_INTERESTING_HAPPENS)
            return
        }

        val outputPotion = potionData.outputPotion

        player.queue {
            // Cache level check to avoid repeated skill lookups
            var cachedLevel = player.getSkills().getCurrentLevel(Skills.HERBLORE)
            var created = 0

            // Repeat until we've created the requested quantity or run out of ingredients
            repeatWhile(delay = 4, immediate = true, canRepeat = {
                created < quantity &&
                // Check if player still has all required items for another potion
                player.inventory.contains(potionData.unfPot) &&
                // Check all secondaries (preserving duplicates)
                potionData.secondaries.all { itemId -> player.inventory.contains(itemId) } &&
                // Check inventory space (removing N items, adding 1)
                (player.inventory.freeSlotCount >= 1 || player.inventory.contains(outputPotion))
            }) {
                // Play herblore animation on each iteration
                player.animate("sequences.human_herbing_vial", interruptable = true)

                // Check level requirement again (in case it changed)
                // Only update cache if we need to check (optimization: check once per loop)
                if (cachedLevel < potionData.levelRequired) {
                    cachedLevel = player.getSkills().getCurrentLevel(Skills.HERBLORE)
                    if (cachedLevel < potionData.levelRequired) {
                        stop()
                        return@repeatWhile
                    }
                }

                // Remove ALL ingredients (primary potion + all secondaries)
                // Loop through secondaries list to preserve duplicates (if same item appears multiple times)
                val removedItems = mutableListOf<Pair<Int, Boolean>>()
                var allRemoved = true

                // Remove unfinished potion (1x)
                val unfinishedRemoved = player.inventory.remove(potionData.unfPot, 1)
                removedItems.add(potionData.unfPot to unfinishedRemoved.hasSucceeded())
                if (!unfinishedRemoved.hasSucceeded()) {
                    allRemoved = false
                }

                // Remove each secondary (preserves duplicates - if same item appears twice, remove it twice)
                for (secondaryId in potionData.secondaries) {
                    val removeResult = player.inventory.remove(secondaryId, 1)
                    removedItems.add(secondaryId to removeResult.hasSucceeded())
                    if (!removeResult.hasSucceeded()) {
                        allRemoved = false
                    }
                }

                if (!allRemoved) {
                    // Restore items if any removal failed
                    removedItems.forEach { (itemId: Int, wasRemoved: Boolean) ->
                        if (wasRemoved) {
                            player.inventory.add(itemId, 1)
                        }
                    }
                    player.message(Entity.NOTHING_INTERESTING_HAPPENS)
                    stop()
                    return@repeatWhile
                }

                // Add output potion
                val addResult = player.inventory.add(outputPotion, 1)
                if (!addResult.hasSucceeded()) {
                    // Restore all items if adding failed
                    // Restore unfinished potion
                    player.inventory.add(potionData.unfPot, 1)
                    // Restore all secondaries (preserving duplicates)
                    potionData.secondaries.forEach { itemId ->
                        player.inventory.add(itemId, 1)
                    }
                    player.filterableMessage("You don't have enough inventory space to make this potion.")
                    stop()
                    return@repeatWhile
                }

                // Award XP and message
                if (potionData.xp > 0) {
                    player.addXp(Skills.HERBLORE, potionData.xp.toDouble())
                }

                // Determine which item name to use for the message
                // If multiple secondaries, use primary (unfinished potion), otherwise use the secondary
                val itemForMessage = if (potionData.secondaries.size > 1) {
                    potionData.unfPot
                } else {
                    potionData.secondaries.firstOrNull() ?: potionData.unfPot
                }
                val itemName = getItem(itemForMessage)?.name ?: "ingredient"
                player.filterableMessage("You mix the ${itemName.lowercase()} into your potion.")

                created++
            }

            // Stop animation when done
            player.animate(RSCM.NONE)
        }
    }
}

