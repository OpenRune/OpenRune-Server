package org.rsmod.content.interfaces.depositbox.scripts

import jakarta.inject.Inject
import org.rsmod.api.player.events.interact.LocUDefaultEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ui.ifOpenMainModal
import org.rsmod.api.script.onOpContentLoc1
import org.rsmod.api.script.onOpContentLocU
import org.rsmod.content.interfaces.bank.scripts.BankInvScript
import org.rsmod.content.interfaces.depositbox.configs.deposit_constants
import org.rsmod.content.interfaces.depositbox.depositInventoryItem
import org.rsmod.content.interfaces.depositbox.opLocUDepositAll
import org.rsmod.events.EventBus
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class DepositBoxScript
@Inject
constructor(private val eventBus: EventBus, private val bankInv: BankInvScript) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpContentLoc1(deposit_constants.content_group) { openDepositBox() }
        onOpContentLocU(deposit_constants.content_group) { depositUsedItem(it) }
    }

    private suspend fun ProtectedAccess.openDepositBox() {
        arriveDelay()
        player.ifOpenMainModal(deposit_constants.interface_main, eventBus)
    }

    /** Code that runs when using an item on the deposit box. */
    private suspend fun ProtectedAccess.depositUsedItem(op: LocUDefaultEvents.OpContent) {
        arriveDelay()
        if (opLocUDepositAll) {
            depositInventoryItem(bankInv, op.invSlot, Int.MAX_VALUE)
            return
        }
        val amount = countDialog()
        if (amount <= 0) {
            return
        }
        depositInventoryItem(bankInv, op.invSlot, amount)
    }
}
