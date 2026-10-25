package org.rsmod.content.skills.farming

import dev.openrune.ServerCacheManager
import dev.openrune.types.ItemServerType
import org.rsmod.api.enums.NamedEnums.water_container_fills
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.table.crafting.CraftingHandRow
import org.rsmod.content.generic.locs.water.WateringCans
import org.rsmod.game.entity.Player

internal object FarmingHumidify {
    private val CLAY_RECIPE by lazy { CraftingHandRow.getRow("dbrow.crafting_soft_clay_bucket_water") }

    fun hasTarget(player: Player): Boolean =
        FarmingSaplings.hasHumidifyTarget(player) ||
            player.inv.any { obj -> obj != null && filled(checkNotNull(ServerCacheManager.getItem(obj.id))) != null }

    fun fill(player: Player) {
        val result = player.invTransaction(player.inv) {
            val inventory = select(player.inv)
            for (slot in player.inv.indices) {
                val obj = player.inv[slot] ?: continue
                val type = checkNotNull(ServerCacheManager.getItem(obj.id))
                if (WateringCans.isCan(type.internalName)) continue
                val output = filled(type) ?: continue
                delete(inventory, obj.id, 1, slot)
                add(inventory, output.id, 1, slot = slot)
            }
        }
        check(result.err == null)
        FarmingSaplings.humidify(player)
    }

    private fun filled(type: ItemServerType): ItemServerType? =
        if (type.id == CLAY_RECIPE.input.first().id) CLAY_RECIPE.output.first()
        else water_container_fills[type]
}
