package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val tzTokJadDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "TzTok-Jad Drops",
    npcs = npcs("npc.tzhaar_fightcave_swarm_boss"),
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 200 weight "obj.jad_pet" count 1
    },
)
