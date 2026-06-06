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
public val theMaidenOfSugadintiDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "The Maiden of Sugadinti Drops",
    npcs = npcs("npc.tob_maiden_100", "npc.tob_maiden_100_hard", "npc.tob_maiden_100_story", "npc.tob_maiden_30", "npc.tob_maiden_50", "npc.tob_maiden_70"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.tob_book_maiden" count 1 condition { player ->
            // Drops Need Manual: No longer dropped after reading the book.
             true
        }
    },
)
