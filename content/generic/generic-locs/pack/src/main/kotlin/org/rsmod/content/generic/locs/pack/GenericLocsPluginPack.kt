package org.rsmod.content.generic.locs.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.pack.PluginPack

class GenericLocsPluginPack : PluginPack() {
    override fun dbTables(): List<DBTable> = listOf(LockpickDoorsTable.doors())
}
