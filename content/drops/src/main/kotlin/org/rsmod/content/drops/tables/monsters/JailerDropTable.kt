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
public val jailerDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Jailer Drops",
    npcs = npcs("npc.jailer"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.jail_key" count 1
    },
)
