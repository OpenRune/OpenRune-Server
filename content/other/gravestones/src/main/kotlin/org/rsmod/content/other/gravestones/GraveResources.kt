package org.rsmod.content.other.gravestones

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import org.rsmod.api.table.mining.MiningRocksRow
import org.rsmod.api.table.prayer.SkillPrayerRow

/**
 * The resources a grave won't stockpile: "unpowered orb, pure essence, bones, ores, planks". Bones
 * and ores come from the prayer and mining tables; `enum.gravestone_resources` holds the rest.
 */
internal object GraveResources {
    private val ids: Set<Int> by lazy {
        val bones = SkillPrayerRow.all().filterNot { it.ashes }.map { it.item.id }
        val ores = MiningRocksRow.all().mapNotNull { it.oreItem?.id }
        val enum = ServerCacheManager.getEnum(RESOURCES_ENUM.asRSCM(RSCMType.ENUM))
        val others = enum?.values?.keys?.map { (it as Number).toInt() }.orEmpty()
        (bones + ores + others).toSet()
    }

    fun isResource(type: ItemServerType): Boolean = type.id in ids

    private const val RESOURCES_ENUM = "enum.gravestone_resources"
}
