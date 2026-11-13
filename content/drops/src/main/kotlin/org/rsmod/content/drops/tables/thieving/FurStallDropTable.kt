package org.rsmod.content.drops.tables.thieving

import dtx.rs.RSDropTable
import dtx.rs.locs
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val furStallDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Fur Stall (Thieving)",
    locs =
        locs(
            "loc.furthiefstall",
            "loc.viking_fur_market",
            "loc.fortis_market_stall_fur",
        ),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.grey_wolf_fur" count 1
    },
)
