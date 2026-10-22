package org.rsmod.content.skills.runecrafting.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.pack.PluginPack

class RunecraftingPluginPack : PluginPack() {
    override fun dbTables(): List<DBTable> = listOf(EssenceMineTeleportersTable.teleporters())
}
