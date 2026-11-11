package org.rsmod.content.areas.misc.warriorsguild.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.pack.PluginPack

class WarriorsGuildPluginPack : PluginPack() {
    override fun dbTables(): List<DBTable> =
        listOf(
            WarriorsGuildTables.animatorSets(),
            WarriorsGuildTables.catapultShots(),
            WarriorsGuildTables.dummies(),
        )
}
