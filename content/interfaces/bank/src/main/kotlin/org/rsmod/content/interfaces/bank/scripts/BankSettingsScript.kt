package org.rsmod.content.interfaces.bank.scripts

import dev.openrune.types.aconverted.interf.IfButtonOp
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onIfModalButton
import org.rsmod.content.interfaces.bank.BankFillerMode
import org.rsmod.content.interfaces.bank.QuantityMode
import org.rsmod.content.interfaces.bank.TabDisplayMode
import org.rsmod.content.interfaces.bank.alwaysPlacehold
import org.rsmod.content.interfaces.bank.bankFillerMode
import org.rsmod.content.interfaces.bank.depositInvButton
import org.rsmod.content.interfaces.bank.depositWornButton
import org.rsmod.content.interfaces.bank.incinerator
import org.rsmod.content.interfaces.bank.insertMode
import org.rsmod.content.interfaces.bank.invItemOptions
import org.rsmod.content.interfaces.bank.lastQtyInput
import org.rsmod.content.interfaces.bank.leftClickQtyMode
import org.rsmod.content.interfaces.bank.setBanksideExtraOps
import org.rsmod.content.interfaces.bank.tabDisplayMode
import org.rsmod.content.interfaces.bank.tutorialButton
import org.rsmod.content.interfaces.bank.withdrawCert
import org.rsmod.game.type.getInvObj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class BankSettingsScript @Inject constructor(private val bankScript: BankInvScript) :
    PluginScript() {
    override fun ScriptContext.startup() {

        onIfModalButton("component.bankmain:swap") { insertMode = false }
        onIfModalButton("component.bankmain:insert") { insertMode = true }
        onIfModalButton("component.bankmain:item") { withdrawCert = false }
        onIfModalButton("component.bankmain:note") { withdrawCert = true }
        onIfModalButton("component.bankmain:placeholder") { alwaysPlacehold = !alwaysPlacehold }
        onIfModalButton("component.bankmain:quantity1") { leftClickQtyMode = QuantityMode.One }
        onIfModalButton("component.bankmain:quantity5") { leftClickQtyMode = QuantityMode.Five }
        onIfModalButton("component.bankmain:quantity10") { leftClickQtyMode = QuantityMode.Ten }
        onIfModalButton("component.bankmain:quantityall") { leftClickQtyMode = QuantityMode.All }
        onIfModalButton("component.bankmain:quantityx") { selectQuantityX(it.op) }

        onIfModalButton("component.bankmain:dropdown_content") { selectTabDisplay(it.comsub) }
        onIfModalButton("component.bankmain:incinerator_toggle") { incinerator = !incinerator }
        onIfModalButton("component.bankmain:banktut_toggle") { tutorialButton = !tutorialButton }
        onIfModalButton("component.bankmain:sideops_toggle") { toggleInvItemOptions() }
        onIfModalButton("component.bankmain:depositinv_toggle") { depositInvButton = !depositInvButton }
        onIfModalButton("component.bankmain:depositworn_toggle") { depositWornButton = !depositWornButton }
        onIfModalButton("component.bankmain:release_placeholders") { selectReleasePlaceholders() }
        onIfModalButton("component.bankmain:bank_filler_1") { bankFillerMode = BankFillerMode.One }
        onIfModalButton("component.bankmain:bank_filler_10") { bankFillerMode = BankFillerMode.Ten }
        onIfModalButton("component.bankmain:bank_filler_50") { bankFillerMode = BankFillerMode.Fifty }
        onIfModalButton("component.bankmain:bank_filler_x") { bankFillerMode = BankFillerMode.X }
        onIfModalButton("component.bankmain:bank_filler_all") { bankFillerMode = BankFillerMode.All }
        onIfModalButton("component.bankmain:bank_filler_confirm") { selectBankFillerFill() }
    }

    private suspend fun ProtectedAccess.selectQuantityX(op: IfButtonOp) {
        if (op == IfButtonOp.Op2) {
            lastQtyInput = countDialog("Enter amount:")
        }
        leftClickQtyMode = if (lastQtyInput == 0) QuantityMode.One else QuantityMode.X
    }

    private fun ProtectedAccess.selectTabDisplay(comsub: Int) {
        val mode =
            when (comsub) {
                1 -> TabDisplayMode.Obj
                3 -> TabDisplayMode.Digit
                5 -> TabDisplayMode.Roman
                else -> throw NotImplementedError("Unhandled tab display comsub selection: $comsub")
            }
        tabDisplayMode = mode
    }

    private fun ProtectedAccess.toggleInvItemOptions() {
        invItemOptions = !invItemOptions
        player.setBanksideExtraOps()
    }

    private suspend fun ProtectedAccess.selectReleasePlaceholders() {
        val containsPlaceholder = bank.any { it != null && getInvObj(it).isPlaceholder }
        if (containsPlaceholder) {
            bankScript.releasePlaceholders(this)
        }
    }

    private suspend fun ProtectedAccess.selectBankFillerFill() {
        val count = bankFillerMode.toCount()
        bankScript.addBankFillers(this, count)
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
