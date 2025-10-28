package org.alter.plugins.content.interfaces.bank

import org.alter.api.*
import org.alter.api.cfg.*
import org.alter.api.dsl.*
import org.alter.api.ext.*
import org.alter.game.*
import org.alter.game.action.EquipAction
import org.alter.game.model.*
import org.alter.game.model.attr.*
import org.alter.game.model.attr.INTERACTING_ITEM_SLOT
import org.alter.game.model.attr.OTHER_ITEM_SLOT_ATTR
import org.alter.game.model.container.*
import org.alter.game.model.container.key.*
import org.alter.game.model.entity.*
import org.alter.game.model.item.*
import org.alter.game.model.queue.*
import org.alter.game.model.shop.*
import org.alter.game.model.timer.*
import org.alter.game.plugin.*
import org.alter.plugins.content.interfaces.bank.config.Interfaces.BANK_MAIN
import org.alter.plugins.content.interfaces.bank.config.Components.BANK_MAINTAB_COMPONENT
import org.alter.plugins.content.interfaces.bank.config.Interfaces.BANKSIDE
import org.alter.plugins.content.interfaces.bank.Bank.deposit
import org.alter.plugins.content.interfaces.bank.Bank.insert
import org.alter.plugins.content.interfaces.bank.Bank.removePlaceholder
import org.alter.plugins.content.interfaces.bank.Bank.withdraw
import org.alter.plugins.content.interfaces.bank.BankTabs.BANK_TAB_ROOT_VARBIT
import org.alter.plugins.content.interfaces.bank.BankTabs.SELECTED_TAB_VARBIT
import org.alter.plugins.content.interfaces.bank.BankTabs.dropToTab
import org.alter.plugins.content.interfaces.bank.BankTabs.getCurrentTab
import org.alter.plugins.content.interfaces.bank.BankTabs.numTabsUnlocked
import org.alter.plugins.content.interfaces.bank.BankTabs.shiftTabs
import org.alter.plugins.content.interfaces.bank.config.Components
import org.alter.plugins.content.interfaces.bank.config.Interfaces
import org.alter.plugins.content.interfaces.bank.config.Varbits

class BankPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {

