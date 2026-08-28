package org.rsmod.content.skills.fishing.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.pack.PluginPack

class FishingPluginPack : PluginPack() {
    override fun dbTables(): List<DBTable> =
        listOf(
            FishingTable.fishingSpot(),
            FishingConfig.fishingMethod(),
            FishingConfig.fishingSpotDef(),
        )

    
}
