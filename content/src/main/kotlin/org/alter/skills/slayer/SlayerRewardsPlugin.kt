package org.alter.skills.slayer

import org.alter.api.InterfaceDestination
import org.alter.api.ext.*
import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onButton
import org.alter.rscm.RSCM.getRSCM
import org.generated.tables.slayer.SlayerUnlockRow

/**
 * Handles the Slayer Rewards interface.
 * - Buy tab: Purchase items with slayer points
 * - Unlock tab: Unlock abilities and extensions
 * - Tasks tab: Block/unblock tasks
 * - Extend tab: Extend task assignments
 */
class SlayerRewardsPlugin : PluginEvent() {

    companion object {
        // Attribute keys for temporary state
        private val SLAYER_REWARDS_TAB_ATTR = AttributeKey<String>("slayer_rewards_tab")
        private val SLAYER_REWARDS_SELECTED_ATTR = AttributeKey<Int>("slayer_rewards_selected")
        // Interface ID
        private const val SLAYER_REWARDS_INTERFACE = "interfaces.slayer_rewards"

        // Tab components
        private const val TAB_BUY = "components.slayer_rewards:buy"
        private const val TAB_UNLOCK = "components.slayer_rewards:unlock"
        private const val TAB_EXTEND = "components.slayer_rewards:extend"
        private const val TAB_TASKS = "components.slayer_rewards:tasks"

        // Confirm button
        private const val CONFIRM_BUTTON = "components.slayer_rewards:confirm_button"

        // Task block slots (6 per master)
        private val TASK_SLOTS = listOf(
            "components.slayer_rewards:tasks_slot_1",
            "components.slayer_rewards:tasks_slot_2",
            "components.slayer_rewards:tasks_slot_3",
            "components.slayer_rewards:tasks_slot_4",
            "components.slayer_rewards:tasks_slot_5",
            "components.slayer_rewards:tasks_slot_6",
        )

        // Buy tab items with costs
        val BUY_ITEMS = listOf(
            BuyItem("Slayer's respite", "items.slayers_respite", 25, "Restore some Slayer level after drinking."),
            BuyItem("Expeditious bracelet", "items.expeditious_bracelet", 25, "25% chance to not count kills toward your task."),
            BuyItem("Bracelet of slaughter", "items.bracelet_slaughter", 25, "25% chance to not count kills toward your task."),
            BuyItem("Herb sack", "items.herb_sack", 750, "Store grimy herbs while on slayer tasks."),
            BuyItem("Rune pouch", "items.rune_pouch", 750, "Store runes in a compact pouch."),
            BuyItem("Coal bag", "items.coal_bag", 100, "Store extra coal for smithing."),
            BuyItem("Ring of wealth scroll", "items.ring_wealth_scroll", 50, "Imbue your ring of wealth."),
        )

        // Unlock items loaded from cache
        val UNLOCKS: List<SlayerUnlockRow> by lazy { SlayerDefinitions.unlocks }
    }

    data class BuyItem(
        val name: String,
        val itemKey: String,
        val cost: Int,
        val description: String
    )

    override fun init() {
        // Handle tab switching
        onButton(TAB_BUY) {
            openTab(player, "buy")
        }

        onButton(TAB_UNLOCK) {
            openTab(player, "unlock")
        }

        onButton(TAB_EXTEND) {
            openTab(player, "extend")
        }

        onButton(TAB_TASKS) {
            openTab(player, "tasks")
        }

        // Handle confirm button
        onButton(CONFIRM_BUTTON) {
            handleConfirm(player)
        }

        // Handle task slot clicks (blocking)
        TASK_SLOTS.forEachIndexed { index, slot ->
            onButton(slot) {
                handleTaskSlotClick(player, index + 1)
            }
        }
    }

    /**
     * Open the slayer rewards interface.
     */
    fun openRewardsInterface(player: Player) {
        player.openInterface(SLAYER_REWARDS_INTERFACE, InterfaceDestination.MAIN_SCREEN)
        updatePointsDisplay(player)
        openTab(player, "buy") // Default to buy tab
    }

    /**
     * Open a specific tab in the rewards interface.
     */
    private fun openTab(player: Player, tab: String) {
        // Update the current tab varbit/state
        player.attr[SLAYER_REWARDS_TAB_ATTR] = tab

        when (tab) {
            "buy" -> populateBuyTab(player)
            "unlock" -> populateUnlockTab(player)
            "extend" -> populateExtendTab(player)
            "tasks" -> populateTasksTab(player)
        }
    }

