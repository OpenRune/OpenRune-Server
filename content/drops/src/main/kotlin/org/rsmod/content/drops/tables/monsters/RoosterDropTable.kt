package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val roosterDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Rooster Drops",
    npcs = npcs("npc.misc_rooster", "npc.rooster"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.raw_chicken" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Rooster Drops")
        64 weight "obj.feather" count 5
        32 weight "obj.feather" count 15
        32 weight ringNothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 300 weight "obj.trail_clue_beginner" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
