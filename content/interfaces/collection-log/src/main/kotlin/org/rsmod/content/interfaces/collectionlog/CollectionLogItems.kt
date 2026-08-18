package org.rsmod.content.interfaces.collectionlog

import dev.openrune.ServerCacheManager
import dev.openrune.types.ItemServerType
import dev.openrune.types.enums.enum
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory

/** The collection log tracks obtained items directly in this inventory rather than varbits. */
public val Player.collectionTransmit: Inventory
    get() = invMap.getOrPut("inv.collection_transmit")

internal fun Inventory.countOf(objId: Int): Int = objs.firstOrNull { it?.id == objId }?.count ?: 0

internal object CollectionLogItems {
    private const val ITEMS_ENUM_PARAM = 690

    internal fun itemsInCategoryStruct(structId: Int): List<Int> {
        val struct = ServerCacheManager.getStruct(structId) ?: return emptyList()
        val enumId = struct.params?.get(ITEMS_ENUM_PARAM) as? Int ?: return emptyList()
        return enum<Int, ItemServerType>(enumId).backing.values.filterNotNull().map { it.id }
    }

    /** Every item obj id that appears in any collection log category. */
    private val allItemIds: Set<Int> by lazy {
        CollectionLogCategories.allCategoryStructIds
            .asSequence()
            .flatMap { itemsInCategoryStruct(it).asSequence() }
            .toSet()
    }

    val totalCount: Int by lazy { allItemIds.size }

    fun contains(objId: Int): Boolean = objId in allItemIds

    fun obtainedCount(player: Player): Int {
        val inv = player.collectionTransmit
        return allItemIds.count { inv.countOf(it) > 0 }
    }
}

public fun Player.applyCollectionCount() {
    val obtained = CollectionLogItems.obtainedCount(this)
    VarPlayerIntMapSetter.set(this, "varp.collection_count", obtained)
    VarPlayerIntMapSetter.set(this, "varp.collection_count_max", CollectionLogItems.totalCount)
    VarPlayerIntMapSetter.set(this, "varp.collection_count_highscores", obtained)
}
