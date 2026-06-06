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
public val kephriDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Kephri Drops",
    npcs = npcs("npc.toa_kephri_boss_enrage"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.toa_book_kephri" count 1 condition { player ->
            // Drops Need Manual: Only received if players do not have it stored in their bank.
             true
        }
        "obj.toa_kephri_poo" count 1 condition { player ->
            // Drops Need Manual: Only received if players dealt the most damage to Kephri.
             true
        }
    },
)
