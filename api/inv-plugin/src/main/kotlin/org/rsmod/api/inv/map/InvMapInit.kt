package org.rsmod.api.inv.map

import dev.openrune.types.InventoryServerType
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory

public class InvMapInit {
    public val defaultInvs: MutableSet<String> = hashSetOf("inv.inv", "inv.worn")

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
        player.inv = player.invMap.getValue("inv.inv")
        player.worn = player.invMap.getValue("inv.worn")
    }

    public operator fun plusAssign(inv: String) {
        defaultInvs += inv
    }
}
