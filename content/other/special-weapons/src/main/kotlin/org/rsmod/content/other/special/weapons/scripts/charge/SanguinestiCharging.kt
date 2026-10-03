package org.rsmod.content.other.special.weapons.scripts.charge

import dev.openrune.rscm.RSCM.asRSCM
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.script.*
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SanguinestiCharging : PluginScript() {
    override fun ScriptContext.startup() {
        for ((charged, empty) in SanguinestiCharges.variants) {
            onOpHeld3(charged) { inspect(it.inventory[it.slot]) }
            onOpWorn2(charged) { inspect(player.righthand) }
            onOpHeld4(charged) { charge(it.inventory, it.slot) }
            onOpHeld5(charged) { uncharge(it.inventory, it.slot) }
            onOpHeld3(empty) { charge(it.inventory, it.slot) }
            for (weapon in listOf(charged, empty)) onOpHeldU(weapon, "obj.bloodrune") { charge(inv, it.firstSlot) }
        }
    }
    private fun ProtectedAccess.inspect(item: InvObj?) {
        if (item != null && SanguinestiCharges.variant(item) != null) mes("Your Sanguinesti staff has ${SanguinestiCharges.count(item)} charges remaining.")
    }
    private suspend fun ProtectedAccess.charge(inventory: Inventory, slot: Int) {
        val original = inventory[slot] ?: return
        val maximum = minOf(SanguinestiCharges.MAX - SanguinestiCharges.count(original), invTotal(inventory, "obj.bloodrune") / SanguinestiCharges.BLOOD_RUNES)
        if (maximum == 0) { mes("Your staff is full or you do not have enough blood runes."); return }
        val amount = countDialog("How many charges? (Up to $maximum)").coerceIn(0, maximum)
        if (amount > 0 && transfer(player, inventory, slot, original, amount, false)) inspect(inventory[slot])
    }
    private suspend fun ProtectedAccess.uncharge(inventory: Inventory, slot: Int) {
        val original = inventory[slot] ?: return
        val count = SanguinestiCharges.count(original)
        if (count == 0) { inventory[slot] = SanguinestiCharges.write(original, 0); return }
        if (!choice2("Uncharge", true, "Cancel", false, title = "Recover all remaining blood runes?")) return
        if (!transfer(player, inventory, slot, original, count, true)) mes("You need more inventory space. Your staff has not been changed.")
    }
    internal companion object {
        fun transfer(player: Player, inventory: Inventory, slot: Int, original: InvObj, amount: Int, refund: Boolean): Boolean {
            if (amount <= 0 || inventory[slot] !== original || SanguinestiCharges.variant(original) == null) return false
            val before = SanguinestiCharges.count(original)
            if (refund && amount != before || !refund && amount > SanguinestiCharges.MAX - before) return false
            val replacement = SanguinestiCharges.write(original, if (refund) 0 else before + amount)
            return player.invTransaction(inventory) {
                val inv = select(inventory)
                delete(inv, original.id, 1, slot = slot)
                add(inv, replacement.id, 1, vars = replacement.vars, slot = slot)
                if (refund) add(inv, "obj.bloodrune".asRSCM(), amount * SanguinestiCharges.BLOOD_RUNES)
                else delete(inv, "obj.bloodrune".asRSCM(), amount * SanguinestiCharges.BLOOD_RUNES)
            }.success
        }
    }
}
