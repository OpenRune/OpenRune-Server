package org.rsmod.content.minigames.gauntlet.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.pack.PluginPack

class GauntletPluginPack : PluginPack() {
    override fun dbTables(): List<DBTable> = listOf(GauntletRoomSlotsTable.table())
}
