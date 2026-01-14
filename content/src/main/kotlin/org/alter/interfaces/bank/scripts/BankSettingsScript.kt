package org.alter.interfaces.bank.scripts

import org.alter.api.ext.inputInt
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.MenuOption
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onIfModalButton
import org.alter.interfaces.bank.configs.bank_components
import org.alter.interfaces.bank.*


class BankSettingsScript : PluginEvent() {

    override fun init() {
        val comps = bank_components

        onIfModalButton(comps.rearrange_mode_swap) { player.insertMode = false }
        onIfModalButton(comps.rearrange_mode_insert) { player.insertMode = true }
        onIfModalButton(comps.withdraw_mode_item) { player.withdrawCert = false }
        onIfModalButton(comps.withdraw_mode_note) { player.withdrawCert = true }
        onIfModalButton(comps.always_placehold) { player.alwaysPlacehold = !player.alwaysPlacehold }
        onIfModalButton(comps.quantity_1) { player.leftClickQtyMode = QuantityMode.One }
        onIfModalButton(comps.quantity_5) { player.leftClickQtyMode = QuantityMode.Five }
        onIfModalButton(comps.quantity_10) { player.leftClickQtyMode = QuantityMode.Ten }
        onIfModalButton(comps.quantity_all) { player.leftClickQtyMode = QuantityMode.All }
        onIfModalButton(comps.quantity_x) { player.selectQuantityX(op) }

        onIfModalButton(comps.bank_tab_display) { player.selectTabDisplay(slot) }
        onIfModalButton(comps.incinerator_toggle) { player.incinerator = !player.incinerator }
        onIfModalButton(comps.tutorial_button_toggle) { player.tutorialButton = !player.tutorialButton }
        onIfModalButton(comps.inventory_item_options_toggle) { player.toggleInvItemOptions() }
        onIfModalButton(comps.deposit_inv_toggle) { player.depositInvButton = !player.depositInvButton }
        onIfModalButton(comps.deposit_worn_toggle) { player.depositWornButton = !player.depositWornButton }
        onIfModalButton(comps.release_placehold) { player.selectReleasePlaceholders() }
        onIfModalButton(comps.bank_fillers_1) { player.bankFillerMode = BankFillerMode.One }
        onIfModalButton(comps.bank_fillers_10) { player.bankFillerMode = BankFillerMode.Ten }
        onIfModalButton(comps.bank_fillers_50) { player.bankFillerMode = BankFillerMode.Fifty }
        onIfModalButton(comps.bank_fillers_x) { player.bankFillerMode = BankFillerMode.X }
        onIfModalButton(comps.bank_fillers_all) { player.bankFillerMode = BankFillerMode.All }
        onIfModalButton(comps.bank_fillers_fill) { player.selectBankFillerFill() }
    }

    private fun Player.selectQuantityX(op: MenuOption) {
        if (op == MenuOption.OP2) {
            queue {
                lastQtyInput = inputInt(this@selectQuantityX)
            }
        }
        leftClickQtyMode = if (lastQtyInput == 0) QuantityMode.One else QuantityMode.X
    }

    private fun Player.selectTabDisplay(comsub: Int) {
        val mode =
            when (comsub) {
                1 -> TabDisplayMode.Obj
                3 -> TabDisplayMode.Digit
                5 -> TabDisplayMode.Roman
                else -> throw NotImplementedError("Unhandled tab display comsub selection: $comsub")
            }
        println("MODE: ${mode}")
        tabDisplayMode = mode
    }

    private fun Player.toggleInvItemOptions() {
        invItemOptions = !invItemOptions
        //player.setBanksideExtraOps(objTypes)
    }

    private suspend fun Player.selectReleasePlaceholders() {
        val containsPlaceholder = inventory.any { it != null && it.getDef().isPlaceholder }
        if (containsPlaceholder) {
            //bankScript.releasePlaceholders(this)
        }
    }

    private suspend fun Player.selectBankFillerFill() {
        val count = bankFillerMode.toCount()
        //bankScript.addBankFillers(this, count)
    }

    private fun BankFillerMode.toCount(): Int? =
        when (this) {
            BankFillerMode.One -> 1
            BankFillerMode.Ten -> 10
            BankFillerMode.Fifty -> 50
            BankFillerMode.X -> null
            BankFillerMode.All -> Int.MAX_VALUE
        }

}