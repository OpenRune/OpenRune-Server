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
public val corruptedUnicornDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Corrupted Unicorn Drops",
    npcs = npcs("npc.crystal_unicorn_hm"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.gauntlet_crystal_shard_hm" count 50..100
    },
    mainTable = rsPlayerWeightedTable(total = 21) {
        name("Corrupted Unicorn Drops")
        9 weight "obj.gauntlet_crystal_shard_hm" count 7..14
        6 weight ringNothing()
        3 weight "obj.gauntlet_raw_food" count 2..4
        2 weight "obj.gauntlet_herb_hm" count 1
        1 weight "obj.gauntlet_teleport_crystal_hm" count 1
    },
    tertiaries = rsPlayerTertiaryTable {
        2 outOf 7 weight "obj.gauntlet_generic_component_hm" count 1 condition { player ->
            // Drops Need Manual: Guaranteed after killing two of the mid-level monsters (unicorn, [[Corrupted Scorpion
             true
        }
    },
)