    init {
        onInterfaceOpen(Interfaces.BANK_MAIN) {
            var slotOffset = 0
            for (tab in 1..9) {
                val size = player.getVarbit(BANK_TAB_ROOT_VARBIT + tab)
                for (slot in slotOffset until slotOffset + size) {
                    if (player.bank[slot] == null) {
                        player.setVarbit(BANK_TAB_ROOT_VARBIT + tab, player.getVarbit(BANK_TAB_ROOT_VARBIT + tab) - 1)
                        // check for empty tab shift
                        if (player.getVarbit(BANK_TAB_ROOT_VARBIT + tab) == 0 && tab <= numTabsUnlocked(player)) {
                            shiftTabs(player, tab)
                        }
                    }
                }
                slotOffset += size
            }
            player.bank.shift()
        }

        onInterfaceClose(Interfaces.BANK_MAIN) {
            player.closeInterface(dest = InterfaceDestination.TAB_AREA)
            player.closeInputDialog()
        }

        intArrayOf(19, 21).forEachIndexed { index, button ->
            onButton(interfaceId = Interfaces.BANK_MAIN, component = button) {
                player.setVarbit(Varbits.INSERTMODE, index)
            }
        }

        intArrayOf(24, 26).forEachIndexed { index, button ->
            onButton(interfaceId = Interfaces.BANK_MAIN, component = button) {
                player.setVarbit(Varbits.WITHDRAW_NOTES, index)
            }
        }

        onButton(interfaceId = Interfaces.BANK_MAIN, component = 40) {
            player.toggleVarbit(Varbits.LEAVEPLACEHOLDERS)
        }

        intArrayOf(30,32,34,36,38).forEach { quantity ->
            onButton(interfaceId = Interfaces.BANK_MAIN, component = quantity) {
                val state = (quantity - 27) / 2 // wat?
                player.message("You clicked? $quantity")
                player.message("You clicked? Also state: $state")
                player.setVarbit(Varbits.QUANITY_TYPE, state - 1)
            }
        }

        /**
         * Added incinerator support.
         */
        onButton(interfaceId = Interfaces.BANK_MAIN, component = 53) {
            player.toggleVarbit(Varbits.SHOW_INCINERATOR)
        }

        onButton(interfaceId = Interfaces.BANK_MAIN, component = Components.DEPOSIT_WORN) {
            val slot = player.getInteractingSlot() - 1
            val destroyItems = player.bank[slot]!!
            val tabAffected = getCurrentTab(player, slot)

            player.playSound(Sound.FIREBREATH)
            player.bank.remove(destroyItems, assureFullRemoval = true)
            player.setVarbit(BANK_TAB_ROOT_VARBIT + tabAffected, player.getVarbit(BANK_TAB_ROOT_VARBIT + tabAffected) - 1)
            player.bank.shift()
        }

// bank inventory
        onButton(interfaceId = Interfaces.BANK_MAIN, component = 44) {
            val from = player.inventory
            val to = player.bank
            for (i in 0 until from.capacity) {
                val item = player.inventory[i]
                item?.let {
                    deposit(player, item.id, item.amount)
                }
            }
            if (!from.isEmpty) {
                /**
                 * @TODO
                 */
                player.message("Bank full. || theres ${Int.MAX_VALUE} of some item.")
            }
        }
// bank equipment
        onButton(interfaceId = Interfaces.BANK_MAIN, component = 46) {
            val from = player.equipment
            val to = player.bank

            var any = false
            for (i in 0 until from.capacity) {
                val item = from[i] ?: continue

                val total = item.amount

                var toSlot = to.removePlaceholder(world, item)
                var placeholder = true
                val curTab = player.getVarbit(SELECTED_TAB_VARBIT)
                if (toSlot == -1) {
                    placeholder = false
                    toSlot = to.getLastFreeSlot()
                }

                val deposited = from.transfer(to, item, fromSlot = i, toSlot = toSlot, note = false, unnote = true)?.completed ?: 0

                if (total != deposited) {
                    // Was not able to deposit the whole stack of [item].
                }
                if (deposited > 0) {
                    any = true
                    if (curTab != 0 && !placeholder) {
                        println("Equipment banker 1.")
                        dropToTab(player, curTab, to.getLastFreeSlot() - 1, true)
                    }
                    EquipAction.onItemUnequip(player, item.id, i)
                }
            }
            if (!any && !from.isEmpty) {
                player.message("Bank full.")
            }
        }

// deposit
        onButton(interfaceId = Interfaces.BANKSIDE, component = Components.BANKSIDE_CHILD) p@{
            val opt = player.getInteractingOption()
            val slot = player.getInteractingSlot()

            val item = player.inventory[slot] ?: return@p

            if (opt == 10) {
                world.sendExamine(player, item.id, ExamineEntityType.ITEM)
                return@p
            }

            val quantityVarbit = player.getVarbit(Varbits.QUANITY_TYPE)
            var amount: Int

            when {
                quantityVarbit == 0 ->
                    amount =
                        when (opt) {
                            2 -> 1
                            4 -> 5
                            5 -> 10
                            6 -> player.getVarbit(Varbits.REQUESTEDQUANTITY)
                            7 -> -1 // X
                            8 -> 0 // All
                            else -> return@p
                        }
                opt == 2 ->
                    amount =
                        when (quantityVarbit) {
                            1 -> 5
                            2 -> 10
                            3 -> if (player.getVarbit(Varbits.REQUESTEDQUANTITY) == 0) -1 else player.getVarbit(Varbits.REQUESTEDQUANTITY)
                            4 -> 0 // All
                            else -> return@p
                        }
                else ->
                    amount =
                        when (opt) {
                            3 -> 1
                            4 -> 5
                            5 -> 10
                            6 -> player.getVarbit(Varbits.REQUESTEDQUANTITY)
                            7 -> -1 // X
                            8 -> 0 // All
                            else -> return@p
                        }
            }

            println("DEPOSIT BUTTON EXEC")
            if (amount == 0) {
                amount = player.inventory.getItemCount(item.id)
            } else if (amount == -1) {
                player.queue(TaskPriority.WEAK) {
                    amount = inputInt(player, "How many would you like to bank?")
                    if (amount > 0) {
                        player.setVarbit(Varbits.REQUESTEDQUANTITY, amount)
                        deposit(player, item.id, amount)
                    }
                }
                return@p
            }
            deposit(player, item.id, amount)
        }

// withdraw
        onButton(interfaceId = Interfaces.BANK_MAIN, component = BANK_MAINTAB_COMPONENT) p@{
            val opt = player.getInteractingOption()
            val slot = player.getInteractingSlot()

            val item = player.bank[slot] ?: return@p

            if (opt == 10) {
                world.sendExamine(player, item.id, ExamineEntityType.ITEM)
                return@p
            }

            var amount: Int
            var placehold = false

            val quantityVarbit = player.getVarbit(Varbits.QUANITY_TYPE)
            when {
                quantityVarbit == 0 ->
                    amount =
                        when (opt) {
                            1 -> 1
                            3 -> 5
                            4 -> 10
                            5 -> player.getVarbit(Varbits.REQUESTEDQUANTITY)
                            6 -> -1 // X
                            7 -> item.amount
                            8 -> item.amount - 1
                            9 -> {
                                placehold = true
                                item.amount
                            }
                            else -> return@p
                        }
                opt == 1 ->
                    amount =
                        when (quantityVarbit) {
                            0 -> 1
                            1 -> 5
                            2 -> 10
                            3 -> if (player.getVarbit(Varbits.REQUESTEDQUANTITY) == 0) -1 else player.getVarbit(Varbits.REQUESTEDQUANTITY)
                            4 -> item.amount
                            8 -> {
                                placehold = true
                                item.amount
                            }
                            else -> return@p
                        }
                else ->
                    amount =
                        when (opt) {
                            2 -> 1
                            3 -> 5
                            4 -> 10
                            5 -> player.getVarbit(Varbits.REQUESTEDQUANTITY)
                            6 -> -1 // X
                            7 -> item.amount
                            8 -> item.amount - 1
                            9 -> {
                                placehold = true
                                item.amount
                            }
                            else -> return@p
                        }
            }

            if (amount == -3) {
                /**
                 * Placeholders' "release" option uses the same option
                 * as "withdraw-x" would.
                 */
                if (item.amount == -2) {
                    player.bank[slot] = null
                    return@p
                }
            }

            if (amount == -1) {
                player.queue(TaskPriority.WEAK) {
                    amount = inputInt(player, "How many would you like to withdraw?")
                    if (amount > 0) {
                        player.setVarbit(Varbits.REQUESTEDQUANTITY, amount)
                        withdraw(player, item.id, amount, slot, placehold)
                    }
                }
                return@p
            }

            amount = Math.max(0, amount)
            if (amount > 0) {
                withdraw(player, item.id, amount, slot, placehold)
            }
        }

        /**
         * Swap items in bank inventory interface.
         */
        onComponentToComponentItemSwap(
            srcInterfaceId = Interfaces.BANKSIDE,
            srcComponent = Components.BANKSIDE_CHILD,
            dstInterfaceId = Interfaces.BANKSIDE,
            dstComponent = Components.BANKSIDE_CHILD,
        ) {
            println("Here")
            val srcSlot = player.attr[INTERACTING_ITEM_SLOT]!!
            val dstSlot = player.attr[OTHER_ITEM_SLOT_ATTR]!!

            val container = player.inventory

            if (srcSlot in 0 until container.capacity && dstSlot in 0 until container.capacity) {
                container.swap(srcSlot, dstSlot)
            }
        }

        /**
         * Swap items in main bank tab.
         */
        onComponentToComponentItemSwap(
            srcInterfaceId = Interfaces.BANK_MAIN,
            srcComponent = BANK_MAINTAB_COMPONENT,
            dstInterfaceId = Interfaces.BANK_MAIN,
            dstComponent = BANK_MAINTAB_COMPONENT,
        ) {
            val srcSlot = player.attr[INTERACTING_ITEM_SLOT]!!
            val dstSlot = player.attr[OTHER_ITEM_SLOT_ATTR]!!

            val container = player.bank

            /**
             * Handles the empty box components in the last row of each tab
             * for dropping items into the specified tab's empty space.
             */
            if (dstSlot in 834..843) {
                dropToTab(player, dstSlot - 834)
                return@onComponentToComponentItemSwap
            }

            if (srcSlot in 0 until container.occupiedSlotCount && dstSlot in 0 until container.occupiedSlotCount) {
                val insertMode = player.getVarbit(Varbits.INSERTMODE) == 1
                if (!insertMode) {
                    container.swap(srcSlot, dstSlot)
                } else { // insert mode patch for movement between bank tabs and updating varbits
                    val curTab = getCurrentTab(player, srcSlot)
                    val dstTab = getCurrentTab(player, dstSlot)
                    if (dstTab != curTab) {
                        if ((dstTab > curTab && curTab != 0) || dstTab == 0) {
                            container.insert(srcSlot, dstSlot - 1)
                        } else {
                            container.insert(srcSlot, dstSlot)
                        }

                        if (dstTab != 0) {
                            player.setVarbit(BANK_TAB_ROOT_VARBIT + dstTab, player.getVarbit(BANK_TAB_ROOT_VARBIT + dstTab) + 1)
                        }
                        if (curTab != 0) {
                            player.setVarbit(BANK_TAB_ROOT_VARBIT + curTab, player.getVarbit(BANK_TAB_ROOT_VARBIT + curTab) - 1)
                            if (player.getVarbit(BANK_TAB_ROOT_VARBIT + curTab) == 0 && curTab <= numTabsUnlocked(player)) {
                                shiftTabs(player, curTab)
                            }
                        }
                    } else {
                        container.insert(srcSlot, dstSlot)
                    }
                }
            } else {
                // Sync the container on the client
                container.dirty = true
            }
        }

        bind_unequip(EquipmentType.HEAD, 76)
        bind_unequip(EquipmentType.CAPE, 77)
        bind_unequip(EquipmentType.AMULET, 78)
        bind_unequip(EquipmentType.AMMO, 86)
        bind_unequip(EquipmentType.WEAPON, 79)
        bind_unequip(EquipmentType.CHEST, 80)
        bind_unequip(EquipmentType.SHIELD, 81)
        bind_unequip(EquipmentType.LEGS, 82)
        bind_unequip(EquipmentType.GLOVES, 83)
        bind_unequip(EquipmentType.BOOTS, 84)
        bind_unequip(EquipmentType.RING, 85)

        onButton(interfaceId = Interfaces.BANKSIDE, component = 4) {
            val slot = player.getInteractingSlot()
            val opt = player.getInteractingOption()
            val item = player.inventory[slot] ?: return@onButton
            if (opt == 0) {
                val result = EquipAction.equip(player, item, inventorySlot = slot)
                if (result == EquipAction.Result.SUCCESS) {
                    player.calculateBonuses()
                    Bank.sendBonuses(player)
                } else if (result == EquipAction.Result.UNHANDLED) {
                    player.message("You can't equip that.")
                }
            } else if (opt == 9) {
                world.sendExamine(player, item.id, ExamineEntityType.ITEM)
            }
        }
    }

    fun bind_unequip(
        equipment: EquipmentType,
        component: Int,
    ) {
        onButton(interfaceId = Interfaces.BANK_MAIN, component = component) {
            val opt = player.getInteractingOption()
            if (opt == 0) {
                EquipAction.unequip(player, equipment.id)
                player.calculateBonuses()
                Bank.sendBonuses(player)
            } else if (opt == 9) {
                val item = player.equipment[equipment.id] ?: return@onButton
                world.sendExamine(player, item.id, ExamineEntityType.ITEM)
            } else {
                val item = player.equipment[equipment.id] ?: return@onButton
                if (!world.plugins.executeItem(player, item.id, opt)) {
                    val slot = player.getInteractingSlot()
                    if (world.devContext.debugButtons) {
                        player.message(
                            "Unhandled button action: [component=[${Interfaces.BANK_MAIN}:$component], option=$opt, slot=$slot, item=${item.id}]",
                        )
                    }
                }
            }
        }
    }
}
