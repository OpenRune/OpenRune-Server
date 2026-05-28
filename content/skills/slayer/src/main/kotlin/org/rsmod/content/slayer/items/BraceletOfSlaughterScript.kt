package org.rsmod.content.slayer.items

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpWorn1
import org.rsmod.content.slayer.items.BraceletOfSlaughter.ITEM
import org.rsmod.game.inv.isType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class BraceletOfSlaughterScript : PluginScript() {

    override fun ScriptContext.startup() {
        onOpHeld2(ITEM) { checkCharges(it.slot) }
        onOpHeld3(ITEM) { breakBracelet(it.slot) }
        onOpWorn1(ITEM) { checkChargesWorn() }
    }

    private fun ProtectedAccess.checkCharges(slot: Int) {
        inv[slot]?.takeIf { it.isType(ITEM) } ?: return
        BraceletOfSlaughter.checkCharges(player)
    }

    private fun ProtectedAccess.checkChargesWorn() {
        if (!BraceletOfSlaughter.isWearing(player)) {
            return
        }
        BraceletOfSlaughter.checkCharges(player)
    }

    private suspend fun ProtectedAccess.breakBracelet(slot: Int) {
        inv[slot]?.takeIf { it.isType(ITEM) } ?: return
        val confirmed = choice2(
            choice1 = "Break the bracelet of slaughter?",
            result1 = true,
            choice2 = "Cancel.",
            result2 = false,
            title = "This will destroy the bracelet."
        )
        if (!confirmed) {
            return
        }
        if (invDel(inv, ITEM, 1, slot = slot).failure) {
            return
        }
        BraceletOfSlaughter.prepareBreak(player)
        mes("You destroy the bracelet. Your next bracelet of slaughter will have " + "${BraceletOfSlaughter.MAX_CHARGES} charges.",)
    }
}
