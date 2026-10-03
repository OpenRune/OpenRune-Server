package org.rsmod.content.other.special.weapons.scripts.charge

import dev.openrune.rscm.RSCM.asRSCM
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.worn.AyakCharges
import org.rsmod.api.script.*
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class AyakCharging : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld3(AyakCharges.CHARGED) { inspect(it.inventory[it.slot]) }
        onOpWorn2(AyakCharges.CHARGED) { inspect(player.righthand) }
        onOpHeld4(AyakCharges.CHARGED) { uncharge(it.inventory, it.slot) }
        onOpHeld3(AyakCharges.EMPTY) {
            val source = choice2("Runes", AyakCharges.Source.Runes, "Demon tears", AyakCharges.Source.Tears, title = "Charge with which resource?")
            charge(it.inventory, it.slot, source)
        }
        for (weapon in listOf(AyakCharges.CHARGED, AyakCharges.EMPTY)) {
            for (source in AyakCharges.Source.entries) for ((resource, _) in source.recipe) {
                onOpHeldU(weapon, resource) { charge(inv, it.firstSlot, source) }
            }
        }
    }
    private fun ProtectedAccess.inspect(item: InvObj?) {
        if (item != null && AyakCharges.accepts(item)) mes("Your Eye of Ayak has ${AyakCharges.count(item)} charges (${AyakCharges.source(item).name.lowercase()}).")
    }
    private suspend fun ProtectedAccess.charge(inventory: Inventory, slot: Int, source: AyakCharges.Source) {
        val original = inventory[slot] ?: return
        if (!AyakCharges.accepts(original)) return
        if (AyakCharges.count(original) > 0 && AyakCharges.source(original) != source) {
            mes("Uncharge your Eye of Ayak before changing between runes and demon tears.")
            return
        }
        val maximum = minOf(AyakCharges.MAX - AyakCharges.count(original), source.recipe.minOf { (symbol, cost) -> invTotal(inventory, symbol) / cost })
        if (maximum == 0) { mes("Your Eye is full or you do not have enough resources."); return }
        val amount = countDialog("How many charges? (Up to $maximum)").coerceIn(0, maximum)
        if (amount > 0 && transfer(player, inventory, slot, original, amount, source, false)) inspect(inventory[slot])
    }
    private suspend fun ProtectedAccess.uncharge(inventory: Inventory, slot: Int) {
        val original = inventory[slot] ?: return
        if (!AyakCharges.accepts(original)) return
        val count = AyakCharges.count(original)
        if (count == 0) { inventory[slot] = AyakCharges.write(original, 0); return }
        if (!choice2("Uncharge", true, "Cancel", false, title = "Recover all remaining runes or tears?")) return
        if (!transfer(player, inventory, slot, original, count, AyakCharges.source(original), true)) mes("You need more inventory space. Your Eye has not been changed.")
    }
    internal companion object {
        fun transfer(player: Player, inventory: Inventory, slot: Int, original: InvObj, amount: Int, source: AyakCharges.Source, refund: Boolean): Boolean {
            if (amount <= 0 || inventory[slot] !== original || !AyakCharges.accepts(original)) return false
            val before = AyakCharges.count(original)
            if (before > 0 && AyakCharges.source(original) != source) return false
            if (refund && amount != before || !refund && amount > AyakCharges.MAX - before) return false
            val replacement = AyakCharges.write(original, if (refund) 0 else before + amount, source)
            return player.invTransaction(inventory) {
                val inv = select(inventory)
                delete(inv, original.id, 1, slot = slot)
                add(inv, replacement.id, 1, vars = replacement.vars, slot = slot)
                for ((symbol, cost) in source.recipe) {
                    if (refund) add(inv, symbol.asRSCM(), cost * amount) else delete(inv, symbol.asRSCM(), cost * amount)
                }
            }.success
        }
    }
}
