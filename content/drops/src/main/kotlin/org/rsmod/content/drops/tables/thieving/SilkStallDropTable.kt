package org.rsmod.content.drops.tables.thieving

import dtx.rs.RSDropTable
import dtx.rs.locs
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val silkStallDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Silk Stall (Thieving)",
    locs =
        locs(
            "loc.silkthiefstall",
            "loc.prif_marketstall_silk",
            "loc.fortis_market_stall_silk",
        ),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.silk" count 1
    },
)
