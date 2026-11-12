package org.rsmod.content.bosses.moons.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.pack.PluginPack

class MoonsPluginPack : PluginPack() {
    override fun dbTables(): List<DBTable> = listOf(MoonsTable.moons())
}
