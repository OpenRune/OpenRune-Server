package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val rockCrabDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Rock Crab Drops",
    npcs = npcs("npc.horror_rockcrab", "npc.horror_rockcrab_inactive", "npc.horror_rockcrab_small", "npc.horror_rockcrab_small_inactive"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Rock Crab Drops")
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
        1 weight "obj.spinach_roll" count 1
        1 weight "obj.casket" count 1

        1 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.trail_elite_emote_exp1" count 1 condition { player ->
            // Drops Need Manual: Elite clue scrolls or elite caskets are only dropped when completing an Treasure Trails/Full guide/Elite#Cryptic clues
             true
        }
        1 outOf 1 weight "obj.trail_reward_casket_elite" count 1
        1 outOf 121 weight "obj.trail_clue_easy_simple001" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_clue_easy_simple001")
        }
    },
)
