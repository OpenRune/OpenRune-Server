package org.rsmod.content.drops.tables.shared

import dtx.rs.RSWeightedTable
import dtx.rs.rsWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.game.entity.Player

/**
 * OSRS rare seed drop table (shared subtable).
 *
 * Weights derived from [[Rare seed drop table]] wiki rarities (1/5.046 … 1/121.1).
 */
public val rareSeedDropTable: RSWeightedTable<Player, DropRollItem> = rsWeightedTable {
    name("Rare seed drop table")
    119 weight DropRollItem("obj.toadflax_seed", 1)
    81 weight DropRollItem("obj.irit_seed", 1)
    79 weight DropRollItem("obj.belladonna_seed", 1)
    57 weight DropRollItem("obj.avantoe_seed", 1)
    56 weight DropRollItem("obj.poison_ivy_seed", 1)
    53 weight DropRollItem("obj.cactus_seed", 1)
    39 weight DropRollItem("obj.potato_cactus_seed", 1)
    38 weight DropRollItem("obj.kwuarm_seed", 1)
    25 weight DropRollItem("obj.snapdragon_seed", 1)
    18 weight DropRollItem("obj.cadantine_seed", 1)
    13 weight DropRollItem("obj.lantadyme_seed", 1)
    11 weight DropRollItem("obj.snape_grass_seed", 3)
    8 weight DropRollItem("obj.dwarf_weed_seed", 1)
    5 weight DropRollItem("obj.torstol_seed", 1)
}
