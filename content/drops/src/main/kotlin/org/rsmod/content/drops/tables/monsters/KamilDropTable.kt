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
public val kamilDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Kamil Drops",
    npcs = npcs("npc.icediamond_icewarrior"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.chocolate_cake" count 2
        "obj.4dose2restore" count 1
    },
)
