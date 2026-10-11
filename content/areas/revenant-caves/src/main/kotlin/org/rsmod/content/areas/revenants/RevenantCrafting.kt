package org.rsmod.content.areas.revenants

import dev.openrune.rscm.RSCM.asRSCM
import org.rsmod.api.invtx.*
import org.rsmod.api.player.stat.stat
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory

internal object RevenantCrafting {
    data class Upgrade(val base: String, val upgraded: String, val component: String, val skill: String)
    val upgrades = listOf(
        Upgrade("bow", "webweaver", "Fangs of Venenatis", "stat.fletching"),
        Upgrade("chainmace", "ursine", "Claws of Callisto", "stat.smithing"),
        Upgrade("sceptre", "accursed", "Skull of Vet'ion", "stat.crafting"),
    )
    fun upgradeFor(kind: RevenantCharges.Kind): Upgrade? = upgrades.firstOrNull { kind.empty == "obj.wild_cave_${it.upgraded}_uncharged" || kind.empty == "obj.wild_cave_${it.upgraded}_uncharged_recol" }
    fun combine(player: Player, slot: Int, item: InvObj, upgrade: Upgrade): Boolean {
        if (player.inv[slot] !== item || player.stat(upgrade.skill) < 85) return false
        val suffix = if (RevenantCharges.kind(item)?.empty?.endsWith("_recol") == true) "_recol" else ""
        val source = "obj.wild_cave_${upgrade.base}_uncharged$suffix"
        val kind = RevenantCharges.kind(item) ?: return false
        if (kind.empty != source) return false
        val output = RevenantCharges.withCount(InvObj("obj.wild_cave_${upgrade.upgraded}_uncharged$suffix", vars = item.vars), RevenantCharges.count(item))
        return player.invTransaction(player.inv) {
            val bag = select(player.inv)
            delete(bag, item.id, 1, slot = slot)
            delete(bag, RevenantLootTable.lookup(upgrade.component).asRSCM(), 1)
            add(bag, output.id, 1, vars = output.vars, slot = slot)
        }.success
    }
    fun dismantle(player: Player, inventory: Inventory, slot: Int, item: InvObj, kind: RevenantCharges.Kind): Boolean {
        if (inventory[slot] !== item || item.id != kind.empty.asRSCM() || RevenantCharges.count(item) != 0) return false
        val upgrade = upgradeFor(kind)
        return player.invTransaction(inventory, player.inv) {
            val bag = select(inventory); val output = if (inventory === player.inv) bag else select(player.inv)
            delete(bag, item.id, 1, slot = slot)
            if (upgrade == null) add(output, RevenantCharges.ETHER.asRSCM(), kind.dismantle)
            else {
                val suffix = if (kind.empty.endsWith("_recol")) "_recol" else ""
                add(output, "obj.wild_cave_${upgrade.base}_uncharged$suffix".asRSCM(), 1)
                add(output, RevenantLootTable.lookup(upgrade.component).asRSCM(), 1)
            }
        }.success
    }
}
