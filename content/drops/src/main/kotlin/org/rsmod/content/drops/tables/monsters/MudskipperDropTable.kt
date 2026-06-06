package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val mudskipperDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Mudskipper Drops",
    npcs = npcs("npc.hundred_pirate_giant_mudskipper", "npc.hundred_pirate_giant_mudskipper_2"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Mudskipper Drops")
        22 weight "obj.oystershell" count 1
        106 weight nothing()
    },
)
