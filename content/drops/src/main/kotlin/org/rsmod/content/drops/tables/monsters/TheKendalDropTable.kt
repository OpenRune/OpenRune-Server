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
public val theKendalDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "The Kendal Drops",
    npcs = npcs("npc.mdaughter_bearman", "npc.mdaughter_bearman_fighter"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.mdaughter_bear_helmet" count 1
    },
)
