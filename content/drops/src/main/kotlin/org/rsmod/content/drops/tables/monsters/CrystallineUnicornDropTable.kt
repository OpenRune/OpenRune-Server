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
public val crystallineUnicornDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Crystalline Unicorn Drops",
    npcs = npcs("npc.crystal_unicorn"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.gauntlet_crystal_shard" count 50..105
    },
)

// Unknown wiki drop rates (text rarity — need data collection):
//   - Raw paddlefish [main/Common]
//   - Weapon frame [main/Common]
//   - Teleport crystal (The Gauntlet) [main/Uncommon]
