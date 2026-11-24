package org.rsmod.content.generic.locs.banks

import dev.openrune.types.ItemServerType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpContentLoc1
import org.rsmod.api.script.onOpContentLoc2
import org.rsmod.api.script.onOpContentLoc3
import org.rsmod.api.script.onOpContentLocU
import org.rsmod.content.interfaces.bank.confirmAndExchangeBanknote
import org.rsmod.content.interfaces.bank.openCollectionBox
import org.rsmod.content.interfaces.bank.tryOpenBank
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class BankLocs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpContentLoc1("content.bank_booth") { tryOpenBank() }
        onOpContentLoc2("content.bank_booth") { tryOpenBank() }
        onOpContentLoc3("content.bank_booth") { openCollectionBox() }
        onOpContentLocU("content.bank_booth") { unnote(it.invSlot, it.objType) }

        onOpContentLoc1("content.bank_chest") { tryOpenBank() }
        onOpContentLoc3("content.bank_chest") { openCollectionBox() }
        onOpContentLocU("content.bank_chest") { unnote(it.invSlot, it.objType) }
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
