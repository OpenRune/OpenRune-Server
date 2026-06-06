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
public val buffaloDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Buffalo Drops",
    npcs = npcs("npc.varlamore_buffalo"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.raw_beef" count 1
    },
)

// Unknown wiki drop rates (text rarity — need data collection):
//   - Token (Varlamore) [guaranteed/Varies]
//   - Clue scroll (beginner) [tertiary/]
//   - Clue scroll (easy) [tertiary/]
