package org.rsmod.content.interfaces.collectionlog

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.VarBitType
import dev.openrune.types.varp.VarpServerType
import org.rsmod.api.table.CollectionLogCategoriesRow
import org.rsmod.game.entity.Player

internal data class CollectionLogCategory(
    val name: String,
    val tab: String,
    val completedVarbit: VarBitType,
    val itemVarbits: Set<VarBitType>,
    val countVarps: List<VarpServerType>,
    val comsub: Int,
)

internal object CollectionLogCategories {

    private val TAB_INDICES =
        mapOf("Bosses" to 0, "Raids" to 1, "Clues" to 2, "Minigames" to 3, "Other" to 4)

    private val allCategories: List<CollectionLogCategory> by lazy {
        CollectionLogCategoriesRow.all().mapNotNull(::resolve)
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
                (tab to category.comsub) to category
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
            row.tabIndex,
        )
    }
}
