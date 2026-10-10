package org.rsmod.content.raids.toa.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.pack.PluginPack

class ToaPluginPack : PluginPack() {
    override fun dbTables(): List<DBTable> =
        listOf(
            ToaRoomsTable.rooms(),
            ToaRoomsTable.exclusions(),
            ToaPathsTable.paths(),
            ToaPickaxesTable.pickaxes(),
        )
}
