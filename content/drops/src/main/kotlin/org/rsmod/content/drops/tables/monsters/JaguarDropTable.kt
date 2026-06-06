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
public val jaguarDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Jaguar Drops",
    npcs = npcs("npc.varlamore_jaguar"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.varlamore_jaguar_fur" count 1
    },
)

// Unknown wiki drop rates (text rarity — need data collection):
//   - Clue scroll (medium) [tertiary/Rare]
