package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.brimstoneKeyRoll
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val toragTheCorruptedDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Torag the Corrupted Drops",
    npcs = npcs("npc.barrows_torag"),
    tertiaries = rsPlayerTertiaryTable {
        onBuilder { brimstoneKeyRoll() }
    },
)
