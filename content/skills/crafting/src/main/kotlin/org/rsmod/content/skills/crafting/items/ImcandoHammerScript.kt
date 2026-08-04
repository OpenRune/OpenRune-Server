package org.rsmod.content.skills.crafting.items

import dev.openrune.util.Wearpos
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.worn.WornUnequipResult
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpWorn4
import org.rsmod.game.inv.Inventory
import org.rsmod.game.inv.isType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ImcandoHammerScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld3(STANDARD) { invReplace(inv, STANDARD, 1, OFFHAND) }
        onOpHeld3(OFFHAND) { invReplace(inv, OFFHAND, 1, STANDARD) }

        onOpWorn4(STANDARD) { swapWorn(it.slot, STANDARD, OFFHAND, Wearpos.LeftHand) }
        onOpWorn4(OFFHAND) { swapWorn(it.slot, OFFHAND, STANDARD, Wearpos.RightHand) }
    }

    /** Swaps a worn [fromType] for [toType], re-equipping into [targetWearpos] when it is free. */
    private fun ProtectedAccess.swapWorn(
        fromSlot: Int,
        fromType: String,
        toType: String,
        targetWearpos: Wearpos,
    ) {
        if (worn[fromSlot]?.isType(fromType) != true) {
            return
        }
        // Read before unequipping, which can push the hammer into the slot we want to fill.
        val targetFree = worn[targetWearpos.slot] == null

        val unequip = wornUnequip(fromSlot)
        if (unequip is WornUnequipResult.Fail) {
            unequip.message?.let(::mes)
            return
        }
        invReplace(inv, fromType, 1, toType)

        if (targetFree) {
            val swapped = inv.slotOf(toType)
            if (swapped != null) {
                invEquip(swapped)
            }
        }
    }

    private companion object {
        private const val STANDARD = "obj.imcando_hammer"
        private const val OFFHAND = "obj.imcando_hammer_offhand"
    }
}

/** The first inventory slot holding [type], or null when the player has none. */
private fun Inventory.slotOf(type: String): Int? = indices.firstOrNull { slot -> this[slot]?.isType(type) == true }
