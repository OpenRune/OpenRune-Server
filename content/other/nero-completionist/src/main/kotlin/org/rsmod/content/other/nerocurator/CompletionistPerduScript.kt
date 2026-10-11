package org.rsmod.content.other.nerocurator

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.script.*
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal class CompletionistPerduScript @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        for (npc in NPCS) for (family in CompletionistProtection.all) for (symbol in family.symbols) onOpNpcU(ServerCacheManager.getNpc(npc.asRSCM())!!, ServerCacheManager.getItem(symbol.asRSCM())!!) { manage(it.invSlot) }
    }
    private suspend fun ProtectedAccess.manage(slot: Int) {
        if (!active()) return
        val original = inv[slot]?.takeIf { it.count == 1 } ?: return
        val family = CompletionistProtection.family(original) ?: return
        val repair = family.broken(original)
        val unlock = !repair && family.locked(original)
        val fee = if (repair) family.fee(original) else 500000
        val origin = coords
        val prompt = when { repair -> "Repair for $fee coins?"; unlock -> "Unlock for a Trouver parchment and 475,000 coins?"; else -> "Lock for a Trouver parchment and 500,000 coins?" }
        if (!choice2(if (repair) "Repair" else if (unlock) "Unlock" else "Lock", true, "Cancel", false, title = prompt)) return
        if (!active() || coords != origin || inv[slot] !== original) return
        val output = family.usable(original, if (repair) family.locked(original) else !unlock)
        val cash = if (unlock) 0 else minOf(fee, inv.physicalCount("obj.coins"))
        val bankFee = if (unlock) 0 else fee - cash
        val result = player.invTransaction(inv, bank) {
            val target = select(inv); val stored = select(bank)
            delete(target, original.id, 1, slot = slot)
            if (!repair && !unlock) delete(target, "obj.trouver_parchment".asRSCM(), 1)
            if (cash > 0) delete(target, "obj.coins".asRSCM(), cash)
            if (bankFee > 0) delete(stored, "obj.coins".asRSCM(), bankFee, placehold = true)
            add(target, output.id, 1, output.vars, slot = slot)
            if (unlock) { add(target, "obj.trouver_parchment".asRSCM(), 1); add(target, "obj.coins".asRSCM(), 475000) }
        }
        mes(if (result.failure) "You need ${if (unlock) "room for the returned parchment and coins" else "$fee coins${if (repair) "" else " and a Trouver parchment"}"}." else if (repair) "Perdu repairs your cape." else if (unlock) "Perdu removes your cape's lock." else "Perdu locks your cape.")
    }
    private fun ProtectedAccess.active() = player.hitpoints > 0 && !player.pendingLogout && !player.loggingOut
    companion object { val NPCS = listOf("npc.lost_property_merchant_model", "npc.lost_property_merchant_standard", "npc.lost_property_merchant_combatty") }
}
