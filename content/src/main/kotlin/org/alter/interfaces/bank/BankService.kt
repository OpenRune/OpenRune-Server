package org.alter.interfaces.bank

import org.alter.api.ext.message
import org.alter.game.info.PlayerInfo
import org.alter.game.model.ExamineEntityType
import org.alter.game.model.entity.Player
import org.alter.game.model.inv.Inventory
import org.alter.game.model.inv.invtx.invAdd
import org.alter.game.model.inv.invtx.invDel
import org.alter.game.model.item.Item
import org.alter.interfaces.bank.BankState.bankActiveTab
import org.alter.interfaces.bank.BankState.bankInsertMode
import org.alter.interfaces.bank.BankState.bankPlaceholderMode
import org.alter.interfaces.bank.BankState.bankSearchMode
import org.alter.interfaces.bank.BankState.bankWithdrawAsNote
import org.alter.interfaces.bank.BankState.getTabSizes
import org.alter.interfaces.bank.BankState.setTabSizes
import org.alter.interfaces.bank.BankState.tabEndSlot
import org.alter.interfaces.bank.BankState.tabForSlot

object BankService {

    // --- Helpers ---

    /** Get the bank inventory. */
    fun Player.getBankInv(): Inventory = invMap.getValue("inv.bank")

    /** Find slot of an item in bank by ID (amount > 0). Returns -1 if not found. */
    fun findItemSlot(bankInv: Inventory, itemId: Int): Int {
        for (i in bankInv.indices) {
            val item = bankInv[i] ?: continue
            if (item.id == itemId && item.amount > 0) return i
        }
        return -1
    }

    /**
     * Find a placeholder slot for the given real item ID.
     * Placeholder items have placeholderTemplate > 0 and placeholderLink == realItemId.
     */
    fun findPlaceholderSlot(bankInv: Inventory, realItemId: Int): Int {
        for (i in bankInv.indices) {
            val item = bankInv[i] ?: continue
            if (item.amount != 0) continue
            val def = item.getDef()
            if (def.placeholderTemplate > 0 && def.placeholderLink == realItemId) return i
        }
        return -1
    }

    /**
     * After removing an item from [slot], shift subsequent items in the same
     * tab down by one and decrement the tab size.
     */
    fun shiftSlotInTab(player: Player, bankInv: Inventory, slot: Int) {
        val sizes = player.getTabSizes()
        val tab = tabForSlot(sizes, slot)
        if (tab < 0) return

        val tabEnd = tabEndSlot(sizes, tab)

        for (i in slot until tabEnd - 1) {
            bankInv[i] = bankInv[i + 1]
        }
        bankInv[tabEnd - 1] = null

        sizes[tab]--
        player.setTabSizes(sizes)
    }

    /** Send an item examine message. */
    fun examine(player: Player, itemId: Int) {
        player.world.sendExamine(player, itemId, ExamineEntityType.ITEM)
    }

    // --- Withdraw ---

    /**
     * Withdraw items from bank at [slot] with given [amount].
     * Uses invDel with placehold flag, then invAdd with cert flag for noted mode.
     */
    fun withdraw(player: Player, slot: Int, amount: Int) {
        val bankInv = player.getBankInv()
        val bankItem = bankInv[slot] ?: return

        // Don't withdraw placeholders
        val def = bankItem.getDef()
        if (def.placeholderTemplate > 0 && def.placeholderLink > 0) return

        val actual = minOf(amount, bankItem.amount)
        if (actual <= 0) return

        // Remove from bank with placeholder support
        val removed = player.invDel(
            bankInv, bankItem.id, actual,
            slot = slot,
            placehold = player.bankPlaceholderMode,
        )

        if (!removed.success) return

        // Add to inventory (with cert flag for noted conversion)
        val added = player.invAdd(
            player.inventory, bankItem.id, actual,
            cert = player.bankWithdrawAsNote,
        )

        if (!added.success) {
            // Rollback: put items back in bank
            player.invAdd(bankInv, bankItem.id, actual, slot = slot)
            player.message("You don't have enough inventory space.")
            return
        }

        // If slot is now empty and no placeholder was left, shift within tab
        if (bankInv[slot] == null) {
            shiftSlotInTab(player, bankInv, slot)
        }
    }

    // --- Deposit ---

