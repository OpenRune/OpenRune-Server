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
public val jubsterDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Jubster Drops",
    npcs = npcs("npc.tol_jubster"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.100_jubbly_meat_raw" count 3..7
    },
    mainTable = rsPlayerWeightedTable(total = 20) {
        name("Jubster Drops")
        3 weight "obj.tol_gold_sack" count 1
        1 outOf 10 separate "obj.tol_tea" count 1
        17 weight nothing()
    },
)
