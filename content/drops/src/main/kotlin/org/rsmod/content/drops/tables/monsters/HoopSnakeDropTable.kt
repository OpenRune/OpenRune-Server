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
public val hoopSnakeDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Hoop Snake Drops",
    npcs = npcs("npc.fossil_hoopsnake"),
    mainTable = rsPlayerWeightedTable(total = 10) {
        name("Hoop Snake Drops")
        3 weight "obj.village_snake_hide" count 1
        7 weight nothing()
    },
)
