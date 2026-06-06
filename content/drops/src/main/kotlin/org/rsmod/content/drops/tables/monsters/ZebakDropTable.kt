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
public val zebakDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Zebak Drops",
    npcs = npcs("npc.toa_zebak", "npc.toa_zebak_enraged"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.toa_book_zebak" count 1 condition { player ->
            // Drops Need Manual: Only received if players do not have it stored in their bank.
             true
        }
        "obj.toa_zebak_fang" count 1 condition { player ->
            // Drops Need Manual: Only received if players dealt the most damage to Zebak.
             true
        }
    },
)
