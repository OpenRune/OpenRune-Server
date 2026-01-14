package org.alter.interfaces.bank.scripts

import dev.openrune.definition.type.widget.IfEvent
import org.alter.tooltip
import org.alter.api.ext.boolVarBit
import org.alter.api.ext.getVarbit
import org.alter.constants
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onIfClose
import org.alter.game.pluginnew.event.impl.onIfOpen
import org.alter.interfaces.bank.configs.bank_components
import org.alter.interfaces.bank.configs.bank_comsubs
import org.alter.interfaces.bank.configs.bank_constants
import org.alter.interfaces.bank.configs.bank_interfaces
import org.alter.interfaces.bank.configs.bank_varbits
import org.alter.interfaces.bank.disableIfEvents
import org.alter.interfaces.bank.highlightNoClickClear
import org.alter.interfaces.bank.setBankWornBonuses
import org.alter.interfaces.bank.util.offset
import org.alter.interfaces.ifSetEvents
import org.alter.interfaces.ifSetText
import org.alter.mesLayerClose

class BankOpenScript : PluginEvent() {
    private val Player.bank
        get() = invMap.getOrPut("inv.bank")

    private var Player.withdrawCert by boolVarBit(bank_varbits.withdraw_mode)

    override fun init() {
        // `onBankOpen` occurs on `bank_side` trigger for emulation purposes.
        onIfOpen("interfaces.bankside") { player.onBankOpen() }
        onIfClose("interfaces.bankmain") { player.onBankClose() }
    }

    private fun Player.onBankOpen() {
        if (!disableIfEvents) {
            val capacityIncrease = bank_constants.purchasable_capacity
            withdrawCert = false
            //setBanksideExtraOps()
            setBankIfEvents()
            setBankWornBonuses()
            ifSetText(bank_components.capacity_text, 800.toString())
            tooltip(
                this,
                "Members' capacity: ${bank_constants.default_capacity}<br>" +
                    "A banker can sell you up to $capacityIncrease more.",
                bank_components.capacity_container,
                bank_components.tooltip,
            )
        }

        startInvTransmit(bank)
    }

    private fun Player.onBankClose() {
        stopInvTransmit(bank)
        mesLayerClose(this, constants.meslayer_mode_objsearch)
        if (!ui.containsOverlay(bank_interfaces.tutorial_overlay)) {
            highlightNoClickClear()
        }
        error("COMPRESS")
        //queue(bank_queues.bank_compress, 1)
    }

    private fun Player.setBankIfEvents() {
        val lastIndex = bank.indices.last
        ifSetEvents(
            bank_components.main_inventory,
            bank.indices,
            IfEvent.Op1,
            IfEvent.Op2,
            IfEvent.Op3,
            IfEvent.Op4,
            IfEvent.Op5,
            IfEvent.Op6,
            IfEvent.Op7,
            IfEvent.Op8,
            IfEvent.Op9,
            IfEvent.Op10,
            IfEvent.Depth2,
            IfEvent.DragTarget,
        )
        ifSetEvents(bank_components.main_inventory, lastIndex + 10..lastIndex + 18, IfEvent.Op1)

        // When dragging an item to a tab beyond its current size, these are the subcomponent ids
        // the server will receive from the client.
        val extendedTabOffsets = bank_comsubs.tab_extended_slots_offset
        val extendedTabSlots = extendedTabOffsets.offset(lastIndex)
        ifSetEvents(bank_components.main_inventory, extendedTabSlots, IfEvent.DragTarget)

        ifSetEvents(
            bank_components.tabs,
            bank_comsubs.main_tab..bank_comsubs.main_tab,
            IfEvent.Op1,
            IfEvent.Op7,
            IfEvent.DragTarget,
        )
        ifSetEvents(
            bank_components.tabs,
            bank_comsubs.other_tabs,
            IfEvent.Op1,
            IfEvent.Op6,
            IfEvent.Op7,
            IfEvent.Depth1,
            IfEvent.DragTarget,
        )

        ifSetEvents(
            bank_components.side_inventory,
            inventory.indices,
            IfEvent.Op1,
            IfEvent.Op2,
            IfEvent.Op3,
            IfEvent.Op4,
            IfEvent.Op5,
            IfEvent.Op6,
            IfEvent.Op7,
            IfEvent.Op8,
            IfEvent.Op9,
            IfEvent.Op10,
            IfEvent.Depth1,
            IfEvent.DragTarget,
        )
        ifSetEvents(
            bank_components.lootingbag_inventory,
            inventory.indices,
            IfEvent.Op1,
            IfEvent.Op2,
            IfEvent.Op3,
            IfEvent.Op4,
            IfEvent.Op5,
            IfEvent.Op6,
            IfEvent.Op7,
            IfEvent.Op10,
        )
        ifSetEvents(
            bank_components.league_inventory,
            inventory.indices,
            IfEvent.Op1,
            IfEvent.Op2,
            IfEvent.Op3,
            IfEvent.Op4,
            IfEvent.Op5,
            IfEvent.Op6,
            IfEvent.Op7,
            IfEvent.Op10,
        )
        ifSetEvents(
            bank_components.worn_inventory,
            inventory.indices,
            IfEvent.Op1,
            IfEvent.Op9,
            IfEvent.Op10,
            IfEvent.Depth1,
            IfEvent.DragTarget,
        )

        ifSetEvents(bank_components.incinerator_confirm, 1..bank.size, IfEvent.Op1)
        ifSetEvents(bank_components.bank_tab_display, 0..8, IfEvent.Op1)
    }

}
