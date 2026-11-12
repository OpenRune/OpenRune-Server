package org.rsmod.content.skills.farming.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.pack.PluginPack

class FarmingPluginPack : PluginPack() {
    override fun dbTables(): List<DBTable> =
        listOf(FarmingTables.crops(), FarmingTables.patches(), FarmingTables.composts())

    override fun dbTableSchemas(): List<DBTable> =
        listOf(
            FarmingTables.crops(includeRows = false),
            FarmingTables.patches(includeRows = false),
            FarmingTables.composts(),
        )
}
