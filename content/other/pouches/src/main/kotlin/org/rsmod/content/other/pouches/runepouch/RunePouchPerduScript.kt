package org.rsmod.content.other.pouches.runepouch

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.script.*
import org.rsmod.api.script.advanced.onDestroyHeld
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal class RunePouchPerduScript @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerLogin { RunePouchLocks.restore(player) }
        onIfClose("interface.bankside") { RunePouchLocks.restore(player) }
        onDestroyHeld { RunePouchLocks.rememberRemoved(player, obj) }
        for (npc in NPCS) for (pouch in RunePouches.ALL) onOpNpcU(ServerCacheManager.getNpc(npc.asRSCM())!!, ServerCacheManager.getItem(pouch.asRSCM())!!) { manage(it.invSlot) }
    }
    private suspend fun ProtectedAccess.manage(slot: Int) {
        if (!active()) return
        val original = inv[slot]?.takeIf { it.count == 1 && it.id in RunePouches.ids } ?: return
        val unlocked = RunePouches.unlocked(original.id) ?: return
        val unlock = original.id in RunePouches.lockedIds
        val output = if (unlock) unlocked else RunePouches.locked(unlocked)!!
        val origin = coords
        if (!choice2(if (unlock) "Unlock" else "Lock", true, "Cancel", false, title = if (unlock) "Return a Trouver parchment and 475,000 coins?" else "Use a Trouver parchment and 500,000 coins?")) return
        if (!active() || coords != origin || inv[slot] !== original) return
        val remembered = RunePouchLocks.remembered(player)
        val cash = if (unlock) 0 else minOf(LOCK_FEE, inv.physicalCount("obj.coins"))
        val bankFee = if (unlock) 0 else LOCK_FEE - cash
        val tx = player.invTransaction(inv, bank) {
            val i = select(inv); val b = select(bank)
            delete(i, original.id, 1, slot = slot)
            if (!unlock) delete(i, PARCHMENT.asRSCM(), 1)
            if (cash > 0) delete(i, "obj.coins".asRSCM(), cash)
            if (bankFee > 0) delete(b, "obj.coins".asRSCM(), bankFee, placehold = true)
            add(i, output.asRSCM(), 1, original.vars, slot = slot)
            if (unlock) { add(i, PARCHMENT.asRSCM(), 1); add(i, "obj.coins".asRSCM(), REFUND) }
        }
        if (tx.failure) { mes(if (unlock) "You need room for the parchment and coins." else "You need a Trouver parchment and 500,000 coins."); return }
        RunePouchLocks.change(player, remembered, unlocked, if (unlock) -1 else 1)
        mes(if (unlock) "Perdu removes the lock from your rune pouch." else "Perdu locks your rune pouch. Runes remain unprotected in PvP.")
    }
    private fun ProtectedAccess.active() = player.hitpoints > 0 && !player.pendingLogout && !player.loggingOut
    companion object {
        val NPCS = listOf("npc.lost_property_merchant_model", "npc.lost_property_merchant_standard", "npc.lost_property_merchant_combatty")
        const val PARCHMENT = "obj.trouver_parchment"
        const val LOCK_FEE = 500000
        const val REFUND = 475000
    }
}
