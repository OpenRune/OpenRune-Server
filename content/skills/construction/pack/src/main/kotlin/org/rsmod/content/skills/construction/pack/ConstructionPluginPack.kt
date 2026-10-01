package org.rsmod.content.skills.construction.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.pack.PluginPack

class ConstructionPluginPack : PluginPack() {
    override fun dbTables(): List<DBTable> = listOf(ConstructionTables.furnitureBuilds(), ConstructionDispensers.table())
}
