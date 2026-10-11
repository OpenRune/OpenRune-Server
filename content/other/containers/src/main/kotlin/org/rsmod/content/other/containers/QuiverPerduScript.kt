package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.*
import org.rsmod.api.script.advanced.onDestroyHeld
import org.rsmod.api.script.advanced.onWearposChange
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal class QuiverPerduScript @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerLogin { QuiverLocks.restore(player) }
        onWearposChange { QuiverLocks.restore(player) }
        onIfClose("interface.bankside") { QuiverLocks.restore(player) }
        onDestroyHeld { if (QuiverForms.family(obj) != null) QuiverLocks.rememberRemoved(player, obj.copy(count = 1)) }
        for (npc in NPCS) {
            onOpNpc1(npc) { mes("Use your quiver or rune pouch on me to manage its Trouver protection.") }
            for (symbol in QuiverForms.symbols) onOpNpcU(ServerCacheManager.getNpc(npc.asRSCM())!!, ServerCacheManager.getItem(symbol.asRSCM())!!) { manage(it.invSlot) }
        }
    }
    private suspend fun ProtectedAccess.manage(slot: Int) {
        if (!AmmoContainers.active(player)) return
        val original = inv[slot]?.takeIf { it.count == 1 } ?: return
        val family = QuiverForms.family(original) ?: return
        val repair = family.broken(original); val unlock = !repair && family.locked(original)
        val fee = if (repair) family.repairCost(original) else LOCK_FEE
        val origin = coords
        val prompt = when { repair -> "Repair this quiver for $fee coins?"; unlock -> "Unlock for a Trouver parchment and 475,000 coins?"; else -> "Lock for a Trouver parchment and 500,000 coins?" }
        if (!choice2(if (repair) "Repair" else if (unlock) "Unlock" else "Lock", true, "Cancel", false, title = prompt)) return
        if (!AmmoContainers.active(player) || coords != origin || inv[slot] !== original) return
        val remembered = QuiverLocks.remembered(player)
        val output = family.usable(original, locked = if (repair) family.locked(original) else !unlock)
        val cash = if (unlock) 0 else minOf(fee, inv.physicalCount("obj.coins"))
        val bankFee = if (unlock) 0 else fee - cash
        val result = player.invTransaction(inv, bank) {
            val inventory = select(inv); val stored = select(bank)
            delete(inventory, original.id, 1, slot = slot)
            if (!repair && !unlock) delete(inventory, PARCHMENT.asRSCM(), 1)
            if (cash > 0) delete(inventory, "obj.coins".asRSCM(), cash)
            // Retain a bank coin placeholder so an NPC payment does not leave a tab gap.
            if (bankFee > 0) delete(stored, "obj.coins".asRSCM(), bankFee, placehold = true)
            add(inventory, output.id, 1, output.vars, slot = slot)
            if (unlock) { add(inventory, PARCHMENT.asRSCM(), 1); add(inventory, "obj.coins".asRSCM(), UNLOCK_REFUND) }
        }
        if (result.failure) { mes(if (unlock) "You need room for the returned parchment and coins." else "You need $fee coins${if (repair) "." else " and a Trouver parchment."}"); return }
        if (!repair) QuiverLocks.change(player, remembered, family, if (unlock) -1 else 1)
        else if (remembered.values.any { it > 0 }) player.attr[QuiverLocks.KEY] = remembered
        mes(if (repair) "Perdu repairs your quiver." else if (unlock) "Perdu removes the lock and refunds your parchment and 95% of the fee." else "Perdu locks your quiver.")
    }
    companion object {
        val NPCS = listOf("npc.lost_property_merchant_model", "npc.lost_property_merchant_standard", "npc.lost_property_merchant_combatty")
        const val PARCHMENT = "obj.trouver_parchment"
        const val LOCK_FEE = 500000
        const val UNLOCK_REFUND = 475000
    }
}
