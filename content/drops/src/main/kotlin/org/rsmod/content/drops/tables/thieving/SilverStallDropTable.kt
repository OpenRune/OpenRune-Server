package org.rsmod.content.drops.tables.thieving

import dtx.rs.RSDropTable
import dtx.rs.locs
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val silverStallDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Silver Stall (Thieving)",
    locs =
        locs(
            "loc.silverthiefstall",
            "loc.prif_marketstall_silver",
            "loc.dwarf_market_silver",
        ),
    mainTable = rsPlayerWeightedTable(total = 20) {
        name("Silver Stall (Thieving)")
        16 weight "obj.silver_ore" count 1
        3 weight "obj.silver_bar" count 1
        1 weight "obj.tiara" count 1
    },
)
