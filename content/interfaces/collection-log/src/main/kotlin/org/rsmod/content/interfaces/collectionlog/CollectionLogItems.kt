package org.rsmod.content.interfaces.collectionlog

import dev.openrune.definition.type.VarBitType
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.table.CollectionLogCategoriesRow
import org.rsmod.game.entity.Player

internal object CollectionLogItems {
    private val itemToVarbit: Map<Int, VarBitType> by lazy {
        CollectionLogCategoriesRow.all()
            .asSequence()
            .flatMap { it.items.asSequence() }
            .mapNotNull { item ->
                item.paramOrNull(params.collection_log_varbit)?.let { item.id to it }
            }
            .toMap()
    }

    private val distinctVarbits: List<VarBitType> by lazy { itemToVarbit.values.distinct() }

    val totalCount: Int by lazy { distinctVarbits.size }

    fun varbitOf(objId: Int): VarBitType? = itemToVarbit[objId]

    fun all(): Map<Int, VarBitType> = itemToVarbit

    fun obtainedCount(player: Player): Int = distinctVarbits.count { player.vars[it] > 0 }
}

public fun Player.applyCollectionCount() {
    val obtained = CollectionLogItems.obtainedCount(this)
    VarPlayerIntMapSetter.set(this, "varp.collection_count", obtained)
    VarPlayerIntMapSetter.set(this, "varp.collection_count_max", CollectionLogItems.totalCount)
    VarPlayerIntMapSetter.set(this, "varp.collection_count_highscores", obtained)
}
