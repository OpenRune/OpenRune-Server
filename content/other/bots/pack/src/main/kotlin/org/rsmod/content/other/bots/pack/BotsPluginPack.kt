package org.rsmod.content.other.bots.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.pack.PluginPack

class BotsPluginPack : PluginPack() {
    override fun dbTables(): List<DBTable> = listOf(BotSpawnsTable.spawns(), BotOutfitsTable.outfits())
}
