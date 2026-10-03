package org.rsmod.content.other.special.attacks.shield

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import java.time.Instant
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.enumVarp
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.player.worn.DragonfireShields
import org.rsmod.api.player.worn.DragonfireShields.Kind
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpWorn2
import org.rsmod.api.script.onOpWorn3
import org.rsmod.api.script.onOpWorn4
import org.rsmod.api.specials.SpecialAttackType
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal var Player.shieldReadyEpoch by intVarp("varp.dragonfire_special_ready_epoch")
internal var Player.shieldSpecialType by enumVarp<SpecialAttackType>("varp.sa_attack")

internal object ShieldCooldown {
    const val SECONDS = 120
    fun now(): Int = Instant.now().epochSecond.toInt()
    fun remaining(ready: Int, now: Int = now()): Int = (ready.toLong() - now).coerceIn(0, SECONDS.toLong()).toInt()
}

class DragonfireShieldScript @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        for (kind in Kind.entries) {
            onOpWorn2(kind.charged) { toggle(it.obj) }
            onOpWorn3(kind.charged) { inspect(it.obj) }
            onOpWorn2(kind.uncharged) { inspect(it.obj) }
            for (symbol in listOf(kind.charged, kind.uncharged)) {
                onOpHeld3(symbol) { inspect(it.obj) }
                if (kind == Kind.WYVERN) {
                    for (resource in resources) {
                        onOpHeldU(resource.symbol, symbol) {
                            recharge(player.inv, it.secondSlot, resource, it.firstSlot)
                        }
                    }
                } else {
                    onOpHeldU("obj.bottled_dragonbreath", symbol) {
                        recharge(player.inv, it.secondSlot, bottledBreath, it.firstSlot)
                    }
                }
            }
            onOpHeld4(kind.charged) {
                it.inventory[it.slot] = DragonfireShields.withCharges(it.obj, 0)
                mes("You release the remaining charges from your shield.")
            }
        }
        onOpWorn4(Kind.WYVERN.charged) { rechargeWorn() }
        onOpWorn3(Kind.WYVERN.uncharged) { rechargeWorn() }
    }

    private fun ProtectedAccess.inspect(shield: InvObj) {
        val charges = DragonfireShields.charges(shield)
        val remaining = ShieldCooldown.remaining(player.shieldReadyEpoch)
        mes("Your shield has $charges/${DragonfireShields.MAX_CHARGES} charges.")
        if (remaining > 0) mes("Its special attack will be ready in $remaining seconds.")
        if (charges == 0) mes(if (DragonfireShields.kind(shield) == Kind.WYVERN) {
            "Recharge it with fossils or numulites (500 per charge)."
        } else {
            "Absorb dragon breath or use bottled dragonbreath to charge it."
        })
    }

    private fun ProtectedAccess.toggle(shield: InvObj) {
        if (player.shieldSpecialType == SpecialAttackType.Shield) {
            player.shieldSpecialType = SpecialAttackType.None
            mes("Shield special attack cancelled.")
            return
        }
        if (DragonfireShields.charges(shield) == 0) {
            inspect(shield)
            return
        }
        val remaining = ShieldCooldown.remaining(player.shieldReadyEpoch)
        if (remaining > 0) {
            mes("Your shield is still cooling down ($remaining seconds).")
            return
        }
        player.shieldSpecialType = SpecialAttackType.Shield
        mes("Your shield is ready to discharge on your next attack.")
    }

    private fun ProtectedAccess.rechargeWorn() {
        var charged = false
        for (resource in resources) {
            for (slot in player.inv.objs.indices) {
                if (player.inv[slot]?.id == resource.symbol.asRSCM()) {
                    charged = recharge(player.worn, Wearpos.LeftHand.slot, resource, slot, silent = true) || charged
                }
            }
        }
        val shield = player.worn[Wearpos.LeftHand.slot] ?: return
        if (!charged && DragonfireShields.charges(shield) < DragonfireShields.MAX_CHARGES) {
            mes("You need fossils or at least 500 numulites in your inventory.")
        }
        inspect(shield)
    }

    private fun ProtectedAccess.recharge(
        inventory: Inventory,
        shieldSlot: Int,
        resource: Resource,
        resourceSlot: Int,
        silent: Boolean = false,
    ): Boolean {
        val shield = inventory[shieldSlot] ?: return false
        val kind = DragonfireShields.kind(shield) ?: return false
        if ((kind == Kind.WYVERN) != (resource != bottledBreath)) return false
        val held = player.inv[resourceSlot] ?: return false
        if (held.id != resource.symbol.asRSCM()) return false
        val before = DragonfireShields.charges(shield)
        val available = DragonfireShields.MAX_CHARGES - before
        val units = minOf((available + resource.charges - 1) / resource.charges, held.count / resource.cost)
        if (units <= 0) {
            if (!silent) mes(if (available == 0) "Your shield is already fully charged." else "You need ${resource.cost} numulites per charge.")
            return false
        }
        // ProtectedAccess serializes this operation; validate the replacement before taking payment.
        val replacement = DragonfireShields.withCharges(shield, minOf(50, before + units * resource.charges))
        val payment = player.invDel(player.inv, held.id, units * resource.cost, slot = resourceSlot)
        if (!payment.success) return false
        inventory[shieldSlot] = replacement
        if (!silent) inspect(replacement)
        return true
    }

    private data class Resource(val symbol: String, val cost: Int, val charges: Int)

    private companion object {
        val bottledBreath = Resource("obj.bottled_dragonbreath", 1, 50)
        val resources = buildList {
            add(Resource("obj.fossil_numulite", 500, 1))
            for (size in listOf("small", "medium", "large", "rare")) {
                add(Resource("obj.fossil_${size}_unid", 1, 1))
            }
            for ((size, charges) in listOf("small" to 1, "medium" to 2, "plant" to 2, "large" to 3)) {
                for (part in 1..5) add(Resource("obj.fossil_${size}_$part", 1, charges))
            }
        }
    }
}
