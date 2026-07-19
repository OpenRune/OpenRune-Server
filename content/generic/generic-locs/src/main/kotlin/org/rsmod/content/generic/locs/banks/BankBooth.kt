package org.rsmod.content.generic.locs.banks

import dev.openrune.types.ItemServerType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpContentLoc2
import org.rsmod.api.script.onOpContentLocU
import org.rsmod.content.interfaces.bank.confirmAndExchangeBanknote
import org.rsmod.content.interfaces.bank.tryOpenBank
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class BankBooth : PluginScript() {
    override fun ScriptContext.startup() {
        onOpContentLoc2("content.bank_booth") { tryOpenBank() }
        onOpContentLocU("content.bank_booth") { unnote(it.invSlot, it.objType) }
    }

    private suspend fun ProtectedAccess.unnote(invSlot: Int, objType: ItemServerType) {
        if (!objType.isCert) {
            mes("Nothing interesting happens.")
            return
        }
        if (inv.isFull()) {
            mes("You don't have any inventory space.")
            return
        }
        startDialogue { confirmAndExchangeBanknote(invSlot, objType) }
    }
}
