package org.rsmod.content.slayer.items

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpWorn1
import org.rsmod.content.slayer.items.ExpeditiousBracelet.ITEM
import org.rsmod.game.inv.isType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ExpeditiousBraceletScript : PluginScript() {

    override fun ScriptContext.startup() {
        onOpHeld2(ITEM) { checkCharges(it.slot) }
        onOpHeld3(ITEM) { breakBracelet(it.slot) }
        onOpWorn1(ITEM) { checkChargesWorn() }
    }

    private fun ProtectedAccess.checkCharges(slot: Int) {
        inv[slot]?.takeIf { it.isType(ITEM) } ?: return
        ExpeditiousBracelet.checkCharges(player)
    }

    private fun ProtectedAccess.checkChargesWorn() {
        if (!ExpeditiousBracelet.isWearing(player)) {
            return
        }
        ExpeditiousBracelet.checkCharges(player)
    }

    private suspend fun ProtectedAccess.breakBracelet(slot: Int) {
        inv[slot]?.takeIf { it.isType(ITEM) } ?: return
        val confirmed = choice2(
            choice1 = "Break the expeditious bracelet?",
            result1 = true,
            choice2 = "Cancel.",
            result2 = false,
            title = "This will destroy the bracelet.",
        )
        if (!confirmed) {
            return
        }
        if (invDel(inv, ITEM, 1, slot = slot).failure) {
            return
        }
        ExpeditiousBracelet.prepareBreak(player)
        mes("You destroy the bracelet. Your next expeditious bracelet will have " + "${ExpeditiousBracelet.MAX_CHARGES} charges.")
    }
}
