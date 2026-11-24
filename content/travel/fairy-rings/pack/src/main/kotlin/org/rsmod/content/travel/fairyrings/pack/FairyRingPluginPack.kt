package org.rsmod.content.travel.fairyrings.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.pack.PluginPack

class FairyRingPluginPack : PluginPack() {
    override fun dbTables(): List<DBTable> = listOf(FairyRingRequirementsTable.requirements())
}
