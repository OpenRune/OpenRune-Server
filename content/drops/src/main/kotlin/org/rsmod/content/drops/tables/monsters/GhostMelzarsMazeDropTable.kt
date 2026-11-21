package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val ghostMelzarsMazeDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Ghost (Melzar's Maze) Drops",
    npcs = npcs("npc.dragonslayer_ghost_1_key"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.orangekey" count 1
    },
)
