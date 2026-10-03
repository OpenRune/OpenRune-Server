package org.rsmod.content.other.special.weapons.scripts.charge

import dev.openrune.rscm.RSCM.asRSCM
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
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

class TridentCharging : PluginScript() {
    override fun ScriptContext.startup() {
        for (kind in TridentCharges.kinds) for (symbol in kind.symbols) {
            onOpHeld3(symbol) { inspect(it.inventory[it.slot]) }
            onOpWorn2(symbol) { inspect(player.righthand) }
            if (symbol != kind.empty) onOpHeld4(symbol) { uncharge(it.inventory, it.slot) }
            for ((resource, _) in kind.recipe) onOpHeldU(symbol, resource) {
                charge(inv, it.firstSlot)
            }
        }
    }

    private fun ProtectedAccess.inspect(obj: InvObj?) {
        if (obj != null) mes("Your trident has ${TridentCharges.count(obj)} charges remaining.")
    }

    private suspend fun ProtectedAccess.charge(inventory: Inventory, slot: Int) {
        val original = inventory[slot] ?: return
        val kind = TridentCharges.kind(original) ?: return
        val capacity = kind.max - TridentCharges.count(original)
        val available = kind.recipe.minOf { (symbol, cost) -> invTotal(inventory, symbol) / cost }.coerceAtMost(capacity)
        if (available <= 0) {
            mes(if (capacity == 0) "Your trident is fully charged." else "You do not have the required resources to charge your trident.")
            return
        }
        val requested = countDialog("How many charges would you like to add? (Up to $available)").coerceIn(0, available)
        if (requested == 0 || inventory[slot] !== original) return
        if (transfer(player, inventory, slot, original, requested, false)) inspect(inventory[slot])
        else mes("Unable to charge your trident. No items have been changed.")
    }

    private suspend fun ProtectedAccess.uncharge(inventory: Inventory, slot: Int) {
        val original = inventory[slot] ?: return
        if (TridentCharges.count(original) == 0) { inventory[slot] = TridentCharges.withCharges(original, 0); return }
        if (!choice2("Uncharge.", true, "Cancel.", false, title = "Recover the remaining runes and scales? Coins are not refunded.")) return
        if (inventory[slot] !== original) return
        if (!transfer(player, inventory, slot, original, TridentCharges.count(original), true)) {
            mes("You need more inventory space. Your trident has not been changed.")
        }
    }

    internal companion object {
        fun transfer(player: Player, inventory: Inventory, slot: Int, original: InvObj, amount: Int, refund: Boolean): Boolean {
            if (amount <= 0 || inventory[slot] !== original) return false
            val kind = TridentCharges.kind(original) ?: return false
            val before = TridentCharges.count(original)
            if (refund && amount != before || !refund && amount > kind.max - before) return false
            val replacement = TridentCharges.withCharges(original, if (refund) 0 else before + amount)
            return player.invTransaction(inventory) {
                val target = select(inventory)
                delete(target, original.id, 1, slot = slot)
                add(target, replacement.id, 1, vars = replacement.vars, slot = slot)
                for ((resource, cost) in if (refund) kind.refund else kind.recipe) {
                    if (refund) add(target, resource.asRSCM(), amount * cost)
                    else delete(target, resource.asRSCM(), amount * cost)
                }
            }.success
        }
    }
}
