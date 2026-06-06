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
public val spidineDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Spidine Drops",
    npcs = npcs("npc.tol_spidine"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.red_spiders_eggs" count 3..6
    },
    mainTable = rsPlayerWeightedTable(total = 20) {
        name("Spidine Drops")
        3 weight "obj.tol_red_sack" count 1
        1 outOf 10 separate "obj.tol_tea" count 1
        17 weight nothing()
    },
)
