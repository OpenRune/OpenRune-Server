package org.rsmod.content.skills.crafting

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CraftingGuildBank : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.diary_guild_bankchest") { useBankChest() }
        onOpLoc1("loc.diary_guild_deposit_box") { useDepositBox() }
    }

    private suspend fun ProtectedAccess.useBankChest() {
        arriveDelay()
        if (!requirementMet()) {
            return
        }
        ifOpenMainSidePair(main = "interface.bankmain", side = "interface.bankside")
    }

    private suspend fun ProtectedAccess.useDepositBox() {
        arriveDelay()
        if (!requirementMet()) {
            return
        }
        ifOpenMainModal("interface.bank_depositbox")
    }

    private fun ProtectedAccess.requirementMet(): Boolean {
        if (player.canUseGuildBank()) {
            return true
        }
        mes("Only master crafters or those who have completed the hard or elite tier of the Falador Diary may use this.")
        return false
    }
}
