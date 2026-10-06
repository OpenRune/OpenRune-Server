package org.rsmod.content.areas.misc.stronghold_of_security.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.pack.PluginPack

class StrongholdOfSecurityPluginPack : PluginPack() {
    override fun dbTables(): List<DBTable> = listOf(StrongholdFloorsTable.strongholdFloors())
}
