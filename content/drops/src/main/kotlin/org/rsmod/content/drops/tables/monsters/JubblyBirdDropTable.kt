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
public val jubblyBirdDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Jubbly bird Drops",
    npcs = npcs("npc.100_jubbly_bird"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.100_jubbly_meat_raw" count 1
        "obj.feather" count 15..45
    },
)
