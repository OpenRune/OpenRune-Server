package org.rsmod.content.drops.tables.shared

import dtx.rs.RSGuaranteedTable
import dtx.rs.RSWeightedTable
import dtx.rs.rsGuaranteedTable
import dtx.rs.rsWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.game.entity.Player

/**
 * Standard OSRS herb drop table (shared subtable).
 *
 * Weights from [[Herb drop table]] on the OSRS Wiki. On F2P worlds the herb roll is replaced by 10 coins.
 */
public val herbDropTable: RSWeightedTable<Player, DropRollItem> = rsWeightedTable {
    name("Herb drop table")
    32 weight DropRollItem("obj.unidentified_guam", 1)
    24 weight DropRollItem("obj.unidentified_marentill", 1)
    18 weight DropRollItem("obj.unidentified_tarromin", 1)
    14 weight DropRollItem("obj.unidentified_harralander", 1)
    11 weight DropRollItem("obj.unidentified_ranarr", 1)
    8 weight DropRollItem("obj.unidentified_irit", 1)
    6 weight DropRollItem("obj.unidentified_avantoe", 1)
    5 weight DropRollItem("obj.unidentified_kwuarm", 1)
    4 weight DropRollItem("obj.unidentified_cadantine", 1)
    3 weight DropRollItem("obj.unidentified_lantadyme", 1)
    3 weight DropRollItem("obj.unidentified_dwarf_weed", 1)
}

public val doubleRollHerbDropTable: RSGuaranteedTable<Player, DropRollItem> = rsGuaranteedTable {
    name("Double herb roll")
    add(herbDropTable)
    add(herbDropTable)
}

public val tripleRollHerbDropTable: RSGuaranteedTable<Player, DropRollItem> = rsGuaranteedTable {
    name("Triple herb roll")
    add(herbDropTable)
    add(herbDropTable)
    add(herbDropTable)
}

public val usefulHerbDropTable: RSWeightedTable<Player, DropRollItem> = rsWeightedTable {
    name("Useful herb drop table")
    5 weight DropRollItem("obj.unidentified_avantoe", 1)
    4 weight DropRollItem("obj.unidentified_snapdragon", 1)
    4 weight DropRollItem("obj.unidentified_ranarr", 1)
    3 weight DropRollItem("obj.unidentified_torstol", 1)
}

public val combatHerbDropTable: RSWeightedTable<Player, DropRollItem> = rsWeightedTable {
    name("Combat herb drop table")
    5 weight DropRollItem("obj.unidentified_kwuarm", 1)
    4 weight DropRollItem("obj.unidentified_dwarf_weed", 1)
    4 weight DropRollItem("obj.unidentified_cadantine", 1)
    3 weight DropRollItem("obj.unidentified_lantadyme", 1)
}
