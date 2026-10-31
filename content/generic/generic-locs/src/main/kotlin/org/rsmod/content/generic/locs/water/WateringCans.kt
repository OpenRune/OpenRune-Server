package org.rsmod.content.generic.locs.water

import dev.openrune.rscm.RSCM.asRSCM
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player

object WateringCans {
    private const val GRICOLLER = "obj.zeah_wateringcan"
    private const val CHARGES = "varbit.zeah_wateringcan_charges"
    private const val TANK_CAPACITY = 1000
    private const val PREFIX = "obj.watering_can_"
    val CANS = (0..8).map { "$PREFIX$it" } + GRICOLLER
    val WATER_VESSELS = setOf("obj.bucket_water", "obj.bowl_water", "obj.jug_water")

    fun isCan(obj: String): Boolean = obj == GRICOLLER || doses(obj) != null

    fun doses(can: String): Int? =
        if (can.length == PREFIX.length + 1 && can.startsWith(PREFIX)) {
            can.last().digitToIntOrNull()?.takeIf { it in 0..8 }
        } else null

    fun consume(player: Player, can: String, canSlot: Int): Boolean = use(player, can, canSlot)

    fun water(
        player: Player,
        seedlingSlot: Int,
        can: String,
        canSlot: Int,
        dry: Int,
        wet: Int,
    ): Boolean = use(player, can, canSlot, seedlingSlot, dry, wet)

    private fun use(
        player: Player,
        can: String,
        canSlot: Int,
        seedlingSlot: Int = -1,
        dry: Int = 0,
        wet: Int = 0,
    ): Boolean {
        if (!isCan(can) || canSlot !in player.inv.indices || player.inv[canSlot]?.id != can.asRSCM()) return false
        if (seedlingSlot >= 0 && (seedlingSlot !in player.inv.indices || player.inv[seedlingSlot]?.id != dry)) return false
        val doses = doses(can)
        if (can == GRICOLLER) {
            if (player.vars[CHARGES] <= 0) return false
            if (seedlingSlot < 0) {
                VarPlayerIntMapSetter.set(player, CHARGES, player.vars[CHARGES] - 1)
                return true
            }
        } else if (doses == null || doses == 0) return false
        val result = player.invTransaction(player.inv) {
            val inv = select(player.inv)
            if (can != GRICOLLER) {
                delete(inv, can.asRSCM(), 1, canSlot)
                add(inv, CANS[checkNotNull(doses) - 1].asRSCM(), 1, slot = canSlot)
            }
            if (seedlingSlot >= 0) {
                delete(inv, dry, 1, seedlingSlot)
                add(inv, wet, 1, slot = seedlingSlot)
            }
        }
        if (!result.success) return false
        if (can == GRICOLLER) {
            VarPlayerIntMapSetter.set(player, CHARGES, player.vars[CHARGES] - 1)
        }
        return true
    }

    fun needsRefill(player: Player, can: String): Boolean =
        isCan(can) && player.inv.physicalCount(can) > 0 && when {
            can == GRICOLLER -> player.vars[CHARGES] < TANK_CAPACITY
            else -> doses(can)?.let { it < 8 } == true
        }

    fun refill(player: Player, can: String): Boolean {
        if (!isCan(can)) return false
        val slot = player.inv.indexOfFirst { it?.id == can.asRSCM() }
        return refill(player, can, slot)
    }

    fun refill(player: Player, can: String, slot: Int): Boolean {
        if (!isCan(can) || player.inv[slot]?.id != can.asRSCM()) return false
        if (!needsRefill(player, can)) return false
        if (can == GRICOLLER) {
            VarPlayerIntMapSetter.set(player, CHARGES, TANK_CAPACITY)
            return true
        }
        return player.invTransaction(player.inv) {
            val inv = select(player.inv)
            delete(inv, can.asRSCM(), 1, slot)
            add(inv, CANS[8].asRSCM(), 1, slot = slot)
        }.success
    }

    fun refillFrom(
        player: Player,
        can: String,
        canSlot: Int,
        water: String,
        waterSlot: Int,
    ): Boolean {
        val doses = doses(can) ?: return false
        if (doses == 8 || water !in WATER_VESSELS) return false
        val empty = water.removeSuffix("_water") + "_empty"
        return player.invTransaction(player.inv) {
            val inv = select(player.inv)
            delete(inv, can.asRSCM(), 1, canSlot)
            delete(inv, water.asRSCM(), 1, waterSlot)
            add(inv, CANS[8].asRSCM(), 1, slot = canSlot)
            add(inv, empty.asRSCM(), 1, slot = waterSlot)
        }.success
    }
}
