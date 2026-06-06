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
public val giantLobsterGhostsAhoyDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Giant lobster (Ghosts Ahoy) Drops",
    npcs = npcs("npc.giant_lobster"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.seaweed" count 1
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 25 weight "obj.arceuus_corpse_scorpion" count 1
    },
)

// Unknown wiki drop rates (text rarity — need data collection):
//   - Clue scroll (beginner) [tertiary/Rare]
