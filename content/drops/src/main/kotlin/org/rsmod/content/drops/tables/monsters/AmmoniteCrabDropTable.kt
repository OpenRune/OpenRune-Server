package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.dropRollable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val ammoniteCrabDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Ammonite Crab Drops",
    npcs = npcs("npc.fossil_ammonitecrab"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Ammonite Crab Drops")
        5 weight "obj.iron_pickaxe" count 1
        29 weight "obj.fossil_numulite" count 4
        8 weight "obj.fossil_numulite" count 2
        6 weight "obj.fossil_numulite" count 8
        6 weight "obj.fossil_volcanic_ash" count 1
        2 weight "obj.iron_ore" count 1
        4 weight "obj.tin_ore" count 3
        2 weight "obj.opal_bolttips" count 5
        2 weight "obj.coal" count 2
        2 weight "obj.copper_ore" count 3
        12 weight "obj.oystershell" count 2
        9 weight "obj.oystershell" count 1
        8 weight "obj.seaweed" count 1..2
        3 weight "obj.oysterempty" count 1
        2 weight "obj.seaweed" count 5
        2 weight "obj.edible_seaweed" count 2
        1 weight "obj.smalloysterpearls" count 1
        1 weight "obj.oysterempty" count 3
        18 weight ringNothing()
        2 weight "obj.fishing_bait" count 10
        1 weight dropRollable(DropRollItem("obj.fossil_calcite", 1, bonusDrops = listOf(
            DropRollItem("obj.fossil_pyrophosphite", 1),
        )))
        1 weight "obj.seaweed_seed" count 2
        1 weight "obj.casket" count 1

        1 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 121 weight "obj.trail_clue_easy_simple001" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_clue_easy_simple001")
        }
    },
)
