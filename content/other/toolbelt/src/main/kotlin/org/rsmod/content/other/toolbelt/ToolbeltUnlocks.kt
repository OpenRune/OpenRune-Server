package org.rsmod.content.other.toolbelt

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.table.ToolbeltSlotsRow
import org.rsmod.game.entity.Player

object ToolbeltUnlocks {
    private const val UNLOCK_VARP = "varp.toolbelt_unlocks"

    fun isUnlocked(player: Player, slot: ToolbeltSlotsRow): Boolean {
        val bit = slot.unlockBit ?: return true
        val varp = ServerCacheManager.getVarp(UNLOCK_VARP.asRSCM(RSCMType.VARP)) ?: return true
        return player.vars[varp] and (1 shl bit) != 0
    }

    fun setUnlocked(player: Player, slot: ToolbeltSlotsRow, unlocked: Boolean) {
        val bit = slot.unlockBit ?: return
        val varp = ServerCacheManager.getVarp(UNLOCK_VARP.asRSCM(RSCMType.VARP)) ?: return
        val flag = 1 shl bit
        val current = player.vars[varp]
        val value =
            if (unlocked) {
                current or flag
            } else {
                current and flag.inv()
            }
        VarPlayerIntMapSetter.set(player, varp, value)
    }

    fun unlockMessage(slot: ToolbeltSlotsRow): String =
        slot.unlockHint?.takeIf { it.isNotBlank() }
            ?: "You cannot add this tool to your tool belt yet."

    fun slotsWithUnlock(): List<ToolbeltSlotsRow> =
        ToolbeltSlotsRow.all().filter { it.unlockBit != null }
}
