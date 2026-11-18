package org.rsmod.content.travel.teleports.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.pack.PluginPack

class TeleportsPluginPack : PluginPack() {
    override fun dbTables(): List<DBTable> =
        listOf(
            TeleportTables.destinations(),
            TeleportTables.items(),
            TeleportRouteTable.routes(),
        )
}
