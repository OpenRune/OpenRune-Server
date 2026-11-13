package org.rsmod.content.skills.hunter.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.pack.PluginPack

class HunterRumourPluginPack : PluginPack() {
    override fun dbTables(): List<DBTable> =
        listOf(
            HunterRumourHuntersTable.hunterRumourHunters(),
            HunterRumoursTable.hunterRumours(),
            HunterLootSackTable.lootSacks(),
            HunterLootSackTable.cascades(),
        )
}
