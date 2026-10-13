package org.rsmod.content.drops.tables.thieving

import dtx.rs.RSDropTable
import dtx.rs.locs
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val spiceStallDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Spice Stall (Thieving)",
    locs =
        locs(
            "loc.spicethiefstall",
            "loc.prif_marketstall_spice",
            "loc.fortis_market_stall_spice",
        ),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.spicespot" count 1
    },
)
