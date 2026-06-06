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
public val mouseDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Mouse Drops",
    npcs = npcs("npc.grim_giant_mouse", "npc.grim_giant_mouse2"),
    mainTable = rsPlayerWeightedTable(total = 4) {
        name("Mouse Drops")
        1 weight "obj.cheese" count 1
        3 weight "obj.coins" count 12..131
    },
)