    /**
     * Update points display.
     */
    private fun updatePointsDisplay(player: Player) {
        val points = SlayerManager.getPoints(player)
        // Set points text in interface
        try {
            player.setComponentText(
                getRSCM(SLAYER_REWARDS_INTERFACE),
                getRSCM("components.slayer_rewards:com_5") and 0xFFFF,
                "Reward points: $points"
            )
        } catch (_: Exception) {
            // Component may not exist
        }
    }

    /**
     * Populate the buy tab with purchasable items.
     */
    private fun populateBuyTab(player: Player) {
        // The buy tab shows items that can be purchased with slayer points
        // This would need proper interface scripting to populate dynamically
    }

    /**
     * Populate the unlock tab with available unlocks.
     */
    private fun populateUnlockTab(player: Player) {
        // Show available unlocks from SlayerDefinitions.unlocks
        // Filter out already purchased unlocks
    }

    /**
     * Populate the extend tab with task extensions.
     */
    private fun populateExtendTab(player: Player) {
        // Show task extension options
        // These increase the amount of monsters assigned for specific tasks
    }

    /**
     * Populate the tasks tab with blocked tasks.
     */
    private fun populateTasksTab(player: Player) {
        val masterId = SlayerManager.getCurrentMaster(player)
        // Show current blocked tasks for this master
        // Allow blocking/unblocking
    }

    /**
     * Handle confirm button press.
     */
    private fun handleConfirm(player: Player) {
        val tab = player.attr.getOrDefault(SLAYER_REWARDS_TAB_ATTR, "buy")
        val selectedItem = player.attr[SLAYER_REWARDS_SELECTED_ATTR] ?: return

        when (tab) {
            "buy" -> handleBuyPurchase(player, selectedItem)
            "unlock" -> handleUnlockPurchase(player, selectedItem)
            "extend" -> handleExtendPurchase(player, selectedItem)
        }
    }

    /**
     * Handle purchasing an item from the buy tab.
     */
    private fun handleBuyPurchase(player: Player, itemIndex: Int) {
        if (itemIndex < 0 || itemIndex >= BUY_ITEMS.size) return

        val item = BUY_ITEMS[itemIndex]
        val points = SlayerManager.getPoints(player)

        if (points < item.cost) {
            player.message("You need ${item.cost} Slayer reward points to purchase this.")
            return
        }

        // Check inventory space
        if (player.inventory.freeSlotCount < 1) {
            player.message("You don't have enough inventory space.")
            return
        }

        // Deduct points and give item
        SlayerManager.addPoints(player, -item.cost)
        try {
            player.inventory.add(getRSCM(item.itemKey))
            player.message("You purchased ${item.name} for ${item.cost} points.")
        } catch (_: Exception) {
            // Item doesn't exist, refund
            SlayerManager.addPoints(player, item.cost)
            player.message("Unable to purchase this item.")
        }

        updatePointsDisplay(player)
    }

    /**
     * Handle purchasing an unlock.
     */
    private fun handleUnlockPurchase(player: Player, unlockIndex: Int) {
        if (unlockIndex < 0 || unlockIndex >= UNLOCKS.size) return

        val unlock = UNLOCKS[unlockIndex]
        val points = SlayerManager.getPoints(player)

        // Check if already unlocked
        if (hasUnlock(player, unlock)) {
            player.message("You have already unlocked this.")
            return
        }

        if (points < unlock.cost) {
            player.message("You need ${unlock.cost} Slayer reward points to unlock this.")
            return
        }

        // Purchase unlock
        SlayerManager.addPoints(player, -unlock.cost)
        setUnlock(player, unlock, true)
        player.message("You've unlocked: ${unlock.name}")

        updatePointsDisplay(player)
    }

    /**
     * Handle purchasing a task extension.
     */
    private fun handleExtendPurchase(player: Player, extensionIndex: Int) {
        // Extensions are stored in the unlock table with specific bits
        // They increase task amounts for specific monster types
        val extensions = UNLOCKS.filter { it.name.contains("Extend") || it.name.contains("Longer") }

        if (extensionIndex < 0 || extensionIndex >= extensions.size) return

        val extension = extensions[extensionIndex]
        val points = SlayerManager.getPoints(player)

        if (hasUnlock(player, extension)) {
            player.message("You have already extended this task.")
            return
        }

        if (points < extension.cost) {
            player.message("You need ${extension.cost} Slayer reward points to extend this task.")
            return
        }

        SlayerManager.addPoints(player, -extension.cost)
        setUnlock(player, extension, true)
        player.message("You've extended: ${extension.name}")

        updatePointsDisplay(player)
    }

