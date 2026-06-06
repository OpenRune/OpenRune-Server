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
public val tanglefootDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Tanglefoot Drops",
    npcs = npcs("npc.fairy_tanglefoot"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.fairy_queen_secateurs" count 1
    },
)
