package org.rsmod.content.other.special.weapons.scripts.charge

import dev.openrune.rscm.RSCM.asRSCM
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.worn.BlowpipeCharges
import org.rsmod.api.player.worn.BlowpipeCharges.Contents
import org.rsmod.api.script.*
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class BlowpipeCharging : PluginScript() {
    override fun ScriptContext.startup() {
        for ((loaded, empty) in BlowpipeCharges.variants) {
            onOpHeld3(loaded) { inspect(it.inventory[it.slot]) }
            onOpWorn2(loaded) { inspect(player.righthand) }
            onOpHeld4(loaded) { unload(it.inventory, it.slot, false) }
            onOpHeld5(loaded) { unload(it.inventory, it.slot, true) }
            for (weapon in listOf(loaded, empty)) {
                for (resource in BlowpipeCharges.darts + "obj.snakeboss_scale") onOpHeldU(weapon, resource) {
                    if (!load(player, inv, it.firstSlot, it.secondSlot)) mes("You cannot add that ammunition. Unload other darts first, and check the capacity.")
                    else inspect(inv[it.firstSlot])
                }
            }
        }
    }
    private fun ProtectedAccess.inspect(item: InvObj?) {
        val contents = BlowpipeCharges.read(item)
        mes("Your blowpipe contains ${contents.count} darts and ${contents.scales} scales.")
    }
    private fun ProtectedAccess.unload(inventory: Inventory, slot: Int, scales: Boolean) {
        if (!unload(player, inventory, slot, scales)) mes("Not enough inventory space. Your blowpipe has not been changed.")
        else inspect(inventory[slot])
    }

    internal companion object {
        fun load(player: Player, inventory: Inventory, weaponSlot: Int, resourceSlot: Int): Boolean {
            val weapon = inventory[weaponSlot] ?: return false
            val resource = inventory[resourceSlot] ?: return false
            if (weaponSlot == resourceSlot || BlowpipeCharges.variant(weapon) == null) return false
            val current = BlowpipeCharges.read(weapon)
            val dart = BlowpipeCharges.darts.indexOfFirst { it.asRSCM() == resource.id } + 1
            val scales = resource.id == "obj.snakeboss_scale".asRSCM()
            if (!scales && (dart == 0 || current.count > 0 && dart != current.dart)) return false
            val amount = minOf(resource.count, BlowpipeCharges.MAX - if (scales) current.scales else current.count)
            if (amount <= 0) return false
            val updated = if (scales) current.copy(scales = current.scales + amount) else current.copy(dart = dart, count = current.count + amount)
            val replacement = BlowpipeCharges.write(weapon, updated)
            return player.invTransaction(inventory) {
                val inv = select(inventory)
                delete(inv, resource.id, amount, resourceSlot)
                delete(inv, weapon.id, 1, weaponSlot)
                add(inv, replacement.id, 1, replacement.vars, weaponSlot)
            }.success
        }
        fun unload(player: Player, inventory: Inventory, slot: Int, all: Boolean): Boolean {
            val weapon = inventory[slot] ?: return false
            if (BlowpipeCharges.variant(weapon) == null) return false
            val current = BlowpipeCharges.read(weapon)
            val replacement = BlowpipeCharges.write(weapon, Contents(0, 0, if (all) 0 else current.scales))
            return player.invTransaction(inventory) {
                val inv = select(inventory)
                delete(inv, weapon.id, 1, slot)
                add(inv, replacement.id, 1, replacement.vars, slot)
                if (current.count > 0) add(inv, checkNotNull(current.ammunition).id, current.count)
                if (all && current.scales > 0) add(inv, "obj.snakeboss_scale".asRSCM(), current.scales)
            }.success
        }
    }
}
