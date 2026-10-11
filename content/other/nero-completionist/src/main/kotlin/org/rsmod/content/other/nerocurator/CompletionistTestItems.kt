package org.rsmod.content.other.nerocurator

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.invtx.*
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory

internal object CompletionistTestItems {
    fun grant(player: Player, inventory: Inventory, items: List<String>): Boolean {
        if (inventory.objs.count { it == null } < items.size)return false
        return player.invTransaction(inventory) {
            val target = select(inventory)
            for (item in items) add(target, item.asRSCM(RSCMType.OBJ), 1)
        }.success
    }
}
