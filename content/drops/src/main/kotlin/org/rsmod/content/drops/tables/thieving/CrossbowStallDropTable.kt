package org.rsmod.content.drops.tables.thieving

import dtx.rs.RSDropTable
import dtx.rs.locs
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val crossbowStallDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Crossbow Stall (Thieving)",
    locs = locs("loc.xbows_dwarf_market"),
    mainTable = rsPlayerWeightedTable(total = 80) {
        name("Crossbow Stall (Thieving)")
        46 weight "obj.bolt" count 3
        18 weight "obj.xbows_crossbow_limbs_bronze" count 3
        12 weight "obj.xbows_crossbow_stock_wood" count 3
        3 weight "obj.xbows_crossbow_bolts_mithril" count 3
        1 weight "obj.xbows_crossbow_limbs_mithril" count 3
    },
)
