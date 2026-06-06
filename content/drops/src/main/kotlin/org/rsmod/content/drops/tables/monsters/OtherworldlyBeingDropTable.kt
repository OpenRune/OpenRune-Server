package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val otherworldlyBeingDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Otherworldly being Drops",
    npcs = npcs("npc.otherworldly_being"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Otherworldly being Drops")
        9 weight "obj.naturerune" count 5
        8 weight "obj.chaosrune" count 4
        7 weight "obj.lawrune" count 2
        5 weight "obj.cosmicrune" count 2
        4 weight "obj.deathrune" count 2
        1 weight "obj.bloodrune" count 2
        59 weight "obj.coins" count 15
        18 weight ringNothing()
        2 weight "obj.ruby_ring" count 1
        1 weight "obj.mithril_mace" count 1
        1 weight "obj.mackerel" count 1

        10 weight SharedDropTables.herb
        3 weight SharedDropTables.gem
    },
)
