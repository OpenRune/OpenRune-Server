package org.rsmod.content.interfaces.depositbox.scripts

import jakarta.inject.Inject
import org.rsmod.api.player.ironman.IronmanRestrictions
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ui.ifOpenMainModal
import org.rsmod.api.script.onOpLocCategory1
import org.rsmod.api.script.onOpLocCategoryU
import org.rsmod.content.interfaces.bank.scripts.BankInvScript
import org.rsmod.content.interfaces.depositbox.configs.DepositBoxConstants
import org.rsmod.content.interfaces.depositbox.depositInventoryItem
import org.rsmod.content.interfaces.depositbox.opLocUDepositAll
import org.rsmod.events.EventBus
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class DepositBoxScript
@Inject
constructor(private val eventBus: EventBus, private val bankInv: BankInvScript) : PluginScript() {
    override fun ScriptContext.startup() {

        //handlers by category. This associates category 276 objects to the deposit box code so we don't have to revisit the loc.toml.
        onOpLocCategory1("category.deposit_box") { openDepositBox() }
        onOpLocCategoryU("category.deposit_box") { depositUsedItem(it.invSlot) }
    }

    private suspend fun ProtectedAccess.openDepositBox() {
        arriveDelay()
        if (IronmanRestrictions.blockUimBank(player)) {
            return
        }
        player.ifOpenMainModal(DepositBoxConstants.INTERFACE_MAIN, eventBus)
    }

    /** Code that runs when using an item on the deposit box. */
    private suspend fun ProtectedAccess.depositUsedItem(invSlot: Int) {
        arriveDelay()
        if (IronmanRestrictions.blockUimBank(player)) {
            return
        }
        if (opLocUDepositAll) {
            depositInventoryItem(bankInv, invSlot, Int.MAX_VALUE)
            return
        }
        val amount = countDialog()
        if (amount <= 0) {
            return
        }
        depositInventoryItem(bankInv, invSlot, amount)
    }
}
