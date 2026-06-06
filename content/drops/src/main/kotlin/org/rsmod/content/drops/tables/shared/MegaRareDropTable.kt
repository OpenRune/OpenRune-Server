package org.rsmod.content.drops.tables.shared

import dtx.rs.RSWeightedTable
import dtx.rs.rsWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.dropRollable
import org.rsmod.api.droptable.nothingDrop
import org.rsmod.game.entity.Player

public val megaRareDropTable: RSWeightedTable<Player, DropRollItem> = rsWeightedTable {
    name("Mega-rare drop table")
    113 weight dropRollable(nothingDrop())
    8 weight DropRollItem("obj.rune_spear", 1)
    4 weight DropRollItem("obj.dragonshield_a", 1)
    3 weight DropRollItem("obj.dragon_spear", 1)
}
