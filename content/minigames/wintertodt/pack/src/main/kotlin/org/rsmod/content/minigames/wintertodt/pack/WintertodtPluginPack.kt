package org.rsmod.content.minigames.wintertodt.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.pack.PluginPack

class WintertodtPluginPack : PluginPack() {
    override fun dbTables(): List<DBTable> = listOf(WintertodtTables.rewards())
}
