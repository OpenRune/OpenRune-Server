package org.rsmod.content.skills.hunter.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.pack.PluginPack

class HunterTrapPluginPack : PluginPack() {
    override fun dbTables(): List<DBTable> =
        listOf(
            HunterTrapsTable.hunterTraps(),
            HunterTrapCreaturesTable.hunterTrapCreatures(),
            HunterNetTrapTable.netTraps(),
            HunterDeadfallTable.deadfalls(),
            HunterFalconryTable.falconry(),
            HunterPitfallTable.pitfalls(),
            HunterTrackingTable.tracking(),
            HunterAerialFishingTable.aerialFishing(),
            HunterCrabTrapTable.crabTraps(),
            HunterMossLizardTable.mossLizards(),
            HunterCustomFurTable.customFurs(),
            HunterPouchTable.pouches(),
            HunterLeonBoltTable.leonBolts(),
            HunterLeonBoltTable.boltFletching(),
        )
}
