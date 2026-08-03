package org.rsmod.content.interfaces.collectionlog

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.VarBitType
import dev.openrune.types.enums.enum
import dev.openrune.types.varp.VarpServerType
import org.rsmod.api.table.CollectionLogCategoriesRow
import org.rsmod.game.entity.Player

internal data class CollectionLogCategory(
    val name: String,
    val tab: String,
    val completedVarbit: VarBitType,
    val itemVarbits: Set<VarBitType>,
    val countVarps: List<VarpServerType>,
)

internal object CollectionLogCategories {

    private const val TAB_STRUCT_BASE = 471
    private const val TAB_CATEGORY_LIST_PARAM = 683
    private const val CATEGORY_NAME_PARAM = 689

    private val TAB_INDICES =
        mapOf("Bosses" to 0, "Raids" to 1, "Clues" to 2, "Minigames" to 3, "Other" to 4)

    private val allCategories: List<CollectionLogCategory> by lazy {
        CollectionLogCategoriesRow.all().mapNotNull(::resolve)
    }

    /**
     * This creates a map that provides stable category indexing by reading the actual clientsided
     * enums. Initially category indices were hardcoded in the DB table but since the categories
     * are sorted alphabetically an index is mutable and will change when a new category is added
     * to the log.
     */
    private val comsubByTabAndName: Map<Pair<Int, String>, Int> by lazy {
        val map = mutableMapOf<Pair<Int, String>, Int>()
        for (tab in TAB_INDICES.values) {
            val tabStruct = ServerCacheManager.getStruct(TAB_STRUCT_BASE + tab) ?: continue
            val enumId = tabStruct.params?.get(TAB_CATEGORY_LIST_PARAM) as? Int ?: continue
            val categoryStructIds = enum<Int, Int>(enumId).backing.toSortedMap().values
            categoryStructIds.filterNotNull().forEachIndexed { comsub, categoryStructId ->
                val categoryName =
                    ServerCacheManager.getStruct(categoryStructId)?.params?.get(CATEGORY_NAME_PARAM)
                        as? String ?: return@forEachIndexed
                map[tab to categoryName] = comsub
            }
        }
        map
    }

    private val itemVarbitToCategories: Map<VarBitType, List<CollectionLogCategory>> by lazy {
        val map = mutableMapOf<VarBitType, MutableList<CollectionLogCategory>>()
        for (category in allCategories) {
            for (itemVarbit in category.itemVarbits) {
                map.getOrPut(itemVarbit) { mutableListOf() }.add(category)
            }
        }
        map
    }

    private val categoryByTabAndComsub: Map<Pair<Int, Int>, CollectionLogCategory> by lazy {
        allCategories
            .mapNotNull { category ->
                val tab = TAB_INDICES[category.tab] ?: return@mapNotNull null
                val comsub = comsubByTabAndName[tab to category.name] ?: return@mapNotNull null
                (tab to comsub) to category
            }
            .toMap()
    }

    private val tabItemVarbits: Map<Int, List<VarBitType>> by lazy {
        allCategories
            .groupBy { TAB_INDICES[it.tab] }
            .mapNotNull { (tab, categories) ->
                tab?.let { it to categories.flatMap { c -> c.itemVarbits }.distinct() }
            }
            .toMap()
    }

    fun categoriesContaining(itemVarbit: VarBitType): List<CollectionLogCategory> =
        itemVarbitToCategories[itemVarbit] ?: emptyList()

    fun forCategory(tab: Int, comsub: Int): CollectionLogCategory? =
        categoryByTabAndComsub[tab to comsub]

    fun tabTotalCount(tab: Int): Int = tabItemVarbits[tab]?.size ?: 0

    fun tabObtainedCount(player: Player, tab: Int): Int =
        tabItemVarbits[tab]?.count { player.vars[it] > 0 } ?: 0

    private fun resolve(row: CollectionLogCategoriesRow): CollectionLogCategory? {
        val completedVarbit = ServerCacheManager.getVarbit(row.completedVarbit) ?: return null
        val itemVarbits = row.items.mapNotNull { CollectionLogItems.varbitOf(it.id) }.toSet()
        if (itemVarbits.isEmpty()) {
            return null
        }
        val countVarps =
            listOfNotNull(row.countVarp1, row.countVarp2, row.countVarp3).mapNotNull { varpId ->
                ServerCacheManager.getVarp(varpId)
            }
        return CollectionLogCategory(
            row.name,
            row.category,
            completedVarbit,
            itemVarbits,
            countVarps,
        )
    }
}
