package org.rsmod.content.other.pouches

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.script.*
import org.rsmod.content.other.pouches.lootingbag.LootingBags
import org.rsmod.content.other.pouches.runepouch.RunePouchLocks
import org.rsmod.content.other.pouches.runepouch.RunePouches
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal class PouchNoteScript @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        for ((note, output) in NOTES) {
            onOpHeld4(note) { mes("Use this note on a banker, bank booth or bank chest to exchange it for a ${if (output == RunePouches.REGULAR) "rune pouch" else "looting bag"}.") }
            for (content in listOf("content.banker", "content.banker_tutor")) onOpContentNpcU(content, note) { redeem(it.invSlot, note, output) }
            onOpContentMixedLocU("content.bank_booth", note) { redeem(it.invSlot, note, output) }
            val type = ServerCacheManager.getItem(note.asRSCM())!!
            for (chest in ServerCacheManager.getObjects().values.filter { it.name.equals("Bank chest", true) }) onOpLocU(chest, type) { redeem(it.invSlot, note, output) }
            if (output == RunePouches.REGULAR) for (clerk in ServerCacheManager.getNpcs().values.filter { it.name.equals("Grand Exchange Clerk", true) }) onOpNpcU(clerk, type) { redeem(it.invSlot, note, output) }
        }
    }
    private fun ProtectedAccess.redeem(slot: Int, note: String, output: String) {
        if (player.hitpoints <= 0 || player.pendingLogout || player.loggingOut) return
        val original = inv[slot]?.takeIf { it.id == note.asRSCM() && it.count > 0 } ?: return
        val ids = if (output == RunePouches.REGULAR) RunePouches.ids else LootingBags.ids
        if ((inv.filterNotNull() + bank.filterNotNull()).any { it.id in ids }) { mes("You already have one of those."); return }
        val tx = player.invTransaction(inv) { val i = select(inv); delete(i, original.id, 1, slot = slot); add(i, output.asRSCM(), 1) }
        if (tx.failure) { mes("You need room for the container."); return }
        if (output == RunePouches.REGULAR) RunePouchLocks.restore(player)
        mes("You exchange one note for a ${if (output == RunePouches.REGULAR) "rune pouch" else "looting bag"}.")
    }
    companion object { val NOTES = listOf("obj.promissory_note_runepouch" to RunePouches.REGULAR, "obj.promissory_note_lootingbag" to LootingBags.CLOSED) }
}
