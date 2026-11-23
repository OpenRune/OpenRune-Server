package org.rsmod.content.areas.city.varrock.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.pack.PluginPack

class VarrockPluginPack : PluginPack() {
    override fun dbTables(): List<DBTable> =
        listOf(
            VarrockMuseumTables.nathisQuestions(),
            VarrockMuseumTables.nathisDisplays(),
            VarrockMuseumTables.nathisDisplayModels(),
        )
}
