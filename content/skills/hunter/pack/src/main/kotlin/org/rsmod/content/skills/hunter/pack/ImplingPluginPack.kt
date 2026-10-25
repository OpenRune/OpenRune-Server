package org.rsmod.content.skills.hunter.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.pack.PluginPack

class ImplingPluginPack : PluginPack() {
    override fun dbTables(): List<DBTable> = listOf(ImplingTypesTable.implingTypes())
}
