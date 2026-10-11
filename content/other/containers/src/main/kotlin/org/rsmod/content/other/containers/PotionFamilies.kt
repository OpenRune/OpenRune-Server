package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import org.rsmod.api.table.PotionRow

/** Same native dose enums and exclusions used by the accepted barrel's CS2. */
internal object PotionFamilies {
    data class Family(val id: Int, val doses: Map<Int, Int>, val potion: PotionRow?) {
        val maximum = doses.keys.max()
        fun dose(item: Int) = doses.entries.firstOrNull { it.value == item }?.key ?: 0
    }
    val all: List<Family> by lazy {
        val effects = PotionRow.all().flatMap { row -> row.items.map { it.id to row } }.toMap()
        listOf(710 to 4, 741 to 2).flatMap { (parent, maximum) ->
            ServerCacheManager.getEnum(parent)!!.values.values.mapNotNull { value ->
                val enum = ServerCacheManager.getEnum((value as Number).toInt()) ?: return@mapNotNull null
                val doses = enum.values.mapNotNull { (key, obj) -> (obj as? Number)?.toInt()?.let { key to it } }.toMap()
                val id = doses[maximum] ?: return@mapNotNull null
                val item = ServerCacheManager.getItem(id) ?: return@mapNotNull null
                val params = item.paramsRaw.orEmpty()
                fun param(id: Int) = (params[id] as? Number)?.toInt() ?: 0
                if (param(468) in setOf(0, 44, 49, 51, 54, 55, 56, 57, 58, 59) || param(403) != 0 || param(181) > 0 || param(59) == 1) null
                else Family(id, doses.filterKeys { it in 0..maximum }, effects[id])
            }
        }.distinctBy { it.id }
    }
    fun byItem(item: Int) = all.firstOrNull { it.dose(item) > 0 }
    fun byId(id: Int) = all.firstOrNull { it.id == id }
}
