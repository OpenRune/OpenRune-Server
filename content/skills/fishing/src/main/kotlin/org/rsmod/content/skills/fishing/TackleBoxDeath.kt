package org.rsmod.content.skills.fishing

import org.rsmod.api.death.PlayerDeathStorageHook
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj

private val Player.emptyTackleBoxOnDeath by boolVarBit("varbit.emptyondeath_tacklebox")

/**
 * "Always empty tackle boxes on death": the box's contents go to the grave even when the box is
 * kept, and always on a PvP death.
 */
class TackleBoxDeath : PlayerDeathStorageHook {
    override fun empty(player: Player, isPvpDeath: Boolean): List<InvObj> {
        if (BOX !in player.inv) {
            return emptyList()
        }
        if (!isPvpDeath && !player.emptyTackleBoxOnDeath) {
            return emptyList()
        }
        val box = player.invMap.getOrPut(INV)
        val contents = box.filterNotNull { true }
        box.fillNulls()
        return contents
    }

    private companion object {
        const val BOX = "obj.tackle_box"
        const val INV = "inv.tackle_box"
    }
}
