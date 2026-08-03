package org.rsmod.content.interfaces.collectionlog

import dev.openrune.definition.type.VarBitType
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.varp.bits
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player
import org.rsmod.utils.bits.bitMask

public object CollectionLog {
    /** Central entry point for marking an item as obtained for the collection log */
    public fun grant(player: Player, obj: String, count: Int = 1) {
        grant(player, obj.asRSCM(RSCMType.OBJ), count)
    }

    public fun grant(player: Player, objId: Int, count: Int = 1) {
        if (count <= 0) {
            return
        }
        val varbit = CollectionLogItems.varbitOf(objId) ?: return
        val current = player.vars[varbit]
        val max = varbit.bits.bitMask.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val updated = (current.toLong() + count).coerceAtMost(max.toLong()).toInt()
        if (updated != current) {
            VarPlayerIntMapSetter.set(player, varbit, updated)
        }
        checkCategoryCompletion(player, varbit)
    }

    private fun checkCategoryCompletion(player: Player, itemVarbit: VarBitType) {
        for (category in CollectionLogCategories.categoriesContaining(itemVarbit)) {
            if (player.vars[category.completedVarbit] != 0) {
                continue
            }
            val allObtained = category.itemVarbits.all { player.vars[it] > 0 }
            if (allObtained) {
                VarPlayerIntMapSetter.set(player, category.completedVarbit, 1)
            }
        }
    }
}
