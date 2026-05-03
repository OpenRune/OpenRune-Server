package org.rsmod.content.generic.locs.banks

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpContentLoc2
import org.rsmod.api.script.onOpLoc2
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class BankBooth : PluginScript() {
    override fun ScriptContext.startup() {
        onOpContentLoc2("content.bank_booth") { openBank() }
    }

    private fun ProtectedAccess.openBank() {
        ifOpenMainSidePair(main = "interface.bankmain", side = "interface.bankside")
    }
}
