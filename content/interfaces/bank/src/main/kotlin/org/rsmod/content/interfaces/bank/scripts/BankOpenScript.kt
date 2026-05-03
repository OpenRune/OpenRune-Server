package org.rsmod.content.interfaces.bank.scripts

import dev.openrune.definition.type.widget.IfEvent
import jakarta.inject.Inject
import org.rsmod.api.combat.weapon.WeaponSpeeds
import org.rsmod.api.config.constants
import org.rsmod.api.player.bonus.WornBonuses
import org.rsmod.api.player.output.ClientScripts.mesLayerClose
import org.rsmod.api.player.output.ClientScripts.tooltip
import org.rsmod.api.player.startInvTransmit
import org.rsmod.api.player.stopInvTransmit
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.ui.ifSetText
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfOpen
import org.rsmod.content.interfaces.bank.bankCapacity
import org.rsmod.content.interfaces.bank.configs.bank_comsubs
import org.rsmod.content.interfaces.bank.configs.bank_constants
import org.rsmod.content.interfaces.bank.disableIfEvents
import org.rsmod.content.interfaces.bank.highlightNoClickClear
import org.rsmod.content.interfaces.bank.setBankWornBonuses
import org.rsmod.content.interfaces.bank.setBanksideExtraOps
import org.rsmod.content.interfaces.bank.util.offset
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class BankOpenScript
@Inject
constructor(private val wornBonuses: WornBonuses, private val weaponSpeeds: WeaponSpeeds) :
    PluginScript() {
    private val Player.bank
        get() = invMap.getOrPut("inv.bank")

    private var Player.withdrawCert by boolVarBit("varbit.bank_withdrawnotes")

    override fun ScriptContext.startup() {
        // `onBankOpen` occurs on `bank_side` trigger for emulation purposes.
        onIfOpen("interface.bankside") { player.onBankOpen() }
        onIfClose("interface.bankmain") { player.onBankClose() }
    }

    private fun Player.onBankOpen() {
        if (!disableIfEvents) {
            val capacityIncrease = bank_constants.purchasable_capacity
            withdrawCert = false
            setBanksideExtraOps()
            setBankIfEvents()
            setBankWornBonuses(wornBonuses, weaponSpeeds)
            ifSetText("component.bankmain:capacity", bankCapacity.toString())
            tooltip(
                this,
                "Members' capacity: ${bank_constants.default_capacity}<br>" +
                    "A banker can sell you up to $capacityIncrease more.",
                "component.bankmain:capacity_layer",
                "component.bankmain:tooltip",
            )
        }

        startInvTransmit(bank)
    }

    private fun Player.onBankClose() {
        stopInvTransmit(bank)
        mesLayerClose(this, constants.meslayer_mode_objsearch)
        if (!ui.containsOverlay("interface.screenhighlight")) {
            highlightNoClickClear()
        }
        queue("queue.bank_compress", 1)
    }

    private fun Player.setBankIfEvents() {
        val lastIndex = bank.indices.last
        ifSetEvents(
            "component.bankmain:items",
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
        ifSetEvents("component.bankmain:items", lastIndex + 10..lastIndex + 18, IfEvent.Op1)

        // When dragging an item to a tab beyond its current size, these are the subcomponent ids
        // the server will receive from the client.
        val extendedTabOffsets = bank_comsubs.tab_extended_slots_offset
        val extendedTabSlots = extendedTabOffsets.offset(lastIndex)
        ifSetEvents("component.bankmain:items", extendedTabSlots, IfEvent.DragTarget)

        ifSetEvents(
            "component.bankmain:tabs",
            bank_comsubs.main_tab..bank_comsubs.main_tab,
            IfEvent.Op1,
            IfEvent.Op7,
            IfEvent.DragTarget,
        )
        ifSetEvents(
            "component.bankmain:tabs",
            bank_comsubs.other_tabs,
            IfEvent.Op1,
            IfEvent.Op6,
            IfEvent.Op7,
            IfEvent.Depth1,
            IfEvent.DragTarget,
        )

        ifSetEvents(
            "component.bankside:items",
            inv.indices,
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
            "component.bankside:lootingbag_items",
            inv.indices,
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
            "component.bankside:league_secondinv_items",
            inv.indices,
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
            "component.bankside:wornops",
            inv.indices,
            IfEvent.Op1,
            IfEvent.Op9,
            IfEvent.Op10,
            IfEvent.Depth1,
            IfEvent.DragTarget,
        )

        ifSetEvents("component.bankmain:incinerator_confirm", 1..bank.size, IfEvent.Op1)
        ifSetEvents("component.bankmain:dropdown_content", 0..8, IfEvent.Op1)
    }
}
