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
public val rowdySlaveDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Rowdy slave Drops",
    npcs = npcs("npc.slave_rowdy"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.slave_shirt" count 1
        "obj.slave_robe" count 1
        "obj.slave_boots" count 1
    },
)
