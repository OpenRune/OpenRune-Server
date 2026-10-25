package org.rsmod.content.skills.hunter.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.pack.PluginPack

class HerbiboarPluginPack : PluginPack() {
    override fun dbTables(): List<DBTable> =
        listOf(
            HerbiboarLegsTable.herbiboarLegs(),
            HerbiboarSpotsTable.herbiboarSpots(),
            HerbiboarTunnelsTable.herbiboarTunnels(),
            HerbiboarHerbsTable.herbiboarHerbs(),
        )
}
