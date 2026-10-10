package org.rsmod.content.areas.revenants

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.util.Wearpos
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.script.*
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal class RevenantItemsScript : PluginScript() {
    override fun ScriptContext.startup() {
        for (upgrade in RevenantCrafting.upgrades) for (suffix in if (upgrade.base == "sceptre") listOf("", "_recol") else listOf("")) {
            for (charge in listOf("uncharged", "charged")) {
                // The native Crafting hand table already owns this exact uncharged pair.
                if (upgrade.base == "sceptre" && suffix.isEmpty() && charge == "uncharged") continue
                onOpHeldU("obj.wild_cave_${upgrade.base}_$charge$suffix", RevenantLootTable.lookup(upgrade.component)) {
                    val item = inv[it.firstSlot] ?: return@onOpHeldU
                    if (player.stat(upgrade.skill) < 85) mes("You need level 85 ${upgrade.skill.removePrefix("stat.")} to combine these items.")
                    else if (!RevenantCrafting.combine(player, it.firstSlot, item, upgrade)) mes("The items changed or there is not enough inventory space.")
                }
            }
        }
        for (kind in RevenantCharges.kinds) {
            for (item in listOf(kind.empty, kind.charged)) onOpHeldU(item, RevenantCharges.ETHER) { charge(inv, it.firstSlot) }
            onOpHeld3(kind.charged) { check(it.obj) }
            onOpHeld5(kind.charged) { uncharge(it.inventory, it.slot, it.obj) }
            if (kind == RevenantCharges.bracelet) onOpHeld4(kind.empty) { dismantle(it.inventory, it.slot, it.obj, kind) }
            else onOpHeld3(kind.empty) { dismantle(it.inventory, it.slot, it.obj, kind) }
            onOpWorn2(kind.charged) { check(player.worn[if (kind == RevenantCharges.bracelet) Wearpos.Hands.slot else Wearpos.RightHand.slot]) }
        }
        onOpHeld3(RevenantCharges.EMPTY_BRACELET) { it.inventory[it.slot] = RevenantCharges.toggle(it.obj); check(it.inventory[it.slot]) }
        onOpHeld4(RevenantCharges.BRACELET) { it.inventory[it.slot] = RevenantCharges.toggle(it.obj); check(it.inventory[it.slot]) }
        onOpWorn2(RevenantCharges.EMPTY_BRACELET) { check(player.worn[Wearpos.Hands.slot]) }
        onOpWorn3(RevenantCharges.EMPTY_BRACELET) {
            val slot = Wearpos.Hands.slot; player.worn[slot]?.let { player.worn[slot] = RevenantCharges.toggle(it) }; check(player.worn[slot])
        }
        onOpWorn3(RevenantCharges.BRACELET) {
            val slot = Wearpos.Hands.slot; player.worn[slot]?.let { player.worn[slot] = RevenantCharges.toggle(it) }; check(player.worn[slot])
        }
        for (artefact in AncientArtefact.entries) {
            onOpHeldU(artefact.item, "obj.chisel") { breakArtefact(it.firstSlot, artefact) }
        }
    }
    private fun ProtectedAccess.check(item: InvObj?) {
        if (item == null) return
        val kind = RevenantCharges.kind(item) ?: return
        val usable = (RevenantCharges.count(item) - kind.deposit).coerceAtLeast(0)
        mes("${usable} usable ether charges${if (kind == RevenantCharges.bracelet) "; absorption ${if (RevenantCharges.absorbing(item)) "on" else "off"}" else " (1,000 activation ether reserved)"}.")
    }
    private suspend fun ProtectedAccess.charge(inventory: Inventory, slot: Int) {
        val item = inventory[slot] ?: return; val kind = RevenantCharges.kind(item) ?: return
        val maximum = minOf(kind.max - RevenantCharges.count(item), invTotal(inv, RevenantCharges.ETHER))
        if (maximum <= 0) { mes("You need revenant ether and room for more charges."); return }
        val amount = countDialog("How much revenant ether? (Up to $maximum)").coerceIn(0, maximum)
        if (RevenantCharges.transfer(player, inventory, slot, item, amount, false)) check(inventory[slot])
    }
    private suspend fun ProtectedAccess.uncharge(inventory: Inventory, slot: Int, item: InvObj) {
        val count = RevenantCharges.count(item); if (count == 0) return
        if (!choice2("Uncharge", true, "Cancel", false, title = "Return all ether to your inventory?")) return
        if (!RevenantCharges.transfer(player, inventory, slot, item, count, true)) mes("The item changed or there is not enough inventory space.")
    }
    private suspend fun ProtectedAccess.dismantle(inventory: Inventory, slot: Int, item: InvObj, kind: RevenantCharges.Kind) {
        if (item.id != kind.empty.asRSCM() || RevenantCharges.count(item) != 0) return
        val upgrade = RevenantCrafting.upgradeFor(kind)
        if (!choice2("Dismantle", true, "Cancel", false, title = if (upgrade == null) "Destroy this item for ${kind.dismantle} ether?" else "Return the original weapon and ${upgrade.component}?")) return
        if (inventory[slot] !== item) return
        if (!RevenantCrafting.dismantle(player, inventory, slot, item, kind)) mes("You don't have enough inventory space.")
    }
    private suspend fun ProtectedAccess.breakArtefact(slot: Int, artefact: AncientArtefact) {
        val item = inv[slot] ?: return
        if (!choice2("Break it down", true, "Cancel", false, title = "Destroy this artefact for ${artefact.ether} ether?")) return
        if (inv[slot] !== item || invTotal(inv, "obj.chisel") == 0) return
        val result = player.invTransaction(inv) { val bag = select(inv); delete(bag, artefact.item.asRSCM(), 1, slot = slot); add(bag, RevenantCharges.ETHER.asRSCM(), artefact.ether) }
        if (!result.success) mes("You don't have enough inventory space.")
    }
}

internal enum class AncientArtefact(val thousands: Int) {
    Emblem(500), Totem(1000), Statuette(2000), Medallion(4000), Effigy(8000), Relic(16000);
    val item get() = "obj.wild_cave_artifact_$thousands"
    val coins get() = thousands * 1000
    val ether get() = thousands * 2
}
