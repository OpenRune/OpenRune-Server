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
public val yakDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Yak Drops",
    npcs = npcs("npc.yak"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.yak_hide" count 1
        "obj.yak_hair" count 1
        "obj.yak_meat_raw" count 1
    },
)
