package org.rsmod.content.other.treasuretrails.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.pack.PluginPack

class TreasureTrailsPluginPack : PluginPack() {
    override fun dbTables(): List<DBTable> =
        listOf(
            TrailTierTable.tiers(),
            TrailCharlieTaskTable.tasks(),
            TrailMapInterfaceTable.maps(),
            TrailEmoteAreaTable.areas(),
        )
}
