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
public val melzarTheMadDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Melzar the Mad Drops",
    npcs = npcs("npc.melzar_the_mad"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.magentakey" count 1
    },
)
