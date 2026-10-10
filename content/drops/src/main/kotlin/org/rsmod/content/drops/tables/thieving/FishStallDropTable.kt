package org.rsmod.content.drops.tables.thieving

import dtx.rs.RSDropTable
import dtx.rs.locs
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val fishStallDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Fish Stall (Thieving)",
    locs =
        locs(
            "loc.viking_fish_market",
            "loc.misc_fish_market",
            "loc.etc_fish_market",
            "loc.fish_stall_warrens",
        ),
    mainTable = rsPlayerWeightedTable(total = 20) {
        name("Fish Stall (Thieving)")
        14 weight "obj.raw_salmon" count 1
        5 weight "obj.raw_tuna" count 1
        1 weight "obj.raw_lobster" count 1
    },
)
