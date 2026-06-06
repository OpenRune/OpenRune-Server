package org.rsmod.content.drops.tables.shared

import dtx.rs.RSWeightedTable
import dtx.rs.rsWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.game.entity.Player

/**
 * Standard OSRS rare drop table (shared subtable).
 *
 * Weights from [[Rare drop table]] on the OSRS Wiki (/128 roll).
 */
public val rareDropTable: RSWeightedTable<Player, DropRollItem> = rsWeightedTable {
    name("Rare drop table")
    21 weight DropRollItem("obj.coins", 3000)
    20 weight gemDropTable
    15 weight megaRareDropTable
    5 weight DropRollItem("obj.runite_bar", 1)
    3 weight DropRollItem("obj.naturerune", 67)
    3 weight DropRollItem("obj.rune_2h_sword", 1)
    3 weight DropRollItem("obj.rune_battleaxe", 1)
    2 weight DropRollItem("obj.adamant_javelin", 20)
    2 weight DropRollItem("obj.deathrune", 45)
    2 weight DropRollItem("obj.lawrune", 45)
    2 weight DropRollItem("obj.rune_arrow", 42)
    2 weight DropRollItem("obj.steel_arrow", 150)
    2 weight DropRollItem("obj.rune_sq_shield", 1)
    2 weight DropRollItem("obj.keyhalf2", 1)
    2 weight DropRollItem("obj.keyhalf1", 1)
    2 weight DropRollItem("obj.dragonstone", 1)
    2 weight DropRollItem("obj.silver_ore_noted", 100)
    1 weight DropRollItem("obj.dragon_med_helm", 1)
    1 weight DropRollItem("obj.rune_kiteshield", 1)
}
