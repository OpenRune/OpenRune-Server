package org.rsmod.content.other.sawmill.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.pack.PluginPack

class SawmillPluginPack : PluginPack() {
    override fun dbTables(): List<DBTable> =
        listOf(SawmillPlanksTable.sawmillPlanks(), SawmillOperatorsTable.sawmillOperators())
}
