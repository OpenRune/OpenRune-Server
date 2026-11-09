package org.rsmod.content.drops.tables.shared

import dtx.rs.RSWeightedTable
import dtx.rs.rsWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.game.entity.Player

public val rareSeedDropTable: RSWeightedTable<Player, DropRollItem> = rsWeightedTable(total = 1090) {
    name("Rare seed drop table")
    216 weight DropRollItem("obj.toadflax_seed", 1)
    148 weight DropRollItem("obj.irit_seed", 1)
    143 weight DropRollItem("obj.belladonna_seed", 1)
    103 weight DropRollItem("obj.avantoe_seed", 1)
    101 weight DropRollItem("obj.poisonivy_bush_seed", 1)
    96 weight DropRollItem("obj.cactus_seed", 1)
    70 weight DropRollItem("obj.potato_cactus_seed", 1)
    69 weight DropRollItem("obj.kwuarm_seed", 1)
    46 weight DropRollItem("obj.snapdragon_seed", 1)
    32 weight DropRollItem("obj.cadantine_seed", 1)
    23 weight DropRollItem("obj.lantadyme_seed", 1)
    20 weight DropRollItem("obj.snape_grass_seed", 3)
    14 weight DropRollItem("obj.dwarf_weed_seed", 1)
    9 weight DropRollItem("obj.torstol_seed", 1)
}
