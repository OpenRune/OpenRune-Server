package org.rsmod.content.other.nerocurator

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory

object CuratorPurchase {
    fun buy(player: Player, inventory: Inventory, item: String, price: Int): Boolean {
        require(price > 0)
        return player.invTransaction(inventory) {
            val target = select(inventory)
            delete(target, "obj.coins".asRSCM(RSCMType.OBJ), price)
            add(target, item.asRSCM(RSCMType.OBJ), 1)
        }.success
    }
}
