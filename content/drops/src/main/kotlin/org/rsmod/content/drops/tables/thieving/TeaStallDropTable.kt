package org.rsmod.content.drops.tables.thieving

import dtx.rs.RSDropTable
import dtx.rs.locs
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val teaStallDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Tea Stall (Thieving)",
    locs = locs("loc.tea_stall"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.cup_of_tea" count 1
    },
)
