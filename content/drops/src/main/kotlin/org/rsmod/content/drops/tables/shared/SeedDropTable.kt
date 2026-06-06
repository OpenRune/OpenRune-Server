package org.rsmod.content.drops.tables.shared

import dtx.rs.RSWeightedTable
import dtx.rs.rsWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.game.entity.Player


public val allotmentSeedDropTable: RSWeightedTable<Player, DropRollItem> = rsWeightedTable {
    name("Allotment seed drop table")
    47 weight DropRollItem("obj.potato_seed", 4)
    36 weight DropRollItem("obj.onion_seed", 4)
    24 weight DropRollItem("obj.cabbage_seed", 4)
    12 weight DropRollItem("obj.tomato_seed", 3)
    6 weight DropRollItem("obj.sweetcorn_seed", 3)
    3 weight DropRollItem("obj.strawberry_seed", 2)
    1 weight DropRollItem("obj.watermelon_seed", 2)
    1 weight DropRollItem("obj.snape_grass_seed", 2)
}
