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
public val airElementalDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Air elemental Drops",
    npcs = npcs("npc.elemental_air"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Air elemental Drops")
        13 weight "obj.airrune" count 15
        9 weight "obj.naturerune" count 2
        7 weight "obj.chaosrune" count 2
        6 weight "obj.lawrune" count 1
        4 weight "obj.deathrune" count 1
        3 weight "obj.airrune" count 20
        1 weight "obj.bloodrune" count 1
        28 weight ringNothing()
        36 weight "obj.coins" count 12
        3 weight "obj.coins" count 42
        2 weight "obj.staff_of_air" count 1

        14 weight SharedDropTables.herb
        2 weight SharedDropTables.gem
    },
)