    /**
     * Handle clicking a task slot for blocking.
     */
    private fun handleTaskSlotClick(player: Player, slot: Int) {
        val masterId = SlayerManager.getCurrentMaster(player)
        if (masterId <= 0) {
            player.message("You need to have a slayer master assigned first.")
            return
        }

        // Check if slot already has a blocked task
        val blockedTask = getBlockedTask(player, masterId, slot)

        if (blockedTask > 0) {
            // Offer to unblock
            player.queue { unblockTaskDialogue(player, masterId, slot, blockedTask) }
        } else {
            // Offer to block current task
            player.queue { blockTaskDialogue(player, masterId, slot) }
        }
    }

    /**
     * Dialogue to unblock a task.
     */
    private suspend fun QueueTask.unblockTaskDialogue(player: Player, masterId: Int, slot: Int, taskId: Int) {
        val taskRow = SlayerDefinitions.tasks.find { it.id == taskId }
        val taskName = taskRow?.nameUppercase ?: "Unknown"

        messageBox(player, "This slot is blocking: $taskName<br><br>Would you like to unblock it?")

        val confirm = options(player, "Yes, unblock it.", "No, keep it blocked.")
        if (confirm == 1) {
            setBlockedTask(player, masterId, slot, 0)
            player.message("You've unblocked $taskName.")
        }
    }

    /**
     * Dialogue to block a task.
     */
    private suspend fun QueueTask.blockTaskDialogue(player: Player, masterId: Int, slot: Int) {
        if (!SlayerManager.hasTask(player)) {
            messageBox(player, "You need an active slayer task to block it.")
            return
        }

        val currentTask = SlayerManager.getCurrentTask(player)
        if (currentTask == null) {
            messageBox(player, "You don't have a valid task to block.")
            return
        }

        val cost = 100
        val points = SlayerManager.getPoints(player)

        if (points < cost) {
            messageBox(player, "You need $cost Slayer reward points to block a task.<br><br>You have $points points.")
            return
        }

        messageBox(player, "Block ${currentTask.nameUppercase}?<br><br>This will cost $cost points.")

        val confirm = options(player, "Yes, block this task.", "No, don't block it.")
        if (confirm == 1) {
            SlayerManager.addPoints(player, -cost)
            setBlockedTask(player, masterId, slot, currentTask.id)
            SlayerManager.clearTask(player)
            player.message("You've blocked ${currentTask.nameUppercase} and cancelled your task.")
            updatePointsDisplay(player)
        }
    }

    /**
     * Get blocked task for a specific slot.
     */
    private fun getBlockedTask(player: Player, masterId: Int, slot: Int): Int {
        val masterPrefix = getMasterPrefix(masterId) ?: return 0
        return try {
            player.getVarbit("varbits.slayer_blocked_${masterPrefix}_$slot")
        } catch (_: Exception) {
            0
        }
    }

    /**
     * Set blocked task for a specific slot.
     */
    private fun setBlockedTask(player: Player, masterId: Int, slot: Int, taskId: Int) {
        val masterPrefix = getMasterPrefix(masterId) ?: return
        try {
            player.setVarbit("varbits.slayer_blocked_${masterPrefix}_$slot", taskId)
        } catch (_: Exception) {
            // Varbit doesn't exist
        }
    }

    /**
     * Get master prefix for varbit keys.
     */
    private fun getMasterPrefix(masterId: Int): String? {
        return when (masterId) {
            1 -> "turael"
            2 -> "mazchna"
            3 -> "vannaka"
            4 -> "chaeldar"
            5 -> "duradel"
            6 -> "konar"
            7 -> "nieve"
            8 -> "krystilia"
            else -> null
        }
    }

    /**
     * Check if player has an unlock.
     */
    private fun hasUnlock(player: Player, unlock: SlayerUnlockRow): Boolean {
        return try {
            // The bit field in unlock row corresponds to a varbit
            player.getVarbit("varbits.${unlock.bit}") == 1
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Set an unlock state.
     */
    private fun setUnlock(player: Player, unlock: SlayerUnlockRow, unlocked: Boolean) {
        try {
            player.setVarbit("varbits.${unlock.bit}", if (unlocked) 1 else 0)
        } catch (_: Exception) {
            // Varbit doesn't exist
        }
    }
}

