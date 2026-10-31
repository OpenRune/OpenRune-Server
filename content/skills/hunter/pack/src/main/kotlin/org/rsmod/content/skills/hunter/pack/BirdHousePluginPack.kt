package org.rsmod.content.skills.hunter.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.pack.PluginPack

class BirdHousePluginPack : PluginPack() {
    override fun dbTables(): List<DBTable> =
        listOf(BirdHouseTypesTable.birdHouseTypes(), BirdHouseSeedsTable.birdHouseSeeds())
}
