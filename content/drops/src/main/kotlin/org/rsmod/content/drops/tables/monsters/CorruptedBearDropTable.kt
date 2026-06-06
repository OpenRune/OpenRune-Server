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
public val corruptedBearDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Corrupted Bear Drops",
    npcs = npcs("npc.crystal_bear_hm"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.gauntlet_crystal_shard_hm" count 30..60
        "obj.gauntlet_generic_component_hm" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 18) {
        name("Corrupted Bear Drops")
        3 weight ringNothing()
        9 weight "obj.gauntlet_crystal_shard_hm" count 10..21
        3 weight "obj.gauntlet_raw_food" count 3..5
        2 weight "obj.gauntlet_herb_hm" count 1
        1 weight "obj.gauntlet_teleport_crystal_hm" count 1
    },
)

// Unknown wiki drop rates (text rarity — need data collection):
//   - Corrupted spike [main/Varies]
//   - Corrupted bowstring [main/Varies]
//   - Corrupted orb [main/Varies]
