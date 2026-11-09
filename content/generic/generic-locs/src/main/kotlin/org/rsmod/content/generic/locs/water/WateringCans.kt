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
    private val CANS =
        listOf(
            "obj.watering_can_0",
            "obj.watering_can_1",
            "obj.watering_can_2",
            "obj.watering_can_3",
            "obj.watering_can_4",
            "obj.watering_can_5",
            "obj.watering_can_6",
            "obj.watering_can_7",
            "obj.watering_can_8",
        )
    private const val FULL_CAN = "obj.watering_can_8"

    private fun isCan(obj: String): Boolean = obj == GRICOLLER || doses(obj) != null

    private fun doses(can: String): Int? = CANS.indexOf(can).takeIf { it >= 0 }

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
