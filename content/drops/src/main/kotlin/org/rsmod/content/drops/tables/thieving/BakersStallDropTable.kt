package org.rsmod.content.drops.tables.thieving

import dtx.rs.RSDropTable
import dtx.rs.locs
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val bakersStallDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Baker's Stall (Thieving)",
    locs =
        locs(
            "loc.cakethiefstall",
            "loc.dwarf_market_bakery",
            "loc.hos_stall_bread",
            "loc.fortis_market_stall_bakers",
        ),
    mainTable = rsPlayerWeightedTable(total = 20) {
        name("Baker's Stall (Thieving)")
        13 weight "obj.cake" count 1
        5 weight "obj.bread" count 1
        2 weight "obj.chocolate_slice" count 1
    },
)
