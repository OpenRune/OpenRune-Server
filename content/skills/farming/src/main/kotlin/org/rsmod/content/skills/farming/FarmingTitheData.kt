package org.rsmod.content.skills.farming

import dev.openrune.types.ItemServerType
import org.rsmod.api.table.farming.FarmingTitheRow

internal class TitheCrop(
    val tier: Int,
    val seed: ItemServerType,
    val fruit: ItemServerType,
    val level: Int,
    val harvestXp: Int,
)

internal object FarmingTitheData {
    val row by lazy { FarmingTitheRow.getRow("dbrow.farming_tithe") }
    val crops by lazy {
        row.crops.mapIndexed { index, value ->
            TitheCrop(index + 1, value.t0, value.t1, value.t2, value.t3)
        }
    }
    fun crop(tier: Int): TitheCrop? = crops.getOrNull(tier - 1)
    fun loc(plant: TithePlant): String {
        val art = row.stages.first { it.t0 == plant.tier && it.t1 == plant.stage }
        return when {
            plant.dead -> art.t4.internalName
            plant.watered -> art.t3.internalName
            else -> art.t2.internalName
        }
    }
}
