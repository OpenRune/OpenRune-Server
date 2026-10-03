package org.rsmod.content.other.commands

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.worn.DragonfireShields
import org.rsmod.api.specials.SpecialAttackRegistry
import org.rsmod.api.specials.weapon.SpecialAttackWeapons
import org.rsmod.api.weapons.WeaponRegistry
import org.rsmod.game.inv.InvObj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class WeaponTestCommand @Inject constructor(
    private val specials: SpecialAttackRegistry,
    private val weapons: WeaponRegistry,
    private val specialWeapons: SpecialAttackWeapons,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("weptest", "Spawn implemented weapons and shields: ::weptest <set>", {
            val sets = items().chunked(20)
            val number = args.singleOrNull()?.toIntOrNull()
            if (number == null || number !in 1..sets.size) {
                player.mes("Usage: ::weptest <1-${sets.size}>. ${sets.sumOf { it.size }} weapons/shields, up to 20 per set.")
                return@onCommand
            }
            val items = sets[number - 1]
            val result = player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                for (item in items) add(inventory, item.id, item.count, vars = item.vars, strict = true)
            }
            if (result.success) {
                player.mes("Weapon test set $number/${sets.size}: ${items.size} items added to inventory.")
                player.mes("Use ::spres for special energy. Charge weapons and supply ammunition normally.")
            } else {
                player.mes("Not enough inventory space for set $number. Nothing was added or removed.")
            }
        })
    }

    internal fun items(): List<InvObj> = ServerCacheManager.getItems().values
        .asSequence()
        .filter { it.wearpos1 == Wearpos.RightHand.slot || it.wearpos1 == Wearpos.LeftHand.slot }
        .sortedBy { it.id }
        .map { InvObj(it, if (it.stackable) 1000 else 1) }
        .filter { item ->
            specials[item] != null || (specialWeapons.getSpecialEnergy(item.id) == null &&
                (weapons.getMelee(item) != null || weapons.getRanged(item) != null || weapons.getMagic(item) != null))
        }
        .map { item ->
            val shield = DragonfireShields.kind(item)
            if (shield != null && item.id == shield.charged.asRSCM()) {
                DragonfireShields.withCharges(item, DragonfireShields.MAX_CHARGES)
            } else item
        }
        .toList()
}
