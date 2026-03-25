package org.alter.interfaces.bank

import org.alter.api.ext.inputInt
import org.alter.game.pluginnew.MenuOption
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onButton
import org.alter.game.pluginnew.event.impl.onIfModalDrag
import org.alter.game.pluginnew.event.impl.onInterfaceClose
import org.alter.interfaces.bank.BankService.getBankInv
import org.alter.interfaces.bank.BankState.bankActiveTab
import org.alter.interfaces.bank.BankState.bankInsertMode
import org.alter.interfaces.bank.BankState.bankLastXAmount
import org.alter.interfaces.bank.BankState.bankPlaceholderMode
import org.alter.interfaces.bank.BankState.bankPreSearchTab
import org.alter.interfaces.bank.BankState.bankSearchMode
import org.alter.interfaces.bank.BankState.bankWithdrawAsNote

class BankPlugin : PluginEvent() {

    override fun init() {

        // --- Close ---
        onInterfaceClose("interfaces.bankmain") {
            player.closeBank()
        }

        // --- Withdraw from bank ---
        onButton("components.bankmain:items") {
            val bankInv = player.getBankInv()
            val bankItem = bankInv[slot] ?: return@onButton

            when (op) {
                MenuOption.OP1 -> BankService.withdraw(player, slot, 1)
                MenuOption.OP2 -> BankService.withdraw(player, slot, 5)
                MenuOption.OP3 -> BankService.withdraw(player, slot, 10)
                MenuOption.OP4 -> {
                    player.queue {
                        val amount = inputInt(player, "Enter amount")
                        if (amount > 0) {
                            player.bankLastXAmount = amount
                            BankService.withdraw(player, slot, amount)
                        }
                    }
                }
                MenuOption.OP5 -> BankService.withdraw(player, slot, bankItem.amount)
                MenuOption.OP6 -> {
                    if (bankItem.amount > 1) {
                        BankService.withdraw(player, slot, bankItem.amount - 1)
                    }
                }
                MenuOption.OP7 -> {
                    // Withdraw last-X; if never set, prompt
                    val lastX = player.bankLastXAmount
                    if (lastX > 0) {
                        BankService.withdraw(player, slot, lastX)
                    } else {
                        player.queue {
                            val amount = inputInt(player, "Enter amount")
                            if (amount > 0) {
                                player.bankLastXAmount = amount
                                BankService.withdraw(player, slot, amount)
                            }
                        }
                    }
                }
                MenuOption.OP8 -> BankService.releasePlaceholder(player, slot)
                MenuOption.OP10 -> BankService.examine(player, item)
                else -> {}
            }
        }

        // --- Deposit from inventory side panel ---
        onButton("components.bankside:items") {
            val invItem = player.inventory[slot] ?: return@onButton

            when (op) {
                MenuOption.OP1 -> BankService.deposit(player, slot, 1)
                MenuOption.OP2 -> BankService.deposit(player, slot, 5)
                MenuOption.OP3 -> BankService.deposit(player, slot, 10)
                MenuOption.OP4 -> {
                    player.queue {
                        val amount = inputInt(player, "Enter amount")
                        if (amount > 0) {
                            player.bankLastXAmount = amount
                            BankService.deposit(player, slot, amount)
                        }
                    }
                }
                MenuOption.OP5 -> BankService.deposit(player, slot, invItem.amount)
                MenuOption.OP10 -> BankService.examine(player, item)
                else -> {}
            }
        }

        // --- Deposit all inventory ---
        onButton("components.bankmain:depositinv") {
            BankService.depositInventory(player)
        }

        // --- Deposit all equipment ---
        onButton("components.bankmain:depositworn") {
            BankService.depositEquipment(player)
        }

        // --- Tab selection ---
        onButton("components.bankmain:tabs") {
            player.bankActiveTab = slot
        }

        // --- Toggle noted withdrawal ---
        onButton("components.bankmain:note_graphic") {
            player.bankWithdrawAsNote = !player.bankWithdrawAsNote
        }

        // --- Toggle insert mode ---
        onButton("components.bankmain:swap_insert_graphic") {
            player.bankInsertMode = !player.bankInsertMode
        }

        // --- Toggle placeholder mode ---
        onButton("components.bankmain:placeholder_graphic") {
            player.bankPlaceholderMode = !player.bankPlaceholderMode
        }

        // --- Search mode ---
        // Search is client-driven (client opens its own search input and filters).
        // Server just tracks state for deposit targeting.
        onButton("components.bankmain:search") {
            player.bankPreSearchTab = player.bankActiveTab
            player.bankSearchMode = true
        }

        // --- Drag to rearrange within bank ---
        onIfModalDrag("components.bankmain:items") {
            val from = selectedSlot ?: return@onIfModalDrag
            val to = targetSlot ?: return@onIfModalDrag
            BankService.moveItem(player, from, to)
        }

        // --- Drag item to tab header to create tab ---
        onIfModalDrag("components.bankmain:tabs") {
            val fromSlot = selectedSlot ?: return@onIfModalDrag
            BankService.createTab(player, fromSlot)
        }

        // --- Release all placeholders ---
        onButton("components.bankmain:menu_button") {
            BankService.releaseAllPlaceholders(player)
        }
    }
}
