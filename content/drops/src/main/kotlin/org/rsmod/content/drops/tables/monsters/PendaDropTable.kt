package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import org.rsmod.api.droptable.ClueTier
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.clue
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val pendaDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Penda Drops",
    npcs = npcs("npc.death_man_indoors2"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Penda Drops")
        2 weight "obj.bronze_med_helm" count 1
        1 weight "obj.iron_dagger" count 1
        22 weight "obj.bolt" count 2..12
        3 weight "obj.bronze_arrow" count 7
        2 weight "obj.earthrune" count 4
        2 weight "obj.firerune" count 6
        2 weight "obj.mindrune" count 9
        1 weight "obj.chaosrune" count 2
        38 weight "obj.coins" count 3
        9 weight "obj.coins" count 5
        4 weight "obj.coins" count 15
        1 weight "obj.coins" count 25
        8 weight ringNothing()
        5 weight "obj.fishing_bait" count 1
        2 weight "obj.copper_ore" count 1
        2 weight "obj.earth_talisman" count 1
        1 weight "obj.cabbage" count 1

        23 weight SharedDropTables.herb
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 90 chance clue(ClueTier.Beginner)
        1 outOf 121 chance clue(ClueTier.Easy)
    },
)
