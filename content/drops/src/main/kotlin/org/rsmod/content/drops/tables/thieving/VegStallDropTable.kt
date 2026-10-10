package org.rsmod.content.drops.tables.thieving

import dtx.rs.RSDropTable
import dtx.rs.locs
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val vegStallDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Veg Stall (Thieving)",
    locs =
        locs(
            "loc.misc_veg_market",
            "loc.etc_veg_market",
        ),
    mainTable = rsPlayerWeightedTable(total = 10) {
        name("Veg Stall (Thieving)")
        3 weight "obj.potato" count 1
        2 weight "obj.cabbage" count 1
        2 weight "obj.onion" count 1
        2 weight "obj.tomato" count 1
        1 weight "obj.garlic" count 1
    },
)