    /**
     * Deposit items from inventory at [invSlot] with given [amount].
     */
    fun deposit(player: Player, invSlot: Int, amount: Int) {
        val bankInv = player.getBankInv()
        val invItem = player.inventory[invSlot] ?: return

        // Get unnoted form for bank storage
        val unnotedItem = invItem.toUnnoted()
        val actual = minOf(amount, player.inventory.getItemCount(invItem.id))
        if (actual <= 0) return

        // Check if item already exists in bank or has a placeholder
        val existingSlot = findItemSlot(bankInv, unnotedItem.id)
        val placeholderSlot = if (existingSlot == -1) findPlaceholderSlot(bankInv, unnotedItem.id) else -1

        if (existingSlot == -1 && placeholderSlot == -1) {
            // Need a new slot — check capacity
            val totalItems = player.getTabSizes().sum()
            if (totalItems >= 800) {
                player.message("Your bank is too full.")
                return
            }
        }

        // If placeholder exists, replace it with real item at amount 0
        // so the invAdd will stack onto it at the correct position
        if (placeholderSlot != -1) {
            bankInv[placeholderSlot] = Item(unnotedItem.id, 0)
        }

        // Remove from inventory
        val removed = player.invDel(player.inventory, invItem.id, actual)
        if (!removed.success) return

        // Add to bank (uncert converts noted -> unnoted automatically)
        val added = player.invAdd(bankInv, invItem.id, actual, uncert = true)

        if (!added.success) {
            // Rollback: give items back
            player.invAdd(player.inventory, invItem.id, actual)
            player.message("Your bank is too full.")
            return
        }

        // If this was a new item (no existing stack, no placeholder), update tab size
        if (existingSlot == -1 && placeholderSlot == -1) {
            val sizes = player.getTabSizes()
            val targetTab = if (player.bankSearchMode) 0 else player.bankActiveTab
            sizes[targetTab]++
            player.setTabSizes(sizes)
        }
    }

    /**
     * Deposit all items from player inventory.
     */
    fun depositInventory(player: Player) {
        for (slot in 0 until 28) {
            if (player.inventory[slot] != null) {
                deposit(player, slot, Int.MAX_VALUE)
            }
        }
    }

    /**
     * Release (remove) a placeholder at [slot].
     */
    fun releasePlaceholder(player: Player, slot: Int) {
        val bankInv = player.getBankInv()
        val bankItem = bankInv[slot] ?: return
        val def = bankItem.getDef()
        if (def.placeholderTemplate <= 0 || def.placeholderLink <= 0) return
        bankInv[slot] = null
        shiftSlotInTab(player, bankInv, slot)
    }

    /**
     * Move (swap or insert) a bank item from [fromSlot] to [toSlot].
     */
    fun moveItem(player: Player, fromSlot: Int, toSlot: Int) {
        val bankInv = player.getBankInv()
        if (fromSlot == toSlot) return
        if (fromSlot < 0 || fromSlot >= bankInv.size) return
        if (toSlot < 0 || toSlot >= bankInv.size) return

        if (player.bankInsertMode) {
            // Insert mode: shift items between from and to
            val item = bankInv[fromSlot] ?: return
            if (fromSlot < toSlot) {
                for (i in fromSlot until toSlot) {
                    bankInv[i] = bankInv[i + 1]
                }
            } else {
                for (i in fromSlot downTo toSlot + 1) {
                    bankInv[i] = bankInv[i - 1]
                }
            }
            bankInv[toSlot] = item
        } else {
            // Swap mode
            val temp = bankInv[fromSlot]
            bankInv[fromSlot] = bankInv[toSlot]
            bankInv[toSlot] = temp
        }
    }

    /**
     * Deposit all equipped items and refresh appearance.
     */
    fun depositEquipment(player: Player) {
        val bankInv = player.getBankInv()
        val equipInv = player.equipment

        for (slot in equipInv.indices) {
            val item = equipInv[slot] ?: continue
            val unnotedId = item.toUnnoted().id

            val existingSlot = findItemSlot(bankInv, unnotedId)
            val placeholderSlot = if (existingSlot == -1) findPlaceholderSlot(bankInv, unnotedId) else -1

            if (existingSlot == -1 && placeholderSlot == -1) {
                val totalItems = player.getTabSizes().sum()
                if (totalItems >= 800) {
                    player.message("Your bank is too full.")
                    return
                }
            }

            if (placeholderSlot != -1) {
                bankInv[placeholderSlot] = Item(unnotedId, 0)
            }

            val removed = player.invDel(equipInv, item.id, item.amount, slot = slot)
            if (!removed.success) continue

            val added = player.invAdd(bankInv, item.id, item.amount, uncert = true)
            if (added.success && existingSlot == -1 && placeholderSlot == -1) {
                val sizes = player.getTabSizes()
                sizes[0]++ // Equipment always deposits to main tab
                player.setTabSizes(sizes)
            }
        }

        // Refresh player appearance — equipment changed
        PlayerInfo(player).syncAppearance()
    }
}
