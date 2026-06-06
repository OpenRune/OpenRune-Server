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
public val mercenaryCaptainDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Mercenary Captain Drops",
    npcs = npcs("npc.desertminingcaptain"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.metal_key" count 1 condition { player ->
            // Drops Need Manual: Only if not owned already
             true
        }
    },
)
