package org.alter.plugins.content.interfaces.bank

import org.alter.api.*
import org.alter.api.cfg.*
import org.alter.api.dsl.*
import org.alter.api.ext.*
import org.alter.game.*
import org.alter.game.model.*
import org.alter.game.model.attr.*
import org.alter.game.model.attr.INTERACTING_COMPONENT_CHILD
import org.alter.game.model.attr.INTERACTING_ITEM_SLOT
import org.alter.game.model.attr.OTHER_ITEM_SLOT_ATTR
import org.alter.game.model.container.*
import org.alter.game.model.container.key.*
import org.alter.game.model.entity.*
import org.alter.game.model.item.*
import org.alter.game.model.priv.Privilege
import org.alter.game.model.queue.*
import org.alter.game.model.shop.*
import org.alter.game.model.timer.*
import org.alter.game.plugin.*
import org.alter.plugins.content.interfaces.bank.Bank.insert
import org.alter.plugins.content.interfaces.bank.BankTabs.dropToTab
import org.alter.plugins.content.interfaces.bank.BankTabs.insertionPoint
import org.alter.plugins.content.interfaces.bank.BankTabs.numTabsUnlocked
import org.alter.plugins.content.interfaces.bank.BankTabs.shiftTabs
import org.alter.plugins.content.interfaces.bank.BankTabs.startPoint
import org.alter.plugins.content.interfaces.bank.config.Components
import org.alter.plugins.content.interfaces.bank.config.Interfaces
import org.alter.plugins.content.interfaces.bank.config.Varbits

class BankTabsPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
        
    init {

        /**
         * Handles setting the current selected tab varbit on tab selection.
         *
         * When you take out to inv from bank -> It leaves empty gaps -> But when you put everything via Bank All -> The empty gaps get subtracted.
         */
        onButton(Interfaces.BANK_MAIN, Components.TABS) {
            val dstTab = player.getInteractingSlot() - 10
            val opt = player.getInteractingOption()
            when (opt) {
                1 -> {
                    if (dstTab <= numTabsUnlocked(player)) {
                        player.setVarbit(Varbits.CURRENTTAB, dstTab)
                    }
                }
                5 -> {
                    player.message("Not implemented [Bank1]")
                }
                6 -> {
                    // @TODO Remove placeholders for that tab
                    // If no placeholders Text: You don't have any placeholders to release. else Nothing xd
                    player.message("Not implemented [Bank2]")
                }
                else -> {
                    player.printAndMessageIfHasPower(
                        ("Unknown option from component: [$Interfaces.BANK_MAIN:${Components.TABS}]: $opt"),
                        Privilege.ADMIN_POWER,
                    )
                }
            }
        }

        onButton(Interfaces.BANK_MAIN, 113) {
        }

        /**
         * Moving items to tabs via the top tabs bar.
         */
        onComponentToComponentItemSwap(
            srcInterfaceId = Interfaces.BANK_MAIN,
            srcComponent = Components.BANK_MAINTAB_COMPONENT,
            dstInterfaceId = Interfaces.BANK_MAIN,
            dstComponent = Components.TABS,
        ) {
            val srcComponent = player.attr[INTERACTING_COMPONENT_CHILD]!!
            if (srcComponent == Components.TABS) { // attempting to drop tab on bank!!
                return@onComponentToComponentItemSwap
            } else { // perform drop to tab
                val dstSlot = player.attr[OTHER_ITEM_SLOT_ATTR]!!
                dropToTab(player, dstSlot - 10)
            }
        }

        /**
         * Moving tabs via the top tabs bar to swap/insert their order.
         */
        onComponentToComponentItemSwap(
            srcInterfaceId = Interfaces.BANK_MAIN,
            srcComponent = Components.TABS,
            dstInterfaceId = Interfaces.BANK_MAIN,
            dstComponent = Components.TABS,
        ) {
            val container = player.bank
            val srcTab = player.attr[INTERACTING_ITEM_SLOT]!!
            val dstTab = player.attr[OTHER_ITEM_SLOT_ATTR]!!
            if (dstTab == 0) {
                var item = startPoint(player, srcTab)
                var end = insertionPoint(player, srcTab)
                while (item != end) {
                    container.insert(item, container.nextFreeSlot - 1)
                    end--
                    player.setVarbit(Varbits.TAB_DISPLAY + srcTab, player.getVarbit(Varbits.TAB_DISPLAY + srcTab) - 1)
                    // check for empty tab shift
                    if (player.getVarbit(Varbits.TAB_DISPLAY + srcTab) == 0 && srcTab <= numTabsUnlocked(player)) {
                        shiftTabs(player, srcTab)
                    }
                }
                return@onComponentToComponentItemSwap
            }
            val srcSize = player.getVarbit(Varbits.TAB_DISPLAY + srcTab)
            val dstSize = player.getVarbit(Varbits.TAB_DISPLAY + dstTab)
            val insertMode = player.getVarbit(Varbits.INSERTMODE) == 1
            if (insertMode) {
                if (dstTab < srcTab) { // insert each of the items in srcTab directly before dstTab moving index up each time to account for shifts
                    var destination = startPoint(player, dstTab)
                    for (item in startPoint(player, srcTab) until insertionPoint(player, srcTab))
                        container.insert(item, destination++)
                    // update tab size varbits according to insertion location
                    var holder = player.getVarbit(Varbits.TAB_DISPLAY + dstTab)
                    player.setVarbit(Varbits.TAB_DISPLAY + dstTab, srcSize)
                    for (tab in dstTab + 1..srcTab) {
                        val temp = player.getVarbit(Varbits.TAB_DISPLAY + tab)
                        player.setVarbit(Varbits.TAB_DISPLAY + tab, holder)
                        holder = temp
                    }
                } else { // insert each item in srcTab before dstTab consuming index move in the shifts already in insert()
                    if (dstTab == srcTab + 1) {
                        return@onComponentToComponentItemSwap
                    }
                    val destination = startPoint(player, dstTab) - 1
                    val srcStart = startPoint(player, srcTab)
                    for (item in 1..srcSize)
                        container.insert(srcStart, destination)
                    var holder = player.getVarbit(4169 + dstTab)
                    player.setVarbit(4169 + dstTab, srcSize)
                    for (tab in dstTab - 2 downTo srcTab) {
                        val temp = player.getVarbit(Varbits.TAB_DISPLAY + tab)
                        player.setVarbit(Varbits.TAB_DISPLAY + tab, holder)
                        holder = temp
                    }
                }
            } else { // swap tabs in place
                val smallerTab = if (dstSize <= srcSize) dstTab else srcTab
                val smallSize = player.getVarbit(Varbits.TAB_DISPLAY + smallerTab)
                val largerTab = if (dstSize > srcSize) dstTab else srcTab
                val largeSize = player.getVarbit(Varbits.TAB_DISPLAY + largerTab)
                val smallStart = startPoint(player, smallerTab)
                val largeStart = startPoint(player, largerTab)

                // direct swap those that will easily fit
                var dex = largeStart
                for (item in smallStart until insertionPoint(player, smallerTab)) {
                    container.swap(item, dex++)
                }
                // insert left overs from larger tab into smaller tab's end
                var insertDex = insertionPoint(player, smallerTab)
                var largeEnd = insertionPoint(player, largerTab)
                while (dex != largeEnd) {
                    if (largerTab < smallerTab) { // not size but tab order
                        container.insert(dex, insertDex - 1)
                        largeEnd--
                    } else {
                        container.insert(dex++, insertDex++)
                    }
                }
                // update each tab's size to reflect new contents
                player.setVarbit(Varbits.TAB_DISPLAY + smallerTab, largeSize)
                player.setVarbit(Varbits.TAB_DISPLAY + largerTab, smallSize)
            }
        }
    }
}
