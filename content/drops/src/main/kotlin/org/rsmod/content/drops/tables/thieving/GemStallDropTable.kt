package org.rsmod.content.drops.tables.thieving

import dtx.rs.RSDropTable
import dtx.rs.locs
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val gemStallDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Gem Stall (Thieving)",
    locs =
        locs(
            "loc.gemthiefstall",
            "loc.prif_marketstall_gem",
            "loc.dwarf_market_gems",
            "loc.fortis_market_stall_gems",
        ),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Gem Stall (Thieving)")
        105 weight "obj.uncut_sapphire" count 1
        17 weight "obj.uncut_emerald" count 1
        5 weight "obj.uncut_ruby" count 1
        1 weight "obj.uncut_diamond" count 1
    },
)
