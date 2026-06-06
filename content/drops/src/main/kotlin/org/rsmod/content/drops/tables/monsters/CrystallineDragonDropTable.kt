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
public val crystallineDragonDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Crystalline Dragon Drops",
    npcs = npcs("npc.crystal_dragon"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.gauntlet_crystal_shard" count 1
        "obj.gauntlet_generic_component" count 1
    },
)

// Unknown wiki drop rates (text rarity — need data collection):
//   - Crystal orb [main/Varies]
//   - Crystal spike [main/Varies]
//   - Crystalline bowstring [main/Varies]
//   - Raw paddlefish [main/Common]
//   - Teleport crystal (The Gauntlet) [main/Uncommon]
