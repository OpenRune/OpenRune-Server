package org.rsmod.content.other.lootingbag

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Check, Deposit, Settings, using objs on the bag in the Wilderness, banking its
// contents and dropping them on death.
class LootingBagScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld1(LootingBag.BAG) { swap(it.slot, LootingBag.OPEN_BAG, OPENED) }
        onOpHeld1(LootingBag.OPEN_BAG) { swap(it.slot, LootingBag.BAG, CLOSED) }
    }

    private fun ProtectedAccess.swap(slot: Int, into: String, message: String) {
        val type = checkNotNull(ServerCacheManager.getItem(into.asRSCM(RSCMType.OBJ)))
        if (invReplaceSlot(inv, slot, count = 1, type).failure) {
            return
        }
        mes(message)
    }

    private companion object {
        const val OPENED = "You open your looting bag, ready to fill it."

        // UNVERIFIED: wording guessed.
        const val CLOSED = "You close your looting bag."
    }
}
