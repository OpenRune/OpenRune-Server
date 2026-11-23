package org.rsmod.content.travel.jewellery

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.utils.bits.getBits
import org.rsmod.utils.bits.withBits

internal class XericTalismanCharges(
    private val chargedName: String,
    private val inertName: String,
    private val fangName: String,
    private val bits: IntRange,
    private val max: Int,
) {
    private val charged = chargedName.asRSCM(RSCMType.OBJ)
    private val inert = inertName.asRSCM(RSCMType.OBJ)
    private val fang = fangName.asRSCM(RSCMType.OBJ)

    fun charges(obj: InvObj?): Int =
        if (obj == null || obj.id != charged) 0 else obj.vars.getBits(bits)

    fun charge(inv: Inventory, slot: Int): ChargeResult {
        val talisman = inv[slot]
        if (talisman == null || (talisman.id != charged && talisman.id != inert)) {
            return ChargeResult.Vanished
        }
        val current = charges(talisman)
        val fangSlot = inv.indexOfFirst { it?.id == fang }
        val fangs = inv[fangSlot]?.count ?: 0
        if (current >= max) {
            return ChargeResult.Full
        }
        if (fangs == 0) {
            return ChargeResult.Vanished
        }
        val added = minOf(fangs, max - current)
        val total = current + added
        inv[fangSlot] = if (added == fangs) null else InvObj(fangName, fangs - added)
        inv[slot] = InvObj(chargedName, 1, talisman.vars.withBits(bits, total))
        return ChargeResult.Charged(total)
    }

    fun uncharge(inv: Inventory, slot: Int): UnchargeResult {
        val talisman = inv[slot]
        if (talisman == null || talisman.id != charged) {
            return UnchargeResult.Vanished
        }
        val count = charges(talisman)
        if (count == 0) {
            return UnchargeResult.Empty
        }
        val fangSlot = inv.indexOfFirst { it?.id == fang }
        val target = if (fangSlot >= 0) fangSlot else inv.indexOfFirst { it == null }
        if (target < 0) {
            return UnchargeResult.NoSpace
        }
        val held = inv[target]?.count ?: 0
        if (held.toLong() + count > Int.MAX_VALUE) {
            return UnchargeResult.NoSpace
        }
        inv[target] = InvObj(fangName, held + count)
        inv[slot] = InvObj(inertName, 1, talisman.vars.withBits(bits, 0))
        return UnchargeResult.Uncharged(count)
    }

    fun spend(inv: Inventory, slot: Int): Int? {
        val talisman = inv[slot]
        if (talisman == null || talisman.id != charged) {
            return null
        }
        val count = charges(talisman)
        if (count == 0) {
            return null
        }
        val left = count - 1
        inv[slot] =
            if (left == 0) {
                InvObj(inertName, 1, talisman.vars.withBits(bits, 0))
            } else {
                talisman.copy(vars = talisman.vars.withBits(bits, left))
            }
        return left
    }

    fun dismantle(inv: Inventory, slot: Int, shards: Int): Boolean {
        val talisman = inv[slot]
        if (talisman == null || talisman.id != inert) {
            return false
        }
        val fangSlot = inv.indexOfFirst { it?.id == fang }
        if (fangSlot < 0) {
            inv[slot] = InvObj(fangName, shards)
            return true
        }
        val held = inv.getValue(fangSlot).count
        if (held.toLong() + shards > Int.MAX_VALUE) {
            return false
        }
        inv[fangSlot] = InvObj(fangName, held + shards)
        inv[slot] = null
        return true
    }
}

internal sealed interface ChargeResult {
    data object Vanished : ChargeResult

    data object Full : ChargeResult

    data class Charged(val total: Int) : ChargeResult
}

internal sealed interface UnchargeResult {
    data object Vanished : UnchargeResult

    data object Empty : UnchargeResult

    data object NoSpace : UnchargeResult

    data class Uncharged(val count: Int) : UnchargeResult
}
