package org.rsmod.content.other.special.weapons.scripts.charge

import dev.openrune.rscm.RSCM.asRSCM
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpWorn2
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ScytheCharging : PluginScript() {
    override fun ScriptContext.startup() {
        for ((charged, empty) in ScytheCharges.variants) {
            onOpHeld3(charged) { inspect(it.inventory[it.slot]) }
            onOpWorn2(charged) { inspect(player.righthand) }
            onOpHeld4(charged) { uncharge(it.inventory, it.slot) }
            onOpHeld4(empty) { charge(it.inventory, it.slot) }
            for (weapon in listOf(charged, empty)) for (resource in listOf("obj.bloodrune", "obj.vial_blood")) {
                onOpHeldU(weapon, resource) { charge(inv, it.firstSlot) }
            }
        }
    }

    private fun ProtectedAccess.inspect(item: InvObj?) {
        if (item != null) mes("Your scythe has ${ScytheCharges.count(item)} charges remaining.")
    }

    private suspend fun ProtectedAccess.charge(inventory: Inventory, slot: Int) {
        val original = inventory[slot] ?: return
        val available = ScytheCharges.batches(ScytheCharges.count(original), invTotal(inventory, "obj.bloodrune"), invTotal(inventory, "obj.vial_blood"))
        if (available == 0) {
            mes("Each 100 charges requires 200 blood runes, one vial of blood and room for 100 charges.")
            return
        }
        val requested = countDialog("How many sets of 100 charges? (Up to $available)").coerceIn(0, available)
        if (requested == 0) return
        if (recharge(player, inventory, slot, original, requested)) inspect(inventory[slot])
        else mes("Unable to recharge your scythe. No items have been changed.")
    }

    private suspend fun ProtectedAccess.uncharge(inventory: Inventory, slot: Int) {
        val original = inventory[slot] ?: return
        if (ScytheCharges.count(original) == 0) { inventory[slot] = ScytheCharges.withCharges(original, 0); return }
        if (!choice2("Remove charges.", true, "Cancel.", false, title = "Remove all charges? Blood runes and vials will be lost.")) return
        if (inventory[slot] === original) inventory[slot] = ScytheCharges.withCharges(original, 0)
    }

    internal companion object {
        fun recharge(player: Player, inventory: Inventory, slot: Int, original: InvObj, batches: Int): Boolean {
            if (inventory[slot] !== original || batches <= 0 || ScytheCharges.variant(original) == null) return false
            val before = ScytheCharges.count(original)
            if (batches > (ScytheCharges.MAX - before) / ScytheCharges.BATCH) return false
            val replacement = ScytheCharges.withCharges(original, before + batches * ScytheCharges.BATCH)
            return player.invTransaction(inventory) {
                val target = select(inventory)
                delete(target, original.id, 1, slot)
                add(target, replacement.id, 1, vars = replacement.vars, slot = slot)
                delete(target, "obj.bloodrune".asRSCM(), batches * ScytheCharges.BLOOD_RUNES)
                delete(target, "obj.vial_blood".asRSCM(), batches)
            }.success
        }
    }
}
