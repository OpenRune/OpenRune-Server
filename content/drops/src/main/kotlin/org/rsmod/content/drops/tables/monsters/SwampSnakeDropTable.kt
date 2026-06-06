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
public val swampSnakeDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Swamp snake Drops",
    npcs = npcs("npc.templetrek_snake_1", "npc.templetrek_snake_2", "npc.templetrek_snake_3"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.templetrek_swamp_snake_hide" count (2..5) condition { player ->
            // Drops Need Manual: Requires knife to skin the dead snake.
             true
        }
    },
)
