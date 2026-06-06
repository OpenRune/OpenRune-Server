package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val fireElementalDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Fire elemental Drops",
    npcs = npcs("npc.elemental_fire"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Fire elemental Drops")
        13 weight "obj.firerune" count 15
        9 weight "obj.naturerune" count 2
        7 weight "obj.chaosrune" count 2
        6 weight "obj.lawrune" count 1
        4 weight "obj.deathrune" count 1
        3 weight "obj.firerune" count 20
        1 weight "obj.bloodrune" count 1
        36 weight "obj.coins" count 12
        3 weight "obj.coins" count 42
        2 weight "obj.staff_of_fire" count 1

        14 weight SharedDropTables.herb
        2 weight SharedDropTables.gem
        28 weight nothing()
    },
)
