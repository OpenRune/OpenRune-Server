package org.alter.interfaces.bank

import org.alter.api.ext.message
import org.alter.game.info.PlayerInfo
import org.alter.api.ext.sendItemContainer
import org.alter.game.model.ExamineEntityType
import org.alter.game.model.entity.Player
import org.alter.game.model.inv.Inventory
import org.alter.game.model.inv.invtx.invAdd
import org.alter.game.model.inv.invtx.invDel
import org.alter.rscm.RSCM.asRSCM
import org.alter.interfaces.bank.BankState.bankInsertMode
import org.alter.interfaces.bank.BankState.bankPlaceholderMode
import org.alter.interfaces.bank.BankState.bankWithdrawAsNote
import org.alter.interfaces.bank.BankState.getTabSizes
import org.alter.interfaces.bank.BankState.setTabSizes
import org.alter.interfaces.bank.BankState.tabEndSlot
import org.alter.interfaces.bank.BankState.tabForSlot
import org.alter.interfaces.bank.BankState.tabStartSlot

object BankService {

    // --- Helpers ---

    /** Get the bank inventory. */
    fun Player.getBankInv(): Inventory = invMap.getValue("inv.bank")

    /** Force send a full bank inventory update to the client. */
    private fun refreshBank(player: Player) {
        val bankInv = player.getBankInv()
        // Send targeted update to the bankmain:items component
        val packed = "components.bankmain:items".asRSCM()
        val interfaceId = packed shr 16
        val componentId = packed and 0xFFFF
        player.sendItemContainer(interfaceId, componentId, bankInv.type.id, bankInv.objs)
    }

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
            // Rollback: clear any placeholder left by invDel, then restore item
            if (bankInv[slot] != null) {
                val slotDef = bankInv[slot]!!.getDef()
                if (slotDef.placeholderTemplate > 0) {
                    bankInv[slot] = null
                }
            }
            player.invAdd(bankInv, bankItem.id, actual, slot = slot)
            player.message("You don't have enough inventory space.")
            return
        }

        // If slot is now empty and no placeholder was left, shift within tab
        if (bankInv[slot] == null) {
            shiftSlotInTab(player, bankInv, slot)
        }

        refreshBank(player)
    }

    // --- Deposit ---

    /**
     * Deposit items from inventory at [invSlot] with given [amount].
     */
    fun deposit(player: Player, invSlot: Int, amount: Int) {
        val bankInv = player.getBankInv()
        val invItem = player.inventory[invSlot] ?: return

        val actual = minOf(amount, player.inventory.getItemCount(invItem.id))
        if (actual <= 0) return

        // Remove from inventory
        val removed = player.invDel(player.inventory, invItem.id, actual)
        if (!removed.success) return

        // Add to bank (uncert converts noted -> unnoted, bank is STACK mode so it auto-stacks)
        val added = player.invAdd(bankInv, invItem.id, actual, uncert = true)

        if (!added.success) {
            // Rollback: give items back
            player.invAdd(player.inventory, invItem.id, actual)
            player.message("Your bank is too full.")
            return
        }

        // Update tab 0 size for new items (items go to main tab by default)
        val sizes = player.getTabSizes()
        val totalBefore = sizes.sum()
        val totalNow = bankInv.occupiedSpace()
        if (totalNow > totalBefore) {
            sizes[0] += (totalNow - totalBefore)
            player.setTabSizes(sizes)
        }

        refreshBank(player)
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
        refreshBank(player)
    }

    /**
     * Move (swap or insert) a bank item from [fromSlot] to [toSlot].
     * In insert mode, only allows movement within the same tab to respect tab boundaries.
     * In swap mode, items are simply exchanged.
     */
    fun moveItem(player: Player, fromSlot: Int, toSlot: Int) {
        val bankInv = player.getBankInv()
        if (fromSlot == toSlot) return
        if (fromSlot < 0 || fromSlot >= bankInv.size) return
        if (toSlot < 0 || toSlot >= bankInv.size) return

        if (player.bankInsertMode) {
            // Insert mode: only allow within the same tab
            val sizes = player.getTabSizes()
            val fromTab = tabForSlot(sizes, fromSlot)
            val toTab = tabForSlot(sizes, toSlot)
            if (fromTab < 0 || toTab < 0) return
            if (fromTab != toTab) return // cannot insert across tab boundaries

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
        refreshBank(player)
    }

    // --- Tab Operations ---

    /**
     * Create a new tab by moving the item at [sourceSlot] to the first empty tab (1-9).
     */
    fun createTab(player: Player, sourceSlot: Int) {
        val bankInv = player.getBankInv()
        val item = bankInv[sourceSlot] ?: return
        val sizes = player.getTabSizes()

        val sourceTab = tabForSlot(sizes, sourceSlot)
        if (sourceTab < 0) return

        // Find first empty tab (1-9)
        var newTab = -1
        for (t in 1..9) {
            if (sizes[t] == 0) { newTab = t; break }
        }
        if (newTab == -1) {
            player.message("You don't have any free bank tabs.")
            return
        }

        // Remove item from source tab: shift items down within source tab
        val sourceEnd = tabEndSlot(sizes, sourceTab)
        for (i in sourceSlot until sourceEnd - 1) {
            bankInv[i] = bankInv[i + 1]
        }
        bankInv[sourceEnd - 1] = null
        sizes[sourceTab]--

        // If source tab (1-9) is now empty, shift higher tabs down to keep contiguous
        if (sourceTab in 1..9 && sizes[sourceTab] == 0) {
            for (i in sourceTab until 9) {
                sizes[i] = sizes[i + 1]
            }
            sizes[9] = 0
            // Recalculate newTab since tab indices may have shifted
            newTab = -1
            for (t in 1..9) {
                if (sizes[t] == 0) { newTab = t; break }
            }
            if (newTab == -1) return // shouldn't happen
        }

        // Calculate insert position for new tab (it has size 0 currently, so start == end)
        val insertPos = tabStartSlot(sizes, newTab)

        // Shift everything from insertPos onward right by 1
        val totalItems = sizes.sum()
        for (i in totalItems downTo insertPos + 1) {
            bankInv[i] = bankInv[i - 1]
        }

        // Place item at insertPos
        bankInv[insertPos] = item
        sizes[newTab] = 1

        player.setTabSizes(sizes)
        refreshBank(player)
    }

    /**
     * Collapse a tab, moving all its items back to the end of tab 0 (main).
     * Tabs above the collapsed one shift down to fill the gap.
     */
    fun collapseTab(player: Player, tab: Int) {
        if (tab !in 1..9) return
        val bankInv = player.getBankInv()
        val sizes = player.getTabSizes()
        val tabSize = sizes[tab]
        if (tabSize == 0) return

        // 1. Save tab items
        val tabStart = tabStartSlot(sizes, tab)
        val tabItems = Array(tabSize) { bankInv[tabStart + it] }

        // 2. Close the gap (shift items after this tab's range down by tabSize)
        val totalItems = sizes.sum()
        for (i in tabStart until totalItems - tabSize) {
            bankInv[i] = bankInv[i + tabSize]
        }
        for (i in totalItems - tabSize until totalItems) {
            bankInv[i] = null
        }
        sizes[tab] = 0

        // 3. Make room at end of tab 0 for the collapsed items
        val mainEnd = sizes[0]
        val remainingAfterMain = sizes.drop(1).sum()
        // Shift remaining items (tabs 1-9) right by tabSize
        for (i in mainEnd + remainingAfterMain - 1 downTo mainEnd) {
            bankInv[i + tabSize] = bankInv[i]
        }
        // Insert tab items at end of main
        for (i in tabItems.indices) {
            bankInv[mainEnd + i] = tabItems[i]
        }
        sizes[0] += tabSize

        // 4. Shift tab numbers: tabs above collapsed one move down
        for (i in tab until 9) {
            sizes[i] = sizes[i + 1]
        }
        sizes[9] = 0

        player.setTabSizes(sizes)
        refreshBank(player)
    }

    // --- Placeholder Operations ---

    /**
     * Release (remove) all placeholders from the bank, compacting within each tab.
     */
    fun releaseAllPlaceholders(player: Player) {
        val bankInv = player.getBankInv()
        val sizes = player.getTabSizes()

        var readOffset = 0
        var writeOffset = 0
        for (tab in 0..9) {
            val tabSize = sizes[tab]
            if (tabSize == 0) continue
            val tabReadStart = readOffset
            val tabWriteStart = writeOffset
            var kept = 0
            for (i in 0 until tabSize) {
                val item = bankInv[tabReadStart + i] ?: continue
                val def = item.getDef()
                // Skip placeholders (template > 0, link > 0, amount == 0)
                if (def.placeholderTemplate > 0 && def.placeholderLink > 0 && item.amount == 0) continue
                if (tabWriteStart + kept != tabReadStart + i) {
                    bankInv[tabWriteStart + kept] = item
                }
                kept++
            }
            // Null out remaining slots in this tab's old range
            for (i in tabWriteStart + kept until tabReadStart + tabSize) {
                bankInv[i] = null
            }
            sizes[tab] = kept
            readOffset += tabSize
            writeOffset += kept
        }
        // Null trailing slots
        for (i in writeOffset until bankInv.size) {
            if (bankInv[i] != null) bankInv[i] = null
        }

        player.setTabSizes(sizes)
        refreshBank(player)
    }

    /**
     * Deposit all equipped items and refresh appearance.
     */
    fun depositEquipment(player: Player) {
        val bankInv = player.getBankInv()
        val equipInv = player.equipment
        val sizesBefore = player.getTabSizes()
        val totalBefore = sizesBefore.sum()

        for (slot in equipInv.indices) {
            val item = equipInv[slot] ?: continue

            val removed = player.invDel(equipInv, item.id, item.amount, slot = slot)
            if (!removed.success) continue

            val added = player.invAdd(bankInv, item.id, item.amount, uncert = true)
            if (!added.success) {
                // Rollback this item
                player.invAdd(equipInv, item.id, item.amount, slot = slot)
            }
        }

        // Update tab 0 size for any new items added
        val totalNow = bankInv.occupiedSpace()
        if (totalNow > totalBefore) {
            val sizes = player.getTabSizes()
            sizes[0] += (totalNow - totalBefore)
            player.setTabSizes(sizes)
        }

        PlayerInfo(player).syncAppearance()
        refreshBank(player)
    }
}
