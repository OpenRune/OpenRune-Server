package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.*
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Dizana's max variant uses the native knife and Mac routes; other max capes keep their owners. */
internal class QuiverMaxReturnScript : PluginScript() {
    override fun ScriptContext.startup() {
        for (symbol in QuiverForms.all.single { it.unlocked == CAPE }.symbols) {
            onOpHeldU(symbol, KNIFE) { separate(it.firstSlot) }
            onOpNpcU(ServerCacheManager.getNpc(MAC.asRSCM())!!, ServerCacheManager.getItem(symbol.asRSCM())!!) { sell(it.invSlot) }
        }
        onOpNpcU(ServerCacheManager.getNpc(MAC.asRSCM())!!, ServerCacheManager.getItem(HOOD.asRSCM())!!) { mes("Show Mac your cape to sell it back.") }
    }
    private suspend fun ProtectedAccess.separate(slot: Int) = returnCape(slot, sell = false)
    private suspend fun ProtectedAccess.sell(slot: Int) = returnCape(slot, sell = true)
    private suspend fun ProtectedAccess.returnCape(slot: Int, sell: Boolean) {
        if (!AmmoContainers.active(player)) return
        val cape = inv[slot]?.takeIf { it.count == 1 } ?: return
        val family = QuiverForms.family(cape)?.takeIf { it.unlocked == CAPE } ?: return
        if (family.locked(cape)) { mes("Ask Perdu to unlock your cape first."); return }
        if (family.broken(cape)) { mes("Repair your cape before returning it."); return }
        if (!sell && !inv.contains(KNIFE)) return
        val hoodSlot = inv.indexOfFirst { it?.id == HOOD.asRSCM() }
        val hood = if (hoodSlot >= 0) inv[hoodSlot] else null
        val origin = coords
        if (!choice2(if (sell) "Sell" else "Separate", true, "Cancel", false, title = if (sell) "Sell for 1,900,800 coins and return your quiver?" else "Separate the max cape and blessed quiver?")) return
        if (!AmmoContainers.active(player) || coords != origin || inv[slot] !== cape || hood != null && inv[hoodSlot] !== hood || !sell && !inv.contains(KNIFE)) return
        val result = player.invTransaction(inv) {
            val inventory = select(inv)
            delete(inventory, cape.id, 1, slot = slot)
            if (hood != null) delete(inventory, hood.id, 1, slot = hoodSlot)
            add(inventory, QUIVER.asRSCM(), 1)
            if (sell) add(inventory, "obj.coins".asRSCM(), REFUND)
            else { add(inventory, "obj.skillcape_max".asRSCM(), 1); if (hood != null) add(inventory, "obj.skillcape_max_hood".asRSCM(), 1) }
        }
        mes(if (result.success) if (sell) "Mac returns your blessed quiver and pays you 1,900,800 coins." else "You separate your max cape and blessed quiver." else "You need more inventory space.")
    }
    internal companion object {
        const val MAC = "npc.mac"
        const val KNIFE = "obj.knife"
        const val CAPE = "obj.skillcape_max_dizanas"
        const val HOOD = "obj.skillcape_max_hood_dizanas"
        const val QUIVER = "obj.dizanas_quiver_infinite"
        const val REFUND = 1900800
    }
}
