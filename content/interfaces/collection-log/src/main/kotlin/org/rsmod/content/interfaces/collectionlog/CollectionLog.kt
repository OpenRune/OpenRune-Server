package org.rsmod.content.interfaces.collectionlog

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player

public object CollectionLog {
    /** Central entry point for marking an item as obtained for the collection log */
    public fun grant(player: Player, obj: String, count: Int = 1) {
        grant(player, obj.asRSCM(RSCMType.OBJ), count)
    }

    public fun grant(player: Player, objId: Int, count: Int = 1) {
        if (count <= 0) {
            return
        }
        if (!CollectionLogItems.contains(objId)) {
            return
        }
        player.invAdd(player.collectionTransmit, objId, count)
        checkCategoryCompletion(player, objId)
    }

    private fun checkCategoryCompletion(player: Player, itemId: Int) {
        val inv = player.collectionTransmit
        for (category in CollectionLogCategories.categoriesContaining(itemId)) {
            if (player.vars[category.completedVarbit] != 0) {
                continue
            }
            val allObtained = category.itemIds.all { inv.countOf(it) > 0 }
            if (allObtained) {
                VarPlayerIntMapSetter.set(player, category.completedVarbit, 1)
            }
        }
    }
}
