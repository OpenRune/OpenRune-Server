package org.rsmod.content.minigames.giantsfoundry.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.pack.PluginPack

class GiantsFoundryPluginPack : PluginPack() {
    override fun dbTables(): List<DBTable> =
        listOf(
            GiantsFoundryTables.tools(),
            GiantsFoundryTables.repairs(),
            GiantsFoundryTables.roomLocs(),
            GiantsFoundryTables.crate(),
        )
}
