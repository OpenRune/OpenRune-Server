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
public val irvigSenayDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Irvig Senay Drops",
    npcs = npcs("npc.irvig_senay"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.heartcrystal_sectionb" count 1 condition { player ->
            // Drops Need Manual: Only if unowned during the Legends' Quest.
             true
        }
    },
)
