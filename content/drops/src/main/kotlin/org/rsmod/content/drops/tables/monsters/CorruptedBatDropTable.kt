package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val corruptedBatDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Corrupted Bat Drops",
    npcs = npcs("npc.crystal_bat_hm"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.gauntlet_crystal_shard_hm" count 10..30
    },
    mainTable = rsPlayerWeightedTable(total = 24) {
        name("Corrupted Bat Drops")
        9 weight ringNothing()
        9 weight "obj.gauntlet_crystal_shard_hm" count 3..7
        3 weight "obj.gauntlet_raw_food" count 1..3
        2 weight "obj.gauntlet_herb_hm" count 1
        1 weight "obj.gauntlet_teleport_crystal_hm" count 1
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 4 weight "obj.gauntlet_generic_component_hm" count 1 condition { player ->
            // Drops Need Manual: Guaranteed after killing three of the low-level monsters ([[Corrupted Rat
             true
        }
    },
)
