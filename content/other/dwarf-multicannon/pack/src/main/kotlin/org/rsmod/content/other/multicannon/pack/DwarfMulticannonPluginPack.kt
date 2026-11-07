package org.rsmod.content.other.multicannon.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.pack.PluginPack

class DwarfMulticannonPluginPack : PluginPack() {
    override fun dbTables(): List<DBTable> =
        listOf(DwarfMulticannonTables.kits(), DwarfMulticannonTables.npcs())
}
