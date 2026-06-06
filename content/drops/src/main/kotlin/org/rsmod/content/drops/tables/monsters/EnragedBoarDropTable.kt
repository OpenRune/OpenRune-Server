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
public val enragedBoarDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Enraged Boar Drops",
    npcs = npcs("npc.gim_boar"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.raw_boar_meat" count 1
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 128 weight "obj.trail_clue_beginner" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
