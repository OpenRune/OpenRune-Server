package org.rsmod.content.areas.misc.deathsoffice.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.pack.PluginPack

class DeathsOfficePluginPack : PluginPack() {
    override fun dbTables(): List<DBTable> = listOf(DeathOfficeEntrancesTable.entrances())
}
