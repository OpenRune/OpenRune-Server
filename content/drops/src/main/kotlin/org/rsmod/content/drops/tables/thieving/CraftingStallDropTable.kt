package org.rsmod.content.drops.tables.thieving

import dtx.rs.RSDropTable
import dtx.rs.locs
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val craftingStallDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Crafting Stall (Thieving)",
    locs = locs("loc.dwarf_market_crafting"),
    mainTable = rsPlayerWeightedTable(total = 54) {
        name("Crafting Stall (Thieving)")
        12 weight "obj.amulet_mould" count 1
        12 weight "obj.jewl_bracelet_mould" count 1
        11 weight "obj.necklace_mould" count 1
        11 weight "obj.ring_mould" count 1
        6 weight "obj.chisel" count 1
        2 weight "obj.gold_bar" count 1
    },
)
