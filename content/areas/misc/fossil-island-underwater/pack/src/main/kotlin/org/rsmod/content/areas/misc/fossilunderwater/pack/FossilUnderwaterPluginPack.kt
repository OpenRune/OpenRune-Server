package org.rsmod.content.areas.misc.fossilunderwater.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.pack.PluginPack

class FossilUnderwaterPluginPack : PluginPack() {
    override fun dbTables(): List<DBTable> =
        listOf(FossilUnderwaterRewardsTable.rewards(), FossilUnderwaterCurrentsTable.currents())
}
