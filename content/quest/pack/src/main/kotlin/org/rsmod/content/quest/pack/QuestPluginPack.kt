package org.rsmod.content.quest.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.pack.PluginPack

class QuestPluginPack : PluginPack() {
    override fun dbTables(): List<DBTable> =
        listOf(GlarialRestrictionsTable.restrictions(), WaterfallPillarRunesTable.runes())
}
