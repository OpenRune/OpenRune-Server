package org.rsmod.content.minigames.motherlodemine.scripts

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc3
import org.rsmod.content.interfaces.bank.tryOpenBank
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeConstants
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class MotherlodeBankChestScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(MotherlodeConstants.BANK_CHEST_LOC) { tryOpenBank() }
        onOpLoc3(MotherlodeConstants.BANK_CHEST_LOC) { openCollectionBox() }
    }

    private fun ProtectedAccess.openCollectionBox() {
        ifOpenMainModal("interface.ge_collect")
    }
}
