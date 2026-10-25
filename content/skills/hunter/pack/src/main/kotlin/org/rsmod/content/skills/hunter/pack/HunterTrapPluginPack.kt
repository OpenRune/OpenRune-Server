package org.rsmod.content.skills.hunter.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.pack.PluginPack

class HunterTrapPluginPack : PluginPack() {
    override fun dbTables(): List<DBTable> =
        listOf(
            HunterTrapsTable.hunterTraps(),
            HunterTrapCreaturesTable.hunterTrapCreatures(),
        )
}
