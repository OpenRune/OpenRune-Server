package org.alter.game.model.inv.map

import org.alter.game.model.entity.Player
import org.alter.game.model.inv.Inventory
import org.alter.rscm.RSCM
import org.alter.rscm.RSCMType

object InvMapInit {

    public val defaultInvs: MutableSet<String> =
        hashSetOf(
            "inv.inv",
            "inv.worn",
        )

    public fun init(player: Player) {
        putIfAbsent(player)
        cacheCommons(player)
    }

    public fun putIfAbsent(player: Player) {
        for (default in defaultInvs) {
            if (default !in player.invMap) {
                val create = Inventory.create(default)
                player.invMap[default] = create
            }
        }
    }

    public fun cacheCommons(player: Player) {
        player.inventory = player.invMap.getValue("inv.inv")
        player.equipment = player.invMap.getValue("inv.worn")
    }

    public operator fun plusAssign(inv: String) {
        RSCM.requireRSCM(RSCMType.INVTYPES, inv)
        defaultInvs += inv
    }
}
