package org.rsmod.content.other.nerocurator

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.invtx.*
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory

internal object CompletionistFusion {
    fun combine(player: Player, inventory: Inventory, input: String, cape: String, hood: String): Boolean =
        player.invTransaction(inventory) {
            val target = select(inventory)
            delete(target, "obj.nero_completionist_2011".asRSCM(RSCMType.OBJ), 1)
            delete(target, "obj.nero_completionist_hood".asRSCM(RSCMType.OBJ), 1)
            delete(target, input.asRSCM(RSCMType.OBJ), 1)
            add(target, cape.asRSCM(RSCMType.OBJ), 1)
            add(target, hood.asRSCM(RSCMType.OBJ), 1)
        }.success
}
