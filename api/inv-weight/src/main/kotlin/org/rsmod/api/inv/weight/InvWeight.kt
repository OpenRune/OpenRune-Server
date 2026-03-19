package org.rsmod.api.inv.weight

import dev.openrune.ServerCacheManager
import kotlin.collections.iterator
import org.rsmod.game.entity.Player

public object InvWeight {
    public fun calculateWeightInGrams(player: Player): Int {
        var grams = 0
        for (transmitted in player.transmittedInvs.intIterator()) {
            val inv = player.invMap.backing[transmitted]
            checkNotNull(inv) { "Inv expected in `invMap`: $transmitted (invMap=${player.invMap})" }

            val affectsWeight = inv.type.runWeight
            if (!affectsWeight) {
                continue
            }

            for (i in inv.indices) {
                val obj = inv[i] ?: continue
                ServerCacheManager.getItem(obj.id)?.let { grams += it.weight.toInt() }
            }
        }
        return grams
    }
}
