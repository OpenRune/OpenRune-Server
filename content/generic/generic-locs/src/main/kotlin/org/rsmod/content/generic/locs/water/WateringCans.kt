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
    private const val FULL_CAN = "${PREFIX}8"

    private fun isCan(obj: String): Boolean = obj == GRICOLLER || doses(obj) != null

    private fun doses(can: String): Int? =
        if (can.length == PREFIX.length + 1 && can.startsWith(PREFIX)) {
            can.last().digitToIntOrNull()?.takeIf { it in 0..8 }
        } else null

    fun needsRefill(player: Player, can: String): Boolean =
        isCan(can) && player.inv.physicalCount(can) > 0 && when {
            can == GRICOLLER -> player.vars[CHARGES] < TANK_CAPACITY
            else -> doses(can)?.let { it < 8 } == true
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
            add(inv, FULL_CAN.asRSCM(), 1, slot = slot)
        }.success
    }
}
