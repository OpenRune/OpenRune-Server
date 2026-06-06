package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val giantLobsterDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Giant lobster Drops",
    npcs = npcs("npc.slug2_giant_lobster", "npc.slug2_giant_lobster_peaceful"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Giant lobster Drops")
        6 weight "obj.bronze_pickaxe" count 1
        5 weight "obj.iron_pickaxe" count 1
        4 weight "obj.seaweed" count 1
        4 weight "obj.seaweed" count 2
        2 weight "obj.seaweed" count 5
        2 weight "obj.edible_seaweed" count 2
        4 weight "obj.tin_ore" count 3
        2 weight "obj.iron_ore" count 1
        2 weight "obj.coal" count 2
        2 weight "obj.copper_ore" count 3
        12 weight "obj.oystershell" count 2
        9 weight "obj.oystershell" count 1
        3 weight "obj.oysterempty" count 1
        1 weight "obj.oysterempty" count 3
        1 weight "obj.smalloysterpearls" count 1
        29 weight "obj.coins" count 4
        6 weight "obj.coins" count 8
        8 weight "obj.coins" count 36
        19 weight ringNothing()
        2 weight "obj.fishing_bait" count 10
        2 weight "obj.opal_bolttips" count 5
        1 weight "obj.slug2_seaslug_young" count 1
        1 weight "obj.spinach_roll" count 1
        1 weight "obj.casket" count 1
    },
)
