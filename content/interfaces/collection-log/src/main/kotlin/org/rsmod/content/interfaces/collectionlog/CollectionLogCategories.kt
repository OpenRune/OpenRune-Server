package org.rsmod.content.interfaces.collectionlog

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.VarBitType
import dev.openrune.types.enums.enum
import dev.openrune.types.varp.VarpServerType
import org.rsmod.api.table.CollectionLogCategoriesRow
import org.rsmod.game.entity.Player

internal data class CollectionLogCategory(
    val structId: Int,
    val completedVarbit: VarBitType,
    val itemIds: Set<Int>,
    val countVarps: List<VarpServerType>,
)

internal object CollectionLogCategories {

    private const val TAB_STRUCT_BASE = 471
    private const val TAB_COUNT = 5
    private const val TAB_CATEGORY_LIST_PARAM = 683

    private val structIdToTabComsub: Map<Int, Pair<Int, Int>> by lazy {
        val map = mutableMapOf<Int, Pair<Int, Int>>()
        for (tab in 0 until TAB_COUNT) {
            val tabStruct = ServerCacheManager.getStruct(TAB_STRUCT_BASE + tab) ?: continue
            val enumId = tabStruct.params?.get(TAB_CATEGORY_LIST_PARAM) as? Int ?: continue
            val categoryStructIds = enum<Int, Int>(enumId).backing.toSortedMap().values
            categoryStructIds.filterNotNull().forEachIndexed { comsub, structId ->
                map[structId] = tab to comsub
            }
        }
        map
    }

    internal val allCategoryStructIds: Set<Int> by lazy { structIdToTabComsub.keys }

    private val allCategories: List<CollectionLogCategory> by lazy {
        CollectionLogCategoriesRow.all().mapNotNull(::resolve)
    }

    private val itemIdToCategories: Map<Int, List<CollectionLogCategory>> by lazy {
        val map = mutableMapOf<Int, MutableList<CollectionLogCategory>>()
        for (category in allCategories) {
            for (itemId in category.itemIds) {
                map.getOrPut(itemId) { mutableListOf() }.add(category)
            }
        }
        map
    }

    private val categoryByTabAndComsub: Map<Pair<Int, Int>, CollectionLogCategory> by lazy {
        allCategories
            .mapNotNull { category -> structIdToTabComsub[category.structId]?.let { it to category } }
            .toMap()
    }

    private val tabItemIds: Map<Int, List<Int>> by lazy {
        allCategories
            .groupBy { structIdToTabComsub[it.structId]?.first }
            .mapNotNull { (tab, categories) ->
                tab?.let { it to categories.flatMap { c -> c.itemIds }.distinct() }
            }
            .toMap()
    }

    fun categoriesContaining(itemId: Int): List<CollectionLogCategory> =
        itemIdToCategories[itemId] ?: emptyList()

    fun forCategory(tab: Int, comsub: Int): CollectionLogCategory? =
        categoryByTabAndComsub[tab to comsub]

    fun tabTotalCount(tab: Int): Int = tabItemIds[tab]?.size ?: 0

    fun tabObtainedCount(player: Player, tab: Int): Int {
        val inv = player.collectionTransmit
        return tabItemIds[tab]?.count { inv.countOf(it) > 0 } ?: 0
    }

    private fun resolve(row: CollectionLogCategoriesRow): CollectionLogCategory? {
        if (row.structId !in structIdToTabComsub) return null
        val completedVarbit = ServerCacheManager.getVarbit(row.completedVarbit) ?: return null
        val itemIds = CollectionLogItems.itemsInCategoryStruct(row.structId).toSet()
        if (itemIds.isEmpty()) {
            return null
        }
        val countVarps =
            listOfNotNull(row.countVarp1, row.countVarp2, row.countVarp3).mapNotNull { varpId ->
                ServerCacheManager.getVarp(varpId)
            }
        return CollectionLogCategory(row.structId, completedVarbit, itemIds, countVarps)
    }
}
