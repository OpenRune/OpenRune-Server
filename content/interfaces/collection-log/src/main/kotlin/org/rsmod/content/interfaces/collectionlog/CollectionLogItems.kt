package org.rsmod.content.interfaces.collectionlog

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.ServerCacheManager
import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.definition.type.VarBitType
import dev.openrune.types.enums.enum
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player

internal object CollectionLogItems {
    private val logger = InlineLogger()
    private const val ITEMS_ENUM_PARAM = 690

    internal fun itemsInCategoryStruct(structId: Int): List<Int> {
        val struct = ServerCacheManager.getStruct(structId) ?: return emptyList()
        val enumId = struct.params?.get(ITEMS_ENUM_PARAM) as? Int ?: return emptyList()
        return enum<Int, Int>(enumId).backing.values.filterNotNull()
    }

    private fun reverseObjName(objId: Int): String? =
        ConstantProvider.mappings["obj"]?.entries?.firstOrNull { it.value == objId }?.key

    /**
     * This is the central map between items in the collection log and their associated varbits
     * which is used to track the number of said item the player has received.
     *
     * The map is constructed by looping over every struct used in the collection log and extracting
     * the corresponding enum which contains the list of items displayed under a collection log
     * category.
     *
     * Varbits used in the collection log MUST adhere to the standard
     * `varbit.collection_item_itemname` where `itemname` is an items gameval.
     */
    private val itemToVarbit: Map<Int, VarBitType> by lazy {
        CollectionLogCategories.allCategoryStructIds
            .asSequence()
            .flatMap { itemsInCategoryStruct(it).asSequence() }
            .distinct()
            .mapNotNull { objId ->
                val name = reverseObjName(objId)
                val varbitId = ConstantProvider.getMappingOrNull("varbit.collection_item_$name")
                if (varbitId == null) {
                    logger.warn {
                        "Collection log item obj.$name ($objId) has no RSCM mapping for varbit.collection_item_$name"
                    }
                    return@mapNotNull null
                }
                val varbit = ServerCacheManager.getVarbit(varbitId)
                if (varbit == null) {
                    logger.warn {
                        "RSCM mapping varbit.collection_item_$name (id $varbitId) exists but is not present in the cache"
                    }
                    return@mapNotNull null
                }
                objId to varbit
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
