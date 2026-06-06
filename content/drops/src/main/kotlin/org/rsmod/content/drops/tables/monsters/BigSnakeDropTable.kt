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
public val bigSnakeDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Big Snake Drops",
    npcs = npcs("npc.hundred_ilm_snake"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.hundred_ilm_snake_corpse" count 1
    },
)
