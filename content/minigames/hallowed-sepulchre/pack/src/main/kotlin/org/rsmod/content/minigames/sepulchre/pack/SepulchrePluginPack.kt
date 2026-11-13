package org.rsmod.content.minigames.sepulchre.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.pack.PluginPack

class SepulchrePluginPack : PluginPack() {
    override fun dbTables(): List<DBTable> =
        listOf(
            SepulchreFloorsTable.floors(),
            SepulchreObstaclesTable.obstacles(),
            SepulchreTrapsTable.traps(),
            SepulchreStrangeTilesTable.strangeTiles(),
        )
}
