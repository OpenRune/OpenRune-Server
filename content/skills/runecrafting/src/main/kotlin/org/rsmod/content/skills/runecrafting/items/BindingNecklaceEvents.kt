package org.rsmod.content.skills.runecrafting.items

import dev.openrune.util.Wearpos
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.skills.runecrafting.bindingNecklaceCharges
import org.rsmod.api.script.advanced.onWearposChange
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeld3
import org.rsmod.game.inv.isType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class BindingNecklaceEvents : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld2(BindingNecklace.ITEM) {
            checkCharges()
        }

        onOpHeld3(BindingNecklace.ITEM) {
            destroyForReset(it.slot)
        }

        onWearposChange {
            if (wearpos != Wearpos.Front) {
                return@onWearposChange
            }

            val equipped = player.worn[wearpos.slot] ?: return@onWearposChange
            if (!equipped.isType(BindingNecklace.ITEM)) {
                return@onWearposChange
            }

            if (player.bindingNecklaceCharges <= 0) {
                player.bindingNecklaceCharges = BindingNecklace.MAX_CHARGES
            }
        }
    }

    private fun ProtectedAccess.checkCharges() {
        val charges = player.bindingNecklaceCharges.coerceAtLeast(0)
        mes("You have $charges uses left before your necklace disintegrates.")
    }

    private fun ProtectedAccess.destroyForReset(slot: Int) {
        if (inv[slot]?.isType(BindingNecklace.ITEM) != true) {
            return
        }

        if (invDel(inv, BindingNecklace.ITEM, 1, slot = slot).success) {
            player.bindingNecklaceCharges = BindingNecklace.MAX_CHARGES
        }
    }
}
