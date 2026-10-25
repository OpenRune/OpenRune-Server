package org.rsmod.content.skills.hunter.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.pack.PluginPack

class HunterPluginPack : PluginPack() {
    override fun dbTables(): List<DBTable> =
        listOf(
            ImplingTypesTable.implingTypes(),
            BirdHouseTypesTable.birdHouseTypes(),
            HunterTrapsTable.hunterTraps(),
            HunterTrapCreaturesTable.hunterTrapCreatures(),
            HerbiboarLegsTable.herbiboarLegs(),
            HerbiboarSpotsTable.herbiboarSpots(),
            HerbiboarTunnelsTable.herbiboarTunnels(),
            HerbiboarHerbsTable.herbiboarHerbs(),
            HunterRumourHuntersTable.hunterRumourHunters(),
            HunterRumoursTable.hunterRumours(),
            HunterButterfliesTable.hunterButterflies(),
        )
}
