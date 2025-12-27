package org.alter.game.model.inv.map

import dev.openrune.ServerCacheManager
import dev.openrune.types.InventoryServerType
import org.alter.game.model.entity.Player
import org.alter.game.model.inv.Inventory
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.asRSCM
import org.alter.rscm.RSCMType
import kotlin.collections.getValue
import kotlin.inv
import kotlin.text.set

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
        for (invName in defaultInvs) {
            val default = ServerCacheManager.getInventory(invName.asRSCM())!!
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
        defaultInvs += inv
    }
}
