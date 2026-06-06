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
public val pestilentBloatDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Pestilent Bloat Drops",
    npcs = npcs("npc.tob_bloat", "npc.tob_bloat_hard", "npc.tob_bloat_story"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.tob_book_bloat" count 1 condition { player ->
            // Drops Need Manual: No longer dropped after reading the book.
             true
        }
    },
)
