package org.rsmod.content.drops.tables.thieving

import dtx.rs.RSDropTable
import dtx.rs.locs
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val wineStallDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Wine Stall (Thieving)",
    locs = locs("loc.rag_market_stall"),
    mainTable = rsPlayerWeightedTable(total = 100) {
        name("Wine Stall (Thieving)")
        39 weight "obj.jug_empty" count 1
        20 weight "obj.jug_water" count 1
        17 weight "obj.grapes" count 1
        13 weight "obj.jug_wine" count 1
        11 weight "obj.rag_bottle_wine" count 1
    },
)
