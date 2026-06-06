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
public val druidDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Druid Drops",
    npcs = npcs("npc.druid"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Druid Drops")
        4 weight "obj.earthrune" count 27
        2 weight "obj.waterrune" count 9
        2 weight "obj.earthrune" count 9
        2 weight "obj.firerune" count 9
        2 weight "obj.chaosrune" count 3
        1 weight "obj.lawrune" count 2
        10 weight "obj.coins" count 2
        4 weight "obj.coins" count 4
        3 weight "obj.coins" count 1
        3 weight "obj.coins" count 15
        1 weight "obj.coins" count 20
        37 weight ringNothing()
        10 weight "obj.vial_empty" count 1
        6 weight "obj.iron_dagger" count 1
        6 weight "obj.druidrobetop" count 1
        5 weight "obj.druidrobebottom" count 1
        3 weight "obj.limpwurt_root" count 1
        1 weight "obj.3doseantipoison" count 1

        26 weight SharedDropTables.herb
    },
)
