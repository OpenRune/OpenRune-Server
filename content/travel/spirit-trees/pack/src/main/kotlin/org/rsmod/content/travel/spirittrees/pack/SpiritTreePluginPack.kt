package org.rsmod.content.travel.spirittrees.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.pack.PluginPack

class SpiritTreePluginPack : PluginPack() {
    override fun dbTables(): List<DBTable> = listOf(SpiritTreeTable.destinations())
}
