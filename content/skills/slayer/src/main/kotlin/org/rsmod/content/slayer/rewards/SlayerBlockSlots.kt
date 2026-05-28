package org.rsmod.content.slayer.rewards

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.table.slayer.SlayerMastersRow
import org.rsmod.game.entity.Player

object SlayerBlockSlots {

    private val qpRequiredBySlot = intArrayOf(50, 100, 150, 200, 250, 300, 0)

    fun isSlotUnlocked(player: Player, slotIndex: Int): Boolean {
        if (slotIndex == 6) {
            return hasLumbridgeDraynorEliteDiary(player)
        }
        return player.vars["varp.qp"] >= qpRequiredBySlot[slotIndex]
    }

    fun firstEmptySlot(player: Player, master: SlayerMastersRow): Int? {
        for (slot in 0 until 7) {
            if (!isSlotUnlocked(player, slot)) continue
            val varbit = RSCM.getReverseMapping(RSCMType.VARBIT,master.blockVarbits[slot])
            if (player.vars[varbit] == 0) return slot
        }
        return null
    }

    private fun hasLumbridgeDraynorEliteDiary(player: Player): Boolean {
        return true
    }
}
